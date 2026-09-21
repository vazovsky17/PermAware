package app.vazovsky.permaware.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.attention.PrivacyOverviewCalculator
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.ui.common.RelativeTime
import app.vazovsky.permaware.ui.components.ChangeEventCard
import app.vazovsky.permaware.ui.components.EmptyState
import app.vazovsky.permaware.ui.components.LocalContentBottomInset
import app.vazovsky.permaware.ui.components.PermScreenHeader
import app.vazovsky.permaware.ui.components.PermissionCategoryCard
import app.vazovsky.permaware.ui.components.PrivacyOverviewCard
import app.vazovsky.permaware.ui.components.SectionHeader
import app.vazovsky.permaware.ui.components.SupportDeveloperCard
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun DashboardScreen(
    onOpenApps: (PermissionCategory?) -> Unit,
    /** Экран приложений с уже применённым фильтром внимания — см. [PrivacyOverviewCard]. */
    onOpenAttention: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApp: (String) -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showScoreExplanation by remember { mutableStateOf(false) }
    val spacing = PermAwareTheme.spacing
    val bottomInset = LocalContentBottomInset.current

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    val headerSubtitle = state.lastScan?.finishedAt
        ?.let { stringResource(R.string.dashboard_last_scan, RelativeTime.dateTime(context, it)) }
        ?: stringResource(R.string.dashboard_header_never)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PermAwareTheme.colors.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DASHBOARD_FEED_TAG),
            contentPadding = PaddingValues(bottom = bottomInset + spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            item(key = "header") {
                PermScreenHeader(
                    title = stringResource(R.string.app_name),
                    subtitle = headerSubtitle,
                )
            }

            item(key = "overview") {
                val overview = state.overview
                Box(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
                    if (state.scanFailed && overview == null) {
                        EmptyState(
                            title = stringResource(R.string.dashboard_scan_failed),
                            body = stringResource(R.string.dashboard_scan_failed_body),
                            actionLabel = stringResource(R.string.action_retry),
                            onActionClick = viewModel::scanNow,
                        )
                    } else {
                        PrivacyOverviewCard(
                            score = overview?.score ?: 0,
                            appsNeedingAttention = overview?.appsNeedingAttention ?: 0,
                            isScanning = state.isScanning,
                            hasResults = overview != null,
                            onScanClick = viewModel::scanNow,
                            onAttentionClick = onOpenAttention,
                            onOpenApps = { onOpenApps(null) },
                            onWhyClick = { showScoreExplanation = true },
                        )
                    }
                }
            }

            val counts = state.overview?.categoryCounts.orEmpty()
            if (counts.isNotEmpty()) {
                item(key = "categories_header") {
                    SectionHeader(
                        title = stringResource(R.string.dashboard_sensitive_permissions),
                        subtitle = stringResource(R.string.dashboard_sensitive_subtitle),
                        actionLabel = stringResource(R.string.dashboard_all_apps),
                        onActionClick = { onOpenApps(null) },
                        modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                    )
                }

                val ordered = PrivacyOverviewCalculator.reportableCategories
                    .filter { counts.containsKey(it) }
                items(ordered.chunked(2), key = { row -> "cat_" + row.joinToString { it.name } }) { row ->
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(spacing.cardGap),
                    ) {
                        row.forEach { category ->
                            PermissionCategoryCard(
                                category = category,
                                appCount = counts[category] ?: 0,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenApps(category) },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            item(key = "changes_header") {
                SectionHeader(
                    title = stringResource(R.string.dashboard_recent_changes),
                    actionLabel = if (state.recentChanges.isNotEmpty()) {
                        stringResource(R.string.dashboard_all_changes)
                    } else {
                        null
                    },
                    onActionClick = onOpenHistory,
                    modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                )
            }

            if (state.recentChanges.isEmpty()) {
                item(key = "changes_empty") {
                    Text(
                        text = if (state.isFirstScan) {
                            stringResource(R.string.dashboard_no_changes_first)
                        } else {
                            stringResource(R.string.dashboard_no_changes)
                        },
                        style = PermAwareType.body,
                        color = PermAwareTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                    )
                }
            } else {
                items(state.recentChanges, key = { "change_" + it.packageName + it.timestamp }) { change ->
                    ChangeEventCard(
                        change = change,
                        timeLabel = RelativeTime.ago(context, change.timestamp),
                        modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                        onClick = { onOpenApp(change.packageName) },
                    )
                }
            }

            if (state.showSupportCard) {
                item(key = "support") {
                    SupportDeveloperCard(
                        modifier = Modifier.padding(
                            top = spacing.lg,
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                        ),
                        dismissible = true,
                        onSupportClick = onSupportClick,
                        onDismiss = viewModel::dismissSupportCard,
                    )
                }
            }
        }
    }

    if (showScoreExplanation) {
        state.overview?.let { overview ->
            ScoreExplanationSheet(overview = overview, onDismiss = { showScoreExplanation = false })
        }
    }
}
const val DASHBOARD_FEED_TAG = "dashboard_feed"
