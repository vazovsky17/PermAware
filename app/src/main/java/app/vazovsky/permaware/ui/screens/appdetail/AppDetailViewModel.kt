package app.vazovsky.permaware.ui.screens.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazovsky.permaware.data.platform.InstalledAppsDataSource
import app.vazovsky.permaware.data.repository.ScanRepository
import app.vazovsky.permaware.domain.attention.AttentionEngine
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppDetailState {
    data object Loading : AppDetailState
    data object NotFound : AppDetailState
    data class Content(
        val app: InstalledApp,
        val sensitivePermissions: List<AppPermission>,
        val otherPermissions: List<AppPermission>,
        val history: List<ChangeEvent>,
    ) : AppDetailState
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scanRepository: ScanRepository,
    private val installedApps: InstalledAppsDataSource,
) : ViewModel() {

    val packageName: String = checkNotNull(savedStateHandle[ARG_PACKAGE_NAME])

    /**
     * Заполняется из результатов проверки в памяти, когда они есть. После смерти процесса — или
     * когда на экран попали раньше любой проверки — пакет читается напрямую, так что экран
     * приложения работает сам по себе, а не вынуждает к полной перепроверке.
     */
    private val loadedApp = MutableStateFlow<InstalledApp?>(null)
    private val notFound = MutableStateFlow(false)

    val state: StateFlow<AppDetailState> = combine(
        scanRepository.apps,
        loadedApp,
        notFound,
        scanRepository.observeChangesForApp(packageName),
    ) { apps, loaded, missing, history ->
        val app = apps.firstOrNull { it.packageName == packageName } ?: loaded
        when {
            app != null -> AppDetailState.Content(
                app = app,
                sensitivePermissions = app.permissions
                    .filter { it.isSensitive }
                    .sortedWith(
                        compareBy<AppPermission> { it.state.displayOrder }
                            .thenBy { it.category.ordinal }
                            .thenBy { it.name },
                    ),
                otherPermissions = app.permissions.filterNot { it.isSensitive }.sortedBy { it.name },
                history = history,
            )
            missing -> AppDetailState.NotFound
            else -> AppDetailState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppDetailState.Loading,
    )

    init {
        viewModelScope.launch {
            if (scanRepository.appOf(packageName) == null) {
                val app = installedApps.loadApp(packageName)
                if (app == null) {
                    notFound.value = true
                } else {
                    loadedApp.value = app.copy(
                        attention = AttentionEngine.assess(
                            permissions = app.permissions,
                            specialAccess = app.specialAccess,
                            isSystemApp = app.isSystem,
                            hasKnownInstallSource = app.installSource.installerPackage != null,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_PACKAGE_NAME = "packageName"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}

private val PermissionState.displayOrder: Int
    get() = when (this) {
        PermissionState.GRANTED -> 0
        PermissionState.UNDETERMINED -> 1
        PermissionState.DENIED -> 2
    }
