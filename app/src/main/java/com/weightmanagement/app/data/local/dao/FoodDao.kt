package com.weightmanagement.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weightmanagement.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Query("SELECT * FROM food WHERE name LIKE '%' || :q || '%' LIMIT 20")
    suspend fun search(q: String): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("DELETE FROM food")
    suspend fun clear()
}
