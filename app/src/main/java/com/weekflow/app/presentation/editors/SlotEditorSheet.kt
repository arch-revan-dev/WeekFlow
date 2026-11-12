package com.weekflow.app.presentation.editors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.IosSegmentedControl
import com.weekflow.app.core.designsystem.components.IosTimePickerButton
import com.weekflow.app.core.designsystem.components.IosWheelPicker
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalTime
import java.util.UUID
import kotlin.math.abs

private fun resolveLessonTimesForPeriod(
    targetPeriod: Int,
    targetDayOfWeek: Int,
    existingSlots: List<Slot>,
    fallbackPresets: List<Pair<String, String>>,
    defaultDurationMin: Int = 45,
    defaultBreakMin: Int = 10
): Pair<String, String> {
    if (targetPeriod <= 0) {
        return Pair("15:00", "16:30")
    }

    val numberedSlots = existingSlots.filter { it.periodOrder > 0 }
    if (numberedSlots.isEmpty()) {
        return fallbackPresets.getOrElse(targetPeriod - 1) {
            val baseStartMin = 8 * 60 + 30 + (targetPeriod - 1) * (defaultDurationMin + defaultBreakMin)
            val start = LocalTime.of((baseStartMin / 60) % 24, baseStartMin % 60)
            val end = start.plusMinutes(defaultDurationMin.toLong())
            String.format("%02d:%02d", start.hour, start.minute) to String.format("%02d:%02d", end.hour, end.minute)
        }
    }

    val exactDayMatch = numberedSlots.firstOrNull {
        it.dayOfWeek == targetDayOfWeek && it.periodOrder == targetPeriod
    }
    if (exactDayMatch != null) {
        return exactDayMatch.startTime to exactDayMatch.endTime
    }

    val samePeriodAcrossDays = numberedSlots.filter { it.periodOrder == targetPeriod }
    if (samePeriodAcrossDays.isNotEmpty()) {
        val mostCommonPair = samePeriodAcrossDays
            .groupingBy { it.startTime to it.endTime }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        if (mostCommonPair != null) {
            return mostCommonPair
        }
    }

    val durations = numberedSlots.mapNotNull { slot ->
        val mins = Duration.between(slot.parseStartTime(), slot.parseEndTime()).toMinutes().toInt()
        mins.takeIf { it in 20..240 }
    }
    val lessonDurationMin = durations
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: defaultDurationMin

    val breaks = mutableListOf<Int>()
    numberedSlots.groupBy { it.dayOfWeek }.values.forEach { dayList ->
        val sortedByPeriod = dayList.sortedBy { it.periodOrder }.distinctBy { it.periodOrder }
        for (i in 0 until sortedByPeriod.size - 1) {
            val cur = sortedByPeriod[i]
            val next = sortedByPeriod[i + 1]
            if (next.periodOrder == cur.periodOrder + 1) {
                val gap = Duration.between(cur.parseEndTime(), next.parseStartTime()).toMinutes().toInt()
                if (gap in 0..90) {
                    breaks.add(gap)
                }
            }
        }
    }
    val breakDurationMin = breaks
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: defaultBreakMin

    val dayPreceding = numberedSlots
        .filter { it.dayOfWeek == targetDayOfWeek && it.periodOrder < targetPeriod }
        .maxByOrNull { it.periodOrder }
    val anyPreceding = numberedSlots
        .filter { it.periodOrder < targetPeriod }
        .maxByOrNull { it.periodOrder }
    val anchorBefore = if (dayPreceding != null && (anyPreceding == null || dayPreceding.periodOrder >= anyPreceding.periodOrder)) {
        dayPreceding
    } else {
        anyPreceding ?: dayPreceding
    }

    if (anchorBefore != null) {
        val steps = (targetPeriod - anchorBefore.periodOrder).coerceAtLeast(1)
        val anchorEnd = anchorBefore.parseEndTime()
        val offsetToStartMin = breakDurationMin.toLong() + (steps - 1L) * (lessonDurationMin + breakDurationMin).toLong()
        val newStart = anchorEnd.plusMinutes(offsetToStartMin)
        val newEnd = newStart.plusMinutes(lessonDurationMin.toLong())
        return String.format("%02d:%02d", newStart.hour, newStart.minute) to
            String.format("%02d:%02d", newEnd.hour, newEnd.minute)
    }

    val anchorAfter = numberedSlots
        .filter { it.periodOrder > targetPeriod }
        .minByOrNull { it.periodOrder }
    if (anchorAfter != null) {
        val steps = (anchorAfter.periodOrder - targetPeriod).coerceAtLeast(1)
        val anchorStart = anchorAfter.parseStartTime()
        val offsetBackMin = steps.toLong() * (lessonDurationMin + breakDurationMin).toLong()
        val newStart = anchorStart.minusMinutes(offsetBackMin)
        val newEnd = newStart.plusMinutes(lessonDurationMin.toLong())
        return String.format("%02d:%02d", newStart.hour, newStart.minute) to
            String.format("%02d:%02d", newEnd.hour, newEnd.minute)
    }

    return Pair("09:00", "10:35")
}

private fun parseRepsInt(reps: String?): Int {
    if (reps.isNullOrBlank()) return 0
    val value = Regex("\\d+").find(reps)?.value?.toIntOrNull() ?: return 0
    return value.coerceIn(0, 100)
}

private fun formatDurationRu(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return when {
        mins > 0 && secs > 0 -> "$mins мин $secs сек"
        mins > 0 -> "$mins мин"
        else -> "$secs сек"
    }
}

private fun formatWorkoutExerciseSummary(item: SubtaskItem): String {
    val parts = mutableListOf<String>()
    val sets = item.sets ?: 0
    if (sets > 0) {
        if (!item.reps.isNullOrBlank()) {
            parts.add("$sets × ${item.reps}")
        } else {
            parts.add(pluralizeRu(sets, "подход", "подхода", "подходов"))
        }
    } else if (!item.reps.isNullOrBlank()) {
        parts.add(item.reps)
    }
    if ((item.durationSeconds ?: 0) > 0) parts.add("работа ${formatDurationRu(item.durationSeconds!!)}")
    if ((item.restSeconds ?: 0) > 0) parts.add("перерыв ${formatDurationRu(item.restSeconds!!)}")
    return parts.joinToString(" • ")
}

@Composable
private fun SlotTimeWheelPairCard(
    title: String,
    minutes: Int,
    seconds: Int,
    minuteOptions: List<Int>,
    secondOptions: List<Int>,
    zeroBadgeText: String,
    onMinutesSelected: (Int) -> Unit,
    onSecondsSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val totalSec = minutes * 60 + seconds
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(iosColors.cardSecondary.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            Text(
                text = if (totalSec > 0) String.format("%02d:%02d", minutes, seconds) else zeroBadgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalSec > 0) IosBlue else iosColors.textSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val minIdx = minuteOptions.indexOf(minutes).coerceAtLeast(0)
            val secIdx = secondOptions.indices.minByOrNull { abs(secondOptions[it] - seconds) } ?: 0
            IosWheelPicker(
                items = minuteOptions,
                selectedIndex = minIdx,
                onItemSelected = { _, m -> onMinutesSelected(m) },
                itemLabel = { m -> String.format("%02d", m) },
                visibleItemsCount = 3,
                itemHeight = 32.dp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ":",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = iosColors.textPrimary
            )
            IosWheelPicker(
                items = secondOptions,
                selectedIndex = secIdx,
                onItemSelected = { _, s -> onSecondsSelected(s) },
                itemLabel = { s -> String.format("%02d", s) },
                visibleItemsCount = 3,
                itemHeight = 32.dp,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("мин", fontSize = 10.sp, color = iosColors.textSecondary)
            Text("сек", fontSize = 10.sp, color = iosColors.textSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotEditorSheet(
    initialSlot: Slot?,
    defaultDayOfWeek: Int,
    activities: List<Activity>,
    isTwoWeekCycleEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Slot) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onAddNewActivityRequest: () -> Unit,
    onEditActivityRequest: ((String) -> Unit)? = null,
    onDuplicateSlot: ((Slot, Int) -> Unit)? = null,
    defaultPeriodOrder: Int? = null,
    defaultStartTime: String? = null,
    defaultEndTime: String? = null,
    defaultActivityId: String? = null,
    existingSlots: List<Slot> = emptyList(),
    presetId: String = SchedulePreset.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current
    val haptic = LocalHapticFeedback.current

    val timePresets = appProfile.defaultBellTimes

    val initialPeriod = remember(initialSlot?.id, defaultPeriodOrder, defaultDayOfWeek, existingSlots.size, appProfile) {
        when {
            initialSlot != null -> initialSlot.periodOrder.coerceAtLeast(if (!appProfile.defaultUsePeriodNumbers) 1 else 0)
            defaultPeriodOrder != null -> defaultPeriodOrder
            defaultStartTime != null -> 1
            else -> {
                val maxOnDay = existingSlots
                    .filter { it.dayOfWeek == defaultDayOfWeek && it.periodOrder > 0 }
                    .maxOfOrNull { it.periodOrder } ?: 0
                (maxOnDay + 1).coerceIn(1, 20)
            }
        }
    }
    val defaultTimesForPeriod = remember(initialPeriod, defaultDayOfWeek, existingSlots, appProfile) {
        resolveLessonTimesForPeriod(
            targetPeriod = initialPeriod,
            targetDayOfWeek = initialSlot?.dayOfWeek ?: defaultDayOfWeek,
            existingSlots = existingSlots,
            fallbackPresets = timePresets,
            defaultDurationMin = appProfile.defaultDurationMinutes,
            defaultBreakMin = appProfile.defaultBreakMinutes
        )
    }

    var selectedActivityId by remember(initialSlot?.id, defaultActivityId) {
        mutableStateOf(
            defaultActivityId?.takeIf { id -> activities.any { it.id == id } }
                ?: initialSlot?.activityTypeId
                ?: activities.firstOrNull()?.id
                ?: ""
        )
    }

    LaunchedEffect(activities.size, defaultActivityId) {
        if (defaultActivityId != null && activities.any { it.id == defaultActivityId }) {
            selectedActivityId = defaultActivityId
        } else if (selectedActivityId.isBlank() && activities.isNotEmpty()) {
            selectedActivityId = activities.first().id
        }
    }

    var dayOfWeek by remember(initialSlot?.id, defaultDayOfWeek) {
        mutableIntStateOf(initialSlot?.dayOfWeek ?: defaultDayOfWeek)
    }
    var periodOrder by remember(initialSlot?.id, defaultPeriodOrder) {
        mutableIntStateOf(initialPeriod)
    }
    var startTime by remember(initialSlot?.id, defaultStartTime, defaultPeriodOrder) {
        mutableStateOf(initialSlot?.startTime ?: defaultStartTime ?: defaultTimesForPeriod.first)
    }
    var endTime by remember(initialSlot?.id, defaultEndTime, defaultPeriodOrder) {
        mutableStateOf(initialSlot?.endTime ?: defaultEndTime ?: defaultTimesForPeriod.second)
    }
    var weekCycle by remember(initialSlot?.id) { mutableStateOf(initialSlot?.weekCycle ?: WeekCycle.EVERY_WEEK) }
    var customLocation by remember(initialSlot?.id) { mutableStateOf(initialSlot?.customLocation ?: "") }
    var customInstructor by remember(initialSlot?.id) { mutableStateOf(initialSlot?.customInstructor ?: "") }
    var customType by remember(initialSlot?.id) { mutableStateOf(initialSlot?.customType ?: "") }
    var grade by remember(initialSlot?.id) { mutableStateOf(initialSlot?.grade) }

    // HEALTH workout exercises ONLY
    val setOptions = remember { (1..20).toList() }
    val repOptions = remember { (0..100).toList() }
    val workMinuteOptions = remember { (0..30).toList() }
    val restMinuteOptions = remember { (0..15).toList() }
    val secondOptions = remember { (0..55 step 5).toList() }

    var sessionItems by remember(initialSlot?.id) { mutableStateOf(initialSlot?.sessionItems ?: emptyList()) }
    var showExerciseForm by remember(initialSlot?.id) { mutableStateOf(false) }
    var editingExerciseId by remember(initialSlot?.id) { mutableStateOf<String?>(null) }
    var exerciseTitle by remember(initialSlot?.id) { mutableStateOf("") }
    var exerciseSets by remember(initialSlot?.id) { mutableIntStateOf(3) }
    var exerciseReps by remember(initialSlot?.id) { mutableIntStateOf(0) }
    var exerciseWorkMin by remember(initialSlot?.id) { mutableIntStateOf(0) }
    var exerciseWorkSec by remember(initialSlot?.id) { mutableIntStateOf(0) }
    var exerciseRestMin by remember(initialSlot?.id) { mutableIntStateOf(1) }
    var exerciseRestSec by remember(initialSlot?.id) { mutableIntStateOf(0) }

    fun openAddExerciseForm() {
        editingExerciseId = null
        exerciseTitle = ""
        exerciseSets = 3
        exerciseReps = 0
        exerciseWorkMin = 0
        exerciseWorkSec = 0
        exerciseRestMin = 1
        exerciseRestSec = 0
        showExerciseForm = true
    }

    fun openEditExerciseForm(item: SubtaskItem) {
        editingExerciseId = item.id
        exerciseTitle = item.title
        exerciseSets = (item.sets ?: 3).coerceIn(1, 20)
        exerciseReps = parseRepsInt(item.reps)
        val work = (item.durationSeconds ?: 0).coerceAtLeast(0)
        exerciseWorkMin = (work / 60).coerceIn(0, 30)
        exerciseWorkSec = ((work % 60 + 2) / 5 * 5).coerceIn(0, 55)
        val rest = (item.restSeconds ?: 0).coerceAtLeast(0)
        exerciseRestMin = (rest / 60).coerceIn(0, 15)
        exerciseRestSec = ((rest % 60 + 2) / 5 * 5).coerceIn(0, 55)
        showExerciseForm = true
    }

    fun closeExerciseForm() {
        showExerciseForm = false
        editingExerciseId = null
    }

    var showDuplicatePicker by remember(initialSlot?.id) { mutableStateOf(false) }

    val selectedActivity = activities.firstOrNull { it.id == selectedActivityId }

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
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialSlot == null) "Новое занятие" else "Редактировать",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // 1. Activity Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appProfile.subjectTerm,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )

                if (selectedActivity != null && onEditActivityRequest != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditActivityRequest(selectedActivity.id)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Изменить",
                            tint = IosBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Изменить",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosBlue
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(iosColors.cardSecondary)
                            .clickable { onAddNewActivityRequest() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = IosBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Новый",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosBlue
                            )
                        }
                    }
                }

                items(activities) { act ->
                    val isSelected = act.id == selectedActivityId
                    val actColor = ColorParser.parseHexColor(act.colorHex)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) IosBlue.copy(alpha = 0.14f) else iosColors.cardSecondary)
                            .border(
                                BorderStroke(
                                    width = if (isSelected) 1.2.dp else 0.dp,
                                    color = if (isSelected) IosBlue else Color.Transparent
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedActivityId = act.id
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(actColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = act.shortName.ifBlank { act.name },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) IosBlue else iosColors.textPrimary
                            )
                        }
                    }
                }
            }

            // 2. Day of Week (Пн..Вс)
            Text(
                text = "День недели",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт",
                    5 to "Пт", 6 to "Сб", 7 to "Вс"
                ).forEach { (day, label) ->
                    val isSelected = day == dayOfWeek
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IosBlue else iosColors.cardSecondary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                dayOfWeek = day
                                if (initialSlot == null && periodOrder > 0) {
                                    val resolved = resolveLessonTimesForPeriod(
                                        targetPeriod = periodOrder,
                                        targetDayOfWeek = day,
                                        existingSlots = existingSlots,
                                        fallbackPresets = timePresets,
                                        defaultDurationMin = appProfile.defaultDurationMinutes,
                                        defaultBreakMin = appProfile.defaultBreakMinutes
                                    )
                                    startTime = resolved.first
                                    endTime = resolved.second
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else iosColors.textPrimary
                        )
                    }
                }
            }

            // 3. Week Cycle (if enabled)
            if (isTwoWeekCycleEnabled) {
                Text(
                    text = "Цикл недели",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )
                IosSegmentedControl(
                    items = listOf("Каждую", "I • Числ", "II • Знам"),
                    selectedIndex = when (weekCycle) {
                        WeekCycle.EVERY_WEEK -> 0
                        WeekCycle.NUMERATOR_A -> 1
                        WeekCycle.DENOMINATOR_B -> 2
                    },
                    onItemSelected = { idx ->
                        weekCycle = when (idx) {
                            1 -> WeekCycle.NUMERATOR_A
                            2 -> WeekCycle.DENOMINATOR_B
                            else -> WeekCycle.EVERY_WEEK
                        }
                    }
                )
            }

            // 4. Period Order (only for bell-grid profiles)
            if (appProfile.defaultUsePeriodNumbers) {
                val itemWord = appProfile.itemTerm.split(" / ").first().lowercase()
                val isUnnumbered = periodOrder <= 0
                var lastNumberedPeriod by remember { mutableIntStateOf(if (initialPeriod > 0) initialPeriod else 1) }

                Text(
                    text = "Номер",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textSecondary
                )

                IosSegmentedControl(
                    items = listOf("По номеру", appProfile.unnumberedLabel),
                    selectedIndex = if (isUnnumbered) 1 else 0,
                    onItemSelected = { idx ->
                        if (idx == 1) {
                            if (periodOrder > 0) lastNumberedPeriod = periodOrder
                            periodOrder = 0
                        } else {
                            periodOrder = lastNumberedPeriod.coerceAtLeast(1)
                        }
                    }
                )

                AnimatedVisibility(visible = !isUnnumbered) {
                    val periodChoices = remember { (1..20).toList() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(iosColors.cardSecondary.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IosWheelPicker(
                            items = periodChoices,
                            selectedIndex = (periodOrder - 1).coerceIn(0, periodChoices.lastIndex),
                            onItemSelected = { _, num ->
                                periodOrder = num
                                lastNumberedPeriod = num
                                val resolved = resolveLessonTimesForPeriod(
                                    targetPeriod = num,
                                    targetDayOfWeek = dayOfWeek,
                                    existingSlots = existingSlots,
                                    fallbackPresets = timePresets,
                                    defaultDurationMin = appProfile.defaultDurationMinutes,
                                    defaultBreakMin = appProfile.defaultBreakMinutes
                                )
                                startTime = resolved.first
                                endTime = resolved.second
                            },
                            itemLabel = { num ->
                                val (s, e) = resolveLessonTimesForPeriod(
                                    targetPeriod = num,
                                    targetDayOfWeek = dayOfWeek,
                                    existingSlots = existingSlots,
                                    fallbackPresets = timePresets,
                                    defaultDurationMin = appProfile.defaultDurationMinutes,
                                    defaultBreakMin = appProfile.defaultBreakMinutes
                                )
                                "$num $itemWord  •  $s – $e"
                            },
                            visibleItemsCount = 3,
                            itemHeight = 38.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. Start and End Times
            Text(
                text = "Время",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IosTimePickerButton(
                    label = "Начало",
                    timeString = startTime,
                    onTimeSelected = { startTime = it },
                    modifier = Modifier.weight(1f)
                )
                IosTimePickerButton(
                    label = "Конец",
                    timeString = endTime,
                    onTimeSelected = { endTime = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Location, Instructor, Format
            OutlinedTextField(
                value = customLocation,
                onValueChange = { customLocation = it },
                label = { Text(appProfile.locationTerm) },
                placeholder = {
                    Text(selectedActivity?.defaultLocation ?: appProfile.locationPlaceholder)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = customInstructor,
                onValueChange = { customInstructor = it },
                label = { Text(appProfile.instructorTerm) },
                placeholder = {
                    Text(selectedActivity?.defaultInstructor ?: appProfile.instructorPlaceholder)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = customType,
                    onValueChange = { customType = it },
                    label = { Text("Формат") },
                    placeholder = {
                        Text(selectedActivity?.defaultType ?: appProfile.defaultLessonTypes.firstOrNull() ?: "")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(appProfile.defaultLessonTypes) { typeOption ->
                        val isChosen = customType.equals(typeOption, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) IosBlue else iosColors.cardSecondary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    customType = if (isChosen) "" else typeOption
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = typeOption,
                                fontSize = 11.5.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChosen) Color.White else iosColors.textPrimary
                            )
                        }
                    }
                }
            }

            // 7. Workout Exercises Builder — ONLY for HEALTH profile
            if (appProfile.supportsWorkouts) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(iosColors.cardSecondary.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Упражнения",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(IosBlue.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (showExerciseForm) closeExerciseForm() else openAddExerciseForm()
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (showExerciseForm) "Скрыть" else "+ Упражнение",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosBlue
                            )
                        }
                    }

                    AnimatedVisibility(visible = showExerciseForm) {
                        key(editingExerciseId) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iosColors.card)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = exerciseTitle,
                                    onValueChange = { exerciseTitle = it },
                                    label = { Text("Упражнение") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Sets & Reps Wheels side-by-side
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(iosColors.cardSecondary.copy(alpha = 0.7f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Подходы",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = iosColors.textSecondary
                                            )
                                            Text(
                                                text = exerciseSets.toString(),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IosBlue
                                            )
                                        }
                                        IosWheelPicker(
                                            items = setOptions,
                                            selectedIndex = (exerciseSets - 1).coerceIn(0, setOptions.lastIndex),
                                            onItemSelected = { _, s -> exerciseSets = s },
                                            itemLabel = { it.toString() },
                                            visibleItemsCount = 3,
                                            itemHeight = 32.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(iosColors.cardSecondary.copy(alpha = 0.7f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Повторы",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = iosColors.textSecondary
                                            )
                                            Text(
                                                text = if (exerciseReps == 0) "выкл" else exerciseReps.toString(),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (exerciseReps == 0) iosColors.textSecondary else IosBlue
                                            )
                                        }
                                        IosWheelPicker(
                                            items = repOptions,
                                            selectedIndex = exerciseReps.coerceIn(0, repOptions.lastIndex),
                                            onItemSelected = { _, r -> exerciseReps = r },
                                            itemLabel = { if (it == 0) "выкл" else it.toString() },
                                            visibleItemsCount = 3,
                                            itemHeight = 32.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Work & Rest Time Wheel Pairs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SlotTimeWheelPairCard(
                                        title = "Работа",
                                        minutes = exerciseWorkMin,
                                        seconds = exerciseWorkSec,
                                        minuteOptions = workMinuteOptions,
                                        secondOptions = secondOptions,
                                        zeroBadgeText = "выкл",
                                        onMinutesSelected = { exerciseWorkMin = it },
                                        onSecondsSelected = { exerciseWorkSec = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SlotTimeWheelPairCard(
                                        title = "Перерыв",
                                        minutes = exerciseRestMin,
                                        seconds = exerciseRestSec,
                                        minuteOptions = restMinuteOptions,
                                        secondOptions = secondOptions,
                                        zeroBadgeText = "нет",
                                        onMinutesSelected = { exerciseRestMin = it },
                                        onSecondsSelected = { exerciseRestSec = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = ::closeExerciseForm) {
                                        Text("Отмена", color = iosColors.textSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            val cleanName = exerciseTitle.trim()
                                            if (cleanName.isNotEmpty()) {
                                                val workTotal = (exerciseWorkMin * 60 + exerciseWorkSec).takeIf { it > 0 }
                                                val restTotal = (exerciseRestMin * 60 + exerciseRestSec).takeIf { it > 0 }
                                                val repsClean = if (exerciseReps == 0) null else pluralizeRu(exerciseReps, "повтор", "повтора", "повторов")
                                                val targetEditId = editingExerciseId
                                                if (targetEditId != null) {
                                                    sessionItems = sessionItems.map { existing ->
                                                        if (existing.id == targetEditId) {
                                                            existing.copy(
                                                                title = cleanName,
                                                                sets = exerciseSets,
                                                                reps = repsClean,
                                                                durationSeconds = workTotal,
                                                                restSeconds = restTotal
                                                            )
                                                        } else existing
                                                    }
                                                } else {
                                                    sessionItems = sessionItems + SubtaskItem(
                                                        title = cleanName,
                                                        sets = exerciseSets,
                                                        reps = repsClean,
                                                        durationSeconds = workTotal,
                                                        restSeconds = restTotal
                                                    )
                                                }
                                                closeExerciseForm()
                                            }
                                        },
                                        enabled = exerciseTitle.isNotBlank(),
                                        colors = iosPrimaryButtonColors(IosBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (editingExerciseId == null) "Добавить" else "Сохранить",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (sessionItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sessionItems.forEach { item ->
                                val summary = formatWorkoutExerciseSummary(item)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iosColors.card)
                                        .clickable { openEditExerciseForm(item) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = iosColors.textPrimary
                                        )
                                        if (summary.isNotBlank()) {
                                            Text(
                                                text = summary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = IosBlue
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (editingExerciseId == item.id) closeExerciseForm()
                                                sessionItems = sessionItems.filterNot { it.id == item.id }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Удалить",
                                            tint = iosColors.textSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Grade (ONLY for profiles supporting grades: STUDY, UNIVERSITY, UNIVERSAL)
            if (appProfile.supportsGrades) {
                SlotGradeSection(
                    grade = grade,
                    onGradeChange = { grade = it }
                )
            }

            // Duplicate day picker (shown when toggled)
            if (initialSlot != null) {
                AnimatedVisibility(visible = showDuplicatePicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(iosColors.cardSecondary)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "День для копии",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт",
                                5 to "Пт", 6 to "Сб", 7 to "Вс"
                            ).forEach { (day, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (day == dayOfWeek) iosColors.card else IosBlue.copy(alpha = 0.14f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val duplicated = Slot(
                                                id = UUID.randomUUID().toString(),
                                                activityTypeId = selectedActivityId,
                                                dayOfWeek = day,
                                                weekCycle = weekCycle,
                                                startTime = startTime.trim(),
                                                endTime = endTime.trim(),
                                                customLocation = customLocation.trim().takeIf { it.isNotBlank() },
                                                customInstructor = customInstructor.trim().takeIf { it.isNotBlank() },
                                                customType = customType.trim().takeIf { it.isNotBlank() },
                                                customNote = initialSlot.customNote,
                                                periodOrder = if (appProfile.defaultUsePeriodNumbers) periodOrder else periodOrder.coerceAtLeast(1),
                                                presetId = initialSlot.presetId.ifBlank { presetId },
                                                sessionItems = if (appProfile.supportsWorkouts) sessionItems else emptyList(),
                                                grade = null
                                            )
                                            if (onDuplicateSlot != null) {
                                                onDuplicateSlot(duplicated, day)
                                            } else {
                                                onSave(duplicated)
                                            }
                                            showDuplicatePicker = false
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (day == dayOfWeek) iosColors.textSecondary else IosBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Save + compact secondary row
            Button(
                onClick = {
                    if (selectedActivityId.isNotBlank()) {
                        val slot = Slot(
                            id = initialSlot?.id ?: UUID.randomUUID().toString(),
                            activityTypeId = selectedActivityId,
                            dayOfWeek = dayOfWeek,
                            weekCycle = weekCycle,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            customLocation = customLocation.trim().takeIf { it.isNotBlank() },
                            customInstructor = customInstructor.trim().takeIf { it.isNotBlank() },
                            customType = customType.trim().takeIf { it.isNotBlank() },
                            customNote = initialSlot?.customNote,
                            periodOrder = if (appProfile.defaultUsePeriodNumbers) periodOrder else periodOrder.coerceAtLeast(1),
                            presetId = initialSlot?.presetId?.ifBlank { presetId } ?: presetId,
                            sessionItems = if (appProfile.supportsWorkouts) sessionItems else emptyList(),
                            grade = if (appProfile.supportsGrades) grade?.trim()?.takeIf { it.isNotBlank() } else null
                        )
                        onSave(slot)
                        onDismiss()
                    }
                },
                enabled = selectedActivityId.isNotBlank(),
                colors = iosPrimaryButtonColors(IosBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Сохранить", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            }

            if (initialSlot != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDuplicatePicker = !showDuplicatePicker },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Дублировать", fontSize = 13.sp)
                    }

                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(initialSlot.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Удалить", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private val QUICK_SLOT_GRADE_OPTIONS = listOf("5", "4", "3", "2", "Зачёт")

@Composable
private fun SlotGradeSection(
    grade: String?,
    onGradeChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val cleanGrade = grade?.trim()?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(iosColors.cardSecondary.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Grade,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Оценка",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
            }

            if (cleanGrade != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosBlue.copy(alpha = 0.14f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGradeChange(null)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = cleanGrade,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Очистить оценку",
                        tint = IosBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(QUICK_SLOT_GRADE_OPTIONS) { option ->
                val isSelected = cleanGrade.equals(option, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) IosBlue else iosColors.card)
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) IosBlue else iosColors.separator.copy(alpha = 0.5f)
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGradeChange(if (isSelected) null else option)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else iosColors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        OutlinedTextField(
            value = grade ?: "",
            onValueChange = { input ->
                onGradeChange(input.takeIf { it.isNotEmpty() })
            },
            label = { Text("Своя оценка") },
            placeholder = { Text("Например: 5+, Отл, 92/100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
