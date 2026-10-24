package com.weightmanagement.app.domain.calculator

import kotlin.math.round

data class MacroTarget(
    val proteinG: Float,
    val carbG: Float,
    val fatG: Float
)

object GoalCalculator {

    /**
     * 每日热量目标 = TDEE ± 速度调整
     * 0.5kg/周 ≈ 550 kcal/天
     */
    fun calorieTarget(
        tdee: Float,
        goalType: String,
        ratePerWeekKg: Float = 0.5f
    ): Int {
        val deltaPerDay = ratePerWeekKg * 1100f / 7f
        val target = when (goalType) {
            "lose" -> tdee - deltaPerDay
            "gain" -> tdee + deltaPerDay
            else -> tdee
        }
        // 安全范围
        return target.toInt().coerceIn(1000, 5000)
    }

    /** 宏量营养素目标：蛋白 30% / 碳水 40% / 脂肪 30% */
    fun macroTargets(calorieTarget: Int, proteinPerKg: Float = 1.6f, weightKg: Float = 70f): MacroTarget {
        val proteinG = (proteinPerKg * weightKg).coerceAtMost((calorieTarget * 0.35f) / 4f)
        val proteinCal = proteinG * 4f
        val remaining = (calorieTarget - proteinCal).coerceAtLeast(0f)
        val fatCal = remaining * 0.33f
        val carbCal = remaining - fatCal
        return MacroTarget(
            proteinG = round(proteinG * 10f) / 10f,
            carbG = round(carbCal / 4f * 10f) / 10f,
            fatG = round(fatCal / 9f * 10f) / 10f
        )
    }
}
