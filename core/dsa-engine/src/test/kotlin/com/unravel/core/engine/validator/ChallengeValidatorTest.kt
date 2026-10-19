package com.unravel.core.engine.validator

import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmMetrics
import com.unravel.core.engine.model.CodeSyncState
import com.unravel.core.engine.model.ExpectedAction
import com.unravel.core.engine.model.HapticCue
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChallengeValidatorTest {

    private fun createFrame(expectedAction: ExpectedAction?): AlgorithmFrame {
        return AlgorithmFrame(
            stepIndex = 3,
            structureState = VisualStructureState.ArrayState(items = persistentListOf()),
            codeSync = CodeSyncState(activeLineIndex = 1),
            metrics = AlgorithmMetrics(),
            hapticCue = HapticCue.NONE,
            expectedUserAction = expectedAction
        )
    }

    @Test
    fun `order-independent symmetric swap validation matches in both directions`() {
        val frame = createFrame(ExpectedAction.SwapIndices(2, 5))

        // Direct match (2, 5)
        val directResult = ChallengeValidator.validateAction(frame, ExpectedAction.SwapIndices(2, 5), nextFrameIndex = 4)
        assertTrue(directResult is ValidationResult.Valid)
        assertEquals(4, (directResult as ValidationResult.Valid).nextFrameIndex)

        // Symmetric match (5, 2)
        val symmetricResult = ChallengeValidator.validateAction(frame, ExpectedAction.SwapIndices(5, 2), nextFrameIndex = 4)
        assertTrue(symmetricResult is ValidationResult.Valid)
        assertEquals(4, (symmetricResult as ValidationResult.Valid).nextFrameIndex)
    }

    @Test
    fun `mismatched swap returns Invalid with descriptive reason`() {
        val frame = createFrame(ExpectedAction.SwapIndices(2, 5))

        val invalidResult = ChallengeValidator.validateAction(frame, ExpectedAction.SwapIndices(2, 4))
        assertTrue(invalidResult is ValidationResult.Invalid)
        val invalid = invalidResult as ValidationResult.Invalid
        assertEquals(ExpectedAction.SwapIndices(2, 5), invalid.expectedAction)
        assertTrue(invalid.reason.contains("2 and 5"))
    }

    @Test
    fun `pointer movement validation succeeds on exact pointer and target index`() {
        val frame = createFrame(ExpectedAction.MovePointer("left", 3))

        val validResult = ChallengeValidator.validateAction(frame, ExpectedAction.MovePointer("left", 3), nextFrameIndex = 10)
        assertTrue(validResult is ValidationResult.Valid)
        assertEquals(10, (validResult as ValidationResult.Valid).nextFrameIndex)

        // Wrong index
        val wrongIndexResult = ChallengeValidator.validateAction(frame, ExpectedAction.MovePointer("left", 4))
        assertTrue(wrongIndexResult is ValidationResult.Invalid)

        // Wrong pointer name
        val wrongNameResult = ChallengeValidator.validateAction(frame, ExpectedAction.MovePointer("right", 3))
        assertTrue(wrongNameResult is ValidationResult.Invalid)
    }

    @Test
    fun `keep and advance validation succeeds on exact action`() {
        val frame = createFrame(ExpectedAction.KeepAndAdvance)

        val validResult = ChallengeValidator.validateAction(frame, ExpectedAction.KeepAndAdvance)
        assertTrue(validResult is ValidationResult.Valid)

        val invalidResult = ChallengeValidator.validateAction(frame, ExpectedAction.SwapIndices(0, 1))
        assertTrue(invalidResult is ValidationResult.Invalid)
    }

    @Test
    fun `validation returns NoActionExpected when current frame has null expected action`() {
        val frame = createFrame(null)
        val result = ChallengeValidator.validateAction(frame, ExpectedAction.KeepAndAdvance)
        assertTrue(result is ValidationResult.NoActionExpected)
    }
}
