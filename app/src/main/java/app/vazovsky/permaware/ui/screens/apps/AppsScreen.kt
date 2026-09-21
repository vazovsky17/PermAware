package app.vazovsky.permaware.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.attention.PrivacyOverviewCalculator
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.ui.common.RelativeTime
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.AppAttentionRow
import app.vazovsky.permaware.ui.components.EmptyState
import app.vazovsky.permaware.ui.components.LocalContentBottomInset
import app.vazovsky.permaware.ui.components.PermChip
import app.vazovsky.permaware.ui.components.PermDivider
import app.vazovsky.permaware.ui.components.PermScreenHeader
import app.vazovsky.permaware.ui.components.PermSearchField
import app.vazovsky.permaware.ui.components.PermSegmentedControl
import app.vazovsky.permaware.ui.components.PermissionRowTextInset
import app.vazovsky.permaware.ui.components.ScanProgress
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

private val CategoryFilters: List<PermissionCategory> =
    PrivacyOverviewCalculator.reportableCategories
private const val PEEK_SCROLL_OFFSET_PX = -64
const val FILTER_CHIP_ROW_TAG = "apps_filter_chips"
private const val LEADING_CHIP_COUNT = 2

@Composable
fun AppsScreen(
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val spacing = PermAwareTheme.spacing
    val colors = PermAwareTheme.colors
    val bottomInset = LocalContentBottomInset.current

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(state.filters, state.sort, state.query) { listState.scrollToItem(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermScreenHeader(
                title = stringResource(R.string.apps_title),
                subtitle = pluralStringResource(
                    R.plurals.plural_apps,
                    state.apps.size,
                    state.apps.size,
                ),
                trailing = { SortMenu(current = state.sort, onSelect = viewModel::onSortChange) },
            )

            PermSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.apps_search_hint),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )

            PermSegmentedControl(
                options = AppTypeFilter.entries,
                selected = state.filters.type,
                label = { stringResource(it.labelRes) },
                onSelect = viewModel::onTypeChange,
                modifier = Modifier.padding(
                    horizontal = spacing.screenHorizontal,
                    vertical = spacing.sm,
                ),
            )

            val chipListState = rememberLazyListState()
            LaunchedEffect(state.filters.category) {
                val index = state.filters.category?.let { CategoryFilters.indexOf(it) } ?: -1
                if (index >= 0) {
                    chipListState.animateScrollToItem(index + LEADING_CHIP_COUNT, PEEK_SCROLL_OFFSET_PX)
                }
            }
            LazyRow(
                state = chipListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FILTER_CHIP_ROW_TAG)
                    .padding(bottom = spacing.sm),
                contentPadding = PaddingValues(horizontal = spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "attention") {
                    PermChip(
                        label = stringResource(R.string.apps_filter_attention),
                        selected = state.filters.needsAttention,
                        onClick = viewModel::onAttentionToggle,
                        showSelectedMark = true,
                    )
                }
                item(key = "attention_divider") {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = spacing.xxs)
                            .width(PermAwareTheme.elevation.hairline)
                            .height(spacing.xl)
                            .background(colors.outline),
                    )
                }

                items(CategoryFilters, key = { it.name }) { category ->
                    PermChip(
                        label = stringResource(category.labelRes),
                        selected = state.filters.category == category,
                        onClick = { viewModel.onCategoryToggle(category) },
                        showSelectedMark = true,
                    )
                }
            }

            when {
                state.isLoading -> ScanProgress(
                    Modifier.padding(horizontal = spacing.screenHorizontal),
                )

                state.failed -> EmptyState(
                    title = stringResource(R.string.dashboard_scan_failed),
                    body = stringResource(R.string.dashboard_scan_failed_body),
                    actionLabel = stringResource(R.string.action_retry),
                    onActionClick = viewModel::retry,
                )

                state.apps.isEmpty() -> EmptyState(
                    title = stringResource(R.string.apps_empty_title),
                    body = when {
                        state.query.isNotBlank() -> stringResource(R.string.apps_empty_search, state.query)
                        state.isFiltered -> stringResource(R.string.apps_empty_filters)
                        else -> stringResource(R.string.apps_empty_filter)
                    },
                    icon = Icons.Outlined.Search,
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.screenHorizontal)
                        .clip(SheetShape)
                        .background(colors.surface),
                    contentPadding = PaddingValues(bottom = bottomInset + spacing.md),
                ) {
                    itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                        if (index > 0) PermDivider(Modifier.padding(start = PermissionRowTextInset))
                        AppAttentionRow(
                            app = app,
                            subtitle = stringResource(
                                R.string.apps_updated_ago,
                                RelativeTime.ago(context, app.lastUpdateTime),
                            ),
                            onClick = { onOpenApp(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

private val SheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

@Composable
private fun SortMenu(current: AppSort, onSelect: (AppSort) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.SwapVert,
                contentDescription = stringResource(R.string.apps_sort),
                tint = PermAwareTheme.colors.textSecondary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes), style = PermAwareType.body) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                    trailingIcon = {
                        if (sort == current) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PermAwareTheme.colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

@get:androidx.annotation.StringRes
private val AppTypeFilter.labelRes: Int
    get() = when (this) {
        AppTypeFilter.ALL -> R.string.apps_filter_all
        AppTypeFilter.USER -> R.string.apps_filter_user
        AppTypeFilter.SYSTEM -> R.string.apps_filter_system
    }

@get:androidx.annotation.StringRes
private val AppSort.labelRes: Int
    get() = when (this) {
        AppSort.ATTENTION -> R.string.apps_sort_attention
        AppSort.NAME -> R.string.apps_sort_name
        AppSort.RECENTLY_UPDATED -> R.string.apps_sort_recently_updated
        AppSort.MOST_SENSITIVE -> R.string.apps_sort_most_permissions
    }
