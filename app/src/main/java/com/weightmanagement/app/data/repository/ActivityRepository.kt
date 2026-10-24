package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.ActivityDao
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.domain.calculator.MetLibrary
import com.weightmanagement.app.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao
) {
    fun observeAll(): Flow<List<ActivityEntity>> = activityDao.observeAll()
    fun observeTodayBurned(): Flow<Float> = activityDao.observeBurnedSince(DateUtils.dayStart())

    suspend fun add(
        type: String,
        durationMin: Int,
        intensity: String,
        weightKg: Float,
        startTime: Long = System.currentTimeMillis()
    ): Long {
        val met = MetLibrary.metFor(type)
        val burned = MetLibrary.caloriesBurned(met, weightKg, durationMin)
        return activityDao.insert(
            ActivityEntity(
                type = type,
                startTime = startTime,
                durationMin = durationMin,
                intensity = intensity,
                met = met,
                caloriesBurned = burned
            )
        )
    }

    suspend fun delete(activity: ActivityEntity) = activityDao.delete(activity)

    /** 近 N 天逐日运动消耗（按本地时区归天） */
    suspend fun dailyBurnedSinceDays(days: Int): Map<Long, Float> {
        val from = DateUtils.dayStart() - (days - 1) * 24L * 3600 * 1000
        return activityDao.getSinceList(from)
            .groupBy { DateUtils.dayStart(it.startTime) }
            .mapValues { (_, list) -> list.sumOf { it.caloriesBurned.toDouble() }.toFloat() }
    }
}
