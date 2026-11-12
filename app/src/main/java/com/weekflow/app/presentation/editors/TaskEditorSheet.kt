package com.weekflow.app.presentation.editors

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.core.util.TaskAttachmentHelper
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.NextLessonMatch
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskAttachment
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.usecase.CalculateNextLessonDateUseCase
import com.weekflow.app.domain.usecase.DetectActiveOrRecentActivityUseCase
import com.weekflow.app.domain.usecase.DetectedActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var grade by remember(initialTask?.id) { mutableStateOf(initialTask?.grade) }
    var attachments by remember(initialTask?.id) {
        mutableStateOf(initialTask?.attachments ?: emptyList())
    }
    var newlyAddedAttachmentIds by remember(initialTask?.id) {
        mutableStateOf(setOf<String>())
    }
    var removedInitialAttachments by remember(initialTask?.id) {
        mutableStateOf(emptyList<TaskAttachment>())
    }
    var subtasks by remember(initialTask?.id) {
        mutableStateOf(initialTask?.subtasks ?: emptyList())
    }
    var newSubtaskText by remember(initialTask?.id) { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val attachment = withContext(Dispatchers.IO) {
                    TaskAttachmentHelper.copyUriToInternalStorage(context, uri)
                }
                if (attachment != null) {
                    newlyAddedAttachmentIds = newlyAddedAttachmentIds + attachment.id
                    attachments = attachments + attachment
                } else {
                    Toast.makeText(context, "Не удалось прикрепить файл", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
    val defaultSmartReferenceDate = remember(referenceDate, vacations.size, preferences.skipHolidaysInSmartHomework) {
        calculateNextLessonDateUseCase.findNextValidStudyDay(referenceDate, preferences, vacations)
    }

    var dueDateIso by remember(initialTask?.id) {
        mutableStateOf(
            initialTask?.dueDateIso
                ?: initialDetected?.nextLessonMatch?.dueDate?.toString()
                ?: defaultSmartReferenceDate.toString()
        )
    }
    var isManualDateOverride by remember(initialTask?.id) { mutableStateOf(false) }
    var smartLessonMatch by remember(initialTask?.id) {
        mutableStateOf<NextLessonMatch?>(initialDetected?.nextLessonMatch)
    }

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

    LaunchedEffect(allSlots.size, vacations.size) {
        if (initialTask == null && !isManualDateOverride) {
            val actId = selectedActivityId
            if (actId != null) {
                recalculateSmartLesson(actId)
            } else if (!isInitialized && initialActivityId == null && detectActiveOrRecentActivityUseCase != null && allSlots.isNotEmpty()) {
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
                    dueDateIso = detected.nextLessonMatch.dueDate.toString()
                    smartLessonMatch = detected.nextLessonMatch
                } else {
                    dueDateIso = defaultSmartReferenceDate.toString()
                }
                isInitialized = true
            } else {
                dueDateIso = defaultSmartReferenceDate.toString()
            }
        }
    }

    LaunchedEffect(selectedActivityId) {
        val actId = selectedActivityId
        if (actId != null && initialTask == null) {
            recalculateSmartLesson(actId)
        }
    }

    fun dismissAndCleanup() {
        // Remove any newly copied files that were never saved
        attachments
            .filter { it.id in newlyAddedAttachmentIds }
            .forEach { TaskAttachmentHelper.deleteLocalFile(it) }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { dismissAndCleanup() },
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
                    text = if (initialTask == null) "Новая задача" else "Редактировать",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (initialTask != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                initialTask.attachments.forEach { TaskAttachmentHelper.deleteLocalFile(it) }
                                onDelete(initialTask.id)
                                onDismiss()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = IosRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    TextButton(onClick = { dismissAndCleanup() }) {
                        Text("Отмена", color = iosColors.textSecondary)
                    }
                }
            }

            // Auto-detection notification banner
            val detected = detectedActivity
            if (showAutoDetectedBanner && detected != null) {
                val act = activities.firstOrNull { it.id == detected.slot.activityTypeId }
                val actName = act?.name ?: appProfile.subjectTerm
                val actColor = ColorParser.parseHexColor(act?.colorHex ?: "#007AFF")
                val nextDateStr = DateTimeUtils.formatDateWithDay(detected.nextLessonMatch.dueDate)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(actColor.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, actColor.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$actName • ${appProfile.itemTerm.lowercase()} $nextDateStr",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAutoDetectedBanner = false
                            selectedActivityId = null
                            linkedSlotId = null
                            dueDateIso = defaultSmartReferenceDate.toString()
                            smartLessonMatch = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Сбросить",
                            tint = iosColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 4
            )

            // Priority Selector
            Text(
                text = "Приоритет",
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

            // Profile-aware Activity Selector
            Text(
                text = appProfile.subjectTerm,
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
                            text = "Без привязки",
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

            // Smart Date Match Card (unified IosBlue palette)
            val match = smartLessonMatch
            if (match != null && match.slot != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosBlue.copy(alpha = 0.10f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = IosBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val parsedSelectedDate = runCatching { LocalDate.parse(dueDateIso.trim()) }.getOrDefault(match.dueDate)
                            Text(
                                text = "Дата ${appProfile.itemTermGenitive}: ${DateTimeUtils.formatDateWithDay(parsedSelectedDate)} (${match.slot.startTime})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textPrimary
                            )
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
                                        .background(if (isNextSelected) IosBlue else iosColors.cardSecondary)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            dueDateIso = match.dueDate.toString()
                                            linkedSlotId = match.linkedSlotId
                                        }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Далее • ${DateTimeUtils.formatDateWithDay(match.dueDate)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNextSelected) Color.White else iosColors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isRefSelected) IosBlue else iosColors.cardSecondary)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            dueDateIso = referenceDate.toString()
                                            linkedSlotId = initialSlotId ?: match.linkedSlotId
                                        }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Этот день • ${DateTimeUtils.formatDateWithDay(referenceDate)}",
                                        fontSize = 11.5.sp,
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
                    text = "Дата вручную",
                    fontSize = 14.sp,
                    color = iosColors.textPrimary
                )
                Switch(
                    checked = isManualDateOverride,
                    onCheckedChange = { enabled ->
                        isManualDateOverride = enabled
                        if (!enabled) {
                            val currentMatch = smartLessonMatch
                            if (currentMatch != null && selectedActivityId != null) {
                                dueDateIso = currentMatch.dueDate.toString()
                                linkedSlotId = currentMatch.linkedSlotId
                            } else if (initialTask == null) {
                                dueDateIso = defaultSmartReferenceDate.toString()
                            }
                        }
                    },
                    colors = iosSwitchColors()
                )
            }

            AnimatedVisibility(visible = isManualDateOverride) {
                IosDatePickerButton(
                    label = "Срок сдачи",
                    dateIso = dueDateIso,
                    onDateSelected = { dueDateIso = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 8
            )

            // Grade Section
            TaskGradeSection(
                grade = grade,
                onGradeChanged = { grade = it },
                applyOnType = true
            )

            // Attachments Section
            TaskAttachmentsSection(
                attachments = attachments,
                onAddClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                onOpenAttachment = { att -> TaskAttachmentHelper.openAttachment(context, att) },
                onRemoveAttachment = { att ->
                    attachments = attachments.filterNot { it.id == att.id }
                    if (att.id in newlyAddedAttachmentIds) {
                        TaskAttachmentHelper.deleteLocalFile(att)
                        newlyAddedAttachmentIds = newlyAddedAttachmentIds - att.id
                    } else {
                        removedInitialAttachments = removedInitialAttachments + att
                    }
                }
            )

            // Minimal Checklist Section (no template chips, no trainer params)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
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
                        text = "Чек-лист",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
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
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (sub.isDone) IosBlue else iosColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sub.title,
                            fontSize = 13.5.sp,
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
                                contentDescription = "Удалить пункт",
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
                        placeholder = { Text("Добавить пункт...", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSubtaskText.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    subtasks = subtasks + SubtaskItem(title = newSubtaskText.trim())
                                    newSubtaskText = ""
                                }
                            }
                        ),
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
                            contentDescription = "Добавить",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        removedInitialAttachments.forEach { TaskAttachmentHelper.deleteLocalFile(it) }
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
                            presetId = initialTask?.presetId?.ifBlank { presetId } ?: presetId,
                            grade = grade?.trim()?.takeIf { it.isNotBlank() },
                            attachments = attachments
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
                Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
