package com.unravel.feature.skillmap

import app.cash.turbine.test
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.database.seed.CurriculumSeed
import com.unravel.feature.skillmap.layout.SkillMapLayoutCoordinates
import com.unravel.feature.skillmap.model.MasteryState
import com.unravel.feature.skillmap.model.SkillMapIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkillMapViewModelTest {

    private class FakeSkillNodeDao(
        initialNodes: List<SkillNodeEntity> = CurriculumSeed.INITIAL_SKILL_NODES
    ) : SkillNodeDao {
        private val nodesFlow = MutableStateFlow(initialNodes)

        override fun getAllSkillNodes(): Flow<List<SkillNodeEntity>> = nodesFlow.asStateFlow()

        override suspend fun getSkillNodeById(id: String): SkillNodeEntity? =
            nodesFlow.value.find { it.id == id }

        override suspend fun updateMasteryState(id: String, masteryState: String) {
            nodesFlow.update { current ->
                current.map { if (it.id == id) it.copy(masteryState = masteryState) else it }
            }
        }

        override suspend fun updateUnlockedState(id: String, isUnlocked: Boolean) {
            nodesFlow.update { current ->
                current.map { if (it.id == id) it.copy(isUnlocked = isUnlocked) else it }
            }
        }

        override suspend fun insertSkillNodes(nodes: List<SkillNodeEntity>) {
            nodesFlow.value = nodes
        }

        override suspend fun insertSkillNode(node: SkillNodeEntity) {
            nodesFlow.update { current ->
                current.filter { it.id != node.id } + node
            }
        }

        override suspend fun getSkillNodeCount(): Int = nodesFlow.value.size
    }

    private class FakeSessionRepository(
        initialSession: ActiveSessionPreferences = ActiveSessionPreferences()
    ) : SessionRepository {
        private val sessionFlow = MutableStateFlow(initialSession)
        override val activeSessionFlow: StateFlow<ActiveSessionPreferences> = sessionFlow.asStateFlow()
        override fun getActiveSession(): ActiveSessionPreferences = sessionFlow.value
        override suspend fun saveActiveSession(session: ActiveSessionPreferences) {
            sessionFlow.value = session
        }
        override suspend fun clearSession() {
            sessionFlow.value = ActiveSessionPreferences()
        }
    }

    @Test
    @DisplayName("WP-30: ViewModel loads 14 curriculum nodes and cluster roots are unlocked by default")
    fun testInitialCurriculumLoading() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            // Initial state with loading = true
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            advanceUntilIdle()

            // Loaded state
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(14, loaded.nodes.size)
            assertEquals(4, loaded.clusterBounds.size)

            // Verify cluster root nodes are unlocked
            for (rootId in SkillMapLayoutCoordinates.CLUSTER_ROOT_IDS) {
                val rootNode = loaded.nodes.find { it.id == rootId }
                assertNotNull(rootNode, "Root node $rootId should exist")
                assertTrue(rootNode!!.isUnlocked, "Cluster root $rootId must be unlocked by default")
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-30: Mastering a node dynamically unlocks downstream dependent nodes")
    fun testPrerequisiteUnlocking() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            awaitItem() // Loading
            advanceUntilIdle()

            val state1 = awaitItem() // Initial loaded
            val slidingWindowBefore = state1.nodes.find { it.id == "sliding_window" }!!
            assertFalse(slidingWindowBefore.isUnlocked, "Sliding Window should initially be locked")

            // Mark two_pointers as MASTERED
            viewModel.processIntent(SkillMapIntent.UpdateMasteryState("two_pointers", MasteryState.MASTERED))
            advanceUntilIdle()

            val state2 = awaitItem()
            val twoPointersAfter = state2.nodes.find { it.id == "two_pointers" }!!
            assertEquals(MasteryState.MASTERED, twoPointersAfter.masteryState)

            val slidingWindowAfter = state2.nodes.find { it.id == "sliding_window" }!!
            assertTrue(slidingWindowAfter.isUnlocked, "Sliding Window should unlock when Two Pointers is mastered")

            val binarySearchAfter = state2.nodes.find { it.id == "binary_search" }!!
            assertTrue(binarySearchAfter.isUnlocked, "Binary Search should unlock when Two Pointers is mastered")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-30: Node selection and clear selection intents update selectedNodeId")
    fun testNodeSelection() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            viewModel.processIntent(SkillMapIntent.SelectNode("two_pointers"))
            val selected = awaitItem()
            assertEquals("two_pointers", selected.selectedNodeId)

            viewModel.processIntent(SkillMapIntent.ClearSelection)
            val cleared = awaitItem()
            assertNull(cleared.selectedNodeId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-30: Active session observation reflects Proto DataStore session")
    fun testActiveSessionObservation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository(
            ActiveSessionPreferences(
                algorithmId = "two_pointers",
                stepIndex = 14,
                playbackSpeed = 1.0f
            )
        )
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertNotNull(state.activeSession)
            assertEquals("two_pointers", state.activeSession?.algorithmId)
            assertEquals(14, state.activeSession?.stepIndex)

            // Clear session intent
            viewModel.processIntent(SkillMapIntent.ClearActiveSession)
            advanceUntilIdle()

            val clearedState = expectMostRecentItem()
            assertNull(clearedState.activeSession)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
