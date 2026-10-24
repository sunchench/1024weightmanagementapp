package com.weightmanagement.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "intake")
data class IntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodName: String,
    val grams: Float,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    @ColumnInfo(defaultValue = "0") val timestamp: Long = System.currentTimeMillis(),
    val source: String = "manual",   // manual / ai / photo
    @ColumnInfo(defaultValue = "") val imageUri: String = ""   // 关联的食物图片（内部存储路径）
)
