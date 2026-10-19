package com.unravel.feature.racemode.model

import androidx.compose.runtime.Immutable
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable UI state model for Split-Screen Race Mode.
 */
@Immutable
data class RaceModeUiState(
    val category: SandboxCategory = SandboxCategory.ARRAY_POINTER,
    val selectedMatchupId: String = "bubble_vs_quick",
    val algorithmA: AlgorithmId = AlgorithmId.BUBBLE_SORT,
    val algorithmB: AlgorithmId = AlgorithmId.QUICK_SORT,
    val framesA: ImmutableList<AlgorithmFrame> = persistentListOf(),
    val framesB: ImmutableList<AlgorithmFrame> = persistentListOf(),
    val stepIndexA: Int = 0,
    val stepIndexB: Int = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val winnerAlgorithmId: AlgorithmId? = null,
    val comparisonDelta: Int = 0,
    val mutationDelta: Int = 0,
    val isDiverged: Boolean = false,
    val divergenceThreshold: Int = 10,
    val isFinishedA: Boolean = false,
    val isFinishedB: Boolean = false,
    val isRaceFinished: Boolean = false
) {
    val currentFrameA: AlgorithmFrame?
        get() = framesA.getOrNull(stepIndexA)

    val currentFrameB: AlgorithmFrame?
        get() = framesB.getOrNull(stepIndexB)

    val totalStepsA: Int
        get() = framesA.size

    val totalStepsB: Int
        get() = framesB.size
}
