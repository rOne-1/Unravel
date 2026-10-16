package com.unravel.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

// Spring Physics Constants
// Reference: unravel_master_architecture_blueprint.md Section 1.5

object UnravelMotionConstants {
    const val DAMPING_RATIO_SPATIAL: Float = 0.72f
    const val STIFFNESS_SPATIAL: Float = 380f

    const val DAMPING_RATIO_DRAG_RELEASE: Float = 0.65f
    const val STIFFNESS_DRAG_RELEASE: Float = 500f

    const val DAMPING_RATIO_CALL_STACK: Float = 0.80f
    const val STIFFNESS_CALL_STACK: Float = 420f
}

@Immutable
data class UnravelMotion(
    val dampingRatioSpatial: Float = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
    val stiffnessSpatial: Float = UnravelMotionConstants.STIFFNESS_SPATIAL,
    val dampingRatioDragRelease: Float = UnravelMotionConstants.DAMPING_RATIO_DRAG_RELEASE,
    val stiffnessDragRelease: Float = UnravelMotionConstants.STIFFNESS_DRAG_RELEASE,
    val dampingRatioCallStack: Float = UnravelMotionConstants.DAMPING_RATIO_CALL_STACK,
    val stiffnessCallStack: Float = UnravelMotionConstants.STIFFNESS_CALL_STACK
) {
    fun <T> spatialSpring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = dampingRatioSpatial,
            stiffness = stiffnessSpatial,
            visibilityThreshold = visibilityThreshold
        )

    fun <T> dragReleaseSpring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = dampingRatioDragRelease,
            stiffness = stiffnessDragRelease,
            visibilityThreshold = visibilityThreshold
        )

    fun <T> callStackSpring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = dampingRatioCallStack,
            stiffness = stiffnessCallStack,
            visibilityThreshold = visibilityThreshold
        )
}

val LocalUnravelMotion = staticCompositionLocalOf { UnravelMotion() }
