package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.ActivityDao
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.domain.calculator.MetLibrary
import com.weightmanagement.app.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao
) {
    fun observeAll(): Flow<List<ActivityEntity>> = activityDao.observeAll()
    fun observeTodayBurned(): Flow<Float> = activityDao.observeBurnedSince(DateUtils.dayStart())

    /** 指定日（本地时区当天 00:00 ~ 次日 00:00）的运动记录 */
    fun observeByDay(day: Long): Flow<List<ActivityEntity>> =
        activityDao.observeBetween(day, DateUtils.dayEnd(day))

    /** 所有有运动记录的日期（去重、倒序），用于历史日期选择 */
    fun observeAllDays(): Flow<List<Long>> = activityDao.observeAll().map { list ->
        list.map { DateUtils.dayStart(it.startTime) }.distinct().sortedDescending()
    }

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
