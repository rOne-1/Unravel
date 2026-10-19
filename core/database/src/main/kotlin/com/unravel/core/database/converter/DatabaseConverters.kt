package com.unravel.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room TypeConverter serializing string lists to JSON and vice-versa.
 */
class DatabaseConverters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }
}
