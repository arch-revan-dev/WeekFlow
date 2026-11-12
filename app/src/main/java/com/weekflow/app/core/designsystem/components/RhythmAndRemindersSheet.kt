package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosPurple
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.domain.model.QuickReminder
import com.weekflow.app.domain.model.RhythmReminder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RhythmAndRemindersSheet(
    rhythmReminders: List<RhythmReminder>,
    quickReminders: List<QuickReminder>,
    onUpdateRhythmReminders: (List<RhythmReminder>) -> Unit,
    onUpdateQuickReminders: (List<QuickReminder>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    // Quick One-Shot Reminder state
    var quickTitle by remember { mutableStateOf("Попить воды") }
    var quickEmoji by remember { mutableStateOf("💧") }
    var showExactTimeDialog by remember { mutableStateOf(false) }

    // Editing or Adding Interval Rhythm Reminder state
    var editingRhythm by remember { mutableStateOf<RhythmReminder?>(null) }
    var isCreatingNewRhythm by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editSubtitle by remember { mutableStateOf("") }
    var editEmoji by remember { mutableStateOf("💧") }
    var editIntervalMin by remember { mutableIntStateOf(60) }
    var editStartTime by remember { mutableStateOf("09:00") }
    var editEndTime by remember { mutableStateOf("21:30") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val intervalOptions = remember { listOf(15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360) }
    val emojiOptions = remember { listOf("💧", "🧘", "👀", "🎯", "🌬️", "💊", "🍎", "🚶", "📞", "☕", "🦷", "📖") }

    val quickChips = remember {
        listOf(
            "💧" to "Попить воды",
            "🧘" to "Размять спину и шею",
            "📞" to "Перезвонить",
            "💊" to "Принять витамины",
            "🍲" to "Проверить плиту / таймер",
            "📨" to "Проверить отчёт / почту"
        )
    }

    fun scheduleQuickByOffsetMinutes(offsetMinutes: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val targetMillis = System.currentTimeMillis() + offsetMinutes * 60_000L
        val targetTime = Instant.ofEpochMilli(targetMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        val item = QuickReminder(
            title = quickTitle.ifBlank { "Напоминание" },
            iconEmoji = quickEmoji,
            triggerAtEpochMillis = targetMillis,
            targetTimeFormatted = "в $targetTime (через $offsetMinutes мин)"
        )
        onUpdateQuickReminders(quickReminders + item)
    }

    if (showExactTimeDialog) {
        IosWheelTimePickerDialog(
            title = "Точное время напоминания",
            initialTime = LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")),
            onDismiss = { showExactTimeDialog = false },
            onConfirm = { chosenTimeStr ->
                showExactTimeDialog = false
                val parsed = runCatching {
                    LocalTime.parse(chosenTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault(LocalTime.now().plusMinutes(30))
                var dt = LocalDateTime.of(LocalDate.now(), parsed)
                if (!dt.isAfter(LocalDateTime.now())) {
                    dt = dt.plusDays(1)
                }
                val targetMillis = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val item = QuickReminder(
                    title = quickTitle.ifBlank { "Напоминание" },
                    iconEmoji = quickEmoji,
                    triggerAtEpochMillis = targetMillis,
                    targetTimeFormatted = "в $chosenTimeStr"
                )
                onUpdateQuickReminders(quickReminders + item)
            }
        )
    }

    if (showStartTimePicker) {
        IosWheelTimePickerDialog(
            title = "Начало активного окна",
            initialTime = editStartTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                editStartTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        IosWheelTimePickerDialog(
            title = "Конец активного окна",
            initialTime = editEndTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                editEndTime = it
                showEndTimePicker = false
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(IosBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Напоминания и ритм дня",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = "Быстрые пинги и регулярные интервалы (вода, разминка)",
                            fontSize = 11.5.sp,
                            color = iosColors.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = iosColors.textSecondary)
                }
            }

            // SECTION 1: Quick One-Shot Reminders ("Быстрое напоминание через 15м / 1ч")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(iosColors.cardSecondary)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚡ БЫСТРОЕ НАПОМИНАНИЕ В ОДИН ТАП",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosBlue
                )

                // Quick presets row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickChips.forEach { (emoji, label) ->
                        val isSelected = quickTitle == label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) IosBlue.copy(alpha = 0.18f) else iosColors.card)
                                .border(
                                    BorderStroke(if (isSelected) 1.2.dp else 0.dp, if (isSelected) IosBlue else Color.Transparent),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    quickEmoji = emoji
                                    quickTitle = label
                                }
                                .padding(horizontal = 8.dp, vertical = 4.5.dp)
                        ) {
                            Text(
                                text = "$emoji $label",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) IosBlue else iosColors.textPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = { quickTitle = it },
                    label = { Text("О чём напомнить?") },
                    leadingIcon = { Text(quickEmoji, fontSize = 18.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 1-Tap Delay Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        15 to "+15 мин",
                        30 to "+30 мин",
                        60 to "+1 час",
                        120 to "+2 часа"
                    ).forEach { (mins, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosBlue)
                                .clickable { scheduleQuickByOffsetMinutes(mins) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosPurple.copy(alpha = 0.16f))
                            .border(BorderStroke(1.dp, IosPurple), RoundedCornerShape(10.dp))
                            .clickable { showExactTimeDialog = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = IosPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Время",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosPurple
                            )
                        }
                    }
                }

                // Active Queued Quick Reminders
                if (quickReminders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Активные быстрые напоминания:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    quickReminders.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(iosColors.card)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(item.iconEmoji, fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textPrimary
                                    )
                                    Text(
                                        text = item.targetTimeFormatted,
                                        fontSize = 11.sp,
                                        color = IosOrange
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    onUpdateQuickReminders(quickReminders.filterNot { it.id == item.id })
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Отменить",
                                    tint = iosColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: Interval Rhythm Reminders ("Регулярные напоминания с интервалом и окном времени")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔄 РЕГУЛЯРНЫЕ НАПОМИНАНИЯ (ИНТЕРВАЛЫ)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosGreen
                    )
                    Text(
                        text = "Нажмите на карточку, чтобы изменить интервал и часы работы",
                        fontSize = 11.sp,
                        color = iosColors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosBlue.copy(alpha = 0.14f))
                        .clickable {
                            editingRhythm = null
                            isCreatingNewRhythm = true
                            editTitle = ""
                            editSubtitle = ""
                            editEmoji = "💧"
                            editIntervalMin = 60
                            editStartTime = "09:00"
                            editEndTime = "21:30"
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ Свой интервал",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue
                    )
                }
            }

            // Inline Editor for Interval Reminder (Interval Wheel + Start/End Time Window)
            AnimatedVisibility(visible = editingRhythm != null || isCreatingNewRhythm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(iosColors.cardSecondary)
                        .border(BorderStroke(1.2.dp, IosBlue.copy(alpha = 0.45f)), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isCreatingNewRhythm) "Новое регулярное напоминание" else "Настройка интервала: ${editingRhythm?.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojiOptions) { emoji ->
                            val isSel = editEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) IosBlue.copy(alpha = 0.22f) else iosColors.card)
                                    .border(BorderStroke(if (isSel) 1.5.dp else 0.dp, if (isSel) IosBlue else Color.Transparent), CircleShape)
                                    .clickable { editEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 17.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название (например, Попить воды)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSubtitle,
                        onValueChange = { editSubtitle = it },
                        label = { Text("Текст уведомления") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Интервал повторения (прокрутите барабан):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(iosColors.card)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        val selectedIdx = intervalOptions.indexOf(editIntervalMin).let { if (it >= 0) it else 3 }
                        IosWheelPicker(
                            items = intervalOptions,
                            selectedIndex = selectedIdx,
                            onItemSelected = { _, mins -> editIntervalMin = mins },
                            itemLabel = { mins ->
                                val hrs = mins / 60
                                val rem = mins % 60
                                when {
                                    hrs > 0 && rem == 0 -> "Каждые $mins мин ($hrs ч)"
                                    hrs > 0 -> "Каждые $mins мин ($hrs ч $rem м)"
                                    else -> "Каждые $mins минут"
                                }
                            },
                            visibleItemsCount = 3,
                            itemHeight = 36.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "Активный промежуток времени (когда присылать уведомления):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iosColors.card)
                                .clickable { showStartTimePicker = true }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Начинать с", fontSize = 11.sp, color = iosColors.textSecondary)
                                Text(editStartTime, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iosColors.card)
                                .clickable { showEndTimePicker = true }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Заканчивать в", fontSize = 11.sp, color = iosColors.textSecondary)
                                Text(editEndTime, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingRhythm != null && editingRhythm!!.id.startsWith("custom_")) {
                            IconButton(
                                onClick = {
                                    val targetId = editingRhythm!!.id
                                    onUpdateRhythmReminders(rhythmReminders.filterNot { it.id == targetId })
                                    editingRhythm = null
                                }
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить", tint = IosOrange)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    editingRhythm = null
                                    isCreatingNewRhythm = false
                                }
                            ) {
                                Text("Отмена", color = iosColors.textSecondary)
                            }
                            Button(
                                onClick = {
                                    if (editTitle.isNotBlank()) {
                                        if (isCreatingNewRhythm) {
                                            val created = RhythmReminder(
                                                id = "custom_${System.currentTimeMillis()}",
                                                title = editTitle.trim(),
                                                subtitle = editSubtitle.trim().ifBlank { "Регулярное напоминание" },
                                                iconEmoji = editEmoji,
                                                intervalMinutes = editIntervalMin,
                                                startTime = editStartTime,
                                                endTime = editEndTime,
                                                isEnabled = true
                                            )
                                            onUpdateRhythmReminders(rhythmReminders + created)
                                        } else if (editingRhythm != null) {
                                            val updated = rhythmReminders.map { item ->
                                                if (item.id == editingRhythm!!.id) {
                                                    item.copy(
                                                        title = editTitle.trim(),
                                                        subtitle = editSubtitle.trim(),
                                                        iconEmoji = editEmoji,
                                                        intervalMinutes = editIntervalMin,
                                                        startTime = editStartTime,
                                                        endTime = editEndTime,
                                                        isEnabled = true
                                                    )
                                                } else item
                                            }
                                            onUpdateRhythmReminders(updated)
                                        }
                                        editingRhythm = null
                                        isCreatingNewRhythm = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Text("Сохранить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List of Interval Rhythm Reminder Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rhythmReminders.forEach { reminder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (reminder.isEnabled) IosGreen.copy(alpha = 0.10f)
                                else iosColors.cardSecondary
                            )
                            .border(
                                BorderStroke(
                                    width = if (reminder.isEnabled) 1.2.dp else 0.dp,
                                    color = if (reminder.isEnabled) IosGreen.copy(alpha = 0.45f) else Color.Transparent
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                isCreatingNewRhythm = false
                                editingRhythm = reminder
                                editTitle = reminder.title
                                editSubtitle = reminder.subtitle
                                editEmoji = reminder.iconEmoji
                                editIntervalMin = reminder.intervalMinutes
                                editStartTime = reminder.startTime
                                editEndTime = reminder.endTime
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iosColors.card),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(reminder.iconEmoji, fontSize = 19.sp)
                            }
                            Column {
                                Text(
                                    text = reminder.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textPrimary
                                )
                                Text(
                                    text = "Каждые ${reminder.intervalMinutes} мин • с ${reminder.startTime} до ${reminder.endTime}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (reminder.isEnabled) IosGreen else iosColors.textSecondary
                                )
                                if (reminder.subtitle.isNotBlank()) {
                                    Text(
                                        text = reminder.subtitle,
                                        fontSize = 11.sp,
                                        color = iosColors.textSecondary
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = reminder.isEnabled,
                            onCheckedChange = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val updated = rhythmReminders.map {
                                    if (it.id == reminder.id) it.copy(isEnabled = enabled) else it
                                }
                                onUpdateRhythmReminders(updated)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IosGreen
                            )
                        )
                    }
                }
            }
        }
    }
}
