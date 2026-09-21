package app.vazovsky.permaware.data.prefs

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Как часто проверять в фоне. WorkManager держит нижнюю границу в 15 минут для периодической
 * работы, а Doze способен задержать любую из этих цифр как следует, поэтому интерфейс никогда не
 * обещает точного времени.
 */
enum class ScanInterval(val hours: Long) {
    EVERY_6_HOURS(6),
    DAILY(24),
    EVERY_3_DAYS(72),
    WEEKLY(168),
}

enum class HistoryRetention(val days: Int) {
    DAYS_30(30),
    DAYS_90(90),
    DAYS_365(365),
    FOREVER(Int.MAX_VALUE),
}

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoScanEnabled: Boolean = true,
    val scanInterval: ScanInterval = ScanInterval.DAILY,
    val historyRetention: HistoryRetention = HistoryRetention.DAYS_90,
    val supportPromptDismissed: Boolean = false,
    /** Нужно, чтобы понять, когда карточка поддержки будет показываться. */
    val completedScanCount: Int = 0,
) {
    /**
     * Предложение поддержать появляется, когда человек уже получил от приложения пользу
     * (просканировал свои приложения), и больше не появляется никогда после того, как его скрыли.
     * Только на экране настроек.
     */
    val shouldOfferSupport: Boolean
        get() = !supportPromptDismissed && completedScanCount >= SCANS_BEFORE_SUPPORT_PROMPT

    companion object {
        const val SCANS_BEFORE_SUPPORT_PROMPT = 3
    }
}
