package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.ui.common.labelRes
import app.vazovsky.permaware.ui.common.shortLabelRes
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

/** Значок уровня. */
val AttentionLevel.icon: ImageVector
    get() = when (this) {
        AttentionLevel.LOW -> Icons.Outlined.CheckCircle
        AttentionLevel.MODERATE -> Icons.Outlined.ErrorOutline
        AttentionLevel.ELEVATED -> Icons.Outlined.PriorityHigh
    }

/** Значок внимания. */
@Composable
fun AttentionChip(level: AttentionLevel, modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing
    val label = stringResource(if (compact) level.shortLabelRes else level.labelRes)
    val description = stringResource(R.string.cd_attention_level, stringResource(level.labelRes))

    val onSolidFill = level != AttentionLevel.LOW
    val container = if (onSolidFill) colors.attentionSolid(level) else colors.attentionContainer(level)
    val content = if (onSolidFill) colors.attentionOnSolid(level) else colors.attentionContent(level)

    Row(
        modifier = modifier
            .clip(PermAwareTheme.shapes.pill)
            .background(container)
            .padding(horizontal = spacing.xs, vertical = spacing.xxs)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Icon(
            imageVector = level.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(PermAwareTheme.icons.sm),
        )
        Text(
            text = label,
            style = PermAwareType.labelSmall,
            color = content,
        )
    }
}

/**
 * Компактная форма для нагруженных строк
 */
@Composable
fun AttentionBadge(level: AttentionLevel, modifier: Modifier = Modifier, size: Dp = DefaultBadgeSize) {
    val colors = PermAwareTheme.colors
    val description = stringResource(R.string.cd_attention_level, stringResource(level.labelRes))
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.attentionSolid(level))
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = level.icon,
            contentDescription = null,
            tint = colors.attentionOnSolid(level),
            modifier = Modifier.size(size * GLYPH_SHARE),
        )
    }
}

/**
 * Одна точка — для отображения общего состояния.
 */
@Composable
fun AttentionDot(level: AttentionLevel, modifier: Modifier = Modifier, size: Int = 6) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PermAwareTheme.colors.attentionSolid(level)),
    )
}

private val DefaultBadgeSize = 18.dp
private const val GLYPH_SHARE = 0.72f

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun AttentionChipPreview() {
    PermAwareTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AttentionLevel.entries.forEach { AttentionChip(it) }
            AttentionLevel.entries.forEach { AttentionChip(it, compact = true) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttentionLevel.entries.forEach { AttentionBadge(it) }
            }
        }
    }
}
