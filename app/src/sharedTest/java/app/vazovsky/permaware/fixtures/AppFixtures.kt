package app.vazovsky.permaware.fixtures

import app.vazovsky.permaware.domain.attention.AttentionEngine
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.AppSnapshot
import app.vazovsky.permaware.domain.model.AttentionAssessment
import app.vazovsky.permaware.domain.model.InstallSource
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionProtection
import app.vazovsky.permaware.domain.model.PermissionSnapshot
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.Sensitivity
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.domain.permission.PermissionCatalog

/**
 * Набор фикстур, на котором стоят все тесты: обычное приложение, приложения с одним датчиком,
 * сайдлоад, жадный до местоположения, чужое разрешение, системное приложение и недавно обновлённое.
 *
 * Здесь ничто не трогает `PackageManager`, поэтому доменный набор гоняется на JVM, а его результаты
 * не зависят от того, что оказалось установлено на машине, где идут тесты.
 */
object AppFixtures {

    const val PERM_CAMERA = "android.permission.CAMERA"
    const val PERM_MIC = "android.permission.RECORD_AUDIO"
    const val PERM_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val PERM_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
    const val PERM_CONTACTS = "android.permission.READ_CONTACTS"
    const val PERM_SMS = "android.permission.READ_SMS"
    const val PERM_INSTALL = "android.permission.REQUEST_INSTALL_PACKAGES"
    const val PERM_OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"
    const val PERM_INTERNET = "android.permission.INTERNET"
    const val PERM_CUSTOM = "com.example.vendor.permission.SYNC_DATA"

    fun permission(
        name: String,
        state: PermissionState = PermissionState.GRANTED,
        protection: PermissionProtection = PermissionProtection.DANGEROUS,
        category: PermissionCategory? = null,
        sensitivity: Sensitivity? = null,
        isCustom: Boolean = false,
        platformLabel: String? = null,
        platformDescription: String? = null,
    ): AppPermission {
        val knowledge = PermissionCatalog.find(name)
        return AppPermission(
            name = name,
            category = category ?: knowledge?.category ?: PermissionCategory.OTHER,
            protection = protection,
            state = state,
            sensitivity = sensitivity ?: knowledge?.sensitivity ?: Sensitivity.NONE,
            isCustom = isCustom,
            platformLabel = platformLabel,
            platformDescription = platformDescription,
        )
    }

    fun app(
        packageName: String,
        label: String = packageName,
        versionName: String? = "1.0",
        versionCode: Long = 1,
        permissions: List<AppPermission> = emptyList(),
        specialAccess: List<SpecialAccessState> = emptyList(),
        isSystem: Boolean = false,
        isUpdatedSystemApp: Boolean = false,
        isEnabled: Boolean = true,
        installer: String? = "com.android.vending",
        firstInstallTime: Long = 1_700_000_000_000,
        lastUpdateTime: Long = 1_700_000_000_000,
        assess: Boolean = true,
    ): InstalledApp {
        val base = InstalledApp(
            packageName = packageName,
            label = label,
            versionName = versionName,
            versionCode = versionCode,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            targetSdk = 35,
            isSystem = isSystem,
            isUpdatedSystemApp = isUpdatedSystemApp,
            isEnabled = isEnabled,
            installSource = InstallSource(installer, installer),
            permissions = permissions,
            specialAccess = specialAccess,
            attention = AttentionAssessment.None,
        )
        return if (!assess) {
            base
        } else {
            base.copy(
                attention = AttentionEngine.assess(
                    permissions = permissions,
                    specialAccess = specialAccess,
                    isSystemApp = isSystem,
                    hasKnownInstallSource = installer != null,
                ),
            )
        }
    }

    /** Безобидная утилита: одно нечувствительное разрешение. */
    val calculator: InstalledApp = app(
        packageName = "com.example.calculator",
        label = "Калькулятор",
        permissions = listOf(permission(PERM_INTERNET, protection = PermissionProtection.NORMAL)),
    )

    val cameraApp: InstalledApp = app(
        packageName = "com.example.camera",
        label = "Камера+",
        permissions = listOf(permission(PERM_CAMERA)),
    )

    val microphoneApp: InstalledApp = app(
        packageName = "com.example.recorder",
        label = "Диктофон",
        permissions = listOf(permission(PERM_MIC)),
    )

    /** Мессенджер: доступ широкий, но законный. Помечаться повышенным НЕ должен. */
    val messenger: InstalledApp = app(
        packageName = "com.example.messenger",
        label = "Мессенджер",
        versionName = "5.8.0",
        versionCode = 580,
        permissions = listOf(
            permission(PERM_CAMERA),
            permission(PERM_MIC),
            permission(PERM_CONTACTS),
            permission(PERM_FINE_LOCATION),
            permission(PERM_INTERNET, protection = PermissionProtection.NORMAL),
        ),
    )

    /** Сайдлоадный фонарик, набравший прав на местоположение и установку. Должен выделяться. */
    val locationHeavySideload: InstalledApp = app(
        packageName = "com.example.flashlight",
        label = "Фонарик",
        versionName = "2.3",
        versionCode = 23,
        permissions = listOf(
            permission(PERM_FINE_LOCATION),
            permission(PERM_BACKGROUND_LOCATION),
            permission(PERM_CONTACTS),
            permission(
                PERM_INSTALL,
                state = PermissionState.UNDETERMINED,
                protection = PermissionProtection.APPOP_GATED,
            ),
        ),
        specialAccess = listOf(
            SpecialAccessState(SpecialAccessType.INSTALL_UNKNOWN_APPS, SpecialAccessStatus.DECLARED),
        ),
        installer = null,
    )

    val systemApp: InstalledApp = app(
        packageName = "com.android.systemui",
        label = "Интерфейс системы",
        permissions = listOf(permission(PERM_CAMERA), permission(PERM_MIC)),
        isSystem = true,
        installer = null,
    )

    val updatedSystemApp: InstalledApp = app(
        packageName = "com.android.vending",
        label = "Google Play",
        permissions = listOf(permission(PERM_FINE_LOCATION)),
        isSystem = true,
        isUpdatedSystemApp = true,
        installer = null,
    )

    val recentlyUpdated: InstalledApp = app(
        packageName = "com.example.news",
        label = "Новости",
        permissions = listOf(permission(PERM_FINE_LOCATION, state = PermissionState.DENIED)),
        lastUpdateTime = 1_757_000_000_000,
    )

    /** Приложение, объявляющее разрешение, о котором PermAware ничего не знает. */
    val customPermissionApp: InstalledApp = app(
        packageName = "com.example.vendor",
        label = "Vendor Sync",
        permissions = listOf(
            permission(
                PERM_CUSTOM,
                protection = PermissionProtection.SIGNATURE,
                category = PermissionCategory.OTHER,
                sensitivity = Sensitivity.NONE,
                isCustom = true,
                platformLabel = "Синхронизация данных",
                platformDescription = "Позволяет обмениваться данными с сервисом производителя.",
            ),
        ),
    )

    val all: List<InstalledApp> = listOf(
        calculator,
        cameraApp,
        microphoneApp,
        messenger,
        locationHeavySideload,
        systemApp,
        updatedSystemApp,
        recentlyUpdated,
        customPermissionApp,
    )

    // ---------------------------------------------------------------------------------------
    // Полный охват категорий
    // ---------------------------------------------------------------------------------------

    /**
     * По одному пользовательскому и одному системному приложению на каждую отчётную категорию — для
     * тестов, которым надо обойти *все* категории, а не те четыре, что случайно задевает [all].
     */
    val categoryPermissions: Map<PermissionCategory, String> = mapOf(
        PermissionCategory.LOCATION to PERM_FINE_LOCATION,
        PermissionCategory.CAMERA to PERM_CAMERA,
        PermissionCategory.MICROPHONE to PERM_MIC,
        PermissionCategory.CONTACTS to PERM_CONTACTS,
        PermissionCategory.SMS to PERM_SMS,
        PermissionCategory.STORAGE to "android.permission.READ_MEDIA_IMAGES",
        PermissionCategory.CALENDAR to "android.permission.READ_CALENDAR",
        PermissionCategory.PHONE to "android.permission.READ_PHONE_STATE",
        PermissionCategory.CALL_LOG to "android.permission.READ_CALL_LOG",
        PermissionCategory.SENSORS to "android.permission.BODY_SENSORS",
        PermissionCategory.ACTIVITY to "android.permission.ACTIVITY_RECOGNITION",
        PermissionCategory.NEARBY to "android.permission.BLUETOOTH_SCAN",
        // Не POST_NOTIFICATIONS: каталог оценивает его как Sensitivity.NONE — показывать
        // уведомления приложению и положено, — а `hasAccessTo` считает только чувствительные
        // выдачи, так что приложение с одним им справедливо не является «приложением с доступом к
        // уведомлениям». Считается ACCESS_NOTIFICATION_POLICY, который позволяет заглушить телефон.
        PermissionCategory.NOTIFICATIONS to "android.permission.ACCESS_NOTIFICATION_POLICY",
        PermissionCategory.ACCOUNTS to "android.permission.GET_ACCOUNTS",
    )

    /** Пользовательское приложение с доступом к [category], и ничего кроме. */
    fun userAppFor(category: PermissionCategory): InstalledApp = app(
        packageName = "com.example.user.${category.name.lowercase()}",
        label = "User ${category.name}",
        permissions = listOf(permission(requirePermissionFor(category))),
    )

    /** Системное приложение с доступом к [category] — то, что обзор считать *не* должен. */
    fun systemAppFor(category: PermissionCategory): InstalledApp = app(
        packageName = "com.android.system.${category.name.lowercase()}",
        label = "System ${category.name}",
        permissions = listOf(permission(requirePermissionFor(category))),
        isSystem = true,
        installer = null,
    )

    private fun requirePermissionFor(category: PermissionCategory): String =
        requireNotNull(categoryPermissions[category]) {
            "No fixture permission for $category. A category was added to the domain without a " +
                "fixture, so the tests that walk every category would silently skip it."
        }

    val everyCategory: List<InstalledApp> =
        categoryPermissions.keys.flatMap { listOf(userAppFor(it), systemAppFor(it)) }

    fun snapshot(app: InstalledApp): AppSnapshot = AppSnapshot(
        packageName = app.packageName,
        label = app.label,
        versionName = app.versionName,
        versionCode = app.versionCode,
        isSystem = app.isSystem,
        lastUpdateTime = app.lastUpdateTime,
        permissions = app.permissions.map { PermissionSnapshot(it.name, it.state) },
        specialAccess = app.specialAccess,
    )

    fun snapshots(vararg apps: InstalledApp): List<AppSnapshot> = apps.map(::snapshot)
}
