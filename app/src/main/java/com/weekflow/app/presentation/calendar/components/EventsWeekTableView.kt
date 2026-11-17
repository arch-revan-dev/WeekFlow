package com.weekflow.app.presentation.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.presentation.schedule.components.ZoomControlPill
import com.weekflow.app.presentation.schedule.components.twoFingerPinchZoom
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun EventsWeekTableView(
    weekDays: List<LocalDate>,
    events: List<CalendarEvent>,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddEventWithTime: (dateIso: String, startTime: String?, endTime: String?, isAllDay: Boolean) -> Unit,
    onMoveEvent: ((event: CalendarEvent, newDateIso: String, newStartTime: String?, newEndTime: String?, newIsAllDay: Boolean) -> Unit)? = null,
    onDateClick: ((LocalDate) -> Unit)? = null,
    isSaturdayWeekend: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val today = LocalDate.now()

    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    var draggingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var dragOriginBand by remember { mutableStateOf(EventTimeBand.ALL_DAY) }
    var dragOriginDayIdx by remember { mutableIntStateOf(0) }
    var dragOriginBandIdx by remember { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetCell by remember { mutableStateOf<Pair<LocalDate, EventTimeBand>?>(null) }

    val bands = remember { EventTimeBand.entries }

    val eventsByDayAndBand = remember(events, weekDays) {
        val grouped = mutableMapOf<Pair<String, EventTimeBand>, List<CalendarEvent>>()
        val byKey = events.groupBy { it.dateIso to it.toTimeBand() }
        byKey.forEach { (key, list) ->
            grouped[key] = list.sortedWith(
                compareBy<CalendarEvent>(
                    { !it.isAllDay },
                    { it.parseStartTime() ?: LocalTime.MIN },
                    { it.title }
                )
            )
        }
        grouped
    }

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val cellWidth = (112f * scaleFactor).dp
    val timeColWidth = (74f * scaleFactor).coerceIn(54f, 118f).dp
    val cellHeight = (118f * scaleFactor).dp
    val headerHeight = (44f * scaleFactor).coerceAtLeast(34f).dp
    val cellPaddingDp = (5f * scaleFactor).coerceIn(3f, 8f).dp
    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }
    val currentOnMoveEvent by rememberUpdatedState(onMoveEvent)
    val currentWeekDays by rememberUpdatedState(weekDays)
    val currentCellWidthPx by rememberUpdatedState(cellWidthPx)
    val currentCellHeightPx by rememberUpdatedState(cellHeightPx)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosColors.background)
            .twoFingerPinchZoom { zoom ->
                scaleFactor = (scaleFactor * zoom).coerceIn(0.6f, 1.8f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // 1. Sticky Top Header Row: Time-band corner + Horizontally Scrollable Days (Пн 16 .. Вс 22)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(headerHeight)
                        .width(timeColWidth)
                        .background(iosColors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Время",
                        fontSize = (11f * scaleFactor).coerceIn(8.5f, 14f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDays.forEach { date ->
                        val dayOfWeek = date.dayOfWeek.value
                        val isToday = date == today
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
                            val dayNameSize = (12f * scaleFactor).coerceIn(9f, 16f)
                            val dayNumSize = (11f * scaleFactor).coerceIn(9f, 15f)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${DateTimeUtils.getDayOfWeekShort(dayOfWeek)} ${date.dayOfMonth}",
                                    fontSize = dayNameSize.sp,
                                    lineHeight = (dayNameSize * 1.15f).sp,
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
                                val dayCount = events.count { it.dateIso == date.toString() }
                                if (dayCount > 0) {
                                    Text(
                                        text = "$dayCount соб.",
                                        fontSize = (dayNumSize * 0.85f).coerceAtLeast(8f).sp,
                                        lineHeight = (dayNumSize * 0.95f).sp,
                                        color = if (isToday) IosBlue else iosColors.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Grid Body: Sticky Left Band Column + 2D Scrollable Day Cells
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Sticky Left Band Column
                Column(
                    modifier = Modifier
                        .width(timeColWidth)
                        .background(iosColors.background)
                ) {
                    bands.forEach { band ->
                        Box(
                            modifier = Modifier
                                .height(cellHeight)
                                .width(timeColWidth)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = band.label,
                                    fontSize = (11.5f * scaleFactor).coerceIn(8.5f, 15f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (band == EventTimeBand.ALL_DAY) IosBlue else iosColors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = (13f * scaleFactor).coerceIn(10f, 17f).sp
                                )
                                if (band.subtitle.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = band.subtitle,
                                        fontSize = (9f * scaleFactor).coerceIn(7f, 12f).sp,
                                        color = iosColors.textSecondary,
                                        textAlign = TextAlign.Center,
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
                        val dateIso = date.toString()

                        Column(
                            modifier = Modifier
                                .width(cellWidth)
                                .padding(horizontal = 2.dp)
                        ) {
                            bands.forEachIndexed { bandIndex, band ->
                                val cellEvents = eventsByDayAndBand[dateIso to band].orEmpty()
                                val isDropTarget = hoveredTargetCell?.first == date && hoveredTargetCell?.second == band
                                val cornerDp = (10f * scaleFactor).coerceIn(6f, 12f).dp

                                if (cellEvents.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .height(cellHeight)
                                            .fillMaxWidth()
                                            .padding(vertical = (2.5f * scaleFactor).coerceIn(1.5f, 3.5f).dp)
                                            .clip(RoundedCornerShape(cornerDp))
                                            .background(
                                                if (isDropTarget) IosBlue.copy(alpha = 0.22f)
                                                else iosColors.cardSecondary.copy(alpha = 0.45f)
                                            )
                                            .then(
                                                if (isDropTarget) {
                                                    Modifier.border(
                                                        BorderStroke(2.dp, IosBlue),
                                                        RoundedCornerShape(cornerDp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAddEventWithTime(
                                                    dateIso,
                                                    if (band.isAllDay) null else band.defaultStart,
                                                    if (band.isAllDay) null else band.defaultEnd,
                                                    band.isAllDay
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Добавить событие",
                                            tint = if (isDropTarget) IosBlue else iosColors.separator,
                                            modifier = Modifier.size((16f * scaleFactor).coerceIn(11f, 20f).dp)
                                        )
                                    }
                                } else {
                                    val maxVisible = if (scaleFactor < 0.8f) 2 else 3
                                    val visibleEvents = cellEvents.take(maxVisible)
                                    val extraCount = cellEvents.size - visibleEvents.size

                                    Box(
                                        modifier = Modifier
                                            .height(cellHeight)
                                            .fillMaxWidth()
                                            .padding(vertical = (2.5f * scaleFactor).coerceIn(1.5f, 3.5f).dp)
                                            .clip(RoundedCornerShape(cornerDp))
                                            .background(
                                                if (isDropTarget) IosBlue.copy(alpha = 0.22f)
                                                else iosColors.cardSecondary.copy(alpha = 0.25f)
                                            )
                                            .then(
                                                if (isDropTarget) {
                                                    Modifier.border(
                                                        BorderStroke(2.dp, IosBlue),
                                                        RoundedCornerShape(cornerDp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAddEventWithTime(
                                                    dateIso,
                                                    if (band.isAllDay) null else band.defaultStart,
                                                    if (band.isAllDay) null else band.defaultEnd,
                                                    band.isAllDay
                                                )
                                            }
                                            .padding((3.5f * scaleFactor).coerceIn(2f, 6f).dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy((3f * scaleFactor).coerceIn(2f, 5f).dp)
                                        ) {
                                            visibleEvents.forEach { event ->
                                                key(event.id) {
                                                    val eventColor = ColorParser.parseHexColor(event.colorHex)
                                                    val isBeingDragged = draggingEvent?.id == event.id
                                                    val cardSurface = eventColor
                                                        .copy(alpha = if (iosColors.isDark) 0.20f else 0.12f)
                                                        .compositeOver(iosColors.card)
                                                    val itemCorner = (7f * scaleFactor).coerceIn(4f, 10f).dp

                                                    val currentEvent by rememberUpdatedState(event)
                                                    val currentBand by rememberUpdatedState(band)
                                                    val currentDayIndex by rememberUpdatedState(dayIndex)
                                                    val currentBandIndex by rememberUpdatedState(bandIndex)
                                                    val currentDate by rememberUpdatedState(date)

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(IntrinsicSize.Min)
                                                            .clip(RoundedCornerShape(itemCorner))
                                                            .background(cardSurface)
                                                            .border(
                                                                BorderStroke(
                                                                    width = if (isBeingDragged) 1.8.dp else 0.9.dp,
                                                                    color = if (isBeingDragged) IosBlue else eventColor.copy(alpha = 0.65f)
                                                                ),
                                                                RoundedCornerShape(itemCorner)
                                                            )
                                                            .alpha(if (isBeingDragged) 0.45f else 1f)
                                                            .pointerInput(event, scaleFactor, weekDays) {
                                                                detectDragGesturesAfterLongPress(
                                                                    onDragStart = {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        draggingEvent = currentEvent
                                                                        dragOriginBand = currentBand
                                                                        dragOriginDayIdx = currentDayIndex
                                                                        dragOriginBandIdx = currentBandIndex
                                                                        dragOffsetPx = Offset.Zero
                                                                        hoveredTargetCell = currentDate to currentBand
                                                                    },
                                                                    onDragEnd = {
                                                                        val target = hoveredTargetCell
                                                                        val dragged = draggingEvent
                                                                        val originBand = dragOriginBand
                                                                        val movedDistance = dragOffsetPx.getDistance()
                                                                        draggingEvent = null
                                                                        hoveredTargetCell = null
                                                                        dragOffsetPx = Offset.Zero
                                                                        val moveHandler = currentOnMoveEvent
                                                                        if (dragged != null && target != null && movedDistance > 18f && moveHandler != null) {
                                                                            val targetDateIso = target.first.toString()
                                                                            val targetBand = target.second
                                                                            if (targetDateIso != dragged.dateIso || targetBand != originBand) {
                                                                                val (newStart, newEnd) = computeMovedBandTimes(
                                                                                    event = dragged,
                                                                                    targetBand = targetBand
                                                                                )
                                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                                moveHandler(
                                                                                    dragged,
                                                                                    targetDateIso,
                                                                                    newStart,
                                                                                    newEnd,
                                                                                    targetBand.isAllDay
                                                                                )
                                                                            }
                                                                        }
                                                                    },
                                                                    onDragCancel = {
                                                                        draggingEvent = null
                                                                        hoveredTargetCell = null
                                                                        dragOffsetPx = Offset.Zero
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        dragOffsetPx += dragAmount
                                                                        val wPx = currentCellWidthPx
                                                                        val hPx = currentCellHeightPx
                                                                        val days = currentWeekDays
                                                                        if (wPx > 0f && hPx > 0f && days.isNotEmpty()) {
                                                                            val deltaCol = (dragOffsetPx.x / wPx).roundToInt()
                                                                            val deltaRow = (dragOffsetPx.y / hPx).roundToInt()
                                                                            val newDayIdx = (dragOriginDayIdx + deltaCol).coerceIn(0, days.lastIndex)
                                                                            val newBandIdx = (dragOriginBandIdx + deltaRow).coerceIn(0, bands.lastIndex)
                                                                            val newTarget = days[newDayIdx] to bands[newBandIdx]
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
                                                                onEditEvent(event)
                                                            }
                                                            .padding(cellPaddingDp * 0.7f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width((3f * scaleFactor).coerceIn(2f, 4f).dp)
                                                                .fillMaxHeight()
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(eventColor)
                                                        )
                                                        Spacer(modifier = Modifier.width((4f * scaleFactor).coerceIn(2.5f, 6f).dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            val titleFontSp = (10.5f * scaleFactor).coerceIn(7.5f, 14f)
                                                            val metaFontSp = (8.8f * scaleFactor).coerceIn(6.5f, 12f)
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                if (!event.isAllDay && !event.startTime.isNullOrBlank()) {
                                                                    Text(
                                                                        text = event.startTime,
                                                                        fontSize = metaFontSp.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = eventColor,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                }
                                                                Text(
                                                                    text = event.title,
                                                                    fontSize = titleFontSp.sp,
                                                                    lineHeight = (titleFontSp * 1.1f).sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = iosColors.textPrimary,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                            if (event.location.isNotBlank() && scaleFactor >= 0.78f && visibleEvents.size <= 2) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.LocationOn,
                                                                        contentDescription = null,
                                                                        tint = iosColors.textSecondary,
                                                                        modifier = Modifier.size((9.5f * scaleFactor).coerceIn(7f, 12f).dp)
                                                                    )
                                                                    Text(
                                                                        text = event.location,
                                                                        fontSize = metaFontSp.sp,
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
                                            if (extraCount > 0) {
                                                Text(
                                                    text = "+$extraCount ещё",
                                                    fontSize = (9f * scaleFactor).coerceIn(7f, 12f).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IosBlue,
                                                    modifier = Modifier.padding(start = 2.dp)
                                                )
                                            }
                                        }
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
                scaleFactor = (scaleFactor - 0.15f).coerceIn(0.6f, 1.8f)
            },
            onReset = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scaleFactor = 1.0f
            },
            onZoomIn = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scaleFactor = (scaleFactor + 0.15f).coerceIn(0.6f, 1.8f)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp)
        )
    }
}
