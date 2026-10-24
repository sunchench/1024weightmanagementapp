package com.weightmanagement.app.data.repository

import com.weightmanagement.app.data.local.dao.WeightDao
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import com.weightmanagement.app.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val weightDao: WeightDao
) {
    fun observeAll(): Flow<List<WeightLogEntity>> = weightDao.observeAll()
    fun observeSince(days: Int): Flow<List<WeightLogEntity>> =
        weightDao.getSince(DateUtils.daysAgoStart(days))

    suspend fun latest(): WeightLogEntity? = weightDao.latest()

    /** 近 N 天窗口内最早与最晚一次体重（估算周变化），样本不足返回 null */
    suspend fun weekChangeKg(days: Int = 7): Pair<Float, Float>? {
        val from = DateUtils.daysAgoStart(days)
        val list = weightDao.getSince(from).first().asReversed()
        if (list.size < 2) return null
        return list.first().weightKg to list.last().weightKg
    }

    suspend fun add(weightKg: Float, note: String, timestamp: Long = System.currentTimeMillis()): Long {
        return weightDao.insert(WeightLogEntity(weightKg = weightKg, note = note, timestamp = timestamp))
    }

    suspend fun update(log: WeightLogEntity) = weightDao.update(log)
    suspend fun delete(log: WeightLogEntity) = weightDao.delete(log)
}
