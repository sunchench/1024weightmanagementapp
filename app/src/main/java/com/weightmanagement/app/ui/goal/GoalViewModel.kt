package com.weightmanagement.app.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.repository.ActivityRepository
import com.weightmanagement.app.data.repository.NutritionRepository
import com.weightmanagement.app.data.repository.ProfileRepository
import com.weightmanagement.app.data.repository.WaterRepository
import com.weightmanagement.app.data.repository.WeightRepository
import com.weightmanagement.app.domain.recommendation.RecommendationEngine
import com.weightmanagement.app.domain.recommendation.Recommendations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    val user: StateFlow<UserEntity?> = profileRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val goal = profileRepository.observeGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _recommendations = MutableStateFlow<Recommendations?>(null)
    val recommendations: StateFlow<Recommendations?> = _recommendations.asStateFlow()

    init {
        refreshRecommendations()
    }

    /** 基于最近一周数据计算建议（目标值变化或记录更新后可再次调用） */
    fun refreshRecommendations() {
        viewModelScope.launch {
            val g = goal.value
            val calorieTarget = g?.calorieTarget ?: 2000
            val waterTarget = g?.waterTargetMl ?: 2000
            val rec = RecommendationEngine.build(
                calorieTarget = calorieTarget,
                waterTargetMl = waterTarget,
                dailyCalories = nutritionRepository.dailyCaloriesSinceDays(7),
                exerciseByDay = activityRepository.dailyBurnedSinceDays(7),
                waterByDay = waterRepository.dailyTotalSinceDays(7),
                weightFirstKg = weightRepository.weekChangeKg(7)?.first,
                weightLastKg = weightRepository.weekChangeKg(7)?.second
            )
            _recommendations.value = rec
        }
    }

    fun update(user: UserEntity, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                profileRepository.saveUser(user)
                refreshRecommendations()
                onDone(true, "目标已更新，营养目标已重新计算")
            } catch (e: Exception) {
                onDone(false, "保存失败：${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
}
