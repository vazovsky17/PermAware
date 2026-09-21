package app.vazovsky.permaware.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.ui.theme.PermAwareTheme

private val FadeHeight = 40.dp

val LocalContentBottomInset = staticCompositionLocalOf { 0.dp }

@Composable
fun PermBottomFade(modifier: Modifier = Modifier, barHeight: Dp, height: Dp = FadeHeight) {
    val background = PermAwareTheme.colors.background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(background),
        )
    }
}
