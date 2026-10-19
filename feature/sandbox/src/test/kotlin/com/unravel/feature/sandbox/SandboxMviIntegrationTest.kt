package com.unravel.feature.sandbox

import app.cash.turbine.test
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.SandboxCategory
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxMviIntegrationTest {

    private class TestSessionRepository(
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
    @DisplayName("WP-29: Turbine flow verification of playback lifecycle: Play -> Pause -> Step -> Scrub")
    fun testTurbinePlaybackLifecycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.uiState.test {
            // Initial state
            val s0 = awaitItem()
            assertEquals(0, s0.currentFrameIndex)
            assertFalse(s0.isPlaying)

            // 1. Play
            viewModel.processIntent(SandboxIntent.Play)
            val s1 = awaitItem()
            assertTrue(s1.isPlaying)

            // Let ticker advance one frame (800ms)
            advanceTimeBy(SandboxViewModel.BASE_INTERVAL_MS + 50L)
            val s2 = awaitItem()
            assertEquals(1, s2.currentFrameIndex)

            // 2. Pause
            viewModel.processIntent(SandboxIntent.Pause)
            val s3 = awaitItem()
            assertFalse(s3.isPlaying)
            assertEquals(1, s3.currentFrameIndex)

            // 3. Step forward
            viewModel.processIntent(SandboxIntent.StepForward)
            val s4 = awaitItem()
            assertEquals(2, s4.currentFrameIndex)

            // 4. Scrub to step 7
            viewModel.processIntent(SandboxIntent.SeekToStep(7))
            val s5 = awaitItem()
            assertEquals(7, s5.currentFrameIndex)
            assertEquals(7, s5.currentFrame?.stepIndex)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-29: Turbine verification of Challenge mode validation, streak increment, and error pulse")
    fun testTurbineChallengeMode() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.uiState.test {
            awaitItem() // Initial

            // Switch to Challenge mode
            viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.CHALLENGE_BE_THE_ALGO))
            val challengeState = awaitItem()
            assertEquals(SandboxMode.CHALLENGE_BE_THE_ALGO, challengeState.sandboxMode)
            assertEquals(0, challengeState.challengeStreak)

            // Seek to frame with expected action
            val targetIndex = challengeState.frames.indexOfFirst { it.expectedUserAction != null }
            assertTrue(targetIndex >= 0)
            viewModel.processIntent(SandboxIntent.SeekToStep(targetIndex))
            val seekedState = awaitItem()
            val expectedAction = seekedState.currentFrame?.expectedUserAction
            assertNotNull(expectedAction)

            // Submit invalid action -> pulse true, streak 0
            val wrongAction = ExpectedAction.SwapIndices(111, 222)
            viewModel.processIntent(SandboxIntent.SubmitChallengeAction(wrongAction))
            val invalidState = awaitItem()
            assertEquals(0, invalidState.challengeStreak)
            assertTrue(invalidState.rejectionPulse)

            // Wait for pulse to clear
            advanceTimeBy(SandboxViewModel.REJECTION_PULSE_DURATION_MS + 50L)
            val pulseClearedState = awaitItem()
            assertFalse(pulseClearedState.rejectionPulse)

            // Submit valid action -> streak +1, advances frame
            viewModel.processIntent(SandboxIntent.SubmitChallengeAction(expectedAction!!))
            val validState = awaitItem()
            assertEquals(1, validState.challengeStreak)
            assertFalse(validState.rejectionPulse)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-29: Mode transition tests preserve active dataset and frame bounds")
    fun testModeTransitions() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        val initialFramesCount = viewModel.uiState.value.totalFrames
        viewModel.processIntent(SandboxIntent.SeekToStep(3))

        // Switch to DIRECT_EDIT
        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.DIRECT_EDIT))
        assertEquals(SandboxMode.DIRECT_EDIT, viewModel.uiState.value.sandboxMode)
        assertEquals(3, viewModel.uiState.value.currentFrameIndex)
        assertEquals(initialFramesCount, viewModel.uiState.value.totalFrames)

        // Switch to CHALLENGE_BE_THE_ALGO
        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.CHALLENGE_BE_THE_ALGO))
        assertEquals(SandboxMode.CHALLENGE_BE_THE_ALGO, viewModel.uiState.value.sandboxMode)
        assertEquals(3, viewModel.uiState.value.currentFrameIndex)
        assertEquals(initialFramesCount, viewModel.uiState.value.totalFrames)

        // Switch back to PLAYBACK
        viewModel.processIntent(SandboxIntent.SetMode(SandboxMode.PLAYBACK))
        assertEquals(SandboxMode.PLAYBACK, viewModel.uiState.value.sandboxMode)
        assertEquals(3, viewModel.uiState.value.currentFrameIndex)
    }

    @Test
    @DisplayName("WP-29: Session persistence onPause checkpoints to SessionRepository")
    fun testSessionPersistenceOnPause() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repo = TestSessionRepository()
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.BINARY_SEARCH,
            sessionRepository = repo,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SeekToStep(5))
        viewModel.processIntent(SandboxIntent.SetSpeed(4.0f))

        viewModel.onLifecyclePause()
        advanceUntilIdle()

        val saved = repo.getActiveSession()
        assertEquals(AlgorithmId.BINARY_SEARCH.id, saved.algorithmId)
        assertEquals(5, saved.stepIndex)
        assertEquals(4.0f, saved.playbackSpeed)
    }

    @ParameterizedTest(name = "WP-29: Algorithm {0} matches its defined SandboxCategory and structure state")
    @EnumSource(AlgorithmId::class)
    fun testAll14AlgorithmsCategoryAndStructureState(algorithmId: AlgorithmId) = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = algorithmId,
            dispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertEquals(algorithmId, state.algorithmId)
        assertEquals(algorithmId.category, state.category)
        assertTrue(state.totalFrames > 0, "$algorithmId should have non-zero frames")
        assertNotNull(state.currentFrame)

        val structure = state.currentFrame?.structureState
        when (algorithmId.category) {
            SandboxCategory.ARRAY_POINTER -> assertTrue(
                structure is VisualStructureState.ArrayState,
                "Expected ArrayState for $algorithmId"
            )
            SandboxCategory.LINKED_STACK_QUEUE -> assertTrue(
                structure is VisualStructureState.LinkedNodeState,
                "Expected LinkedNodeState for $algorithmId"
            )
            SandboxCategory.HIERARCHICAL_TREE -> assertTrue(
                structure is VisualStructureState.HierarchyTreeState,
                "Expected HierarchyTreeState for $algorithmId"
            )
            SandboxCategory.SPATIAL_GRID_GRAPH -> assertTrue(
                structure is VisualStructureState.SpatialGridGraphState,
                "Expected SpatialGridGraphState for $algorithmId"
            )
        }
    }

    @Test
    @DisplayName("WP-29: Dataset reset regenerates frames and resets timeline to frame 0")
    fun testResetDataset() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.TWO_POINTERS,
            dispatcher = testDispatcher
        )

        viewModel.processIntent(SandboxIntent.SeekToStep(6))
        assertEquals(6, viewModel.uiState.value.currentFrameIndex)

        viewModel.processIntent(SandboxIntent.ResetDataset(InputPreset.REVERSED))
        val resetState = viewModel.uiState.value
        assertEquals(0, resetState.currentFrameIndex)
        assertEquals(InputPreset.REVERSED, resetState.activePreset)
        assertTrue(resetState.totalFrames > 0)
        assertFalse(resetState.isPlaying)
    }
}
