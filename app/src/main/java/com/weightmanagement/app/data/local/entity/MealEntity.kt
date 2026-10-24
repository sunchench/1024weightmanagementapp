package com.weightmanagement.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "meal")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,          // 当天 0 点时间戳
    val mealType: String,    // breakfast/lunch/dinner/snack
    val name: String = ""
)
