package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.domain.model.InstalledApp
import app.vazovsky.permaware.domain.model.PermissionCategory
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/**
 * Одна строка списка приложений
 */
@Composable
fun AppAttentionRow(app: InstalledApp, modifier: Modifier = Modifier, subtitle: String? = null, onClick: () -> Unit) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val grantedCount = app.grantedSensitivePermissions.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AppIcon(packageName = app.packageName, label = app.label, size = PermAwareTheme.icons.appRow)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                if (app.attention.level != AttentionLevel.LOW) {
                    AttentionBadge(app.attention.level)
                }
                Text(
                    text = app.label,
                    style = PermAwareType.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = pluralStringResource(R.plurals.plural_row_permissions, grantedCount, grantedCount),
                style = PermAwareType.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PermAwareType.caption,
                    color = colors.textDisabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        CategoryGlyphs(categories = app.grantedCategories)
    }
}

/** Полоска категорий приложения */
@Composable
private fun CategoryGlyphs(categories: Set<PermissionCategory>, modifier: Modifier = Modifier) {
    val shown = HIGHLIGHT_ORDER.filter { it in categories }.take(MAX_GLYPHS)
    if (shown.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PermAwareTheme.spacing.xxs),
    ) {
        shown.forEach { category ->
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = PermAwareTheme.colors.textDisabled,
                modifier = Modifier.size(PermAwareTheme.icons.xs),
            )
        }
    }
}

private val HIGHLIGHT_ORDER = listOf(
    PermissionCategory.LOCATION,
    PermissionCategory.MICROPHONE,
    PermissionCategory.CAMERA,
    PermissionCategory.CONTACTS,
    PermissionCategory.SMS,
)
private const val MAX_GLYPHS = 4

@Preview(showBackground = true, backgroundColor = 0xFFFCFBFE)
@Composable
private fun AppAttentionRowPreview() {
    PermAwareTheme {
        Column {
            AppAttentionRow(PreviewApps.messenger, subtitle = "Обновлено 2 дня назад") {}
            PermDivider(Modifier.padding(start = 68.dp))
            AppAttentionRow(PreviewApps.flashlight, subtitle = "Обновлено вчера") {}
            PermDivider(Modifier.padding(start = 68.dp))
            AppAttentionRow(PreviewApps.calculator, subtitle = "Обновлено 3 месяца назад") {}
        }
    }
}
