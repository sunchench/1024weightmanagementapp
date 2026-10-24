package com.weightmanagement.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_analysis")
data class AiAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String = "",
    val rawResponse: String = "",
    val status: String = "pending",   // pending / done / error
    val createdAt: Long = System.currentTimeMillis()
)
