package app.vazovsky.permaware.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun ScanProgress(modifier: Modifier = Modifier, label: String? = null) {
    val spacing = PermAwareTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ScanDots()
        Text(
            text = label ?: stringResource(R.string.scan_in_progress),
            style = PermAwareType.body,
            color = PermAwareTheme.colors.textSecondary,
        )
    }
}

@Composable
fun ScanDots(modifier: Modifier = Modifier) {
    val colors = PermAwareTheme.colors
    val transition = rememberInfiniteTransition(label = "scan")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, delayMillis = index * 140),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.primary.copy(alpha = alpha)),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE7FD)
@Composable
private fun ScanProgressPreview() {
    PermAwareTheme { ScanProgress(Modifier.padding(20.dp)) }
}
