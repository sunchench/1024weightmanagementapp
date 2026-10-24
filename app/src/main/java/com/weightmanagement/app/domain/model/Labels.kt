package com.weightmanagement.app.domain.model

object Labels {
    val GENDER = mapOf("male" to "男", "female" to "女")
    val GOAL_TYPE = mapOf("lose" to "减重", "maintain" to "维持", "gain" to "增重")
    val ACTIVITY_LEVEL = mapOf(
        "sedentary" to "久坐",
        "light" to "轻度活动",
        "moderate" to "中度活动",
        "high" to "高强度活动",
        "very_high" to "非常高"
    )
    val MEAL_TYPE = mapOf(
        "breakfast" to "早餐",
        "lunch" to "午餐",
        "dinner" to "晚餐",
        "snack" to "零食"
    )
    val INTENSITY = mapOf("low" to "低", "moderate" to "中", "high" to "高")
}
