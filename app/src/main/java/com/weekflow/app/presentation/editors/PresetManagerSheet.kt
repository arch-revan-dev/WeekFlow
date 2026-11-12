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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.ColorPalettePicker
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import java.time.LocalDate
import java.util.UUID

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
    var formProfile by remember { mutableStateOf(AppProfile.STUDY) }
    var formUseAutoDates by remember { mutableStateOf(false) }
    var formStartDateIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var formEndDateIso by remember { mutableStateOf(LocalDate.now().plusWeeks(2).toString()) }

    fun openCreateForm() {
        editingPreset = null
        isCreatingNew = true
        formName = ""
        formEmoji = "🎓"
        formColorHex = "#007AFF"
        formProfile = AppProfile.STUDY
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
        formProfile = preset.appProfile
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
                Text(
                    text = if (isCreatingNew) {
                        if (editingPreset == null) "Новый пресет" else "Настройка пресета"
                    } else {
                        "Пресеты"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
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
                        val slotCount = allSlots.count { it.presetId == preset.id }
                        val taskCount = allTasks.count { it.presetId == preset.id && !it.isCompleted }
                        var menuExpanded by remember(preset.id) { mutableStateOf(false) }

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
                                            text = "${preset.appProfile.title} • ${pluralizeRu(slotCount, "занятие", "занятия", "занятий")} • ${pluralizeRu(taskCount, "задача", "задачи", "задач")}",
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
                                                    text = "$startFormatted – $endFormatted",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = presetColor
                                                )
                                            }
                                        }
                                    }

                                    // Overflow Menu: Edit, Duplicate, Delete
                                    Box {
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "Действия",
                                                tint = iosColors.textSecondary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            containerColor = iosColors.card
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Редактировать", color = iosColors.textPrimary) },
                                                onClick = {
                                                    menuExpanded = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    openEditForm(preset)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Дублировать", color = iosColors.textPrimary) },
                                                onClick = {
                                                    menuExpanded = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val copyPreset = SchedulePreset(
                                                        id = UUID.randomUUID().toString(),
                                                        name = "${preset.name} (копия)",
                                                        iconEmoji = preset.iconEmoji,
                                                        colorHex = preset.colorHex,
                                                        appProfile = preset.appProfile
                                                    )
                                                    onDuplicatePreset(preset.id, copyPreset)
                                                }
                                            )
                                            if (preset.id != SchedulePreset.DEFAULT_ID && presets.size > 1) {
                                                DropdownMenuItem(
                                                    text = { Text("Удалить", color = IosRed) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onDeletePreset(preset.id)
                                                    }
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
                        openCreateForm()
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
                        text = "Создать пресет",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
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
                        Text(
                            text = "Общие задачи во всех пресетах",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Профиль",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppProfile.entries.forEach { profileOption ->
                        val isChosenProfile = profileOption == formProfile
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isChosenProfile) IosBlue.copy(alpha = 0.14f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isChosenProfile) 1.5.dp else 0.dp,
                                        color = if (isChosenProfile) IosBlue else Color.Transparent
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    formProfile = profileOption
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = profileOption.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isChosenProfile) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChosenProfile) IosBlue else iosColors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isChosenProfile) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = IosBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

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
                            Text(
                                text = "Автопереключение по датам",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = formUseAutoDates,
                                onCheckedChange = { formUseAutoDates = it },
                                colors = iosSwitchColors()
                            )
                        }

                        AnimatedVisibility(visible = formUseAutoDates) {
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

                // Save button
                Button(
                    onClick = {
                        if (formName.isNotBlank()) {
                            val saved = SchedulePreset(
                                id = editingPreset?.id ?: UUID.randomUUID().toString(),
                                name = formName.trim(),
                                iconEmoji = formEmoji,
                                colorHex = formColorHex,
                                appProfile = formProfile,
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

internal fun pluralizeRu(count: Int, one: String, few: String, many: String): String {
    val mod100 = kotlin.math.abs(count) % 100
    val mod10 = mod100 % 10
    return when {
        mod100 in 11..19 -> "$count $many"
        mod10 == 1 -> "$count $one"
        mod10 in 2..4 -> "$count $few"
        else -> "$count $many"
    }
}
