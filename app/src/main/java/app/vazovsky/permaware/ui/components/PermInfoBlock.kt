package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PermInfoBlock(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    tint: Color? = null,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val accent = tint ?: colors.textSecondary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(PermAwareTheme.icons.md),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxxs)) {
            Text(text = label, style = PermAwareType.label, color = accent)
            Text(text = body, style = PermAwareType.body, color = colors.textPrimary)
        }
    }
}

@Composable
fun PermStatusBanner(
    state: String,
    modifier: Modifier = Modifier,
    glyph: ImageVector,
    container: Color,
    content: Color,
    note: String? = null,
) {
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PermAwareTheme.shapes.md)
            .background(container)
            .padding(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier = Modifier.size(PermAwareTheme.icons.md),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = glyph,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(PermAwareTheme.icons.md),
                )
            }
            Text(text = state, style = PermAwareType.title, color = content)
        }
        if (note != null) {
            Text(
                text = note,
                style = PermAwareType.caption,
                color = content.copy(alpha = 0.85f),
            )
        }
    }
}

/** Технические значения — константы разрешений, имена пакетов — в приглушённом тихом контейнере. */
@Composable
fun PermTechnicalValue(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PermAwareTheme.shapes.sm)
            .background(colors.surfaceMuted)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxxs),
    ) {
        Text(text = label, style = PermAwareType.labelSmall, color = colors.onSurfaceMuted)
        Text(text = value, style = PermAwareType.caption, color = colors.textSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCFBFE)
@Composable
private fun PermInfoBlockPreview() {
    PermAwareTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermStatusBanner(
                state = "Разрешено",
                glyph = Icons.Outlined.Info,
                container = PermAwareTheme.colors.surfaceMuted,
                content = PermAwareTheme.colors.positive,
                note = "Запрашивается у пользователя",
            )
            PermInfoBlock(
                label = "ЧТО РАЗРЕШАЕТ",
                body = "Приложение может определять точные координаты устройства.",
            )
            PermTechnicalValue("Техническое имя", "android.permission.ACCESS_FINE_LOCATION")
        }
    }
}
