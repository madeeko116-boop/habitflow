package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

enum class HabitCategory(
    val displayName: String,
    val colorHex: String,
    val defaultIconName: String
) {
    HEALTH("Health & Fitness", "#10B981", "fitness_center"),
    MINDFULNESS("Mindfulness", "#8B5CF6", "self_improvement"),
    PRODUCTIVITY("Productivity", "#0EA5E9", "trending_up"),
    LEARNING("Learning", "#6366F1", "menu_book"),
    CREATIVITY("Creativity", "#F59E0B", "brush"),
    LIFESTYLE("Lifestyle", "#F43F5E", "spa");

    fun getIcon(): ImageVector {
        return when (this) {
            HEALTH -> Icons.Default.FitnessCenter
            MINDFULNESS -> Icons.Default.SelfImprovement
            PRODUCTIVITY -> Icons.AutoMirrored.Filled.TrendingUp
            LEARNING -> Icons.AutoMirrored.Filled.MenuBook
            CREATIVITY -> Icons.Default.Brush
            LIFESTYLE -> Icons.Default.Spa
        }
    }
}

enum class HabitTargetType {
    BOOLEAN,
    NUMERIC
}

enum class HabitTimeOfDay(val displayName: String) {
    ANYTIME("Anytime"),
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening")
}
