package com.unravel.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.blur.HazeBlurStyle

// Canonical Haze Frosted Glass Surface Tokens
// Reference: unravel_master_architecture_blueprint.md Section 1.3 & 1.6

object UnravelGlassConstants {
    val BLUR_RADIUS: Dp = 24.dp
    const val NOISE_FACTOR: Float = 0.04f
    val BORDER_WIDTH: Dp = 1.dp
}

@Immutable
data class UnravelGlass(
    val blurRadius: Dp = UnravelGlassConstants.BLUR_RADIUS,
    val noiseFactor: Float = UnravelGlassConstants.NOISE_FACTOR,
    val containerColor: Color = SurfaceGlass,
    val borderColor: Color = SurfaceGlassBorder,
    val borderWidth: Dp = UnravelGlassConstants.BORDER_WIDTH
) {
    fun toHazeBlurStyle(): HazeBlurStyle = HazeBlurStyle {
        blurRadius(this@UnravelGlass.blurRadius)
        noiseFactor(this@UnravelGlass.noiseFactor)
        backgroundColor(this@UnravelGlass.containerColor)
    }
}

val LocalUnravelGlass = staticCompositionLocalOf { UnravelGlass() }
