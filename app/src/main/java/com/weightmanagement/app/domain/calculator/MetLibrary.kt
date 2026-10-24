package com.weightmanagement.app.domain.calculator

import kotlin.math.round

object MetLibrary {

    /** 运动类型 -> MET 值 (静坐代谢当量) */
    val METS: Map<String, Float> = mapOf(
        "walking" to 3.5f,
        "running" to 9.8f,
        "cycling" to 7.5f,
        "swimming" to 6.0f,
        "strength" to 5.0f,
        "yoga" to 2.5f,
        "basketball" to 8.0f,
        "football" to 7.0f,
        "other" to 4.0f
    )

    val LABELS: Map<String, String> = mapOf(
        "walking" to "步行",
        "running" to "跑步",
        "cycling" to "骑行",
        "swimming" to "游泳",
        "strength" to "力量训练",
        "yoga" to "瑜伽",
        "basketball" to "篮球",
        "football" to "足球",
        "other" to "其他"
    )

    fun metFor(type: String): Float = METS[type] ?: 4.0f

    /** 消耗 = MET × 体重(kg) × 时长(h) */
    fun caloriesBurned(met: Float, weightKg: Float, durationMin: Int): Float {
        return round(met * weightKg * (durationMin / 60f) * 10f) / 10f
    }
}
