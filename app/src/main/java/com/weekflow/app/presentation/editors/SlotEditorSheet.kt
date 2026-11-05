package com.weekflow.app.presentation.editors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

private fun resolveLessonTimesForPeriod(
    targetPeriod: Int,
    targetDayOfWeek: Int,
    existingSlots: List<Slot>,
    fallbackPresets: List<Pair<String, String>>
): Pair<String, String> {
    if (targetPeriod <= 0) {
        return Pair("15:00", "16:30")
    }

    val numberedSlots = existingSlots.filter { it.periodOrder > 0 }
    if (numberedSlots.isEmpty()) {
        return fallbackPresets.getOrElse(targetPeriod - 1) {
            val baseStartMin = 8 * 60 + 30 + (targetPeriod - 1) * 55
            val start = LocalTime.of((baseStartMin / 60) % 24, baseStartMin % 60)
            val end = start.plusMinutes(45)
            String.format("%02d:%02d", start.hour, start.minute) to String.format("%02d:%02d", end.hour, end.minute)
        }
    }

    // 1. Exact match on the same day of week
    val exactDayMatch = numberedSlots.firstOrNull {
        it.dayOfWeek == targetDayOfWeek && it.periodOrder == targetPeriod
    }
    if (exactDayMatch != null) {
        return exactDayMatch.startTime to exactDayMatch.endTime
    }

    // 2. Exact match for this periodOrder on any other day of week (take most frequent time pair)
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

    // 3. Infer typical lesson duration & break duration from existing schedule
    val durations = numberedSlots.mapNotNull { slot ->
        val mins = Duration.between(slot.parseStartTime(), slot.parseEndTime()).toMinutes().toInt()
        mins.takeIf { it in 20..240 }
    }
    val lessonDurationMin = durations
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: 45

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
        ?: if (lessonDurationMin <= 50) 10 else 15

    // Find closest preceding lesson (prefer same day, then any day)
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

    // Find closest succeeding lesson if targetPeriod is before all existing lessons
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

    // Standard bell time presets for fallback when schedule is completely empty
    val timePresets = listOf(
        Pair("09:00", "10:35"),
        Pair("10:45", "12:20"),
        Pair("13:00", "14:35"),
        Pair("14:45", "16:20"),
        Pair("16:30", "18:05"),
        Pair("18:15", "19:50"),
        Pair("20:00", "21:35")
    )

    val initialPeriod = remember(initialSlot?.id, defaultPeriodOrder, defaultDayOfWeek, existingSlots.size) {
        when {
            initialSlot != null -> initialSlot.periodOrder
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
    val defaultTimesForPeriod = remember(initialPeriod, defaultDayOfWeek, existingSlots) {
        resolveLessonTimesForPeriod(
            targetPeriod = initialPeriod,
            targetDayOfWeek = initialSlot?.dayOfWeek ?: defaultDayOfWeek,
            existingSlots = existingSlots,
            fallbackPresets = timePresets
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
    var periodOrderText by remember(initialSlot?.id, defaultPeriodOrder) {
        mutableStateOf(initialPeriod.toString())
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
    var customNote by remember(initialSlot?.id) { mutableStateOf(initialSlot?.customNote ?: "") }

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
                .padding(bottom = 32.dp)
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
                    text = if (initialSlot == null) "Новое занятие" else "Редактировать занятие",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // 1. Activity Selector & Edit Activity button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Предмет / Занятие",
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
                            contentDescription = "Редактировать предмет",
                            tint = IosBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Настроить предмет",
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
                            .background(if (isSelected) actColor else iosColors.cardSecondary)
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
                                    .background(if (isSelected) Color.White else actColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = act.shortName.ifBlank { act.name },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else iosColors.textPrimary
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
                                        fallbackPresets = timePresets
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

            // 4. Period Order (Прокрутка 1..20 или Вне уроков)
            val itemWord = appProfile.itemTerm.split(" / ").first().lowercase()
            val isUnnumbered = periodOrder <= 0
            var lastNumberedPeriod by remember { mutableIntStateOf(if (initialPeriod > 0) initialPeriod else 1) }

            Text(
                text = "Номер занятия",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )

            IosSegmentedControl(
                items = listOf("По номеру ($itemWord)", "Вне уроков (без №)"),
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
                            periodOrderText = num.toString()
                            val resolved = resolveLessonTimesForPeriod(
                                targetPeriod = num,
                                targetDayOfWeek = dayOfWeek,
                                existingSlots = existingSlots,
                                fallbackPresets = timePresets
                            )
                            startTime = resolved.first
                            endTime = resolved.second
                        },
                        itemLabel = { num ->
                            val (s, e) = resolveLessonTimesForPeriod(
                                targetPeriod = num,
                                targetDayOfWeek = dayOfWeek,
                                existingSlots = existingSlots,
                                fallbackPresets = timePresets
                            )
                            "$num $itemWord  •  $s – $e"
                        },
                        visibleItemsCount = 3,
                        itemHeight = 38.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Start and End Times (Wheel Time Pickers)
            Text(
                text = "Время проведения",
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

            // 6. Custom Overrides (Location, Instructor, Type)
            OutlinedTextField(
                value = customLocation,
                onValueChange = { customLocation = it },
                label = { Text(appProfile.locationTerm) },
                placeholder = {
                    Text(selectedActivity?.defaultLocation ?: "По умолчанию из предмета")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = customInstructor,
                onValueChange = { customInstructor = it },
                label = { Text(appProfile.instructorTerm) },
                placeholder = {
                    Text(selectedActivity?.defaultInstructor ?: "По умолчанию из предмета")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = customType,
                onValueChange = { customType = it },
                label = { Text("Тип занятия (Лекция / Практика)") },
                placeholder = {
                    Text(selectedActivity?.defaultType ?: "например, Лекция")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Duplicate to another day section
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
                            text = "Выберите день для дублирования:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textPrimary
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
                                        .background(if (day == dayOfWeek) iosColors.card else IosBlue.copy(alpha = 0.15f))
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
                                                customNote = customNote.trim().takeIf { it.isNotBlank() },
                                                periodOrder = periodOrder,
                                                presetId = initialSlot.presetId.ifBlank { presetId }
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

                OutlinedButton(
                    onClick = {
                        showDuplicatePicker = !showDuplicatePicker
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showDuplicatePicker) "Скрыть выбор дня" else "Дублировать на другой день")
                }
            }

            // Save and Delete
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
                            customNote = customNote.trim().takeIf { it.isNotBlank() },
                            periodOrder = periodOrder,
                            presetId = initialSlot?.presetId?.ifBlank { presetId } ?: presetId
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
                    .height(50.dp)
            ) {
                Text("Сохранить занятие", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (initialSlot != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete(initialSlot.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить из расписания")
                }
            }
        }
    }
}
