package com.unravel.feature.sandbox.model

import androidx.compose.runtime.Immutable
import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Operating modes for the Unravel interactive sandbox environment.
 */
enum class SandboxMode {
    /** Auto or manual stepping through deterministic sequence frames. */
    PLAYBACK,

    /** Direct interactive mutation of input structures (drag values, insert nodes). */
    DIRECT_EDIT,

    /** Interactive "Be the Algorithm" challenge mode with gesture validation and streak scoring. */
    CHALLENGE_BE_THE_ALGO
}

/**
 * Immutable UI state model for the Sandbox MVI experience.
 *
 * @param algorithmId Canonical identifier of the active algorithm.
 * @param category Visualization category governing canvas rendering.
 * @param currentFrameIndex Active frame index within the sequence timeline.
 * @param totalFrames Total count of pre-computed deterministic sequence frames.
 * @param currentFrame The [AlgorithmFrame] rendered at [currentFrameIndex], or null if empty.
 * @param isPlaying True if ticker coroutine is currently advancing frames automatically.
 * @param playbackSpeed Execution speed multiplier (e.g. 0.25f, 0.5f, 1.0f, 2.0f, 4.0f).
 * @param sandboxMode Current operating mode ([SandboxMode.PLAYBACK], [SandboxMode.DIRECT_EDIT], or [SandboxMode.CHALLENGE_BE_THE_ALGO]).
 * @param challengeStreak Count of consecutive correct challenge actions submitted.
 * @param rejectionPulse True when an invalid move triggers visual rejection shake/pulse feedback.
 * @param frames Immutable list of all sequence frames for $O(1)$ random-access scrubbing.
 * @param codeSnippet Formatted pseudo-code or Kotlin snippet aligned with active line indices.
 * @param activePreset The active input generation preset used for initial state construction.
 */
@Immutable
data class SandboxUiState(
    val algorithmId: AlgorithmId = AlgorithmId.TWO_POINTERS,
    val category: SandboxCategory = AlgorithmId.TWO_POINTERS.category,
    val currentFrameIndex: Int = 0,
    val totalFrames: Int = 0,
    val currentFrame: AlgorithmFrame? = null,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sandboxMode: SandboxMode = SandboxMode.PLAYBACK,
    val challengeStreak: Int = 0,
    val rejectionPulse: Boolean = false,
    val frames: ImmutableList<AlgorithmFrame> = persistentListOf(),
    val codeSnippet: String = "",
    val activePreset: InputPreset = InputPreset.RANDOM
)
