package com.unravel.app.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RouteSerializationTest {

    @Test
    @DisplayName("WP-38: SkillMapRoute serializes and deserializes as singleton object")
    fun testSkillMapRouteSerialization() {
        val json = UnravelRouteSerializer.serializeRoute(SkillMapRoute)
        val deserialized = UnravelRouteSerializer.deserializeRoute(json)
        assertEquals(SkillMapRoute, deserialized)
    }

    @Test
    @DisplayName("WP-38: SandboxRoute serializes algorithmId and resumeSession flag accurately")
    fun testSandboxRouteSerialization() {
        val routeWithDefaults = SandboxRoute(algorithmId = "quick_sort")
        val jsonDefaults = UnravelRouteSerializer.serializeRoute(routeWithDefaults)
        val deserializedDefaults = UnravelRouteSerializer.deserializeRoute(jsonDefaults) as SandboxRoute
        assertEquals("quick_sort", deserializedDefaults.algorithmId)
        assertFalse(deserializedDefaults.resumeSession)

        val routeWithResume = SandboxRoute(algorithmId = "dijkstra_astar", resumeSession = true)
        val jsonResume = UnravelRouteSerializer.serializeRoute(routeWithResume)
        val deserializedResume = UnravelRouteSerializer.deserializeRoute(jsonResume) as SandboxRoute
        assertEquals("dijkstra_astar", deserializedResume.algorithmId)
        assertTrue(deserializedResume.resumeSession)
    }

    @Test
    @DisplayName("WP-38: RaceModeRoute serializes category properly with defaults")
    fun testRaceModeRouteSerialization() {
        val defaultRoute = RaceModeRoute()
        val jsonDefault = UnravelRouteSerializer.serializeRoute(defaultRoute)
        val deserializedDefault = UnravelRouteSerializer.deserializeRoute(jsonDefault) as RaceModeRoute
        assertEquals("ARRAY_POINTER", deserializedDefault.category)

        val customRoute = RaceModeRoute(category = "HIERARCHICAL_TREE")
        val jsonCustom = UnravelRouteSerializer.serializeRoute(customRoute)
        val deserializedCustom = UnravelRouteSerializer.deserializeRoute(jsonCustom) as RaceModeRoute
        assertEquals("HIERARCHICAL_TREE", deserializedCustom.category)
    }

    @Test
    @DisplayName("WP-38: Backstack list serializes and restores multi-destination history intact")
    fun testBackstackSerialization() {
        val backstack = listOf(
            SkillMapRoute,
            SandboxRoute(algorithmId = "bubble_sort", resumeSession = false),
            RaceModeRoute(category = "SPATIAL_GRID_GRAPH")
        )

        val json = UnravelRouteSerializer.serializeBackstack(backstack)
        val restored = UnravelRouteSerializer.deserializeBackstack(json)

        assertEquals(3, restored.size)
        assertEquals(SkillMapRoute, restored[0])
        assertEquals(SandboxRoute(algorithmId = "bubble_sort", resumeSession = false), restored[1])
        assertEquals(RaceModeRoute(category = "SPATIAL_GRID_GRAPH"), restored[2])
    }
}
