package com.weightmanagement.app.data.remote

import com.weightmanagement.app.domain.model.RecognizedFood
import com.weightmanagement.app.domain.model.RecognizedFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * OpenAI 兼容视觉接口的食物识别客户端。
 * 移植自开源项目 NutriTracker（github.com/cdz-hy/NutriTracker，GPL-3.0）
 * 的 AiFoodAnalyzer：中文营养师 prompt、URL 拼接规则、响应解析与 JSON 提取逻辑。
 */
class AiVisionClient @Inject constructor() {

    sealed class Outcome {
        data class Success(val result: RecognizedFood) : Outcome()
        data class Failure(val message: String) : Outcome()
    }

    /**
     * 端点拼接（移植 NutriTracker 规则并兼容两种习惯）：
     * - baseUrl 已以 /chat/completions 结尾 → 原样使用
     * - baseUrl 以版本段结尾（/v1、/v3、/api/v3 等）→ 只拼 chat/completions
     * - 其他 → 拼 v1/chat/completions
     */
    fun buildEndpoint(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        if (base.endsWith("/chat/completions")) return base
        return if (Regex("""/v\d+$""").containsMatchIn(base)) "$base/chat/completions"
        else "$base/v1/chat/completions"
    }

    suspend fun analyze(
        baseUrl: String,
        apiKey: String,
        model: String,
        imageBase64List: List<String>,
        mime: String = "image/jpeg"
    ): Outcome = withContext(Dispatchers.IO) {
        try {
            if (imageBase64List.isEmpty()) return@withContext Outcome.Failure("请至少提供一张图片！")
            val conn = URL(buildEndpoint(baseUrl)).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 180_000

            val body = buildRequestJson(model, imageBase64List, mime)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val err = try {
                    conn.errorStream?.bufferedReader()?.readText()?.take(300)
                } catch (e: Exception) { null }
                conn.disconnect()
                return@withContext Outcome.Failure("API 服务返回错误 (HTTP $code)${err?.let { "：$it" } ?: ""}，请检查配置或网络！")
            }
            val resp = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val result = try {
                parseResponse(resp)
            } catch (e: Exception) {
                return@withContext Outcome.Failure(e.message ?: "分析结果解析失败")
            }

            if (result.calories <= 0f && result.foodItems.isEmpty()) {
                return@withContext Outcome.Failure("未在图片中检测到明显的食物，请尝试调整拍摄角度重新拍照！")
            }
            Outcome.Success(result)
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: "网络或配置异常，请重试！")
        }
    }

    /**
     * 连通性检测：用极小的纯文本请求探测「接口地址 + Key + 模型」是否可用。
     * @return null=成功；非 null=失败原因
     */
    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(buildEndpoint(baseUrl)).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            val body = """{"model":"$model","messages":[{"role":"user","content":"ping"}],"max_tokens":8}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                null
            } else {
                val err = try {
                    conn.errorStream?.bufferedReader()?.readText()?.take(200)
                } catch (e: Exception) { null }
                conn.disconnect()
                "HTTP $code${err?.let { "：$it" } ?: ""}"
            }
        } catch (e: Exception) {
            "连接失败：${e.message ?: e.javaClass.simpleName}"
        }
    }

    /**
     * 纯文本营养估算：按食物名称 + 克数，让大模型给出总热量与三大营养素。
     * 用于「添加记录」和「识别确认」时自动带出营养值。
     */
    suspend fun estimateNutrition(
        baseUrl: String,
        apiKey: String,
        model: String,
        foodName: String,
        grams: Float
    ): Outcome = withContext(Dispatchers.IO) {
        try {
            if (foodName.isBlank() || grams <= 0f) {
                return@withContext Outcome.Failure("请先填写食物名称和重量")
            }
            val conn = URL(buildEndpoint(baseUrl)).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000

            val prompt = """你是营养师。食物：$foodName，重量：${grams}g。
请估算这份食物的总热量与三大宏量营养素。只返回严格 JSON，不要任何其他文本（不要 markdown 标记）：
{"calories":总千卡,"protein":蛋白质克,"fat":脂肪克,"carbs":碳水克}"""

            val body = """{"model":${JsonPrimitive(model)},"messages":[{"role":"user","content":${JsonPrimitive(prompt)}}],"max_tokens":300,"temperature":0.1}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val err = try {
                    conn.errorStream?.bufferedReader()?.readText()?.take(300)
                } catch (e: Exception) { null }
                conn.disconnect()
                return@withContext Outcome.Failure("估算失败 (HTTP $code)${err?.let { "：$it" } ?: ""}")
            }
            val resp = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // 解析 chat/completions → content → JSON
            val root = Json.parseToJsonElement(resp).jsonObject
            (root["error"] as? JsonObject)?.let {
                return@withContext Outcome.Failure("API 错误：${(it["message"] as? JsonPrimitive)?.content ?: "未知"}")
            }
            val choices = root["choices"] as? JsonArray
                ?: return@withContext Outcome.Failure("AI 响应格式异常")
            if (choices.isEmpty()) return@withContext Outcome.Failure("AI 响应 choices 为空")
            val message = (choices.first() as? JsonObject)?.get("message") as? JsonObject
                ?: return@withContext Outcome.Failure("AI 响应缺少 message")
            val content = (message["content"] as? JsonPrimitive)?.content
                ?: return@withContext Outcome.Failure("AI 响应缺少 content")
            val cleaned = extractJsonFromContent(content)
            if (cleaned.isBlank()) return@withContext Outcome.Failure("AI 未返回有效的营养数据")

            val obj = try {
                Json.parseToJsonElement(cleaned).jsonObject
            } catch (e: Exception) {
                return@withContext Outcome.Failure("营养数据 JSON 解析失败")
            }
            fun num(k: String): Float = (obj[k] as? JsonPrimitive)?.content?.toFloatOrNull() ?: 0f
            val est = com.weightmanagement.app.domain.model.EstimatedNutrition(
                calories = num("calories"),
                protein = num("protein"),
                fat = num("fat"),
                carbs = num("carbs")
            )
            if (est.calories <= 0f) return@withContext Outcome.Failure("AI 返回的热量数据无效")
            Outcome.Success(
                com.weightmanagement.app.domain.model.RecognizedFood(
                    mealName = foodName, name = foodName, grams = grams,
                    calories = est.calories, protein = est.protein,
                    fat = est.fat, carbs = est.carbs
                )
            )
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: "网络异常，请重试")
        }
    }

    /**
     * 请求体构造：移植 NutriTracker 的专业营养师提示词（针对中餐/复合菜/外卖优化）。
     */
    private fun buildRequestJson(model: String, base64Images: List<String>, mime: String): String {
        val prompt = """# Role
你是一个精通全球饮食、尤其是中国本土菜系（外卖、家常菜、地方小吃）的资深营养师与多模态计算机视觉专家。

# Task
我上传了多张同一次餐食的图片。请仔细分析这些图片，将它们视为同一次进食，识别其中的所有食物组件，估算其整体重量（克/g），并计算总热量及三大宏量营养素。

# Execution Rules (思考链路)
1. 跨图整合：综合所有图片内容，不要重复计算相同的食物。如果有不同的食物，请将它们合并计算。
2. 视觉剥离：仔细观察图片，将复合菜品（如番茄炒蛋、青椒炒肉）拆解为主要原料（如鸡蛋、番茄、油、猪肉、青椒）。
3. 本土化估计：结合中国普通盘子/碗的尺寸，合理估算食物的克数（例如：一拳头大小的米饭约为150g，一盘家常炒菜总重约200-300g）。
4. 隐性成分考量：必须考虑烹饪用油（如炒菜默认加入10-15g植物油）和调味品带来的隐性热量。

# Output Format
为了方便我的程序解析，你必须严格按照以下 JSON 格式返回数据。
注意：
1. 只能返回标准的 JSON 数据，绝对不能包含任何 Markdown 标记（例如不要用 ```json 包裹）。
2. JSON 内部不要包含任何注释（不要写 // 或 /* */）。
3. 绝对不要返回任何前言、后记、问候语或解释性文本。
4. meal_name 必须是概括性的自然描述，例如"汉堡薯条套餐"、"番茄炒蛋配米饭"、"外卖便当两荤一素"，而不是将食物项名称简单拼接。

{
  "meal_name": "概括性的饮食名称",
  "total_calories": 0,
  "total_protein": 0.0,
  "total_fat": 0.0,
  "total_carbs": 0.0,
  "food_items": [
    {
      "name": "食物或原料名称",
      "weight_g": 0,
      "calories": 0,
      "protein": 0.0,
      "fat": 0.0,
      "carbs": 0.0
    }
  ]
}"""

        val contentParts = StringBuilder()
        contentParts.append("{\"type\":\"text\",\"text\":")
        contentParts.append(JsonPrimitive(prompt))
        contentParts.append("}")
        base64Images.forEach { b64 ->
            contentParts.append(",{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:$mime;base64,$b64\",\"detail\":\"low\"}}")
        }

        return """{"model":${JsonPrimitive(model)},"messages":[{"role":"user","content":[$contentParts]}],"max_tokens":8192,"temperature":0.1}"""
    }

    /**
     * 响应解析（移植 NutriTracker parseResponse）：错误体检查、content 字符串/数组两种格式、
     * JSON 提取容错（去 markdown 围栏、前后杂文本）。
     */
    private fun parseResponse(json: String): RecognizedFood {
        val root = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (e: Exception) {
            throw Exception("AI 返回的不是有效的 JSON，原始响应(${json.take(200)}...)")
        }

        // API 错误体
        val error = root["error"] as? JsonObject
        if (error != null) {
            val msg = (error["message"] as? JsonPrimitive)?.content ?: "未知错误"
            throw Exception("API 错误: $msg")
        }

        val choices = root["choices"] as? JsonArray
            ?: throw Exception("AI 响应中没有 choices 字段，响应 keys: ${root.keys}")
        if (choices.isEmpty()) throw Exception("AI 响应 choices 为空，可能模型不支持图片识别")

        val choiceFirst = choices.first().jsonObject
        val message = choiceFirst["message"] as? JsonObject
            ?: throw Exception("message 字段不存在或格式异常")

        // content 兼容字符串/数组两种格式
        val content: String = when (val c = message["content"]) {
            null -> {
                (message["refusal"] as? JsonPrimitive)?.let { throw Exception("AI 拒绝回答: ${it.content}") }
                throw Exception("AI 响应中无 content 字段")
            }
            is JsonPrimitive -> c.content
            is JsonArray -> {
                val textPart = c.firstOrNull {
                    (it as? JsonObject)?.get("type")?.jsonPrimitive?.content == "text"
                }
                (textPart as? JsonObject)?.get("text")?.jsonPrimitive?.content ?: c.toString()
            }
            else -> c.toString()
        }

        if (content.isBlank()) {
            val finishReason = (choiceFirst["finish_reason"] as? JsonPrimitive)?.content ?: "unknown"
            throw Exception("AI 返回了空内容 (finish_reason=$finishReason)，可能被内容审查过滤")
        }

        val cleaned = extractJsonFromContent(content)
        if (cleaned.isBlank()) {
            throw Exception("AI 返回的内容中没有 JSON 数据。原始内容: ${content.take(300)}")
        }

        return try {
            parseNutrition(cleaned)
        } catch (e: Exception) {
            throw Exception("JSON 格式错误: ${e.message}。提取的内容: ${cleaned.take(300)}")
        }
    }

    private fun parseNutrition(cleaned: String): RecognizedFood {
        val obj = Json.parseToJsonElement(cleaned).jsonObject
        fun num(o: JsonObject, k: String): Float =
            (o[k] as? JsonPrimitive)?.content?.toFloatOrNull() ?: 0f
        val items = (obj["food_items"] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonObject)?.let {
                RecognizedFoodItem(
                    name = (it["name"] as? JsonPrimitive)?.content ?: "",
                    weightG = num(it, "weight_g"),
                    calories = num(it, "calories"),
                    protein = num(it, "protein"),
                    fat = num(it, "fat"),
                    carbs = num(it, "carbs")
                )
            }
        } ?: emptyList()
        val grams = items.sumOf { it.weightG.toDouble() }.toFloat()
        return RecognizedFood(
            mealName = (obj["meal_name"] as? JsonPrimitive)?.content ?: "",
            grams = grams,
            calories = num(obj, "total_calories"),
            protein = num(obj, "total_protein"),
            fat = num(obj, "total_fat"),
            carbs = num(obj, "total_carbs"),
            foodItems = items
        )
    }

    /**
     * 从 AI 返回内容中提取 JSON（移植 NutriTracker extractJsonFromContent）：
     * 处理 markdown 围栏、前后多余文本。
     */
    private fun extractJsonFromContent(content: String): String {
        var text = content.trim()

        while (text.startsWith("```")) {
            text = text.removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .trim()
        }
        while (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }

        if (!text.startsWith("{") && !text.startsWith("[")) {
            val braceStart = text.indexOf('{')
            val bracketStart = text.indexOf('[')
            val start = when {
                braceStart >= 0 && bracketStart >= 0 -> minOf(braceStart, bracketStart)
                braceStart >= 0 -> braceStart
                bracketStart >= 0 -> bracketStart
                else -> -1
            }
            if (start >= 0) text = text.substring(start)
        }

        if (!text.endsWith("}") && !text.endsWith("]")) {
            val braceEnd = text.lastIndexOf('}')
            val bracketEnd = text.lastIndexOf(']')
            val end = maxOf(braceEnd, bracketEnd)
            if (end >= 0) text = text.substring(0, end + 1)
        }

        return text.trim()
    }
}
