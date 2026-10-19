package com.unravel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unravel.core.database.entity.CustomPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPresetDao {

    @Query("SELECT * FROM custom_presets WHERE algorithmId = :algorithmId ORDER BY createdAtEpochMs DESC")
    fun getCustomPresetsByAlgorithm(algorithmId: String): Flow<List<CustomPresetEntity>>

    @Query("SELECT * FROM custom_presets WHERE id = :id LIMIT 1")
    suspend fun getCustomPresetById(id: String): CustomPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPreset(preset: CustomPresetEntity)

    @Query("DELETE FROM custom_presets WHERE id = :id")
    suspend fun deleteCustomPresetById(id: String)

    @Query("SELECT COUNT(*) FROM custom_presets WHERE algorithmId = :algorithmId")
    suspend fun getPresetCountByAlgorithm(algorithmId: String): Int
}
