package com.unravel.feature.skillmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.unravel.feature.skillmap.model.ClusterBounds
import com.unravel.feature.skillmap.model.MasteryState
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillMapCanvasMathTest {

    @Test
    fun clampScale_enforcesScaleRange() {
        assertEquals(0.5f, SkillMapCanvasMath.clampScale(0.1f), 1e-4f)
        assertEquals(0.5f, SkillMapCanvasMath.clampScale(0.5f), 1e-4f)
        assertEquals(1.2f, SkillMapCanvasMath.clampScale(1.2f), 1e-4f)
        assertEquals(2.5f, SkillMapCanvasMath.clampScale(2.5f), 1e-4f)
        assertEquals(2.5f, SkillMapCanvasMath.clampScale(5.0f), 1e-4f)
    }

    @Test
    fun zoomAroundCentroid_preservesVirtualPointUnderCentroid() {
        val currentPan = Offset(100f, 200f)
        val currentScale = 1.0f
        val newScale = 2.0f
        val centroid = Offset(500f, 600f)

        val virtualPointBefore = Offset(
            (centroid.x - currentPan.x) / currentScale,
            (centroid.y - currentPan.y) / currentScale
        )

        val newPan = SkillMapCanvasMath.zoomAroundCentroid(
            currentPan = currentPan,
            currentScale = currentScale,
            newScale = newScale,
            centroid = centroid
        )

        val virtualPointAfter = Offset(
            (centroid.x - newPan.x) / newScale,
            (centroid.y - newPan.y) / newScale
        )

        assertEquals(virtualPointBefore.x, virtualPointAfter.x, 1e-3f)
        assertEquals(virtualPointBefore.y, virtualPointAfter.y, 1e-3f)
    }

    @Test
    fun clampPan_keepsViewportCenterWithinMapBounds() {
        val viewportSize = Size(1000f, 800f)
        val density = 1.0f
        val mapWidthDp = 2400f
        val mapHeightDp = 1600f
        val scale = 1.0f

        // Try panning excessively to the right (virtual map shifted far right)
        val excessiveRightPan = Offset(2000f, 1500f)
        val clampedRight = SkillMapCanvasMath.clampPan(
            pan = excessiveRightPan,
            scale = scale,
            viewportSize = viewportSize,
            density = density,
            mapWidthDp = mapWidthDp,
            mapHeightDp = mapHeightDp
        )
        // Max pan X is viewportCenterX = 500f
        assertEquals(500f, clampedRight.x, 1e-3f)
        assertEquals(400f, clampedRight.y, 1e-3f)

        // Try panning excessively to the left
        val excessiveLeftPan = Offset(-5000f, -5000f)
        val clampedLeft = SkillMapCanvasMath.clampPan(
            pan = excessiveLeftPan,
            scale = scale,
            viewportSize = viewportSize,
            density = density,
            mapWidthDp = mapWidthDp,
            mapHeightDp = mapHeightDp
        )
        // Min pan X is 500 - 2400 = -1900f
        assertEquals(-1900f, clampedLeft.x, 1e-3f)
        // Min pan Y is 400 - 1600 = -1200f
        assertEquals(-1200f, clampedLeft.y, 1e-3f)
    }

    @Test
    fun findTappedNode_returnsNodeOnDirectHit_andNullOnMiss() {
        val sampleNode = SkillNodeUiModel(
            id = "two_pointers",
            title = "Two Pointers",
            category = "ARRAY",
            clusterIndex = 1,
            x = 300f,
            y = 350f,
            prerequisiteIds = persistentListOf(),
            isUnlocked = true,
            masteryState = MasteryState.MASTERED
        )
        val nodes = listOf(sampleNode)
        val pan = Offset(50f, 100f)
        val scale = 1.0f
        val density = 2.0f

        // Screen position of node center: (pan + xDp * density)
        val nodeScreenCenter = Offset(
            50f + 300f * 2.0f, // 650f
            100f + 350f * 2.0f // 800f
        )

        // Direct hit at center
        val hit = SkillMapCanvasMath.findTappedNode(
            screenTap = nodeScreenCenter,
            pan = pan,
            scale = scale,
            density = density,
            nodes = nodes
        )
        assertNotNull(hit)
        assertEquals("two_pointers", hit?.id)

        // Miss far away
        val miss = SkillMapCanvasMath.findTappedNode(
            screenTap = Offset(50f, 100f),
            pan = pan,
            scale = scale,
            density = density,
            nodes = nodes
        )
        assertNull(miss)
    }

    @Test
    fun computeClusterPan_centersClusterInViewport() {
        val cluster = ClusterBounds(
            clusterIndex = 1,
            title = "LINEAR FOUNDATIONS",
            minX = 100f,
            minY = 100f,
            maxX = 1100f,
            maxY = 700f
        )
        // Cluster center in DP: (600, 400)
        val viewportSize = Size(1000f, 800f)
        val density = 1.0f
        val scale = 1.0f

        val pan = SkillMapCanvasMath.computeClusterPan(
            cluster = cluster,
            scale = scale,
            viewportSize = viewportSize,
            density = density
        )

        // Expected pan: viewportCenter - clusterCenter * scale
        // X: 500 - 600 = -100
        // Y: 400 - 400 = 0
        assertEquals(-100f, pan.x, 1e-3f)
        assertEquals(0f, pan.y, 1e-3f)
    }

    @Test
    fun computeSplineGeometry_tautHasZeroSag_lockedHasPositiveSag() {
        val startCenter = Offset(100f, 200f)
        val endCenter = Offset(500f, 200f)
        val density = 1.0f

        val tautSpline = SkillMapCanvasMath.computeSplineGeometry(
            sourceCenterPx = startCenter,
            targetCenterPx = endCenter,
            isTaut = true,
            density = density
        )
        assertTrue(tautSpline.isTaut)
        // On a horizontal line, taut spline control points have y == start.y
        assertEquals(tautSpline.start.y, tautSpline.control1.y, 1e-3f)
        assertEquals(tautSpline.end.y, tautSpline.control2.y, 1e-3f)

        val lockedSpline = SkillMapCanvasMath.computeSplineGeometry(
            sourceCenterPx = startCenter,
            targetCenterPx = endCenter,
            isTaut = false,
            density = density
        )
        assertFalse(lockedSpline.isTaut)
        // Sag is applied downward (+y)
        assertTrue(lockedSpline.control1.y > lockedSpline.start.y)
        assertEquals(
            SkillMapCanvasMath.SLACK_SAG_DP,
            lockedSpline.control1.y - lockedSpline.start.y,
            1e-3f
        )
    }
}
