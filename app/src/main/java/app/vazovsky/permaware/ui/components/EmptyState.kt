package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/**
 * Единственное представление пустоты и ошибки во всём приложении.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(PermAwareTheme.icons.discLarge)
                .clip(CircleShape)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(PermAwareTheme.icons.xl),
            )
        }
        Text(
            text = title,
            style = PermAwareType.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = PermAwareType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Button(
                onClick = onActionClick,
                shape = PermAwareTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.padding(top = spacing.xs),
            ) {
                Text(actionLabel, style = PermAwareType.titleSmall)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun EmptyStatePreview() {
    PermAwareTheme {
        EmptyState(
            title = "Изменений пока нет",
            body = "PermAware сравнивает каждую проверку с предыдущей.",
            actionLabel = "Проверить сейчас",
            onActionClick = {},
        )
    }
}
