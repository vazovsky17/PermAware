package app.vazovsky.permaware.domain.model

/** Откуда приехало приложение — насколько платформа вообще готова об этом рассказать. */
data class InstallSource(val installerPackage: String?, val installerLabel: String?)

/**
 * Одно установленное приложение, полностью разобранное. Ни одного типа из Android-фреймворка —
 * именно поэтому сканер, движок внимания и движок диффов тестируются на JVM.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val isSystem: Boolean,
    val isUpdatedSystemApp: Boolean,
    val isEnabled: Boolean,
    val installSource: InstallSource,
    val permissions: List<AppPermission>,
    val specialAccess: List<SpecialAccessState>,
    val attention: AttentionAssessment,
) {
    val sensitivePermissions: List<AppPermission>
        get() = permissions.filter { it.isSensitive }

    val grantedSensitivePermissions: List<AppPermission>
        get() = permissions.filter { it.isSensitive && it.state == PermissionState.GRANTED }

    /** Категории, доступ к которым у приложения есть прямо сейчас (выданные runtime-разрешения). */
    val grantedCategories: Set<PermissionCategory>
        get() = grantedSensitivePermissions.map { it.category }.toSet()

    /**
     * Есть ли у приложения доступ к [category] прямо сейчас — *выданный*, а не просто запрошенный.
     *
     * Единственное определение «приложения с доступом к камере»: его читают и счётчики на обзоре
     * (PrivacyOverviewCalculator.categoryCounts), и список приложений (AppFilterState). Оба смотрят
     * в одну фразу, поэтому число и список, который по нему открывается, разойтись не могут. Пока
     * это были два разных выражения, они расходились.
     */
    fun hasAccessTo(category: PermissionCategory): Boolean = category in grantedCategories

    /**
     * Отметил ли движок внимания это приложение вообще.
     *
     * Единственное определение «приложения, требующего внимания» — ровно по той же причине, по
     * которой [hasAccessTo] единственное определение «приложения с доступом к камере». «N
     * приложений требуют внимания» на обзоре и чип «Требуют внимания» на экране приложений — одна и
     * та же фраза, значит и множество приложений обязано быть одним. Оно не было: счётчик спрашивал
     * [AttentionLevel.ELEVATED], а чип — всё, что выше [AttentionLevel.LOW], и счётчик открывал
     * список длиннее обещанного числа.
     *
     * Граница проведена выше LOW, а не по ELEVATED: умеренная оценка — это всё-таки то, что движок
     * поднял не просто так, и экран, открытый по «требуют внимания» и прячущий половину из них,
     * удивил бы сильнее.
     */
    val needsAttention: Boolean get() = attention.level != AttentionLevel.LOW

    /**
     * Только то, что пользователь ставил сам. *Обновлённое* системное приложение всё равно остаётся
     * тем, которое никто не выбирал, так что засчитывать его здесь значило бы раздувать сводку по
     * устройству предустановленным вендорским софтом, который и удалить-то нельзя. Совпадает с `pm
     * list packages -3`.
     */
    val isUserApp: Boolean get() = !isSystem
}
