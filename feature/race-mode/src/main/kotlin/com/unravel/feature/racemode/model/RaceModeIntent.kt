package com.unravel.feature.racemode.model

import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory

/**
 * User actions and state machine triggers for Split-Screen Race Mode.
 */
sealed interface RaceModeIntent {
    data object Play : RaceModeIntent
    data object Pause : RaceModeIntent
    data object TogglePlayPause : RaceModeIntent
    data object StepForward : RaceModeIntent
    data object StepBackward : RaceModeIntent
    data object ResetRace : RaceModeIntent
    data class SelectMatchup(val matchupId: String) : RaceModeIntent
    data class SelectPair(val algorithmA: AlgorithmId, val algorithmB: AlgorithmId) : RaceModeIntent
    data class SelectCategory(val category: SandboxCategory) : RaceModeIntent
    data class SetSpeed(val speed: Float) : RaceModeIntent
}
