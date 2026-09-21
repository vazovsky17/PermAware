package app.vazovsky.permaware.ui.screens.appdetail

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
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AttentionAssessment
import app.vazovsky.permaware.domain.model.AttentionFactor
import app.vazovsky.permaware.domain.model.AttentionFactorKind
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.AttentionChip
import app.vazovsky.permaware.ui.components.PreviewApps
import app.vazovsky.permaware.ui.theme.PermAwareTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionDetailSheet(assessment: AttentionAssessment, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AttentionDetailContent(assessment)
    }
}

@Composable
private fun AttentionDetailContent(assessment: AttentionAssessment, modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.attention_why_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            AttentionChip(level = assessment.level)
            Text(
                text = stringResource(R.string.attention_score, assessment.score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))

        if (assessment.factors.isEmpty()) {
            Text(
                text = stringResource(R.string.attention_no_factors),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            assessment.factors.forEach { factor -> FactorRow(factor) }
        }

        Text(
            text = stringResource(R.string.attention_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.sm),
        )
    }
}

@Composable
private fun FactorRow(factor: AttentionFactor, modifier: Modifier = Modifier) {
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = factor.describe(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.attention_points, factor.points),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AttentionFactor.describe(): String = when (kind) {
    AttentionFactorKind.GRANTED_SENSITIVE_PERMISSION -> {
        val name = permissionRef?.let { PermissionCatalog.find(it)?.titleRes }
            ?.let { stringResource(it) }
            ?: categoryRef?.let { stringResource(it.labelRes) }
            ?: permissionRef.orEmpty()
        stringResource(R.string.attention_factor_granted, name)
    }
    AttentionFactorKind.BACKGROUND_LOCATION ->
        stringResource(R.string.attention_factor_background_location)
    AttentionFactorKind.ACTIVE_SPECIAL_ACCESS -> stringResource(
        R.string.attention_factor_active_special,
        specialAccessRef?.let { stringResource(it.labelRes) }.orEmpty(),
    )
    AttentionFactorKind.DECLARED_SPECIAL_ACCESS -> stringResource(
        R.string.attention_factor_declared_special,
        specialAccessRef?.let { stringResource(it.labelRes) }.orEmpty(),
    )
    AttentionFactorKind.SENSITIVE_COMBINATION ->
        stringResource(R.string.attention_factor_combination, count)
    AttentionFactorKind.UNKNOWN_INSTALL_SOURCE ->
        stringResource(R.string.attention_factor_unknown_source)
}

@Preview(showBackground = true)
@Composable
private fun AttentionDetailPreview() {
    PermAwareTheme {
        AttentionDetailContent(PreviewApps.flashlight.attention)
    }
}
