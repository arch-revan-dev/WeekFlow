package com.weekflow.app.presentation.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.FocusTimerDialog
import com.weekflow.app.core.designsystem.components.InteractiveSessionPlayerSheet
import com.weekflow.app.core.designsystem.components.IosSegmentedControl
import com.weekflow.app.core.designsystem.components.LessonSlotCard
import com.weekflow.app.core.designsystem.components.LiveLessonBanner
import com.weekflow.app.core.designsystem.components.RhythmAndRemindersSheet
import com.weekflow.app.core.designsystem.components.TaskItemRow
import com.weekflow.app.core.designsystem.components.UndoIncidentBanner
import com.weekflow.app.core.designsystem.components.WeekStripHeader
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.ScheduleTimelineItem
import com.weekflow.app.domain.model.ScheduleViewMode
import com.weekflow.app.domain.model.SlotWithDetails
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.presentation.editors.PresetManagerSheet
import com.weekflow.app.presentation.editors.WeeklyInsightsSheet
import com.weekflow.app.presentation.schedule.components.TimelineGridView
import com.weekflow.app.presentation.schedule.components.WeekTableGridView
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onAddNewSlot: (dayOfWeek: Int) -> Unit,
    onEditSlot: (slotId: String) -> Unit,
    onAddNewTaskForSlot: (activityId: String, slotId: String, referenceDate: LocalDate) -> Unit,
    onAddNewEvent: (dateIso: String) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddNewSlotWithPeriod: (dayOfWeek: Int, periodOrder: Int) -> Unit = { day, _ -> onAddNewSlot(day) },
    onAddNewSlotWithTime: (dayOfWeek: Int, startTime: String, endTime: String) -> Unit = { day, _, _ -> onAddNewSlot(day) },
    onEditTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoSnapshot by viewModel.lastUndoSnapshot.collectAsState()
    val iosColors = LocalIosColors.current
    val appProfile = uiState.activePreset.appProfile
    val haptic = LocalHapticFeedback.current

    var focusTimerTitle by remember { mutableStateOf<String?>(null) }
    var focusTimerMinutes by remember { mutableIntStateOf(25) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var showRhythmSheet by remember { mutableStateOf(false) }
    var activeSessionSlotId by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalAppProfile provides appProfile) {
    if (focusTimerTitle != null) {
        FocusTimerDialog(
            initialTitle = focusTimerTitle!!,
            defaultMinutes = focusTimerMinutes,
            onDismiss = { focusTimerTitle = null },
            onSessionCompleted = { mins -> viewModel.recordFocusSession(mins) }
        )
    }

    if (showRhythmSheet) {
        RhythmAndRemindersSheet(
            rhythmReminders = uiState.preferences.rhythmReminders,
            quickReminders = uiState.preferences.quickReminders,
            onUpdateRhythmReminders = { viewModel.updateRhythmReminders(it) },
            onUpdateQuickReminders = { viewModel.updateQuickReminders(it) },
            onDismiss = { showRhythmSheet = false }
        )
    }

    activeSessionSlotId?.let { slotId ->
        val liveSlot = uiState.allSlots.firstOrNull { it.id == slotId }
        if (liveSlot != null) {
            val act = uiState.allActivities.firstOrNull { it.id == liveSlot.activityTypeId }
            InteractiveSessionPlayerSheet(
                sessionTitle = act?.name ?: "Занятие",
                sessionSubtitle = "${liveSlot.startTime} – ${liveSlot.endTime}" +
                    (liveSlot.customLocation?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                accentColor = ColorParser.parseHexColor(act?.colorHex ?: uiState.activePreset.colorHex),
                initialItems = liveSlot.sessionItems,
                onUpdateItems = { updatedItems ->
                    viewModel.updateSlotSessionItems(liveSlot, updatedItems)
                },
                onAddFollowUpTask = { taskTitle ->
                    viewModel.addQuickFollowUpTask(
                        title = taskTitle,
                        activityId = liveSlot.activityTypeId,
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

    if (showInsightsSheet) {
        WeeklyInsightsSheet(
            activePreset = uiState.activePreset,
            activities = uiState.rawAllActivities,
            slots = uiState.rawAllSlots,
            tasks = uiState.rawAllTasks,
            activeWeekCycle = uiState.activeWeekCycle,
            preferences = uiState.preferences,
            onDismiss = { showInsightsSheet = false }
        )
    }

    val presetColor = ColorParser.parseHexColor(uiState.activePreset.colorHex)

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
                                text = DateTimeUtils.formatMonthYearHeader(uiState.selectedDate),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Interactive Preset Pill
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
                                        contentDescription = "Пресеты",
                                        tint = presetColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        val relativeWeek = DateTimeUtils.getRelativeWeekLabel(uiState.selectedDate)
                        val isCurrentWeek = relativeWeek == "Эта неделя"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${DateTimeUtils.formatWeekRange(uiState.weekDays)} • $relativeWeek",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCurrentWeek) iosColors.textSecondary else IosBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (!uiState.isToday) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IosBlue.copy(alpha = 0.14f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.goToToday()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Сегодня",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosBlue,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (uiState.preferences.isTwoWeekCycleEnabled && uiState.activeWeekCycle != WeekCycle.EVERY_WEEK) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(iosColors.cardSecondary)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = uiState.activeWeekCycle.shortBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IosBlue
                                    )
                                }
                            }

                            if (uiState.isPresetAutoScheduled) {
                                Text(
                                    text = "• авто",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = presetColor
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onPreviousWeek()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Предыдущая неделя",
                            tint = IosBlue
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onNextWeek()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Следующая неделя",
                            tint = IosBlue
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRhythmSheet = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        val hasActiveRhythm = uiState.preferences.rhythmReminders.any { it.isEnabled } ||
                            uiState.preferences.quickReminders.any { it.triggerAtEpochMillis > System.currentTimeMillis() }
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Ритм дня и напоминания",
                                tint = if (hasActiveRhythm) IosOrange else IosBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showInsightsSheet = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueryStats,
                            contentDescription = "Аналитика недели",
                            tint = IosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddNewSlot(uiState.selectedDate.dayOfWeek.value)
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
                                contentDescription = "Добавить",
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
            AnimatedVisibility(visible = undoSnapshot != null) {
                undoSnapshot?.let { snap ->
                    UndoIncidentBanner(
                        snapshot = snap,
                        onUndoClick = { viewModel.restoreLastUndo() },
                        onDismissClick = { viewModel.dismissUndo() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Optional banner if date has an auto-scheduled preset that was manually overridden
            val scheduledForDate = uiState.dateScheduledPreset
            if (scheduledForDate != null && scheduledForDate.id != uiState.activePreset.id) {
                val schedColor = ColorParser.parseHexColor(scheduledForDate.colorHex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(schedColor.copy(alpha = 0.14f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.applyDateScheduledPreset()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "На эту дату назначен пресет: ${scheduledForDate.iconEmoji} ${scheduledForDate.name}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Включить",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = schedColor
                        )
                    }
                }
            }
            // Segmented mode switcher: День / Таблица / Сетка
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                IosSegmentedControl(
                    items = listOf("День", "Таблица (Weeklie)", "Сетка"),
                    selectedIndex = when (uiState.preferences.scheduleViewMode) {
                        ScheduleViewMode.DAY_LIST -> 0
                        ScheduleViewMode.WEEK_TABLE_GRID -> 1
                        ScheduleViewMode.TIMELINE_GRID -> 2
                    },
                    onItemSelected = { idx ->
                        val mode = when (idx) {
                            1 -> ScheduleViewMode.WEEK_TABLE_GRID
                            2 -> ScheduleViewMode.TIMELINE_GRID
                            else -> ScheduleViewMode.DAY_LIST
                        }
                        viewModel.setScheduleViewMode(mode)
                    }
                )
            }

            when (uiState.preferences.scheduleViewMode) {
                ScheduleViewMode.WEEK_TABLE_GRID -> {
                    // Mode 2: Weeklie Matrix Table
                    WeekTableGridView(
                        weekDays = uiState.weekDays,
                        slots = uiState.allSlots,
                        activities = uiState.allActivities,
                        activeWeekCycle = uiState.activeWeekCycle,
                        isTwoWeekCycleEnabled = uiState.preferences.isTwoWeekCycleEnabled,
                        onEditSlot = onEditSlot,
                        onAddSlot = { day, periodOrder -> onAddNewSlotWithPeriod(day, periodOrder) },
                        onMoveSlot = { slot, targetDay, targetPeriod ->
                            viewModel.moveSlotToCell(slot, targetDay, targetPeriod)
                        },
                        isSaturdayWeekend = uiState.preferences.isSaturdayWeekend,
                        tasks = uiState.allTasks,
                        onDateClick = { date ->
                            viewModel.onDateSelected(date)
                            viewModel.setScheduleViewMode(ScheduleViewMode.DAY_LIST)
                        }
                    )
                }

                ScheduleViewMode.TIMELINE_GRID -> {
                    // Mode 3: Hourly Google Calendar / TickTick Timeline Grid
                    WeekStripHeader(
                        selectedDate = uiState.selectedDate,
                        weekDays = uiState.weekDays,
                        dayIndicators = uiState.dayIndicators,
                        onDateSelected = { date -> viewModel.onDateSelected(date) },
                        isSaturdayWeekend = uiState.preferences.isSaturdayWeekend,
                        taskCountsByDate = uiState.taskCountsByDate,
                        onPreviousWeek = { viewModel.onPreviousWeek() },
                        onNextWeek = { viewModel.onNextWeek() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TimelineGridView(
                        selectedDate = uiState.selectedDate,
                        slots = uiState.allSlots,
                        events = uiState.allEvents,
                        activities = uiState.allActivities,
                        activeWeekCycle = uiState.activeWeekCycle,
                        isTwoWeekCycleEnabled = uiState.preferences.isTwoWeekCycleEnabled,
                        onEditSlot = onEditSlot,
                        onEditEvent = onEditEvent,
                        onAddSlotWithTime = { startTime, endTime ->
                            onAddNewSlotWithTime(uiState.selectedDate.dayOfWeek.value, startTime, endTime)
                        },
                        onMoveSlotTime = { slot, newStart, newEnd ->
                            viewModel.updateSlotTime(slot, newStart, newEnd)
                        }
                    )
                }

                ScheduleViewMode.DAY_LIST -> {
                    // Mode 1: Classic Day List with Cards
                    WeekStripHeader(
                        selectedDate = uiState.selectedDate,
                        weekDays = uiState.weekDays,
                        dayIndicators = uiState.dayIndicators,
                        onDateSelected = { date -> viewModel.onDateSelected(date) },
                        isSaturdayWeekend = uiState.preferences.isSaturdayWeekend,
                        taskCountsByDate = uiState.taskCountsByDate,
                        onPreviousWeek = { viewModel.onPreviousWeek() },
                        onNextWeek = { viewModel.onNextWeek() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val daySchedule = uiState.daySchedule
                    val activityMap = remember(uiState.allActivities) {
                        uiState.allActivities.associateBy { it.id }
                    }
                    var daySwipeDrag by remember { mutableFloatStateOf(0f) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.selectedDate) {
                                detectHorizontalDragGestures(
                                    onDragStart = { daySwipeDrag = 0f },
                                    onDragEnd = {
                                        if (daySwipeDrag < -75f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.onNextDay()
                                        } else if (daySwipeDrag > 75f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.onPreviousDay()
                                        }
                                        daySwipeDrag = 0f
                                    },
                                    onDragCancel = { daySwipeDrag = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        daySwipeDrag += dragAmount
                                    }
                                )
                            },
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Holiday or Vacation Banner
                        if (daySchedule != null && daySchedule.holidayOrVacationTitle != null) {
                            item(key = "holiday_banner") {
                                val isHoliday = daySchedule.isHoliday
                                val bannerColor = if (isHoliday) IosRed else IosOrange
                                val icon = if (isHoliday) Icons.Rounded.Celebration else Icons.Rounded.BeachAccess

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bannerColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = bannerColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isHoliday) "Государственный праздник РФ" else "Каникулы / Отпуск",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = bannerColor
                                            )
                                            Text(
                                                text = daySchedule.holidayOrVacationTitle,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = iosColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. All-Day Events
                        if (daySchedule != null && daySchedule.allDayEvents.isNotEmpty()) {
                            items(daySchedule.allDayEvents, key = { "allday_${it.id}" }) { event ->
                                AllDayEventCard(
                                    event = event,
                                    onClick = { onEditEvent(event) }
                                )
                            }
                        }

                        // 3. Live Now Banner (if today)
                        if (daySchedule?.liveStatus != null &&
                            (daySchedule.liveStatus.currentSlot != null || (daySchedule.liveStatus.nextSlot != null && daySchedule.liveStatus.isBreakNow))
                        ) {
                            item(key = "live_status") {
                                LiveLessonBanner(liveStatus = daySchedule.liveStatus)
                            }
                        }

                        // 4. Timeline Items: Lessons, Breaks, and Timed Events
                        if (daySchedule != null && daySchedule.timelineItems.isNotEmpty()) {
                            val itemsToRender = if (uiState.preferences.showBreaks) {
                                daySchedule.timelineItems
                            } else {
                                daySchedule.timelineItems.filter { it !is ScheduleTimelineItem.BreakWindow }
                            }

                            items(
                                items = itemsToRender,
                                key = { item ->
                                    when (item) {
                                        is ScheduleTimelineItem.Lesson -> "lesson_${item.details.slot.id}"
                                        is ScheduleTimelineItem.BreakWindow -> "break_${item.startTime}_${item.endTime}"
                                        is ScheduleTimelineItem.TimedEvent -> "event_${item.event.id}"
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
                                            onStartFocusTimer = {
                                                focusTimerTitle = item.details.activity.name
                                                focusTimerMinutes = 45
                                            },
                                            onStartInteractiveSession = {
                                                activeSessionSlotId = item.details.slot.id
                                            }
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
                        } else if (daySchedule != null && daySchedule.timelineItems.isEmpty() && daySchedule.allDayEvents.isEmpty() && daySchedule.unassignedTasks.isEmpty()) {
                            // Empty State: Day off
                            item(key = "empty_state") {
                                EmptyDayScheduleCard(
                                    appProfileTitle = appProfile.pluralGenitive,
                                    onAddLessonClick = { onAddNewSlot(uiState.selectedDate.dayOfWeek.value) },
                                    onAddEventClick = { onAddNewEvent(uiState.selectedDate.toString()) }
                                )
                            }
                        }

                        // 5. Unassigned Tasks due on this exact date (if any)
                        if (daySchedule != null && daySchedule.unassignedTasks.isNotEmpty()) {
                            item(key = "unassigned_day_tasks_header") {
                                Text(
                                    text = "Задачи на ${DateTimeUtils.formatDateWithDay(uiState.selectedDate)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(daySchedule.unassignedTasks, key = { "day_task_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) },
                                    onClick = { onEditTask(task) },
                                    onPostponeTomorrowClick = { viewModel.postponeTaskToTomorrow(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) },
                                    onFocusTimerClick = {
                                        focusTimerTitle = task.title
                                        focusTimerMinutes = 25
                                    }
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
fun AllDayEventCard(
    event: CalendarEvent,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val eventColor = ColorParser.parseHexColor(event.colorHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(eventColor.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textPrimary
                )
                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        color = iosColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun BreakWindowCard(
    breakWindow: ScheduleTimelineItem.BreakWindow,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(iosColors.separator)
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreTime,
                contentDescription = null,
                tint = iosColors.textSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Перерыв ${DateTimeUtils.formatMinutesDuration(breakWindow.durationMinutes)} (${breakWindow.startTime} – ${breakWindow.endTime})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textSecondary
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(iosColors.separator)
        )
    }
}

@Composable
fun TimedEventCard(
    event: CalendarEvent,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val eventColor = ColorParser.parseHexColor(event.colorHex)
    val surfaceColor = if (iosColors.isDark) Color(0xFF232328) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(
                BorderStroke(1.dp, eventColor.copy(alpha = if (iosColors.isDark) 0.35f else 0.25f)),
                RoundedCornerShape(16.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!event.startTime.isNullOrBlank()) {
                        Text(
                            text = "${event.startTime} - ${event.endTime ?: ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = eventColor
                        )
                    }
                    if (event.location.isNotBlank()) {
                        Text(
                            text = event.location,
                            fontSize = 12.sp,
                            color = iosColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDayScheduleCard(
    appProfileTitle: String,
    onAddLessonClick: () -> Unit,
    onAddEventClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(iosColors.cardSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Celebration,
                contentDescription = null,
                tint = IosBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Свободный день!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = iosColors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "На этот день нет запланированных $appProfileTitle",
            fontSize = 14.sp,
            color = iosColors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosBlue)
                    .clickable { onAddLessonClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "+ Добавить занятие",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(iosColors.cardSecondary)
                    .clickable { onAddEventClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "+ Событие",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosColors.textPrimary
                )
            }
        }
    }
}
