package com.unravel.feature.skillmap.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.unravel.core.designsystem.theme.UnravelMotionConstants
import com.unravel.feature.skillmap.model.ClusterBounds
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder managing 2D pan/zoom viewport transformations, boundary clamping,
 * and smooth spring camera animations.
 */
@Stable
class SkillMapCanvasState(
    initialScale: Float = 1.0f,
    initialPan: Offset = Offset.Zero,
    private val coroutineScope: CoroutineScope? = null
) {
    var scale by mutableFloatStateOf(initialScale)
        private set

    var panOffset by mutableStateOf(initialPan)
        private set

    var viewportSize by mutableStateOf(Size.Zero)
        internal set

    var density by mutableFloatStateOf(1f)
        internal set

    private val panAnimatable = Animatable(initialPan, Offset.VectorConverter)
    private val scaleAnimatable = Animatable(initialScale)

    /**
     * Updates pan and zoom during multi-touch pinch or single-finger drag.
     */
    fun onPanZoom(centroid: Offset, pan: Offset, zoomChange: Float) {
        val newScale = SkillMapCanvasMath.clampScale(scale * zoomChange)
        val zoomedPan = SkillMapCanvasMath.zoomAroundCentroid(
            currentPan = panOffset,
            currentScale = scale,
            newScale = newScale,
            centroid = centroid
        )
        val combinedPan = zoomedPan + pan
        val clampedPan = SkillMapCanvasMath.clampPan(
            pan = combinedPan,
            scale = newScale,
            viewportSize = viewportSize,
            density = density
        )
        scale = newScale
        panOffset = clampedPan
    }

    /**
     * Smoothly animates the camera to center on a specific cluster territory.
     */
    fun animateToCluster(cluster: ClusterBounds) {
        val targetPan = SkillMapCanvasMath.computeClusterPan(
            cluster = cluster,
            scale = scale,
            viewportSize = viewportSize,
            density = density
        )
        animateTo(targetPan = targetPan, targetScale = scale)
    }

    /**
     * Smoothly animates the camera to center on a specific skill node.
     */
    fun animateToNode(xDp: Float, yDp: Float) {
        val targetPan = SkillMapCanvasMath.computeNodePan(
            nodeXDp = xDp,
            nodeYDp = yDp,
            scale = scale,
            viewportSize = viewportSize,
            density = density
        )
        animateTo(targetPan = targetPan, targetScale = scale)
    }

    /**
     * Programmatic spring animation to target pan and scale.
     */
    fun animateTo(targetPan: Offset, targetScale: Float = scale) {
        coroutineScope?.launch {
            val clampedPan = SkillMapCanvasMath.clampPan(
                pan = targetPan,
                scale = targetScale,
                viewportSize = viewportSize,
                density = density
            )
            panAnimatable.snapTo(panOffset)
            scaleAnimatable.snapTo(scale)

            launch {
                scaleAnimatable.animateTo(
                    targetValue = SkillMapCanvasMath.clampScale(targetScale),
                    animationSpec = spring(
                        dampingRatio = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
                        stiffness = UnravelMotionConstants.STIFFNESS_SPATIAL
                    )
                ) {
                    scale = value
                }
            }

            launch {
                panAnimatable.animateTo(
                    targetValue = clampedPan,
                    animationSpec = spring(
                        dampingRatio = UnravelMotionConstants.DAMPING_RATIO_SPATIAL,
                        stiffness = UnravelMotionConstants.STIFFNESS_SPATIAL
                    )
                ) {
                    panOffset = value
                }
            }
        }
    }

    /**
     * Immediately snaps the camera to center on a specific cluster territory without animation.
     */
    fun snapToCluster(cluster: ClusterBounds) {
        val targetPan = SkillMapCanvasMath.computeClusterPan(
            cluster = cluster,
            scale = scale,
            viewportSize = viewportSize,
            density = density
        )
        panOffset = targetPan
    }

    /**
     * Determines which skill node was tapped given a screen tap position.
     */
    fun findTappedNode(screenTap: Offset, nodes: List<SkillNodeUiModel>): SkillNodeUiModel? {
        return SkillMapCanvasMath.findTappedNode(
            screenTap = screenTap,
            pan = panOffset,
            scale = scale,
            density = density,
            nodes = nodes
        )
    }

    /**
     * Direct setter for testing / initialization.
     */
    fun setTransform(pan: Offset, targetScale: Float) {
        scale = SkillMapCanvasMath.clampScale(targetScale)
        panOffset = SkillMapCanvasMath.clampPan(
            pan = pan,
            scale = scale,
            viewportSize = viewportSize,
            density = density
        )
    }
}

@Composable
fun rememberSkillMapCanvasState(
    initialScale: Float = 1.0f,
    initialPan: Offset = Offset.Zero
): SkillMapCanvasState {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        SkillMapCanvasState(
            initialScale = initialScale,
            initialPan = initialPan,
            coroutineScope = coroutineScope
        )
    }
}
