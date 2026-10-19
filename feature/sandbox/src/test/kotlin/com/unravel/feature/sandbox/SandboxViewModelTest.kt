package com.unravel.feature.sandbox

import app.cash.turbine.test
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.VisualStructureState
import com.unravel.feature.sandbox.model.SandboxIntent
import com.unravel.feature.sandbox.model.SandboxMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxViewModelTest {

    private class FakeSessionRepository(
        initialSession: ActiveSessionPreferences = ActiveSessionPreferences()
    ) : SessionRepository {
        private val _flow = MutableStateFlow(initialSession)
        override val activeSessionFlow: StateFlow<ActiveSessionPreferences> = _flow.asStateFlow()
        override fun getActiveSession(): ActiveSessionPreferences = _flow.value
        override suspend fun saveActiveSession(session: ActiveSessionPreferences) {
            _flow.value = session
        }
        override suspend fun clearSession() {
            _flow.value = ActiveSessionPreferences()
        }
    }

    @Test
    @DisplayName("WP-26: Initial state is populated with valid frames and metadata")
    fun testInitialState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertEquals(AlgorithmId.TWO_POINTERS, state.algorithmId)
        assertEquals(0, state.currentFrameIndex)
        assertTrue(state.totalFrames > 0, "totalFrames should be > 0")
        assertNotNull(state.currentFrame)
        assertFalse(state.isPlaying)
        assertEquals(1.0f, state.playbackSpeed)
        assertEquals(SandboxMode.PLAYBACK, state.sandboxMode)
        assertEquals(0, state.challengeStreak)
        assertFalse(state.rejectionPulse)
        assertTrue(state.codeSnippet.isNotBlank())
    }

    @Test
    @DisplayName("WP-26: StepForward and StepBackward cleanly navigate timeline frames")
    fun testStepForwardAndStepBackward() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.StepForward)
        assertEquals(1, viewModel.uiState.value.currentFrameIndex)
        assertEquals(1, viewModel.uiState.value.currentFrame?.stepIndex)

        viewModel.processIntent(SandboxIntent.StepForward)
        assertEquals(2, viewModel.uiState.value.currentFrameIndex)

        viewModel.processIntent(SandboxIntent.StepBackward)
        assertEquals(1, viewModel.uiState.value.currentFrameIndex)

        viewModel.processIntent(SandboxIntent.StepBackward)
        assertEquals(0, viewModel.uiState.value.currentFrameIndex)

        // Clamping at zero
        viewModel.processIntent(SandboxIntent.StepBackward)
        assertEquals(0, viewModel.uiState.value.currentFrameIndex)
    }

    @Test
    @DisplayName("WP-26: SeekToStep provides instantaneous O(1) random-access scrubbing")
    fun testSeekToStep() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        val targetStep = 5
        viewModel.processIntent(SandboxIntent.SeekToStep(targetStep))

        val state = viewModel.uiState.value
        assertEquals(targetStep, state.currentFrameIndex)
        assertEquals(targetStep, state.currentFrame?.stepIndex)
        assertFalse(state.isPlaying)
    }

    @Test
    @DisplayName("WP-26: Frame ticker advances timeline when playing and pauses on command")
    fun testPlayPauseTimeline() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.Play)
        assertTrue(viewModel.uiState.value.isPlaying)

        // Advance by one tick interval (800ms)
        advanceTimeBy(SandboxViewModel.BASE_INTERVAL_MS + 50L)
        assertEquals(1, viewModel.uiState.value.currentFrameIndex)

        // Advance by another tick
        advanceTimeBy(SandboxViewModel.BASE_INTERVAL_MS)
        assertEquals(2, viewModel.uiState.value.currentFrameIndex)

        // Pause
        viewModel.processIntent(SandboxIntent.Pause)
        assertFalse(viewModel.uiState.value.isPlaying)

        // Time elapses while paused, index should NOT change
        advanceTimeBy(SandboxViewModel.BASE_INTERVAL_MS * 2)
        assertEquals(2, viewModel.uiState.value.currentFrameIndex)
    }

    @Test
    @DisplayName("WP-26: SetSpeed alters execution speed and ticker interval")
    fun testSetSpeed() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SetSpeed(2.0f))
        assertEquals(2.0f, viewModel.uiState.value.playbackSpeed)

        viewModel.processIntent(SandboxIntent.Play)
        // With 2x speed, interval is 400ms instead of 800ms
        advanceTimeBy(450L)
        assertEquals(1, viewModel.uiState.value.currentFrameIndex)
    }

    @Test
    @DisplayName("WP-26: Mode switching toggles between Playback, Direct Edit, and Challenge")
    fun testModeSwitching() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.DIRECT_EDIT))
        assertEquals(SandboxMode.DIRECT_EDIT, viewModel.uiState.value.sandboxMode)

        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.CHALLENGE_BE_THE_ALGO))
        assertEquals(SandboxMode.CHALLENGE_BE_THE_ALGO, viewModel.uiState.value.sandboxMode)

        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.PLAYBACK))
        assertEquals(SandboxMode.PLAYBACK, viewModel.uiState.value.sandboxMode)
    }

    @Test
    @DisplayName("WP-26: Challenge validation increments streak on correct action and triggers error on invalid")
    fun testChallengeValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )
        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.CHALLENGE_BE_THE_ALGO))

        // Find a frame with an expected action
        val frameWithActionIndex = viewModel.uiState.value.frames.indexOfFirst { it.expectedUserAction != null }
        assertTrue(frameWithActionIndex >= 0, "Curriculum should have frames with expectedUserAction")
        viewModel.processIntent(SandboxIntent.SeekToStep(frameWithActionIndex))

        val expected = viewModel.uiState.value.currentFrame?.expectedUserAction
        assertNotNull(expected)

        // 1. Submit invalid action
        val wrongAction = ExpectedAction.SwapIndices(999, 998)
        viewModel.processIntent(SandboxIntent.SubmitChallengeAction(wrongAction))
        assertEquals(0, viewModel.uiState.value.challengeStreak)
        assertTrue(viewModel.uiState.value.rejectionPulse)

        // Advance time to let rejection pulse decay
        advanceTimeBy(SandboxViewModel.REJECTION_PULSE_DURATION_MS + 50L)
        assertFalse(viewModel.uiState.value.rejectionPulse)

        // 2. Submit valid action
        viewModel.processIntent(SandboxIntent.SubmitChallengeAction(expected!!))
        assertEquals(1, viewModel.uiState.value.challengeStreak)
        assertFalse(viewModel.uiState.value.rejectionPulse)
    }

    @Test
    @DisplayName("WP-26: Lifecycle onPause persists session checkpoint to SessionRepository")
    fun testLifecycleSessionPersistence() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeRepo = FakeSessionRepository()
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            sessionRepository = fakeRepo,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SeekToStep(4))
        viewModel.processIntent(SandboxIntent.SetSpeed(2.0f))

        viewModel.onLifecyclePause()
        advanceUntilIdle()

        val saved = fakeRepo.getActiveSession()
        assertEquals(AlgorithmId.TWO_POINTERS.id, saved.algorithmId)
        assertEquals(4, saved.stepIndex)
        assertEquals(2.0f, saved.playbackSpeed)
    }

    @Test
    @DisplayName("WP-26: Resume active session on init if matching saved session exists")
    fun testResumeSessionOnInit() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeRepo = FakeSessionRepository(
            ActiveSessionPreferences(
                algorithmId = AlgorithmId.TWO_POINTERS.id,
                stepIndex = 3,
                playbackSpeed = 1.5f,
                timestampEpochMs = 12345L
            )
        )

        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            sessionRepository = fakeRepo,
            dispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertEquals(3, state.currentFrameIndex)
        assertEquals(1.5f, state.playbackSpeed)
        assertEquals(3, state.currentFrame?.stepIndex)
    }

    @Test
    @DisplayName("WP-45: DirectEditArrayValue dynamically updates bar value and regenerates frames")
    fun testDirectEditArrayValue() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.BUBBLE_SORT,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.DIRECT_EDIT))
        assertEquals(SandboxMode.DIRECT_EDIT, viewModel.uiState.value.sandboxMode)

        // Modify index 0 to value 88
        viewModel.processIntent(SandboxIntent.DirectEditArrayValue(index = 0, newValue = 88))
        val state = viewModel.uiState.value
        val structure = state.currentFrame?.structureState as VisualStructureState.ArrayState
        assertEquals(88, structure.items[0].value)
        assertEquals(0, state.currentFrameIndex)
        assertTrue(state.totalFrames > 0)
    }

    @Test
    @DisplayName("WP-45: DirectEditSwap interactively swaps two elements and regenerates frames")
    fun testDirectEditSwap() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.BUBBLE_SORT,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.DIRECT_EDIT))

        val initialStructure = viewModel.uiState.value.currentFrame?.structureState as VisualStructureState.ArrayState
        val val0 = initialStructure.items[0].value
        val val1 = initialStructure.items[1].value

        viewModel.processIntent(SandboxIntent.DirectEditSwap(indexA = 0, indexB = 1))
        val postSwapStructure = viewModel.uiState.value.currentFrame?.structureState as VisualStructureState.ArrayState
        assertEquals(val1, postSwapStructure.items[0].value)
        assertEquals(val0, postSwapStructure.items[1].value)
        assertEquals(0, viewModel.uiState.value.currentFrameIndex)
    }
}
