package app.vazovsky.permaware.domain.model

/** Завершённая проверка. */
data class Scan(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val appCount: Int,
    val userAppCount: Int,
    val changeCount: Int,
    val privacyScore: Int,
    val trigger: ScanTrigger,
)

enum class ScanTrigger {
    MANUAL,
    ONBOARDING,
    BACKGROUND,
    PACKAGE_EVENT,
}

/** Компактная запись об одном разрешении внутри снимка. */
data class PermissionSnapshot(val name: String, val state: PermissionState)

/**
 * Компактная запись об одном приложении на момент проверки. Снимки — это то, на чём держится
 * "что изменилось с прошлого раза"; иконок и описаний в них нет намеренно.
 */
data class AppSnapshot(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val lastUpdateTime: Long,
    val permissions: List<PermissionSnapshot>,
    val specialAccess: List<SpecialAccessState>,
)
