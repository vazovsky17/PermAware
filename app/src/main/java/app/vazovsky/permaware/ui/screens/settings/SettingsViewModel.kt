package app.vazovsky.permaware.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.db.ChangeDao
import app.vazovsky.permaware.data.db.ScanDao
import app.vazovsky.permaware.data.prefs.HistoryRetention
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.data.prefs.ScanInterval
import app.vazovsky.permaware.data.prefs.ThemeMode
import app.vazovsky.permaware.data.prefs.UserPreferences
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.export.ReportBuilder
import app.vazovsky.permaware.scan.BackgroundScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Одноразовые исходы, которые экран настроек показывает снекбаром. */
sealed interface SettingsEffect {
    data object HistoryCleared : SettingsEffect
    data object ResetDone : SettingsEffect
    data object ExportEmpty : SettingsEffect
    data class ShareReport(val subject: String, val body: String, val format: ExportFormat) : SettingsEffect
}

enum class ExportFormat { TEXT, JSON }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val scanRepository: ScanRepository,
    private val backgroundScans: BackgroundScanScheduler,
    private val scanDao: ScanDao,
    private val changeDao: ChangeDao,
) : ViewModel() {

    val preferencesState: StateFlow<UserPreferences> = preferences.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UserPreferences(),
    )

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    fun setAutoScan(enabled: Boolean) = viewModelScope.launch {
        preferences.setAutoScanEnabled(enabled)
        backgroundScans.syncWithPreferences()
    }

    fun setScanInterval(interval: ScanInterval) = viewModelScope.launch {
        preferences.setScanInterval(interval)
        backgroundScans.syncWithPreferences()
    }

    fun setHistoryRetention(retention: HistoryRetention) =
        viewModelScope.launch { preferences.setHistoryRetention(retention) }

    fun clearHistory() = viewModelScope.launch {
        changeDao.deleteAll()
        _effects.emit(SettingsEffect.HistoryCleared)
    }

    /** Сносит всё: историю, проверки, снимки (каскадом) и каждую настройку. */
    fun resetEverything() = viewModelScope.launch {
        changeDao.deleteAll()
        scanDao.deleteAll()
        preferences.clear()
        backgroundScans.cancel()
        scanRepository.clearInMemoryResults()
        _effects.emit(SettingsEffect.ResetDone)
    }

    fun export(format: ExportFormat, strings: ExportStrings) = viewModelScope.launch {
        val apps = scanRepository.apps.value
        val overview = scanRepository.overview.value
        if (apps.isEmpty() || overview == null) {
            _effects.emit(SettingsEffect.ExportEmpty)
            return@launch
        }
        val body = when (format) {
            ExportFormat.TEXT -> ReportBuilder.buildText(
                apps = apps,
                overview = overview,
                header = strings.header,
                generatedLabel = strings.generatedLabel,
                footer = strings.footer,
            )
            ExportFormat.JSON -> ReportBuilder.buildJson(apps, overview, System.currentTimeMillis())
        }
        _effects.emit(SettingsEffect.ShareReport(strings.header, body, format))
    }

    /** Локализованные строки для отчёта; их достаёт экран, чтобы ViewModel жила без Android. */
    data class ExportStrings(val header: String, val generatedLabel: String, val footer: String)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
