package app.vazovsky.permaware.ui.screens.appdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.ui.common.displayAttentionNote
import app.vazovsky.permaware.ui.common.displayDescription
import app.vazovsky.permaware.ui.common.displayTitle
import app.vazovsky.permaware.ui.common.isUnexplained
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.components.PermInfoBlock
import app.vazovsky.permaware.ui.components.PermStatusBanner
import app.vazovsky.permaware.ui.components.PermTechnicalValue
import app.vazovsky.permaware.ui.components.PreviewApps
import app.vazovsky.permaware.ui.components.glyph
import app.vazovsky.permaware.ui.components.icon
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDetailSheet(permission: AppPermission, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PermissionDetailContent(permission)
    }
}

@Composable
private fun PermissionDetailContent(permission: AppPermission, modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(PermAwareTheme.icons.disc)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = permission.category.icon,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(PermAwareTheme.icons.lg),
                )
            }
            Text(
                text = permission.displayTitle(),
                style = PermAwareType.headline,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        val stateColor = when (permission.state) {
            PermissionState.GRANTED -> colors.positive
            PermissionState.DENIED -> colors.textSecondary
            PermissionState.UNDETERMINED -> colors.textDisabled
        }
        PermStatusBanner(
            state = stringResource(permission.state.labelRes),
            glyph = permission.state.glyph,
            container = colors.surfaceMuted,
            content = stateColor,
            note = if (permission.state == PermissionState.UNDETERMINED) {
                stringResource(R.string.permission_state_undetermined_explanation)
            } else {
                stringResource(permission.protection.labelRes)
            },
        )

        val description = permission.displayDescription()
        if (description != null) {
            PermInfoBlock(
                label = stringResource(R.string.permission_what_it_allows),
                body = description,
                icon = Icons.Outlined.Info,
            )
        } else if (permission.isUnexplained) {
            PermInfoBlock(
                label = stringResource(R.string.permission_what_it_allows),
                body = stringResource(R.string.permission_no_description),
                icon = Icons.Outlined.Info,
            )
        }

        permission.displayAttentionNote()?.let { note ->
            PermInfoBlock(
                label = stringResource(R.string.permission_why_attention),
                body = note,
                icon = Icons.Outlined.Visibility,
                tint = colors.attentionModerateContent,
            )
        }

        if (permission.isCustom) {
            PermInfoBlock(
                label = stringResource(R.string.permission_custom),
                body = stringResource(R.string.permission_custom_explanation),
                icon = Icons.Outlined.Extension,
            )
        }

        PermTechnicalValue(
            label = stringResource(R.string.permission_technical_name),
            value = permission.name,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionDetailPreview() {
    PermAwareTheme {
        PermissionDetailContent(PreviewApps.flashlight.permissions.first())
    }
}
