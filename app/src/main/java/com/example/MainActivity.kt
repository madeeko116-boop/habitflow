package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.NotificationHelper
import com.example.ui.dialogs.AddEditHabitBottomSheet
import com.example.ui.dialogs.HabitDetailBottomSheet
import com.example.ui.dialogs.RemindersBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.HabitFlowTheme
import com.example.ui.viewmodel.HabitViewModel

enum class MainTab {
    TODAY,
    ANALYTICS
}

class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)

        setContent {
            HabitFlowTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: HabitViewModel) {
    var currentTab by remember { mutableStateOf(MainTab.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val habitsWithStatus by viewModel.habitsWithStatus.collectAsStateWithLifecycle()
    val filteredHabits by viewModel.filteredHabits.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val overallAnalytics by viewModel.overallAnalytics.collectAsStateWithLifecycle()

    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingHabit by viewModel.editingHabit.collectAsStateWithLifecycle()
    val selectedHabitDetail by viewModel.selectedHabitDetail.collectAsStateWithLifecycle()
    val isRemindersSheetOpen by viewModel.isRemindersSheetOpen.collectAsStateWithLifecycle()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()

    // Request notification permission on first launch if Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.setNotificationPermissionGranted(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_title_row")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HabitFlow",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openRemindersSheet() },
                        modifier = Modifier.testTag("top_bar_reminders_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (remindersEnabled) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Daily Reminders",
                                tint = if (remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openAddHabit() },
                        modifier = Modifier.testTag("top_bar_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Habit",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.TODAY,
                    onClick = { currentTab = MainTab.TODAY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.TODAY) Icons.Default.Today else Icons.Outlined.Today,
                            contentDescription = "Today"
                        )
                    },
                    label = { Text("Today") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_today_tab")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.ANALYTICS,
                    onClick = { currentTab = MainTab.ANALYTICS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.ANALYTICS) Icons.Default.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_analytics_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_switch") { tab ->
                when (tab) {
                    MainTab.TODAY -> TodayScreen(
                        viewModel = viewModel,
                        habits = filteredHabits,
                        selectedDate = selectedDate,
                        selectedFilter = selectedFilter
                    )
                    MainTab.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel,
                        analytics = overallAnalytics,
                        habitsWithStatus = habitsWithStatus
                    )
                }
            }
        }
    }

    // Add / Edit Habit Sheet
    if (isAddEditOpen) {
        AddEditHabitBottomSheet(
            habitToEdit = editingHabit,
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { habit -> viewModel.saveHabit(habit) }
        )
    }

    // Habit Detail Sheet
    selectedHabitDetail?.let { item ->
        HabitDetailBottomSheet(
            item = item,
            onDismiss = { viewModel.closeHabitDetail() },
            onEdit = { viewModel.openEditHabit(item.habit) },
            onDelete = { viewModel.deleteHabit(item.habit) },
            onUpdateValue = { newValue ->
                viewModel.updateProgressValue(item.habit, newValue)
            }
        )
    }

    // Reminders & Notification Settings Sheet
    if (isRemindersSheetOpen) {
        RemindersBottomSheet(
            viewModel = viewModel,
            hasNotificationPermission = hasNotificationPermission,
            remindersEnabled = remindersEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            onDismiss = { viewModel.closeRemindersSheet() }
        )
    }
}
