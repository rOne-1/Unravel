package com.unravel.feature.skillmap.model

/**
 * User and system intents consumed by [com.unravel.feature.skillmap.SkillMapViewModel].
 */
sealed interface SkillMapIntent {
    /** Selects a specific skill node to show inspection telemetry / actions. */
    data class SelectNode(val nodeId: String) : SkillMapIntent

    /** Clears the current node selection. */
    data object ClearSelection : SkillMapIntent

    /** Refreshes graph nodes and prerequisite resolutions from Room. */
    data object RefreshGraph : SkillMapIntent

    /** Updates the mastery state of a node (e.g. marking as MASTERED or TINKERED). */
    data class UpdateMasteryState(val nodeId: String, val masteryState: String) : SkillMapIntent

    /** Clears active session in SessionRepository, dismissing Jump Back In banner. */
    data object ClearActiveSession : SkillMapIntent
}
