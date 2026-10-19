package com.unravel.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an interactive skill map node.
 */
@Entity(tableName = "skill_nodes")
data class SkillNodeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val clusterIndex: Int,
    val prerequisiteIds: List<String>,
    val masteryState: String, // "UNEXPLORED", "TINKERED", "MASTERED"
    val isUnlocked: Boolean
)
