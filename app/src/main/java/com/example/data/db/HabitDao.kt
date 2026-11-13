package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitById(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitByIdOnce(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE isArchived = 0")
    suspend fun getAllActiveHabitsOnce(): List<Habit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<Habit>): List<Long>

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("UPDATE habits SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    // Logs
    @Query("SELECT * FROM habit_logs WHERE dateString = :dateString")
    fun getLogsForDate(dateString: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE dateString = :dateString")
    suspend fun getLogsForDateOnce(dateString: String): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY dateString DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY dateString ASC")
    suspend fun getLogsForHabitOnce(habitId: Long): List<HabitLog>

    @Query("SELECT * FROM habit_logs ORDER BY dateString ASC")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs ORDER BY dateString ASC")
    suspend fun getAllLogsOnce(): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getLogForHabitAndDate(habitId: Long, dateString: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<HabitLog>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()
}
