package app.vazovsky.permaware.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.ui.components.LocalContentBottomInset
import app.vazovsky.permaware.ui.components.PermBottomBar
import app.vazovsky.permaware.ui.components.PermBottomBarItem
import app.vazovsky.permaware.ui.components.PermBottomFade
import app.vazovsky.permaware.ui.screens.about.AboutScreen
import app.vazovsky.permaware.ui.screens.about.LicensesScreen
import app.vazovsky.permaware.ui.screens.about.PrivacyScreen
import app.vazovsky.permaware.ui.screens.appdetail.AppDetailScreen
import app.vazovsky.permaware.ui.screens.appdetail.AppDetailViewModel
import app.vazovsky.permaware.ui.screens.apps.AppsScreen
import app.vazovsky.permaware.ui.screens.apps.AppsViewModel
import app.vazovsky.permaware.ui.screens.dashboard.DashboardScreen
import app.vazovsky.permaware.ui.screens.history.HistoryScreen
import app.vazovsky.permaware.ui.screens.onboarding.OnboardingScreen
import app.vazovsky.permaware.ui.screens.settings.SettingsScreen
import app.vazovsky.permaware.ui.theme.PermAwareTheme

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val APPS = "apps"
    const val APPS_FILTERED = "apps?category={category}&attention={attention}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app/{packageName}"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"

    fun apps(category: PermissionCategory? = null, needsAttention: Boolean = false): String {
        val arguments = buildList {
            if (category != null) add("category=${category.name}")
            if (needsAttention) add("attention=true")
        }
        return if (arguments.isEmpty()) APPS else APPS + "?" + arguments.joinToString("&")
    }

    fun appDetail(packageName: String): String = "app/$packageName"
}

private enum class TopLevelDestination(
    val graphRoute: String,
    val tabRoute: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, Routes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Shield),
    APPS(Routes.APPS_FILTERED, Routes.APPS, R.string.nav_apps, Icons.Outlined.Apps),
    HISTORY(Routes.HISTORY, Routes.HISTORY, R.string.nav_history, Icons.Outlined.History),
    SETTINGS(Routes.SETTINGS, Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
    ;

    fun isCurrent(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.route == graphRoute } == true
}

@Composable
fun PermAwareNavigation(
    onboardingComplete: Boolean,
    onSupportClick: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenContact: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { it.isCurrent(currentDestination) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PermAwareTheme.colors.background,
        bottomBar = {
            if (showBottomBar) {
                PermBottomBar {
                    TopLevelDestination.entries.forEach { destination ->
                        PermBottomBarItem(
                            label = stringResource(destination.labelRes),
                            icon = destination.icon,
                            selected = destination.isCurrent(currentDestination),
                            onClick = { navController.navigateToTopLevel(destination.tabRoute) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalContentBottomInset provides padding.calculateBottomPadding()) {
                NavHost(
                    navController = navController,
                    startDestination = if (onboardingComplete) Routes.DASHBOARD else Routes.ONBOARDING,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) },
                    popExitTransition = { slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(220)) },
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            onFinished = {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            onOpenApps = { category ->
                                navController.navigateToTopLevel(Routes.apps(category))
                            },
                            onOpenAttention = {
                                navController.navigateToTopLevel(Routes.apps(needsAttention = true))
                            },
                            onOpenHistory = { navController.navigateToTopLevel(Routes.HISTORY) },
                            onOpenApp = { navController.navigate(Routes.appDetail(it)) },
                            onSupportClick = onSupportClick,
                            modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        )
                    }

                    composable(
                        route = Routes.APPS_FILTERED,
                        arguments = listOf(
                            navArgument(AppsViewModel.ARG_CATEGORY) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument(AppsViewModel.ARG_ATTENTION) {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                    ) {
                        AppsScreen(
                            onOpenApp = { navController.navigate(Routes.appDetail(it)) },
                            modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        )
                    }

                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            onOpenApp = { navController.navigate(Routes.appDetail(it)) },
                            modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onOpenAbout = { navController.navigate(Routes.ABOUT) },
                            onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                            onOpenLicenses = { navController.navigate(Routes.LICENSES) },
                            onSupportClick = onSupportClick,
                            modifier = Modifier.padding(top = padding.calculateTopPadding()),
                        )
                    }

                    composable(
                        route = Routes.APP_DETAIL,
                        arguments = listOf(
                            navArgument(AppDetailViewModel.ARG_PACKAGE_NAME) { type = NavType.StringType },
                        ),
                    ) {
                        AppDetailScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Routes.ABOUT) {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                            onOpenSource = onOpenSource,
                            onOpenContact = onOpenContact,
                            onSupportClick = onSupportClick,
                        )
                    }

                    composable(Routes.PRIVACY) {
                        PrivacyScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Routes.LICENSES) {
                        LicensesScreen(onBack = { navController.popBackStack() })
                    }
                }
            }

            if (showBottomBar) {
                PermBottomFade(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    barHeight = padding.calculateBottomPadding(),
                )
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) {
        // Не inclusive, чтобы обзор остался на дне стека и «назад» уводил из приложения именно с
        // него. Без saveState — см. выше.
        popUpTo(graph.findStartDestination().id) { saveState = false }
        // Вкладка обзора, нажатая на самом обзоре, переиспользует ту запись, которую уже
        // показывает, вместо того чтобы класть сверху вторую копию.
        launchSingleTop = true
        restoreState = false
    }
}
