package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.domain.model.RegularReminder
import java.util.UUID

private val DAY_LABELS = listOf(
    1 to "Пн",
    2 to "Вт",
    3 to "Ср",
    4 to "Чт",
    5 to "Пт",
    6 to "Сб",
    7 to "Вс"
)

private fun formatDaysBadge(daysOfWeek: List<Int>): String {
    if (daysOfWeek.isEmpty() || daysOfWeek.size >= 7) return "каждый день"
    val map = DAY_LABELS.toMap()
    return daysOfWeek.sorted().mapNotNull { map[it] }.joinToString(", ")
}

private fun formatIntervalLabel(minutes: Int): String {
    val hrs = minutes / 60
    val rem = minutes % 60
    return when {
        hrs > 0 && rem == 0 -> "$hrs ч"
        hrs > 0 -> "$hrs ч $rem мин"
        else -> "$minutes мин"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularRemindersSheet(
    reminders: List<RegularReminder>,
    onUpdateReminders: (List<RegularReminder>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    val intervalOptions = remember { (15..240 step 5).toList() }

    var editingReminder by remember { mutableStateOf<RegularReminder?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var editIsIntervalMode by remember { mutableStateOf(true) }
    var editIntervalMinutes by remember { mutableIntStateOf(60) }
    var editStartTime by remember { mutableStateOf("09:00") }
    var editEndTime by remember { mutableStateOf("21:00") }
    var editExactTime by remember { mutableStateOf("09:00") }
    var editDaysOfWeek by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun openCreateEditor() {
        editingReminder = null
        isCreatingNew = true
        editTitle = ""
        editIsIntervalMode = true
        editIntervalMinutes = 60
        editStartTime = "09:00"
        editEndTime = "21:00"
        editExactTime = "09:00"
        editDaysOfWeek = emptyList()
    }

    fun openEditEditor(reminder: RegularReminder) {
        isCreatingNew = false
        editingReminder = reminder
        editTitle = reminder.title
        editIsIntervalMode = reminder.isIntervalMode
        val snapped = ((reminder.intervalMinutes.coerceIn(15, 240) + 2) / 5) * 5
        editIntervalMinutes = snapped.coerceIn(15, 240)
        editStartTime = reminder.startTime
        editEndTime = reminder.endTime
        editExactTime = reminder.exactTime
        editDaysOfWeek = reminder.daysOfWeek.sorted()
    }

    fun closeEditor() {
        editingReminder = null
        isCreatingNew = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Регулярные напоминания",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(IosBlue.copy(alpha = 0.14f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                openCreateEditor()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = IosBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Добавить",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosBlue
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = iosColors.textSecondary
                        )
                    }
                }
            }

            // Inline Create / Edit Card
            AnimatedVisibility(visible = isCreatingNew || editingReminder != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(iosColors.cardSecondary)
                        .border(BorderStroke(1.dp, IosBlue.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    IosSegmentedControl(
                        items = listOf("Интервальное", "Фиксированное время"),
                        selectedIndex = if (editIsIntervalMode) 0 else 1,
                        onItemSelected = { idx -> editIsIntervalMode = idx == 0 }
                    )

                    if (editIsIntervalMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(iosColors.card)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Интервал",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textSecondary
                                )
                                Text(
                                    text = formatIntervalLabel(editIntervalMinutes),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosBlue
                                )
                            }
                            val selectedIdx = intervalOptions.indexOf(editIntervalMinutes).coerceAtLeast(0)
                            IosWheelPicker(
                                items = intervalOptions,
                                selectedIndex = selectedIdx,
                                onItemSelected = { _, mins -> editIntervalMinutes = mins },
                                itemLabel = { mins -> "Каждые ${formatIntervalLabel(mins)}" },
                                visibleItemsCount = 3,
                                itemHeight = 36.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IosTimePickerButton(
                                label = "С",
                                timeString = editStartTime,
                                onTimeSelected = { editStartTime = it },
                                modifier = Modifier.weight(1f)
                            )
                            IosTimePickerButton(
                                label = "До",
                                timeString = editEndTime,
                                onTimeSelected = { editEndTime = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        IosTimePickerButton(
                            label = "Время",
                            timeString = editExactTime,
                            onTimeSelected = { editExactTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Days of week chips (empty = every day)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Дни недели",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textSecondary
                            )
                            Text(
                                text = formatDaysBadge(editDaysOfWeek),
                                fontSize = 11.5.sp,
                                color = if (editDaysOfWeek.isEmpty()) iosColors.textSecondary else IosBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DAY_LABELS.forEach { (day, label) ->
                                val isSelected = day in editDaysOfWeek
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IosBlue else iosColors.card)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            editDaysOfWeek = if (isSelected) {
                                                editDaysOfWeek - day
                                            } else {
                                                (editDaysOfWeek + day).sorted()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else iosColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingReminder != null) {
                            IconButton(
                                onClick = {
                                    val targetId = editingReminder!!.id
                                    onUpdateReminders(reminders.filterNot { it.id == targetId })
                                    closeEditor()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Удалить",
                                    tint = iosColors.textSecondary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = ::closeEditor) {
                                Text("Отмена", color = iosColors.textSecondary)
                            }
                            Button(
                                onClick = {
                                    val cleanTitle = editTitle.trim()
                                    if (cleanTitle.isNotEmpty()) {
                                        val finalInterval = editIntervalMinutes.coerceIn(15, 240)
                                        if (isCreatingNew) {
                                            val created = RegularReminder(
                                                id = UUID.randomUUID().toString(),
                                                title = cleanTitle,
                                                isEnabled = true,
                                                isIntervalMode = editIsIntervalMode,
                                                intervalMinutes = finalInterval,
                                                startTime = editStartTime,
                                                endTime = editEndTime,
                                                exactTime = editExactTime,
                                                daysOfWeek = editDaysOfWeek
                                            )
                                            onUpdateReminders(reminders + created)
                                        } else if (editingReminder != null) {
                                            val targetId = editingReminder!!.id
                                            val updated = reminders.map { item ->
                                                if (item.id == targetId) {
                                                    item.copy(
                                                        title = cleanTitle,
                                                        isIntervalMode = editIsIntervalMode,
                                                        intervalMinutes = finalInterval,
                                                        startTime = editStartTime,
                                                        endTime = editEndTime,
                                                        exactTime = editExactTime,
                                                        daysOfWeek = editDaysOfWeek
                                                    )
                                                } else item
                                            }
                                            onUpdateReminders(updated)
                                        }
                                        closeEditor()
                                    }
                                },
                                enabled = editTitle.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = iosPrimaryButtonColors(IosBlue)
                            ) {
                                Text("Сохранить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Empty State or Configured Reminders List
            if (reminders.isEmpty() && !isCreatingNew) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(iosColors.cardSecondary)
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = iosColors.textSecondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "Нет напоминаний",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEach { reminder ->
                        val scheduleSummary = if (reminder.isIntervalMode) {
                            "Каждые ${formatIntervalLabel(reminder.intervalMinutes)} • ${reminder.startTime}–${reminder.endTime}"
                        } else {
                            "В ${reminder.exactTime}"
                        }
                        val daysSuffix = formatDaysBadge(reminder.daysOfWeek)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (reminder.isEnabled) IosBlue.copy(alpha = 0.08f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (reminder.isEnabled) 1.dp else 0.dp,
                                        color = if (reminder.isEnabled) IosBlue.copy(alpha = 0.35f) else Color.Transparent
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { openEditEditor(reminder) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = reminder.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (reminder.isEnabled) iosColors.textPrimary else iosColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$scheduleSummary • $daysSuffix",
                                    fontSize = 12.sp,
                                    color = if (reminder.isEnabled) IosBlue else iosColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = reminder.isEnabled,
                                    onCheckedChange = { enabled ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val updated = reminders.map {
                                            if (it.id == reminder.id) it.copy(isEnabled = enabled) else it
                                        }
                                        onUpdateReminders(updated)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IosBlue,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = if (iosColors.isDark) Color(0xFF48484C) else Color(0xFFE5E5EA),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (editingReminder?.id == reminder.id) closeEditor()
                                        onUpdateReminders(reminders.filterNot { it.id == reminder.id })
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Удалить",
                                        tint = iosColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
