package com.unravel.feature.racemode

import app.cash.turbine.test
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.model.RaceModeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
class RaceModeViewModelTest {

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
    @DisplayName("WP-34: Initial state loads default BubbleSort vs QuickSort matchup with populated frames")
    fun testInitialMatchupLoading() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("bubble_vs_quick", state.selectedMatchupId)
            assertEquals(AlgorithmId.BUBBLE_SORT, state.algorithmA)
            assertEquals(AlgorithmId.QUICK_SORT, state.algorithmB)
            assertEquals(SandboxCategory.ARRAY_POINTER, state.category)
            assertTrue(state.totalStepsA > 0)
            assertTrue(state.totalStepsB > 0)
            assertEquals(0, state.stepIndexA)
            assertEquals(0, state.stepIndexB)
            assertFalse(state.isPlaying)
            assertNull(state.winnerAlgorithmId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-34: Step forward advances both engines simultaneously and computes operation deltas")
    fun testSynchronizedStepForward() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(0, state.stepIndexA)
            assertEquals(0, state.stepIndexB)

            viewModel.onIntent(RaceModeIntent.StepForward)
            state = awaitItem()
            assertEquals(1, state.stepIndexA)
            assertEquals(1, state.stepIndexB)

            // Step metrics delta check
            val cmpA = state.currentFrameA?.metrics?.comparisons ?: 0
            val cmpB = state.currentFrameB?.metrics?.comparisons ?: 0
            assertEquals(cmpA - cmpB, state.comparisonDelta)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-34: Divergence flag triggers when comparison gap exceeds threshold")
    fun testDivergenceTrigger() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()

            // Step forward repeatedly until divergence or finish
            for (i in 0 until 15) {
                viewModel.onIntent(RaceModeIntent.StepForward)
                state = awaitItem()
            }

            // QuickSort will drastically diverge in comparisons compared to BubbleSort
            if (kotlin.math.abs(state.comparisonDelta) >= state.divergenceThreshold) {
                assertTrue(state.isDiverged)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-34: Reset race returns indices to 0 and clears winner state")
    fun testResetRace() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()

            viewModel.onIntent(RaceModeIntent.StepForward)
            state = awaitItem()
            assertEquals(1, state.stepIndexA)

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

    @Test
    @DisplayName("WP-34: Category switching loads category default matchup and generates shared frames")
    fun testCategorySwitching() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onIntent(RaceModeIntent.SelectCategory(SandboxCategory.SPATIAL_GRID_GRAPH))
            val state = awaitItem()

            assertEquals(SandboxCategory.SPATIAL_GRID_GRAPH, state.category)
            assertEquals(AlgorithmId.GRAPH_BFS_DFS, state.algorithmA)
            assertEquals(AlgorithmId.DIJKSTRA_ASTAR, state.algorithmB)
            assertTrue(state.totalStepsA > 0)
            assertTrue(state.totalStepsB > 0)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-34: Dual ticker advances frames over time when playing")
    fun testPlaybackTicker() = runTest(testDispatcher) {
        val viewModel = RaceModeViewModel(testDispatcher)

        viewModel.uiState.test {
            var state = awaitItem()
            assertFalse(state.isPlaying)

            viewModel.onIntent(RaceModeIntent.Play)
            state = awaitItem()
            assertTrue(state.isPlaying)

            // Advance test dispatcher clock by 800ms (at 1x speed, ~2 ticks)
            advanceTimeBy(800)

            state = expectMostRecentItem()
            assertTrue(state.stepIndexA > 0)
            assertTrue(state.stepIndexB > 0)

            viewModel.onIntent(RaceModeIntent.Pause)
            state = awaitItem()
            assertFalse(state.isPlaying)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
