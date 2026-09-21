package app.vazovsky.permaware.data.platform

import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.PermissionProtection
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.domain.model.SpecialAccessState
import app.vazovsky.permaware.domain.model.SpecialAccessStatus
import app.vazovsky.permaware.domain.model.SpecialAccessType

/**
 * Разрешения, фактический доступ по которым решает запись в AppOps, а не бит выдачи, — и особый
 * доступ, которому каждое из них соответствует.
 *
 * Общий для [PlatformSpecialAccessInspector] и для сверки разрешений ниже, чтобы эти двое никогда
 * не разошлись в том, какие разрешения закрыты AppOps.
 */
object AppOpPermissionMapping {

    val PERMISSION_TO_SPECIAL_ACCESS: Map<String, SpecialAccessType> = mapOf(
        android.Manifest.permission.SYSTEM_ALERT_WINDOW to SpecialAccessType.DISPLAY_OVER_OTHER_APPS,
        android.Manifest.permission.REQUEST_INSTALL_PACKAGES to SpecialAccessType.INSTALL_UNKNOWN_APPS,
        android.Manifest.permission.PACKAGE_USAGE_STATS to SpecialAccessType.USAGE_ACCESS,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE to SpecialAccessType.ALL_FILES_ACCESS,
        android.Manifest.permission.WRITE_SETTINGS to SpecialAccessType.WRITE_SYSTEM_SETTINGS,
        android.Manifest.permission.SCHEDULE_EXACT_ALARM to SpecialAccessType.EXACT_ALARM,
    )

    /**
     * Переносит *проверенный* результат AppOps обратно в соответствующую строку разрешения.
     *
     * Переносятся только [SpecialAccessStatus.ACTIVE] и [SpecialAccessStatus.INACTIVE] — это те два
     * состояния, которые мы действительно установили. Возможность, которая всего лишь
     * [SpecialAccessStatus.DECLARED], оставляет разрешение в [PermissionState.UNDETERMINED].
     */
    fun reconcile(permissions: List<AppPermission>, specialAccess: List<SpecialAccessState>): List<AppPermission> {
        if (permissions.none { it.protection == PermissionProtection.APPOP_GATED }) return permissions
        val statusByType = specialAccess.associate { it.type to it.status }
        return permissions.map { permission ->
            if (permission.protection != PermissionProtection.APPOP_GATED) return@map permission
            val type = PERMISSION_TO_SPECIAL_ACCESS[permission.name] ?: return@map permission
            when (statusByType[type]) {
                SpecialAccessStatus.ACTIVE -> permission.copy(state = PermissionState.GRANTED)
                SpecialAccessStatus.INACTIVE -> permission.copy(state = PermissionState.DENIED)
                else -> permission
            }
        }
    }
}
