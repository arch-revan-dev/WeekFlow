package com.weekflow.app.presentation.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.CalendarEventOccurrence
import com.weekflow.app.domain.model.EventRepeatType
import com.weekflow.app.presentation.schedule.components.ZoomControlPill
import com.weekflow.app.presentation.schedule.components.twoFingerPinchZoom
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val HOURS_IN_DAY = 24
private const val BASE_HOUR_HEIGHT_DP = 56f
private const val BASE_TIME_GUTTER_DP = 38f
private const val MIN_SCALE = 0.6f
private const val MAX_SCALE = 1.8f

@Composable
fun EventsWeekTableView(
    weekDays: List<LocalDate>,
    occurrences: List<CalendarEventOccurrence>,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddEventWithTime: (String, String?, String?, Boolean) -> Unit,
    onMoveEventOccurrence: (CalendarEventOccurrence, String, String?, String?, Boolean) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isSaturdayWeekend: Boolean = false
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val today = LocalDate.now()
    val now = LocalTime.now()

    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    val hourHeightDpVal = BASE_HOUR_HEIGHT_DP * scaleFactor
    val hourHeightDp = hourHeightDpVal.dp
    val timeGutterDp = (BASE_TIME_GUTTER_DP * scaleFactor).dp
    val headerHeightDp = (40f * scaleFactor).coerceIn(32f, 52f).dp

    val allDayByDate = remember(occurrences) {
        occurrences
            .filter { it.event.isAllDay || it.event.startTime.isNullOrBlank() }
            .groupBy { it.occurrenceDate }
    }
    val hasAnyAllDay = allDayByDate.isNotEmpty()

    val positionedByDate = remember(occurrences, weekDays) {
        val timed = occurrences.filter { !it.event.isAllDay && !it.event.startTime.isNullOrBlank() }
        val byDate = timed.groupBy { it.occurrenceDate }
        weekDays.associateWith { date ->
            val rawItems = byDate[date].orEmpty().map { occ ->
                val start = occ.event.parseStartTime() ?: LocalTime.of(12, 0)
                val end = occ.event.parseEndTime() ?: start.plusHours(1)
                val startMin = start.hour * 60 + start.minute
                val endMin = end.hour * 60 + end.minute
                occ to (startMin to endMin)
            }
            layoutOverlappingEvents(rawItems)
        }
    }

    val earliestTimedStartMin = remember(positionedByDate) {
        positionedByDate.values.flatten().minOfOrNull { it.startMinutes }
    }

    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOriginDayIdx by remember { mutableIntStateOf(0) }
    var dragTargetDayIdx by remember { mutableIntStateOf(0) }
    var dragSnappedSteps15 by remember { mutableIntStateOf(0) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    val verticalScroll = rememberScrollState()
    var lastAutoScrolledWeekStart by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(weekDays.firstOrNull(), earliestTimedStartMin) {
        val weekStart = weekDays.firstOrNull() ?: return@LaunchedEffect
        if (lastAutoScrolledWeekStart != weekStart) {
            lastAutoScrolledWeekStart = weekStart
            val anchorMin = earliestTimedStartMin ?: (8 * 60)
            val targetMin = (anchorMin - 30).coerceIn(0, 23 * 60)
            val targetPx = with(density) {
                ((targetMin.toFloat() / 60f) * hourHeightDpVal).dp.toPx()
            }.toInt()
            verticalScroll.scrollTo(targetPx)
        }
    }

    val currentOnAddEventWithTime by rememberUpdatedState(onAddEventWithTime)
    val currentOnMoveOccurrence by rememberUpdatedState(onMoveEventOccurrence)
    val currentWeekDays by rememberUpdatedState(weekDays)
    val separatorColor = iosColors.separator
    val totalGridHeightDp = (hourHeightDpVal * HOURS_IN_DAY + 48f).dp
    val dayCount = maxOf(1, weekDays.size)
    val todayIdx = weekDays.indexOf(today)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .twoFingerPinchZoom { zoom ->
                scaleFactor = (scaleFactor * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Sticky Top Day Headers (Time Gutter + 7 Equal-Width Day Columns)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iosColors.background)
                    .padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(timeGutterDp))

                weekDays.forEach { date ->
                    val dayOfWeek = date.dayOfWeek.value
                    val isToday = date == today
                    val isWeekend = dayOfWeek == 7 || (isSaturdayWeekend && dayOfWeek == 6)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(headerHeightDp)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) IosBlue.copy(alpha = 0.14f) else iosColors.background)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDateClick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = DateTimeUtils.getDayOfWeekShort(dayOfWeek),
                                fontSize = (10f * scaleFactor).coerceIn(8f, 13f).sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isToday -> IosBlue
                                    isWeekend -> IosRed.copy(alpha = 0.78f)
                                    else -> iosColors.textSecondary
                                },
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = (12.5f * scaleFactor).coerceIn(9.5f, 16f).sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = when {
                                    isToday -> IosBlue
                                    isWeekend -> IosRed.copy(alpha = 0.85f)
                                    else -> iosColors.textPrimary
                                },
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 2. Optional "Весь день" strip ONLY when at least one all-day/untimed event exists
            if (hasAnyAllDay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(iosColors.card.copy(alpha = 0.55f))
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(timeGutterDp)
                            .padding(end = 3.dp, top = 2.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = "Весь\nдень",
                            fontSize = (8.5f * scaleFactor).coerceIn(7f, 11f).sp,
                            lineHeight = (9.5f * scaleFactor).coerceIn(8f, 12f).sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textSecondary,
                            textAlign = TextAlign.End,
                            maxLines = 2
                        )
                    }

                    weekDays.forEach { date ->
                        val dayAllDay = allDayByDate[date].orEmpty()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.5.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val visibleChips = dayAllDay.take(3)
                            visibleChips.forEach { occ ->
                                key("allday_${occ.event.id}_${occ.occurrenceDateIso}") {
                                    AllDayWeekChip(
                                        event = occ.event,
                                        scaleFactor = scaleFactor,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onEditEvent(occ.event)
                                        }
                                    )
                                }
                            }
                            if (dayAllDay.size > 3) {
                                Text(
                                    text = "+${dayAllDay.size - 3}",
                                    fontSize = (8.5f * scaleFactor).coerceIn(7f, 11f).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosBlue,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.7.dp)
                    .background(separatorColor.copy(alpha = 0.55f))
            )

            // 3. Proportional 24h Hourly Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                ) {
                    val gridWidthDp = (maxWidth - timeGutterDp).coerceAtLeast(140.dp)
                    val dayColWidthDp = gridWidthDp / dayCount
                    val dayColWidthPx = with(density) { dayColWidthDp.toPx() }
                    val currentDayColWidthPx by rememberUpdatedState(dayColWidthPx)
                    val currentHourPx by rememberUpdatedState(with(density) { hourHeightDp.toPx() })

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalGridHeightDp)
                    ) {
                        // Left Time-Axis Gutter (00:00..23:00)
                        Box(
                            modifier = Modifier
                                .width(timeGutterDp)
                                .fillMaxHeight()
                        ) {
                            for (hour in 0 until HOURS_IN_DAY) {
                                val yDp = (hour * hourHeightDpVal).dp
                                Text(
                                    text = String.format(Locale.US, "%02d:00", hour),
                                    fontSize = (9.5f * scaleFactor).coerceIn(7f, 12.5f).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosColors.textSecondary,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = yDp + 1.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                        }

                        // 7-Column Hourly Canvas + Positioned Events + Today Red Line
                        Box(
                            modifier = Modifier
                                .width(gridWidthDp)
                                .fillMaxHeight()
                                .drawBehind {
                                    val hourPx = hourHeightDp.toPx()
                                    val hairlinePx = 1.dp.toPx()
                                    val vertLinePx = 0.6.dp.toPx()
                                    val colW = size.width / dayCount.toFloat()

                                    // Vertical day column hairlines
                                    for (col in 0 until dayCount) {
                                        val x = col * colW
                                        drawLine(
                                            color = separatorColor.copy(alpha = 0.35f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = vertLinePx
                                        )
                                    }

                                    // Horizontal 1.dp hour hairlines
                                    for (hour in 0 until HOURS_IN_DAY) {
                                        val y = hour * hourPx
                                        drawLine(
                                            color = separatorColor.copy(alpha = 0.55f),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = hairlinePx
                                        )
                                    }
                                }
                                .pointerInput(hourHeightDpVal, weekDays) {
                                    detectTapGestures { offset ->
                                        val days = currentWeekDays
                                        val hourPx = currentHourPx
                                        if (days.isEmpty() || hourPx <= 0f || size.width <= 0) return@detectTapGestures
                                        val colW = size.width.toFloat() / days.size.toFloat()
                                        val dayIdx = (offset.x / colW).toInt().coerceIn(0, days.lastIndex)
                                        val rawMinutes = ((offset.y / hourPx) * 60f).toInt().coerceIn(0, 23 * 60 + 59)
                                        val snappedHour = (rawMinutes / 60).coerceIn(0, 23)
                                        val startMin = snappedHour * 60
                                        val endMin = (startMin + 60).coerceAtMost(23 * 60 + 59)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnAddEventWithTime(
                                            days[dayIdx].toString(),
                                            formatMinutesOfDay(startMin),
                                            formatMinutesOfDay(endMin),
                                            false
                                        )
                                    }
                                }
                        ) {
                            // Subtle column highlight while dragging across days
                            if (draggingKey != null && dragTargetDayIdx in weekDays.indices) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = dayColWidthDp * dragTargetDayIdx)
                                        .width(dayColWidthDp)
                                        .fillMaxHeight()
                                        .background(IosBlue.copy(alpha = 0.06f))
                                )
                            }

                            // Positioned timed events per day column
                            weekDays.forEachIndexed { dayIndex, date ->
                                val dayItems = positionedByDate[date].orEmpty()
                                val dayXStartDp = dayColWidthDp * dayIndex

                                dayItems.forEach { pos ->
                                    val occ = pos.item
                                    val event = occ.event
                                    val occKey = "${event.id}_${occ.occurrenceDateIso}"

                                    key(occKey) {
                                        val eventColor = ColorParser.parseHexColor(event.colorHex)
                                        val isDraggingThis = draggingKey == occKey

                                        val actualDurationMin = computeEventDurationMinutes(
                                            pos.startMinutes,
                                            pos.endMinutes
                                        )
                                        val visualDurationMin = minOf(
                                            actualDurationMin.coerceAtLeast(25),
                                            (24 * 60 - pos.startMinutes).coerceAtLeast(25)
                                        )
                                        val itemHeightDp = (visualDurationMin.toFloat() / 60f) * hourHeightDpVal

                                        val (effectiveStartMin, effectiveEndMin) = if (isDraggingThis) {
                                            computeClampedDraggedEventTimes(
                                                initialStartMin = pos.startMinutes,
                                                initialActualDurationMin = actualDurationMin,
                                                deltaSteps15 = dragSnappedSteps15
                                            )
                                        } else {
                                            pos.startMinutes to pos.endMinutes
                                        }

                                        val subColWidthDp = dayColWidthDp / pos.totalColumns
                                        val xOffsetDp: Dp = if (isDraggingThis) {
                                            dayColWidthDp * dragTargetDayIdx + 1.dp
                                        } else {
                                            dayXStartDp + subColWidthDp * pos.column + 1.dp
                                        }
                                        val itemWidthDp: Dp = if (isDraggingThis) {
                                            (dayColWidthDp - 2.dp).coerceAtLeast(18.dp)
                                        } else {
                                            (subColWidthDp - 2.dp).coerceAtLeast(14.dp)
                                        }
                                        val yOffsetDp = (effectiveStartMin.toFloat() / 60f) * hourHeightDpVal

                                        val currentOcc by rememberUpdatedState(occ)
                                        val currentStartMin by rememberUpdatedState(pos.startMinutes)
                                        val currentActualDur by rememberUpdatedState(actualDurationMin)
                                        val currentDayIdx by rememberUpdatedState(dayIndex)

                                        val cardSurface = eventColor
                                            .copy(alpha = if (isDraggingThis) 0.30f else if (iosColors.isDark) 0.24f else 0.14f)
                                            .compositeOver(iosColors.card)
                                        val cornerDp = (5f * scaleFactor).coerceIn(3.5f, 8f).dp

                                        Row(
                                            modifier = Modifier
                                                .offset(x = xOffsetDp, y = yOffsetDp.dp)
                                                .width(itemWidthDp)
                                                .height(itemHeightDp.dp)
                                                .zIndex(if (isDraggingThis) 10f else 1f)
                                                .clip(RoundedCornerShape(cornerDp))
                                                .background(cardSurface)
                                                .border(
                                                    width = if (isDraggingThis) 1.8.dp else 0.8.dp,
                                                    color = if (isDraggingThis) IosBlue else eventColor.copy(alpha = 0.65f),
                                                    shape = RoundedCornerShape(cornerDp)
                                                )
                                                .pointerInput(occ.event, occ.occurrenceDateIso, hourHeightDpVal, weekDays) {
                                                    val step15Px = (currentHourPx / 4f).coerceAtLeast(4f)
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            draggingKey = "${currentOcc.event.id}_${currentOcc.occurrenceDateIso}"
                                                            dragOriginDayIdx = currentDayIdx
                                                            dragTargetDayIdx = currentDayIdx
                                                            dragSnappedSteps15 = 0
                                                            dragAccumulatedX = 0f
                                                            dragAccumulatedY = 0f
                                                        },
                                                        onDragEnd = {
                                                            val finalSteps = dragSnappedSteps15
                                                            val finalDayIdx = dragTargetDayIdx
                                                            val originIdx = dragOriginDayIdx
                                                            draggingKey = null
                                                            dragSnappedSteps15 = 0
                                                            dragAccumulatedX = 0f
                                                            dragAccumulatedY = 0f

                                                            val days = currentWeekDays
                                                            if (days.isNotEmpty() && (finalSteps != 0 || finalDayIdx != originIdx)) {
                                                                val targetDate = days[finalDayIdx.coerceIn(0, days.lastIndex)]
                                                                val (newStartMin, newEndMin) = computeClampedDraggedEventTimes(
                                                                    initialStartMin = currentStartMin,
                                                                    initialActualDurationMin = currentActualDur,
                                                                    deltaSteps15 = finalSteps
                                                                )
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                currentOnMoveOccurrence(
                                                                    currentOcc,
                                                                    targetDate.toString(),
                                                                    formatMinutesOfDay(newStartMin),
                                                                    formatMinutesOfDay(newEndMin),
                                                                    false
                                                                )
                                                            }
                                                        },
                                                        onDragCancel = {
                                                            draggingKey = null
                                                            dragSnappedSteps15 = 0
                                                            dragAccumulatedX = 0f
                                                            dragAccumulatedY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragAccumulatedX += dragAmount.x
                                                            dragAccumulatedY += dragAmount.y

                                                            val totalDist = hypot(dragAccumulatedX, dragAccumulatedY)
                                                            val newSteps = if (totalDist < 12f) {
                                                                0
                                                            } else {
                                                                (dragAccumulatedY / step15Px).roundToInt()
                                                            }
                                                            val allowsCrossDayDrag = currentOcc.event.repeatType == EventRepeatType.NONE ||
                                                                currentOcc.event.repeatType == EventRepeatType.WEEKLY
                                                            val colW = currentDayColWidthPx
                                                            val days = currentWeekDays
                                                            val newDayIdx = if (allowsCrossDayDrag && colW > 0f && days.isNotEmpty()) {
                                                                (dragOriginDayIdx + (dragAccumulatedX / colW).roundToInt())
                                                                    .coerceIn(0, days.lastIndex)
                                                            } else {
                                                                dragOriginDayIdx
                                                            }

                                                            if (newSteps != dragSnappedSteps15 || newDayIdx != dragTargetDayIdx) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                dragSnappedSteps15 = newSteps
                                                                dragTargetDayIdx = newDayIdx
                                                            }
                                                        }
                                                    )
                                                }
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onEditEvent(event)
                                                }
                                        ) {
                                            // Left accent bar
                                            Box(
                                                modifier = Modifier
                                                    .width((2.5f * scaleFactor).coerceIn(2f, 3.5f).dp)
                                                    .fillMaxHeight()
                                                    .background(if (isDraggingThis) IosBlue else eventColor)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(
                                                        horizontal = (3f * scaleFactor).coerceIn(1.5f, 5f).dp,
                                                        vertical = (2f * scaleFactor).coerceIn(1f, 4f).dp
                                                    ),
                                                verticalArrangement = Arrangement.Top
                                            ) {
                                                val isRepeating = event.repeatType != EventRepeatType.NONE
                                                val titleSp = (9.5f * scaleFactor).coerceIn(7f, 12.5f)
                                                val timeSp = (8f * scaleFactor).coerceIn(6.2f, 10.5f)
                                                val showTimeHeader = itemHeightDp >= 28f && itemWidthDp >= 26.dp

                                                if (showTimeHeader) {
                                                    val startStr = formatMinutesOfDay(effectiveStartMin)
                                                    val endStr = formatMinutesOfDay(effectiveEndMin)
                                                    val timeLabel = if (itemWidthDp >= 56.dp) {
                                                        "$startStr–$endStr"
                                                    } else {
                                                        startStr
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = timeLabel,
                                                            fontSize = timeSp.sp,
                                                            lineHeight = (timeSp * 1.05f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDraggingThis) IosBlue else eventColor,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Clip,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        if (isRepeating) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Repeat,
                                                                contentDescription = null,
                                                                tint = if (isDraggingThis) IosBlue else eventColor,
                                                                modifier = Modifier.size((9f * scaleFactor).coerceIn(7f, 11.5f).dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = event.title,
                                                        fontSize = titleSp.sp,
                                                        lineHeight = (titleSp * 1.12f).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = iosColors.textPrimary,
                                                        maxLines = if (itemHeightDp >= 52f) 2 else 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = event.title,
                                                            fontSize = titleSp.sp,
                                                            lineHeight = (titleSp * 1.08f).sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = iosColors.textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        if (isRepeating && itemWidthDp >= 28.dp) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Repeat,
                                                                contentDescription = null,
                                                                tint = eventColor,
                                                                modifier = Modifier.size((8.5f * scaleFactor).coerceIn(6.5f, 10.5f).dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (event.location.isNotBlank() && itemHeightDp >= 56f && itemWidthDp >= 38.dp) {
                                                    val locSp = (8f * scaleFactor).coerceIn(6.5f, 10.5f)
                                                    Row(
                                                        modifier = Modifier.padding(top = 1.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.LocationOn,
                                                            contentDescription = null,
                                                            tint = iosColors.textSecondary,
                                                            modifier = Modifier.size((8.5f * scaleFactor).coerceIn(6.5f, 11f).dp)
                                                        )
                                                        Text(
                                                            text = event.location,
                                                            fontSize = locSp.sp,
                                                            lineHeight = (locSp * 1.05f).sp,
                                                            color = iosColors.textSecondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Current-Time Red Dot + Line in Today's Column
                            if (todayIdx >= 0) {
                                val nowMinutes = now.hour * 60 + now.minute
                                val lineYOffsetDp = (nowMinutes.toFloat() / 60f) * hourHeightDpVal
                                val todayStartX = dayColWidthDp * todayIdx

                                Box(
                                    modifier = Modifier
                                        .offset(x = todayStartX, y = lineYOffsetDp.dp)
                                        .width(dayColWidthDp)
                                        .zIndex(5f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-3.5).dp, y = (-3).dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(IosRed)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.5.dp)
                                            .background(IosRed)
                                    )
                                }
                            }
                        }
                    }
                }

                // Unobtrusive centered caption when the whole week has 0 events
                if (occurrences.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = iosColors.card.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.7.dp, separatorColor.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "Событий на этой неделе нет",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Floating Zoom Control Pill [ - | 100% | + ]
        ZoomControlPill(
            percentage = (scaleFactor * 100f).roundToInt(),
            onZoomOut = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scaleFactor = (scaleFactor - 0.15f).coerceIn(MIN_SCALE, MAX_SCALE)
            },
            onReset = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scaleFactor = 1.0f
            },
            onZoomIn = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scaleFactor = (scaleFactor + 0.15f).coerceIn(MIN_SCALE, MAX_SCALE)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun AllDayWeekChip(
    event: CalendarEvent,
    scaleFactor: Float,
    onClick: () -> Unit
) {
    val iosColors = LocalIosColors.current
    val eventColor = ColorParser.parseHexColor(event.colorHex)
    val chipSurface = eventColor
        .copy(alpha = if (iosColors.isDark) 0.26f else 0.15f)
        .compositeOver(iosColors.card)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(chipSurface)
            .border(
                BorderStroke(0.7.dp, eventColor.copy(alpha = 0.65f)),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = event.title,
            fontSize = (9f * scaleFactor).coerceIn(7f, 11.5f).sp,
            fontWeight = FontWeight.SemiBold,
            color = iosColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (event.repeatType != EventRepeatType.NONE) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = null,
                tint = eventColor,
                modifier = Modifier.size((8.5f * scaleFactor).coerceIn(6.5f, 10.5f).dp)
            )
        }
    }
}
