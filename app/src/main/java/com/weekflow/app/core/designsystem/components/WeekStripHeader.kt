package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import java.time.LocalDate

@Composable
fun WeekStripHeader(
    selectedDate: LocalDate,
    weekDays: List<LocalDate>,
    dayIndicators: Map<LocalDate, List<String>>, // date to list of hex colors
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isSaturdayWeekend: Boolean = false,
    taskCountsByDate: Map<LocalDate, Int> = emptyMap(),
    onPreviousWeek: (() -> Unit)? = null,
    onNextWeek: (() -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val today = LocalDate.now()
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onPreviousWeek != null && onNextWeek != null) {
                    Modifier.pointerInput(weekDays) {
                        detectHorizontalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onDragEnd = {
                                if (accumulatedDrag < -60f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNextWeek()
                                } else if (accumulatedDrag > 60f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPreviousWeek()
                                }
                                accumulatedDrag = 0f
                            },
                            onDragCancel = { accumulatedDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDrag += dragAmount
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val dots = dayIndicators[date] ?: emptyList()
            val taskCount = taskCountsByDate[date] ?: 0
            val isWeekend = date.dayOfWeek.value == 7 || (isSaturdayWeekend && date.dayOfWeek.value == 6)

            val dayBgColor by animateColorAsState(
                targetValue = if (isSelected) IosBlue else if (isToday) IosBlue.copy(alpha = 0.1f) else Color.Transparent,
                label = "DayBgColor"
            )
            val textColor = when {
                isSelected -> Color.White
                isToday -> IosBlue
                isWeekend -> IosRed.copy(alpha = 0.8f)
                else -> iosColors.textPrimary
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(dayBgColor)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDateSelected(date)
                    }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Day of week (Пн, Вт...)
                    Text(
                        text = DateTimeUtils.getDayOfWeekShort(date.dayOfWeek.value),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isSelected -> Color.White.copy(alpha = 0.85f)
                            isToday -> IosBlue
                            isWeekend -> IosRed.copy(alpha = 0.8f)
                            else -> iosColors.textSecondary
                        }
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Day of month number
                    Text(
                        text = "${date.dayOfMonth}",
                        fontSize = 15.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dots & Task indicator row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(8.dp)
                    ) {
                        if (taskCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color.White else IosOrange)
                                    .padding(horizontal = 3.5.dp, vertical = 0.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$taskCount",
                                    fontSize = 7.5.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) IosBlue else Color.White
                                )
                            }
                        } else if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        } else {
                            dots.take(3).forEach { dotHex ->
                                val dotColor = ColorParser.parseHexColor(dotHex)
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
