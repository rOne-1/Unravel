package com.unravel.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeTokensTest {

    @Test
    fun verifyCanonicalColors() {
        val colors = UnravelColors()

        assertEquals(Color(0xFF0B0E14), colors.canvasVoid)
        assertEquals(Color(0x99161B26), colors.surfaceGlass)
        assertEquals(Color(0x1FFFFFFF), colors.surfaceGlassBorder)
        assertEquals(Color(0xFF262F40), colors.structuralSlate)
        assertEquals(Color(0xFF1A202C), colors.structuralSlateMuted)
        assertEquals(Color(0xFF00F5A0), colors.neonMint)
        assertEquals(Color(0xFF7B2CBF), colors.electricVioletDeep)
        assertEquals(Color(0xFF9D4EDD), colors.electricVioletBright)
        assertEquals(Color(0xFFFF4365), colors.coralFlame)
        assertEquals(Color(0xFFF0F4F8), colors.textPrimary)
        assertEquals(Color(0xFF8A99AD), colors.textTelemetry)
    }

    @Test
    fun verifyMotionPhysicsConstants() {
        assertEquals(0.72f, UnravelMotionConstants.DAMPING_RATIO_SPATIAL)
        assertEquals(380f, UnravelMotionConstants.STIFFNESS_SPATIAL)

        assertEquals(0.65f, UnravelMotionConstants.DAMPING_RATIO_DRAG_RELEASE)
        assertEquals(500f, UnravelMotionConstants.STIFFNESS_DRAG_RELEASE)

        assertEquals(0.80f, UnravelMotionConstants.DAMPING_RATIO_CALL_STACK)
        assertEquals(420f, UnravelMotionConstants.STIFFNESS_CALL_STACK)

        val motion = UnravelMotion()
        val spatial = motion.spatialSpring<Float>()
        assertEquals(0.72f, spatial.dampingRatio)
        assertEquals(380f, spatial.stiffness)

        val dragRelease = motion.dragReleaseSpring<Float>()
        assertEquals(0.65f, dragRelease.dampingRatio)
        assertEquals(500f, dragRelease.stiffness)

        val callStack = motion.callStackSpring<Float>()
        assertEquals(0.80f, callStack.dampingRatio)
        assertEquals(420f, callStack.stiffness)
    }

    @Test
    fun verifyGlassTokens() {
        val glass = UnravelGlass()
        assertEquals(24.dp, glass.blurRadius)
        assertEquals(0.04f, glass.noiseFactor)
        assertEquals(SurfaceGlass, glass.containerColor)
        assertEquals(SurfaceGlassBorder, glass.borderColor)
        assertEquals(1.dp, glass.borderWidth)

        val hazeStyle = glass.toHazeBlurStyle()
        assertNotNull(hazeStyle)
    }

    @Test
    fun verifyTypographyTokens() {
        val typography = UnravelTypography()

        // Telemetry monospace scale uses tabular figures
        assertEquals(FontFamily.Monospace, typography.telemetryHero.fontFamily)
        assertEquals("tnum", typography.telemetryHero.fontFeatureSettings)

        assertEquals(FontFamily.Monospace, typography.nodeValueLabel.fontFamily)
        assertEquals("tnum", typography.nodeValueLabel.fontFeatureSettings)

        assertEquals(FontFamily.Monospace, typography.codeXRayBody.fontFamily)
        assertEquals("tnum", typography.codeXRayBody.fontFeatureSettings)

        assertEquals(FontFamily.Monospace, typography.hudMicroLabel.fontFamily)
        assertEquals("tnum", typography.hudMicroLabel.fontFeatureSettings)

        // UI scale
        assertEquals(FontFamily.Default, typography.displayHeader.fontFamily)
        assertEquals(FontFamily.Default, typography.titleMedium.fontFamily)
        assertEquals(FontFamily.Default, typography.bodyMedium.fontFamily)
        assertEquals(FontFamily.Default, typography.labelMedium.fontFamily)
    }
}
