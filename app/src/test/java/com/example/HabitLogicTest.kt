package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitLogicTest {

    @Test
    fun habit_scheduleForDayOfWeek_correct() {
        val dailyHabit = Habit(
            title = "Morning Water",
            category = HabitCategory.HEALTH,
            frequencyDays = "1,2,3,4,5,6,7"
        )
        assertTrue(dailyHabit.isScheduledForDayOfWeek(1)) // Monday
        assertTrue(dailyHabit.isScheduledForDayOfWeek(7)) // Sunday

        val weekdayHabit = Habit(
            title = "Work Habit",
            category = HabitCategory.PRODUCTIVITY,
            frequencyDays = "1,2,3,4,5"
        )
        assertTrue(weekdayHabit.isScheduledForDayOfWeek(1)) // Monday
        assertTrue(weekdayHabit.isScheduledForDayOfWeek(5)) // Friday
        assertFalse(weekdayHabit.isScheduledForDayOfWeek(6)) // Saturday
        assertFalse(weekdayHabit.isScheduledForDayOfWeek(7)) // Sunday
    }

    @Test
    fun habit_targetType_numericDefaults() {
        val habit = Habit(
            title = "Drink Water",
            category = HabitCategory.HEALTH,
            targetType = HabitTargetType.NUMERIC,
            targetValue = 2000.0,
            unit = "ml"
        )
        assertEquals(HabitTargetType.NUMERIC, habit.targetType)
        assertEquals(2000.0, habit.targetValue, 0.001)
        assertEquals("ml", habit.unit)
    }
}
