package com.weekflow.app.presentation.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosPurple
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyInsightsSheet(
    activePreset: SchedulePreset,
    activities: List<Activity>,
    slots: List<Slot>,
    tasks: List<Task>,
    activeWeekCycle: WeekCycle,
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val today = LocalDate.now()

    val presetActivities = activities.filter { it.presetId == activePreset.id }
    val activityMap = presetActivities.associateBy { it.id }

    val activeSlots = slots.filter { slot ->
        slot.presetId == activePreset.id &&
            (!preferences.isTwoWeekCycleEnabled ||
                slot.weekCycle == WeekCycle.EVERY_WEEK ||
                slot.weekCycle == activeWeekCycle)
    }

    val totalWeeklyMinutes = activeSlots.sumOf { slot ->
        Duration.between(slot.parseStartTime(), slot.parseEndTime()).toMinutes().coerceAtLeast(0)
    }
    val totalWeeklyHours = totalWeeklyMinutes / 60
    val remWeeklyMinutes = totalWeeklyMinutes % 60

    val slotsByDay = (1..7).associateWith { day ->
        activeSlots.count { it.dayOfWeek == day }
    }
    val maxSlotsInADay = (slotsByDay.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val busiestDayEntry = slotsByDay.maxByOrNull { it.value }
    val dayNames = mapOf(
        1 to "Понедельник",
        2 to "Вторник",
        3 to "Среда",
        4 to "Четверг",
        5 to "Пятница",
        6 to "Суббота",
        7 to "Воскресенье"
    )
    val shortDayNames = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")

    val presetTasks = if (preferences.showAllPresetsTasks) tasks else tasks.filter { it.presetId == activePreset.id }
    val completedCount = presetTasks.count { it.isCompleted }
    val pendingCount = presetTasks.count { !it.isCompleted }
    val overdueCount = presetTasks.count { task ->
        !task.isCompleted && task.parseDueDate()?.isBefore(today) == true
    }
    val completionRatio = if (presetTasks.isNotEmpty()) {
        completedCount.toFloat() / presetTasks.size.toFloat()
    } else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Аналитика и продуктивность",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )
                    Text(
                        text = "Пресет: ${activePreset.iconEmoji} ${activePreset.name}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorParser.parseHexColor(activePreset.colorHex)
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", fontWeight = FontWeight.SemiBold, color = IosBlue)
                }
            }

            // 1. KPI Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightStatCard(
                    title = "Занятий в нед.",
                    value = "${activeSlots.size}",
                    subtitle = "${totalWeeklyHours}ч ${remWeeklyMinutes}м всего",
                    accentColor = IosBlue,
                    modifier = Modifier.weight(1f)
                )
                InsightStatCard(
                    title = "Выполнено задач",
                    value = "${(completionRatio * 100).roundToInt()}%",
                    subtitle = "$completedCount из ${presetTasks.size}",
                    accentColor = IosGreen,
                    modifier = Modifier.weight(1f)
                )
                InsightStatCard(
                    title = "Фокус-сессии",
                    value = "${preferences.focusSessionsCompleted}",
                    subtitle = "${preferences.focusMinutesTotal} мин в фокусе",
                    accentColor = IosPurple,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Weekly Load Bar Chart (Mon..Sun)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(iosColors.cardSecondary)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "РАСПРЕДЕЛЕНИЕ ЗАНЯТИЙ ПО ДНЯМ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textSecondary
                        )
                        if (busiestDayEntry != null && busiestDayEntry.value > 0) {
                            Text(
                                text = "Пик: ${dayNames[busiestDayEntry.key]} (${busiestDayEntry.value})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosOrange
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        shortDayNames.forEach { (dayNum, dayLabel) ->
                            val count = slotsByDay[dayNum] ?: 0
                            val fraction = (count.toFloat() / maxSlotsInADay.toFloat()).coerceIn(0.08f, 1f)
                            val isToday = today.dayOfWeek.value == dayNum
                            val barColor = when {
                                count == 0 -> iosColors.separator
                                isToday -> IosBlue
                                count == busiestDayEntry?.value -> IosOrange
                                else -> IosBlue.copy(alpha = 0.65f)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(
                                    text = if (count > 0) "$count" else "–",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (count > 0) iosColors.textPrimary else iosColors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height((60 * fraction).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) IosBlue else iosColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Tasks Breakdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(iosColors.cardSecondary)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ПРОГРЕСС ПО ЗАДАЧАМ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textSecondary
                    )
                    LinearProgressIndicator(
                        progress = { completionRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = IosGreen,
                        trackColor = iosColors.card
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Активных: $pendingCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = "Просрочено: $overdueCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (overdueCount > 0) IosRed else iosColors.textSecondary
                        )
                        Text(
                            text = "Готово: $completedCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosGreen
                        )
                    }
                }
            }

            // 4. Top Subjects by Weekly Hours
            if (activeSlots.isNotEmpty()) {
                val slotsBySubject = activeSlots
                    .groupBy { it.activityTypeId }
                    .mapNotNull { (actId, subjectSlots) ->
                        val act = activityMap[actId] ?: return@mapNotNull null
                        val mins = subjectSlots.sumOf {
                            Duration.between(it.parseStartTime(), it.parseEndTime()).toMinutes().coerceAtLeast(0)
                        }
                        Triple(act, subjectSlots.size, mins)
                    }
                    .sortedByDescending { it.third }
                    .take(5)

                if (slotsBySubject.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(iosColors.cardSecondary)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ТОП ПРЕДМЕТОВ ПО НАГРУЗКЕ В НЕДЕЛЮ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textSecondary
                            )
                            slotsBySubject.forEach { (act, count, mins) ->
                                val actColor = ColorParser.parseHexColor(act.colorHex)
                                val hoursStr = if (mins >= 60) "${mins / 60}ч ${mins % 60}м" else "${mins}м"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(actColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = act.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = iosColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$count зан. • $hoursStr",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(iosColors.cardSecondary)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = iosColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
