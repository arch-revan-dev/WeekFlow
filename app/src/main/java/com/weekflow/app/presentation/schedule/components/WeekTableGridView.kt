package com.weekflow.app.presentation.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.WeekCycle
import java.time.LocalDate

@Composable
fun WeekTableGridView(
    weekDays: List<LocalDate>,
    slots: List<Slot>,
    activities: List<Activity>,
    activeWeekCycle: WeekCycle,
    isTwoWeekCycleEnabled: Boolean,
    onEditSlot: (slotId: String) -> Unit,
    onAddSlot: (dayOfWeek: Int, periodOrder: Int) -> Unit,
    modifier: Modifier = Modifier,
    isSaturdayWeekend: Boolean = false,
    tasks: List<Task> = emptyList(),
    onDateClick: ((LocalDate) -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val activityMap = activities.associateBy { it.id }

    var scaleFactor by remember { mutableFloatStateOf(0.9f) }

    val maxPeriod = maxOf(6, (slots.maxOfOrNull { it.periodOrder } ?: 6))
    val periods = (1..maxPeriod).toList()

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val cellWidth = (96 * scaleFactor).dp
    val timeColWidth = (44 * scaleFactor).coerceAtLeast(30f).dp
    val cellHeight = (84 * scaleFactor).dp
    val headerHeight = (44 * scaleFactor).coerceAtLeast(34f).dp

    val activeSlots = slots.filter { slot ->
        !isTwoWeekCycleEnabled ||
            slot.weekCycle == WeekCycle.EVERY_WEEK ||
            slot.weekCycle == activeWeekCycle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleFactor = (scaleFactor * zoom).coerceIn(0.55f, 1.4f)
                }
            }
    ) {
        // Horizontal scrollable table container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .horizontalScroll(horizontalScroll)
        ) {
            // Sticky Header Row (№ + Days of Week with Dates)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(headerHeight)
                        .width(timeColWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "№",
                        fontSize = (12 * scaleFactor).coerceAtLeast(9f).sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textSecondary
                    )
                }

                weekDays.forEach { date ->
                    val dayOfWeek = date.dayOfWeek.value
                    val isToday = date == LocalDate.now()
                    val isWeekend = dayOfWeek == 7 || (isSaturdayWeekend && dayOfWeek == 6)

                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .padding(horizontal = 2.dp)
                            .height(headerHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) IosBlue.copy(alpha = 0.15f) else iosColors.cardSecondary)
                            .then(
                                if (onDateClick != null) {
                                    Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDateClick(date)
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val dayNameSize = (12f * scaleFactor).coerceIn(9f, 16f)
                            val dayNumSize = (11f * scaleFactor).coerceIn(9f, 15f)

                            Text(
                                text = DateTimeUtils.getDayOfWeekShort(date.dayOfWeek.value),
                                fontSize = dayNameSize.sp,
                                lineHeight = (dayNameSize * 1.1f).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = when {
                                    isToday -> IosBlue
                                    isWeekend -> IosRed.copy(alpha = 0.8f)
                                    else -> iosColors.textPrimary
                                }
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = dayNumSize.sp,
                                lineHeight = (dayNumSize * 1.1f).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isToday -> IosBlue
                                    isWeekend -> IosRed.copy(alpha = 0.8f)
                                    else -> iosColors.textSecondary
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Vertically scrollable grid body
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Left Period Column
                Column(
                    modifier = Modifier.width(timeColWidth)
                ) {
                    periods.forEach { period ->
                        val sampleSlot = activeSlots.firstOrNull { it.periodOrder == period }
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .width(timeColWidth)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = period.toString(),
                                    fontSize = (14 * scaleFactor).coerceAtLeast(10f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textPrimary
                                )
                                if (sampleSlot != null) {
                                    Text(
                                        text = sampleSlot.startTime,
                                        fontSize = (10 * scaleFactor).coerceAtLeast(8f).sp,
                                        color = iosColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Columns for each day (Mon..Sun)
                weekDays.forEach { date ->
                    val dayOfWeek = date.dayOfWeek.value
                    val dateIso = date.toString()
                    val dayPendingTasks = tasks.filter { it.dueDateIso == dateIso && !it.isCompleted }
                    val daySlots = activeSlots.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.parseStartTime() }
                    val daySlotIds = daySlots.map { it.id }.toSet()
                    val firstSlotIdByActivity = daySlots
                        .groupBy { it.activityTypeId }
                        .mapValues { (_, list) -> list.first().id }

                    Column(
                        modifier = Modifier
                            .width(cellWidth)
                            .padding(horizontal = 2.dp)
                    ) {
                        periods.forEach { period ->
                            val slot = activeSlots.firstOrNull { it.dayOfWeek == dayOfWeek && it.periodOrder == period }

                            if (slot != null) {
                                val act = activityMap[slot.activityTypeId]
                                val actColor = ColorParser.parseHexColor(act?.colorHex ?: "#007AFF")
                                val slotTasksOnDate = dayPendingTasks.filter { task ->
                                    when {
                                        task.linkedSlotId == slot.id -> true
                                        task.linkedSlotId == null || task.linkedSlotId !in daySlotIds ->
                                            task.linkedActivityId == slot.activityTypeId &&
                                                firstSlotIdByActivity[slot.activityTypeId] == slot.id
                                        else -> false
                                    }
                                }

                                val cellSurface = actColor
                                    .copy(alpha = if (iosColors.isDark) 0.14f else 0.08f)
                                    .compositeOver(iosColors.card)

                                Box(
                                    modifier = Modifier
                                        .height(cellHeight)
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellSurface)
                                        .border(
                                            BorderStroke(
                                                width = 1.4.dp,
                                                color = actColor.copy(alpha = if (iosColors.isDark) 0.8f else 0.72f)
                                            ),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onEditSlot(slot.id)
                                        }
                                        .padding(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = act?.shortName?.ifBlank { act.name } ?: "Урок",
                                                fontSize = (11f * scaleFactor).coerceAtLeast(8.5f).sp,
                                                lineHeight = (12.5f * scaleFactor).coerceAtLeast(10f).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = iosColors.textPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (slotTasksOnDate.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 2.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(IosOrange)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${slotTasksOnDate.size}",
                                                        fontSize = (8.5f * scaleFactor).coerceAtLeast(7.5f).sp,
                                                        lineHeight = (9.5f * scaleFactor).coerceAtLeast(8.5f).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Column {
                                            val loc = slot.customLocation?.takeIf { it.isNotBlank() } ?: act?.defaultLocation
                                            if (!loc.isNullOrBlank()) {
                                                Text(
                                                    text = loc,
                                                    fontSize = (10 * scaleFactor).coerceAtLeast(8f).sp,
                                                    color = iosColors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${slot.startTime}–${slot.endTime}",
                                                fontSize = (9 * scaleFactor).coerceAtLeast(7f).sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = actColor
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Empty cell -> tap to add slot prefilled with day & period
                                Box(
                                    modifier = Modifier
                                        .height(cellHeight)
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iosColors.cardSecondary.copy(alpha = 0.5f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onAddSlot(dayOfWeek, period)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Добавить в слот",
                                        tint = iosColors.separator,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
