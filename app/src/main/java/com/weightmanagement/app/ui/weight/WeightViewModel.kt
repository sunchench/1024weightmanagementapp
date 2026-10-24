package com.weightmanagement.app.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.local.entity.WeightLogEntity
import com.weightmanagement.app.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val weightRepository: WeightRepository
) : ViewModel() {

    val weights: StateFlow<List<WeightLogEntity>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(weightKg: Float, note: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch { weightRepository.add(weightKg, note, timestamp) }
    }

    fun update(log: WeightLogEntity) {
        viewModelScope.launch { weightRepository.update(log) }
    }

    fun delete(log: WeightLogEntity) {
        viewModelScope.launch { weightRepository.delete(log) }
    }
}
