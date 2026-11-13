package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: HabitCategory = HabitCategory.HEALTH,
    val targetType: HabitTargetType = HabitTargetType.BOOLEAN,
    val targetValue: Double = 1.0,
    val unit: String = "",
    val frequencyDays: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val timeOfDay: String = HabitTimeOfDay.ANYTIME.name,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val colorHex: String = "#10B981",
    val iconName: String = "check_circle",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    fun isScheduledForDayOfWeek(dayOfWeekIso: Int): Boolean {
        // dayOfWeekIso: 1 (Mon) to 7 (Sun)
        val days = frequencyDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        return days.isEmpty() || days.contains(dayOfWeekIso)
    }
}
