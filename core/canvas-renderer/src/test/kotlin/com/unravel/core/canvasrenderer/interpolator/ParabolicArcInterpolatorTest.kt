package com.unravel.core.canvasrenderer.interpolator

import androidx.compose.ui.geometry.Offset
import com.unravel.core.designsystem.theme.UnravelMotionConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class ParabolicArcInterpolatorTest {

    private val arcHeight = 60f
    private val delta = 0.001f

    @Test
    fun `baseline elevation is zero at start and end for both directions`() {
        assertEquals(0f, ParabolicArcInterpolator.calculateArcOffset(true, 0.0f, arcHeight), delta)
        assertEquals(0f, ParabolicArcInterpolator.calculateArcOffset(true, 1.0f, arcHeight), delta)
        assertEquals(0f, ParabolicArcInterpolator.calculateArcOffset(false, 0.0f, arcHeight), delta)
        assertEquals(0f, ParabolicArcInterpolator.calculateArcOffset(false, 1.0f, arcHeight), delta)
    }

    @Test
    fun `peak elevation equals arcHeight at midpoint`() {
        // Left to right travels upper arc (-Y)
        val upperMid = ParabolicArcInterpolator.calculateArcOffset(isLeftToRight = true, progress = 0.5f, arcHeight = arcHeight)
        assertEquals(-arcHeight, upperMid, delta)

        // Right to left travels lower arc (+Y)
        val lowerMid = ParabolicArcInterpolator.calculateArcOffset(isLeftToRight = false, progress = 0.5f, arcHeight = arcHeight)
        assertEquals(arcHeight, lowerMid, delta)
    }

    @Test
    fun `calculatePosition computes correct coordinates across separation distances`() {
        val startX = 100f
        val endX = 300f
        val baseY = 200f
        val source = Offset(startX, baseY)
        val target = Offset(endX, baseY)

        // Progress 0.0
        val pos0 = ParabolicArcInterpolator.calculatePosition(source, target, 0.0f, arcHeight)
        assertEquals(startX, pos0.x, delta)
        assertEquals(baseY, pos0.y, delta)

        // Progress 0.5 (midpoint)
        val posMid = ParabolicArcInterpolator.calculatePosition(source, target, 0.5f, arcHeight)
        assertEquals(200f, posMid.x, delta)
        assertEquals(baseY - arcHeight, posMid.y, delta)

        // Progress 1.0
        val pos1 = ParabolicArcInterpolator.calculatePosition(source, target, 1.0f, arcHeight)
        assertEquals(endX, pos1.x, delta)
        assertEquals(baseY, pos1.y, delta)
    }

    @Test
    fun `swapping elements never overlap in 2D space during transition`() {
        val centerA = Offset(50f, 150f)
        val centerB = Offset(250f, 150f)

        // Test at fine progress increments
        var progress = 0.0f
        while (progress <= 1.0f) {
            val posA = ParabolicArcInterpolator.calculatePosition(centerA, centerB, progress, arcHeight)
            val posB = ParabolicArcInterpolator.calculatePosition(centerB, centerA, progress, arcHeight)

            if (progress > 0.0f && progress < 1.0f) {
                val dx = posA.x - posB.x
                val dy = posA.y - posB.y
                val distance = sqrt(dx * dx + dy * dy)
                assertTrue(
                    distance >= arcHeight,
                    "Swapping elements came too close at progress $progress: distance=$distance"
                )
            }

            // At exact crossover (p = 0.5f), x values are identical, and y separation is exactly 2 * arcHeight
            if (progress == 0.5f) {
                assertEquals(posA.x, posB.x, delta)
                assertEquals(2f * arcHeight, posB.y - posA.y, delta)
            }

            progress += 0.05f
        }
    }

    @Test
    fun `progress is safely clamped to 0 to 1 range`() {
        val offsetNegative = ParabolicArcInterpolator.calculateArcOffset(true, -0.5f, arcHeight)
        val offsetExceeded = ParabolicArcInterpolator.calculateArcOffset(true, 1.5f, arcHeight)

        assertEquals(0f, offsetNegative, delta)
        assertEquals(0f, offsetExceeded, delta)
    }

    @Test
    fun `spring specification matches design system motion tokens`() {
        val spring = ParabolicArcInterpolator.DefaultSpringSpec
        assertEquals(UnravelMotionConstants.DAMPING_RATIO_SPATIAL, spring.dampingRatio, delta)
        assertEquals(UnravelMotionConstants.STIFFNESS_SPATIAL, spring.stiffness, delta)
    }
}
