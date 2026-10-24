package com.weightmanagement.app.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weightmanagement.app.data.repository.AiConfigRepository
import com.weightmanagement.app.data.repository.BackupRepository
import com.weightmanagement.app.data.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val aiAnalyzer: com.weightmanagement.app.data.analyzer.AiVisionFoodAnalyzer,
    private val nutritionRepository: NutritionRepository,
    private val application: Application
) : ViewModel() {

    val aiConfig: StateFlow<com.weightmanagement.app.data.repository.AiConfig> = aiConfigRepository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.weightmanagement.app.data.repository.AiConfig())

    /** 检测结果：null=未检测，null-ish 不用；用 sealed 简化为状态+消息 */
    private val _testResult = kotlinx.coroutines.flow.MutableStateFlow<Pair<Boolean, String>?>(null)
    val testResult: StateFlow<Pair<Boolean, String>?> = _testResult

    private val _testing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing

    // ---------- 检查更新 ----------

    sealed class UpdateUiState {
        data object Idle : UpdateUiState()
        data object Checking : UpdateUiState()
        data class UpToDate(val currentName: String) : UpdateUiState()
        data class Available(
            val versionName: String,
            val releaseNotes: List<String>,
            val downloadUrl: String
        ) : UpdateUiState()
        data class Error(val message: String) : UpdateUiState()
    }

    private val _updateState = kotlinx.coroutines.flow.MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState

    private val updateChecker = com.weightmanagement.app.data.remote.UpdateChecker()

    fun checkUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            val versionInfo = try {
                val pm = application.packageManager
                val info = pm.getPackageInfo(application.packageName, 0)
                @Suppress("DEPRECATION")
                val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                code to (info.versionName ?: "")
            } catch (e: Exception) {
                0L to ""
            }
            _updateState.value = when (val r = updateChecker.check(versionInfo.first, versionInfo.second)) {
                is com.weightmanagement.app.data.remote.UpdateChecker.Result.UpToDate ->
                    UpdateUiState.UpToDate(r.currentName)
                is com.weightmanagement.app.data.remote.UpdateChecker.Result.UpdateAvailable ->
                    UpdateUiState.Available(r.remote.versionName, r.remote.releaseNotes, r.remote.downloadUrl)
                is com.weightmanagement.app.data.remote.UpdateChecker.Result.Error ->
                    UpdateUiState.Error(r.message)
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateUiState.Idle
    }

    fun testAiConfig(baseUrl: String, apiKey: String, model: String) {
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            _testResult.value = false to "请先填写完整的接口地址、API Key 和模型名"
            return
        }
        _testing.value = true
        _testResult.value = null
        viewModelScope.launch {
            val err = aiAnalyzer.testConnection(baseUrl.trim(), apiKey.trim(), model.trim())
            _testing.value = false
            _testResult.value = if (err == null) true to "连接成功，模型可用 ✓" else false to err
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            backupRepository.clearAll()
            onDone()
        }
    }

    fun saveAiConfig(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch { aiConfigRepository.save(baseUrl, apiKey, model) }
    }
}
