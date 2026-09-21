package app.vazovsky.permaware.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.model.ScanTrigger
import app.vazovsky.permaware.scan.BackgroundScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isScanning: Boolean = false,
    val finished: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val scanRepository: ScanRepository,
    private val backgroundScans: BackgroundScanScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /**
     * Первая проверка запускается здесь — после экрана с уведомлением и только по нажатию человека.
     * Именно это делает заметное уведомление осмысленным, а не декоративным.
     */
    fun startFirstScan() {
        if (_state.value.isScanning) return
        viewModelScope.launch {
            _state.value = OnboardingUiState(isScanning = true)
            val result = scanRepository.scan(ScanTrigger.ONBOARDING)
            if (result.isSuccess) {
                preferences.setOnboardingComplete(true)
                backgroundScans.syncWithPreferences()
                _state.value = OnboardingUiState(finished = true)
            } else {
                // Неудавшаяся первая проверка не должна запереть человека на экране знакомства
                // навсегда.
                _state.value = OnboardingUiState(failed = true)
            }
        }
    }

    /** Пускает внутрь, даже если первая проверка провалилась: обзор сможет повторить. */
    fun skipToApp() {
        viewModelScope.launch {
            preferences.setOnboardingComplete(true)
            backgroundScans.syncWithPreferences()
            _state.value = OnboardingUiState(finished = true)
        }
    }
}
