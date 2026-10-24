package com.weightmanagement.app.ui.nutrition

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.analyzer.AiVisionFoodAnalyzer
import com.weightmanagement.app.data.analyzer.AnalyzeOutcome
import com.weightmanagement.app.data.local.dao.NutritionSum
import com.weightmanagement.app.data.local.entity.FoodEntity
import com.weightmanagement.app.data.local.entity.IntakeEntity
import com.weightmanagement.app.data.local.entity.MealEntity
import com.weightmanagement.app.data.repository.AiConfigRepository
import com.weightmanagement.app.data.repository.NutritionRepository
import com.weightmanagement.app.domain.model.RecognizedFood
import com.weightmanagement.app.domain.util.DateUtils
import com.weightmanagement.app.domain.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** 拍照/上传识别流程的状态（mealType 记录识别结果归属的餐别） */
sealed class PhotoScanState {
    data object Idle : PhotoScanState()
    data class Analyzing(val mealType: String, val imageUri: String) : PhotoScanState()
    /** AI 识别成功，等待用户确认分量与营养 */
    data class ConfirmAi(val mealType: String, val imageUri: String, val result: RecognizedFood) : PhotoScanState()
    /** 未配置 AI：提示去设置页，或手动选择本地食物库 */
    data class NeedConfig(val mealType: String, val imageUri: String) : PhotoScanState()
    /** AI 识别失败：展示失败原因，可重试或转本地食物库手动选择 */
    data class AiFailed(val mealType: String, val imageUri: String, val message: String) : PhotoScanState()
    /** 本地食物库搜索选择（用户主动进入） */
    data class PickLocal(val mealType: String, val imageUri: String, val candidates: List<FoodEntity>, val keyword: String = "") : PhotoScanState()
    data class Error(val message: String) : PhotoScanState()
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val aiAnalyzer: AiVisionFoodAnalyzer,
    private val aiConfigRepository: AiConfigRepository,
    private val application: Application
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.dayStart())
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    val meals: StateFlow<List<MealEntity>> = _selectedDay.flatMapLatest { day ->
        nutritionRepository.observeMeals(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 选中日的全部饮食记录（跨餐别合并，按时间倒序），供单一列表展示。
     *  单一 SQL 按日期观测：切日期立即整体重发射，无多流合并竞态/残留。 */
    val dayIntakes: StateFlow<List<Pair<String, IntakeEntity>>> = _selectedDay.flatMapLatest { day ->
        nutritionRepository.observeIntakesByDay(day)
    }.map { list -> list.map { it.mealType to it.intake } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val daily: StateFlow<NutritionSum> = _selectedDay.flatMapLatest { day ->
        nutritionRepository.observeDaily(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionSum(0f, 0f, 0f, 0f))

    val availableDays: StateFlow<List<Long>> = nutritionRepository.observeAllDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiConfigured: StateFlow<Boolean> = aiConfigRepository.config
        .map { it.configured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _scanState = MutableStateFlow<PhotoScanState>(PhotoScanState.Idle)
    val scanState: StateFlow<PhotoScanState> = _scanState.asStateFlow()

    fun selectDay(day: Long) {
        _selectedDay.value = day
    }

    fun intakesForMeal(mealId: Long) = nutritionRepository.observeIntakes(mealId)

    fun add(
        mealType: String,
        name: String,
        grams: Float,
        calories: Float,
        protein: Float,
        fat: Float,
        carbs: Float,
        source: String = "manual",
        imageUri: String = ""
    ) {
        viewModelScope.launch {
            nutritionRepository.addIntake(_selectedDay.value, mealType, name, grams, calories, protein, fat, carbs, source, imageUri)
        }
    }

    fun delete(intake: IntakeEntity) {
        viewModelScope.launch { nutritionRepository.deleteIntake(intake) }
    }

    /** 相机拍照落盘的临时文件 → 压缩入库 → 识别 */
    fun onCameraShot(mealType: String, file: File) {
        viewModelScope.launch {
            val saved: File? = withContext(Dispatchers.IO) {
                ImageUtils.saveFileToInternal(application, file)
            }
            withContext(Dispatchers.IO) { file.delete() }
            if (saved == null) {
                _scanState.value = PhotoScanState.Error("图片读取失败，请重试")
                return@launch
            }
            runAiAnalysis(mealType, saved)
        }
    }

    /** 相册选图 → 压缩入库 → 识别 */
    fun onImagePicked(mealType: String, uri: Uri) {
        viewModelScope.launch {
            val saved: File? = withContext(Dispatchers.IO) {
                ImageUtils.saveToInternal(application, uri)
            }
            if (saved == null) {
                _scanState.value = PhotoScanState.Error("图片读取失败，请重试")
                return@launch
            }
            runAiAnalysis(mealType, saved)
        }
    }

    /** 调用 AI 大模型识别（移植 NutriTracker 调用链路：失败原因透出，不静默回退） */
    private suspend fun runAiAnalysis(mealType: String, saved: File) {
        _scanState.value = PhotoScanState.Analyzing(mealType = mealType, imageUri = saved.absolutePath)
        val b64 = withContext(Dispatchers.IO) { ImageUtils.toBase64(saved) }
        val outcome = aiAnalyzer.analyze(listOf(b64), "image/jpeg")
        _scanState.value = when (outcome) {
            is AnalyzeOutcome.Success ->
                PhotoScanState.ConfirmAi(mealType, saved.absolutePath, outcome.result)
            is AnalyzeOutcome.NotConfigured ->
                PhotoScanState.NeedConfig(mealType, saved.absolutePath)
            is AnalyzeOutcome.Failure ->
                PhotoScanState.AiFailed(mealType, saved.absolutePath, outcome.message)
        }
    }

    /** 从 AiFailed/NeedConfig 重试识别 */
    fun retryAi() {
        val cur = _scanState.value
        val mealType = when (cur) {
            is PhotoScanState.AiFailed -> cur.mealType
            is PhotoScanState.NeedConfig -> cur.mealType
            else -> return
        }
        val imageUri = when (cur) {
            is PhotoScanState.AiFailed -> cur.imageUri
            is PhotoScanState.NeedConfig -> cur.imageUri
            else -> return
        }
        val f = File(imageUri)
        if (f.exists()) {
            viewModelScope.launch { runAiAnalysis(mealType, f) }
        }
    }

    /** 转本地食物库手动选择 */
    fun goLocalPicker() {
        val cur = _scanState.value
        val pair = when (cur) {
            is PhotoScanState.AiFailed -> cur.mealType to cur.imageUri
            is PhotoScanState.NeedConfig -> cur.mealType to cur.imageUri
            is PhotoScanState.Analyzing -> cur.mealType to cur.imageUri
            else -> return
        }
        viewModelScope.launch {
            val candidates = nutritionRepository.searchFood("")
            _scanState.value = PhotoScanState.PickLocal(pair.first, pair.second, candidates)
        }
    }

    /** 本地候选搜索 */
    fun searchLocal(keyword: String) {
        val cur = _scanState.value
        if (cur is PhotoScanState.PickLocal) {
            viewModelScope.launch {
                val list = nutritionRepository.searchFood(keyword)
                _scanState.value = cur.copy(candidates = list, keyword = keyword)
            }
        }
    }

    /** 用户确认 AI 识别结果（可修改后保存） */
    fun confirmAi(food: RecognizedFood) {
        val cur = _scanState.value
        if (cur is PhotoScanState.ConfirmAi) {
            val displayName = food.name.ifBlank { food.displayName }.ifBlank { "AI 识别食物" }
            add(
                cur.mealType,
                displayName,
                food.grams, food.calories, food.protein, food.fat, food.carbs,
                source = "ai", imageUri = cur.imageUri
            )
            _scanState.value = PhotoScanState.Idle
        }
    }

    /** 用户从本地食物库选择 */
    fun pickLocal(food: FoodEntity, grams: Float) {
        val cur = _scanState.value
        if (cur is PhotoScanState.PickLocal) {
            val g = if (grams > 0f) grams else food.servingSizeG
            val ratio = g / food.servingSizeG.coerceAtLeast(0.01f)
            add(
                cur.mealType, food.name, g,
                food.calories * ratio, food.protein * ratio, food.fat * ratio, food.carbs * ratio,
                source = "photo", imageUri = cur.imageUri
            )
            _scanState.value = PhotoScanState.Idle
        }
    }

    fun dismissScan() {
        _scanState.value = PhotoScanState.Idle
    }

    // ---------- 营养估算（手动添加 / 识别确认 自动带出） ----------

    private val _estimateState = MutableStateFlow<EstimateUiState>(EstimateUiState.Idle)
    val estimateState: StateFlow<EstimateUiState> = _estimateState.asStateFlow()

    sealed class EstimateUiState {
        data object Idle : EstimateUiState()
        data object Loading : EstimateUiState()
        /** 成功：带估算的营养值（fields: cal/pro/fat/carb） */
        data class Done(val grams: Float, val calories: Float, val protein: Float, val fat: Float, val carbs: Float, val source: String) : EstimateUiState()
        data class Error(val message: String) : EstimateUiState()
    }

    fun resetEstimate() {
        _estimateState.value = EstimateUiState.Idle
    }

    /** 调本地库/大模型估算营养并填充（AddIntakeDialog / ConfirmAiDialog 的「自动估算」） */
    fun estimateNutrition(foodName: String, grams: Float) {
        viewModelScope.launch {
            _estimateState.value = EstimateUiState.Loading
            val outcome = aiAnalyzer.estimateNutrition(foodName, grams) { q -> nutritionRepository.searchFood(q) }
            _estimateState.value = when (outcome) {
                is AnalyzeOutcome.Success -> EstimateUiState.Done(
                    grams = outcome.result.grams,
                    calories = outcome.result.calories,
                    protein = outcome.result.protein,
                    fat = outcome.result.fat,
                    carbs = outcome.result.carbs,
                    source = "local"
                )
                is AnalyzeOutcome.NotConfigured -> EstimateUiState.Error("本地库无匹配，且未配置 AI 接口，请手动填写或到设置页配置 AI")
                is AnalyzeOutcome.Failure -> EstimateUiState.Error(outcome.message)
            }
        }
    }
}
