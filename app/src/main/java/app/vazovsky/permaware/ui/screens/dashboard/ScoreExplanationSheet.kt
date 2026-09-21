package app.vazovsky.permaware.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.attention.PrivacyOverview
import app.vazovsky.permaware.domain.attention.PrivacyReason
import app.vazovsky.permaware.domain.attention.PrivacyReasonKind
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.theme.PermAwareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreExplanationSheet(overview: PrivacyOverview, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ScoreExplanationContent(overview)
    }
}

@Composable
private fun ScoreExplanationContent(overview: PrivacyOverview, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md)
            .padding(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.score_explain_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.score_explain_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))

        ScoreLine(
            label = stringResource(R.string.score_explain_base),
            value = "100",
            emphasised = false,
        )

        if (overview.reasons.isEmpty()) {
            Text(
                text = stringResource(R.string.score_explain_no_reasons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            overview.reasons.forEach { reason ->
                ScoreLine(
                    label = reason.describe(),
                    value = stringResource(R.string.score_explain_penalty, reason.penalty),
                    emphasised = false,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))

        ScoreLine(
            label = stringResource(R.string.score_explain_result),
            value = overview.score.toString(),
            emphasised = true,
        )

        Text(
            text = stringResource(R.string.score_explain_system_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )
    }
}

@Composable
private fun PrivacyReason.describe(): String = when (kind) {
    PrivacyReasonKind.APPS_WITH_CATEGORY -> stringResource(
        R.string.score_reason_category,
        stringResource(category!!.labelRes),
        appCount,
    )
    PrivacyReasonKind.APPS_WITH_BACKGROUND_LOCATION ->
        stringResource(R.string.score_reason_background_location, appCount)
    PrivacyReasonKind.APPS_WITH_ACTIVE_SPECIAL_ACCESS ->
        stringResource(R.string.score_reason_special_access, appCount)
    PrivacyReasonKind.APPS_NEEDING_ATTENTION ->
        stringResource(R.string.score_reason_attention, appCount)
}

@Composable
private fun ScoreLine(label: String, value: String, emphasised: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasised) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreExplanationPreview() {
    PermAwareTheme {
        ScoreExplanationContent(
            PrivacyOverview(
                score = 85,
                reasons = listOf(
                    PrivacyReason(
                        PrivacyReasonKind.APPS_WITH_CATEGORY,
                        11,
                        4,
                        app.vazovsky.permaware.domain.model.PermissionCategory.LOCATION,
                    ),
                    PrivacyReason(
                        PrivacyReasonKind.APPS_WITH_CATEGORY,
                        10,
                        3,
                        app.vazovsky.permaware.domain.model.PermissionCategory.MICROPHONE,
                    ),
                    PrivacyReason(PrivacyReasonKind.APPS_WITH_ACTIVE_SPECIAL_ACCESS, 13, 2),
                ),
                appsNeedingAttention = 2,
                userAppCount = 53,
                categoryCounts = emptyMap(),
            ),
        )
    }
}
