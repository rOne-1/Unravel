package com.unravel.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Top-level sealed destination hierarchy for Unravel.
 * Implements Navigation 3 NavKey contract with full kotlinx.serialization support.
 */
@Serializable
sealed interface UnravelRoute : NavKey

/**
 * Interactive DAG Skill Map root destination.
 */
@Serializable
data object SkillMapRoute : UnravelRoute

/**
 * Interactive Sandbox tactile execution machine destination.
 *
 * @param algorithmId Canonical identifier of the active algorithm.
 * @param resumeSession Whether to restore prior session frames and execution pointer from Proto DataStore.
 */
@Serializable
data class SandboxRoute(
    val algorithmId: String,
    val resumeSession: Boolean = false
) : UnravelRoute

/**
 * Split-screen head-to-head algorithm benchmark race destination.
 *
 * @param category Canonical SandboxCategory name (default: "ARRAY_POINTER").
 */
@Serializable
data class RaceModeRoute(
    val category: String = "ARRAY_POINTER"
) : UnravelRoute

/**
 * Serialization utilities for backstack state persistence and deep linking.
 */
object UnravelRouteSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun serializeRoute(route: UnravelRoute): String {
        return json.encodeToString(route)
    }

    fun deserializeRoute(payload: String): UnravelRoute {
        return json.decodeFromString(payload)
    }

    fun serializeBackstack(backstack: List<UnravelRoute>): String {
        return json.encodeToString(backstack)
    }

    fun deserializeBackstack(payload: String): List<UnravelRoute> {
        return json.decodeFromString(payload)
    }
}
