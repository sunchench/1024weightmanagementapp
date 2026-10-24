package com.weightmanagement.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.weightmanagement.app.data.local.dao.AiAnalysisDao
import com.weightmanagement.app.data.local.dao.ActivityDao
import com.weightmanagement.app.data.local.dao.FoodDao
import com.weightmanagement.app.data.local.dao.GoalDao
import com.weightmanagement.app.data.local.dao.IntakeDao
import com.weightmanagement.app.data.local.dao.MealDao
import com.weightmanagement.app.data.local.dao.UserDao
import com.weightmanagement.app.data.local.dao.WaterDao
import com.weightmanagement.app.data.local.dao.WeightDao
import com.weightmanagement.app.data.local.entity.AiAnalysisEntity
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.data.local.entity.FoodEntity
import com.weightmanagement.app.data.local.entity.GoalEntity
import com.weightmanagement.app.data.local.entity.IntakeEntity
import com.weightmanagement.app.data.local.entity.MealEntity
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.local.entity.WaterEntity
import com.weightmanagement.app.data.local.entity.WeightLogEntity

@Database(
    entities = [
        UserEntity::class,
        WeightLogEntity::class,
        MealEntity::class,
        FoodEntity::class,
        IntakeEntity::class,
        ActivityEntity::class,
        WaterEntity::class,
        GoalEntity::class,
        AiAnalysisEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun weightDao(): WeightDao
    abstract fun mealDao(): MealDao
    abstract fun foodDao(): FoodDao
    abstract fun intakeDao(): IntakeDao
    abstract fun activityDao(): ActivityDao
    abstract fun waterDao(): WaterDao
    abstract fun goalDao(): GoalDao
    abstract fun aiAnalysisDao(): AiAnalysisDao
}
