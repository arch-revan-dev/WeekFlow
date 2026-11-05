package com.weekflow.app.presentation.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

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
    val activityMap = remember(activities) { activities.associateBy { it.id } }

    // Full 24 Hours: 00:00 to 23:00
    val hoursCount = 24
    var hourHeightDpVal by remember { mutableFloatStateOf(80f) }
    val hourHeightDp = hourHeightDpVal.dp
    val timeColWidth = 54.dp

    val now = LocalTime.now()
    val isToday = selectedDate == LocalDate.now()

    val daySlots = remember(slots, selectedDate, isTwoWeekCycleEnabled, activeWeekCycle) {
        slots.filter { slot ->
            slot.dayOfWeek == selectedDate.dayOfWeek.value &&
                (!isTwoWeekCycleEnabled ||
                    slot.weekCycle == WeekCycle.EVERY_WEEK ||
                    slot.weekCycle == activeWeekCycle)
        }
    }

    val dayEvents = remember(events, selectedDate) {
        events.filter { it.dateIso == selectedDate.toString() && !it.isAllDay }
    }

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

    val positionedItems = remember(renderItems) {
        if (renderItems.isEmpty()) return@remember emptyList<PositionedRenderItem>()

        val result = mutableListOf<PositionedRenderItem>()
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

    LaunchedEffect(selectedDate) {
        val firstStart = renderItems.minOfOrNull { it.startMinutes } ?: (7 * 60 + 30)
        val targetMin = (firstStart - 30).coerceIn(0, 24 * 60)
        val targetPx = with(density) {
            ((targetMin.toFloat() / 60f) * hourHeightDpVal).dp.toPx()
        }.toInt()
        scrollState.scrollTo(targetPx)
    }

    val separatorColor = iosColors.separator
    val totalHeightDp = (hourHeightDpVal * hoursCount + 48f).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .twoFingerPinchZoom { zoom ->
                hourHeightDpVal = (hourHeightDpVal * zoom).coerceIn(44f, 220f)
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val availableWidth = (maxWidth - timeColWidth - 14.dp).coerceAtLeast(100.dp)

            // 1. Hardware-accelerated Canvas Grid + Single Tap Gesture Detector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
                    .drawBehind {
                        val hourPx = hourHeightDp.toPx()
                        val startX = timeColWidth.toPx()
                        val endX = size.width
                        val strokePx = 0.7.dp.toPx()

                        for (hour in 0 until hoursCount) {
                            val yHour = hour * hourPx
                            drawLine(
                                color = separatorColor.copy(alpha = 0.65f),
                                start = Offset(startX, yHour),
                                end = Offset(endX, yHour),
                                strokeWidth = strokePx
                            )
                            if (hourHeightDpVal >= 75f) {
                                val yHalf = yHour + hourPx * 0.5f
                                drawLine(
                                    color = separatorColor.copy(alpha = 0.35f),
                                    start = Offset(startX, yHalf),
                                    end = Offset(endX, yHalf),
                                    strokeWidth = strokePx
                                )
                            }
                            if (hourHeightDpVal > 135f) {
                                val yQ1 = yHour + hourPx * 0.25f
                                val yQ3 = yHour + hourPx * 0.75f
                                drawLine(
                                    color = separatorColor.copy(alpha = 0.20f),
                                    start = Offset(startX, yQ1),
                                    end = Offset(endX, yQ1),
                                    strokeWidth = strokePx
                                )
                                drawLine(
                                    color = separatorColor.copy(alpha = 0.20f),
                                    start = Offset(startX, yQ3),
                                    end = Offset(endX, yQ3),
                                    strokeWidth = strokePx
                                )
                            }
                        }
                    }
                    .pointerInput(hourHeightDpVal) {
                        detectTapGestures { offset ->
                            val hourPx = hourHeightDp.toPx()
                            if (hourPx <= 0f) return@detectTapGestures
                            val rawMinutes = ((offset.y / hourPx) * 60f).toInt().coerceIn(0, 23 * 60 + 45)
                            val step = when {
                                hourHeightDpVal > 135f -> 15
                                hourHeightDpVal >= 75f -> 30
                                else -> 60
                            }
                            val snappedMinutes = (rawMinutes / step) * step
                            val startH = (snappedMinutes / 60).coerceIn(0, 23)
                            val startM = snappedMinutes % 60
                            val endTotal = (snappedMinutes + 45).coerceAtMost(23 * 60 + 59)
                            val endH = endTotal / 60
                            val endM = endTotal % 60
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddSlotWithTime(
                                String.format("%02d:%02d", startH, startM),
                                String.format("%02d:%02d", endH, endM)
                            )
                        }
                    }
            ) {
                // Time labels along the left column
                for (hour in 0 until hoursCount) {
                    val yHourDp = (hour * hourHeightDpVal).dp
                    Text(
                        text = String.format("%02d:00", hour),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary,
                        modifier = Modifier
                            .offset(x = 8.dp, y = yHourDp + 1.dp)
                            .width(timeColWidth - 8.dp)
                    )
                    if (hourHeightDpVal >= 85f) {
                        Text(
                            text = String.format("%02d:30", hour),
                            fontSize = 9.5.sp,
                            color = iosColors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .offset(x = 8.dp, y = yHourDp + (hourHeightDpVal * 0.5f).dp + 1.dp)
                                .width(timeColWidth - 8.dp)
                        )
                    }
                    if (hourHeightDpVal > 140f) {
                        Text(
                            text = String.format("%02d:15", hour),
                            fontSize = 8.5.sp,
                            color = iosColors.textSecondary.copy(alpha = 0.55f),
                            modifier = Modifier
                                .offset(x = 8.dp, y = yHourDp + (hourHeightDpVal * 0.25f).dp + 1.dp)
                                .width(timeColWidth - 8.dp)
                        )
                        Text(
                            text = String.format("%02d:45", hour),
                            fontSize = 8.5.sp,
                            color = iosColors.textSecondary.copy(alpha = 0.55f),
                            modifier = Modifier
                                .offset(x = 8.dp, y = yHourDp + (hourHeightDpVal * 0.75f).dp + 1.dp)
                                .width(timeColWidth - 8.dp)
                        )
                    }
                }
            }

            // 2. Render Positioned Items (Slots & Events side-by-side)
            positionedItems.forEach { pos ->
                val colWidth = availableWidth / pos.totalColumns
                val xOffset = timeColWidth + 4.dp + (colWidth * pos.columnIndex)
                val yOffsetDp = (pos.item.startMinutes.toFloat() / 60f) * hourHeightDpVal
                val durationMin = (pos.item.endMinutes - pos.item.startMinutes).coerceAtLeast(20)
                val itemHeightDp = (durationMin.toFloat() / 60f) * hourHeightDpVal
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

            // 3. Red Live Time Indicator Line
            if (isToday) {
                val nowMinutes = now.hour * 60 + now.minute
                val lineYOffsetDp = (nowMinutes.toFloat() / 60f) * hourHeightDpVal

                Box(
                    modifier = Modifier
                        .offset(y = lineYOffsetDp.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .offset(x = timeColWidth - 5.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(IosRed)
                        )
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

        // 4. Floating Zoom Control Pill [ - | 100% | + ]
        ZoomControlPill(
            percentage = ((hourHeightDpVal / 80f) * 100f).roundToInt(),
            onZoomOut = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                hourHeightDpVal = (hourHeightDpVal - 16f).coerceIn(44f, 220f)
            },
            onReset = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                hourHeightDpVal = 80f
            },
            onZoomIn = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                hourHeightDpVal = (hourHeightDpVal + 16f).coerceIn(44f, 220f)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp)
        )
    }
}
