package com.weekflow.app.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.FocusTimerDialog
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.usecase.CalculateNextLessonDateUseCase
import com.weekflow.app.domain.usecase.DetectActiveOrRecentActivityUseCase
import com.weekflow.app.presentation.calendar.CalendarScreen
import com.weekflow.app.presentation.calendar.CalendarViewModel
import com.weekflow.app.presentation.editors.ActivityEditorSheet
import com.weekflow.app.presentation.editors.EventEditorSheet
import com.weekflow.app.presentation.editors.SlotEditorSheet
import com.weekflow.app.presentation.editors.TaskDetailSheet
import com.weekflow.app.presentation.editors.TaskEditorSheet
import com.weekflow.app.presentation.schedule.ScheduleScreen
import com.weekflow.app.presentation.schedule.ScheduleViewModel
import com.weekflow.app.presentation.settings.SettingsScreen
import com.weekflow.app.presentation.settings.SettingsViewModel
import com.weekflow.app.presentation.tasks.TasksScreen
import com.weekflow.app.presentation.tasks.TasksViewModel
import com.weekflow.app.widget.WeekFlowWidgetUpdater
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDate

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SCHEDULE("Расписание", Icons.Rounded.CalendarViewWeek, Icons.Outlined.CalendarViewWeek),
    CALENDAR("Календарь", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    TASKS("Задачи", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle),
    SETTINGS("Настройки", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data class AddSlot(
        val defaultDayOfWeek: Int,
        val defaultPeriodOrder: Int? = null,
        val defaultStartTime: String? = null,
        val defaultEndTime: String? = null,
        val defaultActivityId: String? = null
    ) : ActiveSheet
    data class EditSlot(
        val slotId: String,
        val dateIso: String,
        val defaultActivityId: String? = null
    ) : ActiveSheet
    data class AddTask(val activityId: String?, val slotId: String?, val date: LocalDate) : ActiveSheet
    data class ViewTask(val taskId: String, val fallbackTask: Task) : ActiveSheet
    data class EditTask(val task: Task) : ActiveSheet
    data class AddEvent(val defaultDateIso: String) : ActiveSheet
    data class EditEvent(val event: CalendarEvent) : ActiveSheet
    data object AddActivity : ActiveSheet
    data class EditActivity(val activityId: String) : ActiveSheet
}

@Composable
fun WeekFlowNavHost(
    modifier: Modifier = Modifier,
    initialAction: String? = null,
    onActionHandled: () -> Unit = {}
) {
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scheduleViewModel: ScheduleViewModel = koinViewModel()
    val calendarViewModel: CalendarViewModel = koinViewModel()
    val tasksViewModel: TasksViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()

    val scheduleRepository: ScheduleRepository = koinInject()
    val notificationScheduler: NotificationScheduler = koinInject()
    val calculateNextLessonDateUseCase: CalculateNextLessonDateUseCase = koinInject()
    val detectActiveOrRecentActivityUseCase: DetectActiveOrRecentActivityUseCase = koinInject()

    val scheduleUiState by scheduleViewModel.uiState.collectAsState()
    val tasksUiState by tasksViewModel.uiState.collectAsState()
    val activeAppProfile = scheduleUiState.activePreset.appProfile

    var currentTab by remember { mutableStateOf(NavigationTab.SCHEDULE) }
    var activeSheet by remember { mutableStateOf<ActiveSheet>(ActiveSheet.None) }
    var returnSheetAfterActivity by remember { mutableStateOf<ActiveSheet?>(null) }
    var navFocusTimerTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(
        scheduleUiState.activePreset.id,
        scheduleUiState.allSlots.size,
        scheduleUiState.allTasks.size,
        scheduleUiState.preferences.isTwoWeekCycleEnabled,
        scheduleUiState.preferences.invertWeekCycle
    ) {
        try {
            WeekFlowWidgetUpdater.forceUpdateAll(context)
        } catch (_: Exception) {}
    }

    LaunchedEffect(initialAction) {
        if (initialAction == "com.weekflow.app.ACTION_ADD_TASK") {
            currentTab = NavigationTab.TASKS
            activeSheet = ActiveSheet.AddTask(
                activityId = null,
                slotId = null,
                date = LocalDate.now()
            )
            onActionHandled()
        }
    }

    fun notifyWidgetUpdate() {
        scope.launch {
            try {
                WeekFlowWidgetUpdater.forceUpdateAll(context)
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }

    CompositionLocalProvider(LocalAppProfile provides activeAppProfile) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Sleek iOS / TickTick bottom tab bar without blocking blocks
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = iosColors.card.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Subtle 0.5.dp top separator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(iosColors.separator.copy(alpha = 0.5f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val isSelected = tab == currentTab
                            val tabLabel = tab.title
                            val interactionSource = remember { MutableInteractionSource() }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (currentTab != tab) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentTab = tab
                                        }
                                    }
                                    .padding(vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) IosBlue.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel,
                                        tint = if (isSelected) IosBlue else iosColors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = tabLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) IosBlue else iosColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    NavigationTab.SCHEDULE -> {
                        ScheduleScreen(
                            viewModel = scheduleViewModel,
                            onAddNewSlot = { day ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddSlot(defaultDayOfWeek = day)
                            },
                            onAddNewSlotWithPeriod = { day, periodOrder ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddSlot(
                                    defaultDayOfWeek = day,
                                    defaultPeriodOrder = periodOrder
                                )
                            },
                            onAddNewSlotWithTime = { day, startTime, endTime ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddSlot(
                                    defaultDayOfWeek = day,
                                    defaultStartTime = startTime,
                                    defaultEndTime = endTime
                                )
                            },
                            onEditSlot = { slotId, dateIso ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditSlot(slotId, dateIso)
                            },
                            onAddNewTaskForSlot = { actId, slotId, date ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddTask(actId, slotId, date)
                            },
                            onEditTask = { task ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.ViewTask(task.id, task)
                            },
                            onAddNewEvent = { dateIso ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddEvent(dateIso)
                            },
                            onEditEvent = { event ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditEvent(event)
                            }
                        )
                    }
                    NavigationTab.CALENDAR -> {
                        CalendarScreen(
                            viewModel = calendarViewModel,
                            onAddNewEvent = { dateIso ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddEvent(dateIso)
                            },
                            onEditEvent = { event ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditEvent(event)
                            },
                            onEditSlot = { slotId, dateIso ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditSlot(slotId, dateIso)
                            },
                            onAddNewTaskForSlot = { actId, slotId, date ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddTask(actId, slotId, date)
                            },
                            onEditTask = { task ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.ViewTask(task.id, task)
                            }
                        )
                    }
                    NavigationTab.TASKS -> {
                        TasksScreen(
                            viewModel = tasksViewModel,
                            onAddNewTask = {
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddTask(null, null, LocalDate.now())
                            },
                            onViewTask = { task ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.ViewTask(task.id, task)
                            },
                            onEditTask = { task ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditTask(task)
                            }
                        )
                    }
                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onAddNewActivity = {
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.AddActivity
                            },
                            onEditActivity = { actId ->
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.EditActivity(actId)
                            }
                        )
                    }
                }
            }
        }

        // Active Bottom Sheets
        val currentPresetId = scheduleUiState.activePreset.id
        when (val sheet = activeSheet) {
            is ActiveSheet.AddSlot -> {
                SlotEditorSheet(
                    initialSlot = null,
                    defaultDayOfWeek = sheet.defaultDayOfWeek,
                    defaultPeriodOrder = sheet.defaultPeriodOrder,
                    defaultStartTime = sheet.defaultStartTime,
                    defaultEndTime = sheet.defaultEndTime,
                    defaultActivityId = sheet.defaultActivityId,
                    activities = scheduleUiState.allActivities,
                    existingSlots = scheduleUiState.allSlots,
                    isTwoWeekCycleEnabled = scheduleUiState.preferences.isTwoWeekCycleEnabled,
                    presetId = currentPresetId,
                    onDismiss = {
                        returnSheetAfterActivity = null
                        activeSheet = ActiveSheet.None
                    },
                    onSave = { slot ->
                        scope.launch {
                            scheduleRepository.upsertSlot(slot)
                            notifyWidgetUpdate()
                        }
                    },
                    onAddNewActivityRequest = {
                        returnSheetAfterActivity = sheet
                        activeSheet = ActiveSheet.AddActivity
                    },
                    onEditActivityRequest = { actId ->
                        returnSheetAfterActivity = sheet
                        activeSheet = ActiveSheet.EditActivity(actId)
                    }
                )
            }
            is ActiveSheet.EditSlot -> {
                val slot = scheduleUiState.rawAllSlots.firstOrNull { it.id == sheet.slotId }
                if (slot != null) {
                    val slotActivities = scheduleUiState.rawAllActivities.filter { it.presetId == slot.presetId }
                    val slotSlots = scheduleUiState.rawAllSlots.filter { it.presetId == slot.presetId }
                    val slotProfile = scheduleUiState.allPresets.firstOrNull { it.id == slot.presetId }?.appProfile
                        ?: activeAppProfile
                    CompositionLocalProvider(LocalAppProfile provides slotProfile) {
                        SlotEditorSheet(
                            initialSlot = slot,
                            defaultDayOfWeek = slot.dayOfWeek,
                            defaultActivityId = sheet.defaultActivityId,
                            targetDateIso = sheet.dateIso,
                            activities = slotActivities,
                            existingSlots = slotSlots,
                            isTwoWeekCycleEnabled = scheduleUiState.preferences.isTwoWeekCycleEnabled,
                            presetId = slot.presetId,
                            onDismiss = {
                                returnSheetAfterActivity = null
                                activeSheet = ActiveSheet.None
                            },
                            onSave = { updatedSlot ->
                                scope.launch {
                                    scheduleRepository.upsertSlot(updatedSlot)
                                    notifyWidgetUpdate()
                                }
                            },
                            onDelete = { slotId ->
                                scope.launch {
                                    scheduleRepository.deleteSlot(slotId)
                                    notifyWidgetUpdate()
                                }
                            },
                            onAddNewActivityRequest = {
                                returnSheetAfterActivity = sheet
                                activeSheet = ActiveSheet.AddActivity
                            },
                            onEditActivityRequest = { actId ->
                                returnSheetAfterActivity = sheet
                                activeSheet = ActiveSheet.EditActivity(actId)
                            }
                        )
                    }
                }
            }
            is ActiveSheet.AddTask -> {
                TaskEditorSheet(
                    initialTask = null,
                    initialActivityId = sheet.activityId,
                    initialSlotId = sheet.slotId,
                    referenceDate = sheet.date,
                    activities = scheduleUiState.allActivities,
                    allSlots = scheduleUiState.allSlots,
                    vacations = scheduleUiState.allVacations,
                    preferences = scheduleUiState.preferences,
                    calculateNextLessonDateUseCase = calculateNextLessonDateUseCase,
                    detectActiveOrRecentActivityUseCase = detectActiveOrRecentActivityUseCase,
                    presetId = currentPresetId,
                    onDismiss = {
                        returnSheetAfterActivity = null
                        activeSheet = ActiveSheet.None
                    },
                    onAddNewActivityRequest = {
                        returnSheetAfterActivity = sheet
                        activeSheet = ActiveSheet.AddActivity
                    },
                    onSave = { task ->
                        scope.launch {
                            scheduleRepository.upsertTask(task)
                            notifyWidgetUpdate()
                        }
                    }
                )
            }
            is ActiveSheet.ViewTask -> {
                val liveTask = tasksUiState.rawAllTasks.firstOrNull { it.id == sheet.taskId }
                    ?: scheduleUiState.allTasks.firstOrNull { it.id == sheet.taskId }
                    ?: sheet.fallbackTask
                val linkedAct = liveTask.linkedActivityId?.let { actId ->
                    scheduleUiState.rawAllActivities.firstOrNull { it.id == actId }
                }
                TaskDetailSheet(
                    task = liveTask,
                    linkedActivity = linkedAct,
                    onDismiss = {
                        returnSheetAfterActivity = null
                        activeSheet = ActiveSheet.None
                    },
                    onEditClick = {
                        activeSheet = ActiveSheet.EditTask(liveTask)
                    },
                    onToggleCompleted = {
                        scope.launch {
                            scheduleRepository.toggleTaskCompleted(liveTask.id, !liveTask.isCompleted)
                            notifyWidgetUpdate()
                        }
                    },
                    onUpdateTask = { updatedTask ->
                        scope.launch {
                            scheduleRepository.upsertTask(updatedTask)
                            notifyWidgetUpdate()
                        }
                    },
                    onDeleteTask = {
                        scope.launch {
                            scheduleRepository.deleteTask(liveTask.id)
                            notifyWidgetUpdate()
                        }
                        activeSheet = ActiveSheet.None
                    },
                    onStartFocusTimer = { title ->
                        activeSheet = ActiveSheet.None
                        navFocusTimerTitle = title
                    }
                )
            }
            is ActiveSheet.EditTask -> {
                val taskPresetId = sheet.task.presetId.ifBlank { currentPresetId }
                val taskActivities = scheduleUiState.rawAllActivities.filter { it.presetId == taskPresetId }
                val taskSlots = scheduleUiState.rawAllSlots.filter { it.presetId == taskPresetId }
                TaskEditorSheet(
                    initialTask = sheet.task,
                    initialActivityId = sheet.task.linkedActivityId,
                    initialSlotId = sheet.task.linkedSlotId,
                    referenceDate = sheet.task.parseDueDate() ?: LocalDate.now(),
                    activities = taskActivities.ifEmpty { scheduleUiState.allActivities },
                    allSlots = taskSlots.ifEmpty { scheduleUiState.allSlots },
                    vacations = scheduleUiState.allVacations,
                    preferences = scheduleUiState.preferences,
                    calculateNextLessonDateUseCase = calculateNextLessonDateUseCase,
                    detectActiveOrRecentActivityUseCase = detectActiveOrRecentActivityUseCase,
                    presetId = taskPresetId,
                    onDismiss = {
                        returnSheetAfterActivity = null
                        activeSheet = ActiveSheet.None
                    },
                    onAddNewActivityRequest = {
                        returnSheetAfterActivity = sheet
                        activeSheet = ActiveSheet.AddActivity
                    },
                    onSave = { task ->
                        scope.launch {
                            scheduleRepository.upsertTask(task)
                            notifyWidgetUpdate()
                        }
                    },
                    onDelete = { taskId ->
                        scope.launch {
                            scheduleRepository.deleteTask(taskId)
                            notifyWidgetUpdate()
                        }
                    }
                )
            }
            is ActiveSheet.AddEvent -> {
                EventEditorSheet(
                    initialEvent = null,
                    defaultDateIso = sheet.defaultDateIso,
                    presetId = currentPresetId,
                    onDismiss = { activeSheet = ActiveSheet.None },
                    onSave = { event ->
                        scope.launch {
                            scheduleRepository.upsertEvent(event)
                            notifyWidgetUpdate()
                        }
                    }
                )
            }
            is ActiveSheet.EditEvent -> {
                EventEditorSheet(
                    initialEvent = sheet.event,
                    defaultDateIso = sheet.event.dateIso,
                    presetId = sheet.event.presetId.ifBlank { currentPresetId },
                    onDismiss = { activeSheet = ActiveSheet.None },
                    onSave = { event ->
                        scope.launch {
                            scheduleRepository.upsertEvent(event)
                            notifyWidgetUpdate()
                        }
                    },
                    onDelete = { eventId ->
                        scope.launch {
                            scheduleRepository.deleteEvent(eventId)
                            notifyWidgetUpdate()
                        }
                    }
                )
            }
            is ActiveSheet.AddActivity -> {
                ActivityEditorSheet(
                    initialActivity = null,
                    presetId = currentPresetId,
                    onDismiss = {
                        val parent = returnSheetAfterActivity
                        returnSheetAfterActivity = null
                        activeSheet = parent ?: ActiveSheet.None
                    },
                    onSave = { act ->
                        scope.launch {
                            scheduleRepository.upsertActivity(act)
                            notifyWidgetUpdate()
                        }
                        val parent = returnSheetAfterActivity
                        returnSheetAfterActivity = null
                        activeSheet = when (parent) {
                            is ActiveSheet.AddSlot -> parent.copy(defaultActivityId = act.id)
                            is ActiveSheet.EditSlot -> parent.copy(defaultActivityId = act.id)
                            is ActiveSheet.AddTask -> parent.copy(activityId = act.id)
                            is ActiveSheet.EditTask -> parent.copy(task = parent.task.copy(linkedActivityId = act.id))
                            else -> ActiveSheet.None
                        }
                    }
                )
            }
            is ActiveSheet.EditActivity -> {
                val act = scheduleUiState.rawAllActivities.firstOrNull { it.id == sheet.activityId }
                if (act != null) {
                    ActivityEditorSheet(
                        initialActivity = act,
                        presetId = act.presetId,
                        onDismiss = {
                            val parent = returnSheetAfterActivity
                            returnSheetAfterActivity = null
                            activeSheet = parent ?: ActiveSheet.None
                        },
                        onSave = { updatedAct ->
                            scope.launch {
                                scheduleRepository.upsertActivity(updatedAct)
                                notifyWidgetUpdate()
                            }
                            val parent = returnSheetAfterActivity
                            returnSheetAfterActivity = null
                            activeSheet = parent ?: ActiveSheet.None
                        },
                        onDelete = { actId ->
                            scope.launch {
                                scheduleRepository.deleteActivity(actId)
                                notifyWidgetUpdate()
                            }
                            val parent = returnSheetAfterActivity
                            returnSheetAfterActivity = null
                            activeSheet = parent ?: ActiveSheet.None
                        }
                    )
                }
            }
            ActiveSheet.None -> {}
        }

        navFocusTimerTitle?.let { timerTitle ->
            FocusTimerDialog(
                initialTitle = timerTitle,
                onDismiss = { navFocusTimerTitle = null },
                onSessionCompleted = { mins ->
                    tasksViewModel.recordFocusSession(mins)
                }
            )
        }
    }
    }
}
