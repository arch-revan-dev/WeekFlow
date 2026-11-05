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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.ColorPalettePicker
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

private data class QuickPresetTemplate(
    val name: String,
    val emoji: String,
    val colorHex: String
)

private val quickTemplates = listOf(
    QuickPresetTemplate("Сессия", "🔥", "#FF3B30"),
    QuickPresetTemplate("Работа", "💼", "#5856D6"),
    QuickPresetTemplate("2 семестр", "📚", "#34C759"),
    QuickPresetTemplate("Спорт / Режим", "🏋️", "#FF9500"),
    QuickPresetTemplate("Каникулы", "🏖️", "#00C7BE")
)

private val presetEmojis = listOf(
    "🎓", "🔥", "💼", "📚", "⚡", "🏋️", "🏖️", "🎯", "💻", "🧪", "🎨", "🌟"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetManagerSheet(
    presets: List<SchedulePreset>,
    activePresetId: String,
    allActivities: List<Activity>,
    allSlots: List<Slot>,
    allTasks: List<Task>,
    showAllPresetsTasks: Boolean,
    onSelectPreset: (SchedulePreset) -> Unit,
    onSavePreset: (SchedulePreset) -> Unit,
    onDuplicatePreset: (sourcePresetId: String, newPreset: SchedulePreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onToggleShowAllPresetsTasks: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    var editingPreset by remember { mutableStateOf<SchedulePreset?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var formName by remember { mutableStateOf("") }
    var formEmoji by remember { mutableStateOf("🎓") }
    var formColorHex by remember { mutableStateOf("#007AFF") }
    var formUseAutoDates by remember { mutableStateOf(false) }
    var formStartDateIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var formEndDateIso by remember { mutableStateOf(LocalDate.now().plusWeeks(2).toString()) }

    fun openCreateForm(template: QuickPresetTemplate? = null) {
        editingPreset = null
        isCreatingNew = true
        formName = template?.name ?: ""
        formEmoji = template?.emoji ?: "🎓"
        formColorHex = template?.colorHex ?: "#007AFF"
        formUseAutoDates = false
        formStartDateIso = LocalDate.now().toString()
        formEndDateIso = LocalDate.now().plusWeeks(2).toString()
    }

    fun openEditForm(preset: SchedulePreset) {
        editingPreset = preset
        isCreatingNew = true
        formName = preset.name
        formEmoji = preset.iconEmoji
        formColorHex = preset.colorHex
        formUseAutoDates = !preset.autoStartDateIso.isNullOrBlank() && !preset.autoEndDateIso.isNullOrBlank()
        formStartDateIso = preset.autoStartDateIso ?: LocalDate.now().toString()
        formEndDateIso = preset.autoEndDateIso ?: LocalDate.now().plusWeeks(2).toString()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCreatingNew) {
                            if (editingPreset == null) "Новый пресет" else "Настройка пресета"
                        } else {
                            "Пресеты расписания и задач"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )
                    if (!isCreatingNew) {
                        Text(
                            text = "Разделяйте расписания (учеба, сессия, работа) и назначайте даты их работы",
                            fontSize = 12.sp,
                            color = iosColors.textSecondary
                        )
                    }
                }
                TextButton(
                    onClick = {
                        if (isCreatingNew) {
                            isCreatingNew = false
                            editingPreset = null
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        text = if (isCreatingNew) "Назад" else "Готово",
                        fontWeight = FontWeight.SemiBold,
                        color = IosBlue
                    )
                }
            }

            if (!isCreatingNew) {
                // List of Existing Presets
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { preset ->
                        val isSelected = preset.id == activePresetId
                        val presetColor = ColorParser.parseHexColor(preset.colorHex)
                        val actCount = allActivities.count { it.presetId == preset.id }
                        val slotCount = allSlots.count { it.presetId == preset.id }
                        val taskCount = allTasks.count { it.presetId == preset.id && !it.isCompleted }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) presetColor.copy(alpha = 0.12f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) presetColor else iosColors.separator.copy(alpha = 0.4f)
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectPreset(preset)
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emoji icon badge
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(presetColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.iconEmoji,
                                            fontSize = 20.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = preset.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = iosColors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(presetColor)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = "Активен",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Предметов: $actCount • Занятий в нед: $slotCount • Задач: $taskCount",
                                            fontSize = 12.sp,
                                            color = iosColors.textSecondary
                                        )

                                        if (!preset.autoStartDateIso.isNullOrBlank() && !preset.autoEndDateIso.isNullOrBlank()) {
                                            val startFormatted = runCatching {
                                                DateTimeUtils.formatShortDayMonth(LocalDate.parse(preset.autoStartDateIso))
                                            }.getOrDefault(preset.autoStartDateIso)
                                            val endFormatted = runCatching {
                                                DateTimeUtils.formatShortDayMonth(LocalDate.parse(preset.autoEndDateIso))
                                            }.getOrDefault(preset.autoEndDateIso)

                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = presetColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Авто по датам: $startFormatted – $endFormatted",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = presetColor
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons: Duplicate, Edit, Delete
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val copyPreset = SchedulePreset(
                                                    id = UUID.randomUUID().toString(),
                                                    name = "${preset.name} (копия)",
                                                    iconEmoji = preset.iconEmoji,
                                                    colorHex = preset.colorHex
                                                )
                                                onDuplicatePreset(preset.id, copyPreset)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Клонировать пресет",
                                                tint = iosColors.textSecondary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                openEditForm(preset)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Редактировать пресет",
                                                tint = IosBlue,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        if (preset.id != SchedulePreset.DEFAULT_ID && presets.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onDeletePreset(preset.id)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Удалить пресет",
                                                    tint = IosRed,
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

                // Create New Preset Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        openCreateForm(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Создать новый пресет",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Quick 1-Tap Templates
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "БЫСТРЫЕ ШАБЛОНЫ В 1 ТАП",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickTemplates) { tpl ->
                            val tplColor = ColorParser.parseHexColor(tpl.colorHex)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tplColor.copy(alpha = 0.14f))
                                    .border(BorderStroke(1.dp, tplColor.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openCreateForm(tpl)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(text = tpl.emoji, fontSize = 13.sp)
                                    Text(
                                        text = "+ ${tpl.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tplColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Global Tasks Option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(iosColors.cardSecondary)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Общие задачи во всех пресетах",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textPrimary
                            )
                            Text(
                                text = "Если выключено, у каждого пресета свой отдельный список задач",
                                fontSize = 11.5.sp,
                                color = iosColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = showAllPresetsTasks,
                            onCheckedChange = { onToggleShowAllPresetsTasks(it) },
                            colors = iosSwitchColors()
                        )
                    }
                }
            } else {
                // Create / Edit Preset Form
                OutlinedTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = { Text("Название пресета") },
                    placeholder = { Text("например, Зимняя сессия / Работа / 2 семестр") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Иконка пресета",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetEmojis) { emoji ->
                        val isChosen = emoji == formEmoji
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isChosen) IosBlue.copy(alpha = 0.2f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isChosen) 2.dp else 0.dp,
                                        color = if (isChosen) IosBlue else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    formEmoji = emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Text(
                    text = "Цвет акцента",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )
                ColorPalettePicker(
                    selectedColorHex = formColorHex,
                    onColorSelected = { formColorHex = it }
                )

                // Auto Date Range section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(iosColors.cardSecondary)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Назначить даты работы пресета",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textPrimary
                                )
                                Text(
                                    text = "В выбранный период это расписание будет включаться автоматически",
                                    fontSize = 11.5.sp,
                                    color = iosColors.textSecondary
                                )
                            }
                            Switch(
                                checked = formUseAutoDates,
                                onCheckedChange = { formUseAutoDates = it },
                                colors = iosSwitchColors()
                            )
                        }

                        AnimatedVisibility(visible = formUseAutoDates) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Quick date range pills
                                val today = LocalDate.now()
                                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Triple("След. неделя", nextMonday, nextMonday.plusDays(6)),
                                        Triple("На 2 недели", today, today.plusWeeks(2)),
                                        Triple("На месяц", today, today.plusMonths(1))
                                    ).forEach { (label, start, end) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IosBlue.copy(alpha = 0.14f))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    formStartDateIso = start.toString()
                                                    formEndDateIso = end.toString()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = IosBlue
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IosDatePickerButton(
                                        label = "С даты",
                                        dateIso = formStartDateIso,
                                        onDateSelected = { formStartDateIso = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IosDatePickerButton(
                                        label = "По дату",
                                        dateIso = formEndDateIso,
                                        onDateSelected = { formEndDateIso = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Save button
                Button(
                    onClick = {
                        if (formName.isNotBlank()) {
                            val saved = SchedulePreset(
                                id = editingPreset?.id ?: UUID.randomUUID().toString(),
                                name = formName.trim(),
                                iconEmoji = formEmoji,
                                colorHex = formColorHex,
                                autoStartDateIso = if (formUseAutoDates) formStartDateIso.trim().takeIf { it.isNotBlank() } else null,
                                autoEndDateIso = if (formUseAutoDates) formEndDateIso.trim().takeIf { it.isNotBlank() } else null,
                                createdAt = editingPreset?.createdAt ?: System.currentTimeMillis()
                            )
                            onSavePreset(saved)
                            if (editingPreset == null) {
                                onSelectPreset(saved)
                            }
                            isCreatingNew = false
                            editingPreset = null
                        }
                    },
                    enabled = formName.isNotBlank(),
                    colors = iosPrimaryButtonColors(IosBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (editingPreset == null) "Создать и включить пресет" else "Сохранить изменения",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
