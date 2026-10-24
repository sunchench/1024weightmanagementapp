package com.weightmanagement.app.domain.model

data class DailyNutrition(
    val intakeCalories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f,
    val exerciseCalories: Float = 0f,
    val waterMl: Int = 0
) {
    val netCalories: Float
        get() = intakeCalories - exerciseCalories
}
