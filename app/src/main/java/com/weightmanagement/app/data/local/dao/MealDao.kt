package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.weightmanagement.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal WHERE date = :day ORDER BY id ASC")
    fun observeByDay(day: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE id = :id")
    suspend fun getById(id: Long): MealEntity?

    @Query("SELECT DISTINCT date FROM meal ORDER BY date DESC")
    fun observeAllDays(): Flow<List<Long>>

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("DELETE FROM meal")
    suspend fun clear()
}
