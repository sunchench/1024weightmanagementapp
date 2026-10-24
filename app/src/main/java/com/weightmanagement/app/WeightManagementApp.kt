package com.weightmanagement.app

import android.app.Application
import com.weightmanagement.app.data.repository.NutritionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WeightManagementApp : Application() {

    @Inject
    lateinit var nutritionRepository: NutritionRepository

    override fun onCreate() {
        super.onCreate()
        // 首次启动写入内置食物营养库（用于拍照识别的本地辅助匹配）
        CoroutineScope(Dispatchers.IO).launch {
            nutritionRepository.seedIfEmpty()
        }
    }
}
