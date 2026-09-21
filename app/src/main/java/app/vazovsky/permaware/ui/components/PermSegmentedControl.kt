package app.vazovsky.permaware.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun <T> PermSegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val shape = PermAwareTheme.shapes.pill

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .horizontalScroll(rememberScrollState())
            .padding(PermSegmentedControlPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(PermSegmentedControlPadding),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.surface,
                animationSpec = tween(160),
                label = "segmentContainer",
            )
            val content by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.textSecondary,
                animationSpec = tween(160),
                label = "segmentContent",
            )
            Text(
                text = label(option),
                style = PermAwareType.labelSmall,
                color = content,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(shape)
                    .background(container)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = spacing.xs, horizontal = spacing.sm),
            )
        }
    }
}

private val PermSegmentedControlPadding = 3.dp

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun PermSegmentedControlPreview() {
    PermAwareTheme {
        PermSegmentedControl(
            options = listOf("Все", "Пользовательские", "Системные"),
            selected = "Пользовательские",
            label = { it },
            onSelect = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
