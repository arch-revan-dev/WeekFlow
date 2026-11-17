package com.weekflow.app.presentation.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.presentation.schedule.components.ZoomControlPill
import com.weekflow.app.presentation.schedule.components.twoFingerPinchZoom
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EventsTimelineGridView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddEventWithTime: (dateIso: String, startTime: String?, endTime: String?, isAllDay: Boolean) -> Unit,
    onMoveEvent: ((event: CalendarEvent, newDateIso: String, newStartTime: String?, newEndTime: String?, newIsAllDay: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val hoursCount = 24
    var hourHeightDpVal by remember { mutableFloatStateOf(80f) }
    val scaleFactor = (hourHeightDpVal / 80f).coerceIn(0.6f, 1.8f)
    val hourHeightDp = hourHeightDpVal.dp
    val timeColWidth = 54.dp

    var draggingEventId by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedPx by remember { mutableFloatStateOf(0f) }
    var dragSnappedSteps15 by remember { mutableIntStateOf(0) }

    val now = LocalTime.now()
    val isToday = selectedDate == LocalDate.now()
    val dateIso = selectedDate.toString()

    val allDayEvents = remember(events, dateIso) {
        events.filter { it.dateIso == dateIso && it.isAllDay }
    }

    val timedEvents = remember(events, dateIso) {
        events.filter { it.dateIso == dateIso && !it.isAllDay }
    }

    val currentOnAddEventWithTime by rememberUpdatedState(onAddEventWithTime)

    val positionedItems = remember(timedEvents) {
        val rawItems = timedEvents.map { event ->
            val eventStart = event.parseStartTime() ?: LocalTime.of(12, 0)
            val eventEnd = event.parseEndTime() ?: eventStart.plusHours(1)
            val startMin = eventStart.hour * 60 + eventStart.minute
            val endMin = eventEnd.hour * 60 + eventEnd.minute
            event to (startMin to endMin)
        }
        layoutOverlappingEvents(rawItems)
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(selectedDate) {
        val firstStart = positionedItems.minOfOrNull { it.startMinutes } ?: (8 * 60)
        val targetMin = (firstStart - 30).coerceIn(0, 24 * 60)
        val targetPx = with(density) {
            ((targetMin.toFloat() / 60f) * hourHeightDpVal).dp.toPx()
        }.toInt()
        scrollState.scrollTo(targetPx)
    }

    val separatorColor = iosColors.separator
    val totalHeightDp = (hourHeightDpVal * hoursCount + 48f).dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
    ) {
        // Optional horizontal "Весь день" strip when selected day has all-day events
        if (allDayEvents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iosColors.card.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Весь день",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary,
                    modifier = Modifier.width(timeColWidth - 6.dp)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allDayEvents.forEach { event ->
                        val eventColor = ColorParser.parseHexColor(event.colorHex)
                        val chipSurface = eventColor
                            .copy(alpha = if (iosColors.isDark) 0.22f else 0.13f)
                            .compositeOver(iosColors.card)

                        Row(
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipSurface)
                                .border(
                                    BorderStroke(1.dp, eventColor.copy(alpha = 0.65f)),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEditEvent(event)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(eventColor)
                            )
                            Text(
                                text = event.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(separatorColor.copy(alpha = 0.6f))
            )
        }

        // Hourly Grid + Zoom Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .twoFingerPinchZoom { zoom ->
                    hourHeightDpVal = (hourHeightDpVal * zoom).coerceIn(48f, 144f)
                }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                val availableWidth = (maxWidth - timeColWidth - 14.dp).coerceAtLeast(100.dp)

                // 1. Canvas Grid + Single Tap Gesture Detector
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
                                if (hourHeightDpVal > 125f) {
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
                        .pointerInput(hourHeightDpVal, dateIso) {
                            detectTapGestures { offset ->
                                val hourPx = hourHeightDp.toPx()
                                if (hourPx <= 0f) return@detectTapGestures
                                val rawMinutes = ((offset.y / hourPx) * 60f).toInt().coerceIn(0, 23 * 60 + 45)
                                val step = when {
                                    hourHeightDpVal > 125f -> 15
                                    hourHeightDpVal >= 75f -> 30
                                    else -> 60
                                }
                                val snappedMinutes = (rawMinutes / step) * step
                                val startH = (snappedMinutes / 60).coerceIn(0, 23)
                                val startM = snappedMinutes % 60
                                val endTotal = (snappedMinutes + 60).coerceAtMost(23 * 60 + 59)
                                val endH = endTotal / 60
                                val endM = endTotal % 60
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnAddEventWithTime(
                                    dateIso,
                                    formatMinutesOfDay(snappedMinutes),
                                    formatMinutesOfDay(endTotal),
                                    false
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
                        if (hourHeightDpVal > 125f) {
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

                // 2. Render Positioned Events
                positionedItems.forEach { pos ->
                    val event = pos.item
                    key(event.id) {
                        val eventColor = ColorParser.parseHexColor(event.colorHex)
                        val colWidth = availableWidth / pos.totalColumns
                        val xOffset = timeColWidth + 4.dp + (colWidth * pos.column)
                        val actualDurationMin = computeEventDurationMinutes(pos.startMinutes, pos.endMinutes)
                        val visualDurationMin = minOf(
                            actualDurationMin.coerceAtLeast(25),
                            (24 * 60 - pos.startMinutes).coerceAtLeast(25)
                        )
                        val itemHeightDp = (visualDurationMin.toFloat() / 60f) * hourHeightDpVal
                        val itemWidth = (colWidth - 4.dp).coerceAtLeast(44.dp)

                        val isDraggingThis = draggingEventId == event.id
                        val (effectiveStartMin, effectiveEndMin) = if (isDraggingThis) {
                            computeClampedDraggedEventTimes(
                                initialStartMin = pos.startMinutes,
                                initialActualDurationMin = actualDurationMin,
                                deltaSteps15 = dragSnappedSteps15
                            )
                        } else {
                            pos.startMinutes to pos.endMinutes
                        }
                        val effectiveYOffsetDp = (effectiveStartMin.toFloat() / 60f) * hourHeightDpVal

                        val displayStartStr = formatMinutesOfDay(effectiveStartMin)
                        val displayEndStr = formatMinutesOfDay(effectiveEndMin)

                        val currentEvent by rememberUpdatedState(event)
                        val currentStartMinutes by rememberUpdatedState(pos.startMinutes)
                        val currentActualDurationMin by rememberUpdatedState(actualDurationMin)
                        val currentOnMoveEvent by rememberUpdatedState(onMoveEvent)
                        val currentDateIso by rememberUpdatedState(dateIso)

                        val vertPadDp = (4f * scaleFactor).coerceIn(2f, 7f).dp
                        val horizPadDp = (6.5f * scaleFactor).coerceIn(4f, 10f).dp
                        val titleFontSp = (11.5f * scaleFactor).coerceIn(8f, 15f)
                        val timeFontSp = (9.5f * scaleFactor).coerceIn(7f, 13f)
                        val cornerDp = (9f * scaleFactor).coerceIn(6f, 12f).dp
                        val cardBg = eventColor
                            .copy(alpha = if (isDraggingThis) 0.28f else if (iosColors.isDark) 0.22f else 0.14f)
                            .compositeOver(iosColors.card)

                        Row(
                            modifier = Modifier
                                .offset(x = xOffset, y = effectiveYOffsetDp.dp)
                                .width(itemWidth)
                                .height(itemHeightDp.dp)
                                .zIndex(if (isDraggingThis) 10f else 1f)
                                .clip(RoundedCornerShape(cornerDp))
                                .background(cardBg)
                                .border(
                                    width = if (isDraggingThis) 2.2.dp else 1.2.dp,
                                    color = if (isDraggingThis) IosBlue else eventColor.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(cornerDp)
                                )
                                .pointerInput(event, hourHeightDpVal, dateIso) {
                                    val step15Px = (hourHeightDp.toPx() / 4f).coerceAtLeast(4f)
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingEventId = currentEvent.id
                                            dragAccumulatedPx = 0f
                                            dragSnappedSteps15 = 0
                                        },
                                        onDragEnd = {
                                            val steps = dragSnappedSteps15
                                            draggingEventId = null
                                            dragAccumulatedPx = 0f
                                            dragSnappedSteps15 = 0
                                            val moveHandler = currentOnMoveEvent
                                            if (steps != 0 && moveHandler != null) {
                                                val (newStartMin, newEndMin) = computeClampedDraggedEventTimes(
                                                    initialStartMin = currentStartMinutes,
                                                    initialActualDurationMin = currentActualDurationMin,
                                                    deltaSteps15 = steps
                                                )
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                moveHandler(
                                                    currentEvent,
                                                    currentDateIso,
                                                    formatMinutesOfDay(newStartMin),
                                                    formatMinutesOfDay(newEndMin),
                                                    false
                                                )
                                            }
                                        },
                                        onDragCancel = {
                                            draggingEventId = null
                                            dragAccumulatedPx = 0f
                                            dragSnappedSteps15 = 0
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumulatedPx += dragAmount.y
                                            val candidateSteps = if (abs(dragAccumulatedPx) < 12f) {
                                                0
                                            } else {
                                                (dragAccumulatedPx / step15Px).roundToInt()
                                            }
                                            if (candidateSteps != dragSnappedSteps15) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                dragSnappedSteps15 = candidateSteps
                                            }
                                        }
                                    )
                                }
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEditEvent(event)
                                }
                                .padding(horizontal = horizPadDp, vertical = vertPadDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width((3.5f * scaleFactor).coerceIn(2.5f, 4.5f).dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isDraggingThis) IosBlue else eventColor)
                            )
                            Spacer(modifier = Modifier.width((5f * scaleFactor).coerceIn(3f, 8f).dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = event.title,
                                        fontSize = titleFontSp.sp,
                                        lineHeight = (titleFontSp * 1.12f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosColors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$displayStartStr–$displayEndStr",
                                        fontSize = timeFontSp.sp,
                                        lineHeight = (timeFontSp * 1.05f).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDraggingThis) IosBlue else eventColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                if (event.location.isNotBlank() && itemHeightDp >= 44f) {
                                    val locFontSp = (9.5f * scaleFactor).coerceIn(7.5f, 12.5f)
                                    Row(
                                        modifier = Modifier.padding(top = (1.5f * scaleFactor).coerceIn(1f, 3f).dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = iosColors.textSecondary,
                                            modifier = Modifier.size((10f * scaleFactor).coerceIn(8f, 13f).dp)
                                        )
                                        Text(
                                            text = event.location,
                                            fontSize = locFontSp.sp,
                                            lineHeight = (locFontSp * 1.1f).sp,
                                            color = iosColors.textSecondary,
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

                // 3. Red Live Time Indicator Line
                if (isToday) {
                    val nowMinutes = now.hour * 60 + now.minute
                    val lineYOffsetDp = (nowMinutes.toFloat() / 60f) * hourHeightDpVal

                    Box(
                        modifier = Modifier
                            .offset(y = lineYOffsetDp.dp)
                            .fillMaxWidth()
                            .zIndex(5f)
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
                    hourHeightDpVal = (hourHeightDpVal - 12f).coerceIn(48f, 144f)
                },
                onReset = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hourHeightDpVal = 80f
                },
                onZoomIn = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    hourHeightDpVal = (hourHeightDpVal + 12f).coerceIn(48f, 144f)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp)
            )
        }
    }
}
