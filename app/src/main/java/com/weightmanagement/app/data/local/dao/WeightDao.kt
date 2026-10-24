package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): WeightLogEntity?

    @Query("SELECT * FROM weight_log WHERE timestamp >= :from ORDER BY timestamp ASC")
    fun getSince(from: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log WHERE id = :id")
    suspend fun getById(id: Long): WeightLogEntity?

    @Insert
    suspend fun insert(log: WeightLogEntity): Long

    @Update
    suspend fun update(log: WeightLogEntity)

    @Delete
    suspend fun delete(log: WeightLogEntity)

    @Query("DELETE FROM weight_log")
    suspend fun clear()
}
