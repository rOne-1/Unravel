package com.unravel.core.canvasrenderer.interpolator

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import com.unravel.core.designsystem.theme.UnravelMotionConstants

/**
 * Trajectory calculator computing non-colliding over/under parabolic Bézier paths
 * for animated swapping elements on the Canvas.
 *
 * Trajectory formulas:
 *   - Upper parabola (left-to-right): y_upper(p) = -4 * H_arc * p * (1 - p)
 *   - Lower parabola (right-to-left): y_lower(p) = +4 * H_arc * p * (1 - p)
 *
 * Returns primitive inline [Offset] instances to guarantee zero heap allocations
 * during active 60/120fps frame rendering.
 */
object ParabolicArcInterpolator {

    /**
     * Default spring specification for spatial interpolation.
     * Damping ratio = 0.72f, Stiffness = 380f (from design system motion tokens).
     */
    val DefaultSpringSpec: SpringSpec<Float> = spring(
        dampingRatio = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
        stiffness = UnravelMotionConstants.STIFFNESS_SPATIAL
    )

    /**
     * Calculates the 1D vertical elevation offset for progress [progress] in [0.0f, 1.0f].
     *
     * @param isLeftToRight True if traveling from lower X to higher X (upper arc: negative Y).
     * @param progress Normalized animation progress in [0.0f, 1.0f].
     * @param arcHeight Peak vertical height of the parabolic arc.
     * @return Vertical offset in pixels (negative for upper arc, positive for lower arc).
     */
    fun calculateArcOffset(
        isLeftToRight: Boolean,
        progress: Float,
        arcHeight: Float
    ): Float {
        val clampedP = progress.coerceIn(0f, 1f)
        val parabolicFactor = 4f * arcHeight * clampedP * (1f - clampedP)
        return if (isLeftToRight) -parabolicFactor else parabolicFactor
    }

    /**
     * Computes the 2D coordinate [Offset] along the parabolic path.
     *
     * Left-to-right movement traverses the upper parabola (-Y offset).
     * Right-to-left movement traverses the lower parabola (+Y offset).
     *
     * @param sourceCenter The starting 2D center position.
     * @param targetCenter The target 2D center position.
     * @param progress Normalized animation progress in [0.0f, 1.0f].
     * @param arcHeight Peak vertical height of the arc.
     * @return 2D [Offset] at the given animation progress.
     */
    fun calculatePosition(
        sourceCenter: Offset,
        targetCenter: Offset,
        progress: Float,
        arcHeight: Float
    ): Offset {
        val clampedP = progress.coerceIn(0f, 1f)
        val currentX = sourceCenter.x + clampedP * (targetCenter.x - sourceCenter.x)
        val baseY = sourceCenter.y + clampedP * (targetCenter.y - sourceCenter.y)
        val isLeftToRight = targetCenter.x >= sourceCenter.x
        val arcOffsetY = calculateArcOffset(isLeftToRight, clampedP, arcHeight)
        return Offset(currentX, baseY + arcOffsetY)
    }
}
