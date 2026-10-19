package com.unravel.feature.skillmap

import app.cash.turbine.test
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.database.seed.CurriculumSeed
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
class SkillMapIntegrationTest {

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
    @DisplayName("WP-33: Mastering two_pointers dynamically unlocks downstream nodes in Turbine flow")
    fun testUnlockCascadeWithTurbine() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val customNodes = listOf(
            SkillNodeEntity(
                id = "two_pointers",
                title = "Two Pointers",
                clusterIndex = 1,
                category = "ARRAY",
                prerequisiteIds = emptyList(),
                masteryState = MasteryState.UNEXPLORED,
                isUnlocked = true
            ),
            SkillNodeEntity(
                id = "sliding_window",
                title = "Sliding Window",
                clusterIndex = 1,
                category = "ARRAY",
                prerequisiteIds = listOf("two_pointers"),
                masteryState = MasteryState.UNEXPLORED,
                isUnlocked = false
            ),
            SkillNodeEntity(
                id = "binary_search",
                title = "Binary Search",
                clusterIndex = 1,
                category = "ARRAY",
                prerequisiteIds = listOf("two_pointers"),
                masteryState = MasteryState.UNEXPLORED,
                isUnlocked = false
            )
        )
        val dao = FakeSkillNodeDao(customNodes)
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            advanceUntilIdle()
            val stateBefore = expectMostRecentItem()
            val childBefore = stateBefore.nodes.first { it.id == "sliding_window" }
            assertFalse(childBefore.isUnlocked)

            // User masters two_pointers
            viewModel.onIntent(SkillMapIntent.UpdateMasteryState("two_pointers", MasteryState.MASTERED))
            advanceUntilIdle()

            val stateAfter = expectMostRecentItem()
            val childAfter = stateAfter.nodes.first { it.id == "sliding_window" }
            assertTrue(childAfter.isUnlocked)
            assertEquals(1, stateAfter.masteredCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-33: Node selection and clearing updates UI state")
    fun testSelectionLifecycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertNull(state.selectedNodeId)

            viewModel.onIntent(SkillMapIntent.SelectNode("bubble_sort"))
            state = awaitItem()
            assertEquals("bubble_sort", state.selectedNodeId)

            viewModel.onIntent(SkillMapIntent.ClearSelection)
            state = awaitItem()
            assertNull(state.selectedNodeId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-33: Active session is observed and clearing resets to empty state")
    fun testActiveSessionClearing() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val initialSession = ActiveSessionPreferences(
            algorithmId = "quick_sort",
            stepIndex = 14,
            playbackSpeed = 2.0f
        )
        val repo = FakeSessionRepository(initialSession)
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertNotNull(state.activeSession)
            assertEquals("quick_sort", state.activeSession?.algorithmId)

            viewModel.onIntent(SkillMapIntent.ClearActiveSession)
            advanceUntilIdle()

            state = expectMostRecentItem()
            assertNull(state.activeSession)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-33: Skill map state exposes all 4 curriculum cluster bounds")
    fun testClusterBoundsStructure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dao = FakeSkillNodeDao()
        val repo = FakeSessionRepository()
        val viewModel = SkillMapViewModel(dao, repo, testDispatcher)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(4, state.clusterBounds.size)
            assertEquals("LINEAR FOUNDATIONS", state.clusterBounds[0].title)
            assertEquals("SORTING & RECURSION", state.clusterBounds[1].title)
            assertEquals("POINTER MECHANICS", state.clusterBounds[2].title)
            assertEquals("NON-LINEAR & SPATIAL", state.clusterBounds[3].title)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
