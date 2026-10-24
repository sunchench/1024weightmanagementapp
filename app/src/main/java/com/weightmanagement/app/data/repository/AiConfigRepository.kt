package com.weightmanagement.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存储「AI 食物识别」的视觉模型配置（OpenAI 兼容接口）。
 * 未配置时，App 走本地食物库辅助识别，无需任何密钥即可使用。
 */
@Singleton
class AiConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory
        .create { context.preferencesDataStoreFile("ai_config") }

    private val KEY_BASE = stringPreferencesKey("ai_base_url")
    private val KEY_KEY = stringPreferencesKey("ai_api_key")
    private val KEY_MODEL = stringPreferencesKey("ai_model")

    val config: Flow<AiConfig> = dataStore.data.map {
        AiConfig(
            baseUrl = it[KEY_BASE] ?: "",
            apiKey = it[KEY_KEY] ?: "",
            model = it[KEY_MODEL] ?: ""
        )
    }

    suspend fun save(baseUrl: String, apiKey: String, model: String) {
        dataStore.edit {
            it[KEY_BASE] = baseUrl.trim()
            it[KEY_KEY] = apiKey.trim()
            it[KEY_MODEL] = model.trim()
        }
    }
}

data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
) {
    val configured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}
