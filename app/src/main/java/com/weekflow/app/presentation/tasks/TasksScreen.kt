package com.weekflow.app.presentation.tasks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.FocusTimerDialog
import com.weekflow.app.core.designsystem.components.IosSegmentedControl
import com.weekflow.app.core.designsystem.components.RhythmAndRemindersSheet
import com.weekflow.app.core.designsystem.components.TaskItemRow
import com.weekflow.app.core.designsystem.components.UndoIncidentBanner
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import java.time.LocalDate
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.presentation.editors.PresetManagerSheet
import com.weekflow.app.presentation.editors.WeeklyInsightsSheet
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onAddNewTask: () -> Unit,
    onViewTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoSnapshot by viewModel.lastUndoSnapshot.collectAsState()
    val iosColors = LocalIosColors.current
    val appProfile = uiState.activePreset.appProfile
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    fun copyTextToClipboard(text: String, toastMessage: String) {
        if (text.isBlank()) {
            Toast.makeText(context, "Нет актуальных задач для копирования", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Задачи WeekFlow", text))
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    val activityMap = uiState.rawAllActivities.associateBy { it.id }

    var quickTaskText by remember { mutableStateOf("") }
    var quickTaskPriority by remember { mutableStateOf(TaskPriority.NONE) }

    var showFocusTimer by remember { mutableStateOf(false) }
    var focusTimerTitle by remember { mutableStateOf("Фокус-сессия") }
    var showSearchBar by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var showRhythmSheet by remember { mutableStateOf(false) }

    val presetColor = ColorParser.parseHexColor(uiState.activePreset.colorHex)

    CompositionLocalProvider(LocalAppProfile provides appProfile) {
    if (showRhythmSheet) {
        RhythmAndRemindersSheet(
            rhythmReminders = uiState.preferences.rhythmReminders,
            quickReminders = uiState.preferences.quickReminders,
            onUpdateRhythmReminders = { viewModel.updateRhythmReminders(it) },
            onUpdateQuickReminders = { viewModel.updateQuickReminders(it) },
            onDismiss = { showRhythmSheet = false }
        )
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
            activeWeekCycle = WeekCycle.EVERY_WEEK,
            preferences = uiState.preferences,
            onDismiss = { showInsightsSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosColors.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = appProfile.taskTerm,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Interactive Preset Pill
                        Box(
                            modifier = Modifier
                                .widthIn(min = 78.dp, max = 132.dp)
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
                                    text = if (uiState.preferences.showAllPresetsTasks) "Все пресеты" else uiState.activePreset.name,
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
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRhythmSheet = true
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Напоминания",
                            tint = IosBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val formatted = viewModel.buildFormattedTasksByDays()
                            copyTextToClipboard(formatted, "Задачи по дням скопированы в буфер обмена!")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Скопировать задачи по дням",
                            tint = IosBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) {
                                viewModel.onSearchQueryChanged("")
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Поиск задач",
                            tint = IosBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showInsightsSheet = true
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueryStats,
                            contentDescription = "Статистика",
                            tint = IosBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            focusTimerTitle = "Фокус-сессия"
                            showFocusTimer = true
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = "Фокус таймер",
                            tint = IosBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddNewTask()
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

            // Search Bar (Expandable)
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Поиск по задачам, шагам или предметам...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = iosColors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Очистить",
                                    tint = iosColors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            // Segmented Control (4 filter tabs)
            IosSegmentedControl(
                items = listOf("Актуальные", "По дням", appProfile.bySubjectTabLabel, "Готовые"),
                selectedIndex = uiState.selectedTab,
                onItemSelected = { viewModel.selectTab(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // TickTick Quick Add Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iosColors.card)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority picker button
                val priorityColor = when (quickTaskPriority) {
                    TaskPriority.HIGH -> Color(0xFFFF3B30)
                    TaskPriority.MEDIUM -> Color(0xFFFF9500)
                    TaskPriority.LOW -> Color(0xFF007AFF)
                    TaskPriority.NONE -> Color(0xFF8E8E93)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(priorityColor.copy(alpha = 0.12f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            quickTaskPriority = when (quickTaskPriority) {
                                TaskPriority.NONE -> TaskPriority.LOW
                                TaskPriority.LOW -> TaskPriority.MEDIUM
                                TaskPriority.MEDIUM -> TaskPriority.HIGH
                                TaskPriority.HIGH -> TaskPriority.NONE
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = "Приоритет",
                        tint = priorityColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = quickTaskText,
                    onValueChange = { quickTaskText = it },
                    placeholder = {
                        Text(
                            text = "Быстро добавить задачу на сегодня...",
                            fontSize = 13.sp,
                            color = iosColors.textSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (quickTaskText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.quickAddTask(quickTaskText, quickTaskPriority)
                                quickTaskText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (quickTaskText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.quickAddTask(quickTaskText, quickTaskPriority)
                            quickTaskText = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Создать",
                            tint = IosBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (uiState.selectedTab) {
                    0 -> {
                        // Tab 0: "Актуальные"
                        // 1. Просроченные
                        if (uiState.overdueTasks.isNotEmpty()) {
                            item(key = "header_overdue") {
                                TaskSectionHeader(title = "Просроченные", color = IosRed, count = uiState.overdueTasks.size)
                            }
                            items(uiState.overdueTasks, key = { "task_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onClick = { onViewTask(task) },
                                    onEditClick = { onEditTask(task) },
                                    onFocusTimerClick = {
                                        focusTimerTitle = task.title
                                        showFocusTimer = true
                                    },
                                    onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) },
                                    onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                )
                            }
                        }

                        // 2. На сегодня и завтра
                        if (uiState.todayAndTomorrowTasks.isNotEmpty()) {
                            item(key = "header_today_tomorrow") {
                                TaskSectionHeader(title = "На сегодня и завтра", color = IosOrange, count = uiState.todayAndTomorrowTasks.size)
                            }
                            items(uiState.todayAndTomorrowTasks, key = { "task_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onClick = { onViewTask(task) },
                                    onEditClick = { onEditTask(task) },
                                    onFocusTimerClick = {
                                        focusTimerTitle = task.title
                                        showFocusTimer = true
                                    },
                                    onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) },
                                    onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                )
                            }
                        }

                        // 3. На ближайшие 7 дней
                        if (uiState.nextWeekTasks.isNotEmpty()) {
                            item(key = "header_next_week") {
                                TaskSectionHeader(title = "На ближайшие 7 дней", color = IosBlue, count = uiState.nextWeekTasks.size)
                            }
                            items(uiState.nextWeekTasks, key = { "task_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onClick = { onViewTask(task) },
                                    onEditClick = { onEditTask(task) },
                                    onFocusTimerClick = {
                                        focusTimerTitle = task.title
                                        showFocusTimer = true
                                    },
                                    onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) },
                                    onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                )
                            }
                        }

                        // 4. Позже / Без срока
                        if (uiState.laterTasks.isNotEmpty()) {
                            item(key = "header_later") {
                                TaskSectionHeader(title = "Позже / Без срока", color = iosColors.textSecondary, count = uiState.laterTasks.size)
                            }
                            items(uiState.laterTasks, key = { "task_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onClick = { onViewTask(task) },
                                    onEditClick = { onEditTask(task) },
                                    onFocusTimerClick = {
                                        focusTimerTitle = task.title
                                        showFocusTimer = true
                                    },
                                    onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) },
                                    onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                )
                            }
                        }

                        // Empty State if no pending tasks
                        if (uiState.overdueTasks.isEmpty() &&
                            uiState.todayAndTomorrowTasks.isEmpty() &&
                            uiState.nextWeekTasks.isEmpty() &&
                            uiState.laterTasks.isEmpty()
                        ) {
                            item(key = "tasks_empty") {
                                EmptyTasksState(
                                    title = if (uiState.searchQuery.isNotBlank()) "Ничего не найдено" else "Все задачи выполнены!",
                                    subtitle = if (uiState.searchQuery.isNotBlank()) "Попробуйте изменить запрос поиска." else "У вас нет невыполненных задач. Нажмите +, чтобы добавить новую.",
                                    onAddNew = onAddNewTask
                                )
                            }
                        }
                    }

                    1 -> {
                        // Tab 1: "По дням"
                        val today = LocalDate.now()
                        if (uiState.tasksByDate.isNotEmpty()) {
                            uiState.tasksByDate.forEach { (date, tasks) ->
                                val dayKey = date?.toString() ?: "no_date"
                                val headerTitle = when {
                                    date == null -> "Без даты"
                                    date.isEqual(today) -> "Сегодня • ${DateTimeUtils.getDayOfWeekFull(date.dayOfWeek.value)}, ${DateTimeUtils.formatShortDayMonth(date)}"
                                    date.isEqual(today.plusDays(1)) -> "Завтра • ${DateTimeUtils.getDayOfWeekFull(date.dayOfWeek.value)}, ${DateTimeUtils.formatShortDayMonth(date)}"
                                    else -> "${DateTimeUtils.getDayOfWeekFull(date.dayOfWeek.value)}, ${DateTimeUtils.formatShortDayMonth(date)}"
                                }
                                val headerColor = when {
                                    date == null -> iosColors.textSecondary
                                    date.isBefore(today) -> IosRed
                                    date.isEqual(today) -> IosOrange
                                    else -> IosBlue
                                }

                                item(key = "day_hdr_$dayKey") {
                                    TaskSectionHeader(
                                        title = headerTitle,
                                        color = headerColor,
                                        count = tasks.size,
                                        onCopyGroupClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val text = viewModel.buildFormattedTasksByDays(date to tasks)
                                            copyTextToClipboard(text, "Скопированы задачи на «$headerTitle»!")
                                        }
                                    )
                                }
                                items(tasks, key = { "day_task_${it.id}" }) { task ->
                                    TaskItemRow(
                                        task = task,
                                        linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                        onToggleCompleted = { viewModel.toggleTask(task) },
                                        onClick = { onViewTask(task) },
                                        onEditClick = { onEditTask(task) },
                                        onFocusTimerClick = {
                                            focusTimerTitle = task.title
                                            showFocusTimer = true
                                        },
                                        onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                        onDeleteClick = { viewModel.deleteTask(task.id) },
                                        onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                    )
                                }
                            }
                        } else {
                            item(key = "tasks_by_day_empty") {
                                EmptyTasksState(
                                    title = "Нет задач по дням",
                                    subtitle = "Все активные задачи сгруппируются здесь по дням недели и датам сдачи.",
                                    onAddNew = onAddNewTask
                                )
                            }
                        }
                    }

                    2 -> {
                        // Tab 2: "По предметам"
                        if (uiState.tasksByActivity.isNotEmpty() || uiState.unassignedTasks.isNotEmpty()) {
                            uiState.tasksByActivity.forEach { (activity, tasks) ->
                                item(key = "act_hdr_${activity.id}") {
                                    val actColor = ColorParser.parseHexColor(activity.colorHex)
                                    TaskSectionHeader(title = activity.name, color = actColor, count = tasks.size)
                                }
                                items(tasks, key = { "act_task_${it.id}" }) { task ->
                                    TaskItemRow(
                                        task = task,
                                        linkedActivity = activity,
                                        onToggleCompleted = { viewModel.toggleTask(task) },
                                        onClick = { onViewTask(task) },
                                        onEditClick = { onEditTask(task) },
                                        onFocusTimerClick = {
                                            focusTimerTitle = task.title
                                            showFocusTimer = true
                                        },
                                        onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                        onDeleteClick = { viewModel.deleteTask(task.id) },
                                        onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                    )
                                }
                            }

                            if (uiState.unassignedTasks.isNotEmpty()) {
                                item(key = "unassigned_hdr") {
                                    TaskSectionHeader(title = "Без привязки к предмету", color = iosColors.textSecondary, count = uiState.unassignedTasks.size)
                                }
                                items(uiState.unassignedTasks, key = { "unassigned_task_${it.id}" }) { task ->
                                    TaskItemRow(
                                        task = task,
                                        linkedActivity = null,
                                        onToggleCompleted = { viewModel.toggleTask(task) },
                                        onClick = { onViewTask(task) },
                                        onEditClick = { onEditTask(task) },
                                        onFocusTimerClick = {
                                            focusTimerTitle = task.title
                                            showFocusTimer = true
                                        },
                                        onPostponeTomorrowClick = { viewModel.postponeToTomorrow(task) },
                                        onDeleteClick = { viewModel.deleteTask(task.id) },
                                        onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) }
                                    )
                                }
                            }
                        } else {
                            item(key = "tasks_by_act_empty") {
                                EmptyTasksState(
                                    title = "Задач по предметам нет",
                                    subtitle = "Привязывайте задачи к предметам при создании для удобной группировки.",
                                    onAddNew = onAddNewTask
                                )
                            }
                        }
                    }

                    3 -> {
                        // Tab 3: "Выполненные"
                        if (uiState.completedTasks.isNotEmpty()) {
                            item(key = "clear_completed_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Выполнено: ${uiState.completedTasks.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textSecondary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(IosRed.copy(alpha = 0.12f))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.clearCompletedTasks()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteSweep,
                                                contentDescription = null,
                                                tint = IosRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Очистить выполненные",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = IosRed
                                            )
                                        }
                                    }
                                }
                            }
                            items(uiState.completedTasks, key = { "completed_${it.id}" }) { task ->
                                TaskItemRow(
                                    task = task,
                                    linkedActivity = task.linkedActivityId?.let { activityMap[it] },
                                    onToggleCompleted = { viewModel.toggleTask(task) },
                                    onClick = { onViewTask(task) },
                                    onEditClick = { onEditTask(task) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) }
                                )
                            }
                        } else {
                            item(key = "completed_empty") {
                                EmptyTasksState(
                                    title = "Нет выполненных задач",
                                    subtitle = "Завершенные задачи будут отображаться в этом списке.",
                                    onAddNew = onAddNewTask
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFocusTimer) {
        FocusTimerDialog(
            initialTitle = focusTimerTitle,
            onDismiss = { showFocusTimer = false },
            onSessionCompleted = { mins -> viewModel.recordFocusSession(mins) }
        )
    }
    }
}

@Composable
fun TaskSectionHeader(
    title: String,
    color: Color,
    count: Int,
    onCopyGroupClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (onCopyGroupClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .clickable { onCopyGroupClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Скопировать",
                        tint = color,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Копировать",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = iosColors.textSecondary
        )
    }
}

@Composable
fun EmptyTasksState(
    title: String,
    subtitle: String,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(IosGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = IosGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = iosColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = iosColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(IosBlue)
                    .clickable { onAddNew() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "+ Добавить задачу",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
