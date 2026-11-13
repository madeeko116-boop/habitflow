package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habitId", "dateString"], unique = true),
        Index(value = ["dateString"]),
        Index(value = ["habitId"])
    ]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val dateString: String, // Format: YYYY-MM-DD
    val isCompleted: Boolean = false,
    val value: Double = 0.0,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
