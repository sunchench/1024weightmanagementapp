package com.weightmanagement.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.ActivityEntity
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.repository.ActivityRepository
import com.weightmanagement.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val activities: StateFlow<List<ActivityEntity>> = activityRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val user: StateFlow<UserEntity?> = profileRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun add(type: String, durationMin: Int, intensity: String, weightKg: Float) {
        viewModelScope.launch { activityRepository.add(type, durationMin, intensity, weightKg) }
    }

    fun delete(activity: ActivityEntity) {
        viewModelScope.launch { activityRepository.delete(activity) }
    }
}
