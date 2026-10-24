package com.weightmanagement.app.domain.calculator

import kotlin.math.round

object TdeeCalculator {

    /** 活动系数 */
    fun factor(activityLevel: String): Float = when (activityLevel) {
        "sedentary" -> 1.2f
        "light" -> 1.375f
        "moderate" -> 1.55f
        "high" -> 1.725f
        "very_high" -> 1.9f
        else -> 1.55f
    }

    fun tdee(bmr: Float, activityLevel: String): Float {
        return round(bmr * factor(activityLevel))
    }
}
