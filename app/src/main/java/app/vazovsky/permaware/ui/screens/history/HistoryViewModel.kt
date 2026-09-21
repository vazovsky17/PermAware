package app.vazovsky.permaware.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.model.AppChange
import app.vazovsky.permaware.domain.model.ChangeFilter
import app.vazovsky.permaware.domain.model.groupToAppChanges
import app.vazovsky.permaware.domain.model.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryDay(val dayStart: Long, val changes: List<AppChange>)

data class HistoryUiState(
    val filter: ChangeFilter = ChangeFilter.ALL,
    val days: List<HistoryDay> = emptyList(),
    val isEmpty: Boolean = true,
    val hasAnyHistory: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(scanRepository: ScanRepository) : ViewModel() {

    private val filter = MutableStateFlow(ChangeFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        scanRepository.observeRecentChanges(HISTORY_LIMIT),
        filter,
    ) { events, filter ->
        val filtered = events.filter { it.type.matches(filter) }
        HistoryUiState(
            filter = filter,
            days = filtered.groupToAppChanges().groupByDay(),
            isEmpty = filtered.isEmpty(),
            hasAnyHistory = events.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HistoryUiState(),
    )

    fun onFilterChange(value: ChangeFilter) {
        filter.value = value
    }

    private companion object {
        const val HISTORY_LIMIT = 500
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun List<AppChange>.groupByDay(): List<HistoryDay> = groupBy { change ->
    java.util.Calendar.getInstance().apply {
        timeInMillis = change.timestamp
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}
    .map { (dayStart, changes) -> HistoryDay(dayStart, changes.sortedByDescending { it.timestamp }) }
    .sortedByDescending { it.dayStart }
