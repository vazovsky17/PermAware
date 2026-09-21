package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PermScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.screenContentTop,
                bottom = spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PermAwareType.headline,
                color = PermAwareTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PermAwareType.caption,
                    color = PermAwareTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = spacing.xxxs),
                )
            }
        }
        trailing?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermScreenHeaderPreview() {
    PermAwareTheme {
        PermScreenHeader(title = "PermAware", subtitle = "Последняя проверка: сегодня, 18:04")
    }
}
