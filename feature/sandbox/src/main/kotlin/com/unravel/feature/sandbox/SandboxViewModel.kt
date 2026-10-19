package com.unravel.feature.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.validator.ChallengeValidator
import com.unravel.core.engine.validator.ValidationResult
import com.unravel.feature.sandbox.engine.AlgorithmCatalog
import com.unravel.feature.sandbox.model.SandboxIntent
import com.unravel.feature.sandbox.model.SandboxMode
import com.unravel.feature.sandbox.model.SandboxUiState
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

/**
 * MVI ViewModel managing interactive sandbox state, frame ticker coroutines,
 * bidirectional scrubbing, challenge validation, and session checkpointing.
 */
class SandboxViewModel(
    initialAlgorithmId: AlgorithmId = AlgorithmId.TWO_POINTERS,
    initialPreset: InputPreset = InputPreset.RANDOM,
    private val sessionRepository: SessionRepository? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState: MutableStateFlow<SandboxUiState>
    val uiState: StateFlow<SandboxUiState>

    private var tickerJob: Job? = null
    private var rejectionPulseJob: Job? = null

    companion object {
        const val BASE_INTERVAL_MS = 800L
        const val REJECTION_PULSE_DURATION_MS = 400L
    }

    init {
        val initialFrames = AlgorithmCatalog.generateFrames(initialAlgorithmId, initialPreset)
        val initialSnippet = AlgorithmCatalog.getCodeSnippet(initialAlgorithmId)

        // Check if there is a saved session for this algorithm to resume
        val savedSession = sessionRepository?.getActiveSession()
        val canResume = savedSession != null &&
                savedSession.algorithmId == initialAlgorithmId.id &&
                initialFrames.isNotEmpty()

        val restoredStep = if (canResume) {
            savedSession.stepIndex.coerceIn(0, initialFrames.lastIndex)
        } else {
            0
        }
        val restoredSpeed = if (canResume && savedSession.playbackSpeed > 0f) {
            savedSession.playbackSpeed
        } else {
            1.0f
        }

        val initialState = SandboxUiState(
            algorithmId = initialAlgorithmId,
            category = initialAlgorithmId.category,
            currentFrameIndex = restoredStep,
            totalFrames = initialFrames.size,
            currentFrame = initialFrames.getOrNull(restoredStep),
            isPlaying = false,
            playbackSpeed = restoredSpeed,
            sandboxMode = SandboxMode.PLAYBACK,
            challengeStreak = 0,
            rejectionPulse = false,
            frames = initialFrames,
            codeSnippet = initialSnippet,
            activePreset = initialPreset
        )

        _uiState = MutableStateFlow(initialState)
        uiState = _uiState.asStateFlow()
    }

    /**
     * Dispatches a user or lifecycle intent into the MVI state machine.
     */
    fun processIntent(intent: SandboxIntent) {
        when (intent) {
            SandboxIntent.Play -> startPlayback()
            SandboxIntent.Pause -> pausePlayback()
            SandboxIntent.TogglePlayPause -> {
                if (_uiState.value.isPlaying) pausePlayback() else startPlayback()
            }
            SandboxIntent.StepForward -> stepForward()
            SandboxIntent.StepBackward -> stepBackward()
            is SandboxIntent.SeekToStep -> seekToStep(intent.step)
            is SandboxIntent.SetSpeed -> setSpeed(intent.speed)
            is SandboxIntent.SetMode -> setMode(intent.mode)
            is SandboxIntent.SubmitChallengeAction -> submitChallengeAction(intent.action)
            is SandboxIntent.ResetDataset -> resetDataset(intent.preset)
            is SandboxIntent.SelectAlgorithm -> selectAlgorithm(intent.algorithmId)
            SandboxIntent.CheckpointSession -> checkpointSession()
        }
    }

    /**
     * Alias for [processIntent] matching standard MVI convention.
     */
    fun onIntent(intent: SandboxIntent) = processIntent(intent)

    /**
     * Lifecycle callback invoked when the host UI moves to the background (ON_PAUSE).
     */
    fun onLifecyclePause() {
        pausePlayback()
        checkpointSession()
    }

    private fun startPlayback() {
        val currentState = _uiState.value
        if (currentState.frames.isEmpty()) return

        // If at end of timeline, loop back to the beginning before starting
        val startIndex = if (currentState.currentFrameIndex >= currentState.totalFrames - 1) {
            0
        } else {
            currentState.currentFrameIndex
        }

        _uiState.update {
            it.copy(
                isPlaying = true,
                currentFrameIndex = startIndex,
                currentFrame = it.frames.getOrNull(startIndex)
            )
        }

        launchTicker()
    }

    private fun pausePlayback() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun launchTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(dispatcher) {
            while (isActive && _uiState.value.isPlaying) {
                val speed = _uiState.value.playbackSpeed.coerceAtLeast(0.1f)
                val interval = (BASE_INTERVAL_MS / speed).toLong()
                delay(interval)

                val state = _uiState.value
                if (!state.isPlaying) break

                val nextIndex = state.currentFrameIndex + 1
                if (nextIndex < state.totalFrames) {
                    _uiState.update {
                        it.copy(
                            currentFrameIndex = nextIndex,
                            currentFrame = it.frames[nextIndex]
                        )
                    }
                } else {
                    _uiState.update { it.copy(isPlaying = false) }
                    break
                }
            }
        }
    }

    private fun stepForward() {
        pausePlayback()
        _uiState.update { state ->
            if (state.frames.isEmpty()) return@update state
            val nextIndex = (state.currentFrameIndex + 1).coerceAtMost(state.totalFrames - 1)
            state.copy(
                currentFrameIndex = nextIndex,
                currentFrame = state.frames[nextIndex],
                isPlaying = false
            )
        }
    }

    private fun stepBackward() {
        pausePlayback()
        _uiState.update { state ->
            if (state.frames.isEmpty()) return@update state
            val prevIndex = (state.currentFrameIndex - 1).coerceAtLeast(0)
            state.copy(
                currentFrameIndex = prevIndex,
                currentFrame = state.frames[prevIndex],
                isPlaying = false
            )
        }
    }

    private fun seekToStep(step: Int) {
        pausePlayback()
        _uiState.update { state ->
            if (state.frames.isEmpty()) return@update state
            val target = step.coerceIn(0, state.totalFrames - 1)
            state.copy(
                currentFrameIndex = target,
                currentFrame = state.frames[target],
                isPlaying = false
            )
        }
    }

    private fun setSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.25f, 4.0f)
        _uiState.update { it.copy(playbackSpeed = validSpeed) }
        // If currently playing, relaunch ticker with the updated interval
        if (_uiState.value.isPlaying) {
            launchTicker()
        }
    }

    private fun setMode(mode: SandboxMode) {
        pausePlayback()
        _uiState.update { it.copy(sandboxMode = mode, isPlaying = false) }
    }

    private fun submitChallengeAction(action: com.unravel.core.engine.model.ExpectedAction) {
        val currentFrame = _uiState.value.currentFrame ?: return
        val validationResult = ChallengeValidator.validateAction(currentFrame, action)

        when (validationResult) {
            is ValidationResult.Valid -> {
                rejectionPulseJob?.cancel()
                val nextIndex = validationResult.nextFrameIndex.coerceIn(0, _uiState.value.totalFrames - 1)
                _uiState.update { state ->
                    state.copy(
                        currentFrameIndex = nextIndex,
                        currentFrame = state.frames.getOrNull(nextIndex),
                        challengeStreak = state.challengeStreak + 1,
                        rejectionPulse = false
                    )
                }
            }
            is ValidationResult.Invalid -> {
                _uiState.update { state ->
                    state.copy(
                        challengeStreak = 0,
                        rejectionPulse = true
                    )
                }
                rejectionPulseJob?.cancel()
                rejectionPulseJob = viewModelScope.launch(dispatcher) {
                    delay(REJECTION_PULSE_DURATION_MS)
                    _uiState.update { it.copy(rejectionPulse = false) }
                }
            }
            ValidationResult.NoActionExpected -> {
                // Advance frame gracefully if no action was expected
                stepForward()
            }
        }
    }

    private fun resetDataset(preset: InputPreset?) {
        pausePlayback()
        val targetPreset = preset ?: _uiState.value.activePreset
        val newFrames = AlgorithmCatalog.generateFrames(_uiState.value.algorithmId, targetPreset)
        _uiState.update { state ->
            state.copy(
                currentFrameIndex = 0,
                totalFrames = newFrames.size,
                currentFrame = newFrames.firstOrNull(),
                isPlaying = false,
                challengeStreak = 0,
                rejectionPulse = false,
                frames = newFrames,
                activePreset = targetPreset
            )
        }
    }

    private fun selectAlgorithm(algorithmId: AlgorithmId) {
        pausePlayback()
        val newFrames = AlgorithmCatalog.generateFrames(algorithmId, _uiState.value.activePreset)
        val snippet = AlgorithmCatalog.getCodeSnippet(algorithmId)
        _uiState.update {
            it.copy(
                algorithmId = algorithmId,
                category = algorithmId.category,
                currentFrameIndex = 0,
                totalFrames = newFrames.size,
                currentFrame = newFrames.firstOrNull(),
                isPlaying = false,
                challengeStreak = 0,
                rejectionPulse = false,
                frames = newFrames,
                codeSnippet = snippet
            )
        }
    }

    private fun checkpointSession() {
        val state = _uiState.value
        val repo = sessionRepository ?: return
        viewModelScope.launch(dispatcher) {
            repo.saveActiveSession(
                ActiveSessionPreferences(
                    algorithmId = state.algorithmId.id,
                    stepIndex = state.currentFrameIndex,
                    playbackSpeed = state.playbackSpeed,
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        }
    }
}
