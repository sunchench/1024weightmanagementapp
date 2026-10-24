package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.weightmanagement.app.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_log ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_log WHERE startTime >= :from")
    fun getSince(from: Long): Flow<List<ActivityEntity>>

    /** 指定时间窗（本地时区某天）的运动记录 */
    @Query("SELECT * FROM activity_log WHERE startTime >= :from AND startTime < :to ORDER BY startTime DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<ActivityEntity>>

    @Query("SELECT COALESCE(SUM(caloriesBurned),0) FROM activity_log WHERE startTime >= :from")
    fun observeBurnedSince(from: Long): Flow<Float>

    /** 近 N 天全部运动记录（归天聚合在 Repository 层按本地时区完成） */
    @Query("SELECT * FROM activity_log WHERE startTime >= :from")
    suspend fun getSinceList(from: Long): List<ActivityEntity>

    @Insert
    suspend fun insert(a: ActivityEntity): Long

    @Update
    suspend fun update(a: ActivityEntity)

    @Delete
    suspend fun delete(a: ActivityEntity)

    @Query("DELETE FROM activity_log")
    suspend fun clear()
}
