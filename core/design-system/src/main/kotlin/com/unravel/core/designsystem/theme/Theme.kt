package com.unravel.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Dark color scheme mapped from Unravel design tokens
// Reference: unravel_master_architecture_blueprint.md Section 1.3
private val DarkColorScheme = darkColorScheme(
    primary = NeonMint,
    onPrimary = CanvasVoid,
    secondary = ElectricVioletBright,
    onSecondary = CanvasVoid,
    tertiary = CoralFlame,
    onTertiary = CanvasVoid,
    background = CanvasVoid,
    onBackground = TextPrimary,
    surface = SurfaceGlass,
    onSurface = TextPrimary,
    surfaceVariant = StructuralSlate,
    onSurfaceVariant = TextTelemetry,
    outline = SurfaceGlassBorder
)

@Composable
fun UnravelTheme(
    colors: UnravelColors = UnravelColors(),
    typography: UnravelTypography = UnravelTypography(),
    motion: UnravelMotion = UnravelMotion(),
    glass: UnravelGlass = UnravelGlass(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalUnravelColors provides colors,
        LocalUnravelTypography provides typography,
        LocalUnravelMotion provides motion,
        LocalUnravelGlass provides glass
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

object UnravelTheme {
    val colors: UnravelColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUnravelColors.current

    val typography: UnravelTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalUnravelTypography.current

    val motion: UnravelMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalUnravelMotion.current

    val glass: UnravelGlass
        @Composable
        @ReadOnlyComposable
        get() = LocalUnravelGlass.current
}
