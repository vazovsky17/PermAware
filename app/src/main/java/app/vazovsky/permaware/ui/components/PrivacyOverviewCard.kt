package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PrivacyOverviewCard(
    score: Int,
    appsNeedingAttention: Int,
    isScanning: Boolean,
    hasResults: Boolean,
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit,
    onAttentionClick: () -> Unit,
    onOpenApps: () -> Unit,
    onWhyClick: () -> Unit,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val level = attentionLevelFor(score)

    PermCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(PermAwareTheme.spacing.cardPaddingLarge),
    ) {
        Text(
            text = stringResource(R.string.dashboard_privacy_overview),
            style = PermAwareType.label,
            color = colors.textSecondary,
        )

        PrivacyScoreDial(
            score = score,
            level = level,
            isScanning = isScanning,
            hasResults = hasResults,
            onClick = onScanClick,
            modifier = Modifier.padding(top = spacing.md),
        )

        if (hasResults) {
            AttentionChip(
                level = level,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = spacing.md),
            )

            Text(
                text = stringResource(verdictFor(level)),
                style = PermAwareType.body,
                color = colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = spacing.sm),
            )

            val hasAttention = appsNeedingAttention > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.minTouchTarget)
                    .clip(PermAwareTheme.shapes.sm)
                    .clickable(onClick = if (hasAttention) onAttentionClick else onOpenApps),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (!hasAttention) {
                    AttentionDot(AttentionLevel.LOW, size = 7)
                    Text(
                        text = stringResource(R.string.dashboard_attention_none),
                        style = PermAwareType.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(PermAwareTheme.icons.xs),
                    )
                } else {
                    AttentionDot(level, size = 7)
                    Text(
                        text = pluralStringResource(
                            R.plurals.plural_apps_need_attention,
                            appsNeedingAttention,
                            appsNeedingAttention,
                        ),
                        style = PermAwareType.caption,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(PermAwareTheme.icons.xs),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PermAwareTheme.shapes.sm)
                    .clickable(onClick = onWhyClick)
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_why_score, score),
                    style = PermAwareType.titleSmall,
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(PermAwareTheme.icons.md),
                )
            }
        }
    }
}

internal fun attentionLevelFor(score: Int): AttentionLevel = when {
    score >= 80 -> AttentionLevel.LOW
    score >= 55 -> AttentionLevel.MODERATE
    else -> AttentionLevel.ELEVATED
}

private fun verdictFor(level: AttentionLevel): Int = when (level) {
    AttentionLevel.LOW -> R.string.dashboard_verdict_good
    AttentionLevel.MODERATE -> R.string.dashboard_verdict_watch
    AttentionLevel.ELEVATED -> R.string.dashboard_verdict_review
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD, name = "Overview — healthy")
@Composable
private fun PrivacyOverviewCardPreview() {
    PermAwareTheme {
        PrivacyOverviewCard(
            score = 84,
            appsNeedingAttention = 0,
            isScanning = false,
            hasResults = true,
            modifier = Modifier.padding(20.dp),
            onScanClick = {},
            onAttentionClick = {},
            onOpenApps = {},
            onWhyClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0C1B, name = "Overview — attention, dark")
@Composable
private fun PrivacyOverviewCardDarkPreview() {
    PermAwareTheme(themeMode = app.vazovsky.permaware.data.prefs.ThemeMode.DARK) {
        PrivacyOverviewCard(
            score = 48,
            appsNeedingAttention = 3,
            isScanning = false,
            hasResults = true,
            modifier = Modifier.padding(20.dp),
            onScanClick = {},
            onAttentionClick = {},
            onOpenApps = {},
            onWhyClick = {},
        )
    }
}
