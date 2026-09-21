package app.vazovsky.permaware.ui.screens.apps

import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory

enum class AppTypeFilter {
    ALL,
    USER,
    SYSTEM,
    ;

    fun matches(app: InstalledApp): Boolean = when (this) {
        ALL -> true
        USER -> app.isUserApp
        SYSTEM -> app.isSystem
    }
}

data class AppFilterState(
    /**
     * По умолчанию — собственные приложения пользователя: предустановленных пакетов на устройстве
     * сотни.
     */
    val type: AppTypeFilter = AppTypeFilter.USER,
    val category: PermissionCategory? = null,
    val needsAttention: Boolean = false,
) {
    val isNarrowed: Boolean
        get() = type != AppTypeFilter.ALL || category != null || needsAttention

    fun matches(app: InstalledApp): Boolean = type.matches(app) && matchesCategory(app) && matchesAttention(app)
    private fun matchesCategory(app: InstalledApp): Boolean = category == null || app.hasAccessTo(category)
    private fun matchesAttention(app: InstalledApp): Boolean = !needsAttention || app.needsAttention
    fun toggleCategory(value: PermissionCategory): AppFilterState =
        copy(category = if (category == value) null else value)
}

enum class AppSort { ATTENTION, NAME, RECENTLY_UPDATED, MOST_SENSITIVE }

object AppListBuilder {

    fun build(
        apps: List<InstalledApp>,
        query: String = "",
        filters: AppFilterState = AppFilterState(),
        sort: AppSort = AppSort.ATTENTION,
    ): List<InstalledApp> = apps
        .asSequence()
        .filter { filters.matches(it) }
        .filter { it.matches(query) }
        .sortedWith(sort.comparator())
        .toList()

    private fun InstalledApp.matches(query: String): Boolean {
        if (query.isBlank()) return true
        val needle = query.trim()
        return label.contains(needle, ignoreCase = true) ||
            packageName.contains(needle, ignoreCase = true)
    }

    private fun AppSort.comparator(): Comparator<InstalledApp> = when (this) {
        AppSort.ATTENTION -> compareByDescending<InstalledApp> { it.attention.score }
            .thenBy { it.label.lowercase() }
        AppSort.NAME -> compareBy { it.label.lowercase() }
        AppSort.RECENTLY_UPDATED -> compareByDescending<InstalledApp> { it.lastUpdateTime }
            .thenBy { it.label.lowercase() }
        AppSort.MOST_SENSITIVE -> compareByDescending<InstalledApp> { it.grantedSensitivePermissions.size }
            .thenBy { it.label.lowercase() }
    }
}
