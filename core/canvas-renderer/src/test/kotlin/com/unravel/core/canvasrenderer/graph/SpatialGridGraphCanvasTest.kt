package com.unravel.core.canvasrenderer.graph

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.SpatialMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SpatialGridGraphCanvasTest {

    @Test
    @DisplayName("SpatialGrid: Grid state contains valid orthogonal layout and obstacle flags")
    fun testGridStateCalculations() {
        val rows = 4
        val cols = 4
        val obstacles = setOf(Pair(1, 1), Pair(2, 2))

        val state = GraphLayoutEngine.layoutGrid(rows, cols, obstacles)

        assertEquals(SpatialMode.GRID_2D, state.mode)
        assertEquals(16, state.vertices.size)

        val obstacleCount = state.vertices.count { it.isObstacle }
        assertEquals(2, obstacleCount, "Should flag exactly 2 obstacle vertices")

        // Start and target IDs are set correctly
        assertTrue(state.startVertexId.isNotEmpty())
        assertTrue(state.targetVertexId.isNotEmpty())
    }

    @Test
    @DisplayName("SpatialGrid: Free graph assigns valid normalized positions")
    fun testFreeGraphLayout() {
        val v1 = com.unravel.core.engine.model.SpatialVertex(label = "A")
        val v2 = com.unravel.core.engine.model.SpatialVertex(label = "B")
        val v3 = com.unravel.core.engine.model.SpatialVertex(label = "C")

        val edge1 = com.unravel.core.engine.model.SpatialEdge(fromId = v1.id, toId = v2.id)
        val edge2 = com.unravel.core.engine.model.SpatialEdge(fromId = v2.id, toId = v3.id)

        val state = GraphLayoutEngine.layoutFreeGraph(listOf(v1, v2, v3), listOf(edge1, edge2))

        assertEquals(SpatialMode.FREE_GRAPH, state.mode)
        assertEquals(3, state.vertices.size)
        assertEquals(2, state.edges.size)

        state.vertices.forEach { v ->
            assertTrue(v.normX in 0f..1f)
            assertTrue(v.normY in 0f..1f)
        }
    }
}
