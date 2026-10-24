package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.WaterDao
import com.weightmanagement.app.data.local.entity.WaterEntity
import com.weightmanagement.app.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val waterDao: WaterDao
) {
    fun observeTotal(day: Long): Flow<Int> =
        waterDao.observeTotalBetween(day, DateUtils.dayEnd(day))

    fun observeRecords(day: Long): Flow<List<WaterEntity>> =
        waterDao.observeFromDay(day)

    /** 所有有饮水记录的日期（去重、倒序），用于历史查询 */
    fun observeAllDays(): Flow<List<Long>> =
        waterDao.observeAllRecords().map { list ->
            list.map { DateUtils.dayStart(it.timestamp) }.distinct().sortedDescending()
        }

    suspend fun add(ml: Int, timestamp: Long = System.currentTimeMillis()) =
        waterDao.insert(WaterEntity(ml = ml, timestamp = timestamp))

    suspend fun delete(water: WaterEntity) = waterDao.delete(water)

    /** 近 N 天逐日饮水总量（按本地时区归天） */
    suspend fun dailyTotalSinceDays(days: Int): Map<Long, Int> {
        val from = DateUtils.dayStart() - (days - 1) * 24L * 3600 * 1000
        return waterDao.getSinceList(from)
            .groupBy { DateUtils.dayStart(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.ml } }
    }
}
