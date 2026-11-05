package com.weekflow.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.util.DateTimeUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/**
 * Reusable iOS-style vertical drum/wheel picker with snapping, center highlight bar,
 * haptic feedback on scroll, and tap-to-scroll on adjacent items.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> IosWheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int, T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 5,
    itemHeight: Dp = 42.dp
) {
    if (items.isEmpty()) return
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val halfVisible = visibleItemsCount / 2
    val safeInitialIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPx = with(density) { itemHeight.toPx() }

    val centerIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = if (offset > itemHeightPx / 2f) {
                listState.firstVisibleItemIndex + 1
            } else {
                listState.firstVisibleItemIndex
            }
            idx.coerceIn(0, items.lastIndex)
        }
    }

    // Sync external selectedIndex changes (e.g. from quick preset buttons)
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        if (!listState.isScrollInProgress && centerIndex != target) {
            listState.animateScrollToItem(target)
        }
    }

    // Emit selection & haptic feedback when scrolling settles or crosses an item
    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { idx ->
                if (idx in items.indices) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onItemSelected(idx, items[idx])
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Center selection highlight bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(if (iosColors.isDark) Color(0xFF383840) else Color(0xFFE5E5EC))
                .border(
                    width = 1.dp,
                    color = IosBlue.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val distance = abs(index - centerIndex)
                val isCenter = index == centerIndex
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.28f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(alpha)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                                onItemSelected(index, items[index])
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(items[index]),
                        fontSize = if (isCenter) 19.sp else 15.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCenter) iosColors.textPrimary else iosColors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Clickable iOS-style Time Picker field button that opens [IosWheelTimePickerDialog].
 */
@Composable
fun IosTimePickerButton(
    label: String,
    timeString: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }

    val surfaceColor = if (iosColors.isDark) Color(0xFF26262C) else Color(0xFFF2F2F7)
    val borderColor = if (iosColors.isDark) Color(0xFF3E3E46) else Color(0xFFD8D8E0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showDialog = true
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = timeString.ifBlank { "09:00" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
            }
        }
    }

    if (showDialog) {
        IosWheelTimePickerDialog(
            title = label,
            initialTime = timeString,
            onDismiss = { showDialog = false },
            onConfirm = { selected ->
                onTimeSelected(selected)
                showDialog = false
            }
        )
    }
}

/**
 * Dialog with vertical scrolling wheels for Hours (00..23) and Minutes (00..59).
 */
@Composable
fun IosWheelTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    val parsed = remember(initialTime) {
        runCatching { LocalTime.parse(initialTime.trim()) }.getOrDefault(LocalTime.of(9, 0))
    }

    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    var selectedHour by remember { mutableIntStateOf(parsed.hour) }
    var selectedMinute by remember { mutableIntStateOf(parsed.minute) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = iosColors.card,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )

                // Live Digital Preview Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(IosBlue.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IosBlue
                    )
                }

                // Dual Wheel Row: Hours : Minutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Часы",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IosWheelPicker(
                            items = hours,
                            selectedIndex = selectedHour,
                            onItemSelected = { _, h -> selectedHour = h },
                            itemLabel = { String.format("%02d", it) },
                            visibleItemsCount = 5,
                            itemHeight = 40.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = ":",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .padding(top = 18.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Минуты",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IosWheelPicker(
                            items = minutes,
                            selectedIndex = selectedMinute,
                            onItemSelected = { _, m -> selectedMinute = m },
                            itemLabel = { String.format("%02d", it) },
                            visibleItemsCount = 5,
                            itemHeight = 40.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Quick minute chips (:00, :15, :30, :45)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { minPreset ->
                        val isChosen = selectedMinute == minPreset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isChosen) IosBlue else iosColors.cardSecondary
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedMinute = minPreset
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(":%02d", minPreset),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isChosen) Color.White else iosColors.textPrimary
                            )
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Отмена", color = iosColors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onConfirm(String.format("%02d:%02d", selectedHour, selectedMinute))
                        },
                        colors = iosPrimaryButtonColors(IosBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Готово", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Clickable iOS-style Date Picker field button that opens [IosCalendarDatePickerDialog].
 */
@Composable
fun IosDatePickerButton(
    label: String,
    dateIso: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }

    val parsedDate = remember(dateIso) {
        runCatching { LocalDate.parse(dateIso.trim()) }.getOrDefault(LocalDate.now())
    }
    val formattedText = remember(parsedDate) {
        "${DateTimeUtils.formatDateWithDay(parsedDate)} ${parsedDate.year}"
    }

    val surfaceColor = if (iosColors.isDark) Color(0xFF26262C) else Color(0xFFF2F2F7)
    val borderColor = if (iosColors.isDark) Color(0xFF3E3E46) else Color(0xFFD8D8E0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showDialog = true
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formattedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
            }
        }
    }

    if (showDialog) {
        IosCalendarDatePickerDialog(
            title = label,
            initialDateIso = dateIso,
            onDismiss = { showDialog = false },
            onConfirm = { selectedIso ->
                onDateSelected(selectedIso)
                showDialog = false
            }
        )
    }
}

/**
 * Interactive Calendar Date Picker Dialog ("календарик") with month navigation,
 * quick date pills (Today, Tomorrow, Next Monday), and selectable day cells.
 */
@Composable
fun IosCalendarDatePickerDialog(
    title: String,
    initialDateIso: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val today = LocalDate.now()

    val initialDate = remember(initialDateIso) {
        runCatching { LocalDate.parse(initialDateIso.trim()) }.getOrDefault(today)
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = iosColors.card,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Title & Selected Date Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosBlue.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.formatDateWithDay(selectedDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }
                }

                // Quick Jump Chips (Сегодня, Завтра, След. Пн)
                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Сегодня" to today,
                        "Завтра" to today.plusDays(1),
                        "След. Пн" to nextMonday
                    ).forEach { (chipLabel, chipDate) ->
                        val isSelected = selectedDate == chipDate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) IosBlue else iosColors.cardSecondary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDate = chipDate
                                    displayedMonth = YearMonth.from(chipDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chipLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else iosColors.textPrimary
                            )
                        }
                    }
                }

                // Month & Year Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц",
                            tint = IosBlue
                        )
                    }

                    Text(
                        text = DateTimeUtils.formatMonthYearHeader(displayedMonth.atDay(1)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = IosBlue
                        )
                    }
                }

                // Weekday Labels (Пн..Вс)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { idx, dayLabel ->
                        Text(
                            text = dayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (idx >= 5) IosRed.copy(alpha = 0.8f) else iosColors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Calendar Days Grid
                val firstOfMonth = displayedMonth.atDay(1)
                val daysInMonth = displayedMonth.lengthOfMonth()
                val leadingEmptyCells = firstOfMonth.dayOfWeek.value - 1
                val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dow in 0 until 7) {
                                val cellIndex = week * 7 + dow
                                val dayNumber = cellIndex - leadingEmptyCells + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = displayedMonth.atDay(dayNumber)
                                    val isChosen = cellDate == selectedDate
                                    val isTodayCell = cellDate == today
                                    val isWeekend = dow >= 5

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isChosen -> IosBlue
                                                    isTodayCell -> IosBlue.copy(alpha = 0.14f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isTodayCell && !isChosen) {
                                                    Modifier.border(1.dp, IosBlue, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedDate = cellDate
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isChosen || isTodayCell) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isChosen -> Color.White
                                                isTodayCell -> IosBlue
                                                isWeekend -> IosRed.copy(alpha = 0.85f)
                                                else -> iosColors.textPrimary
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Отмена", color = iosColors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirm(selectedDate.toString()) },
                        colors = iosPrimaryButtonColors(IosBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Выбрать", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
