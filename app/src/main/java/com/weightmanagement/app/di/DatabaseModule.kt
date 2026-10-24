package com.weightmanagement.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weightmanagement.app.data.local.AppDatabase
import com.weightmanagement.app.data.local.dao.AiAnalysisDao
import com.weightmanagement.app.data.local.dao.ActivityDao
import com.weightmanagement.app.data.local.dao.FoodDao
import com.weightmanagement.app.data.local.dao.GoalDao
import com.weightmanagement.app.data.local.dao.IntakeDao
import com.weightmanagement.app.data.local.dao.MealDao
import com.weightmanagement.app.data.local.dao.UserDao
import com.weightmanagement.app.data.local.dao.WaterDao
import com.weightmanagement.app.data.local.dao.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 个人信息管理：头像 / 姓名 / 电话
        db.execSQL("ALTER TABLE user_profile ADD COLUMN avatarUri TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN name TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        // 饮食记录增加时间戳，并回填历史记录到所属餐的日期
        db.execSQL("ALTER TABLE intake ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE intake SET timestamp = (SELECT m.date FROM meal m WHERE m.id = intake.mealId) WHERE timestamp = 0")
    }
}

// 版本 2 → 3：为饮食记录增加食物图片字段（仅新增列，不破坏任何历史数据）
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE intake ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "weight_management.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides fun provideUserDao(db: AppDatabase) = db.userDao()
    @Provides fun provideWeightDao(db: AppDatabase) = db.weightDao()
    @Provides fun provideMealDao(db: AppDatabase) = db.mealDao()
    @Provides fun provideFoodDao(db: AppDatabase) = db.foodDao()
    @Provides fun provideIntakeDao(db: AppDatabase) = db.intakeDao()
    @Provides fun provideActivityDao(db: AppDatabase) = db.activityDao()
    @Provides fun provideWaterDao(db: AppDatabase) = db.waterDao()
    @Provides fun provideGoalDao(db: AppDatabase) = db.goalDao()
    @Provides fun provideAiAnalysisDao(db: AppDatabase) = db.aiAnalysisDao()
}
