package com.unravel.feature.skillmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.unravel.feature.skillmap.layout.SkillMapLayoutCoordinates
import com.unravel.feature.skillmap.model.ClusterBounds
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Geometric calculation result for a prerequisite spline connecting two nodes.
 */
data class SplineGeometry(
    val start: Offset,
    val end: Offset,
    val control1: Offset,
    val control2: Offset,
    val arrowWing1: Offset,
    val arrowWing2: Offset,
    val isTaut: Boolean
)

/**
 * Pure mathematical functions for coordinate projections, gesture clamping,
 * and spline geometry on the 2D Skill Map canvas.
 * Fully testable without Compose UI instrumentation.
 */
object SkillMapCanvasMath {
    const val MIN_SCALE: Float = 0.5f
    const val MAX_SCALE: Float = 2.5f

    const val NODE_WIDTH_DP: Float = 140f
    const val NODE_HEIGHT_DP: Float = 64f
    const val HIT_PADDING_DP: Float = 12f
    const val SLACK_SAG_DP: Float = 36f
    const val ARROW_LENGTH_DP: Float = 8f
    const val ARROW_WING_DP: Float = 5f

    /**
     * Clamps scale between [MIN_SCALE] (0.5x) and [MAX_SCALE] (2.5x).
     */
    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * Preserves the virtual point under the centroid when changing scale.
     */
    fun zoomAroundCentroid(
        currentPan: Offset,
        currentScale: Float,
        newScale: Float,
        centroid: Offset
    ): Offset {
        if (currentScale == 0f) return currentPan
        val scaleRatio = newScale / currentScale
        return Offset(
            x = centroid.x - (centroid.x - currentPan.x) * scaleRatio,
            y = centroid.y - (centroid.y - currentPan.y) * scaleRatio
        )
    }

    /**
     * Clamps pan translation so the viewport center never drifts outside the virtual map bounds.
     */
    fun clampPan(
        pan: Offset,
        scale: Float,
        viewportSize: Size,
        density: Float,
        mapWidthDp: Float = SkillMapLayoutCoordinates.MAP_WIDTH,
        mapHeightDp: Float = SkillMapLayoutCoordinates.MAP_HEIGHT
    ): Offset {
        if (viewportSize.width <= 0f || viewportSize.height <= 0f || density <= 0f) {
            return pan
        }

        val mapWidthPx = mapWidthDp * density
        val mapHeightPx = mapHeightDp * density
        val scaledWidth = mapWidthPx * scale
        val scaledHeight = mapHeightPx * scale

        val viewportCenterX = viewportSize.width / 2f
        val viewportCenterY = viewportSize.height / 2f

        // Center virtual coordinates must remain within [0, mapWidthPx] and [0, mapHeightPx]
        val minPanX = viewportCenterX - scaledWidth
        val maxPanX = viewportCenterX
        val minPanY = viewportCenterY - scaledHeight
        val maxPanY = viewportCenterY

        val clampedX = if (minPanX <= maxPanX) {
            pan.x.coerceIn(minPanX, maxPanX)
        } else {
            (viewportSize.width - scaledWidth) / 2f
        }

        val clampedY = if (minPanY <= maxPanY) {
            pan.y.coerceIn(minPanY, maxPanY)
        } else {
            (viewportSize.height - scaledHeight) / 2f
        }

        return Offset(clampedX, clampedY)
    }

    /**
     * Converts a screen coordinate to virtual map DP coordinate.
     */
    fun screenToVirtualDp(
        screenOffset: Offset,
        pan: Offset,
        scale: Float,
        density: Float
    ): Offset {
        if (scale == 0f || density == 0f) return Offset.Zero
        val virtualPxX = (screenOffset.x - pan.x) / scale
        val virtualPxY = (screenOffset.y - pan.y) / scale
        return Offset(virtualPxX / density, virtualPxY / density)
    }

    /**
     * Determines which skill node was tapped given a screen tap position.
     */
    fun findTappedNode(
        screenTap: Offset,
        pan: Offset,
        scale: Float,
        density: Float,
        nodes: List<SkillNodeUiModel>
    ): SkillNodeUiModel? {
        val virtualDp = screenToVirtualDp(screenTap, pan, scale, density)
        val halfW = NODE_WIDTH_DP / 2f + HIT_PADDING_DP
        val halfH = NODE_HEIGHT_DP / 2f + HIT_PADDING_DP

        var closestNode: SkillNodeUiModel? = null
        var minDistanceSq = Float.MAX_VALUE

        for (node in nodes) {
            val dx = virtualDp.x - node.x
            val dy = virtualDp.y - node.y
            if (dx >= -halfW && dx <= halfW && dy >= -halfH && dy <= halfH) {
                val distSq = dx * dx + dy * dy
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq
                    closestNode = node
                }
            }
        }

        return closestNode
    }

    /**
     * Computes the target pan offset required to center the camera on a given cluster.
     */
    fun computeClusterPan(
        cluster: ClusterBounds,
        scale: Float,
        viewportSize: Size,
        density: Float
    ): Offset {
        val clusterCenterX = (cluster.minX + cluster.maxX) / 2f * density
        val clusterCenterY = (cluster.minY + cluster.maxY) / 2f * density
        val rawPan = Offset(
            x = viewportSize.width / 2f - clusterCenterX * scale,
            y = viewportSize.height / 2f - clusterCenterY * scale
        )
        return clampPan(rawPan, scale, viewportSize, density)
    }

    /**
     * Computes the target pan offset required to center the camera on a specific node.
     */
    fun computeNodePan(
        nodeXDp: Float,
        nodeYDp: Float,
        scale: Float,
        viewportSize: Size,
        density: Float
    ): Offset {
        val nodeCenterPx = Offset(nodeXDp * density, nodeYDp * density)
        val rawPan = Offset(
            x = viewportSize.width / 2f - nodeCenterPx.x * scale,
            y = viewportSize.height / 2f - nodeCenterPx.y * scale
        )
        return clampPan(rawPan, scale, viewportSize, density)
    }

    /**
     * Computes the spline geometry and directional arrowhead between two node centers.
     */
    fun computeSplineGeometry(
        sourceCenterPx: Offset,
        targetCenterPx: Offset,
        isTaut: Boolean,
        density: Float
    ): SplineGeometry {
        val dx = targetCenterPx.x - sourceCenterPx.x
        val dy = targetCenterPx.y - sourceCenterPx.y
        val distance = sqrt(dx * dx + dy * dy)

        val ux = if (distance > 0f) dx / distance else 1f
        val uy = if (distance > 0f) dy / distance else 0f
        val vx = -uy
        val vy = ux

        val halfW = (NODE_WIDTH_DP / 2f) * density
        val halfH = (NODE_HEIGHT_DP / 2f) * density

        val start = Offset(
            x = sourceCenterPx.x + ux * halfW,
            y = sourceCenterPx.y + uy * halfH
        )
        val end = Offset(
            x = targetCenterPx.x - ux * halfW,
            y = targetCenterPx.y - uy * halfH
        )

        val spanX = end.x - start.x
        val sag = if (isTaut) 0f else SLACK_SAG_DP * density

        val control1 = Offset(
            x = start.x + spanX * 0.35f,
            y = start.y + sag
        )
        val control2 = Offset(
            x = end.x - spanX * 0.35f,
            y = end.y + sag
        )

        val arrowLen = ARROW_LENGTH_DP * density
        val arrowWing = ARROW_WING_DP * density
        val arrowWing1 = Offset(
            x = end.x - ux * arrowLen + vx * arrowWing,
            y = end.y - uy * arrowLen + vy * arrowWing
        )
        val arrowWing2 = Offset(
            x = end.x - ux * arrowLen - vx * arrowWing,
            y = end.y - uy * arrowLen - vy * arrowWing
        )

        return SplineGeometry(
            start = start,
            end = end,
            control1 = control1,
            control2 = control2,
            arrowWing1 = arrowWing1,
            arrowWing2 = arrowWing2,
            isTaut = isTaut
        )
    }
}
