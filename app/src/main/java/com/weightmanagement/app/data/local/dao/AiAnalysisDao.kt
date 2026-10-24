package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.weightmanagement.app.data.local.entity.AiAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAnalysisDao {
    @Insert
    suspend fun insert(a: AiAnalysisEntity): Long

    @Query("SELECT * FROM ai_analysis ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AiAnalysisEntity>>
}
