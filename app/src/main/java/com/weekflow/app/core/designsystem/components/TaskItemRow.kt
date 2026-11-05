package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import java.time.LocalDate

@Composable
fun TaskItemRow(
    task: Task,
    linkedActivity: Activity?,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit,
    onFocusTimerClick: (() -> Unit)? = null,
    onPostponeTomorrowClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val today = LocalDate.now()

    val taskDueDate = task.parseDueDate()
    val isOverdue = taskDueDate != null && taskDueDate.isBefore(today) && !task.isCompleted
    val isDueToday = taskDueDate != null && taskDueDate.isEqual(today) && !task.isCompleted

    val activityColor = linkedActivity?.let { ColorParser.parseHexColor(it.colorHex) } ?: IosBlue

    // Priority color mapping according to TickTick standard
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF3B30)
        TaskPriority.MEDIUM -> Color(0xFFFF9500)
        TaskPriority.LOW -> Color(0xFF007AFF)
        TaskPriority.NONE -> Color(0xFF8E8E93)
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
            .border(BorderStroke(0.8.dp, cardBorderColor), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .alpha(contentAlpha)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TickTick Circular Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
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
                // Task title
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) iosColors.textSecondary else iosColors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Task note if present
                task.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = noteText,
                        fontSize = 13.sp,
                        color = iosColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: Priority chip + Subject chip + Due date badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority chip if non-default
                    if (task.priority != TaskPriority.NONE) {
                        val (label, pColor) = when (task.priority) {
                            TaskPriority.HIGH -> "Высокий" to Color(0xFFFF3B30)
                            TaskPriority.MEDIUM -> "Средний" to Color(0xFFFF9500)
                            TaskPriority.LOW -> "Низкий" to Color(0xFF007AFF)
                            TaskPriority.NONE -> "" to Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(pColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pColor
                            )
                        }
                    }

                    // Subject chip
                    linkedActivity?.let { act ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activityColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = act.shortName.ifBlank { act.name },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activityColor,
                                maxLines = 1,
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
                            isOverdue -> "Просрочено (${DateTimeUtils.formatDateWithDay(dueDate)})"
                            isDueToday -> "Сегодня"
                            dueDate.isEqual(today.plusDays(1)) -> "Завтра"
                            else -> DateTimeUtils.formatDateWithDay(dueDate)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                fontWeight = if (isOverdue || isDueToday) FontWeight.SemiBold else FontWeight.Normal,
                                color = dateBadgeColor
                            )
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
                                    if (allDone) IosGreen.copy(alpha = 0.14f)
                                    else iosColors.cardSecondary
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Checklist,
                                    contentDescription = null,
                                    tint = if (allDone) IosGreen else iosColors.textSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "$doneCount/${task.subtasks.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (allDone) IosGreen else iosColors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Inline Subtask rows if any
                if (task.subtasks.isNotEmpty() && !task.isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        task.subtasks.take(5).forEach { sub ->
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sub.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (sub.isDone) IosGreen else iosColors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.title,
                                    fontSize = 12.sp,
                                    color = if (sub.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions (Focus timer, Postpone, Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (onFocusTimerClick != null && !task.isCompleted) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFocusTimerClick()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = "Фокус таймер",
                            tint = iosColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onPostponeTomorrowClick != null && !task.isCompleted) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPostponeTomorrowClick()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Update,
                            contentDescription = "Перенести на завтра",
                            tint = iosColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteClick()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = iosColors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
