package com.weekflow.app.presentation.editors

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import java.time.LocalDate

/**
 * Full-screen / bottom-sheet reader for viewing a Task in full detail without any text truncation.
 * Supports reading long titles, multi-paragraph notes, checking/adding subtasks, copying text,
 * starting a focus timer, postponing, or jumping to the editor.
 */
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
    val today = LocalDate.now()

    var newSubtaskText by remember { mutableStateOf("") }

    val taskDueDate = task.parseDueDate()
    val isOverdue = taskDueDate != null && taskDueDate.isBefore(today) && !task.isCompleted
    val isDueToday = taskDueDate != null && taskDueDate.isEqual(today) && !task.isCompleted

    val activityColor = linkedActivity?.let { ColorParser.parseHexColor(it.colorHex) } ?: IosBlue
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF3B30)
        TaskPriority.MEDIUM -> Color(0xFFFF9500)
        TaskPriority.LOW -> Color(0xFF007AFF)
        TaskPriority.NONE -> Color(0xFF8E8E93)
    }

    val checkboxBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) IosGreen else priorityColor.copy(alpha = 0.12f),
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
            // Clean Top Header: Status Badge on the left, compact Close button on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusLabel, statusColor) = when {
                    task.isCompleted -> "Выполнено" to IosGreen
                    isOverdue -> "Просрочено" to IosRed
                    isDueToday -> "На сегодня" to IosOrange
                    else -> "Актуально" to IosBlue
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1
                    )
                }

                // Compact 32.dp Close circle (custom Box to avoid M3 48.dp minimumInteractiveComponentSize inflation)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iosColors.cardSecondary)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = iosColors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Full Untruncated Title with Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(iosColors.cardSecondary.copy(alpha = if (iosColors.isDark) 0.55f else 0.7f))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(checkboxBgColor)
                        .border(
                            border = BorderStroke(
                                width = if (task.isCompleted) 0.dp else 2.dp,
                                color = if (task.isCompleted) IosGreen else priorityColor
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) iosColors.textSecondary else iosColors.textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            // Metadata Badges (Subject, Due Date, Priority)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Linked Subject
                linkedActivity?.let { act ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(activityColor.copy(alpha = 0.14f))
                            .border(BorderStroke(1.dp, activityColor.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(activityColor)
                            )
                            Text(
                                text = act.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activityColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Due Date
                taskDueDate?.let { dueDate ->
                    val dateColor = when {
                        isOverdue -> IosRed
                        isDueToday -> IosOrange
                        else -> iosColors.textPrimary
                    }
                    val dateLabel = when {
                        isOverdue -> "Просрочено: ${DateTimeUtils.formatDateWithDay(dueDate)}"
                        isDueToday -> "Сегодня (${DateTimeUtils.formatDateWithDay(dueDate)})"
                        dueDate.isEqual(today.plusDays(1)) -> "Завтра (${DateTimeUtils.formatDateWithDay(dueDate)})"
                        else -> DateTimeUtils.formatDateWithDay(dueDate)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(iosColors.cardSecondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Event,
                                contentDescription = null,
                                tint = dateColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = dateLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = dateColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Priority
                if (task.priority != TaskPriority.NONE) {
                    val pLabel = when (task.priority) {
                        TaskPriority.HIGH -> "Высокий приоритет"
                        TaskPriority.MEDIUM -> "Средний приоритет"
                        TaskPriority.LOW -> "Низкий приоритет"
                        TaskPriority.NONE -> ""
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(priorityColor.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(14.dp)
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

            // Full Note / Detailed Description Section
            if (!task.note.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(iosColors.cardSecondary)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = IosBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Подробное описание / Заметка",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textSecondary
                        )
                    }

                    SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = task.note,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = iosColors.textPrimary
                        )
                    }
                }
            }

            // Full Interactive Checklist / Subtasks Section
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
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Чек-лист / Подзадачи",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                    }
                    if (totalCount > 0) {
                        Text(
                            text = "$doneCount из $totalCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (doneCount == totalCount) IosGreen else IosBlue
                        )
                    }
                }

                if (totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / totalCount.toFloat() },
                        color = if (doneCount == totalCount) IosGreen else IosBlue,
                        trackColor = iosColors.card,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
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
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (sub.isDone) IosGreen else iosColors.textSecondary,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(20.dp)
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

                // Quick add subtask right inside the detail view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Добавить шаг в чек-лист...", fontSize = 13.sp) },
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
                            contentDescription = "Добавить шаг",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Action Grid: 2x2 well-spaced custom action buttons (never wraps or overflows)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Изменить + Копировать
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskDetailActionTile(
                        icon = Icons.Rounded.Edit,
                        label = "Изменить",
                        tint = IosBlue,
                        backgroundColor = IosBlue.copy(alpha = 0.12f),
                        borderColor = IosBlue.copy(alpha = 0.30f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditClick()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    TaskDetailActionTile(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Копировать",
                        tint = iosColors.textPrimary,
                        backgroundColor = iosColors.cardSecondary,
                        borderColor = iosColors.separator,
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
                            Toast.makeText(context, "Текст задачи скопирован", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Фокус + На завтра (for active tasks)
                if (!task.isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onStartFocusTimer != null) {
                            TaskDetailActionTile(
                                icon = Icons.Rounded.HourglassEmpty,
                                label = "Фокус",
                                tint = IosBlue,
                                backgroundColor = iosColors.cardSecondary,
                                borderColor = iosColors.separator,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartFocusTimer(task.title)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        TaskDetailActionTile(
                            icon = Icons.Rounded.Update,
                            label = "На завтра",
                            tint = IosOrange,
                            backgroundColor = IosOrange.copy(alpha = 0.10f),
                            borderColor = IosOrange.copy(alpha = 0.30f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val tomorrowIso = LocalDate.now().plusDays(1).toString()
                                onUpdateTask(task.copy(dueDateIso = tomorrowIso))
                                Toast.makeText(context, "Перенесено на завтра", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 3: Primary Complete/Restore Button + Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = if (task.isCompleted) IosOrange else IosGreen
                    Box(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleCompleted()
                            }
                            .padding(horizontal = 12.dp),
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
                                text = if (task.isCompleted) "В работу" else "Выполнено",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosRed.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, IosRed.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteTask()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = IosRed,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Удалить",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosRed,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDetailActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(0.8.dp, borderColor), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
