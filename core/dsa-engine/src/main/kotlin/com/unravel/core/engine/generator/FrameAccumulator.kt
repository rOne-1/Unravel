package com.unravel.core.engine.generator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.AlgorithmMetrics
import com.unravel.core.engine.model.CallStackFrame
import com.unravel.core.engine.model.CodeSyncState
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.TokenRange
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

/**
 * Accumulates deterministic algorithm execution frames with cumulative telemetry tracking.
 * Strictly enforces MAX_FRAMES ceiling (1,500) per architecture blueprint §4.1.
 */
class FrameAccumulator(
    val algorithmId: AlgorithmId,
    val maxFrames: Int = AlgorithmGenerator.MAX_FRAMES
) {
    private val frames = mutableListOf<AlgorithmFrame>()
    private var comparisons: Int = 0
    private var mutations: Int = 0
    private var swaps: Int = 0
    private var maxRecursionDepth: Int = 0

    val currentComparisons: Int get() = comparisons
    val currentMutations: Int get() = mutations
    val currentSwaps: Int get() = swaps
    val frameCount: Int get() = frames.size

    fun incComparisons(delta: Int = 1) { comparisons += delta }
    fun incMutations(delta: Int = 1) { mutations += delta }
    fun incSwaps(delta: Int = 1) { swaps += delta }
    fun updateMaxDepth(depth: Int) {
        if (depth > maxRecursionDepth) maxRecursionDepth = depth
    }

    fun emitFrame(
        structureState: VisualStructureState,
        activeLineIndex: Int,
        variables: Map<String, String> = emptyMap(),
        tokenHighlights: List<TokenRange> = emptyList(),
        callStack: List<CallStackFrame> = emptyList(),
        hapticCue: HapticCue = HapticCue.NONE,
        expectedUserAction: ExpectedAction? = null
    ): Boolean {
        if (frames.size >= maxFrames) return false

        val allocated = when (structureState) {
            is VisualStructureState.ArrayState -> structureState.items.size + structureState.auxiliaryItems.size
            else -> 0
        }

        frames.add(
            AlgorithmFrame(
                stepIndex = frames.size,
                structureState = structureState,
                codeSync = CodeSyncState(
                    activeLineIndex = activeLineIndex,
                    tokenHighlights = tokenHighlights.toImmutableList(),
                    variableBindings = variables.toImmutableMap()
                ),
                callStack = callStack.toImmutableList(),
                metrics = AlgorithmMetrics(
                    comparisons = comparisons,
                    mutations = mutations,
                    swaps = swaps,
                    maxRecursionDepth = maxRecursionDepth,
                    allocatedElements = allocated
                ),
                hapticCue = hapticCue,
                expectedUserAction = expectedUserAction
            )
        )
        return true
    }

    fun build(): ImmutableList<AlgorithmFrame> = frames.toImmutableList()
}
