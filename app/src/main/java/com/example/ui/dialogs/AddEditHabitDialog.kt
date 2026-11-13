package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.HabitTargetType
import com.example.data.model.HabitTimeOfDay
import com.example.ui.screens.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitBottomSheet(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.HEALTH) }
    var targetType by remember { mutableStateOf(habitToEdit?.targetType ?: HabitTargetType.BOOLEAN) }
    var targetValueText by remember { mutableStateOf(habitToEdit?.targetValue?.toInt()?.toString() ?: "1") }
    var unitText by remember { mutableStateOf(habitToEdit?.unit ?: "") }
    var timeOfDay by remember { mutableStateOf(habitToEdit?.timeOfDay ?: HabitTimeOfDay.ANYTIME.name) }
    var reminderEnabled by remember { mutableStateOf(habitToEdit?.reminderEnabled ?: false) }
    var reminderHour by remember { mutableStateOf(habitToEdit?.reminderHour ?: 20) }
    var reminderMinute by remember { mutableStateOf(habitToEdit?.reminderMinute ?: 0) }
    var selectedColorHex by remember { mutableStateOf(habitToEdit?.colorHex ?: "#10B981") }

    // Frequency days 1..7
    val initialDays = habitToEdit?.frequencyDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
        ?: listOf(1, 2, 3, 4, 5, 6, 7)
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(initialDays) } }

    var titleError by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        "#10B981", "#6366F1", "#8B5CF6", "#0EA5E9",
        "#F59E0B", "#F43F5E", "#14B8A6", "#EC4899"
    )

    val dayNames = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
        5 to "Fri", 6 to "Sat", 7 to "Sun"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_edit_habit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (habitToEdit == null) "New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Habit Title *") },
                placeholder = { Text("e.g. Morning 20m Meditation") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title cannot be empty") }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Motivation or Note (Optional)") },
                placeholder = { Text("Why this habit matters to you") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_desc_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(HabitCategory.values()) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = category
                            selectedColorHex = category.colorHex
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = category.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(category.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = parseHexColor(category.colorHex).copy(alpha = 0.2f),
                            selectedLabelColor = parseHexColor(category.colorHex)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Goal Target Type: Boolean vs Numeric
            Text(
                text = "Goal Target Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { targetType = HabitTargetType.BOOLEAN }
                        .testTag("target_type_boolean"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (targetType == HabitTargetType.BOOLEAN)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (targetType == HabitTargetType.BOOLEAN)
                        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Yes / No Check",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Simple done / undone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { targetType = HabitTargetType.NUMERIC }
                        .testTag("target_type_numeric"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (targetType == HabitTargetType.NUMERIC)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (targetType == HabitTargetType.NUMERIC)
                        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Target Amount",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "e.g. 2000 ml, 20 pages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (targetType == HabitTargetType.NUMERIC) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = { targetValueText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Amount") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_value_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Unit") },
                        placeholder = { Text("pages, ml, min") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_unit_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Frequency: Days of Week
            Text(
                text = "Repeat Days",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEach { (dayNum, label) ->
                    val isDaySelected = selectedDays.contains(dayNum)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDaySelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (isDaySelected) {
                                    if (selectedDays.size > 1) selectedDays.remove(dayNum)
                                } else {
                                    selectedDays.add(dayNum)
                                }
                            }
                            .testTag("day_toggle_$dayNum"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.take(1),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Daily Reminder Toggle & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (reminderEnabled) "Notify at %02d:%02d".format(reminderHour, reminderMinute)
                            else "Reminders off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it },
                    modifier = Modifier.testTag("reminder_switch")
                )
            }

            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick time presets
                    listOf(
                        Triple(7, 30, "7:30 AM"),
                        Triple(12, 0, "12:00 PM"),
                        Triple(18, 0, "6:00 PM"),
                        Triple(20, 0, "8:00 PM"),
                        Triple(21, 30, "9:30 PM")
                    ).forEach { (h, m, label) ->
                        val isPicked = reminderHour == h && reminderMinute == m
                        FilterChip(
                            selected = isPicked,
                            onClick = {
                                reminderHour = h
                                reminderMinute = m
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Color Palette Selector
            Text(
                text = "Accent Color",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorOptions.forEach { hex ->
                    val color = parseHexColor(hex)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_habit_button")
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        val numericTarget = targetValueText.toDoubleOrNull() ?: 1.0
                        val freqString = selectedDays.sorted().joinToString(",")

                        val newHabit = Habit(
                            id = habitToEdit?.id ?: 0,
                            title = title.trim(),
                            description = description.trim(),
                            category = selectedCategory,
                            targetType = targetType,
                            targetValue = if (targetType == HabitTargetType.NUMERIC) numericTarget else 1.0,
                            unit = if (targetType == HabitTargetType.NUMERIC) unitText.trim() else "",
                            frequencyDays = freqString,
                            timeOfDay = timeOfDay,
                            reminderEnabled = reminderEnabled,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            colorHex = selectedColorHex,
                            createdAt = habitToEdit?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(newHabit)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("save_habit_button")
                ) {
                    Text(if (habitToEdit == null) "Create Habit" else "Save Changes")
                }
            }
        }
    }
}
