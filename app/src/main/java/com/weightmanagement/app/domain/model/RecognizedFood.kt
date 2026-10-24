package com.weightmanagement.app.domain.model

/**
 * AI 视觉识别返回的一餐营养估算结果（移植自 NutriTracker NutritionResult）。
 * @param mealName 概括性餐名，如"番茄炒蛋配米饭"
 * @param grams 全部食物项重量合计（g）
 * @param calories 总热量（kcal）
 * @param foodItems 拆解后的各食物/原料明细
 */
data class RecognizedFood(
    val mealName: String = "",
    val name: String = "",
    val grams: Float = 0f,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f,
    val foodItems: List<RecognizedFoodItem> = emptyList()
) {
    /** 显示名：优先概括餐名，兜底明细拼接 */
    val displayName: String
        get() = mealName.ifBlank {
            if (foodItems.isNotEmpty()) foodItems.take(3).joinToString(" + ") { it.name }
            else name
        }
}

/** AI 拆解的单个食物/原料项 */
data class RecognizedFoodItem(
    val name: String = "",
    val weightG: Float = 0f,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f
)

/** 纯文本营养估算结果（按食物名+克数） */
data class EstimatedNutrition(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f
)
