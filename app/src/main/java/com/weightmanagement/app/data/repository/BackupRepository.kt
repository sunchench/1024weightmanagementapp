package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.ActivityDao
import com.weightmanagement.app.data.local.dao.AiAnalysisDao
import com.weightmanagement.app.data.local.dao.FoodDao
import com.weightmanagement.app.data.local.dao.GoalDao
import com.weightmanagement.app.data.local.dao.IntakeDao
import com.weightmanagement.app.data.local.dao.MealDao
import com.weightmanagement.app.data.local.dao.UserDao
import com.weightmanagement.app.data.local.dao.WaterDao
import com.weightmanagement.app.data.local.dao.WeightDao
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.data.local.entity.AiAnalysisEntity
import com.weightmanagement.app.data.local.entity.FoodEntity
import com.weightmanagement.app.data.local.entity.GoalEntity
import com.weightmanagement.app.data.local.entity.IntakeEntity
import com.weightmanagement.app.data.local.entity.MealEntity
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.local.entity.WaterEntity
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String = "",
    val user: UserEntity? = null,
    val goal: GoalEntity? = null,
    val weightLogs: List<WeightLogEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val intakes: List<IntakeEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val water: List<WaterEntity> = emptyList()
)

@Singleton
class BackupRepository @Inject constructor(
    private val userDao: UserDao,
    private val weightDao: WeightDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val intakeDao: IntakeDao,
    private val activityDao: ActivityDao,
    private val waterDao: WaterDao,
    private val goalDao: GoalDao,
    private val aiAnalysisDao: AiAnalysisDao
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun export(): String {
        val data = BackupData(
            exportedAt = Instant.now().toString(),
            user = userDao.get(),
            goal = goalDao.observe().first(),
            weightLogs = weightDao.observeAll().first(),
            meals = mealDao.observeByDay(Long.MIN_VALUE).first(),
            foods = foodDao.observeAll().first(),
            intakes = intakeDao.observeByMeal(Long.MIN_VALUE).first(),
            activities = activityDao.observeAll().first(),
            water = waterDao.observeToday(Long.MIN_VALUE).first()
        )
        return json.encodeToString(data)
    }

    /** 清空全部数据（用于"清空数据"与恢复前准备） */
    suspend fun clearAll() {
        userDao.clear()
        goalDao.clear()
        weightDao.clear()
        mealDao.clear()
        foodDao.clear()
        intakeDao.clear()
        activityDao.clear()
        waterDao.clear()
    }

    suspend fun import(jsonString: String) {
        val data = json.decodeFromString<BackupData>(jsonString)
        clearAll()
        data.user?.let { userDao.upsert(it) }
        data.goal?.let { goalDao.upsert(it) }
        data.weightLogs.forEach { weightDao.insert(it) }
        data.meals.forEach { mealDao.insert(it) }
        data.foods.forEach { foodDao.insert(it) }
        data.intakes.forEach { intakeDao.insert(it) }
        data.activities.forEach { activityDao.insert(it) }
        data.water.forEach { waterDao.insert(it) }
    }
}
