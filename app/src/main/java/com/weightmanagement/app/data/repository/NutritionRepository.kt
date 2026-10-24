package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.FoodDao
import com.weightmanagement.app.data.local.dao.IntakeDao
import com.weightmanagement.app.data.local.dao.MealDao
import com.weightmanagement.app.data.local.dao.NutritionSum
import com.weightmanagement.app.data.local.entity.FoodEntity
import com.weightmanagement.app.data.local.entity.FoodSeed
import com.weightmanagement.app.data.local.entity.IntakeEntity
import com.weightmanagement.app.data.local.entity.MealEntity
import com.weightmanagement.app.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val mealDao: MealDao,
    private val intakeDao: IntakeDao,
    private val foodDao: FoodDao
) {
    fun observeMeals(day: Long): Flow<List<MealEntity>> = mealDao.observeByDay(day)
    fun observeIntakes(mealId: Long): Flow<List<IntakeEntity>> = intakeDao.observeByMeal(mealId)
    fun observeDaily(day: Long): Flow<NutritionSum> = intakeDao.observeSumByDay(day)
    fun observeIntakesByDay(day: Long): Flow<List<com.weightmanagement.app.data.local.dao.IntakeWithMealType>> = intakeDao.observeByDay(day)
    fun observeAllDays(): Flow<List<Long>> = mealDao.observeAllDays()

    private suspend fun getOrCreateMeal(day: Long, mealType: String): Long {
        val meals = mealDao.observeByDay(day).first()
        val existing = meals.find { it.mealType == mealType }
        return existing?.id ?: mealDao.insert(MealEntity(date = day, mealType = mealType))
    }

    suspend fun addIntake(
        day: Long,
        mealType: String,
        foodName: String,
        grams: Float,
        calories: Float,
        protein: Float,
        fat: Float,
        carbs: Float,
        source: String = "manual",
        imageUri: String = ""
    ): Long {
        val mealId = getOrCreateMeal(day, mealType)
        return intakeDao.insert(
            IntakeEntity(
                mealId = mealId,
                foodName = foodName,
                grams = grams,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                source = source,
                imageUri = imageUri
            )
        )
    }

    suspend fun addIntakeToMeal(mealId: Long, intake: IntakeEntity) = intakeDao.insert(intake)
    suspend fun updateIntake(intake: IntakeEntity) = intakeDao.update(intake)
    suspend fun deleteIntake(intake: IntakeEntity) = intakeDao.delete(intake)

    fun observeFoods(): Flow<List<FoodEntity>> = foodDao.observeAll()
    suspend fun searchFood(q: String): List<FoodEntity> = foodDao.search(q)
    suspend fun addFood(food: FoodEntity) = foodDao.insert(food)
    suspend fun deleteFood(food: FoodEntity) = foodDao.delete(food)

    /** 近 N 天（含今天）逐日总热量 */
    suspend fun dailyCaloriesSinceDays(days: Int): List<com.weightmanagement.app.data.local.dao.DailyCalorie> =
        intakeDao.dailyCaloriesSince(DateUtils.dayStart() - (days - 1) * 24L * 3600 * 1000)

    /** 首次启动时把内置食物营养库写入数据库（幂等） */
    suspend fun seedIfEmpty() {
        if (foodDao.count() == 0) {
            FoodSeed.items.forEach { foodDao.insert(it) }
        }
    }

    /** 默认今天 */
    fun today(): Long = DateUtils.dayStart()
}
