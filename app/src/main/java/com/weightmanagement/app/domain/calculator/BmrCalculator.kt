package com.weightmanagement.app.domain.calculator

import kotlin.math.round

object BmrCalculator {

    /**
     * Mifflin-St Jeor 公式
     * BMR = 10*weight + 6.25*height - 5*age + s
     * s = +5 (male) / -161 (female)
     */
    fun mifflinStJeor(weightKg: Float, heightCm: Float, age: Int, gender: String): Float {
        val s = if (gender == "male") 5f else -161f
        return round(10f * weightKg + 6.25f * heightCm - 5f * age + s)
    }
}
