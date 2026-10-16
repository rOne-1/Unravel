package com.unravel.core.engine.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface VisualStructureState {

    @Serializable
    data class ArrayState(
        val items: @Serializable(with = ImmutableListSerializer::class) ImmutableList<ArrayNode>,
        val activeIndices: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<Int> = persistentSetOf(),
        val mutatingIndices: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<Int> = persistentSetOf(),
        val sortedIndices: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<Int> = persistentSetOf(),
        val discardedRange: @Serializable(with = IntRangeSerializer::class) IntRange? = null,
        val windowRange: @Serializable(with = IntRangeSerializer::class) IntRange? = null,
        val pointers: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, Int> = persistentMapOf(),
        val auxiliaryItems: @Serializable(with = ImmutableListSerializer::class) ImmutableList<ArrayNode> = persistentListOf()
    ) : VisualStructureState

    @Serializable
    data class LinkedNodeState(
        val nodes: @Serializable(with = ImmutableListSerializer::class) ImmutableList<LinkedNode>,
        val pointers: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, String> = persistentMapOf(),
        val activeLinkIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val mutatingLinkIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val lockedNodeIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val isCycleDetected: Boolean = false
    ) : VisualStructureState

    @Serializable
    data class HierarchyTreeState(
        val nodes: @Serializable(with = ImmutableListSerializer::class) ImmutableList<TreeNode>,
        val edges: @Serializable(with = ImmutableListSerializer::class) ImmutableList<TreeEdge> = persistentListOf(),
        val activeNodeId: String? = null,
        val rotatingSubtreeRootId: String? = null,
        val visitedNodeIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val traversalOrderIds: @Serializable(with = ImmutableListSerializer::class) ImmutableList<String> = persistentListOf()
    ) : VisualStructureState

    @Serializable
    data class SpatialGridGraphState(
        val mode: SpatialMode,
        val vertices: @Serializable(with = ImmutableListSerializer::class) ImmutableList<SpatialVertex>,
        val edges: @Serializable(with = ImmutableListSerializer::class) ImmutableList<SpatialEdge> = persistentListOf(),
        val frontierIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val visitedIds: @Serializable(with = ImmutableSetSerializer::class) ImmutableSet<String> = persistentSetOf(),
        val shortestPathIds: @Serializable(with = ImmutableListSerializer::class) ImmutableList<String> = persistentListOf(),
        val distanceMap: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, Int> = persistentMapOf(),
        val startVertexId: String,
        val targetVertexId: String
    ) : VisualStructureState
}
