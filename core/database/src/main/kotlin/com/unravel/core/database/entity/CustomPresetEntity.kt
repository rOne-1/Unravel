package com.unravel.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing user-created or customized sandbox presets.
 */
@Entity(tableName = "custom_presets")
data class CustomPresetEntity(
    @PrimaryKey
    val id: String,
    val algorithmId: String,
    val name: String,
    val datasetPayload: String, // Serialized JSON payload
    val createdAtEpochMs: Long
)
