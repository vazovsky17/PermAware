package app.vazovsky.permaware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.data.prefs.ThemeMode
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.model.ScanTrigger
import app.vazovsky.permaware.scan.BackgroundScanScheduler
import app.vazovsky.permaware.scan.PackageChangeObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val loaded: Boolean = false,
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: PreferencesRepository,
    private val scanRepository: ScanRepository,
    private val packageChanges: PackageChangeObserver,
    private val backgroundScans: BackgroundScanScheduler,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferences.preferences
        .map {
            MainUiState(
                loaded = true,
                onboardingComplete = it.onboardingComplete,
                themeMode = it.themeMode,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState(),
        )

    init {
        viewModelScope.launch { backgroundScans.syncWithPreferences() }

        // Пока приложение на переднем плане, установка, обновление или удаление запускают повторную
        // проверку, чтобы список, на который человек смотрит, не устарел втихую. С дребезгом:
        // установка приложения выдаёт несколько широковещаний подряд.
        viewModelScope.launch {
            packageChanges.events
                .debounce(PACKAGE_EVENT_DEBOUNCE_MS)
                .collect { scanRepository.scan(ScanTrigger.PACKAGE_EVENT) }
        }
    }

    fun onForeground() = packageChanges.start()

    fun onBackground() = packageChanges.stop()

    private companion object {
        const val PACKAGE_EVENT_DEBOUNCE_MS = 1_500L
    }
}
