package com.weightmanagement.app.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    suspend fun exportJson(): String = backupRepository.export()

    fun exportAndSave(text: String, onResult: (String) -> Unit) {
        viewModelScope.launch { onResult(backupRepository.export()) }
    }

    fun importJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                backupRepository.import(json)
                true to "恢复成功"
            }.onSuccess { onResult(true, it.second) }
                .onFailure { onResult(false, it.message ?: "恢复失败") }
        }
    }
}
