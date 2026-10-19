package com.unravel.core.engine.validator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.ExpectedAction

sealed interface ValidationResult {
    data class Valid(val nextFrameIndex: Int) : ValidationResult
    data class Invalid(val reason: String, val expectedAction: ExpectedAction) : ValidationResult
    data object NoActionExpected : ValidationResult
}

/**
 * Validates user-initiated gestures during Challenge Mode ("Be the Algorithm").
 * Performs fuzzy, order-independent symmetric swap matching and pointer verification.
 */
object ChallengeValidator {

    fun validateAction(
        currentFrame: AlgorithmFrame,
        userAction: ExpectedAction,
        nextFrameIndex: Int = currentFrame.stepIndex + 1
    ): ValidationResult {
        val expected = currentFrame.expectedUserAction ?: return ValidationResult.NoActionExpected

        val isValid = when {
            expected is ExpectedAction.SwapIndices && userAction is ExpectedAction.SwapIndices -> {
                // Order-independent symmetric swap: (A, B) == (i, j) or (A, B) == (j, i)
                (userAction.indexA == expected.indexA && userAction.indexB == expected.indexB) ||
                        (userAction.indexA == expected.indexB && userAction.indexB == expected.indexA)
            }
            expected is ExpectedAction.MovePointer && userAction is ExpectedAction.MovePointer -> {
                expected.pointerName == userAction.pointerName && expected.targetIndex == userAction.targetIndex
            }
            expected is ExpectedAction.RewirePointer && userAction is ExpectedAction.RewirePointer -> {
                expected.fromNodeId == userAction.fromNodeId && expected.newTargetNodeId == userAction.newTargetNodeId
            }
            expected is ExpectedAction.SelectBranch && userAction is ExpectedAction.SelectBranch -> {
                expected.targetNodeId == userAction.targetNodeId
            }
            expected is ExpectedAction.RotateSubtree && userAction is ExpectedAction.RotateSubtree -> {
                expected.pivotNodeId == userAction.pivotNodeId && expected.direction == userAction.direction
            }
            expected is ExpectedAction.ExpandFrontierNode && userAction is ExpectedAction.ExpandFrontierNode -> {
                expected.vertexId == userAction.vertexId
            }
            expected is ExpectedAction.KeepAndAdvance && userAction is ExpectedAction.KeepAndAdvance -> {
                true
            }
            else -> false
        }

        return if (isValid) {
            ValidationResult.Valid(nextFrameIndex)
        } else {
            val reason = when (expected) {
                is ExpectedAction.SwapIndices -> "Expected swap between indices ${expected.indexA} and ${expected.indexB}"
                is ExpectedAction.MovePointer -> "Expected pointer '${expected.pointerName}' to move to index ${expected.targetIndex}"
                is ExpectedAction.RewirePointer -> "Expected pointer from '${expected.fromNodeId}' to rewire to '${expected.newTargetNodeId}'"
                is ExpectedAction.SelectBranch -> "Expected selection of branch '${expected.targetNodeId}'"
                is ExpectedAction.RotateSubtree -> "Expected subtree rotation of '${expected.pivotNodeId}' ${expected.direction}"
                is ExpectedAction.ExpandFrontierNode -> "Expected frontier expansion to vertex '${expected.vertexId}'"
                ExpectedAction.KeepAndAdvance -> "Expected advance without swap"
            }
            ValidationResult.Invalid(reason, expected)
        }
    }
}
