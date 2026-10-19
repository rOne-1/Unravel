package com.unravel.feature.skillmap.model

import androidx.compose.runtime.Immutable
import com.unravel.core.database.model.ActiveSessionPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Mastery states for a curriculum algorithm skill node.
 */
object MasteryState {
    const val UNEXPLORED = "UNEXPLORED"
    const val TINKERED = "TINKERED"
    const val MASTERED = "MASTERED"
}

/**
 * UI representation of an interactive skill node on the 2D DAG canvas.
 *
 * @param id Canonical string algorithm identifier.
 * @param title Human-readable algorithm name.
 * @param category Category name matching [com.unravel.core.engine.model.SandboxCategory].
 * @param clusterIndex Curriculum cluster index (1..4).
 * @param prerequisiteIds Upstream node identifiers required before unlocking.
 * @param masteryState Current mastery level ([MasteryState.UNEXPLORED], [MasteryState.TINKERED], or [MasteryState.MASTERED]).
 * @param isUnlocked True if all prerequisites are mastered or if node is a cluster root.
 * @param x Spatial X coordinate in the 2400x1600 map coordinate space.
 * @param y Spatial Y coordinate in the 2400x1600 map coordinate space.
 */
@Immutable
data class SkillNodeUiModel(
    val id: String,
    val title: String,
    val category: String,
    val clusterIndex: Int,
    val prerequisiteIds: ImmutableList<String>,
    val masteryState: String = MasteryState.UNEXPLORED,
    val isUnlocked: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * Spatial boundary envelope of a curriculum cluster territory.
 */
@Immutable
data class ClusterBounds(
    val clusterIndex: Int,
    val title: String,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

/**
 * Immutable UI state model for the Skill Map screen.
 */
@Immutable
data class SkillMapUiState(
    val nodes: ImmutableList<SkillNodeUiModel> = persistentListOf(),
    val clusterBounds: ImmutableList<ClusterBounds> = persistentListOf(),
    val selectedNodeId: String? = null,
    val activeSession: ActiveSessionPreferences? = null,
    val totalNodes: Int = 14,
    val masteredCount: Int = 0,
    val unlockedCount: Int = 0,
    val isLoading: Boolean = false
)
