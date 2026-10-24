package com.weightmanagement.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val id: Int = 1,
    val targetWeightKg: Float,
    val calorieTarget: Int,
    val proteinTarget: Float,
    val carbTarget: Float,
    val fatTarget: Float,
    val waterTargetMl: Int = 2000,
    val ratePerWeekKg: Float = 0.5f
)
