package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.weightmanagement.app.data.local.entity.WaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT COALESCE(SUM(ml),0) FROM water_intake WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    fun observeTodayTotal(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT * FROM water_intake WHERE timestamp >= :dayStart ORDER BY timestamp DESC")
    fun observeToday(dayStart: Long): Flow<List<WaterEntity>>

    @Query("SELECT COALESCE(SUM(ml),0) FROM water_intake WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    fun observeTotalBetween(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT * FROM water_intake WHERE timestamp >= :dayStart ORDER BY timestamp DESC")
    fun observeFromDay(dayStart: Long): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water_intake ORDER BY timestamp DESC")
    fun observeAllRecords(): Flow<List<WaterEntity>>

    /** 近 N 天全部饮水记录（归天聚合在 Repository 层按本地时区完成） */
    @Query("SELECT * FROM water_intake WHERE timestamp >= :from")
    suspend fun getSinceList(from: Long): List<WaterEntity>

    @Insert
    suspend fun insert(w: WaterEntity): Long

    @Delete
    suspend fun delete(w: WaterEntity)

    @Query("DELETE FROM water_intake")
    suspend fun clear()
}
