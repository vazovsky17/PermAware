package app.vazovsky.permaware.ui.screens.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.data.repository.ScanStatus
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.domain.model.ScanTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val query: String = "",
    val filters: AppFilterState = AppFilterState(),
    val sort: AppSort = AppSort.ATTENTION,
    val apps: List<InstalledApp> = emptyList(),
    val totalCount: Int = 0,
) {
    val isFiltered: Boolean
        get() = filters.isNarrowed || query.isNotBlank()
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(
        AppFilterState(
            type = AppTypeFilter.USER,
            category = savedStateHandle.get<String>(ARG_CATEGORY)
                ?.let { name -> runCatching { PermissionCategory.valueOf(name) }.getOrNull() },
            needsAttention = savedStateHandle.get<Boolean>(ARG_ATTENTION) == true,
        ),
    )

    private val sort = MutableStateFlow(AppSort.ATTENTION)

    val uiState: StateFlow<AppsUiState> = combine(
        scanRepository.apps,
        scanRepository.status,
        query,
        filters,
        sort,
    ) { apps, status, query, filters, sort ->
        AppsUiState(
            isLoading = status is ScanStatus.Running && apps.isEmpty(),
            failed = status is ScanStatus.Failed && apps.isEmpty(),
            query = query,
            filters = filters,
            sort = sort,
            apps = AppListBuilder.build(apps, query, filters, sort),
            totalCount = apps.size,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppsUiState(isLoading = true),
    )

    fun loadIfNeeded() {
        viewModelScope.launch { scanRepository.ensureResults(ScanTrigger.MANUAL) }
    }

    fun retry() {
        viewModelScope.launch { scanRepository.scan(ScanTrigger.MANUAL) }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onTypeChange(value: AppTypeFilter) {
        filters.value = filters.value.copy(type = value)
    }

    fun onCategoryToggle(value: PermissionCategory) {
        filters.value = filters.value.toggleCategory(value)
    }

    fun onAttentionToggle() {
        filters.value = filters.value.copy(needsAttention = !filters.value.needsAttention)
    }

    fun onSortChange(value: AppSort) {
        sort.value = value
    }

    companion object {
        const val ARG_CATEGORY = "category"
        const val ARG_ATTENTION = "attention"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
