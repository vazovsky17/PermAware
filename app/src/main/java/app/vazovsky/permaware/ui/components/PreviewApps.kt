package app.vazovsky.permaware.ui.components

import app.vazovsky.permaware.domain.attention.AttentionEngine
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.InstallSource
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType
import app.vazovsky.permaware.domain.permission.PermissionCatalog

/**
 * Данные-образцы только для `@Preview`.
 */
internal object PreviewApps {

    private fun permission(name: String, state: PermissionState = PermissionState.GRANTED): AppPermission {
        val knowledge = PermissionCatalog.find(name)
        return AppPermission(
            name = name,
            category = knowledge?.category ?: PermissionCategory.OTHER,
            protection = app.vazovsky.permaware.domain.model.PermissionProtection.DANGEROUS,
            state = state,
            sensitivity = knowledge?.sensitivity
                ?: app.vazovsky.permaware.domain.model.Sensitivity.NOTABLE,
        )
    }

    private fun build(
        packageName: String,
        label: String,
        versionName: String,
        permissions: List<AppPermission>,
        specialAccess: List<SpecialAccessState> = emptyList(),
        isSystem: Boolean = false,
        installer: String? = "com.android.vending",
        lastUpdateTime: Long = 1_757_000_000_000,
    ): InstalledApp {
        val base = InstalledApp(
            packageName = packageName,
            label = label,
            versionName = versionName,
            versionCode = 100,
            firstInstallTime = 1_700_000_000_000,
            lastUpdateTime = lastUpdateTime,
            targetSdk = 35,
            isSystem = isSystem,
            isUpdatedSystemApp = false,
            isEnabled = true,
            installSource = InstallSource(installer, installer?.let { "Google Play" }),
            permissions = permissions,
            specialAccess = specialAccess,
            attention = app.vazovsky.permaware.domain.model.AttentionAssessment.None,
        )
        return base.copy(
            attention = AttentionEngine.assess(
                permissions = permissions,
                specialAccess = specialAccess,
                isSystemApp = isSystem,
                hasKnownInstallSource = installer != null,
            ),
        )
    }

    val messenger: InstalledApp = build(
        packageName = "com.example.messenger",
        label = "Мессенджер",
        versionName = "5.9.0",
        permissions = listOf(
            permission("android.permission.CAMERA"),
            permission("android.permission.RECORD_AUDIO"),
            permission("android.permission.READ_CONTACTS"),
            permission("android.permission.ACCESS_FINE_LOCATION", PermissionState.DENIED),
            permission("android.permission.POST_NOTIFICATIONS"),
        ),
    )

    val flashlight: InstalledApp = build(
        packageName = "com.example.flashlight",
        label = "Фонарик",
        versionName = "2.4",
        permissions = listOf(
            permission("android.permission.ACCESS_FINE_LOCATION"),
            permission("android.permission.ACCESS_BACKGROUND_LOCATION"),
            permission("android.permission.READ_CONTACTS"),
            permission("android.permission.REQUEST_INSTALL_PACKAGES", PermissionState.UNDETERMINED),
        ),
        specialAccess = listOf(
            SpecialAccessState(SpecialAccessType.INSTALL_UNKNOWN_APPS, SpecialAccessStatus.DECLARED),
            SpecialAccessState(SpecialAccessType.DISPLAY_OVER_OTHER_APPS, SpecialAccessStatus.ACTIVE),
        ),
        installer = null,
    )

    val calculator: InstalledApp = build(
        packageName = "com.example.calculator",
        label = "Калькулятор",
        versionName = "1.0.2",
        permissions = listOf(permission("android.permission.VIBRATE", PermissionState.GRANTED)),
        lastUpdateTime = 1_740_000_000_000,
    )

    val all: List<InstalledApp> = listOf(messenger, flashlight, calculator)

    val changes: List<ChangeEvent> = listOf(
        ChangeEvent(
            id = 1,
            timestamp = 1_757_000_000_000,
            packageName = flashlight.packageName,
            appLabel = flashlight.label,
            type = ChangeType.APP_UPDATED,
            previousVersion = "2.3",
            newVersion = "2.4",
        ),
        ChangeEvent(
            id = 2,
            timestamp = 1_757_000_000_000,
            packageName = flashlight.packageName,
            appLabel = flashlight.label,
            type = ChangeType.PERMISSION_REQUEST_ADDED,
            permission = "android.permission.ACCESS_BACKGROUND_LOCATION",
            category = PermissionCategory.LOCATION,
        ),
        ChangeEvent(
            id = 3,
            timestamp = 1_756_900_000_000,
            packageName = messenger.packageName,
            appLabel = messenger.label,
            type = ChangeType.PERMISSION_GRANTED,
            permission = "android.permission.READ_CONTACTS",
            category = PermissionCategory.CONTACTS,
        ),
    )
}
