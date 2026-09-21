package app.vazovsky.permaware.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "permission_watch_prefs")

/**
 * Все мелкие пользовательские настройки.
 *
 * DataStore, а не SharedPreferences: экран настроек наблюдает за ними как за Flow, и любая запись
 * уходит с главного потока.
 */
@Singleton
class PreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    val preferences: Flow<UserPreferences> = context.dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { it.toUserPreferences() }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun setOnboardingComplete(complete: Boolean) = edit { it[Keys.ONBOARDING_COMPLETE] = complete }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setAutoScanEnabled(enabled: Boolean) = edit { it[Keys.AUTO_SCAN] = enabled }

    suspend fun setScanInterval(interval: ScanInterval) = edit { it[Keys.SCAN_INTERVAL] = interval.name }

    suspend fun setHistoryRetention(retention: HistoryRetention) = edit { it[Keys.HISTORY_RETENTION] = retention.name }

    suspend fun dismissSupportPrompt() = edit { it[Keys.SUPPORT_DISMISSED] = true }

    suspend fun incrementCompletedScans() = edit {
        it[Keys.COMPLETED_SCANS] = (it[Keys.COMPLETED_SCANS] ?: 0) + 1
    }

    suspend fun clear() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toUserPreferences() = UserPreferences(
        onboardingComplete = this[Keys.ONBOARDING_COMPLETE] ?: false,
        themeMode = enumOrDefault(this[Keys.THEME_MODE], ThemeMode.SYSTEM),
        autoScanEnabled = this[Keys.AUTO_SCAN] ?: true,
        scanInterval = enumOrDefault(this[Keys.SCAN_INTERVAL], ScanInterval.DAILY),
        historyRetention = enumOrDefault(this[Keys.HISTORY_RETENTION], HistoryRetention.DAYS_90),
        supportPromptDismissed = this[Keys.SUPPORT_DISMISSED] ?: false,
        completedScanCount = this[Keys.COMPLETED_SCANS] ?: 0,
    )

    /** Переживает значение, записанное будущей версией приложения, о котором эта ещё не знает. */
    private inline fun <reified T : Enum<T>> enumOrDefault(stored: String?, fallback: T): T =
        stored?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: fallback

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan_enabled")
        val SCAN_INTERVAL = stringPreferencesKey("scan_interval")
        val HISTORY_RETENTION = stringPreferencesKey("history_retention")
        val SUPPORT_DISMISSED = booleanPreferencesKey("support_prompt_dismissed")
        val COMPLETED_SCANS = intPreferencesKey("completed_scan_count")
    }
}
