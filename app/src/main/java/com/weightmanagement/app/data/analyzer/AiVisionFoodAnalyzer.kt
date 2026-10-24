package com.weightmanagement.app.data.analyzer

import com.weightmanagement.app.data.remote.AiVisionClient
import com.weightmanagement.app.data.repository.AiConfigRepository
import com.weightmanagement.app.domain.model.RecognizedFood
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed class AnalyzeOutcome {
    /** 未配置 AI */
    data object NotConfigured : AnalyzeOutcome()
    data class Success(val result: RecognizedFood) : AnalyzeOutcome()
    data class Failure(val message: String) : AnalyzeOutcome()
}

/**
 * 基于已配置视觉模型的图片识别器（移植 NutriTracker AiFoodAnalyzer 的调用链路）。
 * - 未配置 AI → [AnalyzeOutcome.NotConfigured]，调用方引导去设置页
 * - 已配置但识别失败 → [AnalyzeOutcome.Failure]（带可读原因），不再静默吞掉
 */
class AiVisionFoodAnalyzer @Inject constructor(
    private val configRepository: AiConfigRepository,
    private val client: AiVisionClient
) {
    suspend fun analyze(imageBase64List: List<String>, mime: String = "image/jpeg"): AnalyzeOutcome {
        val c = configRepository.config.first()
        if (!c.configured) return AnalyzeOutcome.NotConfigured
        return when (val r = client.analyze(c.baseUrl, c.apiKey, c.model, imageBase64List, mime)) {
            is AiVisionClient.Outcome.Success -> AnalyzeOutcome.Success(r.result)
            is AiVisionClient.Outcome.Failure -> AnalyzeOutcome.Failure(r.message)
        }
    }

    /** 连通性检测（用于设置页「检测」按钮）。返回 null=成功，否则为失败原因 */
    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): String? {
        return client.testConnection(baseUrl, apiKey, model)
    }

    /**
     * 纯文本营养估算（按食物名 + 克数带出热量/蛋白/脂肪/碳水）。
     * 未配置 AI → NotConfigured；本地食物库有匹配 → LocalMatch（免网络，优先）。
     */
    suspend fun estimateNutrition(
        foodName: String,
        grams: Float,
        localSearch: suspend (String) -> List<com.weightmanagement.app.data.local.entity.FoodEntity>
    ): AnalyzeOutcome {
        if (foodName.isBlank() || grams <= 0f) return AnalyzeOutcome.Failure("请先填写食物名称和重量")
        // 1) 本地食物库精确匹配优先（免网络、即时）
        val local = localSearch(foodName).firstOrNull { it.name == foodName.trim() }
        if (local != null) {
            val ratio = grams / local.servingSizeG.coerceAtLeast(0.01f)
            return AnalyzeOutcome.Success(
                com.weightmanagement.app.domain.model.RecognizedFood(
                    mealName = local.name, name = local.name, grams = grams,
                    calories = local.calories * ratio,
                    protein = local.protein * ratio,
                    fat = local.fat * ratio,
                    carbs = local.carbs * ratio
                )
            )
        }
        // 2) 未配置 AI → 提示
        val c = configRepository.config.first()
        if (!c.configured) return AnalyzeOutcome.NotConfigured
        // 3) 大模型估算
        return when (val r = client.estimateNutrition(c.baseUrl, c.apiKey, c.model, foodName.trim(), grams)) {
            is com.weightmanagement.app.data.remote.AiVisionClient.Outcome.Success -> AnalyzeOutcome.Success(r.result)
            is com.weightmanagement.app.data.remote.AiVisionClient.Outcome.Failure -> AnalyzeOutcome.Failure(r.message)
        }
    }
}
