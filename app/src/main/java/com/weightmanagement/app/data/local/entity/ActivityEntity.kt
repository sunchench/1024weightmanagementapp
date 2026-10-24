package com.weightmanagement.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "activity_log")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // walking/running/...
    val startTime: Long = System.currentTimeMillis(),
    val durationMin: Int,
    val intensity: String = "moderate",
    val met: Float = 0f,
    val caloriesBurned: Float = 0f
)
