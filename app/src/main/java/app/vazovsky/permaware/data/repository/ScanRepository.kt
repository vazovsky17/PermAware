package app.vazovsky.permaware.data.repository

import app.vazovsky.permaware.data.db.ChangeDao
import app.vazovsky.permaware.data.db.ScanDao
import app.vazovsky.permaware.data.db.ScanEntity
import app.vazovsky.permaware.domain.attention.PrivacyOverview
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.Scan
import app.vazovsky.permaware.domain.model.ScanTrigger
import app.vazovsky.permaware.scan.ScanOutcome
import app.vazovsky.permaware.scan.ScanPipeline
import app.vazovsky.permaware.scan.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ScanStatus {
    data object Idle : ScanStatus
    data object Running : ScanStatus
    data class Failed(val cause: Throwable) : ScanStatus
}

/**
 * Единственный источник истины о результатах проверки, пока приложение живёт.
 *
 * Полный список [InstalledApp] держится в памяти всю жизнь процесса: именно его рисуют список
 * приложений и экран приложения, а перечитывать `PackageManager` на каждый переход стоило бы
 * секунд. Целиком он *не* сохраняется — в базе лежит только компактный снимок, нужный, чтобы
 * сравнить со следующей проверкой, плюс история изменений.
 */
@Singleton
class ScanRepository @Inject constructor(
    private val pipeline: ScanPipeline,
    private val scanDao: ScanDao,
    private val changeDao: ChangeDao,
) {
    private val scanMutex = Mutex()

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val status: StateFlow<ScanStatus> = _status.asStateFlow()

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _overview = MutableStateFlow<PrivacyOverview?>(null)
    val overview: StateFlow<PrivacyOverview?> = _overview.asStateFlow()

    /** Истина с того момента, как проверка в этом процессе дала результат. */
    val hasResults: Boolean get() = _apps.value.isNotEmpty()

    val latestScan: Flow<Scan?> = scanDao.observeLatest().map { it?.toDomain() }

    val scanCount: Flow<Int> = scanDao.observeCount()

    fun observeRecentChanges(limit: Int = RECENT_CHANGE_LIMIT): Flow<List<ChangeEvent>> =
        changeDao.observeRecent(limit).map { events -> events.map { it.toDomain() } }

    fun observeChangesForApp(packageName: String): Flow<List<ChangeEvent>> =
        changeDao.observeForApp(packageName).map { events -> events.map { it.toDomain() } }

    fun appOf(packageName: String): InstalledApp? = _apps.value.firstOrNull { it.packageName == packageName }

    /**
     * Запускает проверку, если ни одна ещё не идёт; если идёт — вызывающая сторона просто ждёт её.
     * Две одновременные проверки "подрались бы" за таблицу снимков и могли бы наплодить изменений,
     * которых не было.
     */
    suspend fun scan(trigger: ScanTrigger): Result<ScanOutcome> = scanMutex.withLock {
        runScan(trigger)
    }

    /**
     * Подгружает результаты, если у процесса их ещё нет. Вызывается при открытии экрана.
     */
    suspend fun ensureResults(trigger: ScanTrigger = ScanTrigger.MANUAL) {
        scanMutex.withLock {
            if (hasResults) return
            runScan(trigger)
        }
    }

    /** Вызывать только удерживая [scanMutex]. */
    private suspend fun runScan(trigger: ScanTrigger): Result<ScanOutcome> {
        _status.value = ScanStatus.Running
        return try {
            val outcome = pipeline.run(trigger)
            _apps.value = outcome.apps
            _overview.value = outcome.overview
            _status.value = ScanStatus.Idle
            Result.success(outcome)
        } catch (e: Exception) {
            // То, что уже на экране, остаётся: неудачное обновление не должно обнулять обзор.
            _status.value = ScanStatus.Failed(e)
            Result.failure(e)
        }
    }

    fun clearInMemoryResults() {
        _apps.value = emptyList()
        _overview.value = null
        _status.value = ScanStatus.Idle
    }

    private companion object {
        const val RECENT_CHANGE_LIMIT = 200
    }
}

fun ScanEntity.toDomain(): Scan = Scan(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    appCount = appCount,
    userAppCount = userAppCount,
    changeCount = changeCount,
    privacyScore = privacyScore,
    trigger = runCatching { ScanTrigger.valueOf(trigger) }.getOrDefault(ScanTrigger.MANUAL),
)
