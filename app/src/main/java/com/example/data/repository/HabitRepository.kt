package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.HabitDao
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitLog
import com.example.data.model.HabitTargetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class HabitWithStatus(
    val habit: Habit,
    val todayLog: HabitLog?,
    val isCompletedToday: Boolean,
    val currentValueToday: Double,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val completionRate30Days: Float
)

data class HeatmapDay(
    val date: LocalDate,
    val dateString: String,
    val completedCount: Int,
    val totalHabits: Int,
    val completionRate: Float,
    val intensity: Int // 0..4
)

data class CategoryStat(
    val category: HabitCategory,
    val activeHabitsCount: Int,
    val completionsLast30Days: Int,
    val completionRate: Float
)

data class DayOfWeekStat(
    val dayOfWeek: Int, // 1=Mon, 7=Sun
    val shortName: String,
    val rate: Float,
    val completions: Int
)

data class OverallAnalytics(
    val totalActiveHabits: Int,
    val todayCompletedCount: Int,
    val todayScheduledCount: Int,
    val todayCompletionRate: Float,
    val averageRate30Days: Float,
    val overallBestStreak: Int,
    val allTimeCompletions: Int,
    val heatmapDays: List<HeatmapDay>,
    val categoryStats: List<CategoryStat>,
    val dayOfWeekStats: List<DayOfWeekStat>
)

class HabitRepository(private val habitDao: HabitDao) {

    val activeHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()

    fun getLogsForDate(dateString: String): Flow<List<HabitLog>> =
        habitDao.getLogsForDate(dateString)

    fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>> =
        habitDao.getLogsForHabit(habitId)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteLogsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate): Boolean {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existingLog = habitDao.getLogForHabitAndDate(habit.id, dateString)

        val newCompleted = !(existingLog?.isCompleted ?: false)
        val newValue = if (newCompleted) {
            if (habit.targetType == HabitTargetType.NUMERIC) habit.targetValue else 1.0
        } else {
            0.0
        }

        val updatedLog = HabitLog(
            id = existingLog?.id ?: 0,
            habitId = habit.id,
            dateString = dateString,
            isCompleted = newCompleted,
            value = newValue,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsertLog(updatedLog)
        return newCompleted
    }

    suspend fun updateHabitValue(habit: Habit, date: LocalDate, newValue: Double) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existingLog = habitDao.getLogForHabitAndDate(habit.id, dateString)

        val isCompleted = if (habit.targetType == HabitTargetType.NUMERIC) {
            newValue >= habit.targetValue
        } else {
            newValue >= 1.0
        }

        val updatedLog = HabitLog(
            id = existingLog?.id ?: 0,
            habitId = habit.id,
            dateString = dateString,
            isCompleted = isCompleted,
            value = newValue,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsertLog(updatedLog)
    }

    suspend fun resetDemoData() {
        habitDao.clearAllLogs()
        habitDao.clearAllHabits()
        AppDatabase.populateInitialData(habitDao)
    }

    suspend fun getHabitsWithStatusForDate(date: LocalDate): List<HabitWithStatus> {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val habits = habitDao.getAllActiveHabitsOnce()
        val allLogs = habitDao.getAllLogsOnce()
        val logsByHabit = allLogs.groupBy { it.habitId }

        val dayOfWeekIso = date.dayOfWeek.value // 1=Mon .. 7=Sun

        return habits.map { habit ->
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val logsByDate = habitLogs.associateBy { it.dateString }
            val currentLog = logsByDate[dateString]
            val isCompleted = currentLog?.isCompleted == true
            val currentValue = currentLog?.value ?: 0.0

            val streaks = calculateStreaks(habit, habitLogs)
            val rate30 = calculate30DayRate(habit, habitLogs)
            val totalDone = habitLogs.count { it.isCompleted }

            HabitWithStatus(
                habit = habit,
                todayLog = currentLog,
                isCompletedToday = isCompleted,
                currentValueToday = currentValue,
                currentStreak = streaks.first,
                bestStreak = streaks.second,
                totalCompletions = totalDone,
                completionRate30Days = rate30
            )
        }
    }

    fun observeHabitsWithStatus(selectedDate: Flow<LocalDate>): Flow<List<HabitWithStatus>> {
        return combine(activeHabits, allLogs, selectedDate) { habits, logs, date ->
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val logsByHabit = logs.groupBy { it.habitId }

            habits.map { habit ->
                val habitLogs = logsByHabit[habit.id] ?: emptyList()
                val currentLog = habitLogs.firstOrNull { it.dateString == dateString }
                val isCompleted = currentLog?.isCompleted == true
                val currentValue = currentLog?.value ?: 0.0

                val streaks = calculateStreaks(habit, habitLogs)
                val rate30 = calculate30DayRate(habit, habitLogs)
                val totalDone = habitLogs.count { it.isCompleted }

                HabitWithStatus(
                    habit = habit,
                    todayLog = currentLog,
                    isCompletedToday = isCompleted,
                    currentValueToday = currentValue,
                    currentStreak = streaks.first,
                    bestStreak = streaks.second,
                    totalCompletions = totalDone,
                    completionRate30Days = rate30
                )
            }
        }
    }

    fun observeOverallAnalytics(): Flow<OverallAnalytics> {
        return combine(activeHabits, allLogs) { habits, logs ->
            computeAnalytics(habits, logs)
        }
    }

    private fun computeAnalytics(habits: List<Habit>, logs: List<HabitLog>): OverallAnalytics {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeekToday = today.dayOfWeek.value

        val todayScheduledHabits = habits.filter { it.isScheduledForDayOfWeek(dayOfWeekToday) }
        val logsByDate = logs.groupBy { it.dateString }
        val todayLogs = logsByDate[todayStr] ?: emptyList()
        val todayDoneCount = todayLogs.count { log ->
            log.isCompleted && habits.any { it.id == log.habitId }
        }

        val todayRate = if (todayScheduledHabits.isNotEmpty()) {
            (todayDoneCount.toFloat() / todayScheduledHabits.size.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Heatmap for last 60 days
        val heatmapDays = mutableListOf<HeatmapDay>()
        var totalScheduled30 = 0
        var totalCompleted30 = 0

        for (i in 59 downTo 0) {
            val d = today.minusDays(i.toLong())
            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dDayOfWeek = d.dayOfWeek.value
            val scheduledOnDay = habits.filter { it.isScheduledForDayOfWeek(dDayOfWeek) }
            val dayLogs = logsByDate[dStr] ?: emptyList()
            val dayCompleted = dayLogs.count { log ->
                log.isCompleted && habits.any { it.id == log.habitId }
            }

            val scheduledCount = scheduledOnDay.size.coerceAtLeast(1)
            val rate = if (scheduledOnDay.isNotEmpty()) {
                (dayCompleted.toFloat() / scheduledOnDay.size.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val intensity = when {
                rate <= 0f -> 0
                rate < 0.35f -> 1
                rate < 0.70f -> 2
                rate < 1.0f -> 3
                else -> 4
            }

            if (i < 30) {
                totalScheduled30 += scheduledOnDay.size
                totalCompleted30 += dayCompleted
            }

            heatmapDays.add(
                HeatmapDay(
                    date = d,
                    dateString = dStr,
                    completedCount = dayCompleted,
                    totalHabits = scheduledOnDay.size,
                    completionRate = rate,
                    intensity = intensity
                )
            )
        }

        val avgRate30 = if (totalScheduled30 > 0) {
            (totalCompleted30.toFloat() / totalScheduled30.toFloat()).coerceIn(0f, 1f)
        } else 0f

        // Streaks and totals
        var bestStreakOverall = 0
        var totalAllTime = 0
        val logsByHabit = logs.groupBy { it.habitId }

        habits.forEach { habit ->
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val (_, best) = calculateStreaks(habit, habitLogs)
            if (best > bestStreakOverall) bestStreakOverall = best
            totalAllTime += habitLogs.count { it.isCompleted }
        }

        // Category stats
        val categoryStats = HabitCategory.values().map { cat ->
            val catHabits = habits.filter { it.category == cat }
            val catHabitIds = catHabits.map { it.id }.toSet()
            val catLogs30 = logs.filter {
                catHabitIds.contains(it.habitId) &&
                        it.isCompleted &&
                        isWithinLast30Days(it.dateString, today)
            }
            val daysScheduledIn30 = catHabits.sumOf { habit ->
                (0..29).count { dayOffset ->
                    val date = today.minusDays(dayOffset.toLong())
                    habit.isScheduledForDayOfWeek(date.dayOfWeek.value)
                }
            }
            val rate = if (daysScheduledIn30 > 0) {
                (catLogs30.size.toFloat() / daysScheduledIn30.toFloat()).coerceIn(0f, 1f)
            } else 0f

            CategoryStat(
                category = cat,
                activeHabitsCount = catHabits.size,
                completionsLast30Days = catLogs30.size,
                completionRate = rate
            )
        }.filter { it.activeHabitsCount > 0 || it.completionsLast30Days > 0 }

        // Day of week stats (1=Mon .. 7=Sun)
        val shortNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayOfWeekStats = (1..7).map { dow ->
            var completions = 0
            var opportunities = 0
            for (i in 0..59) {
                val date = today.minusDays(i.toLong())
                if (date.dayOfWeek.value == dow) {
                    val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val scheduled = habits.filter { it.isScheduledForDayOfWeek(dow) }
                    opportunities += scheduled.size
                    val dLogs = logsByDate[dStr] ?: emptyList()
                    completions += dLogs.count { log ->
                        log.isCompleted && habits.any { it.id == log.habitId }
                    }
                }
            }
            val rate = if (opportunities > 0) {
                (completions.toFloat() / opportunities.toFloat()).coerceIn(0f, 1f)
            } else 0f

            DayOfWeekStat(
                dayOfWeek = dow,
                shortName = shortNames[dow],
                rate = rate,
                completions = completions
            )
        }

        return OverallAnalytics(
            totalActiveHabits = habits.size,
            todayCompletedCount = todayDoneCount,
            todayScheduledCount = todayScheduledHabits.size,
            todayCompletionRate = todayRate,
            averageRate30Days = avgRate30,
            overallBestStreak = bestStreakOverall,
            allTimeCompletions = totalAllTime,
            heatmapDays = heatmapDays,
            categoryStats = categoryStats,
            dayOfWeekStats = dayOfWeekStats
        )
    }

    private fun isWithinLast30Days(dateString: String, today: LocalDate): Boolean {
        return try {
            val date = LocalDate.parse(dateString)
            val diff = ChronoUnit.DAYS.between(date, today)
            diff in 0..29
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateStreaks(habit: Habit, logs: List<HabitLog>): Pair<Int, Int> {
        val completedDates = logs.filter { it.isCompleted }
            .mapNotNull {
                try {
                    LocalDate.parse(it.dateString)
                } catch (_: Exception) {
                    null
                }
            }
            .toSet()

        if (completedDates.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()
        var currentStreak = 0
        var checkDate = today

        // If today is scheduled and not completed yet, check if yesterday was completed to keep streak alive
        if (habit.isScheduledForDayOfWeek(checkDate.dayOfWeek.value) && !completedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        while (true) {
            val isScheduled = habit.isScheduledForDayOfWeek(checkDate.dayOfWeek.value)
            if (isScheduled) {
                if (completedDates.contains(checkDate)) {
                    currentStreak++
                } else {
                    break
                }
            }
            checkDate = checkDate.minusDays(1)
            // Limit search back to 365 days
            if (ChronoUnit.DAYS.between(checkDate, today) > 365) break
        }

        // Calculate best streak historically
        val sortedDates = completedDates.sorted()
        var bestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (!habit.isScheduledForDayOfWeek(date.dayOfWeek.value)) continue
            if (prevDate == null) {
                tempStreak = 1
            } else {
                // Check if days between were all non-scheduled or consecutive
                var daysBetween = ChronoUnit.DAYS.between(prevDate, date)
                var wasContinuous = true
                var probe = prevDate.plusDays(1)
                while (probe.isBefore(date)) {
                    if (habit.isScheduledForDayOfWeek(probe.dayOfWeek.value)) {
                        wasContinuous = false
                        break
                    }
                    probe = probe.plusDays(1)
                }

                if (wasContinuous) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            if (tempStreak > bestStreak) bestStreak = tempStreak
            prevDate = date
        }

        return Pair(currentStreak, maxOf(currentStreak, bestStreak))
    }

    private fun calculate30DayRate(habit: Habit, logs: List<HabitLog>): Float {
        val today = LocalDate.now()
        var scheduledDays = 0
        var completedDays = 0

        val completedDates = logs.filter { it.isCompleted }.map { it.dateString }.toSet()

        for (i in 0..29) {
            val date = today.minusDays(i.toLong())
            if (habit.isScheduledForDayOfWeek(date.dayOfWeek.value)) {
                scheduledDays++
                if (completedDates.contains(date.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
                    completedDays++
                }
            }
        }

        return if (scheduledDays > 0) {
            (completedDays.toFloat() / scheduledDays.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
}
