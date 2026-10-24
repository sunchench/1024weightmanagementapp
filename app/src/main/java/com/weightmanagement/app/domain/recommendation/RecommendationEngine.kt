package com.weightmanagement.app.domain.recommendation

import com.weightmanagement.app.data.local.dao.DailyCalorie
import com.weightmanagement.app.domain.util.DateUtils

/**
 * 最近一周数据（含今天，共 7 天）。
 * 无记录的日期补 0，保证样本数完整。
 */
data class WeekStats(
    val days: Int = 7,
    val recordedDays: Int = 0,          // 有饮食记录的天数
    val avgCalories: Float = 0f,        // 7 天日均摄入（无记录日算 0）
    val avgCaloriesActive: Float = 0f,  // 仅有记录日的日均摄入
    val avgExercise: Float = 0f,        // 7 天日均运动消耗
    val exerciseDays: Int = 0,          // 有运动记录的天数
    val avgWater: Float = 0f,           // 7 天日均饮水 ml
    val waterDays: Int = 0,             // 有饮水记录的天数
    val weightChangeKg: Float? = null   // 近 7 天体重变化（正=增重）
)

data class Recommendations(
    val stats: WeekStats,
    val diet: List<String>,
    val exercise: List<String>,
    val water: List<String>
)

object RecommendationEngine {

    fun build(
        calorieTarget: Int,
        waterTargetMl: Int,
        dailyCalories: List<DailyCalorie>,
        exerciseByDay: Map<Long, Float>,   // key=dayStart
        waterByDay: Map<Long, Int>,        // key=dayStart
        weightFirstKg: Float?,
        weightLastKg: Float?
    ): Recommendations {
        val today = DateUtils.dayStart()
        val days = (0 until 7).map { today - it * 24L * 3600 * 1000 }
        val calByDay = dailyCalories.associate { it.day to it.calories }

        val caloriesPerDay = days.map { calByDay[it] ?: 0f }
        val exercisePerDay = days.map { exerciseByDay[it] ?: 0f }
        val waterPerDay = days.map { waterByDay[it] ?: 0 }

        val recordedDays = caloriesPerDay.count { it > 0f }
        val exerciseDays = exercisePerDay.count { it > 0f }
        val waterDays = waterPerDay.count { it > 0 }

        val stats = WeekStats(
            days = 7,
            recordedDays = recordedDays,
            avgCalories = caloriesPerDay.average().toFloat(),
            avgCaloriesActive = if (recordedDays > 0) caloriesPerDay.filter { it > 0f }.average().toFloat() else 0f,
            avgExercise = exercisePerDay.average().toFloat(),
            exerciseDays = exerciseDays,
            avgWater = waterPerDay.map { it.toFloat() }.average().toFloat(),
            waterDays = waterDays,
            weightChangeKg = if (weightFirstKg != null && weightLastKg != null) weightLastKg - weightFirstKg else null
        )

        return Recommendations(
            stats = stats,
            diet = dietAdvice(stats, calorieTarget),
            exercise = exerciseAdvice(stats),
            water = waterAdvice(stats, waterTargetMl)
        )
    }

    private fun dietAdvice(s: WeekStats, target: Int): List<String> {
        val list = mutableListOf<String>()
        if (s.recordedDays == 0) {
            list.add("近 7 天还没有饮食记录，先记录几天饮食，才能给出更准确的建议。")
            return list
        }
        val t = target.toFloat()
        val avg = s.avgCaloriesActive
        when {
            avg > t * 1.15f -> {
                val over = (avg - t).toInt()
                list.add("近一周平均每日摄入 ${avg.toInt()} kcal，超出目标约 $over kcal，建议减少高油高糖食物、控制主食分量。")
            }
            avg < t * 0.85f -> {
                list.add("近一周平均每日摄入 ${avg.toInt()} kcal，明显低于目标 ${target} kcal，长期热量不足易反弹，建议适当增加优质蛋白与主食。")
            }
            else -> list.add("近一周平均每日摄入 ${avg.toInt()} kcal，与目标 ${target} kcal 匹配良好，请继续保持。")
        }
        if (s.recordedDays < 7) {
            list.add("本周有 ${7 - s.recordedDays} 天未记录饮食，记录越完整，建议越准确。")
        }
        val change = s.weightChangeKg
        if (change != null) {
            if (change > 0.8f) {
                list.add("近一周体重上升 ${"%.1f".format(change)} kg，如目标是减重，需关注摄入与运动平衡。")
            } else if (change < -1.2f) {
                list.add("近一周体重下降 ${"%.1f".format(-change)} kg，速度偏快，建议放缓减重节奏保证营养。")
            }
        }
        return list
    }

    private fun exerciseAdvice(s: WeekStats): List<String> {
        val list = mutableListOf<String>()
        when {
            s.exerciseDays == 0 -> list.add("近 7 天没有运动记录，建议从每天快走 30 分钟开始，循序渐进。")
            s.exerciseDays in 1..2 -> list.add("本周运动 ${s.exerciseDays} 天，建议增加到每周 3~5 次，每次 30 分钟以上中等强度运动。")
            else -> list.add("本周运动 ${s.exerciseDays} 天、日均消耗 ${s.avgExercise.toInt()} kcal，运动习惯保持得不错。")
        }
        return list
    }

    private fun waterAdvice(s: WeekStats, targetMl: Int): List<String> {
        val list = mutableListOf<String>()
        when {
            s.waterDays == 0 -> list.add("近 7 天没有饮水记录，建议每天饮水 ${targetMl}ml 左右，可少量多次。")
            s.avgWater < targetMl * 0.8f -> {
                val gap = (targetMl - s.avgWater).toInt()
                list.add("日均饮水 ${s.avgWater.toInt()}ml，距离目标还有约 ${gap}ml，建议晨起与每餐前各补充一杯水。")
            }
            else -> list.add("日均饮水 ${s.avgWater.toInt()}ml，已达标，请继续保持。")
        }
        return list
    }
}
