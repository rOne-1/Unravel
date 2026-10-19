package com.unravel.feature.skillmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.entity.SkillNodeEntity
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.database.seed.CurriculumSeed
import com.unravel.feature.skillmap.layout.SkillMapLayoutCoordinates
import com.unravel.feature.skillmap.model.MasteryState
import com.unravel.feature.skillmap.model.SkillMapIntent
import com.unravel.feature.skillmap.model.SkillMapUiState
import com.unravel.feature.skillmap.model.SkillNodeUiModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the interactive 2D DAG Skill Map.
 * Queries Room for curriculum nodes, resolves prerequisite unlock dependencies,
 * and monitors Proto DataStore for active session resumption.
 */
class SkillMapViewModel(
    private val skillNodeDao: SkillNodeDao,
    private val sessionRepository: SessionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SkillMapUiState(
            clusterBounds = SkillMapLayoutCoordinates.CLUSTER_BOUNDS,
            isLoading = true
        )
    )
    val uiState: StateFlow<SkillMapUiState> = _uiState.asStateFlow()

    init {
        observeSkillNodes()
        observeActiveSession()
    }

    /**
     * Dispatches user or system intents into the Skill Map state machine.
     */
    fun processIntent(intent: SkillMapIntent) {
        when (intent) {
            is SkillMapIntent.SelectNode -> selectNode(intent.nodeId)
            SkillMapIntent.ClearSelection -> clearSelection()
            is SkillMapIntent.UpdateMasteryState -> updateMastery(intent.nodeId, intent.masteryState)
            SkillMapIntent.ClearActiveSession -> clearActiveSession()
            SkillMapIntent.RefreshGraph -> observeSkillNodes()
        }
    }

    /**
     * Alias for [processIntent] adhering to standard MVI nomenclature.
     */
    fun onIntent(intent: SkillMapIntent) = processIntent(intent)

    private fun observeSkillNodes() {
        viewModelScope.launch(dispatcher) {
            skillNodeDao.getAllSkillNodes().collect { entities ->
                val activeEntities = if (entities.isEmpty()) {
                    // Seed initial curriculum if empty
                    val seeded = CurriculumSeed.INITIAL_SKILL_NODES
                    skillNodeDao.insertSkillNodes(seeded)
                    seeded
                } else {
                    entities
                }

                val entityMap = activeEntities.associateBy { it.id }

                val uiNodes = activeEntities.map { entity ->
                    val isUnlocked = computeIsUnlocked(entity, entityMap)

                    // Keep DB synchronized with resolved unlock state
                    if (isUnlocked != entity.isUnlocked) {
                        launch(dispatcher) {
                            skillNodeDao.updateUnlockedState(entity.id, isUnlocked)
                        }
                    }

                    val coords = SkillMapLayoutCoordinates.NODE_COORDINATES[entity.id]
                    val x = coords?.first ?: 0f
                    val y = coords?.second ?: 0f

                    SkillNodeUiModel(
                        id = entity.id,
                        title = entity.title,
                        category = entity.category,
                        clusterIndex = entity.clusterIndex,
                        prerequisiteIds = entity.prerequisiteIds.toImmutableList(),
                        masteryState = entity.masteryState,
                        isUnlocked = isUnlocked,
                        x = x,
                        y = y
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        nodes = uiNodes.toImmutableList(),
                        totalNodes = uiNodes.size,
                        masteredCount = uiNodes.count { it.masteryState == MasteryState.MASTERED },
                        unlockedCount = uiNodes.count { it.isUnlocked },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeActiveSession() {
        viewModelScope.launch(dispatcher) {
            sessionRepository.activeSessionFlow.collect { session ->
                _uiState.update { state ->
                    state.copy(
                        activeSession = session.takeIf { it.algorithmId.isNotBlank() }
                    )
                }
            }
        }
    }

    private fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedNodeId = null) }
    }

    private fun updateMastery(nodeId: String, masteryState: String) {
        viewModelScope.launch(dispatcher) {
            skillNodeDao.updateMasteryState(nodeId, masteryState)
        }
    }

    private fun clearActiveSession() {
        viewModelScope.launch(dispatcher) {
            sessionRepository.clearSession()
        }
    }

    /**
     * Resolves unlock status:
     * - Cluster roots and nodes with empty prerequisites are unlocked on fresh state.
     * - Downstream nodes unlock when all prerequisites have [MasteryState.MASTERED].
     */
    private fun computeIsUnlocked(
        node: SkillNodeEntity,
        allNodesMap: Map<String, SkillNodeEntity>
    ): Boolean {
        if (node.isUnlocked) return true
        if (node.prerequisiteIds.isEmpty()) return true
        if (node.id in SkillMapLayoutCoordinates.CLUSTER_ROOT_IDS) return true

        return node.prerequisiteIds.all { prereqId ->
            allNodesMap[prereqId]?.masteryState == MasteryState.MASTERED
        }
    }
}
