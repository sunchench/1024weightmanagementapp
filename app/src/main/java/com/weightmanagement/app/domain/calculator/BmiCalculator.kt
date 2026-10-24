package com.weightmanagement.app.domain.calculator

import kotlin.math.round

object BmiCalculator {

    /** BMI = weight(kg) / height(m)^2 */
    fun bmi(weightKg: Float, heightCm: Float): Float {
        val h = heightCm / 100f
        if (h <= 0f) return 0f
        return (weightKg / (h * h))
    }

    /** WHO 标准 */
    fun whoCategory(bmi: Float): String = when {
        bmi < 18.5f -> "偏瘦"
        bmi < 25f -> "正常"
        bmi < 30f -> "超重"
        else -> "肥胖"
    }

    /** 中国 WGOC 标准 */
    fun wgocCategory(bmi: Float): String = when {
        bmi < 18.5f -> "偏瘦"
        bmi < 24f -> "正常"
        bmi < 28f -> "超重"
        else -> "肥胖"
    }

    fun round1(value: Float): Float = round(value * 10f) / 10f
}
