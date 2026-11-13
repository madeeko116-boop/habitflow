package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.MainActivity
import com.example.R
import com.example.receiver.HabitReminderReceiver
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_ID = "habit_flow_daily_reminders"
    private const val CHANNEL_NAME = "Daily Habit Reminders"
    private const val CHANNEL_DESC = "Notifications to remind you of your daily habits and streaks"
    const val NOTIFICATION_ID_DAILY = 1001
    const val NOTIFICATION_ID_TEST = 1002

    const val ACTION_DAILY_REMINDER = "com.aistudio.habitflow.ACTION_DAILY_REMINDER"
    const val PREFS_NAME = "habit_flow_notification_prefs"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    const val KEY_REMINDERS_ENABLED = "reminders_enabled"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        createNotificationChannel(context)

        // Save preferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_REMINDERS_ENABLED, true)
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback for devices restricting exact alarms
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_REMINDERS_ENABLED, false) }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showDailyReminderNotification(
        context: Context,
        pendingHabits: List<String>,
        completedCount: Int,
        totalCount: Int
    ) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val contentText: String
        val bigText: String

        if (pendingHabits.isEmpty() && totalCount > 0) {
            title = "🎉 All Habits Complete!"
            contentText = "Amazing dedication! You finished all $totalCount habits today. Streak protected!"
            bigText = "Amazing dedication! You've checked off every single habit for today. Your consistency is paying off. Rest well and keep the momentum going tomorrow!"
        } else if (pendingHabits.isNotEmpty()) {
            val remaining = pendingHabits.size
            title = "⚡ Keep Your Streak Alive!"
            contentText = "$remaining habit${if (remaining > 1) "s" else ""} left for today: ${pendingHabits.take(2).joinToString(", ")}"
            val listString = pendingHabits.joinToString("\n• ", prefix = "• ")
            bigText = "You have $remaining habit${if (remaining > 1) "s" else ""} waiting:\n$listString\n\nTake a few minutes to check them off and protect your streak!"
        } else {
            title = "✨ HabitFlow Daily Check-in"
            contentText = "Time to check in on your habits and build your routine!"
            bigText = "Take a moment to review your goals for today. Consistency is the key to lasting change!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, notification)
        } catch (_: SecurityException) {
            // Permission missing
        }
    }

    fun showInstantTestNotification(context: Context) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔥 HabitFlow Reminder (Test)")
            .setContentText("Your daily habits are calling! Don't break the streak today.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "This is how your daily reminder appears!\n\n" +
                            "• Drink 2L Water (In Progress)\n" +
                            "• Read 20 Pages (Pending)\n" +
                            "• Evening Reflection (Pending)\n\n" +
                            "Tap to open HabitFlow and check them off!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
        } catch (_: SecurityException) {
            // Permission missing
        }
    }
}
