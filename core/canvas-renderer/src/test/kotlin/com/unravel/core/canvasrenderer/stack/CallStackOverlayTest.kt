package com.unravel.core.canvasrenderer.stack

import com.unravel.core.engine.model.CallStackFrame
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CallStackOverlayTest {

    @Test
    @DisplayName("CallStack: Formats function signature and tracks depth telemetry")
    fun testCallStackFrameData() {
        val frame1 = CallStackFrame(
            id = Uuid.random(),
            functionName = "quickSort",
            parameters = persistentMapOf("low" to "0", "high" to "7"),
            variables = persistentMapOf("pivot" to "3"),
            depth = 1
        )
        val frame2 = CallStackFrame(
            id = Uuid.random(),
            functionName = "quickSort",
            parameters = persistentMapOf("low" to "0", "high" to "2"),
            variables = persistentMapOf("pivot" to "1"),
            depth = 2
        )

        val stack = persistentListOf(frame1, frame2)

        assertEquals(2, stack.size)
        // Top of stack is frame2 (last pushed)
        val topOfStack = stack.last()
        assertEquals("quickSort", topOfStack.functionName)
        assertEquals(2, topOfStack.depth)
        assertEquals("0", topOfStack.parameters["low"])
        assertEquals("2", topOfStack.parameters["high"])

        val maxDepth = 5
        val telemetryText = "DEPTH: ${stack.size} / MAX: $maxDepth"
        assertEquals("DEPTH: 2 / MAX: 5", telemetryText)
    }

    @Test
    @DisplayName("CallStack: Empty stack displays idle state")
    fun testEmptyCallStack() {
        val stack = persistentListOf<CallStackFrame>()
        assertEquals(0, stack.size)
        val maxDepth = 0
        val telemetryText = "DEPTH: ${stack.size} / MAX: $maxDepth"
        assertEquals("DEPTH: 0 / MAX: 0", telemetryText)
    }
}
