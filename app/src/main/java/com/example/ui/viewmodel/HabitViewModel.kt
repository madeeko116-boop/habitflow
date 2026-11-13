package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitTimeOfDay
import com.example.data.repository.HabitRepository
import com.example.data.repository.HabitWithStatus
import com.example.data.repository.OverallAnalytics
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitFilterType(val label: String) {
    ALL("All"),
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    HEALTH("Health"),
    MINDFULNESS("Mind"),
    PRODUCTIVITY("Productivity"),
    LEARNING("Learning")
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HabitFilterType.ALL)
    val selectedFilter: StateFlow<HabitFilterType> = _selectedFilter.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    private val _selectedHabitDetail = MutableStateFlow<HabitWithStatus?>(null)
    val selectedHabitDetail: StateFlow<HabitWithStatus?> = _selectedHabitDetail.asStateFlow()

    private val _isRemindersSheetOpen = MutableStateFlow(false)
    val isRemindersSheetOpen: StateFlow<Boolean> = _isRemindersSheetOpen.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _reminderHour = MutableStateFlow(20)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    val habitsWithStatus: StateFlow<List<HabitWithStatus>>
    val filteredHabits: StateFlow<List<HabitWithStatus>>
    val overallAnalytics: StateFlow<OverallAnalytics>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())

        // Load reminder settings
        val prefs = application.getSharedPreferences(
            NotificationHelper.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        _remindersEnabled.value = prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, true)
        _reminderHour.value = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 20)
        _reminderMinute.value = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
        _hasNotificationPermission.value = NotificationHelper.hasNotificationPermission(application)

        // Schedule notification if enabled
        if (_remindersEnabled.value) {
            NotificationHelper.scheduleDailyReminder(
                application,
                _reminderHour.value,
                _reminderMinute.value
            )
        }

        habitsWithStatus = repository.observeHabitsWithStatus(_selectedDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        filteredHabits = combine(habitsWithStatus, _selectedFilter, _selectedDate) { list, filter, date ->
            val dayOfWeek = date.dayOfWeek.value
            val scheduledForDay = list.filter { it.habit.isScheduledForDayOfWeek(dayOfWeek) }

            when (filter) {
                HabitFilterType.ALL -> scheduledForDay
                HabitFilterType.MORNING -> scheduledForDay.filter {
                    it.habit.timeOfDay.equals(HabitTimeOfDay.MORNING.name, ignoreCase = true)
                }
                HabitFilterType.AFTERNOON -> scheduledForDay.filter {
                    it.habit.timeOfDay.equals(HabitTimeOfDay.AFTERNOON.name, ignoreCase = true)
                }
                HabitFilterType.EVENING -> scheduledForDay.filter {
                    it.habit.timeOfDay.equals(HabitTimeOfDay.EVENING.name, ignoreCase = true)
                }
                HabitFilterType.HEALTH -> scheduledForDay.filter { it.habit.category == HabitCategory.HEALTH }
                HabitFilterType.MINDFULNESS -> scheduledForDay.filter { it.habit.category == HabitCategory.MINDFULNESS }
                HabitFilterType.PRODUCTIVITY -> scheduledForDay.filter { it.habit.category == HabitCategory.PRODUCTIVITY }
                HabitFilterType.LEARNING -> scheduledForDay.filter { it.habit.category == HabitCategory.LEARNING }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        overallAnalytics = repository.observeOverallAnalytics()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = OverallAnalytics(
                    totalActiveHabits = 0,
                    todayCompletedCount = 0,
                    todayScheduledCount = 0,
                    todayCompletionRate = 0f,
                    averageRate30Days = 0f,
                    overallBestStreak = 0,
                    allTimeCompletions = 0,
                    heatmapDays = emptyList(),
                    categoryStats = emptyList(),
                    dayOfWeekStats = emptyList()
                )
            )
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectFilter(filter: HabitFilterType) {
        _selectedFilter.value = filter
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val completed = repository.toggleHabitCompletion(habit, _selectedDate.value)
            if (completed) {
                _userMessage.value = "Great job! \"${habit.title}\" checked off! 🎉"
            }
        }
    }

    fun updateProgressValue(habit: Habit, newValue: Double) {
        viewModelScope.launch {
            repository.updateHabitValue(habit, _selectedDate.value, newValue)
        }
    }

    fun openAddHabit() {
        _editingHabit.value = null
        _isAddEditOpen.value = true
    }

    fun openEditHabit(habit: Habit) {
        _editingHabit.value = habit
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _editingHabit.value = null
    }

    fun openHabitDetail(item: HabitWithStatus) {
        _selectedHabitDetail.value = item
    }

    fun closeHabitDetail() {
        _selectedHabitDetail.value = null
    }

    fun openRemindersSheet() {
        _isRemindersSheetOpen.value = true
    }

    fun closeRemindersSheet() {
        _isRemindersSheetOpen.value = false
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            if (habit.id == 0L) {
                repository.insertHabit(habit)
                _userMessage.value = "Habit \"${habit.title}\" created!"
            } else {
                repository.updateHabit(habit)
                _userMessage.value = "Habit \"${habit.title}\" updated!"
            }
            closeAddEdit()
            closeHabitDetail()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _userMessage.value = "\"${habit.title}\" deleted"
            closeHabitDetail()
            closeAddEdit()
        }
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _hasNotificationPermission.value = granted
    }

    fun saveReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        _remindersEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
        val context = getApplication<Application>()
        if (enabled) {
            NotificationHelper.scheduleDailyReminder(context, hour, minute)
            _userMessage.value = "Daily reminder set for %02d:%02d".format(hour, minute)
        } else {
            NotificationHelper.cancelDailyReminder(context)
            _userMessage.value = "Daily reminders disabled"
        }
    }

    fun triggerTestNotification() {
        val context = getApplication<Application>()
        NotificationHelper.showInstantTestNotification(context)
        _userMessage.value = "Test notification sent! Check your notification bar."
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDemoData()
            _userMessage.value = "Demo habits and 14-day history re-seeded!"
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }
}
