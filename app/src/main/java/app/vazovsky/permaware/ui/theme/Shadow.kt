package app.vazovsky.permaware.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun Modifier.permAwareShadow(
    shape: Shape,
    y: Dp = PermAwareTheme.elevation.restingY,
    blur: Dp = PermAwareTheme.elevation.restingBlur,
    alpha: Float = PermAwareTheme.elevation.restingAlpha,
): Modifier {
    val enabled = !PermAwareTheme.colors.isDark
    return if (!enabled) {
        this
    } else {
        drawBehind {
            val blurPx = blur.toPx()
            if (blurPx <= 0f) return@drawBehind
            drawIntoCanvas { canvas ->
                val paint = Paint()
                val frameworkPaint = paint.asFrameworkPaint()
                frameworkPaint.color = Color.Transparent.toArgb()
                frameworkPaint.setShadowLayer(
                    blurPx,
                    0f,
                    y.toPx(),
                    ShadowViolet.copy(alpha = alpha).toArgb(),
                )
                canvas.nativeCanvas.drawOutline(shape, size, layoutDirection, density = this, paint)
            }
        }
    }
}

private fun android.graphics.Canvas.drawOutline(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    paint: Paint,
) {
    when (val outline = shape.createOutline(size, layoutDirection, density)) {
        is Outline.Rectangle ->
            drawRect(
                outline.rect.left,
                outline.rect.top,
                outline.rect.right,
                outline.rect.bottom,
                paint.asFrameworkPaint(),
            )
        is Outline.Rounded -> {
            val r = outline.roundRect
            drawRoundRect(
                r.left,
                r.top,
                r.right,
                r.bottom,
                r.topLeftCornerRadius.x,
                r.topLeftCornerRadius.y,
                paint.asFrameworkPaint(),
            )
        }
        is Outline.Generic ->
            drawPath(outline.path.asAndroidPath(), paint.asFrameworkPaint())
    }
}
