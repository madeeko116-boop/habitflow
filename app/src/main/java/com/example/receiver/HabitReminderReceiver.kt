package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != NotificationHelper.ACTION_DAILY_REMINDER
        ) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == NotificationHelper.ACTION_DAILY_REMINDER) {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.habitDao()
                    val today = LocalDate.now()
                    val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val dayOfWeek = today.dayOfWeek.value

                    val activeHabits = dao.getAllActiveHabitsOnce()
                    val scheduledToday = activeHabits.filter { it.isScheduledForDayOfWeek(dayOfWeek) }
                    val todayLogs = dao.getLogsForDateOnce(todayStr).associateBy { it.habitId }

                    val pendingHabitTitles = scheduledToday
                        .filter { habit ->
                            val log = todayLogs[habit.id]
                            log == null || !log.isCompleted
                        }
                        .map { it.title }

                    val completedCount = scheduledToday.size - pendingHabitTitles.size

                    NotificationHelper.showDailyReminderNotification(
                        context = context,
                        pendingHabits = pendingHabitTitles,
                        completedCount = completedCount,
                        totalCount = scheduledToday.size
                    )
                }

                // Reschedule for next day (on both daily alarm firing and device boot)
                val prefs = context.getSharedPreferences(
                    NotificationHelper.PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                val enabled = prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)
                val hour = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 20)
                val minute = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)

                if (enabled) {
                    NotificationHelper.scheduleDailyReminder(context, hour, minute)
                }
            } catch (_: Exception) {
                // Ignore receiver error gracefully
            } finally {
                pendingResult.finish()
            }
        }
    }
}
