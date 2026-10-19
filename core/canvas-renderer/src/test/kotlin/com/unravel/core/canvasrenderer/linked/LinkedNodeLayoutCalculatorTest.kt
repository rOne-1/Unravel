package com.unravel.core.canvasrenderer.linked

import com.unravel.core.engine.model.LinkedNode
import com.unravel.core.engine.model.VisualStructureState.LinkedNodeState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LinkedNodeLayoutCalculatorTest {

    @Test
    @DisplayName("Linear list: Calculates non-overlapping horizontal node coordinates")
    fun testLinearListLayout() {
        val id1 = Uuid.random()
        val id2 = Uuid.random()
        val id3 = Uuid.random()

        val state = LinkedNodeState(
            nodes = persistentListOf(
                LinkedNode(id = id1, value = 10, nextId = id2),
                LinkedNode(id = id2, value = 20, nextId = id3),
                LinkedNode(id = id3, value = 30, nextId = null)
            ),
            pointers = persistentMapOf("head" to id1.toString(), "curr" to id2.toString())
        )

        val result = LinkedNodeLayoutCalculator.calculateLayout(state, width = 400f, height = 300f)

        assertEquals(LinkedNodeLayoutCalculator.ContainerMode.LINEAR_LIST, result.mode)
        assertEquals(3, result.nodePositions.size)
        assertEquals(2, result.links.size)

        val p1 = result.nodePositions[id1.toString()]!!
        val p2 = result.nodePositions[id2.toString()]!!
        val p3 = result.nodePositions[id3.toString()]!!

        assertTrue(p1.x < p2.x, "Node 1 should be to the left of Node 2")
        assertTrue(p2.x < p3.x, "Node 2 should be to the left of Node 3")
        assertEquals(p1.y, p2.y, 0.01f, "Horizontal spine Y positions should match")
    }

    @Test
    @DisplayName("Floyd cycle: Detects cycle loop and calculates circular orbital geometry")
    fun testFloydOrbitalCycleLayout() {
        val id1 = Uuid.random()
        val id2 = Uuid.random()
        val id3 = Uuid.random()
        val id4 = Uuid.random()

        // Loop: 1 -> 2 -> 3 -> 4 -> 2 (Cycle entry at id2)
        val state = LinkedNodeState(
            nodes = persistentListOf(
                LinkedNode(id = id1, value = 1, nextId = id2),
                LinkedNode(id = id2, value = 2, nextId = id3),
                LinkedNode(id = id3, value = 3, nextId = id4),
                LinkedNode(id = id4, value = 4, nextId = id2)
            ),
            pointers = persistentMapOf("slow" to id2.toString(), "fast" to id4.toString()),
            isCycleDetected = true
        )

        val result = LinkedNodeLayoutCalculator.calculateLayout(state, width = 600f, height = 400f)

        assertEquals(LinkedNodeLayoutCalculator.ContainerMode.FLOYD_ORBITAL, result.mode)
        assertNotNull(result.orbitCenter, "Orbit center must be calculated")
        assertNotNull(result.orbitRadius, "Orbit radius must be calculated")
        assertTrue(result.orbitRadius!! > 30f)

        // Verify cycle closure link exists
        assertTrue(result.links.any { it.isCycleLoop }, "Should identify cycle return loop link")
    }

    @Test
    @DisplayName("Vertical Stack: Positions nodes vertically from bottom to top")
    fun testVerticalStackLayout() {
        val id1 = Uuid.random()
        val id2 = Uuid.random()

        val state = LinkedNodeState(
            nodes = persistentListOf(
                LinkedNode(id = id1, value = 10, nextId = id2),
                LinkedNode(id = id2, value = 20, nextId = null)
            ),
            pointers = persistentMapOf("top" to id1.toString())
        )

        val result = LinkedNodeLayoutCalculator.calculateLayout(state, width = 300f, height = 500f)

        assertEquals(LinkedNodeLayoutCalculator.ContainerMode.VERTICAL_STACK, result.mode)
        val p1 = result.nodePositions[id1.toString()]!!
        val p2 = result.nodePositions[id2.toString()]!!

        assertEquals(p1.x, p2.x, 0.01f, "Vertical stack X positions should be aligned")
        assertTrue(p1.y > p2.y, "Top node index 0 is at base, index 1 stacked higher")
    }
}
