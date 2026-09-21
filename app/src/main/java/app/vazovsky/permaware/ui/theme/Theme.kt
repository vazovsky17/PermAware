package app.vazovsky.permaware.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import app.vazovsky.permaware.data.prefs.ThemeMode

@Composable
fun PermAwareTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkPermAwareColors else LightPermAwareColors

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = Violet30,
            onPrimaryContainer = Violet90,
            secondary = colors.secondary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.surfaceMuted,
            onSecondaryContainer = colors.onSurfaceMuted,
            tertiary = colors.secondary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = colors.background,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.elevatedSurface,
            surfaceContainerHighest = colors.elevatedSurface,
            outline = colors.outline,
            outlineVariant = colors.divider,
            error = colors.danger,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = colors.danger,
            scrim = DarkBackground,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = Violet90,
            onPrimaryContainer = Violet20,
            secondary = colors.secondary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.surfaceMuted,
            onSecondaryContainer = colors.onSurfaceMuted,
            tertiary = colors.secondary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceMuted,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = colors.elevatedSurface,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.elevatedSurface,
            surfaceContainerHighest = colors.elevatedSurface,
            outline = colors.outline,
            outlineVariant = colors.divider,
            error = colors.danger,
            onError = OnErrorLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = colors.danger,
            scrim = LightTextPrimary,
        )
    }

    val shapes = PermAwareShapes()
    CompositionLocalProvider(
        LocalPermAwareColors provides colors,
        LocalPermAwareSpacing provides PermAwareSpacing(),
        LocalPermAwareShapes provides shapes,
        LocalPermAwareIconSizes provides PermAwareIconSizes(),
        LocalPermAwareElevation provides PermAwareElevation(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PermAwareTypography,
            shapes = Shapes(
                extraSmall = shapes.sm,
                small = shapes.sm,
                medium = shapes.md,
                large = shapes.lg,
                extraLarge = shapes.xl,
            ),
            content = content,
        )
    }
}

object PermAwareTheme {
    val colors: PermAwareColors
        @Composable @ReadOnlyComposable
        get() = LocalPermAwareColors.current

    val spacing: PermAwareSpacing
        @Composable @ReadOnlyComposable
        get() = LocalPermAwareSpacing.current

    val shapes: PermAwareShapes
        @Composable @ReadOnlyComposable
        get() = LocalPermAwareShapes.current

    val elevation: PermAwareElevation
        @Composable @ReadOnlyComposable
        get() = LocalPermAwareElevation.current

    val icons: PermAwareIconSizes
        @Composable @ReadOnlyComposable
        get() = LocalPermAwareIconSizes.current

    val typography = PermAwareType
}
