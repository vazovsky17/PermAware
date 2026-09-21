package app.vazovsky.permaware.ui.common

import androidx.annotation.StringRes
import app.vazovsky.permaware.R
import app.vazovsky.permaware.data.prefs.HistoryRetention
import app.vazovsky.permaware.data.prefs.ScanInterval
import app.vazovsky.permaware.data.prefs.ThemeMode
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.domain.model.ChangeFilter
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.PermissionProtection
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType

@get:StringRes
val PermissionCategory.labelRes: Int
    get() = when (this) {
        PermissionCategory.LOCATION -> R.string.category_location
        PermissionCategory.CAMERA -> R.string.category_camera
        PermissionCategory.MICROPHONE -> R.string.category_microphone
        PermissionCategory.CONTACTS -> R.string.category_contacts
        PermissionCategory.CALENDAR -> R.string.category_calendar
        PermissionCategory.SMS -> R.string.category_sms
        PermissionCategory.PHONE -> R.string.category_phone
        PermissionCategory.CALL_LOG -> R.string.category_call_log
        PermissionCategory.STORAGE -> R.string.category_storage
        PermissionCategory.SENSORS -> R.string.category_sensors
        PermissionCategory.ACTIVITY -> R.string.category_activity
        PermissionCategory.NEARBY -> R.string.category_nearby
        PermissionCategory.NOTIFICATIONS -> R.string.category_notifications
        PermissionCategory.ACCOUNTS -> R.string.category_accounts
        PermissionCategory.SPECIAL_ACCESS -> R.string.category_special_access
        PermissionCategory.SYSTEM -> R.string.category_system
        PermissionCategory.OTHER -> R.string.category_other
    }

@get:StringRes
val AttentionLevel.labelRes: Int
    get() = when (this) {
        AttentionLevel.LOW -> R.string.attention_low
        AttentionLevel.MODERATE -> R.string.attention_moderate
        AttentionLevel.ELEVATED -> R.string.attention_elevated
    }

@get:StringRes
val AttentionLevel.shortLabelRes: Int
    get() = when (this) {
        AttentionLevel.LOW -> R.string.attention_low_short
        AttentionLevel.MODERATE -> R.string.attention_moderate_short
        AttentionLevel.ELEVATED -> R.string.attention_elevated_short
    }

@get:StringRes
val PermissionState.labelRes: Int
    get() = when (this) {
        PermissionState.GRANTED -> R.string.permission_state_granted
        PermissionState.DENIED -> R.string.permission_state_denied
        PermissionState.UNDETERMINED -> R.string.permission_state_undetermined
    }

@get:StringRes
val PermissionProtection.labelRes: Int
    get() = when (this) {
        PermissionProtection.NORMAL -> R.string.permission_protection_normal
        PermissionProtection.DANGEROUS -> R.string.permission_protection_dangerous
        PermissionProtection.SIGNATURE -> R.string.permission_protection_signature
        PermissionProtection.APPOP_GATED -> R.string.permission_protection_appop
        PermissionProtection.UNKNOWN -> R.string.permission_protection_unknown
    }

@get:StringRes
val SpecialAccessType.labelRes: Int
    get() = when (this) {
        SpecialAccessType.ACCESSIBILITY -> R.string.special_accessibility
        SpecialAccessType.NOTIFICATION_LISTENER -> R.string.special_notification_listener
        SpecialAccessType.DEVICE_ADMIN -> R.string.special_device_admin
        SpecialAccessType.BATTERY_UNRESTRICTED -> R.string.special_battery
        SpecialAccessType.VPN_SERVICE -> R.string.special_vpn
        SpecialAccessType.DISPLAY_OVER_OTHER_APPS -> R.string.special_overlay
        SpecialAccessType.INSTALL_UNKNOWN_APPS -> R.string.special_install_unknown
        SpecialAccessType.USAGE_ACCESS -> R.string.special_usage_access
        SpecialAccessType.EXACT_ALARM -> R.string.special_exact_alarm
        SpecialAccessType.ALL_FILES_ACCESS -> R.string.special_all_files
        SpecialAccessType.WRITE_SYSTEM_SETTINGS -> R.string.special_write_settings
        SpecialAccessType.PICTURE_IN_PICTURE -> R.string.special_pip
    }

@get:StringRes
val SpecialAccessStatus.labelRes: Int
    get() = when (this) {
        SpecialAccessStatus.ACTIVE -> R.string.special_status_active
        SpecialAccessStatus.INACTIVE -> R.string.special_status_inactive
        SpecialAccessStatus.DECLARED -> R.string.special_status_declared
        SpecialAccessStatus.UNDETERMINED -> R.string.special_status_undetermined
    }

@get:StringRes
val ChangeType.labelRes: Int
    get() = when (this) {
        ChangeType.APP_INSTALLED -> R.string.change_app_installed
        ChangeType.APP_REMOVED -> R.string.change_app_removed
        ChangeType.APP_UPDATED -> R.string.change_app_updated
        ChangeType.PERMISSION_REQUEST_ADDED -> R.string.change_permission_added
        ChangeType.PERMISSION_REQUEST_REMOVED -> R.string.change_permission_removed
        ChangeType.PERMISSION_GRANTED -> R.string.change_permission_granted
        ChangeType.PERMISSION_REVOKED -> R.string.change_permission_revoked
        ChangeType.SPECIAL_ACCESS_GAINED -> R.string.change_special_gained
        ChangeType.SPECIAL_ACCESS_LOST -> R.string.change_special_lost
    }

@get:StringRes
val ChangeFilter.labelRes: Int
    get() = when (this) {
        ChangeFilter.ALL -> R.string.history_filter_all
        ChangeFilter.PERMISSIONS -> R.string.history_filter_permissions
        ChangeFilter.UPDATES -> R.string.history_filter_updates
        ChangeFilter.INSTALLED -> R.string.history_filter_installed
        ChangeFilter.REMOVED -> R.string.history_filter_removed
        ChangeFilter.SPECIAL_ACCESS -> R.string.history_filter_special
    }

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@get:StringRes
val ScanInterval.labelRes: Int
    get() = when (this) {
        ScanInterval.EVERY_6_HOURS -> R.string.settings_interval_6h
        ScanInterval.DAILY -> R.string.settings_interval_daily
        ScanInterval.EVERY_3_DAYS -> R.string.settings_interval_3d
        ScanInterval.WEEKLY -> R.string.settings_interval_weekly
    }

@get:StringRes
val HistoryRetention.labelRes: Int
    get() = when (this) {
        HistoryRetention.DAYS_30 -> R.string.settings_retention_30
        HistoryRetention.DAYS_90 -> R.string.settings_retention_90
        HistoryRetention.DAYS_365 -> R.string.settings_retention_365
        HistoryRetention.FOREVER -> R.string.settings_retention_forever
    }
