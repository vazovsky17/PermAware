package app.vazovsky.permaware.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType
import app.vazovsky.permaware.ui.theme.permAwareShadow

private const val COMPACT_LABEL_FONT_SCALE = 1.0f
const val BOTTOM_BAR_LABEL_TAG = "bottom_bar_label"
private val IndicatorWidth = 44.dp
private val IndicatorHeight = 30.dp
private val RippleRadius = 28.dp

/**
 * Плавающая панель навигации в форме пилюли, отступающая от краёв экрана, — подпись семейства
 * вместо материаловского `NavigationBar` во всю ширину.
 */
@Composable
fun PermBottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = PermAwareTheme.shapes.xl
    val spacing = PermAwareTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .permAwareShadow(
                shape = shape,
                y = PermAwareTheme.elevation.floatingY,
                blur = PermAwareTheme.elevation.floatingBlur,
                alpha = PermAwareTheme.elevation.floatingAlpha,
            )
            .clip(shape)
            .background(PermAwareTheme.colors.elevatedSurface)
            .padding(horizontal = spacing.xxs, vertical = spacing.xs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun RowScope.PermBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val compact = LocalDensity.current.fontScale > COMPACT_LABEL_FONT_SCALE
    val showLabel = !compact || selected
    val indicator by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceMuted.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 180),
        label = "navIndicatorColor",
    )
    val iconTint = if (selected) colors.onPrimary else colors.textSecondary
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .then(
                if (showLabel) {
                    Modifier.weight(1f)
                } else {
                    Modifier.defaultMinSize(minWidth = spacing.minTouchTarget)
                },
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .indication(interaction, ripple(bounded = false, radius = RippleRadius))
            .defaultMinSize(minHeight = spacing.minTouchTarget)
            .padding(vertical = spacing.xxs)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxxs),
    ) {
        Box(
            modifier = Modifier
                .size(width = IndicatorWidth, height = IndicatorHeight)
                .clip(CircleShape)
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(PermAwareTheme.icons.lg),
            )
        }
        if (showLabel) {
            Text(
                text = label,
                style = PermAwareType.labelSmall,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { testTag = BOTTOM_BAR_LABEL_TAG },
            )
        }
    }
}
