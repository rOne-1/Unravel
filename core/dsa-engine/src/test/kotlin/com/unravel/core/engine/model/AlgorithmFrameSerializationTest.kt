package com.unravel.core.engine.model

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AlgorithmFrameSerializationTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = false
    }

    @Test
    fun testArrayStateSerializationRoundtrip() {
        val node1 = ArrayNode(id = Uuid.random(), value = 42)
        val node2 = ArrayNode(id = Uuid.random(), value = 17)
        val arrayState = VisualStructureState.ArrayState(
            items = persistentListOf(node1, node2),
            activeIndices = persistentSetOf(0),
            mutatingIndices = persistentSetOf(1),
            sortedIndices = persistentSetOf(),
            discardedRange = 2..5,
            windowRange = 0..1,
            pointers = persistentMapOf("i" to 0, "j" to 1),
            auxiliaryItems = persistentListOf(ArrayNode(id = Uuid.random(), value = 99))
        )

        val frame = AlgorithmFrame(
            stepIndex = 14,
            structureState = arrayState,
            codeSync = CodeSyncState(
                activeLineIndex = 3,
                tokenHighlights = persistentListOf(TokenRange(3, 4, 12)),
                variableBindings = persistentMapOf("i" to "0", "j" to "1")
            ),
            callStack = persistentListOf(
                CallStackFrame(
                    functionName = "quickSort",
                    parameters = persistentMapOf("low" to "0", "high" to "1"),
                    depth = 1
                )
            ),
            metrics = AlgorithmMetrics(comparisons = 12, mutations = 4, swaps = 2),
            hapticCue = HapticCue.SWAP_THUD,
            expectedUserAction = ExpectedAction.SwapIndices(0, 1)
        )

        val encoded = json.encodeToString(AlgorithmFrame.serializer(), frame)
        val decoded = json.decodeFromString(AlgorithmFrame.serializer(), encoded)

        assertEquals(frame, decoded)
        assertTrue(decoded.structureState is VisualStructureState.ArrayState)
        assertEquals(HapticCue.SWAP_THUD, decoded.hapticCue)
        assertEquals(ExpectedAction.SwapIndices(0, 1), decoded.expectedUserAction)
    }

    @Test
    fun testLinkedNodeStateSerializationRoundtrip() {
        val id1 = Uuid.random()
        val id2 = Uuid.random()
        val node1 = LinkedNode(id = id1, value = 10, nextId = id2)
        val node2 = LinkedNode(id = id2, value = 20, prevId = id1)

        val linkedState = VisualStructureState.LinkedNodeState(
            nodes = persistentListOf(node1, node2),
            pointers = persistentMapOf("head" to id1.toString(), "curr" to id2.toString()),
            activeLinkIds = persistentSetOf(id1.toString()),
            mutatingLinkIds = persistentSetOf(),
            lockedNodeIds = persistentSetOf(id2.toString()),
            isCycleDetected = true
        )

        val frame = AlgorithmFrame(
            stepIndex = 7,
            structureState = linkedState,
            codeSync = CodeSyncState(activeLineIndex = 5),
            metrics = AlgorithmMetrics(comparisons = 3, mutations = 1),
            hapticCue = HapticCue.SUCCESS_LOCK,
            expectedUserAction = ExpectedAction.RewirePointer(id1.toString(), null)
        )

        val encoded = json.encodeToString(AlgorithmFrame.serializer(), frame)
        val decoded = json.decodeFromString(AlgorithmFrame.serializer(), encoded)

        assertEquals(frame, decoded)
        assertTrue(decoded.structureState is VisualStructureState.LinkedNodeState)
        val decodedState = decoded.structureState as VisualStructureState.LinkedNodeState
        assertTrue(decodedState.isCycleDetected)
    }

    @Test
    fun testHierarchyTreeStateSerializationRoundtrip() {
        val rootId = Uuid.random()
        val leftId = Uuid.random()
        val rightId = Uuid.random()

        val rootNode = TreeNode(id = rootId, value = 50, leftId = leftId, rightId = rightId, height = 2, balanceFactor = 0)
        val leftNode = TreeNode(id = leftId, value = 30, height = 1, balanceFactor = 0)
        val rightNode = TreeNode(id = rightId, value = 70, height = 1, balanceFactor = 0)

        val treeState = VisualStructureState.HierarchyTreeState(
            nodes = persistentListOf(rootNode, leftNode, rightNode),
            edges = persistentListOf(
                TreeEdge(fromId = rootId, toId = leftId, isHighlighted = true),
                TreeEdge(fromId = rootId, toId = rightId)
            ),
            activeNodeId = leftId.toString(),
            rotatingSubtreeRootId = null,
            visitedNodeIds = persistentSetOf(rootId.toString()),
            traversalOrderIds = persistentListOf(rootId.toString(), leftId.toString())
        )

        val frame = AlgorithmFrame(
            stepIndex = 3,
            structureState = treeState,
            codeSync = CodeSyncState(activeLineIndex = 8),
            metrics = AlgorithmMetrics(comparisons = 2, maxRecursionDepth = 2),
            hapticCue = HapticCue.TICK,
            expectedUserAction = ExpectedAction.RotateSubtree(rootId.toString(), RotationDirection.RIGHT)
        )

        val encoded = json.encodeToString(AlgorithmFrame.serializer(), frame)
        val decoded = json.decodeFromString(AlgorithmFrame.serializer(), encoded)

        assertEquals(frame, decoded)
        assertTrue(decoded.structureState is VisualStructureState.HierarchyTreeState)
    }

    @Test
    fun testSpatialGridGraphStateSerializationRoundtrip() {
        val vStart = SpatialVertex(id = Uuid.random(), label = "S", gridRow = 0, gridCol = 0, weight = 1)
        val vTarget = SpatialVertex(id = Uuid.random(), label = "T", gridRow = 2, gridCol = 2, weight = 1)
        val vObstacle = SpatialVertex(id = Uuid.random(), label = "X", gridRow = 1, gridCol = 1, isObstacle = true)

        val graphState = VisualStructureState.SpatialGridGraphState(
            mode = SpatialMode.GRID_2D,
            vertices = persistentListOf(vStart, vTarget, vObstacle),
            edges = persistentListOf(SpatialEdge(fromId = vStart.id, toId = vTarget.id, weight = 2)),
            frontierIds = persistentSetOf(vStart.id.toString()),
            visitedIds = persistentSetOf(),
            shortestPathIds = persistentListOf(vStart.id.toString(), vTarget.id.toString()),
            distanceMap = persistentMapOf(vStart.id.toString() to 0, vTarget.id.toString() to 2),
            startVertexId = vStart.id.toString(),
            targetVertexId = vTarget.id.toString()
        )

        val frame = AlgorithmFrame(
            stepIndex = 1,
            structureState = graphState,
            codeSync = CodeSyncState(activeLineIndex = 12),
            metrics = AlgorithmMetrics(comparisons = 1, allocatedElements = 3),
            hapticCue = HapticCue.NONE,
            expectedUserAction = ExpectedAction.ExpandFrontierNode(vStart.id.toString())
        )

        val encoded = json.encodeToString(AlgorithmFrame.serializer(), frame)
        val decoded = json.decodeFromString(AlgorithmFrame.serializer(), encoded)

        assertEquals(frame, decoded)
        assertTrue(decoded.structureState is VisualStructureState.SpatialGridGraphState)
    }

    @Test
    fun testAllExpectedActionsSerializationRoundtrip() {
        val actions = listOf(
            ExpectedAction.SwapIndices(2, 5),
            ExpectedAction.MovePointer("pivot", 4),
            ExpectedAction.RewirePointer("node-1", "node-2"),
            ExpectedAction.SelectBranch("node-3"),
            ExpectedAction.RotateSubtree("root", RotationDirection.LEFT),
            ExpectedAction.ExpandFrontierNode("vertex-1"),
            ExpectedAction.KeepAndAdvance
        )

        for (action in actions) {
            val encoded = json.encodeToString(ExpectedAction.serializer(), action)
            val decoded = json.decodeFromString(ExpectedAction.serializer(), encoded)
            assertEquals(action, decoded)
        }
    }
}
