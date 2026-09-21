package app.vazovsky.permaware.domain.model

enum class ChangeType {
    APP_INSTALLED,
    APP_REMOVED,
    APP_UPDATED,
    PERMISSION_REQUEST_ADDED,
    PERMISSION_REQUEST_REMOVED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    SPECIAL_ACCESS_GAINED,
    SPECIAL_ACCESS_LOST,
}

/** Крупная группировка для чипов-фильтров на экране изменений. */
enum class ChangeFilter {
    ALL,
    PERMISSIONS,
    UPDATES,
    INSTALLED,
    REMOVED,
    SPECIAL_ACCESS,
}

fun ChangeType.matches(filter: ChangeFilter): Boolean = when (filter) {
    ChangeFilter.ALL -> true
    ChangeFilter.PERMISSIONS -> this in setOf(
        ChangeType.PERMISSION_REQUEST_ADDED,
        ChangeType.PERMISSION_REQUEST_REMOVED,
        ChangeType.PERMISSION_GRANTED,
        ChangeType.PERMISSION_REVOKED,
    )
    ChangeFilter.UPDATES -> this == ChangeType.APP_UPDATED
    ChangeFilter.INSTALLED -> this == ChangeType.APP_INSTALLED
    ChangeFilter.REMOVED -> this == ChangeType.APP_REMOVED
    ChangeFilter.SPECIAL_ACCESS -> this in setOf(
        ChangeType.SPECIAL_ACCESS_GAINED,
        ChangeType.SPECIAL_ACCESS_LOST,
    )
}

/** Одно отличие между двумя соседними снимками одного и того же приложения. */
data class ChangeEvent(
    val id: Long = 0L,
    val scanId: Long = 0L,
    val timestamp: Long,
    val packageName: String,
    val appLabel: String,
    val type: ChangeType,
    /** Константа разрешения — для изменений разрешений, в остальных случаях null. */
    val permission: String? = null,
    val category: PermissionCategory? = null,
    val specialAccess: SpecialAccessType? = null,
    /** Прежнее имя версии, для APP_UPDATED. */
    val previousVersion: String? = null,
    /** Новое имя версии, для APP_UPDATED. */
    val newVersion: String? = null,
)

/** Все изменения по одному приложению за одну проверку — для сгруппированного списка. */
data class AppChange(
    val packageName: String,
    val appLabel: String,
    val timestamp: Long,
    val events: List<ChangeEvent>,
) {
    val isRemoval: Boolean get() = events.any { it.type == ChangeType.APP_REMOVED }

    val displayLabel: String?
        get() = appLabel.takeIf { it.isNotBlank() && it != packageName }
}

/**
 * Собирает события в одну карточку на приложение **за одну проверку**.
 */
fun List<ChangeEvent>.groupToAppChanges(): List<AppChange> = groupBy { it.packageName to it.timestamp }
    .map { (key, events) ->
        AppChange(
            packageName = key.first,
            appLabel = events.first().appLabel,
            timestamp = key.second,
            events = events.sortedBy { it.type.ordinal },
        )
    }
    .sortedWith(compareByDescending<AppChange> { it.timestamp }.thenBy { it.appLabel.lowercase() })
