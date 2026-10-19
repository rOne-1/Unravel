package com.unravel.core.database.model

import kotlinx.serialization.Serializable

/**
 * Persisted state of the active learning session corresponding to the active_session.proto schema.
 *
 * @param algorithmId Canonical string ID of the active algorithm (e.g., "two_pointers", "quick_sort").
 * @param stepIndex The current scrubber frame index reached in the active session.
 * @param playbackSpeed Execution speed multiplier (e.g., 0.5f, 1.0f, 2.0f).
 * @param datasetJson JSON-serialized snapshot of the custom or active array/graph state.
 * @param timestampEpochMs System epoch timestamp in milliseconds when the session was saved.
 */
@Serializable
data class ActiveSessionPreferences(
    val algorithmId: String = "",
    val stepIndex: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val datasetJson: String = "",
    val timestampEpochMs: Long = 0L
)
