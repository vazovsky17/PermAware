package app.vazovsky.permaware.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.SpecialAccessType

/** По иконке на категорию, чтобы одно и то же понятие всюду выглядело одинаково. */
val PermissionCategory.icon: ImageVector
    get() = when (this) {
        PermissionCategory.LOCATION -> Icons.Outlined.LocationOn
        PermissionCategory.CAMERA -> Icons.Outlined.CameraAlt
        PermissionCategory.MICROPHONE -> Icons.Outlined.Mic
        PermissionCategory.CONTACTS -> Icons.Filled.Contacts
        PermissionCategory.CALENDAR -> Icons.Outlined.CalendarMonth
        PermissionCategory.SMS -> Icons.Filled.Sms
        PermissionCategory.PHONE -> Icons.Outlined.PhoneAndroid
        PermissionCategory.CALL_LOG -> Icons.Outlined.Call
        PermissionCategory.STORAGE -> Icons.Outlined.Folder
        PermissionCategory.SENSORS -> Icons.Outlined.MonitorHeart
        PermissionCategory.ACTIVITY -> Icons.Outlined.DirectionsRun
        PermissionCategory.NEARBY -> Icons.Outlined.Bluetooth
        PermissionCategory.NOTIFICATIONS -> Icons.Outlined.Notifications
        PermissionCategory.ACCOUNTS -> Icons.Outlined.AccountCircle
        PermissionCategory.SPECIAL_ACCESS -> Icons.Outlined.Key
        PermissionCategory.SYSTEM -> Icons.Outlined.Settings
        PermissionCategory.OTHER -> Icons.Outlined.Apps
    }

val SpecialAccessType.icon: ImageVector
    get() = when (this) {
        SpecialAccessType.ACCESSIBILITY -> Icons.Outlined.Accessibility
        SpecialAccessType.NOTIFICATION_LISTENER -> Icons.Outlined.Notifications
        SpecialAccessType.DEVICE_ADMIN -> Icons.Outlined.Shield
        SpecialAccessType.BATTERY_UNRESTRICTED -> Icons.Outlined.BatteryChargingFull
        SpecialAccessType.VPN_SERVICE -> Icons.Outlined.VpnKey
        SpecialAccessType.DISPLAY_OVER_OTHER_APPS -> Icons.Outlined.Layers
        SpecialAccessType.INSTALL_UNKNOWN_APPS -> Icons.Outlined.InstallMobile
        SpecialAccessType.USAGE_ACCESS -> Icons.Outlined.History
        SpecialAccessType.EXACT_ALARM -> Icons.Outlined.Alarm
        SpecialAccessType.ALL_FILES_ACCESS -> Icons.Outlined.Folder
        SpecialAccessType.WRITE_SYSTEM_SETTINGS -> Icons.Outlined.Settings
        SpecialAccessType.PICTURE_IN_PICTURE -> Icons.Outlined.PictureInPicture
    }

internal val SecurityIcon: ImageVector get() = Icons.Outlined.Security
internal val TimerIcon: ImageVector get() = Icons.Outlined.Timer
