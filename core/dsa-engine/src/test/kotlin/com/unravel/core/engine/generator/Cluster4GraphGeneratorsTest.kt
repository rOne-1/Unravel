package com.unravel.core.engine.generator

import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.SpatialMode
import com.unravel.core.engine.model.VisualStructureState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class Cluster4GraphGeneratorsTest {

    @Test
    @DisplayName("GraphLayoutEngine: Grid layout assigns normalized coordinates in [0.05, 0.95] and marks obstacles")
    fun testGridLayoutProperties() {
        val rows = 4
        val cols = 5
        val obstacles = setOf(Pair(1, 1), Pair(2, 2))

        val state = GraphLayoutEngine.layoutGrid(rows, cols, obstacles)

        assertEquals(20, state.vertices.size)
        assertEquals(SpatialMode.GRID_2D, state.mode)

        state.vertices.forEach { vertex ->
            assertTrue(vertex.normX in 0.05f..0.95f, "normX should be in [0.05, 0.95]")
            assertTrue(vertex.normY in 0.05f..0.95f, "normY should be in [0.05, 0.95]")
            if (obstacles.contains(Pair(vertex.gridRow, vertex.gridCol))) {
                assertTrue(vertex.isObstacle, "Vertex at (${vertex.gridRow}, ${vertex.gridCol}) should be obstacle")
            } else {
                assertFalse(vertex.isObstacle, "Vertex at (${vertex.gridRow}, ${vertex.gridCol}) should not be obstacle")
            }
        }
    }

    @Test
    @DisplayName("GraphBfsDfs: BFS visits reachable vertices and excludes obstacles")
    fun testBfsObstacleAvoidanceAndPath() {
        val obstacles = setOf(Pair(1, 0), Pair(1, 1), Pair(1, 2)) // Wall blocking top from bottom
        val gridState = GraphLayoutEngine.layoutGrid(
            rows = 3,
            cols = 4,
            obstacles = obstacles,
            startCoord = Pair(0, 0),
            targetCoord = Pair(2, 0)
        )

        val bfsGen = GraphBfsDfsGenerator(useDfs = false)
        val frames = bfsGen.generateFrames(gridState)

        assertTrue(frames.isNotEmpty())
        assertTrue(frames.size <= AlgorithmGenerator.MAX_FRAMES)

        // Verify ExpandFrontierNode actions are emitted
        val expandActions = frames.filter { it.expectedUserAction is ExpectedAction.ExpandFrontierNode }
        assertTrue(expandActions.isNotEmpty())

        val finalFrame = frames.last()
        val finalState = finalFrame.structureState as VisualStructureState.SpatialGridGraphState

        // Verify obstacle vertices are strictly excluded from visited set
        val obstacleIds = gridState.vertices.filter { it.isObstacle }.map { it.id.toString() }.toSet()
        val visitedIds = finalState.visitedIds
        assertTrue(obstacleIds.intersect(visitedIds).isEmpty(), "Obstacles must never be in visited set")

        // Verify path discovered reaches target around the wall (via col 3)
        assertTrue(finalState.shortestPathIds.isNotEmpty(), "Path should be found around obstacle wall")
        assertEquals(gridState.startVertexId, finalState.shortestPathIds.first())
        assertEquals(gridState.targetVertexId, finalState.shortestPathIds.last())
        assertEquals(HapticCue.SUCCESS_LOCK, finalFrame.hapticCue)
    }

    @Test
    @DisplayName("DijkstraAStar: Dijkstra and A* find optimal path, and A* visits <= nodes than Dijkstra")
    fun testDijkstraAndAStarOptimalityAndEfficiency() {
        val gridState = GraphLayoutEngine.layoutGrid(
            rows = 6,
            cols = 6,
            obstacles = setOf(Pair(2, 1), Pair(2, 2), Pair(2, 3), Pair(2, 4)),
            startCoord = Pair(0, 0),
            targetCoord = Pair(5, 5)
        )

        val dijkstraGen = DijkstraAStarGenerator(useAStar = false)
        val aStarGen = DijkstraAStarGenerator(useAStar = true)

        val dijkstraFrames = dijkstraGen.generateFrames(gridState)
        val aStarFrames = aStarGen.generateFrames(gridState)

        val dijkstraFinal = dijkstraFrames.last().structureState as VisualStructureState.SpatialGridGraphState
        val aStarFinal = aStarFrames.last().structureState as VisualStructureState.SpatialGridGraphState

        // Both find the same optimal path length
        assertEquals(dijkstraFinal.shortestPathIds.size, aStarFinal.shortestPathIds.size, "Both algorithms must find the optimal path length")
        assertTrue(dijkstraFinal.shortestPathIds.isNotEmpty(), "Dijkstra must find path")
        assertTrue(aStarFinal.shortestPathIds.isNotEmpty(), "A* must find path")

        // Both have distance to target identical
        val dijkstraDist = dijkstraFinal.distanceMap[gridState.targetVertexId]
        val aStarDist = aStarFinal.distanceMap[gridState.targetVertexId]
        assertEquals(dijkstraDist, aStarDist, "Path cost must be identical and optimal")

        // A* visits <= nodes than Dijkstra on grid
        val dijkstraVisitedCount = dijkstraFinal.visitedIds.size
        val aStarVisitedCount = aStarFinal.visitedIds.size
        assertTrue(
            aStarVisitedCount <= dijkstraVisitedCount,
            "A* should visit fewer or equal nodes than Dijkstra ($aStarVisitedCount <= $dijkstraVisitedCount)"
        )
    }

    @Test
    @DisplayName("Cluster 4 Graphs: Determinism across all graph generators")
    fun testCluster4GraphDeterminism() {
        val bfsGen = GraphBfsDfsGenerator(useDfs = false)
        val dfsGen = GraphBfsDfsGenerator(useDfs = true)
        val dijGen = DijkstraAStarGenerator(useAStar = false)
        val aStarGen = DijkstraAStarGenerator(useAStar = true)

        val gridState = GraphLayoutEngine.layoutGrid(rows = 3, cols = 3)

        val run1Bfs = bfsGen.generateFrames(gridState)
        val run2Bfs = bfsGen.generateFrames(gridState)
        assertEquals(run1Bfs.size, run2Bfs.size)

        val run1Dfs = dfsGen.generateFrames(gridState)
        val run2Dfs = dfsGen.generateFrames(gridState)
        assertEquals(run1Dfs.size, run2Dfs.size)

        val run1Dij = dijGen.generateFrames(gridState)
        val run2Dij = dijGen.generateFrames(gridState)
        assertEquals(run1Dij.size, run2Dij.size)

        val run1AStar = aStarGen.generateFrames(gridState)
        val run2AStar = aStarGen.generateFrames(gridState)
        assertEquals(run1AStar.size, run2AStar.size)
    }
}
