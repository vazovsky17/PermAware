package app.vazovsky.permaware.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/** Чип-фильтр. */
@Composable
fun PermChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSelectedMark: Boolean = false,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val shape = PermAwareTheme.shapes.pill
    val container by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surface,
        animationSpec = tween(160),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.textSecondary,
        animationSpec = tween(160),
        label = "chipContent",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(PermAwareTheme.elevation.hairline, colors.outline, shape)
                },
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
    ) {
        if (showSelectedMark && selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(PermAwareTheme.icons.sm),
            )
        }
        Text(
            text = label,
            style = PermAwareType.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermChipPreview() {
    PermAwareTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermChip("Местоположение", selected = false, onClick = {})
            PermChip("Камера", selected = true, onClick = {}, showSelectedMark = true)
            PermChip("Микрофон", selected = false, onClick = {})
        }
    }
}
