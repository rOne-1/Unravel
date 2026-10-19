package com.unravel.feature.racemode

import app.cash.turbine.test
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.engine.RaceAlgorithmCatalog
import com.unravel.feature.racemode.model.RaceModeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RaceModeIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("WP-37: Switching categories and matchups dynamically updates engines and frame sequences")
    fun testCategoryAndMatchupSwitching() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            // Initial default
            val initial = awaitItem()
            assertEquals("bubble_vs_quick", initial.selectedMatchupId)
            assertEquals(SandboxCategory.ARRAY_POINTER, initial.category)

            // Switch to Hierarchical Tree category
            viewModel.onIntent(RaceModeIntent.SelectCategory(SandboxCategory.HIERARCHICAL_TREE))
            val treeState = awaitItem()
            assertEquals(SandboxCategory.HIERARCHICAL_TREE, treeState.category)
            assertEquals("bst_vs_avl", treeState.selectedMatchupId)
            assertEquals(AlgorithmId.BST_OPERATIONS, treeState.algorithmA)
            assertEquals(AlgorithmId.AVL_ROTATIONS, treeState.algorithmB)
            assertTrue(treeState.totalStepsA > 0)
            assertTrue(treeState.totalStepsB > 0)

            // Switch to Spatial Graph category
            viewModel.onIntent(RaceModeIntent.SelectCategory(SandboxCategory.SPATIAL_GRID_GRAPH))
            val graphState = awaitItem()
            assertEquals(SandboxCategory.SPATIAL_GRID_GRAPH, graphState.category)
            assertEquals("bfs_vs_dijkstra", graphState.selectedMatchupId)
            assertEquals(AlgorithmId.GRAPH_BFS_DFS, graphState.algorithmA)
            assertEquals(AlgorithmId.DIJKSTRA_ASTAR, graphState.algorithmB)

            // Switch to Linked Nodes
            viewModel.onIntent(RaceModeIntent.SelectCategory(SandboxCategory.LINKED_STACK_QUEUE))
            val linkedState = awaitItem()
            assertEquals(SandboxCategory.LINKED_STACK_QUEUE, linkedState.category)
            assertEquals("linked_reversal_vs_floyd", linkedState.selectedMatchupId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-37: Synchronized dual playback advances both engines concurrently via ticker")
    fun testSynchronizedDualPlaybackWithTurbine() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isPlaying)
            assertEquals(0, initial.stepIndexA)
            assertEquals(0, initial.stepIndexB)

            // Start playback
            viewModel.onIntent(RaceModeIntent.Play)
            val playingState = awaitItem()
            assertTrue(playingState.isPlaying)

            // Advance time to allow ticker ticks
            advanceTimeBy(360)
            val tick1 = awaitItem()
            assertEquals(1, tick1.stepIndexA)
            assertEquals(1, tick1.stepIndexB)

            advanceTimeBy(360)
            val tick2 = awaitItem()
            assertEquals(2, tick2.stepIndexA)
            assertEquals(2, tick2.stepIndexB)

            // Pause playback
            viewModel.onIntent(RaceModeIntent.Pause)
            val pausedState = awaitItem()
            assertFalse(pausedState.isPlaying)
            assertEquals(2, pausedState.stepIndexA)
            assertEquals(2, pausedState.stepIndexB)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-37: Real-time divergence triggers when operation delta exceeds divergenceThreshold")
    fun testDivergenceDetection() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()

            // Step through until divergence or completion
            while (!state.isDiverged && !state.isRaceFinished && state.stepIndexA < state.totalStepsA - 1) {
                viewModel.onIntent(RaceModeIntent.StepForward)
                state = awaitItem()
            }

            assertTrue(state.isDiverged)
            assertTrue(kotlin.math.abs(state.comparisonDelta) >= state.divergenceThreshold)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-37: Winner resolution declares first-to-finish algorithm and completes race")
    fun testWinnerResolution() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()

            // Advance until first algorithm finishes
            while (state.winnerAlgorithmId == null && state.stepIndexA < state.totalStepsA - 1 && state.stepIndexB < state.totalStepsB - 1) {
                viewModel.onIntent(RaceModeIntent.StepForward)
                state = awaitItem()
            }

            assertNotNull(state.winnerAlgorithmId)
            val winner = state.winnerAlgorithmId

            // Winner must be one of the two competing algorithms
            assertTrue(winner == state.algorithmA || winner == state.algorithmB)

            // Advance until race is finished
            while (!state.isRaceFinished) {
                viewModel.onIntent(RaceModeIntent.StepForward)
                state = awaitItem()
            }

            assertTrue(state.isRaceFinished)
            assertEquals(winner, state.winnerAlgorithmId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-37: Speed cycling updates playback speed and resets properly")
    fun testSpeedCyclingAndReset() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(1.0f, state.playbackSpeed)

            // Set speed to 2.0x
            viewModel.onIntent(RaceModeIntent.SetSpeed(2.0f))
            state = awaitItem()
            assertEquals(2.0f, state.playbackSpeed)

            // Set speed to 3.0x
            viewModel.onIntent(RaceModeIntent.SetSpeed(3.0f))
            state = awaitItem()
            assertEquals(3.0f, state.playbackSpeed)

            // Advance some steps
            viewModel.onIntent(RaceModeIntent.StepForward)
            state = awaitItem()
            assertEquals(1, state.stepIndexA)

            // Reset race
            viewModel.onIntent(RaceModeIntent.ResetRace)
            state = awaitItem()
            assertEquals(0, state.stepIndexA)
            assertEquals(0, state.stepIndexB)
            assertFalse(state.isPlaying)
            assertNull(state.winnerAlgorithmId)
            assertFalse(state.isRaceFinished)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
