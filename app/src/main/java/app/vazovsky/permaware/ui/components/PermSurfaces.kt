package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType
import app.vazovsky.permaware.ui.theme.permAwareShadow

enum class PermSurfaceStyle { Floating, Outlined, Flat }

@Composable
fun PermCard(
    modifier: Modifier = Modifier,
    style: PermSurfaceStyle = PermSurfaceStyle.Floating,
    shape: Shape = PermAwareTheme.shapes.lg,
    contentPadding: PaddingValues = PaddingValues(PermAwareTheme.spacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .permSurface(style, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun PermSurface(
    modifier: Modifier = Modifier,
    style: PermSurfaceStyle = PermSurfaceStyle.Floating,
    shape: Shape = PermAwareTheme.shapes.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.permSurface(style, shape), content = content)
}

@Composable
fun Modifier.permSurface(style: PermSurfaceStyle, shape: Shape): Modifier {
    val colors = PermAwareTheme.colors
    return when (style) {
        PermSurfaceStyle.Floating ->
            this
                .permAwareShadow(shape)
                .clip(shape)
                .background(colors.surface)

        PermSurfaceStyle.Outlined ->
            this
                .clip(shape)
                .background(colors.surface)
                .border(PermAwareTheme.elevation.hairline, colors.outline, shape)

        PermSurfaceStyle.Flat ->
            this
                .clip(shape)
                .background(colors.surface)
    }
}

@Composable
fun PermDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = PermAwareTheme.elevation.hairline,
        color = PermAwareTheme.colors.divider,
    )
}

@Composable
fun PermSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PermAwareType.label,
        color = PermAwareTheme.colors.textSecondary,
        modifier = modifier,
    )
}
