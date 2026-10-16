package com.unravel.core.engine.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ArrayNode(
    val id: Uuid = Uuid.random(),
    val value: Int
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class LinkedNode(
    val id: Uuid = Uuid.random(),
    val value: Int,
    val nextId: Uuid? = null,
    val prevId: Uuid? = null
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class TreeNode(
    val id: Uuid = Uuid.random(),
    val value: Int,
    val leftId: Uuid? = null,
    val rightId: Uuid? = null,
    val height: Int = 1,
    val balanceFactor: Int = 0,
    val normX: Float = 0.5f,
    val normY: Float = 0.5f
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class TreeEdge(
    val fromId: Uuid,
    val toId: Uuid,
    val isHighlighted: Boolean = false
)

@Serializable
enum class SpatialMode {
    GRID_2D,
    FREE_GRAPH
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class SpatialVertex(
    val id: Uuid = Uuid.random(),
    val label: String = "",
    val gridRow: Int = 0,
    val gridCol: Int = 0,
    val normX: Float = 0f,
    val normY: Float = 0f,
    val weight: Int = 1,
    val isObstacle: Boolean = false
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class SpatialEdge(
    val fromId: Uuid,
    val toId: Uuid,
    val weight: Int = 1,
    val isDirected: Boolean = false,
    val isHighlighted: Boolean = false
)

@Serializable
enum class RotationDirection {
    LEFT,
    RIGHT
}
