package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.domain.model.AppPermission
import app.vazovsky.permaware.domain.model.PermissionState
import app.vazovsky.permaware.ui.common.displayTitle
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PermissionRow(permission: AppPermission, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val icons = PermAwareTheme.icons
    val granted = permission.state == PermissionState.GRANTED
    val stateColor: Color = when (permission.state) {
        PermissionState.GRANTED -> colors.positive
        PermissionState.DENIED -> colors.textSecondary
        PermissionState.UNDETERMINED -> colors.textDisabled
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = spacing.listRowHeight)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(icons.disc)
                .clip(CircleShape)
                .background(if (granted) colors.surfaceMuted else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = permission.category.icon,
                contentDescription = null,
                tint = if (granted) colors.onSurfaceMuted else colors.textDisabled,
                modifier = Modifier.size(icons.md),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxxs),
        ) {
            Text(
                text = permission.displayTitle(),
                style = PermAwareType.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Icon(
                    imageVector = permission.state.glyph,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(icons.xs),
                )
                Text(
                    text = stringResource(permission.state.labelRes),
                    style = PermAwareType.caption,
                    color = stateColor,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textDisabled,
            modifier = Modifier.size(icons.md),
        )
    }
}

internal val PermissionState.glyph: ImageVector
    get() = when (this) {
        PermissionState.GRANTED -> Icons.Outlined.CheckCircle
        PermissionState.DENIED -> Icons.Outlined.RemoveCircleOutline
        PermissionState.UNDETERMINED -> Icons.Outlined.HelpOutline
    }

internal val app.vazovsky.permaware.domain.model.SpecialAccessStatus.glyph: ImageVector
    get() = when (this) {
        app.vazovsky.permaware.domain.model.SpecialAccessStatus.ACTIVE -> Icons.Outlined.CheckCircle
        app.vazovsky.permaware.domain.model.SpecialAccessStatus.INACTIVE -> Icons.Outlined.RemoveCircleOutline
        app.vazovsky.permaware.domain.model.SpecialAccessStatus.DECLARED,
        app.vazovsky.permaware.domain.model.SpecialAccessStatus.UNDETERMINED,
        -> Icons.Outlined.HelpOutline
    }

val PermissionRowTextInset = 34.dp + 12.dp + 16.dp

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermissionRowPreview() {
    PermAwareTheme {
        PermSurface(modifier = Modifier.padding(20.dp), style = PermSurfaceStyle.Flat) {
            PreviewApps.messenger.permissions.take(4).forEachIndexed { index, permission ->
                if (index > 0) PermDivider(Modifier.padding(start = PermissionRowTextInset))
                PermissionRow(permission) {}
            }
        }
    }
}
