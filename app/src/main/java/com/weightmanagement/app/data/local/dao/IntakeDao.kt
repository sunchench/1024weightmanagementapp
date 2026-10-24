package com.weightmanagement.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.weightmanagement.app.data.local.entity.IntakeEntity
import kotlinx.coroutines.flow.Flow

data class NutritionSum(
    @ColumnInfo(name = "c") val calories: Float,
    @ColumnInfo(name = "p") val protein: Float,
    @ColumnInfo(name = "f") val fat: Float,
    @ColumnInfo(name = "cb") val carbs: Float
)

data class DailyCalorie(
    @ColumnInfo(name = "day") val day: Long,
    @ColumnInfo(name = "c") val calories: Float
)

/** 饮食记录 + 所属餐别（JOIN meal 表带出） */
data class IntakeWithMealType(
    @Embedded val intake: IntakeEntity,
    @ColumnInfo(name = "mealType") val mealType: String
)

@Dao
interface IntakeDao {
    @Query("SELECT * FROM intake WHERE mealId = :mealId ORDER BY id ASC")
    fun observeByMeal(mealId: Long): Flow<List<IntakeEntity>>

    @Insert
    suspend fun insert(intake: IntakeEntity): Long

    @Insert
    suspend fun insertAll(list: List<IntakeEntity>)

    @Update
    suspend fun update(intake: IntakeEntity)

    @Delete
    suspend fun delete(intake: IntakeEntity)

    @Query("DELETE FROM intake WHERE mealId = :mealId")
    suspend fun deleteByMeal(mealId: Long)

    @Query("DELETE FROM intake")
    suspend fun clear()

    @Query(
        "SELECT COALESCE(SUM(i.calories),0) AS c, COALESCE(SUM(i.protein),0) AS p, " +
            "COALESCE(SUM(i.fat),0) AS f, COALESCE(SUM(i.carbs),0) AS cb " +
            "FROM intake i INNER JOIN meal m ON i.mealId = m.id WHERE m.date = :day"
    )
    fun observeSumByDay(day: Long): Flow<NutritionSum>

    /** 按日期观测全部饮食记录（含餐别），单一 SQL，切日期即整体重发射 */
    @Query(
        "SELECT i.*, m.mealType AS mealType FROM intake i " +
            "INNER JOIN meal m ON i.mealId = m.id " +
            "WHERE m.date = :day ORDER BY i.timestamp DESC, i.id DESC"
    )
    fun observeByDay(day: Long): Flow<List<IntakeWithMealType>>

    /** 近 N 天（含今天）逐日总热量，无记录的日期不返回行 */
    @Query(
        "SELECT m.date AS day, COALESCE(SUM(i.calories),0) AS c " +
            "FROM intake i INNER JOIN meal m ON i.mealId = m.id " +
            "WHERE m.date >= :fromDay GROUP BY m.date ORDER BY m.date ASC"
    )
    suspend fun dailyCaloriesSince(fromDay: Long): List<DailyCalorie>
}
