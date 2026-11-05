package com.weekflow.app.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

private data class TimelineRenderItem(
    val id: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val slot: Slot? = null,
    val event: CalendarEvent? = null
)

private data class PositionedRenderItem(
    val item: TimelineRenderItem,
    val columnIndex: Int,
    val totalColumns: Int
)

@Composable
fun TimelineGridView(
    selectedDate: LocalDate,
    slots: List<Slot>,
    events: List<CalendarEvent>,
    activities: List<Activity>,
    activeWeekCycle: WeekCycle,
    isTwoWeekCycleEnabled: Boolean,
    onEditSlot: (slotId: String) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddSlotWithTime: (startTime: String, endTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val activityMap = activities.associateBy { it.id }

    // Full 24 Hours: 00:00 to 23:00
    val startHour = 0
    val endHour = 23
    val hoursCount = 24

    var hourHeightDpVal by remember { mutableFloatStateOf(80f) }
    val hourHeightDp = hourHeightDpVal.dp
    val timeColWidth = 54.dp

    val now = LocalTime.now()
    val isToday = selectedDate == LocalDate.now()

    // Filter slots for selectedDate
    val daySlots = slots.filter { slot ->
        slot.dayOfWeek == selectedDate.dayOfWeek.value &&
            (!isTwoWeekCycleEnabled ||
                slot.weekCycle == WeekCycle.EVERY_WEEK ||
                slot.weekCycle == activeWeekCycle)
    }

    // Filter events for selectedDate
    val dayEvents = events.filter { it.dateIso == selectedDate.toString() && !it.isAllDay }

    // Convert both into unified timeline items
    val renderItems = remember(daySlots, dayEvents) {
        val list = mutableListOf<TimelineRenderItem>()

        daySlots.forEach { slot ->
            val slotStart = slot.parseStartTime()
            val slotEnd = slot.parseEndTime()
            val startMin = slotStart.hour * 60 + slotStart.minute
            val endMin = maxOf(startMin + 20, slotEnd.hour * 60 + slotEnd.minute)
            list.add(
                TimelineRenderItem(
                    id = "slot_${slot.id}",
                    startMinutes = startMin,
                    endMinutes = endMin,
                    slot = slot
                )
            )
        }

        dayEvents.forEach { event ->
            val eventStart = event.parseStartTime() ?: LocalTime.of(12, 0)
            val eventEnd = event.parseEndTime() ?: eventStart.plusHours(1)
            val startMin = eventStart.hour * 60 + eventStart.minute
            val endMin = maxOf(startMin + 20, eventEnd.hour * 60 + eventEnd.minute)
            list.add(
                TimelineRenderItem(
                    id = "event_${event.id}",
                    startMinutes = startMin,
                    endMinutes = endMin,
                    event = event
                )
            )
        }

        list.sortedWith(compareBy({ it.startMinutes }, { -(it.endMinutes - it.startMinutes) }))
    }

    // Assign parallel columns for overlapping items (Side-by-side layout)
    val positionedItems = remember(renderItems) {
        if (renderItems.isEmpty()) return@remember emptyList<PositionedRenderItem>()

        val result = mutableListOf<PositionedRenderItem>()

        // Cluster items that overlap
        val clusters = mutableListOf<MutableList<TimelineRenderItem>>()
        var currentCluster = mutableListOf<TimelineRenderItem>()
        var clusterEnd = -1

        for (item in renderItems) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(item)
                clusterEnd = item.endMinutes
            } else if (item.startMinutes < clusterEnd) {
                currentCluster.add(item)
                clusterEnd = maxOf(clusterEnd, item.endMinutes)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(item)
                clusterEnd = item.endMinutes
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        // Layout columns in each cluster
        for (cluster in clusters) {
            val columnEndTimes = mutableListOf<Int>()
            val clusterPositions = mutableListOf<Pair<TimelineRenderItem, Int>>()

            for (item in cluster) {
                var placedCol = -1
                for (col in columnEndTimes.indices) {
                    if (columnEndTimes[col] <= item.startMinutes) {
                        placedCol = col
                        columnEndTimes[col] = item.endMinutes
                        break
                    }
                }
                if (placedCol == -1) {
                    placedCol = columnEndTimes.size
                    columnEndTimes.add(item.endMinutes)
                }
                clusterPositions.add(item to placedCol)
            }

            val numCols = maxOf(1, columnEndTimes.size)
            for ((item, colIndex) in clusterPositions) {
                result.add(PositionedRenderItem(item, colIndex, numCols))
            }
        }

        result
    }

    val scrollState = rememberScrollState()

    // Auto-scroll to 07:30 or first lesson on launch
    LaunchedEffect(selectedDate) {
        val firstStart = renderItems.minOfOrNull { it.startMinutes } ?: (7 * 60 + 30)
        val targetMin = (firstStart - 30).coerceIn(0, 24 * 60)
        val targetPx = with(density) {
            ((targetMin.toFloat() / 60f) * hourHeightDp.value).dp.toPx()
        }.toInt()
        scrollState.scrollTo(targetPx)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    hourHeightDpVal = (hourHeightDpVal * zoom).coerceIn(40f, 240f)
                }
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val availableWidth = (maxWidth - timeColWidth - 14.dp).coerceAtLeast(100.dp)

            // 1. Adaptive 15-Minute / 30-Minute / 60-Minute Grid Lines & Labels
            Column(modifier = Modifier.fillMaxWidth()) {
                for (hour in startHour..endHour) {
                    when {
                        // High zoom (> 135.dp): full 15-minute grid
                        hourHeightDpVal > 135f -> {
                            val subHeight = hourHeightDp / 4
                            listOf(
                                0 to (0.7f to 12),
                                15 to (0.25f to 9),
                                30 to (0.4f to 10),
                                45 to (0.25f to 9)
                            ).forEach { (minute, meta) ->
                                val (lineAlpha, fontSp) = meta
                                val isHourStart = minute == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(subHeight),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", hour, minute),
                                        fontSize = fontSp.sp,
                                        fontWeight = if (isHourStart) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isHourStart) iosColors.textPrimary else iosColors.textSecondary.copy(alpha = if (minute == 30) 0.8f else 0.6f),
                                        modifier = Modifier
                                            .width(timeColWidth)
                                            .padding(start = 8.dp, top = 1.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(0.5.dp)
                                            .background(iosColors.separator.copy(alpha = lineAlpha))
                                    )
                                }
                            }
                        }

                        // Medium zoom (75.dp..135.dp): 30-minute intervals
                        hourHeightDpVal in 75f..135f -> {
                            val subHeight = hourHeightDp / 2
                            listOf(
                                0 to (0.7f to 12),
                                30 to (0.4f to 10)
                            ).forEach { (minute, meta) ->
                                val (lineAlpha, fontSp) = meta
                                val isHourStart = minute == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(subHeight),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", hour, minute),
                                        fontSize = fontSp.sp,
                                        fontWeight = if (isHourStart) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isHourStart) iosColors.textPrimary else iosColors.textSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .width(timeColWidth)
                                            .padding(start = 8.dp, top = 2.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(0.5.dp)
                                            .background(iosColors.separator.copy(alpha = lineAlpha))
                                    )
                                }
                            }
                        }

                        // Low zoom (< 75.dp): whole hours only
                        else -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeightDp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = String.format("%02d:00", hour),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosColors.textSecondary,
                                    modifier = Modifier
                                        .width(timeColWidth)
                                        .padding(start = 8.dp, top = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(0.5.dp)
                                        .background(iosColors.separator.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Clickable empty areas for adding events/slots matching the active LOD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = timeColWidth, end = 12.dp)
            ) {
                for (hour in startHour..endHour) {
                    when {
                        hourHeightDpVal > 135f -> {
                            val subHeight = hourHeightDp / 4
                            listOf(0, 15, 30, 45).forEach { minute ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(subHeight)
                                        .clickable {
                                            val startStr = String.format("%02d:%02d", hour, minute)
                                            val endMinTotal = minute + 45
                                            val endHourCalc = (hour + endMinTotal / 60) % 24
                                            val endMinCalc = endMinTotal % 60
                                            val endStr = String.format("%02d:%02d", endHourCalc, endMinCalc)
                                            onAddSlotWithTime(startStr, endStr)
                                        }
                                )
                            }
                        }

                        hourHeightDpVal in 75f..135f -> {
                            val subHeight = hourHeightDp / 2
                            listOf(0, 30).forEach { minute ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(subHeight)
                                        .clickable {
                                            val startStr = String.format("%02d:%02d", hour, minute)
                                            val endMinTotal = minute + 45
                                            val endHourCalc = (hour + endMinTotal / 60) % 24
                                            val endMinCalc = endMinTotal % 60
                                            val endStr = String.format("%02d:%02d", endHourCalc, endMinCalc)
                                            onAddSlotWithTime(startStr, endStr)
                                        }
                                )
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeightDp)
                                    .clickable {
                                        val startStr = String.format("%02d:00", hour)
                                        val endStr = String.format("%02d:45", hour)
                                        onAddSlotWithTime(startStr, endStr)
                                    }
                            )
                        }
                    }
                }
            }

            // 3. Render Positioned Items (Slots & Events side-by-side with NO right clipping)
            positionedItems.forEach { pos ->
                val colWidth = availableWidth / pos.totalColumns
                val xOffset = timeColWidth + 4.dp + (colWidth * pos.columnIndex)
                val yOffsetDp = (pos.item.startMinutes.toFloat() / 60f) * hourHeightDp.value
                val durationMin = (pos.item.endMinutes - pos.item.startMinutes).coerceAtLeast(20)
                val itemHeightDp = (durationMin.toFloat() / 60f) * hourHeightDp.value
                val itemWidth = (colWidth - 4.dp).coerceAtLeast(40.dp)

                if (pos.item.slot != null) {
                    val slot = pos.item.slot
                    val act = activityMap[slot.activityTypeId]
                    val actColor = ColorParser.parseHexColor(act?.colorHex ?: "#007AFF")

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffsetDp.dp)
                            .width(itemWidth)
                            .height(itemHeightDp.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(actColor.copy(alpha = 0.16f))
                            .border(1.5.dp, actColor, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditSlot(slot.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = act?.name ?: "Занятие",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${slot.startTime}–${slot.endTime}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = actColor
                                )
                            }

                            val loc = slot.customLocation?.takeIf { it.isNotBlank() } ?: act?.defaultLocation
                            if (!loc.isNullOrBlank() && itemHeightDp >= 44f) {
                                Text(
                                    text = loc,
                                    fontSize = 10.sp,
                                    color = iosColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else if (pos.item.event != null) {
                    val event = pos.item.event
                    val eventColor = ColorParser.parseHexColor(event.colorHex)

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffsetDp.dp)
                            .width(itemWidth)
                            .height(itemHeightDp.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(eventColor.copy(alpha = 0.16f))
                            .border(1.5.dp, eventColor, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditEvent(event)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = event.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${event.startTime ?: ""}–${event.endTime ?: ""}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = eventColor
                            )
                        }
                    }
                }
            }

            // 4. Red Live Time Indicator Line
            if (isToday) {
                val nowMinutes = now.hour * 60 + now.minute
                val lineYOffsetDp = (nowMinutes.toFloat() / 60f) * hourHeightDp.value

                Box(
                    modifier = Modifier
                        .offset(y = lineYOffsetDp.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Red indicator circle
                        Box(
                            modifier = Modifier
                                .offset(x = timeColWidth - 5.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(IosRed)
                        )
                        // Red horizontal line
                        Box(
                            modifier = Modifier
                                .offset(x = timeColWidth - 5.dp)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(IosRed)
                        )
                    }
                }
            }
        }
    }
}
