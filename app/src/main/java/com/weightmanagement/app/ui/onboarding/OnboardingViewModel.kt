package com.weightmanagement.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.UserEntity
import com.weightmanagement.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    fun save(user: UserEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            profileRepository.saveUser(user)
            onDone()
        }
    }
}
