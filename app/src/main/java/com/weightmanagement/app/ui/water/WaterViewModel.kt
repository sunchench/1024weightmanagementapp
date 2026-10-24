package com.weightmanagement.app.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.GoalEntity
import com.weightmanagement.app.data.local.entity.WaterEntity
import com.weightmanagement.app.data.repository.ProfileRepository
import com.weightmanagement.app.data.repository.WaterRepository
import com.weightmanagement.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.dayStart())
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    val todayTotal: StateFlow<Int> = _selectedDay.flatMapLatest { day ->
        waterRepository.observeTotal(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val records: StateFlow<List<WaterEntity>> = _selectedDay.flatMapLatest { day ->
        waterRepository.observeRecords(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableDays: StateFlow<List<Long>> = waterRepository.observeAllDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goal: StateFlow<GoalEntity?> = profileRepository.observeGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDay(day: Long) {
        _selectedDay.value = day
    }

    fun add(ml: Int) {
        viewModelScope.launch { waterRepository.add(ml) }
    }

    fun delete(water: WaterEntity) {
        viewModelScope.launch { waterRepository.delete(water) }
    }
}
