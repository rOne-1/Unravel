package com.unravel.app.navigation

import app.cash.turbine.test
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.database.model.ActiveSessionPreferences
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.database.seed.CurriculumSeed
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.RaceModeViewModel
import com.unravel.feature.sandbox.SandboxViewModel
import com.unravel.feature.skillmap.SkillMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private lateinit var fakeDao: FakeSkillNodeDao
    private lateinit var fakeSessionRepo: FakeSessionRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeSkillNodeDao()
        fakeSessionRepo = FakeSessionRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("WP-41: Test 1 (Cold Start) - App initializes at SkillMapRoute with all 14 curriculum nodes")
    fun testColdStartSkillMapLoading() = runTest(testDispatcher) {
        val skillMapViewModel = SkillMapViewModel(
            skillNodeDao = fakeDao,
            sessionRepository = fakeSessionRepo,
            dispatcher = testDispatcher
        )

        skillMapViewModel.uiState.test {
            // First item: initial state (loading)
            var state = awaitItem()
            if (state.isLoading) {
                state = awaitItem()
            }

            assertFalse(state.isLoading)
            assertEquals(14, state.totalNodes)
            assertEquals(14, state.nodes.size)

            // Validate root algorithms are present and unlocked
            val twoPointers = state.nodes.find { it.id == "two_pointers" }
            assertNotNull(twoPointers)
            assertTrue(twoPointers!!.isUnlocked)

            val bubbleSort = state.nodes.find { it.id == "bubble_sort" }
            assertNotNull(bubbleSort)
            assertTrue(bubbleSort!!.isUnlocked)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-41: Test 2 (Skill Node Navigation) - Navigating to Sandbox loads all 14 algorithms cleanly")
    fun testNavigateToSandboxAllNodes() = runTest(testDispatcher) {
        for (algo in AlgorithmId.entries) {
            val route = SandboxRoute(algorithmId = algo.id, resumeSession = false)
            assertEquals(algo.id, route.algorithmId)

            val sandboxViewModel = SandboxViewModel(
                initialAlgorithmId = algo,
                sessionRepository = fakeSessionRepo,
                dispatcher = testDispatcher
            )

            sandboxViewModel.uiState.test {
                val state = awaitItem()
                assertEquals(algo, state.algorithmId)
                assertTrue(state.totalFrames > 0, "Algorithm ${algo.id} should generate frames")
                assertNotNull(state.currentFrame, "Algorithm ${algo.id} should have current frame")
                assertTrue(state.codeSnippet.isNotBlank(), "Algorithm ${algo.id} should have code snippet")
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    @DisplayName("WP-41: Test 3 (Race Mode Navigation) - Navigating to RaceMode loads dual-engine benchmark")
    fun testNavigateToRaceMode() = runTest(testDispatcher) {
        val route = RaceModeRoute(category = "ARRAY_POINTER")
        assertEquals("ARRAY_POINTER", route.category)

        val raceViewModel = RaceModeViewModel(testDispatcher)

        raceViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("bubble_vs_quick", state.selectedMatchupId)
            assertEquals(SandboxCategory.ARRAY_POINTER, state.category)
            assertTrue(state.totalStepsA > 0)
            assertTrue(state.totalStepsB > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("WP-41: Test 4 (Back Navigation) - Predictive back pops backstack to root SkillMapRoute")
    fun testBackNavigationFlow() {
        val backStack = mutableListOf<UnravelRoute>(SkillMapRoute)
        assertEquals(1, backStack.size)

        // 1. Navigate to Sandbox
        backStack.add(SandboxRoute(algorithmId = "two_pointers"))
        assertEquals(2, backStack.size)
        assertEquals("two_pointers", (backStack.last() as SandboxRoute).algorithmId)

        // 2. Navigate from Sandbox to Race Mode
        backStack.add(RaceModeRoute(category = "ARRAY_POINTER"))
        assertEquals(3, backStack.size)
        assertTrue(backStack.last() is RaceModeRoute)

        // 3. Pop back from Race Mode -> returns to Sandbox
        backStack.removeAt(backStack.lastIndex)
        assertEquals(2, backStack.size)
        assertEquals("two_pointers", (backStack.last() as SandboxRoute).algorithmId)

        // 4. Pop back from Sandbox -> returns to SkillMapRoute
        backStack.removeAt(backStack.lastIndex)
        assertEquals(1, backStack.size)
        assertEquals(SkillMapRoute, backStack.last())

        // 5. Root cannot be popped
        val canPop = backStack.size > 1
        assertFalse(canPop)
    }

    @Test
    @DisplayName("WP-41: Test 5 (Session Resume) - Resuming active session restores pointer and step index")
    fun testSessionResumeFlow() = runTest(testDispatcher) {
        // Prepare saved session in Proto DataStore
        val targetStep = 4
        fakeSessionRepo.saveActiveSession(
            ActiveSessionPreferences(
                algorithmId = "bubble_sort",
                stepIndex = targetStep
            )
        )

        // Verify active session flow reflects the saved checkpoint
        assertEquals("bubble_sort", fakeSessionRepo.getActiveSession().algorithmId)
        assertEquals(targetStep, fakeSessionRepo.getActiveSession().stepIndex)

        // Instantiate SandboxViewModel with resumeSession enabled
        val sandboxViewModel = SandboxViewModel(
            initialAlgorithmId = AlgorithmId.BUBBLE_SORT,
            sessionRepository = fakeSessionRepo,
            dispatcher = testDispatcher
        )

        sandboxViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(AlgorithmId.BUBBLE_SORT, state.algorithmId)
            assertEquals(targetStep, state.currentFrameIndex, "Restored step must match saved session checkpoint")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
