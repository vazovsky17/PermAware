package app.vazovsky.permaware.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.R
import app.vazovsky.permaware.domain.model.AttentionLevel
import app.vazovsky.permaware.ui.theme.PermAwareTheme
import app.vazovsky.permaware.ui.theme.PermAwareType

@Composable
fun PrivacyScoreDial(
    score: Int,
    level: AttentionLevel,
    isScanning: Boolean,
    hasResults: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PermAwareTheme.colors
    val spacing = PermAwareTheme.spacing

    val description = if (hasResults) {
        stringResource(R.string.cd_scan_dial, score)
    } else {
        stringResource(R.string.cd_scan_dial_empty)
    }

    val progress by animateFloatAsState(
        targetValue = if (hasResults && !isScanning) score / 100f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow,
        ),
        label = "dialProgress",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dialPress",
    )

    val ring = if (level == AttentionLevel.LOW) colors.primary else colors.attentionSolid(level)
    val track = colors.surfaceMuted

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = DialSize)
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(press)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    onClick = onClick,
                )
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                val stroke = size.minDimension * STROKE_SHARE
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (!isScanning && progress > 0f) {
                    drawArc(
                        color = ring,
                        startAngle = START_ANGLE,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }

            if (isScanning) {
                SweepArc(color = ring)
            }

            AnimatedContent(
                targetState = isScanning to hasResults,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "dialCentre",
            ) { (scanning, results) ->
                when {
                    scanning -> ScanDots()
                    results -> Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stringResource(R.string.dashboard_score_value, score),
                            style = PermAwareType.score,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_score_max),
                            style = PermAwareType.title,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = spacing.xxs, bottom = spacing.xs),
                        )
                    }

                    else -> AttentionChip(level = AttentionLevel.LOW)
                }
            }
        }

        Text(
            text = when {
                isScanning -> stringResource(R.string.dashboard_dial_scanning)
                hasResults -> stringResource(R.string.dashboard_dial_start)
                else -> stringResource(R.string.dashboard_dial_first)
            },
            style = PermAwareType.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.sm),
        )
    }
}

@Composable
private fun SweepArc(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dialSweep")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "dialRotation",
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val stroke = size.minDimension * STROKE_SHARE
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private val DialSize = 220.dp
private const val STROKE_SHARE = 0.085f
private const val START_ANGLE = -90f
