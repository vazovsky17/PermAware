package app.vazovsky.permaware.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vazovsky.permaware.domain.model.AttentionLevel

@Immutable
data class PermAwareColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val surfaceMuted: Color,
    val onSurfaceMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val divider: Color,
    val outline: Color,
    val positive: Color,
    val danger: Color,
    val attentionLowContainer: Color,
    val attentionLowContent: Color,
    val attentionLowSolid: Color,
    val attentionLowOnSolid: Color,
    val attentionModerateContainer: Color,
    val attentionModerateContent: Color,
    val attentionModerateSolid: Color,
    val attentionModerateOnSolid: Color,
    val attentionElevatedContainer: Color,
    val attentionElevatedContent: Color,
    val attentionElevatedSolid: Color,
    val attentionElevatedOnSolid: Color,
    val isDark: Boolean,
) {
    fun attentionContainer(level: AttentionLevel): Color = when (level) {
        AttentionLevel.LOW -> attentionLowContainer
        AttentionLevel.MODERATE -> attentionModerateContainer
        AttentionLevel.ELEVATED -> attentionElevatedContainer
    }

    fun attentionContent(level: AttentionLevel): Color = when (level) {
        AttentionLevel.LOW -> attentionLowContent
        AttentionLevel.MODERATE -> attentionModerateContent
        AttentionLevel.ELEVATED -> attentionElevatedContent
    }

    fun attentionSolid(level: AttentionLevel): Color = when (level) {
        AttentionLevel.LOW -> attentionLowSolid
        AttentionLevel.MODERATE -> attentionModerateSolid
        AttentionLevel.ELEVATED -> attentionElevatedSolid
    }

    fun attentionOnSolid(level: AttentionLevel): Color = when (level) {
        AttentionLevel.LOW -> attentionLowOnSolid
        AttentionLevel.MODERATE -> attentionModerateOnSolid
        AttentionLevel.ELEVATED -> attentionElevatedOnSolid
    }
}

internal val LightPermAwareColors = PermAwareColors(
    background = LightBackground,
    surface = LightSurface,
    elevatedSurface = LightElevated,
    surfaceMuted = LightSurfaceMuted,
    onSurfaceMuted = LightOnSurfaceMuted,
    primary = Violet50,
    onPrimary = Color.White,
    secondary = Violet60,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textDisabled = LightTextDisabled,
    divider = LightDivider,
    outline = LightBorder,
    positive = PositiveLight,
    danger = ErrorLight,
    attentionLowContainer = AttentionLowContainerLight,
    attentionLowContent = AttentionLowContentLight,
    attentionLowSolid = AttentionLowSolidLight,
    attentionLowOnSolid = AttentionLowOnSolidLight,
    attentionModerateContainer = AttentionModerateContainerLight,
    attentionModerateContent = AttentionModerateContentLight,
    attentionModerateSolid = AttentionModerateSolidLight,
    attentionModerateOnSolid = AttentionModerateOnSolidLight,
    attentionElevatedContainer = AttentionElevatedContainerLight,
    attentionElevatedContent = AttentionElevatedContentLight,
    attentionElevatedSolid = AttentionElevatedSolidLight,
    attentionElevatedOnSolid = AttentionElevatedOnSolidLight,
    isDark = false,
)

internal val DarkPermAwareColors = PermAwareColors(
    background = DarkBackground,
    surface = DarkSurface,
    elevatedSurface = DarkElevated,
    surfaceMuted = DarkSurfaceMuted,
    onSurfaceMuted = DarkOnSurfaceMuted,
    primary = Violet80,
    onPrimary = Violet20,
    secondary = Violet60,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textDisabled = DarkTextDisabled,
    divider = DarkDivider,
    outline = DarkBorder,
    positive = PositiveDark,
    danger = ErrorDark,
    attentionLowContainer = AttentionLowContainerDark,
    attentionLowContent = AttentionLowContentDark,
    attentionLowSolid = AttentionLowSolidDark,
    attentionLowOnSolid = AttentionLowOnSolidDark,
    attentionModerateContainer = AttentionModerateContainerDark,
    attentionModerateContent = AttentionModerateContentDark,
    attentionModerateSolid = AttentionModerateSolidDark,
    attentionModerateOnSolid = AttentionModerateOnSolidDark,
    attentionElevatedContainer = AttentionElevatedContainerDark,
    attentionElevatedContent = AttentionElevatedContentDark,
    attentionElevatedSolid = AttentionElevatedSolidDark,
    attentionElevatedOnSolid = AttentionElevatedOnSolidDark,
    isDark = true,
)

@Immutable
data class PermAwareSpacing(
    val xxxs: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenContentTop: Dp = 12.dp,
    val cardPadding: Dp = 16.dp,
    val cardPaddingLarge: Dp = 20.dp,
    val cardGap: Dp = 12.dp,
    val sectionGap: Dp = 32.dp,
    val listRowHeight: Dp = 56.dp,
    val minTouchTarget: Dp = 48.dp,
)

/**
 * Размеры иконок.
 */
@Immutable
data class PermAwareIconSizes(
    /** В строку с подписью. */
    val xs: Dp = 14.dp,
    /** Внутри чипа или компактной строки. */
    val sm: Dp = 16.dp,
    /** По умолчанию: строки списка, кнопки, действия секций. */
    val md: Dp = 18.dp,
    val lg: Dp = 22.dp,
    /** Значки пустых состояний и знакомства с приложением. */
    val xl: Dp = 26.dp,
    /** Кружок, в котором сидит иконка в строке списка. */
    val disc: Dp = 34.dp,
    /** Кружок в пустом состоянии. */
    val discLarge: Dp = 56.dp,
    /** Иконка приложения в строке списка. */
    val appRow: Dp = 40.dp,
    /** Иконка приложения в компактной строке изменений. */
    val appCompact: Dp = 32.dp,
    /** Иконка приложения в шапке экрана приложения. */
    val appHeader: Dp = 56.dp,
)

@Immutable
data class PermAwareShapes(
    val smRadius: Dp = 10.dp,
    val mdRadius: Dp = 16.dp,
    val lgRadius: Dp = 22.dp,
    val xlRadius: Dp = 28.dp,
    val artworkRadius: Dp = 50.dp,
) {
    val sm: CornerBasedShape = RoundedCornerShape(smRadius)
    val md: CornerBasedShape = RoundedCornerShape(mdRadius)
    val lg: CornerBasedShape = RoundedCornerShape(lgRadius)
    val xl: CornerBasedShape = RoundedCornerShape(xlRadius)
    val artwork: CornerBasedShape = RoundedCornerShape(artworkRadius)
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50)
}

@Immutable
data class PermAwareElevation(
    val restingY: Dp = 6.dp,
    val restingBlur: Dp = 16.dp,
    val restingAlpha: Float = 0.07f,
    val floatingY: Dp = 8.dp,
    val floatingBlur: Dp = 24.dp,
    val floatingAlpha: Float = 0.10f,
    val hairline: Dp = 1.dp,
)

val LocalPermAwareIconSizes = staticCompositionLocalOf { PermAwareIconSizes() }
val LocalPermAwareColors = staticCompositionLocalOf { LightPermAwareColors }
val LocalPermAwareSpacing = staticCompositionLocalOf { PermAwareSpacing() }
val LocalPermAwareShapes = staticCompositionLocalOf { PermAwareShapes() }
val LocalPermAwareElevation = staticCompositionLocalOf { PermAwareElevation() }
