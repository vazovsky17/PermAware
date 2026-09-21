package app.vazovsky.permaware.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.ChangeFilter
import app.vazovsky.permaware.ui.common.RelativeTime
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.ChangeEventCard
import app.vazovsky.permaware.ui.components.EmptyState
import app.vazovsky.permaware.ui.components.LocalContentBottomInset
import app.vazovsky.permaware.ui.components.PermChip
import app.vazovsky.permaware.ui.components.PermScreenHeader
import app.vazovsky.permaware.ui.components.PermSectionLabel
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun HistoryScreen(
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = PermAwareTheme.spacing
    val colors = PermAwareTheme.colors
    val bottomInset = LocalContentBottomInset.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermScreenHeader(title = stringResource(R.string.history_title))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                ChangeFilter.entries.forEach { filter ->
                    PermChip(
                        label = stringResource(filter.labelRes),
                        selected = state.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                    )
                }
            }

            if (state.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.history_empty_title),
                    body = if (state.hasAnyHistory) {
                        stringResource(R.string.history_empty_filter)
                    } else {
                        stringResource(R.string.history_empty_body)
                    },
                    icon = Icons.Outlined.History,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.screenHorizontal,
                        end = spacing.screenHorizontal,
                        bottom = bottomInset + spacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
                ) {
                    state.days.forEach { day ->
                        item(key = "day_" + day.dayStart) {
                            PermSectionLabel(
                                text = dayLabel(day.dayStart),
                                modifier = Modifier.padding(top = spacing.xs, bottom = spacing.xxs),
                            )
                        }
                        items(
                            day.changes,
                            key = { "change_" + day.dayStart + it.packageName + it.timestamp },
                        ) { change ->
                            ChangeEventCard(
                                change = change,
                                timeLabel = RelativeTime.ago(context, change.timestamp),
                                onClick = { onOpenApp(change.packageName) },
                            )
                        }
                    }
                    item(key = "local_note") {
                        Text(
                            text = stringResource(R.string.history_local_note),
                            style = PermAwareType.caption,
                            color = colors.textDisabled,
                            modifier = Modifier.padding(top = spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun dayLabel(dayStart: Long): String {
    val context = LocalContext.current
    return when {
        RelativeTime.isSameDay(dayStart, System.currentTimeMillis()) ->
            stringResource(R.string.history_today)
        RelativeTime.isYesterday(dayStart) -> stringResource(R.string.history_yesterday)
        else -> RelativeTime.date(context, dayStart)
    }
}
