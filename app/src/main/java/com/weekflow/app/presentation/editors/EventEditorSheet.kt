package com.weekflow.app.presentation.editors

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.ColorPalettePicker
import com.weekflow.app.core.designsystem.components.IosCalendarDatePickerDialog
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.components.IosTimePickerButton
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.EventRepeatType
import com.weekflow.app.domain.model.SchedulePreset
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    initialEvent: CalendarEvent?,
    defaultDateIso: String,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    presetId: String = SchedulePreset.DEFAULT_ID,
    defaultStartTime: String? = null,
    defaultEndTime: String? = null,
    defaultIsAllDay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    var title by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.title ?: "")
    }
    var dateIso by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.dateIso ?: defaultDateIso)
    }
    var isAllDay by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.isAllDay ?: defaultIsAllDay)
    }
    var startTime by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.startTime ?: defaultStartTime ?: "10:00")
    }
    var endTime by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.endTime ?: defaultEndTime ?: "11:30")
    }
    var repeatType by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.repeatType ?: EventRepeatType.NONE)
    }
    var repeatDays by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        val cleaned = initialEvent?.repeatDays?.filter { it in 1..7 }?.distinct()?.sorted().orEmpty()
        val initial = if ((initialEvent?.repeatType ?: EventRepeatType.NONE) == EventRepeatType.WEEKLY && cleaned.isEmpty()) {
            listOf(
                runCatching { LocalDate.parse(initialEvent?.dateIso ?: defaultDateIso).dayOfWeek.value }
                    .getOrDefault(LocalDate.now().dayOfWeek.value)
            )
        } else {
            cleaned
        }
        mutableStateOf(initial)
    }
    var repeatEndDateIso by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.repeatEndDateIso?.trim()?.takeIf { it.isNotEmpty() })
    }
    var showRepeatEndDatePicker by remember { mutableStateOf(false) }
    var colorHex by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.colorHex ?: "#007AFF")
    }
    var location by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.location ?: "")
    }
    var description by remember(initialEvent?.id, defaultDateIso, defaultStartTime, defaultEndTime, defaultIsAllDay) {
        mutableStateOf(initialEvent?.description ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEvent == null) "Новое событие" else "Редактировать событие",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название события") },
                placeholder = { Text("например, Экзамен по физике") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date (Calendar Picker)
            IosDatePickerButton(
                label = "Дата проведения",
                dateIso = dateIso,
                onDateSelected = { dateIso = it },
                modifier = Modifier.fillMaxWidth()
            )

            // All Day switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Весь день", fontSize = 15.sp, color = iosColors.textPrimary)
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it },
                    colors = iosSwitchColors()
                )
            }

            // Times (if not all day) — Wheel Time Pickers
            AnimatedVisibility(visible = !isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosTimePickerButton(
                        label = "Начало",
                        timeString = startTime,
                        onTimeSelected = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    IosTimePickerButton(
                        label = "Конец",
                        timeString = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recurrence ("Повтор")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = if (repeatType != EventRepeatType.NONE) IosBlue else iosColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Повтор",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EventRepeatType.entries) { type ->
                    val isSelected = repeatType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) IosBlue else iosColors.cardSecondary)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) IosBlue else iosColors.separator.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                repeatType = type
                                if (type == EventRepeatType.WEEKLY && repeatDays.isEmpty()) {
                                    val defaultDay = runCatching {
                                        LocalDate.parse(dateIso.trim()).dayOfWeek.value
                                    }.getOrDefault(LocalDate.now().dayOfWeek.value)
                                    repeatDays = listOf(defaultDay)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else iosColors.textPrimary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = repeatType == EventRepeatType.WEEKLY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт",
                        5 to "Пт", 6 to "Сб", 7 to "Вс"
                    ).forEach { (day, label) ->
                        val isSelected = day in repeatDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) IosBlue else iosColors.cardSecondary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    repeatDays = if (isSelected) {
                                        if (repeatDays.size > 1) repeatDays - day else repeatDays
                                    } else {
                                        (repeatDays + day).distinct().sorted()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else iosColors.textPrimary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = repeatType != EventRepeatType.NONE) {
                val endPickerSurface = if (iosColors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                val endPickerBorder = if (iosColors.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                val formattedEndText = remember(repeatEndDateIso) {
                    repeatEndDateIso?.let { iso ->
                        runCatching { LocalDate.parse(iso) }.getOrNull()?.let { parsed ->
                            "${DateTimeUtils.formatDateWithDay(parsed)} ${parsed.year}"
                        }
                    } ?: "Бессрочно"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(endPickerSurface)
                        .border(1.dp, endPickerBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showRepeatEndDatePicker = true
                        }
                        .padding(start = 12.dp, end = if (repeatEndDateIso != null) 4.dp else 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Повторять до",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = if (repeatEndDateIso != null) IosBlue else iosColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formattedEndText,
                                fontSize = 14.sp,
                                fontWeight = if (repeatEndDateIso != null) FontWeight.Bold else FontWeight.Medium,
                                color = if (repeatEndDateIso != null) iosColors.textPrimary else iosColors.textSecondary
                            )
                        }
                    }
                    if (repeatEndDateIso != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                repeatEndDateIso = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Убрать дату окончания",
                                tint = iosColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (showRepeatEndDatePicker) {
                IosCalendarDatePickerDialog(
                    title = "Повторять до",
                    initialDateIso = repeatEndDateIso ?: dateIso,
                    onDismiss = { showRepeatEndDatePicker = false },
                    onConfirm = { selectedIso ->
                        val startD = runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull()
                        val endD = runCatching { LocalDate.parse(selectedIso) }.getOrNull()
                        repeatEndDateIso = if (startD != null && endD != null && endD.isBefore(startD)) {
                            dateIso
                        } else {
                            selectedIso
                        }
                        showRepeatEndDatePicker = false
                    }
                )
            }

            // Color Picker
            Text(
                text = "Цвет маркера",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            ColorPalettePicker(
                selectedColorHex = colorHex,
                onColorSelected = { colorHex = it }
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Место проведения / Аудитория") },
                placeholder = { Text("например, Ауд. 301") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание / Заметки") },
                placeholder = { Text("Дополнительные детали...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Duplicate button if editing
            if (initialEvent != null) {
                OutlinedButton(
                    onClick = {
                        val duplicated = CalendarEvent(
                            id = UUID.randomUUID().toString(),
                            title = "${title.trim()} (копия)",
                            dateIso = try { LocalDate.parse(dateIso).plusDays(1).toString() } catch (_: Exception) { dateIso },
                            startTime = if (isAllDay) null else startTime.trim(),
                            endTime = if (isAllDay) null else endTime.trim(),
                            isAllDay = isAllDay,
                            colorHex = colorHex,
                            location = location.trim(),
                            description = description.trim(),
                            category = ActivityCategory.PERSONAL,
                            presetId = initialEvent.presetId.ifBlank { presetId },
                            repeatType = EventRepeatType.NONE,
                            repeatDays = emptyList(),
                            repeatEndDateIso = null
                        )
                        onSave(duplicated)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Дублировать на следующий день")
                }
            }

            // Save and Delete
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val persistedRepeatDays = if (repeatType == EventRepeatType.WEEKLY) {
                            repeatDays.filter { it in 1..7 }.distinct().sorted().ifEmpty {
                                listOf(
                                    runCatching { LocalDate.parse(dateIso.trim()).dayOfWeek.value }
                                        .getOrDefault(LocalDate.now().dayOfWeek.value)
                                )
                            }
                        } else {
                            emptyList()
                        }
                        val persistedRepeatEnd = if (repeatType != EventRepeatType.NONE) {
                            repeatEndDateIso?.trim()?.takeIf { it.isNotEmpty() }?.let { rawEnd ->
                                val startD = runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull()
                                val endD = runCatching { LocalDate.parse(rawEnd) }.getOrNull()
                                if (startD != null && endD != null && endD.isBefore(startD)) dateIso.trim() else rawEnd
                            }
                        } else {
                            null
                        }
                        val event = CalendarEvent(
                            id = initialEvent?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            dateIso = dateIso.trim(),
                            startTime = if (isAllDay) null else startTime.trim(),
                            endTime = if (isAllDay) null else endTime.trim(),
                            isAllDay = isAllDay,
                            colorHex = colorHex,
                            location = location.trim(),
                            description = description.trim(),
                            category = ActivityCategory.PERSONAL,
                            presetId = initialEvent?.presetId?.ifBlank { presetId } ?: presetId,
                            repeatType = repeatType,
                            repeatDays = persistedRepeatDays,
                            repeatEndDateIso = persistedRepeatEnd
                        )
                        onSave(event)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                colors = iosPrimaryButtonColors(IosBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сохранить событие", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (initialEvent != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete(initialEvent.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить событие")
                }
            }
        }
    }
}
