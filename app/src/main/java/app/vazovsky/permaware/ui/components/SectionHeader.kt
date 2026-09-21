package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val spacing = PermAwareTheme.spacing
    val colors = PermAwareTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.lg, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PermAwareType.label, color = colors.textSecondary)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PermAwareType.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxxs),
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick, contentPadding = PaddingValuesCompact) {
                Text(actionLabel, style = PermAwareType.labelSmall, color = colors.primary)
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(PermAwareTheme.icons.sm),
                )
            }
        }
    }
}

private val PaddingValuesCompact = androidx.compose.foundation.layout.PaddingValues(
    horizontal = 8.dp,
    vertical = 4.dp,
)

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun SectionHeaderPreview() {
    PermAwareTheme {
        SectionHeader(
            title = "ЧУВСТВИТЕЛЬНЫЕ РАЗРЕШЕНИЯ",
            subtitle = "Сколько ваших приложений сейчас имеют доступ",
            actionLabel = "Все",
            onActionClick = {},
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
