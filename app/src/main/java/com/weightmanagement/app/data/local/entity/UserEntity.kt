package com.weightmanagement.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "",
    val gender: String = "male",
    val age: Int = 25,
    val heightCm: Float = 170f,
    val currentWeightKg: Float = 70f,
    val targetWeightKg: Float = 65f,
    val activityLevel: String = "moderate",
    val goalType: String = "lose",
    @ColumnInfo(defaultValue = "''") val name: String = "",
    @ColumnInfo(defaultValue = "''") val phone: String = "",
    @ColumnInfo(defaultValue = "''") val avatarUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
