package app.vazovsky.permaware.scan

import androidx.room.withTransaction
import app.vazovsky.permaware.core.util.TimeProvider
import app.vazovsky.permaware.data.db.AppSnapshotEntity
import app.vazovsky.permaware.data.db.ChangeDao
import app.vazovsky.permaware.data.db.ChangeEventEntity
import app.vazovsky.permaware.data.db.PermissionWatchDatabase
import app.vazovsky.permaware.data.db.ScanDao
import app.vazovsky.permaware.data.db.ScanEntity
import app.vazovsky.permaware.data.db.SnapshotCodec
import app.vazovsky.permaware.data.db.SnapshotDao
import app.vazovsky.permaware.data.platform.InstalledAppsDataSource
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.domain.attention.AttentionEngine
import app.vazovsky.permaware.domain.attention.PrivacyOverview
import app.vazovsky.permaware.domain.attention.PrivacyOverviewCalculator
import app.vazovsky.permaware.domain.diff.SnapshotDiffer
import app.vazovsky.permaware.domain.model.AppSnapshot
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionSnapshot
import app.vazovsky.permaware.domain.model.Scan
import app.vazovsky.permaware.domain.model.ScanTrigger
import app.vazovsky.permaware.domain.model.SpecialAccessType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Бросается, когда платформа сообщает, что пакетов нет вообще.
 *
 * Пакеты есть на любом Android-устройстве — как минимум само это приложение, — так что пустой
 * результат означает, что перебор не удался, а не что устройство пустое. Счесть это успехом было бы
 * худшим отказом, на какой этот продукт способен: человеку показали бы безупречные 100/100 и
 * «приложений, требующих внимания, не найдено» — *именно потому*, что проверка не сработала.
 * Вдобавок это отравило бы следующую проверку: она сравнила бы полное устройство с пустым снимком и
 * объявила бы удалёнными все приложения разом.
 */
class EmptyScanException : IllegalStateException("Package enumeration returned no packages")

/** Всё, что дала одна проверка, — интерфейсу единым куском. */
data class ScanOutcome(
    val scan: Scan,
    val apps: List<InstalledApp>,
    val overview: PrivacyOverview,
    val changes: List<ChangeEvent>,
)

/**
 * Конвейер проверки, в том порядке, в каком его описывает README:
 *
 *  1. прочитать пакеты, которые платформа даёт увидеть
 *  2. разобрать их разрешения и особый доступ
 *  3. оценить внимание по каждому приложению
 *  4. свести сводку по устройству
 *  5. сравнить с прошлым снимком
 *  6. сохранить проверку, снимки и изменения одной транзакцией
 *  7. применить политику хранения
 *
 * Шаги с 1 по 5 либо чистые, либо упираются в ввод-вывод, и главного потока не касаются
 * никогда. Проверка устройства с 473 пакетами на эталонном железе занимает вхолодную около
 * трёх секунд.
 */
@Singleton
class ScanPipeline @Inject constructor(
    private val installedApps: InstalledAppsDataSource,
    private val database: PermissionWatchDatabase,
    private val scanDao: ScanDao,
    private val snapshotDao: SnapshotDao,
    private val changeDao: ChangeDao,
    private val preferences: PreferencesRepository,
    private val time: TimeProvider,
) {

    suspend fun run(trigger: ScanTrigger): ScanOutcome = withContext(Dispatchers.Default) {
        val startedAt = time.now()

        // 1–2. Факты от платформы.
        val rawApps = installedApps.loadApps()
        if (rawApps.isEmpty()) throw EmptyScanException()

        // 3. Толкование: внимание считается здесь, а не в источнике данных.
        val apps = rawApps.map { app ->
            app.copy(
                attention = AttentionEngine.assess(
                    permissions = app.permissions,
                    specialAccess = app.specialAccess,
                    isSystemApp = app.isSystem,
                    hasKnownInstallSource = app.installSource.installerPackage != null,
                ),
            )
        }.sortedBy { it.label.lowercase() }

        // 4. Сводка по устройству.
        val overview = PrivacyOverviewCalculator.calculate(apps)

        // 5. Сравнение с прошлой проверкой.
        val previousScan = scanDao.latest()
        val previousSnapshots = previousScan
            ?.let { snapshotDao.forScan(it.id).map(::toDomain) }
            .orEmpty()
        val currentSnapshots = apps.map(::toSnapshot)
        val finishedAt = time.now()
        val changes = SnapshotDiffer.diff(previousSnapshots, currentSnapshots, finishedAt)

        // 6. Сохранение.
        val scanEntity = ScanEntity(
            startedAt = startedAt,
            finishedAt = finishedAt,
            appCount = apps.size,
            userAppCount = apps.count { it.isUserApp },
            changeCount = changes.size,
            privacyScore = overview.score,
            trigger = trigger.name,
        )

        val scanId = database.withTransaction {
            val id = scanDao.insert(scanEntity)
            snapshotDao.insertAll(currentSnapshots.map { it.toEntity(id) })
            if (changes.isNotEmpty()) {
                changeDao.insertAll(changes.map { it.toEntity(id) })
            }
            id
        }

        // 7. Хранение.
        applyRetention()
        preferences.incrementCompletedScans()

        ScanOutcome(
            scan = Scan(
                id = scanId,
                startedAt = startedAt,
                finishedAt = finishedAt,
                appCount = apps.size,
                userAppCount = scanEntity.userAppCount,
                changeCount = changes.size,
                privacyScore = overview.score,
                trigger = trigger,
            ),
            apps = apps,
            overview = overview,
            changes = changes.map { it.copy(scanId = scanId) },
        )
    }

    /**
     * Снимки существуют только затем, чтобы *следующей* проверке было с чем сравнивать, поэтому их
     * держится всего два последних — независимо от настройки хранения. История, которую человек
     * действительно читает, живёт в `change_events` и подчиняется выбранному им сроку.
     */
    private suspend fun applyRetention() {
        snapshotDao.trimToRecentScans(SNAPSHOTS_TO_KEEP)
        val retention = preferences.current().historyRetention
        if (retention.days == Int.MAX_VALUE) return
        val cutoff = time.now() - retention.days * MILLIS_PER_DAY
        changeDao.deleteOlderThan(cutoff)
        scanDao.deleteOlderThan(cutoff, keepAtLeast = SNAPSHOTS_TO_KEEP)
    }

    private fun toSnapshot(app: InstalledApp) = AppSnapshot(
        packageName = app.packageName,
        label = app.label,
        versionName = app.versionName,
        versionCode = app.versionCode,
        isSystem = app.isSystem,
        lastUpdateTime = app.lastUpdateTime,
        permissions = app.permissions.map { PermissionSnapshot(it.name, it.state) },
        specialAccess = app.specialAccess,
    )

    private fun toDomain(entity: AppSnapshotEntity) = AppSnapshot(
        packageName = entity.packageName,
        label = entity.label,
        versionName = entity.versionName,
        versionCode = entity.versionCode,
        isSystem = entity.isSystem,
        lastUpdateTime = entity.lastUpdateTime,
        permissions = SnapshotCodec.decodePermissions(entity.permissions),
        specialAccess = SnapshotCodec.decodeSpecialAccess(entity.specialAccess),
    )

    private fun AppSnapshot.toEntity(scanId: Long) = AppSnapshotEntity(
        scanId = scanId,
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        isSystem = isSystem,
        lastUpdateTime = lastUpdateTime,
        permissions = SnapshotCodec.encodePermissions(permissions),
        specialAccess = SnapshotCodec.encodeSpecialAccess(specialAccess),
    )

    private fun ChangeEvent.toEntity(scanId: Long) = ChangeEventEntity(
        scanId = scanId,
        timestamp = timestamp,
        packageName = packageName,
        appLabel = appLabel,
        type = type.name,
        permission = permission,
        category = category?.name,
        specialAccess = specialAccess?.name,
        previousVersion = previousVersion,
        newVersion = newVersion,
    )

    private companion object {
        const val SNAPSHOTS_TO_KEEP = 2
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}

fun ChangeEventEntity.toDomain(): ChangeEvent = ChangeEvent(
    id = id,
    scanId = scanId,
    timestamp = timestamp,
    packageName = packageName,
    appLabel = appLabel,
    type = runCatching { ChangeType.valueOf(type) }.getOrDefault(ChangeType.APP_UPDATED),
    permission = permission,
    category = category?.let { name -> runCatching { PermissionCategory.valueOf(name) }.getOrNull() },
    specialAccess = specialAccess?.let { name ->
        runCatching { SpecialAccessType.valueOf(name) }.getOrNull()
    },
    previousVersion = previousVersion,
    newVersion = newVersion,
)
