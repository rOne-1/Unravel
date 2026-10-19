package com.unravel.feature.sandbox.engine

import com.unravel.core.engine.generator.AvlRotationsGenerator
import com.unravel.core.engine.generator.BinarySearchGenerator
import com.unravel.core.engine.generator.BstOperationsGenerator
import com.unravel.core.engine.generator.BubbleSortGenerator
import com.unravel.core.engine.generator.DijkstraAStarGenerator
import com.unravel.core.engine.generator.FloydCycleGenerator
import com.unravel.core.engine.generator.GraphBfsDfsGenerator
import com.unravel.core.engine.generator.InputPreset
import com.unravel.core.engine.generator.InputPresetGenerator
import com.unravel.core.engine.generator.InsertionSortGenerator
import com.unravel.core.engine.generator.LinkedListReversalGenerator
import com.unravel.core.engine.generator.MergeSortGenerator
import com.unravel.core.engine.generator.QuickSortGenerator
import com.unravel.core.engine.generator.SlidingWindowGenerator
import com.unravel.core.engine.generator.StackQueueGenerator
import com.unravel.core.engine.generator.TwoPointersGenerator
import com.unravel.core.engine.layout.GraphLayoutEngine
import com.unravel.core.engine.model.AlgorithmFrame
import com.unravel.core.engine.model.AlgorithmGenerator
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.VisualStructureState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Registry providing pre-computed or on-demand deterministic sequence generation
 * and canonical syntax code snippets for all 14 curriculum algorithms.
 */
object AlgorithmCatalog {

    /**
     * Generates the deterministic sequence frames for a given algorithm and preset.
     */
    fun generateFrames(
        algorithmId: AlgorithmId,
        preset: InputPreset = InputPreset.RANDOM,
        arraySize: Int = 10,
        seed: Long? = 42L
    ): ImmutableList<AlgorithmFrame> {
        val (generator, initialState) = createGeneratorAndState(algorithmId, preset, arraySize, seed)
        @Suppress("UNCHECKED_CAST")
        val typedGenerator = generator as AlgorithmGenerator<VisualStructureState>
        return typedGenerator.generateFrames(initialState)
    }

    /**
     * Returns the formatted source code representation aligned with 1-based active line indices.
     */
    fun getCodeSnippet(algorithmId: AlgorithmId): String {
        return when (algorithmId) {
            AlgorithmId.TWO_POINTERS -> """
                var left = 0, right = items.size - 1
                while (left < right) {
                    val currentSum = arr[left] + arr[right]
                    if (currentSum == target) return Pair(left, right)
                    else if (currentSum < target) left++
                    else right--
                }
                return null
            """.trimIndent()

            AlgorithmId.SLIDING_WINDOW -> """
                var windowSum = 0, maxSeen = 0
                for (right in 0 until n) {
                    windowSum += arr[right]
                    if (right >= k) {
                        windowSum -= arr[right - k]
                    }
                    maxSeen = maxOf(maxSeen, windowSum)
                }
                return maxSeen
            """.trimIndent()

            AlgorithmId.BINARY_SEARCH -> """
                var low = 0, high = n - 1
                while (low <= high) {
                    val mid = low + (high - low) / 2
                    if (arr[mid] == target) {
                        return mid
                    } else if (arr[mid] < target) {
                        low = mid + 1
                    } else {
                        high = mid - 1
                    }
                }
                return -1
            """.trimIndent()

            AlgorithmId.BUBBLE_SORT -> """
                for (i in 0 until n - 1) {
                    for (j in 0 until n - i - 1) {
                        if (arr[j] > arr[j + 1]) {
                            swap(arr, j, j + 1)
                        }
                    }
                }
            """.trimIndent()

            AlgorithmId.INSERTION_SORT -> """
                for (i in 1 until n) {
                    val key = arr[i]
                    var j = i - 1
                    while (j >= 0 && arr[j] > key) {
                        arr[j + 1] = arr[j]
                        j--
                    }
                    arr[j + 1] = key
                }
            """.trimIndent()

            AlgorithmId.MERGE_SORT -> """
                fun mergeSort(arr, low, high) {
                    if (low >= high) return
                    val mid = low + (high - low) / 2
                    mergeSort(arr, low, mid)
                    mergeSort(arr, mid + 1, high)
                    merge(arr, low, mid, high)
                }
            """.trimIndent()

            AlgorithmId.QUICK_SORT -> """
                fun quickSort(arr, low, high) {
                    if (low >= high) return
                    val pivot = partition(arr, low, high)
                    quickSort(arr, low, pivot - 1)
                    quickSort(arr, pivot + 1, high)
                }
            """.trimIndent()

            AlgorithmId.LINKED_LIST_REVERSAL -> """
                var prev: Node? = null, curr = head
                while (curr != null) {
                    val next = curr.next
                    curr.next = prev
                    prev = curr
                    curr = next
                }
                return prev
            """.trimIndent()

            AlgorithmId.FLOYD_CYCLE -> """
                var slow = head, fast = head
                while (fast != null && fast.next != null) {
                    slow = slow.next
                    fast = fast.next.next
                    if (slow == fast) return true
                }
                return false
            """.trimIndent()

            AlgorithmId.STACK_QUEUE_LIFO_FIFO -> """
                fun push(item: T) { stack.addLast(item) }
                fun pop(): T { return stack.removeLast() }
                fun enqueue(item: T) { queue.addLast(item) }
                fun dequeue(): T { return queue.removeFirst() }
            """.trimIndent()

            AlgorithmId.BST_OPERATIONS -> """
                fun insert(root: Node?, value: Int): Node {
                    if (root == null) return Node(value)
                    if (value < root.value) root.left = insert(root.left, value)
                    else root.right = insert(root.right, value)
                    return root
                }
            """.trimIndent()

            AlgorithmId.AVL_ROTATIONS -> """
                val balance = getBalance(node)
                if (balance > 1 && value < node.left.value) rotateRight(node)
                if (balance < -1 && value > node.right.value) rotateLeft(node)
                if (balance > 1 && value > node.left.value) rotateLeftRight(node)
                if (balance < -1 && value < node.right.value) rotateRightLeft(node)
            """.trimIndent()

            AlgorithmId.GRAPH_BFS_DFS -> """
                queue.add(startNode)
                visited.add(startNode)
                while (queue.isNotEmpty()) {
                    val curr = queue.removeFirst()
                    for (neighbor in curr.neighbors) {
                        if (neighbor !in visited) {
                            visited.add(neighbor)
                            queue.add(neighbor)
                        }
                    }
                }
            """.trimIndent()

            AlgorithmId.DIJKSTRA_ASTAR -> """
                pq.add(startNode, priority = 0)
                dist[startNode] = 0
                while (pq.isNotEmpty()) {
                    val curr = pq.extractMin()
                    for (edge in curr.edges) {
                        val newDist = dist[curr] + edge.weight + heuristic(edge.target)
                        if (newDist < dist[edge.target]) {
                            dist[edge.target] = newDist
                            pq.insertOrUpdate(edge.target, newDist)
                        }
                    }
                }
            """.trimIndent()
        }
    }

    private fun createGeneratorAndState(
        algorithmId: AlgorithmId,
        preset: InputPreset,
        arraySize: Int,
        seed: Long?
    ): Pair<AlgorithmGenerator<*>, VisualStructureState> {
        return when (algorithmId) {
            AlgorithmId.TWO_POINTERS -> Pair(
                TwoPointersGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.SLIDING_WINDOW -> Pair(
                SlidingWindowGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.BINARY_SEARCH -> Pair(
                BinarySearchGenerator(),
                InputPresetGenerator.generateInitialState(InputPreset.NEARLY_SORTED, size = arraySize, seed = seed)
            )
            AlgorithmId.BUBBLE_SORT -> Pair(
                BubbleSortGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.INSERTION_SORT -> Pair(
                InsertionSortGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.MERGE_SORT -> Pair(
                MergeSortGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.QUICK_SORT -> Pair(
                QuickSortGenerator(),
                InputPresetGenerator.generateInitialState(preset, size = arraySize, seed = seed)
            )
            AlgorithmId.LINKED_LIST_REVERSAL -> Pair(
                LinkedListReversalGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(12, 24, 36, 48, 60))
            )
            AlgorithmId.FLOYD_CYCLE -> Pair(
                FloydCycleGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(10, 20, 30, 40, 50, 60, 70), cycleEntryIndex = 3)
            )
            AlgorithmId.STACK_QUEUE_LIFO_FIFO -> Pair(
                StackQueueGenerator(),
                InputPresetGenerator.generateLinkedListState(listOf(5, 15, 25, 35))
            )
            AlgorithmId.BST_OPERATIONS -> Pair(
                BstOperationsGenerator(),
                VisualStructureState.HierarchyTreeState(nodes = persistentListOf())
            )
            AlgorithmId.AVL_ROTATIONS -> Pair(
                AvlRotationsGenerator(),
                VisualStructureState.HierarchyTreeState(nodes = persistentListOf())
            )
            AlgorithmId.GRAPH_BFS_DFS -> Pair(
                GraphBfsDfsGenerator(useDfs = false),
                GraphLayoutEngine.layoutGrid(rows = 3, cols = 4, obstacles = setOf(Pair(1, 1), Pair(1, 2)))
            )
            AlgorithmId.DIJKSTRA_ASTAR -> Pair(
                DijkstraAStarGenerator(useAStar = true),
                GraphLayoutEngine.layoutGrid(rows = 4, cols = 4, obstacles = setOf(Pair(1, 1), Pair(2, 2)))
            )
        }
    }
}
