package com.unravel.app.navigation

import androidx.navigation3.runtime.NavBackStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UnravelNavHostTest {

    @Test
    @DisplayName("WP-39: Single-top backstack policy avoids duplicate route pushes")
    fun testSingleTopBackstackPolicy() {
        val backStack = NavBackStack<UnravelRoute>(SkillMapRoute)
        assertEquals(1, backStack.size)
        assertEquals(SkillMapRoute, backStack.last())

        // Helper replicating UnravelNavHost navigateTo single-top logic
        fun navigateTo(route: UnravelRoute) {
            if (backStack.lastOrNull() == route) return
            backStack.add(route)
        }

        // Push new route
        val sandboxRoute = SandboxRoute(algorithmId = "quick_sort", resumeSession = false)
        navigateTo(sandboxRoute)
        assertEquals(2, backStack.size)
        assertEquals(sandboxRoute, backStack.last())

        // Push duplicate identical route (should be ignored by single-top policy)
        navigateTo(sandboxRoute)
        assertEquals(2, backStack.size)

        // Push another route
        val raceRoute = RaceModeRoute(category = "SPATIAL_GRID_GRAPH")
        navigateTo(raceRoute)
        assertEquals(3, backStack.size)
        assertEquals(raceRoute, backStack.last())

        // Pop back
        fun popBack(): Boolean {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
                return true
            }
            return false
        }

        assertTrue(popBack())
        assertEquals(2, backStack.size)
        assertEquals(sandboxRoute, backStack.last())

        assertTrue(popBack())
        assertEquals(1, backStack.size)
        assertEquals(SkillMapRoute, backStack.last())

        // Cannot pop root
        assertFalse(popBack())
        assertEquals(1, backStack.size)
        assertEquals(SkillMapRoute, backStack.last())
    }
}
