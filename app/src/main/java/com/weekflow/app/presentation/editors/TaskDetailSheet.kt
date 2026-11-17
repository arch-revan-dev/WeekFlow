package com.weekflow.app.presentation.editors

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.core.util.TaskAttachmentHelper
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskAttachment
import com.weekflow.app.domain.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    linkedActivity: Activity?,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: () -> Unit,
    onStartFocusTimer: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var newSubtaskText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val attachment = withContext(Dispatchers.IO) {
                    TaskAttachmentHelper.copyUriToInternalStorage(context, uri)
                }
                if (attachment != null) {
                    onUpdateTask(task.copy(attachments = task.attachments + attachment))
                } else {
                    Toast.makeText(context, "Не удалось прикрепить файл", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val taskDueDate = task.parseDueDate()
    val isOverdue = taskDueDate != null && taskDueDate.isBefore(today) && !task.isCompleted
    val isDueToday = taskDueDate != null && taskDueDate.isEqual(today) && !task.isCompleted

    val activityColor = linkedActivity?.let { ColorParser.parseHexColor(it.colorHex) } ?: IosBlue
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> IosRed
        TaskPriority.MEDIUM -> IosOrange
        TaskPriority.LOW -> IosBlue
        TaskPriority.NONE -> iosColors.textSecondary
    }

    val checkboxBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) IosBlue else priorityColor.copy(alpha = 0.12f),
        label = "DetailCheckboxBg"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar: Status Badge + Compact icon-only action bar (Copy, Edit, Focus, Tomorrow, Delete, Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusLabel, statusColor) = when {
                    task.isCompleted -> "Выполнено" to IosBlue
                    isOverdue -> "Просрочено" to IosRed
                    isDueToday -> "Сегодня" to IosOrange
                    else -> "Актуально" to IosBlue
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopActionIconButton(
                        icon = Icons.Rounded.ContentCopy,
                        contentDescription = "Копировать",
                        tint = iosColors.textPrimary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val fullText = buildString {
                                append(task.title)
                                if (!task.note.isNullOrBlank()) {
                                    append("\n\n")
                                    append(task.note)
                                }
                                if (task.subtasks.isNotEmpty()) {
                                    append("\n\nЧек-лист:\n")
                                    task.subtasks.forEach { sub ->
                                        append(if (sub.isDone) "☑ " else "☐ ")
                                        append(sub.title)
                                        append("\n")
                                    }
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Задача", fullText.trim()))
                            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                        }
                    )

                    TopActionIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Изменить",
                        tint = IosBlue,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditClick()
                        }
                    )

                    if (!task.isCompleted && onStartFocusTimer != null) {
                        TopActionIconButton(
                            icon = Icons.Rounded.HourglassEmpty,
                            contentDescription = "Фокус",
                            tint = IosBlue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartFocusTimer(task.title)
                            }
                        )
                    }

                    if (!task.isCompleted) {
                        TopActionIconButton(
                            icon = Icons.Rounded.Update,
                            contentDescription = "На завтра",
                            tint = IosBlue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val tomorrowIso = LocalDate.now().plusDays(1).toString()
                                onUpdateTask(task.copy(dueDateIso = tomorrowIso))
                                Toast.makeText(context, "Перенесено на завтра", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    TopActionIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = IosRed,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteTask()
                        }
                    )

                    TopActionIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = iosColors.textSecondary,
                        onClick = onDismiss
                    )
                }
            }

            // Full Untruncated Title with Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(iosColors.cardSecondary.copy(alpha = if (iosColors.isDark) 0.55f else 0.7f))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(checkboxBgColor)
                        .border(
                            border = BorderStroke(
                                width = if (task.isCompleted) 0.dp else 2.dp,
                                color = if (task.isCompleted) IosBlue else priorityColor
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleCompleted()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Выполнено",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) iosColors.textSecondary else iosColors.textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            // Metadata Badges (Subject, Due Date, Priority)
            if (linkedActivity != null || taskDueDate != null || task.priority != TaskPriority.NONE) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    linkedActivity?.let { act ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(activityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(activityColor)
                                )
                                Text(
                                    text = act.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activityColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    taskDueDate?.let { dueDate ->
                        val dateColor = when {
                            isOverdue -> IosRed
                            isDueToday -> IosOrange
                            else -> iosColors.textPrimary
                        }
                        val dateLabel = when {
                            isOverdue -> DateTimeUtils.formatDateWithDay(dueDate)
                            isDueToday -> "Сегодня (${DateTimeUtils.formatDateWithDay(dueDate)})"
                            dueDate.isEqual(today.plusDays(1)) -> "Завтра (${DateTimeUtils.formatDateWithDay(dueDate)})"
                            else -> DateTimeUtils.formatDateWithDay(dueDate)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isOverdue) IosRed.copy(alpha = 0.12f) else iosColors.cardSecondary
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Event,
                                    contentDescription = null,
                                    tint = dateColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = dateLabel,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = dateColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (task.priority != TaskPriority.NONE) {
                        val pLabel = when (task.priority) {
                            TaskPriority.HIGH -> "Высокий"
                            TaskPriority.MEDIUM -> "Средний"
                            TaskPriority.LOW -> "Низкий"
                            TaskPriority.NONE -> ""
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Flag,
                                    contentDescription = null,
                                    tint = priorityColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = pLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = priorityColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Note Section
            if (!task.note.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(iosColors.cardSecondary)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Заметка",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textSecondary
                        )
                    }

                    SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = task.note,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            color = iosColors.textPrimary
                        )
                    }
                }
            }

            // Attachments Section
            TaskAttachmentsSection(
                attachments = task.attachments,
                onAddClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                onOpenAttachment = { att -> TaskAttachmentHelper.openAttachment(context, att) },
                onRemoveAttachment = { att ->
                    TaskAttachmentHelper.deleteLocalFile(att)
                    onUpdateTask(task.copy(attachments = task.attachments.filterNot { it.id == att.id }))
                }
            )

            // Clean Minimal Checklist Section (no template chips, no trainer params)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(iosColors.cardSecondary)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val doneCount = task.subtasks.count { it.isDone }
                val totalCount = task.subtasks.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Чек-лист",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                    }
                    if (totalCount > 0) {
                        Text(
                            text = "$doneCount/$totalCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }
                }

                if (totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / totalCount.toFloat() },
                        color = IosBlue,
                        trackColor = iosColors.card,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        task.subtasks.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iosColors.card)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val updated = task.subtasks.map {
                                            if (it.id == sub.id) it.copy(isDone = !it.isDone) else it
                                        }
                                        onUpdateTask(task.copy(subtasks = updated))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (sub.isDone) IosBlue else iosColors.textSecondary,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sub.title,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = if (sub.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newSubtaskText.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val updated = task.subtasks + SubtaskItem(title = newSubtaskText.trim())
                                    onUpdateTask(task.copy(subtasks = updated))
                                    newSubtaskText = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosBlue)
                            .clickable {
                                if (newSubtaskText.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val updated = task.subtasks + SubtaskItem(title = newSubtaskText.trim())
                                    onUpdateTask(task.copy(subtasks = updated))
                                    newSubtaskText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Добавить",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Single clean primary bottom CTA
            val primaryColor = if (task.isCompleted) IosOrange else IosBlue
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleCompleted()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Rounded.Replay else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (task.isCompleted) "Вернуть" else "Выполнено",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun TopActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    val iosColors = LocalIosColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(iosColors.cardSecondary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun TaskAttachmentsSection(
    attachments: List<TaskAttachment>,
    onAddClick: () -> Unit,
    onOpenAttachment: (TaskAttachment) -> Unit,
    onRemoveAttachment: (TaskAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var previewImageAttachment by remember { mutableStateOf<TaskAttachment?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(iosColors.cardSecondary)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Вложения",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosBlue.copy(alpha = 0.12f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddClick()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Прикрепить файл",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosBlue
                )
            }
        }

        attachments.forEach { att ->
            val isImage = TaskAttachmentHelper.isImageAttachment(att)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(iosColors.card)
                    .clickable {
                        if (isImage) {
                            previewImageAttachment = att
                        } else {
                            onOpenAttachment(att)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isImage) {
                    AttachmentImageThumbnail(attachment = att)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.InsertDriveFile,
                        contentDescription = null,
                        tint = IosBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = att.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = TaskAttachmentHelper.formatFileSize(att.sizeBytes),
                        fontSize = 11.sp,
                        color = iosColors.textSecondary
                    )
                }
                if (isImage) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                TaskAttachmentHelper.copyImageToClipboard(context, att)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Копировать фото",
                            tint = IosBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRemoveAttachment(att)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Удалить вложение",
                        tint = iosColors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    previewImageAttachment?.let { imgAtt ->
        InAppImageViewerDialog(
            attachment = imgAtt,
            onDismiss = { previewImageAttachment = null },
            onOpenExternal = { TaskAttachmentHelper.openAttachment(context, imgAtt) }
        )
    }
}

@Composable
private fun AttachmentImageThumbnail(
    attachment: TaskAttachment,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = TaskAttachmentHelper.getCachedBitmap(attachment.localPath, 160),
        key1 = attachment.localPath
    ) {
        if (value == null) {
            value = TaskAttachmentHelper.decodeSampledBitmap(attachment.localPath, 160)
        }
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(iosColors.cardSecondary),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = thumbnail
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = attachment.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = IosBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun InAppImageViewerDialog(
    attachment: TaskAttachment,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        var bitmap by remember(attachment.localPath) { mutableStateOf<ImageBitmap?>(null) }
        var isLoading by remember(attachment.localPath) { mutableStateOf(true) }
        var showChrome by remember(attachment.localPath) { mutableStateOf(true) }
        var scale by remember(attachment.localPath) { mutableFloatStateOf(1f) }
        var offset by remember(attachment.localPath) { mutableStateOf(Offset.Zero) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        LaunchedEffect(attachment.localPath) {
            isLoading = true
            bitmap = TaskAttachmentHelper.decodeSampledBitmap(attachment.localPath, 2048)
            isLoading = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
                .pointerInput(attachment.localPath) {
                    detectTapGestures(
                        onTap = { showChrome = !showChrome },
                        onDoubleTap = { tapOffset ->
                            val loaded = bitmap ?: return@detectTapGestures
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val targetScale = 2.5f
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val rawOffset = (center - tapOffset) * (targetScale - 1f)
                                scale = targetScale
                                offset = clampViewerOffset(rawOffset, targetScale, containerSize, loaded)
                            }
                        }
                    )
                }
                .pointerInput(attachment.localPath) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val loaded = bitmap ?: return@detectTransformGestures
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        val newOffset = if (newScale <= 1.01f) {
                            Offset.Zero
                        } else {
                            val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            val scaleFactor = if (scale > 0f) newScale / scale else 1f
                            val nextOffset = offset * scaleFactor + (center - centroid) * (scaleFactor - 1f) + pan
                            clampViewerOffset(nextOffset, newScale, containerSize, loaded)
                        }
                        scale = newScale
                        offset = newOffset
                    }
                }
        ) {
            val loadedBitmap = bitmap
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                loadedBitmap != null -> {
                    Image(
                        bitmap = loadedBitmap,
                        contentDescription = attachment.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Не удалось загрузить изображение",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { onOpenExternal() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Открыть в другом приложении",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.displayName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = TaskAttachmentHelper.formatFileSize(attachment.sizeBytes),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                TaskAttachmentHelper.copyImageToClipboard(context, attachment)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Копировать фото",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { onOpenExternal() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Открыть в другом приложении",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun clampViewerOffset(
    rawOffset: Offset,
    scale: Float,
    containerSize: IntSize,
    bitmap: ImageBitmap
): Offset {
    if (containerSize.width <= 0 || containerSize.height <= 0 || bitmap.width <= 0 || bitmap.height <= 0) {
        return Offset.Zero
    }
    val containerW = containerSize.width.toFloat()
    val containerH = containerSize.height.toFloat()
    val fitScale = minOf(containerW / bitmap.width.toFloat(), containerH / bitmap.height.toFloat())
    val fittedW = bitmap.width * fitScale
    val fittedH = bitmap.height * fitScale
    val maxX = ((fittedW * scale - containerW) / 2f).coerceAtLeast(0f)
    val maxY = ((fittedH * scale - containerH) / 2f).coerceAtLeast(0f)
    return Offset(
        x = rawOffset.x.coerceIn(-maxX, maxX),
        y = rawOffset.y.coerceIn(-maxY, maxY)
    )
}
