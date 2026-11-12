package com.weekflow.app.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskAttachment
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.repository.UndoSnapshotInfo
import com.weekflow.app.core.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class TasksUiState(
    val selectedTab: Int = 0, // 0 = Актуальные, 1 = По дням, 2 = По предметам, 3 = Выполненные
    val searchQuery: String = "",
    val overdueTasks: List<Task> = emptyList(),
    val todayAndTomorrowTasks: List<Task> = emptyList(),
    val nextWeekTasks: List<Task> = emptyList(),
    val laterTasks: List<Task> = emptyList(),
    val tasksByDate: List<Pair<LocalDate?, List<Task>>> = emptyList(),
    val tasksByActivity: Map<Activity, List<Task>> = emptyMap(),
    val unassignedTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val allPresets: List<SchedulePreset> = listOf(SchedulePreset.DEFAULT_PRESET),
    val activePreset: SchedulePreset = SchedulePreset.DEFAULT_PRESET,
    val allActivities: List<Activity> = emptyList(),
    val allSlots: List<Slot> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val rawAllActivities: List<Activity> = emptyList(),
    val rawAllSlots: List<Slot> = emptyList(),
    val rawAllTasks: List<Task> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class TasksViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TasksUiState> = combine(
        _selectedTab,
        _searchQuery,
        scheduleRepository.getAllPresets(),
        scheduleRepository.getAllTasks(),
        scheduleRepository.getAllActivities(),
        scheduleRepository.getAllSlots(),
        preferencesRepository.userPreferences
    ) { params ->
        val tab = params[0] as Int
        val query = (params[1] as String).trim()
        @Suppress("UNCHECKED_CAST")
        val presets = params[2] as List<SchedulePreset>
        @Suppress("UNCHECKED_CAST")
        val rawTasks = params[3] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val rawActivities = params[4] as List<Activity>
        @Suppress("UNCHECKED_CAST")
        val rawSlots = params[5] as List<Slot>
        val preferences = params[6] as UserPreferences

        val activePreset = presets.firstOrNull { it.id == preferences.activePresetId }
            ?: presets.firstOrNull()
            ?: SchedulePreset.DEFAULT_PRESET

        val activities = if (preferences.showAllPresetsTasks) {
            rawActivities
        } else {
            rawActivities.filter { it.presetId == activePreset.id }
        }
        val slots = rawSlots.filter { it.presetId == activePreset.id }
        val activityMap = rawActivities.associateBy { it.id }

        val presetTasks = if (preferences.showAllPresetsTasks) {
            rawTasks
        } else {
            rawTasks.filter { it.presetId == activePreset.id }
        }

        val tasks = if (query.isBlank()) {
            presetTasks
        } else {
            val q = query.lowercase()
            presetTasks.filter { task ->
                val act = task.linkedActivityId?.let { activityMap[it] }
                task.title.lowercase().contains(q) ||
                    (task.note?.lowercase()?.contains(q) == true) ||
                    task.subtasks.any { it.title.lowercase().contains(q) } ||
                    (act?.name?.lowercase()?.contains(q) == true) ||
                    (act?.shortName?.lowercase()?.contains(q) == true)
            }
        }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val endOfNextWeek = today.plusDays(7)

        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }.sortedByDescending { it.dueDateIso }

        // Grouping for Tab 0: "Актуальные"
        val overdue = mutableListOf<Task>()
        val todayAndTomorrow = mutableListOf<Task>()
        val nextWeek = mutableListOf<Task>()
        val later = mutableListOf<Task>()

        for (task in pending) {
            val dueDate = task.parseDueDate()
            when {
                dueDate == null -> later.add(task)
                dueDate.isBefore(today) -> overdue.add(task)
                dueDate.isEqual(today) || dueDate.isEqual(tomorrow) -> todayAndTomorrow.add(task)
                dueDate.isAfter(tomorrow) && !dueDate.isAfter(endOfNextWeek) -> nextWeek.add(task)
                else -> later.add(task)
            }
        }

        // Grouping for Tab 1: "По дням"
        val byDateMap = pending.groupBy { it.parseDueDate() }
        val sortedByDate: List<Pair<LocalDate?, List<Task>>> = byDateMap.entries
            .sortedWith(compareBy(nullsLast()) { it.key })
            .map { (date, taskList) ->
                date to taskList.sortedWith(compareBy({ it.priority.ordinal }, { it.title }))
            }

        // Grouping for Tab 2: "По предметам"
        val byActivity = mutableMapOf<Activity, MutableList<Task>>()
        val unassigned = mutableListOf<Task>()

        for (task in pending) {
            val actId = task.linkedActivityId
            if (actId != null && activityMap.containsKey(actId)) {
                val act = activityMap[actId]!!
                byActivity.getOrPut(act) { mutableListOf() }.add(task)
            } else {
                unassigned.add(task)
            }
        }

        TasksUiState(
            selectedTab = tab,
            searchQuery = params[1] as String,
            overdueTasks = overdue.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso })),
            todayAndTomorrowTasks = todayAndTomorrow.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso })),
            nextWeekTasks = nextWeek.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso })),
            laterTasks = later.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso })),
            tasksByDate = sortedByDate,
            tasksByActivity = byActivity.mapValues { (_, taskList) ->
                taskList.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso }))
            },
            unassignedTasks = unassigned.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDateIso })),
            completedTasks = completed,
            allPresets = presets,
            activePreset = activePreset,
            allActivities = activities,
            allSlots = slots,
            allTasks = presetTasks,
            rawAllActivities = rawActivities,
            rawAllSlots = rawSlots,
            rawAllTasks = rawTasks,
            preferences = preferences
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TasksUiState()
    )

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectPreset(preset: SchedulePreset) {
        viewModelScope.launch {
            preferencesRepository.updateActivePresetId(preset.id)
        }
    }

    fun savePreset(preset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.upsertPreset(preset)
        }
    }

    fun duplicatePreset(sourcePresetId: String, newPreset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.duplicatePreset(sourcePresetId, newPreset)
            preferencesRepository.updateActivePresetId(newPreset.id)
        }
    }

    fun deletePreset(presetId: String) {
        if (presetId == SchedulePreset.DEFAULT_ID) return
        viewModelScope.launch {
            scheduleRepository.deletePreset(presetId)
            if (uiState.value.activePreset.id == presetId) {
                preferencesRepository.updateActivePresetId(SchedulePreset.DEFAULT_ID)
            }
        }
    }

    fun toggleShowAllPresetsTasks(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowAllPresetsTasks(show)
        }
    }

    fun recordFocusSession(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.recordCompletedFocusSession(minutes)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            scheduleRepository.toggleTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun toggleSubtask(task: Task, subtaskId: String) {
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
        }
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(subtasks = updatedSubtasks))
        }
    }

    val lastUndoSnapshot: StateFlow<UndoSnapshotInfo?> = scheduleRepository.lastUndoSnapshot.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun restoreLastUndo() {
        viewModelScope.launch {
            scheduleRepository.restoreLastUndoSnapshot()
        }
    }

    fun dismissUndo() {
        viewModelScope.launch {
            scheduleRepository.dismissUndoSnapshot()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteTask(taskId)
        }
    }

    fun clearCompletedTasks() {
        val presetId = if (uiState.value.preferences.showAllPresetsTasks) null else uiState.value.activePreset.id
        viewModelScope.launch {
            scheduleRepository.deleteCompletedTasks(presetId)
        }
    }

    fun postponeToTomorrow(task: Task) {
        val tomorrowIso = LocalDate.now().plusDays(1).toString()
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(dueDateIso = tomorrowIso))
        }
    }

    fun quickAddTask(title: String, priority: TaskPriority = TaskPriority.NONE) {
        if (title.isBlank()) return
        val currentPresetId = uiState.value.activePreset.id
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            dueDateIso = LocalDate.now().toString(),
            priority = priority,
            presetId = currentPresetId
        )
        viewModelScope.launch {
            scheduleRepository.upsertTask(newTask)
        }
    }

    fun upsertTask(task: Task) {
        viewModelScope.launch {
            scheduleRepository.upsertTask(task)
        }
    }

    fun updateTaskSubtasks(task: Task, items: List<SubtaskItem>) {
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(subtasks = items))
        }
    }

    fun updateTaskAttachments(task: Task, attachments: List<TaskAttachment>) {
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(attachments = attachments))
        }
    }

    fun addQuickFollowUpTask(title: String, activityId: String?, presetId: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            scheduleRepository.upsertTask(
                Task(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    dueDateIso = LocalDate.now().plusDays(1).toString(),
                    linkedActivityId = activityId,
                    presetId = presetId
                )
            )
        }
    }

    fun updateRegularReminders(reminders: List<RegularReminder>) {
        viewModelScope.launch {
            preferencesRepository.updateRegularReminders(reminders)
            try {
                notificationScheduler?.scheduleRegularReminders()
            } catch (_: Exception) {}
        }
    }

    fun buildFormattedTasksByDays(singleDayFilter: Pair<LocalDate?, List<Task>>? = null): String {
        val state = uiState.value
        val actMap = state.rawAllActivities.associateBy { it.id }
        val groups = if (singleDayFilter != null) {
            listOf(singleDayFilter)
        } else {
            state.tasksByDate
        }
        if (groups.isEmpty()) return ""

        return buildString {
            groups.forEachIndexed { idx, (date, tasks) ->
                if (idx > 0) append("\n\n")
                val header = if (date != null) {
                    val dayShort = DateTimeUtils.getDayOfWeekShort(date.dayOfWeek.value)
                    val fullDate = DateTimeUtils.formatShortDayMonth(date)
                    "📅 $dayShort ($fullDate) — задачи:"
                } else {
                    "📌 Без срока — задачи:"
                }
                append(header)
                tasks.forEach { task ->
                    val subjectName = task.linkedActivityId?.let { actMap[it]?.name } ?: "Общее"
                    val noteSuffix = task.note?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                    append("\n• $subjectName: ${task.title}$noteSuffix")
                    task.subtasks.filter { !it.isDone }.forEach { sub ->
                        append("\n   ◦ ${sub.title}")
                    }
                }
            }
        }
    }
}
