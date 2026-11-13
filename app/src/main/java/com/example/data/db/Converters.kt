package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.HabitCategory
import com.example.data.model.HabitTargetType

class Converters {
    @TypeConverter
    fun fromCategory(category: HabitCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): HabitCategory =
        try {
            HabitCategory.valueOf(value)
        } catch (_: Exception) {
            HabitCategory.HEALTH
        }

    @TypeConverter
    fun fromTargetType(type: HabitTargetType): String = type.name

    @TypeConverter
    fun toTargetType(value: String): HabitTargetType =
        try {
            HabitTargetType.valueOf(value)
        } catch (_: Exception) {
            HabitTargetType.BOOLEAN
        }
}
