package com.weekflow.app.core.designsystem.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskItemRow(
    task: Task,
    linkedActivity: Activity?,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onFocusTimerClick: (() -> Unit)? = null,
    onPostponeTomorrowClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val today = LocalDate.now()

    var isExpanded by remember(task.id) { mutableStateOf(false) }
    var isActionsVisible by remember(task.id) { mutableStateOf(false) }
    var isTitleOverflowing by remember(task.id, task.title) { mutableStateOf(false) }
    var isNoteOverflowing by remember(task.id, task.note) { mutableStateOf(false) }
    val hasLongContent = isTitleOverflowing || isNoteOverflowing || task.subtasks.size > 4 || isExpanded

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
        targetValue = if (task.isCompleted) priorityColor else priorityColor.copy(alpha = 0.08f),
        label = "TaskCheckboxBg"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.5f else 1f,
        label = "TaskAlpha"
    )

    val cardSurface = if (iosColors.isDark) Color(0xFF232328) else Color(0xFFFFFFFF)
    val cardBorderColor = if (iosColors.isDark) Color(0xFF383840) else Color(0xFFE5E5EC)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardSurface)
            .border(
                BorderStroke(
                    width = if (isActionsVisible) 1.2.dp else 0.8.dp,
                    color = if (isActionsVisible) IosBlue.copy(alpha = 0.7f) else cardBorderColor
                ),
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = {
                    if (isActionsVisible) {
                        isActionsVisible = false
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isActionsVisible = !isActionsVisible
                }
            )
            .alpha(contentAlpha)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Circular Priority Checkbox aligned with top title line
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(checkboxBgColor)
                        .border(
                            border = BorderStroke(
                                width = if (task.isCompleted) 0.dp else 1.8.dp,
                                color = priorityColor
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) iosColors.textSecondary else iosColors.textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (!isExpanded) {
                                isTitleOverflowing = result.hasVisualOverflow
                            }
                        }
                    )

                    task.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = noteText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = iosColors.textSecondary,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!isExpanded) {
                                    isNoteOverflowing = result.hasVisualOverflow
                                }
                            }
                        )
                    }

                    if (task.subtasks.isNotEmpty() && !task.isCompleted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val displayedSubtasks = if (isExpanded) task.subtasks else task.subtasks.take(4)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            displayedSubtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (onToggleSubtask != null) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggleSubtask(sub.id)
                                            } else {
                                                onClick()
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (sub.isDone) IosBlue else iosColors.textSecondary,
                                        modifier = Modifier
                                            .padding(top = 1.dp)
                                            .size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sub.title,
                                        fontSize = 12.sp,
                                        color = if (sub.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                        textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    val gradeValue = task.grade?.trim()?.takeIf { it.isNotBlank() }
                    val hasMetadata = linkedActivity != null ||
                        taskDueDate != null ||
                        task.subtasks.isNotEmpty() ||
                        gradeValue != null ||
                        task.attachments.isNotEmpty() ||
                        hasLongContent

                    if (hasMetadata) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Subject / Activity chip
                                linkedActivity?.let { act ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(activityColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = act.shortName.ifBlank { act.name },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = activityColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Due date badge
                                taskDueDate?.let { dueDate ->
                                    val dateBadgeColor = when {
                                        isOverdue -> IosRed
                                        isDueToday -> IosOrange
                                        else -> iosColors.textSecondary
                                    }
                                    val dateText = when {
                                        isOverdue -> DateTimeUtils.formatDateWithDay(dueDate)
                                        isDueToday -> "Сегодня"
                                        dueDate.isEqual(today.plusDays(1)) -> "Завтра"
                                        else -> DateTimeUtils.formatDateWithDay(dueDate)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when {
                                                    isOverdue -> IosRed.copy(alpha = 0.12f)
                                                    isDueToday -> IosOrange.copy(alpha = 0.12f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .padding(
                                                horizontal = if (isOverdue || isDueToday) 6.dp else 2.dp,
                                                vertical = 2.5.dp
                                            )
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Event,
                                                contentDescription = null,
                                                tint = dateBadgeColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = dateText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isOverdue || isDueToday) FontWeight.SemiBold else FontWeight.Medium,
                                                color = dateBadgeColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // Grade pill
                                gradeValue?.let { grade ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(IosBlue.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Grade,
                                                contentDescription = null,
                                                tint = IosBlue,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = grade,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IosBlue,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // Attachments count badge
                                if (task.attachments.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(iosColors.cardSecondary)
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AttachFile,
                                                contentDescription = null,
                                                tint = iosColors.textSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "${task.attachments.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = iosColors.textSecondary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // Subtasks count badge
                                if (task.subtasks.isNotEmpty()) {
                                    val doneCount = task.subtasks.count { it.isDone }
                                    val allDone = doneCount == task.subtasks.size
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (allDone) IosBlue.copy(alpha = 0.12f)
                                                else iosColors.cardSecondary
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Checklist,
                                                contentDescription = null,
                                                tint = if (allDone) IosBlue else iosColors.textSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "$doneCount/${task.subtasks.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (allDone) IosBlue else iosColors.textSecondary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasLongContent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(iosColors.cardSecondary)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isExpanded = !isExpanded
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                                        tint = IosBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom icon-only action bar evenly distributed across full card width on long-press
            AnimatedVisibility(visible = isActionsVisible) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(iosColors.separator.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskQuickActionIcon(
                            icon = if (task.isCompleted) Icons.Rounded.Replay else Icons.Rounded.Check,
                            contentDescription = if (task.isCompleted) "В работу" else "Выполнено",
                            tint = if (task.isCompleted) IosOrange else IosBlue,
                            backgroundColor = iosColors.cardSecondary,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isActionsVisible = false
                                onToggleCompleted()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (onEditClick != null) {
                            TaskQuickActionIcon(
                                icon = Icons.Rounded.Edit,
                                contentDescription = "Изменить",
                                tint = IosBlue,
                                backgroundColor = iosColors.cardSecondary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isActionsVisible = false
                                    onEditClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TaskQuickActionIcon(
                            icon = Icons.Rounded.ContentCopy,
                            contentDescription = "Копировать",
                            tint = iosColors.textPrimary,
                            backgroundColor = iosColors.cardSecondary,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isActionsVisible = false
                                if (onCopyClick != null) {
                                    onCopyClick()
                                } else {
                                    val text = buildString {
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
                                    }.trim()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Задача", text))
                                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (onFocusTimerClick != null && !task.isCompleted) {
                            TaskQuickActionIcon(
                                icon = Icons.Rounded.HourglassEmpty,
                                contentDescription = "Фокус",
                                tint = IosBlue,
                                backgroundColor = iosColors.cardSecondary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isActionsVisible = false
                                    onFocusTimerClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (onPostponeTomorrowClick != null && !task.isCompleted) {
                            TaskQuickActionIcon(
                                icon = Icons.Rounded.Update,
                                contentDescription = "Завтра",
                                tint = IosBlue,
                                backgroundColor = iosColors.cardSecondary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isActionsVisible = false
                                    onPostponeTomorrowClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (onDeleteClick != null) {
                            TaskQuickActionIcon(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = IosRed,
                                backgroundColor = IosRed.copy(alpha = 0.10f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isActionsVisible = false
                                    onDeleteClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TaskQuickActionIcon(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = iosColors.textSecondary,
                            backgroundColor = iosColors.cardSecondary,
                            onClick = {
                                isActionsVisible = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskQuickActionIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
