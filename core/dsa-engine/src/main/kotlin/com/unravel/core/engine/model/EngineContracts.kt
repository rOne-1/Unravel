package com.unravel.core.engine.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class CallStackFrame(
    val id: Uuid = Uuid.random(),
    val functionName: String,
    val parameters: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, String> = persistentMapOf(),
    val variables: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, String> = persistentMapOf(),
    val depth: Int = 0
)

@Serializable
data class TokenRange(
    val lineIndex: Int,
    val startOffset: Int,
    val endOffset: Int
)

@Serializable
data class CodeSyncState(
    val activeLineIndex: Int = 0,
    val tokenHighlights: @Serializable(with = ImmutableListSerializer::class) ImmutableList<TokenRange> = persistentListOf(),
    val variableBindings: @Serializable(with = ImmutableMapSerializer::class) ImmutableMap<String, String> = persistentMapOf()
)

@Serializable
data class AlgorithmMetrics(
    val comparisons: Int = 0,
    val mutations: Int = 0,
    val swaps: Int = 0,
    val maxRecursionDepth: Int = 0,
    val allocatedElements: Int = 0
)

@Serializable
sealed interface ExpectedAction {
    @Serializable
    data class SwapIndices(val indexA: Int, val indexB: Int) : ExpectedAction
    @Serializable
    data class MovePointer(val pointerName: String, val targetIndex: Int) : ExpectedAction
    @Serializable
    data class RewirePointer(val fromNodeId: String, val newTargetNodeId: String?) : ExpectedAction
    @Serializable
    data class SelectBranch(val targetNodeId: String) : ExpectedAction
    @Serializable
    data class RotateSubtree(val pivotNodeId: String, val direction: RotationDirection) : ExpectedAction
    @Serializable
    data class ExpandFrontierNode(val vertexId: String) : ExpectedAction
    @Serializable
    data object KeepAndAdvance : ExpectedAction
}

@Serializable
data class AlgorithmFrame(
    val stepIndex: Int,
    val structureState: VisualStructureState,
    val codeSync: CodeSyncState,
    val callStack: @Serializable(with = ImmutableListSerializer::class) ImmutableList<CallStackFrame> = persistentListOf(),
    val metrics: AlgorithmMetrics,
    val hapticCue: HapticCue = HapticCue.NONE,
    val expectedUserAction: ExpectedAction? = null
)

/**
 * Fundamental engine contract defining deterministic sequence generation for an algorithm.
 */
interface AlgorithmGenerator<T : VisualStructureState> {
    val algorithmId: AlgorithmId

    /**
     * Generates a deterministic sequence of immutable frames from an initial structure state.
     * Enforces the hard frame ceiling MAX_FRAMES = 1_500 per blueprint §4.1.
     */
    fun generateFrames(initialState: T): ImmutableList<AlgorithmFrame>

    companion object {
        const val MAX_FRAMES = 1_500
    }
}
