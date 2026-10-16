package com.unravel.core.engine.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ArrayNode(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid = Uuid.random(),
    val value: Int
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class LinkedNode(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid = Uuid.random(),
    val value: Int,
    val nextId: @Serializable(with = UuidSerializer::class) Uuid? = null,
    val prevId: @Serializable(with = UuidSerializer::class) Uuid? = null
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class TreeNode(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid = Uuid.random(),
    val value: Int,
    val leftId: @Serializable(with = UuidSerializer::class) Uuid? = null,
    val rightId: @Serializable(with = UuidSerializer::class) Uuid? = null,
    val height: Int = 1,
    val balanceFactor: Int = 0,
    val normX: Float = 0.5f,
    val normY: Float = 0.5f
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class TreeEdge(
    @Serializable(with = UuidSerializer::class)
    val fromId: Uuid,
    @Serializable(with = UuidSerializer::class)
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
    @Serializable(with = UuidSerializer::class)
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
    @Serializable(with = UuidSerializer::class)
    val fromId: Uuid,
    @Serializable(with = UuidSerializer::class)
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
