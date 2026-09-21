package app.vazovsky.permaware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.vazovsky.permaware.R
import app.vazovsky.permaware.core.config.AppLinks
import app.vazovsky.permaware.data.platform.AppIconLoader
import app.vazovsky.permaware.ui.common.IntentLaunchers
import app.vazovsky.permaware.ui.components.LocalAppIconLoader
import app.vazovsky.permaware.ui.navigation.PermAwareNavigation
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appIconLoader: AppIconLoader

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !viewModel.uiState.value.loaded }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onForeground()
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    viewModel.onBackground()
                }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val browserMissing = stringResource(R.string.support_browser_missing)

            PermAwareTheme(themeMode = state.themeMode) {
                CompositionLocalProvider(LocalAppIconLoader provides appIconLoader) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        if (state.loaded) {
                            PermAwareNavigation(
                                onboardingComplete = state.onboardingComplete,
                                onSupportClick = {
                                    val url = AppLinks.BOOSTY_URL
                                    val opened =
                                        url != null && IntentLaunchers.openUrl(context, url)
                                    if (!opened) {
                                        scope.launch { snackbarHostState.showSnackbar(browserMissing) }
                                    }
                                },
                                onOpenContact = {
                                    val url = AppLinks.CONTACT_URL
                                    val opened =
                                        url != null && IntentLaunchers.openUrl(context, url)
                                    if (!opened) {
                                        scope.launch { snackbarHostState.showSnackbar(browserMissing) }
                                    }
                                },
                                onOpenSource = {
                                    val url = AppLinks.GITHUB_URL
                                    val opened =
                                        url != null && IntentLaunchers.openUrl(context, url)
                                    if (!opened) {
                                        scope.launch { snackbarHostState.showSnackbar(browserMissing) }
                                    }
                                },
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            SnackbarHost(snackbarHostState)
                        }
                    }
                }
            }
        }
    }
}
