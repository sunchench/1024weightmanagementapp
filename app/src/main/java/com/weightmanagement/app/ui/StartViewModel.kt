package com.weightmanagement.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    val isOnboarded: StateFlow<Boolean?> = profileRepository.observeUser()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
