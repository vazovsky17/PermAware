package app.vazovsky.permaware.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.prefs.PreferencesRepository
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.data.repository.ScanStatus
import app.vazovsky.permaware.domain.attention.PrivacyOverview
import app.vazovsky.permaware.domain.model.AppChange
import app.vazovsky.permaware.domain.model.Scan
import app.vazovsky.permaware.domain.model.ScanTrigger
import app.vazovsky.permaware.domain.model.groupToAppChanges
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isScanning: Boolean = false,
    val hasResults: Boolean = false,
    val overview: PrivacyOverview? = null,
    val recentChanges: List<AppChange> = emptyList(),
    val lastScan: Scan? = null,
    val scanFailed: Boolean = false,
    val showSupportCard: Boolean = false,
    val isFirstScan: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        scanRepository.status,
        scanRepository.overview,
        scanRepository.observeRecentChanges(RECENT_CHANGES_SHOWN),
        scanRepository.latestScan,
        preferences.preferences,
    ) { status, overview, changes, lastScan, prefs ->
        DashboardUiState(
            isScanning = status is ScanStatus.Running,
            hasResults = overview != null,
            overview = overview,
            recentChanges = changes.groupToAppChanges().take(RECENT_APPS_SHOWN),
            lastScan = lastScan,
            scanFailed = status is ScanStatus.Failed,
            showSupportCard = prefs.shouldOfferSupport,
            isFirstScan = prefs.completedScanCount <= 1,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DashboardUiState(),
    )

    /** Проверяет один раз за процесс, когда обзор впервые показался, дальше — только по просьбе. */
    fun loadIfNeeded() {
        viewModelScope.launch { scanRepository.ensureResults(ScanTrigger.MANUAL) }
    }

    fun scanNow() {
        viewModelScope.launch { scanRepository.scan(ScanTrigger.MANUAL) }
    }

    fun dismissSupportCard() {
        viewModelScope.launch { preferences.dismissSupportPrompt() }
    }

    private companion object {
        const val RECENT_CHANGES_SHOWN = 40
        const val RECENT_APPS_SHOWN = 4
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
