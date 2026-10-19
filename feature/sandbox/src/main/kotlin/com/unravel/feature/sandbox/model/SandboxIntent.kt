package com.unravel.feature.sandbox.model

import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction

/**
 * User and lifecycle intents consumed by [com.unravel.feature.sandbox.SandboxViewModel].
 */
sealed interface SandboxIntent {
    /** Starts automatic forward frame playback. */
    data object Play : SandboxIntent

    /** Pauses automatic frame playback. */
    data object Pause : SandboxIntent

    /** Toggles between play and pause states. */
    data object TogglePlayPause : SandboxIntent

    /** Steps forward by one frame (+1) and pauses playback. */
    data object StepForward : SandboxIntent

    /** Steps backward by one frame (-1) and pauses playback. */
    data object StepBackward : SandboxIntent

    /** Instantly scrubs to the specified frame index. */
    data class SeekToStep(val step: Int) : SandboxIntent

    /** Changes the playback execution speed multiplier. */
    data class SetSpeed(val speed: Float) : SandboxIntent

    /** Switches the active sandbox operating mode. */
    data class SetMode(val mode: SandboxMode) : SandboxIntent

    /** Submits a user gesture action to be validated in Challenge Mode. */
    data class SubmitChallengeAction(val action: ExpectedAction) : SandboxIntent

    /** Regenerates the input dataset using the current or specified preset. */
    data class ResetDataset(val preset: InputPreset? = null) : SandboxIntent

    /** Switches to a different curriculum algorithm. */
    data class SelectAlgorithm(val algorithmId: AlgorithmId) : SandboxIntent

    /** Saves current session checkpoint to persistence. */
    data object CheckpointSession : SandboxIntent
}
