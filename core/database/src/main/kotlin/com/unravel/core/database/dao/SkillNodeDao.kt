package com.unravel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unravel.core.database.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillNodeDao {

    @Query("SELECT * FROM skill_nodes ORDER BY clusterIndex ASC, id ASC")
    fun getAllSkillNodes(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_nodes WHERE id = :id LIMIT 1")
    suspend fun getSkillNodeById(id: String): SkillNodeEntity?

    @Query("UPDATE skill_nodes SET masteryState = :masteryState WHERE id = :id")
    suspend fun updateMasteryState(id: String, masteryState: String)

    @Query("UPDATE skill_nodes SET isUnlocked = :isUnlocked WHERE id = :id")
    suspend fun updateUnlockedState(id: String, isUnlocked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillNodes(nodes: List<SkillNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillNode(node: SkillNodeEntity)

    @Query("SELECT COUNT(*) FROM skill_nodes")
    suspend fun getSkillNodeCount(): Int
}
