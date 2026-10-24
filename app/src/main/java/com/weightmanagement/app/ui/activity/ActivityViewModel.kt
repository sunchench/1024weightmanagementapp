package com.weightmanagement.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.repository.ActivityRepository
import com.weightmanagement.app.data.repository.ProfileRepository
import com.weightmanagement.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.dayStart())
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    /** 选中日的运动记录（本地时区归天，与饮水页同模式） */
    val activities: StateFlow<List<ActivityEntity>> = _selectedDay.flatMapLatest { day ->
        activityRepository.observeByDay(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 所有有运动记录的日期（去重倒序，首项由 UI 补「今天」） */
    val availableDays: StateFlow<List<Long>> = activityRepository.observeAllDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val user: StateFlow<UserEntity?> = profileRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDay(day: Long) {
        _selectedDay.value = day
    }

    fun add(type: String, durationMin: Int, intensity: String, weightKg: Float) {
        viewModelScope.launch {
            activityRepository.add(type, durationMin, intensity, weightKg)
            // 新记录落在今天时自动切回今天视图
            _selectedDay.value = DateUtils.dayStart()
        }
    }

    fun delete(activity: ActivityEntity) {
        viewModelScope.launch { activityRepository.delete(activity) }
    }
}
