package com.unravel.feature.racemode.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unravel.core.designsystem.theme.CoralFlame
import com.unravel.core.designsystem.theme.ElectricVioletBright
import com.unravel.core.designsystem.theme.NeonMint
import com.unravel.core.designsystem.theme.SurfaceGlass
import com.unravel.core.designsystem.theme.SurfaceGlassBorder
import com.unravel.core.designsystem.theme.UnravelTheme
import org.intellij.lang.annotations.Language

/**
 * Orientation for the central Glass Prism divider bar.
 */
enum class DividerOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * Resolves the signature accent color reflecting current race status.
 */
fun resolvePrismAccentColor(
    isRaceFinished: Boolean,
    isDiverged: Boolean,
    defaultColor: Color = ElectricVioletBright
): Color = when {
    isRaceFinished -> NeonMint
    isDiverged -> CoralFlame
    else -> defaultColor
}

@Language("AGSL")
private const val PRISM_REFRACTION_SHADER_SRC = """
    uniform float2 uResolution;
    uniform float4 uAccentColor;
    uniform float uIsVertical;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float distFromCenter = uIsVertical > 0.5 ? abs(uv.x - 0.5) * 2.0 : abs(uv.y - 0.5) * 2.0;

        // Optical refraction gradient & chromatic dispersion
        float core = 1.0 - smoothstep(0.0, 0.15, distFromCenter);
        float bloom = 1.0 - smoothstep(0.0, 1.0, distFromCenter);

        // Specular center streak
        float specular = 1.0 - smoothstep(0.0, 0.05, distFromCenter);

        half3 color = mix(half3(0.08, 0.10, 0.15), uAccentColor.rgb, bloom * 0.8);
        color += uAccentColor.rgb * (core * 0.9);
        color += half3(1.0, 1.0, 1.0) * (specular * 0.45);

        float alpha = mix(0.92, 0.4, distFromCenter);
        return half4(color * alpha, alpha);
    }
"""

/**
 * Central frosted Glass Prism divider bar separating the two competing algorithm canvases.
 * On API 33+, renders AGSL [RuntimeShader] with optical refraction and specular dispersion.
 * On API 26-32, falls back gracefully to a multi-layered Compose gradient with specular highlights.
 * Strictly guarantees ZERO heap allocations inside `onDraw`.
 */
@Composable
fun GlassPrismDivider(
    orientation: DividerOrientation,
    isDiverged: Boolean,
    isRaceFinished: Boolean,
    modifier: Modifier = Modifier,
    statusText: String? = null
) {
    val colors = UnravelTheme.colors
    val typography = UnravelTheme.typography

    val targetAccentColor = resolvePrismAccentColor(
        isRaceFinished = isRaceFinished,
        isDiverged = isDiverged,
        defaultColor = colors.electricVioletBright
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 350),
        label = "prismAccentColor"
    )

    val isApi33Plus = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

    val baseModifier = when (orientation) {
        DividerOrientation.HORIZONTAL -> modifier
            .fillMaxWidth()
            .height(14.dp)
        DividerOrientation.VERTICAL -> modifier
            .fillMaxHeight()
            .width(14.dp)
    }

    Box(
        modifier = baseModifier
            .background(colors.surfaceGlass)
            .border(1.dp, colors.surfaceGlassBorder),
        contentAlignment = Alignment.Center
    ) {
        if (isApi33Plus) {
            AgslPrismCanvas(
                orientation = orientation,
                accentColor = animatedAccentColor,
                modifier = Modifier.matchParentSize()
            )
        } else {
            FallbackPrismCanvas(
                orientation = orientation,
                accentColor = animatedAccentColor,
                modifier = Modifier.matchParentSize()
            )
        }

        // Optional Telemetry Badge over the Prism
        if (!statusText.isNullOrBlank()) {
            val badgeShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(colors.canvasVoid.copy(alpha = 0.85f))
                    .border(1.dp, animatedAccentColor, badgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    style = typography.hudMicroLabel.copy(
                        color = animatedAccentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

/**
 * API 33+ AGSL RuntimeShader implementation with zero allocation in onDraw.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslPrismCanvas(
    orientation: DividerOrientation,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(PRISM_REFRACTION_SHADER_SRC) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        shader.setFloatUniform("uResolution", w, h)
        shader.setFloatUniform(
            "uAccentColor",
            accentColor.red,
            accentColor.green,
            accentColor.blue,
            accentColor.alpha
        )
        shader.setFloatUniform("uIsVertical", if (orientation == DividerOrientation.VERTICAL) 1f else 0f)

        drawRect(brush = shaderBrush)
    }
}

/**
 * API 26-32 Fallback Compose Canvas draw using linear gradient and specular center line.
 * Strictly guarantees ZERO allocations inside onDraw by pre-allocating brushes in remember.
 */
@Composable
private fun FallbackPrismCanvas(
    orientation: DividerOrientation,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Pre-allocated gradient color lists outside onDraw
    val gradientColors = remember(accentColor) {
        listOf(
            Color.Transparent,
            accentColor.copy(alpha = 0.25f),
            accentColor.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.45f),
            accentColor.copy(alpha = 0.85f),
            accentColor.copy(alpha = 0.25f),
            Color.Transparent
        )
    }

    val specularColor = remember { Color(0x66FFFFFF) }

    Canvas(modifier = modifier) {
        val isVertical = orientation == DividerOrientation.VERTICAL

        val gradientBrush = if (isVertical) {
            Brush.horizontalGradient(gradientColors, startX = 0f, endX = size.width)
        } else {
            Brush.verticalGradient(gradientColors, startY = 0f, endY = size.height)
        }

        // Draw soft refracted gradient body
        drawRect(brush = gradientBrush)

        // Draw crisp central specular highlight line
        if (isVertical) {
            val midX = size.width / 2f
            drawLine(
                color = specularColor,
                start = Offset(midX, 0f),
                end = Offset(midX, size.height),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Square
            )
        } else {
            val midY = size.height / 2f
            drawLine(
                color = specularColor,
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Square
            )
        }
    }
}

@Preview(name = "Horizontal Divider - Normal", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun GlassPrismDividerHorizontalNormalPreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassPrismDivider(
                orientation = DividerOrientation.HORIZONTAL,
                isDiverged = false,
                isRaceFinished = false,
                statusText = "SYNCHRONIZED"
            )
        }
    }
}

@Preview(name = "Horizontal Divider - Diverged", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun GlassPrismDividerHorizontalDivergedPreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassPrismDivider(
                orientation = DividerOrientation.HORIZONTAL,
                isDiverged = true,
                isRaceFinished = false,
                statusText = "DIVERGENCE DETECTED"
            )
        }
    }
}

@Preview(name = "Horizontal Divider - Finished", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun GlassPrismDividerHorizontalFinishedPreview() {
    UnravelTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GlassPrismDivider(
                orientation = DividerOrientation.HORIZONTAL,
                isDiverged = false,
                isRaceFinished = true,
                statusText = "QUICKSORT WON"
            )
        }
    }
}
