package com.weekflow.app.presentation.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Multi-touch pinch-to-zoom modifier that intercepts ONLY when 2 or more fingers are on screen
 * (`PointerEventPass.Initial`), never interfering with 1-finger horizontal or vertical scrolling.
 */
internal fun Modifier.twoFingerPinchZoom(
    onZoom: (Float) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f && !zoom.isNaN() && zoom in 0.5f..2.0f) {
                    // Dampen slightly for ultra-smooth control
                    val dampedZoom = 1f + (zoom - 1f) * 0.85f
                    if (abs(dampedZoom - 1f) > 0.002f) {
                        onZoom(dampedZoom)
                    }
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
fun WeekTableGridView(
    weekDays: List<LocalDate>,
    slots: List<Slot>,
    activities: List<Activity>,
    activeWeekCycle: WeekCycle,
    isTwoWeekCycleEnabled: Boolean,
    onEditSlot: (slotId: String) -> Unit,
    onAddSlot: (dayOfWeek: Int, periodOrder: Int) -> Unit,
    onMoveSlot: ((slot: Slot, targetDayOfWeek: Int, targetPeriodOrder: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSaturdayWeekend: Boolean = false,
    tasks: List<Task> = emptyList(),
    onDateClick: ((LocalDate) -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val activityMap = remember(activities) { activities.associateBy { it.id } }

    var scaleFactor by remember { mutableFloatStateOf(0.95f) }
    var draggingSlot by remember { mutableStateOf<Slot?>(null) }
    var dragOriginDayIdx by remember { mutableStateOf(0) }
    var dragOriginPeriodIdx by remember { mutableStateOf(0) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetCell by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (dayOfWeek, periodOrder)

    val activeSlots = remember(slots, isTwoWeekCycleEnabled, activeWeekCycle) {
        slots.filter { slot ->
            !isTwoWeekCycleEnabled ||
                slot.weekCycle == WeekCycle.EVERY_WEEK ||
                slot.weekCycle == activeWeekCycle
        }
    }

    val periods = remember(activeSlots) {
        val maxPeriod = maxOf(6, (activeSlots.maxOfOrNull { it.periodOrder } ?: 6))
        val numbered = (1..maxPeriod).toList()
        val hasUnnumbered = activeSlots.any { it.periodOrder <= 0 }
        if (hasUnnumbered) numbered + listOf(0) else numbered
    }

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val cellWidth = (106f * scaleFactor).dp
    val timeColWidth = (48f * scaleFactor).coerceAtLeast(36f).dp
    val cellHeight = (88f * scaleFactor).dp
    val headerHeight = (44f * scaleFactor).coerceAtLeast(34f).dp
    val cellPaddingDp = (6f * scaleFactor).coerceIn(3f, 8f).dp
    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .twoFingerPinchZoom { zoom ->
                scaleFactor = (scaleFactor * zoom).coerceIn(0.55f, 1.55f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // 1. Sticky Top Header Row: Fixed top-left "№" + Horizontally Scrollable Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sticky Top-Left "№" Corner
                Box(
                    modifier = Modifier
                        .height(headerHeight)
                        .width(timeColWidth)
                        .background(iosColors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "№",
                        fontSize = (12f * scaleFactor).coerceAtLeast(9f).sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textSecondary
                    )
                }

                // Horizontally Scrollable Day Headers
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Grid Body: Sticky Left Period Column + 2D Scrollable Day Cells
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Sticky Left Period Column
                Column(
                    modifier = Modifier
                        .width(timeColWidth)
                        .background(iosColors.background)
                ) {
                    periods.forEach { period ->
                        val sampleSlot = activeSlots.firstOrNull {
                            if (period <= 0) it.periodOrder <= 0 else it.periodOrder == period
                        }
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .width(timeColWidth)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (period <= 0) "Вне" else period.toString(),
                                    fontSize = (if (period <= 0) 11f * scaleFactor else 14f * scaleFactor).coerceAtLeast(9f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (period <= 0) IosBlue else iosColors.textPrimary
                                )
                                if (sampleSlot != null) {
                                    Text(
                                        text = sampleSlot.startTime,
                                        fontSize = (9.5f * scaleFactor).coerceAtLeast(7f).sp,
                                        lineHeight = (10.5f * scaleFactor).coerceAtLeast(8f).sp,
                                        color = iosColors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(64.dp))
                }

                // Horizontally Scrollable Columns for each day (Mon..Sun)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    weekDays.forEachIndexed { dayIndex, date ->
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
                            periods.forEachIndexed { periodIndex, period ->
                                val matchingSlots = activeSlots.filter {
                                    it.dayOfWeek == dayOfWeek &&
                                        (if (period <= 0) it.periodOrder <= 0 else it.periodOrder == period)
                                }.sortedBy { it.parseStartTime() }
                                val slot = matchingSlots.firstOrNull()
                                val isDropTarget = hoveredTargetCell?.first == dayOfWeek && hoveredTargetCell?.second == period

                                if (slot != null) {
                                    val act = activityMap[slot.activityTypeId]
                                    val actColor = ColorParser.parseHexColor(act?.colorHex ?: "#007AFF")
                                    val isBeingDragged = draggingSlot?.id == slot.id
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
                                        .copy(alpha = if (iosColors.isDark) 0.16f else 0.10f)
                                        .compositeOver(iosColors.card)

                                    Box(
                                        modifier = Modifier
                                            .height(cellHeight)
                                            .fillMaxWidth()
                                            .padding(vertical = (2.5f * scaleFactor).coerceIn(1.5f, 3.5f).dp)
                                            .clip(RoundedCornerShape((10f * scaleFactor).coerceIn(6f, 12f).dp))
                                            .background(
                                                if (isDropTarget) IosBlue.copy(alpha = 0.25f) else cellSurface
                                            )
                                            .border(
                                                BorderStroke(
                                                    width = if (isDropTarget || isBeingDragged) 2.2.dp else 1.4.dp,
                                                    color = if (isDropTarget) IosBlue else actColor.copy(alpha = if (iosColors.isDark) 0.8f else 0.72f)
                                                ),
                                                RoundedCornerShape((10f * scaleFactor).coerceIn(6f, 12f).dp)
                                            )
                                            .alpha(if (isBeingDragged) 0.45f else 1f)
                                            .pointerInput(slot.id, scaleFactor, periods.size) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        draggingSlot = slot
                                                        dragOriginDayIdx = dayIndex
                                                        dragOriginPeriodIdx = periodIndex
                                                        dragOffsetPx = Offset.Zero
                                                        hoveredTargetCell = dayOfWeek to period
                                                    },
                                                    onDragEnd = {
                                                        val target = hoveredTargetCell
                                                        val dragged = draggingSlot
                                                        val movedDistance = dragOffsetPx.getDistance()
                                                        draggingSlot = null
                                                        hoveredTargetCell = null
                                                        dragOffsetPx = Offset.Zero
                                                        // Require at least 18px movement to avoid false triggers on long-press release
                                                        if (dragged != null && target != null && movedDistance > 18f && onMoveSlot != null) {
                                                            if (target.first != dragged.dayOfWeek || target.second != dragged.periodOrder) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                onMoveSlot(dragged, target.first, target.second)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        draggingSlot = null
                                                        hoveredTargetCell = null
                                                        dragOffsetPx = Offset.Zero
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetPx += dragAmount
                                                        if (cellWidthPx > 0f && cellHeightPx > 0f) {
                                                            val deltaCol = (dragOffsetPx.x / cellWidthPx).roundToInt()
                                                            val deltaRow = (dragOffsetPx.y / cellHeightPx).roundToInt()
                                                            val newDayIdx = (dragOriginDayIdx + deltaCol).coerceIn(0, weekDays.lastIndex)
                                                            val newPeriodIdx = (dragOriginPeriodIdx + deltaRow).coerceIn(0, periods.lastIndex)
                                                            val newTarget = weekDays[newDayIdx].dayOfWeek.value to periods[newPeriodIdx]
                                                            if (newTarget != hoveredTargetCell) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                hoveredTargetCell = newTarget
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onEditSlot(slot.id)
                                            }
                                            .padding(cellPaddingDp)
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
                                                val titleFontSp = (11f * scaleFactor).coerceIn(7.5f, 15f)
                                                Text(
                                                    text = act?.shortName?.ifBlank { act.name } ?: "Урок",
                                                    fontSize = titleFontSp.sp,
                                                    lineHeight = (titleFontSp * 1.12f).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = iosColors.textPrimary,
                                                    maxLines = if (scaleFactor < 0.78f) 1 else 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (slotTasksOnDate.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = 2.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(IosOrange)
                                                            .padding(
                                                                horizontal = (3.5f * scaleFactor).coerceAtLeast(2.5f).dp,
                                                                vertical = 0.5.dp
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${slotTasksOnDate.size}",
                                                            fontSize = (8.5f * scaleFactor).coerceIn(6.5f, 11f).sp,
                                                            lineHeight = (9.5f * scaleFactor).coerceIn(7.5f, 12f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }

                                            Column {
                                                val loc = slot.customLocation?.takeIf { it.isNotBlank() } ?: act?.defaultLocation
                                                if (!loc.isNullOrBlank() && scaleFactor >= 0.70f) {
                                                    val locFontSp = (9.5f * scaleFactor).coerceIn(7f, 13f)
                                                    Text(
                                                        text = loc,
                                                        fontSize = locFontSp.sp,
                                                        lineHeight = (locFontSp * 1.1f).sp,
                                                        color = iosColors.textSecondary,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val timeFontSp = (8.8f * scaleFactor).coerceIn(6.5f, 12.5f)
                                                    Text(
                                                        text = "${slot.startTime}–${slot.endTime}",
                                                        fontSize = timeFontSp.sp,
                                                        lineHeight = (timeFontSp * 1.05f).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = actColor,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Clip
                                                    )
                                                    if (matchingSlots.size > 1) {
                                                        Text(
                                                            text = "+${matchingSlots.size - 1}",
                                                            fontSize = (8f * scaleFactor).coerceIn(6.5f, 11f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = actColor,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty cell -> tap to add slot prefilled with day & period (or drop target)
                                    Box(
                                        modifier = Modifier
                                            .height(cellHeight)
                                            .fillMaxWidth()
                                            .padding(vertical = (2.5f * scaleFactor).coerceIn(1.5f, 3.5f).dp)
                                            .clip(RoundedCornerShape((10f * scaleFactor).coerceIn(6f, 12f).dp))
                                            .background(
                                                if (isDropTarget) IosBlue.copy(alpha = 0.22f)
                                                else iosColors.cardSecondary.copy(alpha = 0.5f)
                                            )
                                            .then(
                                                if (isDropTarget) {
                                                    Modifier.border(
                                                        BorderStroke(2.dp, IosBlue),
                                                        RoundedCornerShape((10f * scaleFactor).coerceIn(6f, 12f).dp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAddSlot(dayOfWeek, period)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Добавить в слот",
                                            tint = if (isDropTarget) IosBlue else iosColors.separator,
                                            modifier = Modifier.size((16f * scaleFactor).coerceIn(11f, 20f).dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        // 3. Floating Zoom Control Pill [ - | 100% | + ]
        ZoomControlPill(
            percentage = (scaleFactor * 100f).roundToInt(),
            onZoomOut = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scaleFactor = (scaleFactor - 0.12f).coerceIn(0.55f, 1.55f)
            },
            onReset = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scaleFactor = 0.95f
            },
            onZoomIn = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scaleFactor = (scaleFactor + 0.12f).coerceIn(0.55f, 1.55f)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp)
        )
    }
}

@Composable
internal fun ZoomControlPill(
    percentage: Int,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = iosColors.card.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = BorderStroke(0.8.dp, iosColors.separator.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(34.dp)
                .padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onZoomOut() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Отдалить",
                    tint = iosColors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosBlue
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onZoomIn() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Приблизить",
                    tint = iosColors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
