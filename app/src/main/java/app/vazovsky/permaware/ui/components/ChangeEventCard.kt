package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AppChange
import app.vazovsky.permaware.domain.model.ChangeEvent
import app.vazovsky.permaware.domain.model.ChangeType
import app.vazovsky.permaware.domain.model.groupToAppChanges
import app.vazovsky.permaware.domain.permission.PermissionCatalog
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/**
 * Все изменения по одному приложению за одну проверку.
 */
@Composable
fun ChangeEventCard(change: AppChange, modifier: Modifier = Modifier, timeLabel: String? = null, onClick: () -> Unit) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val updatedWithNewPermissions = change.events.any { it.type == ChangeType.APP_UPDATED } &&
        change.events.any { it.type == ChangeType.PERMISSION_REQUEST_ADDED }
    val removed = change.isRemoval
    val title = change.displayLabel ?: stringResource(R.string.history_removed_app_unknown)
    val removedDescription = stringResource(R.string.cd_removed_app, title, change.packageName)

    PermCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(PermAwareTheme.shapes.lg)
            // У удалённого приложения нет экрана, который можно открыть.
            .then(if (removed) Modifier else Modifier.clickable(onClick = onClick))
            .then(
                if (removed) {
                    Modifier.semantics { contentDescription = removedDescription }
                } else {
                    Modifier
                },
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (removed) {
                // Для исчезнувшего пакета иконку не загрузить.
                Box(
                    modifier = Modifier
                        .size(PermAwareTheme.icons.appCompact)
                        .clip(PermAwareTheme.shapes.sm)
                        .background(colors.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        // Знак архива.
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(PermAwareTheme.icons.sm),
                    )
                }
            } else {
                AppIcon(
                    packageName = change.packageName,
                    label = change.appLabel,
                    size = PermAwareTheme.icons.appCompact,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PermAwareType.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (removed) {
                    Text(
                        text = change.packageName,
                        style = PermAwareType.caption,
                        color = colors.textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (timeLabel != null) {
                Text(
                    text = timeLabel,
                    style = PermAwareType.caption,
                    color = colors.textDisabled,
                )
            }
        }

        if (updatedWithNewPermissions) {
            Text(
                text = stringResource(R.string.change_after_update),
                style = PermAwareType.labelSmall,
                color = colors.attentionModerateContent,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }

        Column(
            modifier = Modifier.padding(top = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            change.events.forEach { event -> ChangeEventLine(event) }
        }

        if (removed) {
            Text(
                text = stringResource(R.string.history_removed_note),
                style = PermAwareType.caption,
                color = colors.textDisabled,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

@Composable
private fun ChangeEventLine(event: ChangeEvent, modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val tone = event.type.tone()
    val container: Color = when (tone) {
        EventTone.Gained -> colors.attentionModerateContainer
        EventTone.Released -> colors.surfaceMuted
        EventTone.Neutral -> colors.surfaceMuted
    }
    val content: Color = when (tone) {
        EventTone.Gained -> colors.attentionModerateContent
        EventTone.Released -> colors.positive
        EventTone.Neutral -> colors.onSurfaceMuted
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(PermAwareTheme.icons.xl)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = event.type.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(PermAwareTheme.icons.xs),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(event.type.labelRes),
                style = PermAwareType.caption,
                color = colors.textSecondary,
            )
            val detail = event.detailText()
            if (detail != null) {
                Text(
                    text = detail,
                    style = PermAwareType.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class EventTone { Gained, Released, Neutral }

private fun ChangeType.tone(): EventTone = when (this) {
    ChangeType.PERMISSION_REQUEST_ADDED,
    ChangeType.PERMISSION_GRANTED,
    ChangeType.SPECIAL_ACCESS_GAINED,
    -> EventTone.Gained

    ChangeType.PERMISSION_REQUEST_REMOVED,
    ChangeType.PERMISSION_REVOKED,
    ChangeType.SPECIAL_ACCESS_LOST,
    -> EventTone.Released

    else -> EventTone.Neutral
}

/** То конкретное, что изменилось: разрешение, возможность или переход версии. */
@Composable
private fun ChangeEvent.detailText(): String? = when {
    previousVersion != null && newVersion != null ->
        stringResource(R.string.change_version_transition, previousVersion, newVersion)
    permission != null -> PermissionCatalog.find(permission)
        ?.let { stringResource(it.titleRes) }
        ?: permission.substringAfterLast('.')
    specialAccess != null -> stringResource(specialAccess.labelRes)
    newVersion != null -> newVersion
    previousVersion != null -> if (type == ChangeType.APP_REMOVED) {
        stringResource(R.string.history_removed_last_version, previousVersion)
    } else {
        previousVersion
    }
    else -> null
}

private val ChangeType.icon: ImageVector
    get() = when (this) {
        ChangeType.APP_INSTALLED -> Icons.Outlined.DownloadDone
        ChangeType.APP_REMOVED -> Icons.Outlined.Delete
        ChangeType.APP_UPDATED -> Icons.Outlined.SystemUpdateAlt
        ChangeType.PERMISSION_REQUEST_ADDED, ChangeType.PERMISSION_GRANTED -> Icons.Outlined.Add
        ChangeType.PERMISSION_REQUEST_REMOVED, ChangeType.PERMISSION_REVOKED -> Icons.Outlined.Remove
        ChangeType.SPECIAL_ACCESS_GAINED -> Icons.Outlined.Add
        ChangeType.SPECIAL_ACCESS_LOST -> Icons.Outlined.Remove
    }

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun ChangeEventCardPreview() {
    PermAwareTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            with(PreviewApps.changes) { groupToAppChanges() }.forEach {
                ChangeEventCard(change = it, timeLabel = "12:42") {}
            }
        }
    }
}
