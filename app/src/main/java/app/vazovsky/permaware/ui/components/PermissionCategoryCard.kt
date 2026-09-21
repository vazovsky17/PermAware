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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/** Счётчик для разрешений. */
@Composable
fun PermissionCategoryCard(
    category: PermissionCategory,
    appCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val label = stringResource(category.labelRes)
    val description = stringResource(
        R.string.cd_category_card,
        label,
        pluralStringResource(R.plurals.plural_apps, appCount, appCount),
    )

    PermSurface(
        modifier = modifier
            .clip(PermAwareTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = PermAwareTheme.shapes.md,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(PermAwareTheme.icons.appCompact)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(PermAwareTheme.icons.md),
                    )
                }
                Text(
                    text = appCount.toString(),
                    style = PermAwareType.title,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = label,
                style = PermAwareType.caption,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermissionCategoryCardPreview() {
    PermAwareTheme {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCategoryCard(PermissionCategory.MICROPHONE, 10, Modifier.weight(1f)) {}
            PermissionCategoryCard(PermissionCategory.LOCATION, 11, Modifier.weight(1f)) {}
        }
    }
}
