package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitTargetType
import com.example.data.repository.HabitWithStatus
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.viewmodel.HabitFilterType
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: HabitViewModel,
    habits: List<HabitWithStatus>,
    selectedDate: LocalDate,
    selectedFilter: HabitFilterType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isToday = selectedDate == LocalDate.now()

    val totalHabits = habits.size
    val completedHabits = habits.count { it.isCompletedToday }
    val progressFraction = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(),
        label = "progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("today_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal Date Navigation Strip
            item {
                WeekDateStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onTodayClicked = { viewModel.selectDate(LocalDate.now()) }
                )
            }

            // Hero Daily Progress Card
            item {
                DailyProgressHeroCard(
                    isToday = isToday,
                    selectedDate = selectedDate,
                    totalHabits = totalHabits,
                    completedHabits = completedHabits,
                    animatedProgress = animatedProgress,
                    onRemindersClick = { viewModel.openRemindersSheet() }
                )
            }

            // Filter Chips
            item {
                FilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) }
                )
            }

            // Habits Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isToday) "Today's Habits" else "Habits for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$completedHabits of $totalHabits done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Empty State or Habit Items
            if (habits.isEmpty()) {
                item {
                    EmptyHabitsCard(
                        filter = selectedFilter,
                        onAddHabit = { viewModel.openAddHabit() }
                    )
                }
            } else {
                items(habits, key = { it.habit.id }) { item ->
                    HabitCardItem(
                        item = item,
                        onToggle = {
                            vibrateHaptic(context)
                            viewModel.toggleHabit(item.habit)
                        },
                        onQuickIncrement = { increment ->
                            val current = item.currentValueToday
                            val updated = current + increment
                            vibrateHaptic(context)
                            viewModel.updateProgressValue(item.habit, updated)
                        },
                        onClick = {
                            viewModel.openHabitDetail(item)
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add New Habit
        FloatingActionButton(
            onClick = { viewModel.openAddHabit() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_habit_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add New Habit",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun WeekDateStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onTodayClicked: () -> Unit
) {
    val today = LocalDate.now()
    val scrollState = rememberScrollState()

    // 14-day window: 7 days before, 6 days after
    val days = (-7..6).map { today.plusDays(it.toLong()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedDate != today) {
                    Surface(
                        onClick = onTodayClicked,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("jump_to_today_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { date ->
                    val isSelected = date == selectedDate
                    val isDateToday = date == today
                    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
                    val dayNumber = date.dayOfMonth.toString()

                    val containerColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isDateToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        label = "day_container"
                    )

                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isDateToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(containerColor)
                            .clickable { onDateSelected(date) }
                            .padding(vertical = 8.dp)
                            .testTag("date_pill_${date}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dayNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected || isDateToday) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = textColor
                        )
                        if (isDateToday && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyProgressHeroCard(
    isToday: Boolean,
    selectedDate: LocalDate,
    totalHabits: Int,
    completedHabits: Int,
    animatedProgress: Float,
    onRemindersClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_progress_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isToday) "Daily Momentum" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE Progress")),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val motivationalText = when {
                        totalHabits == 0 -> "No habits scheduled. Add one to start!"
                        completedHabits == totalHabits -> "All goals checked off! Outstanding job! 🌟"
                        completedHabits > 0 -> "Keep going! You're making steady progress today."
                        else -> "Ready to take on the day? Check off your first habit!"
                    }
                    Text(
                        text = motivationalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                IconButton(
                    onClick = onRemindersClick,
                    modifier = Modifier.testTag("open_reminders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Daily Reminders Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage & Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}% Done",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$completedHabits / $totalHabits habits",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: HabitFilterType,
    onFilterSelected: (HabitFilterType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HabitFilterType.values()) { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun HabitCardItem(
    item: HabitWithStatus,
    onToggle: () -> Unit,
    onQuickIncrement: (Double) -> Unit,
    onClick: () -> Unit
) {
    val habit = item.habit
    val isCompleted = item.isCompletedToday
    val isNumeric = habit.targetType == HabitTargetType.NUMERIC

    val cardBorder = if (isCompleted) {
        BorderStrokeModifier(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Pill
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parseHexColor(habit.colorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = habit.category.getIcon(),
                        contentDescription = habit.category.displayName,
                        tint = parseHexColor(habit.colorHex),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Category details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = habit.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Streak Badge
                        if (item.currentStreak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${item.currentStreak}d",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Interactive Checkmark button
                Surface(
                    onClick = onToggle,
                    shape = CircleShape,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("check_habit_${habit.id}"),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    }
                }
            }

            // Numeric Target Progress Bar and Quick Increment
            if (isNumeric) {
                Spacer(modifier = Modifier.height(12.dp))
                val target = habit.targetValue.coerceAtLeast(1.0)
                val current = item.currentValueToday
                val progress = (current / target).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${current.toInt()} / ${target.toInt()} ${habit.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick increments (+1, +250, etc.)
                    val step = when {
                        target >= 1000 -> 250.0
                        target >= 50 -> 10.0
                        target >= 10 -> 2.0
                        else -> 1.0
                    }

                    Surface(
                        onClick = { onQuickIncrement(step) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("increment_habit_${habit.id}")
                    ) {
                        Text(
                            text = "+${step.toInt()} ${habit.unit}".trim(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = parseHexColor(habit.colorHex),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun EmptyHabitsCard(
    filter: HabitFilterType,
    onAddHabit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (filter == HabitFilterType.ALL) "No habits scheduled today" else "No habits found for ${filter.label}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Build a consistent routine by adding habits for fitness, reading, mindfulness, or work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddHabit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create a Habit")
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Emerald500
    }
}

fun vibrateHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        }
    } catch (_: Exception) {
        // Ignore haptic failure
    }
}

@Composable
fun BorderStrokeModifier(color: Color): Modifier {
    return Modifier.border(1.dp, color, RoundedCornerShape(18.dp))
}
