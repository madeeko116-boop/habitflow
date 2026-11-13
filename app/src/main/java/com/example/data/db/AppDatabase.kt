package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitLog
import com.example.data.model.HabitTargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [Habit::class, HabitLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_flow_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.habitDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: HabitDao) {
            val initialHabits = listOf(
                Habit(
                    id = 1,
                    title = "Drink 2L Water",
                    description = "Stay hydrated throughout the day for focus and energy",
                    category = HabitCategory.HEALTH,
                    targetType = HabitTargetType.NUMERIC,
                    targetValue = 2000.0,
                    unit = "ml",
                    frequencyDays = "1,2,3,4,5,6,7",
                    timeOfDay = "ANYTIME",
                    reminderEnabled = true,
                    reminderHour = 10,
                    reminderMinute = 0,
                    colorHex = "#10B981"
                ),
                Habit(
                    id = 2,
                    title = "Read 20 Pages",
                    description = "Non-fiction or inspiring books to broaden perspective",
                    category = HabitCategory.LEARNING,
                    targetType = HabitTargetType.NUMERIC,
                    targetValue = 20.0,
                    unit = "pages",
                    frequencyDays = "1,2,3,4,5,6,7",
                    timeOfDay = "EVENING",
                    reminderEnabled = true,
                    reminderHour = 21,
                    reminderMinute = 0,
                    colorHex = "#6366F1"
                ),
                Habit(
                    id = 3,
                    title = "Morning Meditation",
                    description = "10 minutes of box breathing and mindfulness",
                    category = HabitCategory.MINDFULNESS,
                    targetType = HabitTargetType.BOOLEAN,
                    targetValue = 1.0,
                    unit = "",
                    frequencyDays = "1,2,3,4,5,6,7",
                    timeOfDay = "MORNING",
                    reminderEnabled = true,
                    reminderHour = 7,
                    reminderMinute = 30,
                    colorHex = "#8B5CF6"
                ),
                Habit(
                    id = 4,
                    title = "Deep Work Session",
                    description = "90 minutes of uninterrupted focused coding or writing",
                    category = HabitCategory.PRODUCTIVITY,
                    targetType = HabitTargetType.NUMERIC,
                    targetValue = 90.0,
                    unit = "min",
                    frequencyDays = "1,2,3,4,5",
                    timeOfDay = "MORNING",
                    reminderEnabled = false,
                    reminderHour = 9,
                    reminderMinute = 0,
                    colorHex = "#0EA5E9"
                ),
                Habit(
                    id = 5,
                    title = "Daily Evening Walk",
                    description = "Clear your mind and get some fresh air",
                    category = HabitCategory.LIFESTYLE,
                    targetType = HabitTargetType.BOOLEAN,
                    targetValue = 1.0,
                    unit = "",
                    frequencyDays = "1,2,3,4,5,6,7",
                    timeOfDay = "EVENING",
                    reminderEnabled = true,
                    reminderHour = 18,
                    reminderMinute = 30,
                    colorHex = "#F43F5E"
                )
            )

            dao.insertAllHabits(initialHabits)

            // Seed past 14 days of realistic logs
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val sampleLogs = mutableListOf<HabitLog>()

            // Habit 1 (Water): completed 12 of 14 days
            for (i in 14 downTo 1) {
                val date = today.minusDays(i.toLong())
                val isCompleted = i != 4 && i != 11
                sampleLogs.add(
                    HabitLog(
                        habitId = 1,
                        dateString = date.format(formatter),
                        isCompleted = isCompleted,
                        value = if (isCompleted) 2000.0 else 1250.0
                    )
                )
            }

            // Habit 2 (Reading): completed 10 of 14 days
            for (i in 14 downTo 1) {
                val date = today.minusDays(i.toLong())
                val isCompleted = i % 3 != 0
                sampleLogs.add(
                    HabitLog(
                        habitId = 2,
                        dateString = date.format(formatter),
                        isCompleted = isCompleted,
                        value = if (isCompleted) 25.0 else 8.0
                    )
                )
            }

            // Habit 3 (Meditation): strong current streak of 7 days
            for (i in 14 downTo 1) {
                val date = today.minusDays(i.toLong())
                val isCompleted = i <= 7 || i == 12 || i == 14
                sampleLogs.add(
                    HabitLog(
                        habitId = 3,
                        dateString = date.format(formatter),
                        isCompleted = isCompleted,
                        value = if (isCompleted) 1.0 else 0.0
                    )
                )
            }

            // Habit 4 (Deep Work): weekdays
            for (i in 14 downTo 1) {
                val date = today.minusDays(i.toLong())
                val dayOfWeek = date.dayOfWeek.value
                if (dayOfWeek in 1..5) {
                    val isCompleted = i != 8
                    sampleLogs.add(
                        HabitLog(
                            habitId = 4,
                            dateString = date.format(formatter),
                            isCompleted = isCompleted,
                            value = if (isCompleted) 90.0 else 45.0
                        )
                    )
                }
            }

            // Habit 5 (Evening Walk): 11 of 14 days
            for (i in 14 downTo 1) {
                val date = today.minusDays(i.toLong())
                val isCompleted = i !in listOf(2, 7, 13)
                sampleLogs.add(
                    HabitLog(
                        habitId = 5,
                        dateString = date.format(formatter),
                        isCompleted = isCompleted,
                        value = if (isCompleted) 1.0 else 0.0
                    )
                )
            }

            dao.insertAllLogs(sampleLogs)
        }
    }
}
