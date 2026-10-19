package com.unravel.feature.racemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.engine.RaceAlgorithmCatalog
import com.unravel.feature.racemode.model.RaceModeIntent
import com.unravel.feature.racemode.model.RaceModeUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Orchestrator ViewModel coordinating synchronized dual algorithm frame tickers,
 * real-time comparison/mutation deltas, divergence detection, and winner resolution.
 */
class RaceModeViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaceModeUiState())
    val uiState: StateFlow<RaceModeUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadMatchup("bubble_vs_quick")
    }

    fun onIntent(intent: RaceModeIntent) {
        when (intent) {
            is RaceModeIntent.Play -> play()
            is RaceModeIntent.Pause -> pause()
            is RaceModeIntent.TogglePlayPause -> {
                if (_uiState.value.isPlaying) pause() else play()
            }
            is RaceModeIntent.StepForward -> {
                pause()
                advanceStep()
            }
            is RaceModeIntent.StepBackward -> {
                pause()
                retreatStep()
            }
            is RaceModeIntent.ResetRace -> resetRace()
            is RaceModeIntent.SelectMatchup -> loadMatchup(intent.matchupId)
            is RaceModeIntent.SelectPair -> selectCustomPair(intent.algorithmA, intent.algorithmB)
            is RaceModeIntent.SelectCategory -> selectCategory(intent.category)
            is RaceModeIntent.SetSpeed -> setSpeed(intent.speed)
        }
    }

    private fun loadMatchup(matchupId: String) {
        pause()
        val matchup = RaceAlgorithmCatalog.getMatchupById(matchupId)
        val (framesA, framesB) = RaceAlgorithmCatalog.generateMatchupFrames(
            algorithmA = matchup.algorithmA,
            algorithmB = matchup.algorithmB,
            category = matchup.category
        )

        _uiState.update { state ->
            val firstFrameA = framesA.firstOrNull()
            val firstFrameB = framesB.firstOrNull()
            val cmpA = firstFrameA?.metrics?.comparisons ?: 0
            val cmpB = firstFrameB?.metrics?.comparisons ?: 0
            val mutA = firstFrameA?.metrics?.mutations ?: 0
            val mutB = firstFrameB?.metrics?.mutations ?: 0

            state.copy(
                category = matchup.category,
                selectedMatchupId = matchup.id,
                algorithmA = matchup.algorithmA,
                algorithmB = matchup.algorithmB,
                framesA = framesA,
                framesB = framesB,
                stepIndexA = 0,
                stepIndexB = 0,
                isPlaying = false,
                winnerAlgorithmId = null,
                comparisonDelta = cmpA - cmpB,
                mutationDelta = mutA - mutB,
                isDiverged = abs(cmpA - cmpB) >= state.divergenceThreshold,
                isFinishedA = false,
                isFinishedB = false,
                isRaceFinished = false
            )
        }
    }

    private fun selectCustomPair(algorithmA: AlgorithmId, algorithmB: AlgorithmId) {
        pause()
        val category = algorithmA.category
        val (framesA, framesB) = RaceAlgorithmCatalog.generateMatchupFrames(
            algorithmA = algorithmA,
            algorithmB = algorithmB,
            category = category
        )

        _uiState.update { state ->
            val firstFrameA = framesA.firstOrNull()
            val firstFrameB = framesB.firstOrNull()
            val cmpA = firstFrameA?.metrics?.comparisons ?: 0
            val cmpB = firstFrameB?.metrics?.comparisons ?: 0
            val mutA = firstFrameA?.metrics?.mutations ?: 0
            val mutB = firstFrameB?.metrics?.mutations ?: 0

            state.copy(
                category = category,
                algorithmA = algorithmA,
                algorithmB = algorithmB,
                framesA = framesA,
                framesB = framesB,
                stepIndexA = 0,
                stepIndexB = 0,
                isPlaying = false,
                winnerAlgorithmId = null,
                comparisonDelta = cmpA - cmpB,
                mutationDelta = mutA - mutB,
                isDiverged = abs(cmpA - cmpB) >= state.divergenceThreshold,
                isFinishedA = false,
                isFinishedB = false,
                isRaceFinished = false
            )
        }
    }

    private fun selectCategory(category: SandboxCategory) {
        val matchups = RaceAlgorithmCatalog.getMatchupsByCategory(category)
        val defaultMatchup = matchups.firstOrNull() ?: RaceAlgorithmCatalog.MATCHUPS.first()
        loadMatchup(defaultMatchup.id)
    }

    private fun play() {
        val current = _uiState.value
        if (current.isRaceFinished) {
            resetRace()
        }

        _uiState.update { it.copy(isPlaying = true) }
        startTicker()
    }

    private fun pause() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(dispatcher) {
            while (isActive) {
                val speed = _uiState.value.playbackSpeed
                val delayMs = (350L / speed).toLong().coerceAtLeast(30L)
                delay(delayMs)

                val shouldContinue = advanceStep()
                if (!shouldContinue) {
                    break
                }
            }
        }
    }

    /**
     * Advances both algorithms forward by 1 frame simultaneously and evaluates real-time deltas.
     * Returns false if both algorithms have reached terminal completion.
     */
    private fun advanceStep(): Boolean {
        var shouldContinue = true

        _uiState.update { state ->
            val maxA = maxOf(0, state.totalStepsA - 1)
            val maxB = maxOf(0, state.totalStepsB - 1)

            val nextA = if (state.stepIndexA < maxA) state.stepIndexA + 1 else state.stepIndexA
            val nextB = if (state.stepIndexB < maxB) state.stepIndexB + 1 else state.stepIndexB

            val isDoneA = nextA >= maxA
            val isDoneB = nextB >= maxB
            val bothDone = isDoneA && isDoneB

            // Evaluate winner on the first completion transition
            var winner = state.winnerAlgorithmId
            if (winner == null) {
                if (isDoneA && !isDoneB) {
                    winner = state.algorithmA
                } else if (isDoneB && !isDoneA) {
                    winner = state.algorithmB
                } else if (bothDone) {
                    winner = if (state.totalStepsA <= state.totalStepsB) {
                        state.algorithmA
                    } else {
                        state.algorithmB
                    }
                }
            }

            val frameA: AlgorithmFrame? = state.framesA.getOrNull(nextA)
            val frameB: AlgorithmFrame? = state.framesB.getOrNull(nextB)

            val cmpA = frameA?.metrics?.comparisons ?: 0
            val cmpB = frameB?.metrics?.comparisons ?: 0
            val mutA = frameA?.metrics?.mutations ?: 0
            val mutB = frameB?.metrics?.mutations ?: 0

            val cmpDelta = cmpA - cmpB
            val mutDelta = mutA - mutB
            val isDiverged = abs(cmpDelta) >= state.divergenceThreshold

            if (bothDone) {
                shouldContinue = false
            }

            state.copy(
                stepIndexA = nextA,
                stepIndexB = nextB,
                winnerAlgorithmId = winner,
                comparisonDelta = cmpDelta,
                mutationDelta = mutDelta,
                isDiverged = isDiverged,
                isFinishedA = isDoneA,
                isFinishedB = isDoneB,
                isRaceFinished = bothDone,
                isPlaying = if (bothDone) false else state.isPlaying
            )
        }

        return shouldContinue
    }

    private fun retreatStep() {
        _uiState.update { state ->
            val prevA = maxOf(0, state.stepIndexA - 1)
            val prevB = maxOf(0, state.stepIndexB - 1)

            val frameA = state.framesA.getOrNull(prevA)
            val frameB = state.framesB.getOrNull(prevB)

            val cmpA = frameA?.metrics?.comparisons ?: 0
            val cmpB = frameB?.metrics?.comparisons ?: 0
            val mutA = frameA?.metrics?.mutations ?: 0
            val mutB = frameB?.metrics?.mutations ?: 0

            val cmpDelta = cmpA - cmpB
            val mutDelta = mutA - mutB

            state.copy(
                stepIndexA = prevA,
                stepIndexB = prevB,
                comparisonDelta = cmpDelta,
                mutationDelta = mutDelta,
                isDiverged = abs(cmpDelta) >= state.divergenceThreshold,
                isFinishedA = false,
                isFinishedB = false,
                isRaceFinished = false,
                winnerAlgorithmId = if (prevA < state.totalStepsA - 1 && prevB < state.totalStepsB - 1) null else state.winnerAlgorithmId
            )
        }
    }

    private fun resetRace() {
        pause()
        _uiState.update { state ->
            val firstA = state.framesA.firstOrNull()
            val firstB = state.framesB.firstOrNull()
            val cmpA = firstA?.metrics?.comparisons ?: 0
            val cmpB = firstB?.metrics?.comparisons ?: 0
            val mutA = firstA?.metrics?.mutations ?: 0
            val mutB = firstB?.metrics?.mutations ?: 0

            state.copy(
                stepIndexA = 0,
                stepIndexB = 0,
                isPlaying = false,
                winnerAlgorithmId = null,
                comparisonDelta = cmpA - cmpB,
                mutationDelta = mutA - mutB,
                isDiverged = abs(cmpA - cmpB) >= state.divergenceThreshold,
                isFinishedA = false,
                isFinishedB = false,
                isRaceFinished = false
            )
        }
    }

    private fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 3.0f)
        _uiState.update { it.copy(playbackSpeed = clamped) }
        if (_uiState.value.isPlaying) {
            startTicker()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
