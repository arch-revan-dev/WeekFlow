package com.weekflow.app.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.FocusTimerDialog
import com.weekflow.app.core.designsystem.components.InteractiveSessionPlayerSheet
import com.weekflow.app.core.designsystem.components.LessonSlotCard
import com.weekflow.app.core.designsystem.components.TaskItemRow
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.ScheduleTimelineItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.usecase.CalendarDayUiModel
import com.weekflow.app.presentation.editors.PresetManagerSheet
import com.weekflow.app.presentation.schedule.AllDayEventCard
import com.weekflow.app.presentation.schedule.BreakWindowCard
import com.weekflow.app.presentation.schedule.TimedEventCard
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onAddNewEvent: (dateIso: String) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onEditSlot: (slotId: String) -> Unit,
    onAddNewTaskForSlot: (activityId: String, slotId: String, referenceDate: LocalDate) -> Unit,
    onEditTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val iosColors = LocalIosColors.current
    val appProfile = uiState.activePreset.appProfile
    val haptic = LocalHapticFeedback.current
    var isMonthExpanded by remember { mutableStateOf(true) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var focusTimerTitle by remember { mutableStateOf<String?>(null) }
    var activeSessionSlotId by remember { mutableStateOf<String?>(null) }

    val presetColor = ColorParser.parseHexColor(uiState.activePreset.colorHex)
    val activityMap = remember(uiState.allActivities) {
        uiState.allActivities.associateBy { it.id }
    }

    if (showPresetSheet) {
        PresetManagerSheet(
            presets = uiState.allPresets,
            activePresetId = uiState.activePreset.id,
            allActivities = uiState.rawAllActivities,
            allSlots = uiState.rawAllSlots,
            allTasks = uiState.rawAllTasks,
            showAllPresetsTasks = uiState.preferences.showAllPresetsTasks,
            onSelectPreset = { preset ->
                viewModel.selectPreset(preset)
                showPresetSheet = false
            },
            onSavePreset = { preset -> viewModel.savePreset(preset) },
            onDuplicatePreset = { srcId, newPreset -> viewModel.duplicatePreset(srcId, newPreset) },
            onDeletePreset = { presetId -> viewModel.deletePreset(presetId) },
            onToggleShowAllPresetsTasks = { show -> viewModel.toggleShowAllPresetsTasks(show) },
            onDismiss = { showPresetSheet = false }
        )
    }

    focusTimerTitle?.let { title ->
        FocusTimerDialog(
            initialTitle = title,
            onDismiss = { focusTimerTitle = null },
            onSessionCompleted = { mins -> viewModel.recordFocusSession(mins) }
        )
    }

    CompositionLocalProvider(LocalAppProfile provides appProfile) {
    activeSessionSlotId?.let { slotId ->
        val details = uiState.selectedDaySchedule?.timelineItems
            ?.filterIsInstance<ScheduleTimelineItem.Lesson>()
            ?.firstOrNull { it.details.slot.id == slotId }?.details
        if (details != null) {
            InteractiveSessionPlayerSheet(
                sessionTitle = details.activity.name,
                sessionSubtitle = "${details.slot.startTime} – ${details.slot.endTime}" +
                    (details.slot.customLocation?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                accentColor = ColorParser.parseHexColor(details.activity.colorHex),
                initialItems = details.slot.sessionItems,
                onUpdateItems = { updatedItems ->
                    viewModel.updateSlotSessionItems(details.slot, updatedItems)
                },
                onAddFollowUpTask = { taskTitle ->
                    viewModel.addQuickFollowUpTask(
                        title = taskTitle,
                        activityId = details.activity.id,
                        presetId = uiState.activePreset.id
                    )
                },
                onSessionCompleted = { mins -> viewModel.recordFocusSession(mins) },
                onDismiss = { activeSessionSlotId = null }
            )
        } else {
            activeSessionSlotId = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosColors.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = DateTimeUtils.formatMonthYearHeader(uiState.currentYearMonth.atDay(1)),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 135.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(presetColor.copy(alpha = 0.14f))
                                    .border(
                                        BorderStroke(1.dp, presetColor.copy(alpha = 0.4f)),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showPresetSheet = true
                                    }
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(text = uiState.activePreset.iconEmoji, fontSize = 11.sp)
                                    Text(
                                        text = uiState.activePreset.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = presetColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Выбрать пресет",
                                        tint = presetColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.selectedDate != LocalDate.now() || uiState.currentYearMonth != YearMonth.now()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Выбрано: ${DateTimeUtils.formatDateWithDay(uiState.selectedDate)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosColors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IosBlue.copy(alpha = 0.14f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.goToToday()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "Сегодня",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosBlue
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onPreviousMonth()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц",
                            tint = IosBlue
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onNextMonth()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = IosBlue
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddNewEvent(uiState.selectedDate.toString())
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(IosBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Добавить событие",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosColors.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Days of week header (Пн..Вс)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { index, day ->
                    val isWeekend = index == 6 || (index == 5 && uiState.preferences.isSaturdayWeekend)
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWeekend) IosRed.copy(alpha = 0.8f) else iosColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Month Grid (Full Month or Selected Week Row) with smooth spring animation & vertical swipe
            val daysToShow = remember(isMonthExpanded, uiState.monthDays, uiState.selectedDate) {
                if (isMonthExpanded) {
                    uiState.monthDays
                } else {
                    val chunks = uiState.monthDays.chunked(7)
                    val selectedChunk = chunks.firstOrNull { chunk -> chunk.any { it.date == uiState.selectedDate } }
                        ?: chunks.firstOrNull() ?: emptyList()
                    selectedChunk
                }
            }

            var accumulatedVerticalDrag by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .pointerInput(isMonthExpanded) {
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedVerticalDrag = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedVerticalDrag += dragAmount
                                if (accumulatedVerticalDrag < -32f && isMonthExpanded) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMonthExpanded = false
                                    accumulatedVerticalDrag = 0f
                                } else if (accumulatedVerticalDrag > 32f && !isMonthExpanded) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMonthExpanded = true
                                    accumulatedVerticalDrag = 0f
                                }
                            }
                        )
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    items(daysToShow, key = { it.date.toString() }) { dayModel ->
                        CalendarDayCell(
                            day = dayModel,
                            isSelected = dayModel.date == uiState.selectedDate,
                            isSaturdayWeekend = uiState.preferences.isSaturdayWeekend,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDateSelected(dayModel.date)
                            }
                        )
                    }
                }
            }

            val chevronRotation by animateFloatAsState(
                targetValue = if (isMonthExpanded) 180f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "CalendarHandleChevron"
            )

            // Interactive swipe & tap handle bar between month grid and day sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(isMonthExpanded) {
                        var dragSum = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragSum = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragSum += dragAmount
                                if (dragSum < -20f && isMonthExpanded) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMonthExpanded = false
                                    dragSum = 0f
                                } else if (dragSum > 20f && !isMonthExpanded) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMonthExpanded = true
                                    dragSum = 0f
                                }
                            }
                        )
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isMonthExpanded = !isMonthExpanded
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(iosColors.card.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(iosColors.textSecondary.copy(alpha = 0.45f))
                    )
                    Text(
                        text = if (isMonthExpanded) "Неделя" else "Весь месяц",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = iosColors.textSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Lower half: Selected Day Summary
            val daySchedule = uiState.selectedDaySchedule
            val visibleTimelineItems = remember(daySchedule, uiState.preferences.showBreaks) {
                val items = daySchedule?.timelineItems ?: emptyList()
                if (uiState.preferences.showBreaks) {
                    items
                } else {
                    items.filter { it !is ScheduleTimelineItem.BreakWindow }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(iosColors.card),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day Title header with swipe gesture to expand/collapse calendar
                item(key = "day_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(isMonthExpanded) {
                                var dragDelta = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { dragDelta = 0f },
                                    onVerticalDrag = { _, dragAmount ->
                                        dragDelta += dragAmount
                                        if (dragDelta < -24f && isMonthExpanded) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isMonthExpanded = false
                                            dragDelta = 0f
                                        } else if (dragDelta > 24f && !isMonthExpanded) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isMonthExpanded = true
                                            dragDelta = 0f
                                        }
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = DateTimeUtils.getDayOfWeekFull(uiState.selectedDate.dayOfWeek.value),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary
                            )
                            Text(
                                text = DateTimeUtils.formatDateWithDay(uiState.selectedDate),
                                fontSize = 13.sp,
                                color = iosColors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosBlue.copy(alpha = 0.12f))
                                .clickable { onAddNewEvent(uiState.selectedDate.toString()) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Событие",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosBlue
                            )
                        }
                    }
                }

                // Holiday or Vacation notice
                if (daySchedule != null && daySchedule.holidayOrVacationTitle != null) {
                    item(key = "holiday_summary") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosRed.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Celebration,
                                contentDescription = null,
                                tint = IosRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = daySchedule.holidayOrVacationTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = IosRed
                            )
                        }
                    }
                }

                // All-day events
                if (daySchedule != null && daySchedule.allDayEvents.isNotEmpty()) {
                    items(daySchedule.allDayEvents, key = { "cal_allday_${it.id}" }) { event ->
                        AllDayEventCard(
                            event = event,
                            onClick = { onEditEvent(event) }
                        )
                    }
                }

                // Timeline Items
                if (visibleTimelineItems.isNotEmpty()) {
                    items(
                        items = visibleTimelineItems,
                        key = { item ->
                            when (item) {
                                is ScheduleTimelineItem.Lesson -> "cal_lesson_${item.details.slot.id}"
                                is ScheduleTimelineItem.BreakWindow -> "cal_break_${item.startTime}_${item.endTime}"
                                is ScheduleTimelineItem.TimedEvent -> "cal_event_${item.event.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ScheduleTimelineItem.Lesson -> {
                                LessonSlotCard(
                                    details = item.details,
                                    cardStyle = uiState.preferences.cardStyle,
                                    cardDensity = uiState.preferences.cardDensity,
                                    cornerRadiusDp = uiState.preferences.cornerRadiusDp,
                                    showProgressBar = uiState.preferences.showProgressBar,
                                    isTwoWeekCycleEnabled = uiState.preferences.isTwoWeekCycleEnabled,
                                    isToday = uiState.selectedDate == LocalDate.now(),
                                    onCardClick = { onEditSlot(item.details.slot.id) },
                                    onAddTaskClick = {
                                        onAddNewTaskForSlot(
                                            item.details.activity.id,
                                            item.details.slot.id,
                                            uiState.selectedDate
                                        )
                                    },
                                    onTaskToggle = { task -> viewModel.toggleTask(task) },
                                    onTaskClick = { task -> onEditTask(task) },
                                    onStartFocusTimer = { focusTimerTitle = item.details.activity.name },
                                    onStartInteractiveSession = { activeSessionSlotId = item.details.slot.id }
                                )
                            }
                            is ScheduleTimelineItem.BreakWindow -> {
                                BreakWindowCard(breakWindow = item)
                            }
                            is ScheduleTimelineItem.TimedEvent -> {
                                TimedEventCard(
                                    event = item.event,
                                    onClick = { onEditEvent(item.event) }
                                )
                            }
                        }
                    }
                }

                // Unassigned tasks for the selected day
                if (daySchedule != null && daySchedule.unassignedTasks.isNotEmpty()) {
                    item(key = "cal_unassigned_header") {
                        Text(
                            text = "ЗАДАЧИ НА ДЕНЬ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textSecondary,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }
                    items(daySchedule.unassignedTasks, key = { "cal_unassigned_${it.id}" }) { task ->
                        TaskItemRow(
                            task = task,
                            linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                            onToggleCompleted = { viewModel.toggleTask(task) },
                            onClick = { onEditTask(task) },
                            onFocusTimerClick = { focusTimerTitle = task.title },
                            onPostponeTomorrowClick = { viewModel.postponeTaskToTomorrow(task) },
                            onDeleteClick = { viewModel.deleteTask(task.id) },
                            onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                        )
                    }
                }

                if (daySchedule != null &&
                    visibleTimelineItems.isEmpty() &&
                    daySchedule.allDayEvents.isEmpty() &&
                    daySchedule.unassignedTasks.isEmpty()
                ) {
                    item(key = "cal_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "На этот день событий и ${appProfile.pluralGenitive} нет",
                                fontSize = 14.sp,
                                color = iosColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDayUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaturdayWeekend: Boolean = false
) {
    val iosColors = LocalIosColors.current
    val isWeekend = day.date.dayOfWeek.value == 7 || (isSaturdayWeekend && day.date.dayOfWeek.value == 6)

    val textColor = when {
        isSelected -> Color.White
        day.isHoliday -> IosRed
        !day.isCurrentMonth -> iosColors.textSecondary.copy(alpha = 0.35f)
        day.isToday -> IosBlue
        isWeekend -> IosRed.copy(alpha = 0.8f)
        else -> iosColors.textPrimary
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) IosBlue else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.date.dayOfMonth}",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }

            // Pending tasks count badge on day cell
            if (day.pendingTasksCount > 0) {
                val isOverdueDay = day.date.isBefore(LocalDate.now())
                val badgeBg = when {
                    isOverdueDay -> IosRed
                    day.isToday -> IosOrange
                    isSelected -> Color.White
                    else -> IosBlue
                }
                val badgeTextColor = if (isSelected && !isOverdueDay && !day.isToday) IosBlue else Color.White

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 4.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (day.pendingTasksCount > 9) "9+" else "${day.pendingTasksCount}",
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Mini indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(4.dp)
        ) {
            day.dotColorsHex.take(4).forEach { hex ->
                val dotColor = ColorParser.parseHexColor(hex)
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
