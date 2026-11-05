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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.runtime.LaunchedEffect
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
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosPurple
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.NextLessonMatch
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.usecase.CalculateNextLessonDateUseCase
import com.weekflow.app.domain.usecase.DetectActiveOrRecentActivityUseCase
import com.weekflow.app.domain.usecase.DetectedActivity
import androidx.compose.ui.text.style.TextDecoration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initialTask: Task?,
    initialActivityId: String?,
    initialSlotId: String?,
    referenceDate: LocalDate,
    activities: List<Activity>,
    allSlots: List<Slot>,
    vacations: List<Vacation>,
    preferences: UserPreferences,
    calculateNextLessonDateUseCase: CalculateNextLessonDateUseCase,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onAddNewActivityRequest: (() -> Unit)? = null,
    detectActiveOrRecentActivityUseCase: DetectActiveOrRecentActivityUseCase? = null,
    presetId: String = SchedulePreset.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current
    val haptic = LocalHapticFeedback.current

    // Auto-detect activity if adding a new task without pre-selected activity
    val initialDetected: DetectedActivity? = remember(initialTask?.id) {
        if (initialTask == null && initialActivityId == null && allSlots.isNotEmpty() && detectActiveOrRecentActivityUseCase != null) {
            detectActiveOrRecentActivityUseCase(
                allSlots = allSlots,
                preferences = preferences,
                vacations = vacations
            )
        } else null
    }

    var detectedActivity by remember(initialTask?.id) { mutableStateOf(initialDetected) }
    var showAutoDetectedBanner by remember(initialTask?.id) { mutableStateOf(initialDetected != null) }
    var isInitialized by remember(initialTask?.id) {
        mutableStateOf(initialTask != null || initialActivityId != null || initialDetected != null)
    }

    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var note by remember(initialTask?.id) { mutableStateOf(initialTask?.note ?: "") }
    var priority by remember(initialTask?.id) {
        mutableStateOf(initialTask?.priority ?: TaskPriority.NONE)
    }
    var subtasks by remember(initialTask?.id) {
        mutableStateOf(initialTask?.subtasks ?: emptyList())
    }
    var newSubtaskText by remember(initialTask?.id) { mutableStateOf("") }

    var selectedActivityId by remember(initialTask?.id, initialActivityId) {
        mutableStateOf(
            initialTask?.linkedActivityId
                ?: initialActivityId
                ?: initialDetected?.slot?.activityTypeId
        )
    }

    LaunchedEffect(initialActivityId, activities.size) {
        if (initialActivityId != null && activities.any { it.id == initialActivityId }) {
            selectedActivityId = initialActivityId
        }
    }
    var linkedSlotId by remember(initialTask?.id) {
        mutableStateOf(
            initialTask?.linkedSlotId
                ?: initialSlotId
                ?: initialDetected?.nextLessonMatch?.linkedSlotId
        )
    }
    var dueDateIso by remember(initialTask?.id) {
        mutableStateOf(
            initialTask?.dueDateIso
                ?: initialDetected?.nextLessonMatch?.dueDate?.toString()
                ?: referenceDate.toString()
        )
    }
    var isManualDateOverride by remember(initialTask?.id) { mutableStateOf(false) }
    var smartLessonMatch by remember(initialTask?.id) {
        mutableStateOf<NextLessonMatch?>(initialDetected?.nextLessonMatch)
    }

    // Smart homework calculation trigger on subject selection
    fun recalculateSmartLesson(activityId: String) {
        val clickedSlot = allSlots.firstOrNull { it.id == initialSlotId && it.activityTypeId == activityId }
        val refTime = when {
            clickedSlot != null -> clickedSlot.parseEndTime()
            referenceDate == LocalDate.now() -> LocalTime.now()
            else -> null
        }
        val match = calculateNextLessonDateUseCase(
            activityId = activityId,
            referenceDate = referenceDate,
            referenceTime = refTime,
            preferences = preferences,
            allSlots = allSlots,
            vacations = vacations
        )
        smartLessonMatch = match
        if (!isManualDateOverride) {
            dueDateIso = match.dueDate.toString()
            linkedSlotId = match.linkedSlotId
        }
    }

    LaunchedEffect(allSlots.size) {
        if (!isInitialized && initialTask == null && initialActivityId == null && detectActiveOrRecentActivityUseCase != null && allSlots.isNotEmpty()) {
            val detected = detectActiveOrRecentActivityUseCase(
                allSlots = allSlots,
                preferences = preferences,
                vacations = vacations
            )
            if (detected != null) {
                detectedActivity = detected
                showAutoDetectedBanner = true
                selectedActivityId = detected.slot.activityTypeId
                linkedSlotId = detected.nextLessonMatch.linkedSlotId
                if (!isManualDateOverride) {
                    dueDateIso = detected.nextLessonMatch.dueDate.toString()
                }
                smartLessonMatch = detected.nextLessonMatch
            }
            isInitialized = true
        }
    }

    LaunchedEffect(selectedActivityId) {
        val actId = selectedActivityId
        if (actId != null && initialTask == null && detectedActivity?.slot?.activityTypeId != actId) {
            recalculateSmartLesson(actId)
        }
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
                    text = if (initialTask == null) "Новая задача" else "Редактировать задачу",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // Auto-detection notification banner
            val detected = detectedActivity
            if (showAutoDetectedBanner && detected != null) {
                val act = activities.firstOrNull { it.id == detected.slot.activityTypeId }
                val actName = act?.name ?: "Предмет"
                val actColor = ColorParser.parseHexColor(act?.colorHex ?: "#007AFF")
                val nextDateStr = DateTimeUtils.formatDateWithDay(detected.nextLessonMatch.dueDate)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(actColor.copy(alpha = 0.12f))
                        .border(BorderStroke(1.5.dp, actColor), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Автовыбор по времени:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = actColor
                            )
                            Text(
                                text = "«$actName» (${if (detected.isOngoingOrUpcoming) "сейчас идет" else "недавно завершилось"}) → записано на следующее занятие ($nextDateStr)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAutoDetectedBanner = false
                                selectedActivityId = null
                                linkedSlotId = null
                                dueDateIso = referenceDate.toString()
                                smartLessonMatch = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Сбросить",
                                tint = iosColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название задачи / задания") },
                placeholder = { Text("например, Прочитать главу 3, выучить формулы") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Priority Selector (TickTick style)
            Text(
                text = "Приоритет задачи",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { p ->
                    val isSelected = p == priority
                    val priorityColor = ColorParser.parseHexColor(p.colorHex)
                    val activeBorderColor = if (p == TaskPriority.NONE) {
                        if (iosColors.isDark) Color(0xFFA0A0A8) else Color(0xFF787880)
                    } else {
                        priorityColor
                    }
                    val activeTextColor = if (p == TaskPriority.NONE) iosColors.textPrimary else priorityColor

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    if (p == TaskPriority.NONE) {
                                        if (iosColors.isDark) Color(0xFF3E3E44) else Color(0xFFE5E5EC)
                                    } else {
                                        priorityColor.copy(alpha = if (iosColors.isDark) 0.22f else 0.16f)
                                    }
                                } else {
                                    iosColors.cardSecondary
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) activeBorderColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                priority = p
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Text(
                                text = p.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) activeTextColor else iosColors.textPrimary
                            )
                        }
                    }
                }
            }

            // Subject Selector
            Text(
                text = "Привязка к предмету (необязательно)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAddNewActivityRequest != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(iosColors.cardSecondary)
                                .clickable { onAddNewActivityRequest() }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = IosBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Новый",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosBlue
                                )
                            }
                        }
                    }
                }

                // "No Subject" chip
                item {
                    val isNoSubject = selectedActivityId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isNoSubject) IosBlue else iosColors.cardSecondary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedActivityId = null
                                linkedSlotId = null
                                smartLessonMatch = null
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Без предмета",
                            fontSize = 12.sp,
                            fontWeight = if (isNoSubject) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isNoSubject) Color.White else iosColors.textPrimary
                        )
                    }
                }

                items(activities) { act ->
                    val isSelected = act.id == selectedActivityId
                    val actColor = ColorParser.parseHexColor(act.colorHex)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) actColor else iosColors.cardSecondary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedActivityId = act.id
                                recalculateSmartLesson(act.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else actColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = act.shortName.ifBlank { act.name },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else iosColors.textPrimary
                            )
                        }
                    }
                }
            }

            // Smart Homework Engine Badge
            val match = smartLessonMatch
            if (match != null && match.slot != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosPurple.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = IosPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Привязка к дате занятия:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosPurple
                                )
                                val parsedSelectedDate = runCatching { LocalDate.parse(dueDateIso.trim()) }.getOrDefault(match.dueDate)
                                Text(
                                    text = "${DateTimeUtils.formatDateWithDay(parsedSelectedDate)} (${match.slot.startTime})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textPrimary
                                )
                            }
                        }

                        if (match.dueDate != referenceDate) {
                            val isNextSelected = dueDateIso == match.dueDate.toString()
                            val isRefSelected = dueDateIso == referenceDate.toString()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isNextSelected) IosPurple else iosColors.cardSecondary)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            dueDateIso = match.dueDate.toString()
                                            linkedSlotId = match.linkedSlotId
                                        }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "След: ${DateTimeUtils.formatDateWithDay(match.dueDate)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNextSelected) Color.White else iosColors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isRefSelected) IosPurple else iosColors.cardSecondary)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            dueDateIso = referenceDate.toString()
                                            linkedSlotId = initialSlotId ?: match.linkedSlotId
                                        }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Этот: ${DateTimeUtils.formatDateWithDay(referenceDate)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRefSelected) Color.White else iosColors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Manual date toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Выбрать дату сдачи вручную",
                    fontSize = 14.sp,
                    color = iosColors.textPrimary
                )
                Switch(
                    checked = isManualDateOverride,
                    onCheckedChange = { isManualDateOverride = it },
                    colors = iosSwitchColors()
                )
            }

            AnimatedVisibility(visible = isManualDateOverride || selectedActivityId == null) {
                OutlinedTextField(
                    value = dueDateIso,
                    onValueChange = { dueDateIso = it },
                    label = { Text("Срок сдачи (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка / Подробности") },
                placeholder = { Text("например, Страница 42, задачи 1-15") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            // Subtasks / Checklist section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(iosColors.cardSecondary)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Чек-лист / Подзадачи",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary
                    )
                    if (subtasks.isNotEmpty()) {
                        val doneCount = subtasks.count { it.isDone }
                        Text(
                            text = "$doneCount/${subtasks.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }
                }

                subtasks.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(iosColors.card)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                subtasks = subtasks.map {
                                    if (it.id == sub.id) it.copy(isDone = !it.isDone) else it
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (sub.isDone) IosGreen else iosColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sub.title,
                            fontSize = 13.sp,
                            color = if (sub.isDone) iosColors.textSecondary else iosColors.textPrimary,
                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                subtasks = subtasks.filterNot { it.id == sub.id }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Удалить шаг",
                                tint = iosColors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Добавить пункт чек-листа...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                subtasks = subtasks + SubtaskItem(title = newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Добавить шаг",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save and Delete
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalSubtasks = if (newSubtaskText.isNotBlank()) {
                            subtasks + SubtaskItem(title = newSubtaskText.trim())
                        } else {
                            subtasks
                        }
                        val task = Task(
                            id = initialTask?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            dueDateIso = dueDateIso.trim().takeIf { it.isNotBlank() },
                            linkedSlotId = linkedSlotId,
                            linkedActivityId = selectedActivityId,
                            isCompleted = initialTask?.isCompleted ?: false,
                            note = note.trim().takeIf { it.isNotBlank() },
                            priority = priority,
                            subtasks = finalSubtasks,
                            presetId = initialTask?.presetId?.ifBlank { presetId } ?: presetId
                        )
                        onSave(task)
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
                Text("Сохранить задачу", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (initialTask != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete(initialTask.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить задачу")
                }
            }
        }
    }
}
