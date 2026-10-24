package com.weightmanagement.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.dao.NutritionSum
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import com.weightmanagement.app.data.repository.ActivityRepository
import com.weightmanagement.app.data.repository.NutritionRepository
import com.weightmanagement.app.data.repository.ProfileRepository
import com.weightmanagement.app.data.repository.WaterRepository
import com.weightmanagement.app.data.repository.WeightRepository
import com.weightmanagement.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    val user = profileRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val goal = profileRepository.observeGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nutrition: StateFlow<NutritionSum> = nutritionRepository.observeDaily(DateUtils.dayStart())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionSum(0f, 0f, 0f, 0f))

    val exerciseBurned: StateFlow<Float> = activityRepository.observeTodayBurned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val waterTotal: StateFlow<Int> = waterRepository.observeTotal(DateUtils.dayStart())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weights: StateFlow<List<WeightLogEntity>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
