package com.weekflow.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.CalendarEventOccurrence
import com.weekflow.app.domain.model.DayScheduleUiModel
import com.weekflow.app.domain.model.EventRepeatType
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.ScheduleViewMode
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.usecase.CalendarDayUiModel
import com.weekflow.app.domain.usecase.GetDayScheduleUseCase
import com.weekflow.app.domain.usecase.GetMonthCalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class CalendarUiState(
    val currentYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<LocalDate> = emptyList(),
    val weekEventOccurrences: List<CalendarEventOccurrence> = emptyList(),
    val weekDayTaskCounts: Map<String, Int> = emptyMap(),
    val isToday: Boolean = selectedDate == LocalDate.now(),
    val monthDays: List<CalendarDayUiModel> = emptyList(),
    val selectedDaySchedule: DayScheduleUiModel? = null,
    val preferences: UserPreferences = UserPreferences(),
    val allPresets: List<SchedulePreset> = listOf(SchedulePreset.DEFAULT_PRESET),
    val activePreset: SchedulePreset = SchedulePreset.DEFAULT_PRESET,
    val allActivities: List<Activity> = emptyList(),
    val rawAllActivities: List<Activity> = emptyList(),
    val rawAllSlots: List<Slot> = emptyList(),
    val rawAllTasks: List<Task> = emptyList()
)

class CalendarViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getMonthCalendarUseCase: GetMonthCalendarUseCase,
    private val getDayScheduleUseCase: GetDayScheduleUseCase,
    private val notificationScheduler: NotificationScheduler? = null
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentYearMonth,
        _selectedDate,
        preferencesRepository.userPreferences,
        scheduleRepository.getAllPresets(),
        scheduleRepository.getAllActivities(),
        scheduleRepository.getAllSlots(),
        scheduleRepository.getAllEvents(),
        scheduleRepository.getAllTasks(),
        scheduleRepository.getAllVacations()
    ) { params ->
        val ym = params[0] as YearMonth
        val selectedDate = params[1] as LocalDate
        val preferences = params[2] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val presets = params[3] as List<SchedulePreset>
        @Suppress("UNCHECKED_CAST")
        val rawActivities = params[4] as List<Activity>
        @Suppress("UNCHECKED_CAST")
        val rawSlots = params[5] as List<Slot>
        @Suppress("UNCHECKED_CAST")
        val rawEvents = params[6] as List<CalendarEvent>
        @Suppress("UNCHECKED_CAST")
        val rawTasks = params[7] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val vacations = params[8] as List<Vacation>

        val (effectivePreset, _) = SchedulePreset.resolveForDate(
            date = selectedDate,
            presets = presets,
            activePresetId = preferences.activePresetId
        )

        val activities = rawActivities.filter { it.presetId == effectivePreset.id }
        val slots = rawSlots.filter { it.presetId == effectivePreset.id }
        val events = rawEvents.filter { it.presetId == effectivePreset.id }
        val tasks = if (preferences.showAllPresetsTasks) {
            rawTasks
        } else {
            rawTasks.filter { it.presetId == effectivePreset.id }
        }

        val monthDays = getMonthCalendarUseCase(
            yearMonth = ym,
            preferences = preferences,
            activities = activities,
            slots = slots,
            events = events,
            tasks = tasks,
            vacations = vacations
        )

        val selectedDaySchedule = getDayScheduleUseCase(
            targetDate = selectedDate,
            currentTime = null,
            preferences = preferences,
            activities = activities,
            slots = slots,
            events = events,
            tasks = tasks,
            vacations = vacations
        )

        val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDays = (0..6).map { monday.plusDays(it.toLong()) }
        val weekEventOccurrences = weekDays.flatMap { day ->
            events.filter { it.occursOnDate(day) }
                .map { event -> CalendarEventOccurrence(event = event, occurrenceDate = day) }
        }.sortedWith(
            compareBy<CalendarEventOccurrence>(
                { it.occurrenceDate },
                { !it.event.isAllDay },
                { it.event.parseStartTime() ?: LocalTime.MIN },
                { it.event.startTime ?: "" }
            )
        )
        val weekDayTaskCounts = weekDays.mapNotNull { day ->
            val dayIso = day.toString()
            val pendingCount = tasks.count { it.dueDateIso == dayIso && !it.isCompleted }
            if (pendingCount > 0) dayIso to pendingCount else null
        }.toMap()

        CalendarUiState(
            currentYearMonth = ym,
            selectedDate = selectedDate,
            weekDays = weekDays,
            weekEventOccurrences = weekEventOccurrences,
            weekDayTaskCounts = weekDayTaskCounts,
            isToday = selectedDate == LocalDate.now(),
            monthDays = monthDays,
            selectedDaySchedule = selectedDaySchedule,
            preferences = preferences,
            allPresets = presets,
            activePreset = effectivePreset,
            allActivities = activities,
            rawAllActivities = rawActivities,
            rawAllSlots = rawSlots,
            rawAllTasks = rawTasks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CalendarUiState()
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        if (date.year != _currentYearMonth.value.year || date.monthValue != _currentYearMonth.value.monthValue) {
            _currentYearMonth.value = YearMonth.of(date.year, date.monthValue)
        }
    }

    fun onPreviousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun setCalendarViewMode(mode: ScheduleViewMode) {
        if (mode == ScheduleViewMode.WEEK_TABLE_GRID || mode == ScheduleViewMode.TIMELINE_GRID) {
            val selectedYm = YearMonth.from(_selectedDate.value)
            if (_currentYearMonth.value != selectedYm) {
                _currentYearMonth.value = selectedYm
            }
        }
        viewModelScope.launch {
            preferencesRepository.updateCalendarViewMode(mode)
        }
    }

    fun onPreviousWeek() {
        val newDate = _selectedDate.value.minusWeeks(1)
        _selectedDate.value = newDate
        if (newDate.year != _currentYearMonth.value.year || newDate.monthValue != _currentYearMonth.value.monthValue) {
            _currentYearMonth.value = YearMonth.of(newDate.year, newDate.monthValue)
        }
    }

    fun onNextWeek() {
        val newDate = _selectedDate.value.plusWeeks(1)
        _selectedDate.value = newDate
        if (newDate.year != _currentYearMonth.value.year || newDate.monthValue != _currentYearMonth.value.monthValue) {
            _currentYearMonth.value = YearMonth.of(newDate.year, newDate.monthValue)
        }
    }

    fun moveEventOccurrence(
        occurrence: CalendarEventOccurrence,
        targetDateIso: String,
        newStartTime: String? = occurrence.event.startTime,
        newEndTime: String? = occurrence.event.endTime,
        newIsAllDay: Boolean = occurrence.event.isAllDay
    ) {
        val event = occurrence.event
        val resolvedStart = if (newIsAllDay) null else newStartTime
        val resolvedEnd = if (newIsAllDay) null else newEndTime
        val targetDate = try {
            LocalDate.parse(targetDateIso)
        } catch (_: Exception) {
            null
        } ?: return

        val updated = when (event.repeatType) {
            EventRepeatType.NONE -> event.copy(
                dateIso = targetDateIso,
                startTime = resolvedStart,
                endTime = resolvedEnd,
                isAllDay = newIsAllDay
            )
            EventRepeatType.WEEKLY -> {
                val updatedDays = if (occurrence.occurrenceDateIso != targetDateIso) {
                    val oldWeekday = occurrence.occurrenceDate.dayOfWeek.value
                    val newWeekday = targetDate.dayOfWeek.value
                    val baseDays = event.repeatDays
                        .filter { it in 1..7 }
                        .ifEmpty { listOf(event.parseDate().dayOfWeek.value) }
                    (baseDays.filter { it != oldWeekday } + newWeekday)
                        .distinct()
                        .sorted()
                } else {
                    event.repeatDays
                }
                val updatedDateIso = if (targetDate.isBefore(event.parseDate())) {
                    targetDateIso
                } else {
                    event.dateIso
                }
                val currentEndDate = event.parseRepeatEndDate()
                val updatedRepeatEndIso = if (currentEndDate != null && targetDate.isAfter(currentEndDate)) {
                    targetDateIso
                } else {
                    event.repeatEndDateIso
                }
                event.copy(
                    dateIso = updatedDateIso,
                    repeatDays = updatedDays,
                    repeatEndDateIso = updatedRepeatEndIso,
                    startTime = resolvedStart,
                    endTime = resolvedEnd,
                    isAllDay = newIsAllDay
                )
            }
            EventRepeatType.DAILY,
            EventRepeatType.WEEKDAYS,
            EventRepeatType.MONTHLY -> event.copy(
                startTime = resolvedStart,
                endTime = resolvedEnd,
                isAllDay = newIsAllDay
            )
        }

        if (updated == event) return

        viewModelScope.launch {
            scheduleRepository.upsertEvent(updated)
            try {
                notificationScheduler?.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }

    fun goToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _currentYearMonth.value = YearMonth.now()
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

    fun postponeTaskToTomorrow(task: Task) {
        val tomorrowIso = LocalDate.now().plusDays(1).toString()
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(dueDateIso = tomorrowIso))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteTask(taskId)
        }
    }

    fun recordFocusSession(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.recordCompletedFocusSession(minutes)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteEvent(eventId)
        }
    }

    fun updateSlotSessionItems(slot: Slot, items: List<SubtaskItem>) {
        viewModelScope.launch {
            scheduleRepository.upsertSlot(slot.copy(sessionItems = items))
        }
    }

    fun updateTaskSubtasks(task: Task, items: List<SubtaskItem>) {
        viewModelScope.launch {
            scheduleRepository.upsertTask(task.copy(subtasks = items))
        }
    }

    fun addQuickFollowUpTask(title: String, activityId: String?, presetId: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            scheduleRepository.upsertTask(
                Task(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    dueDateIso = uiState.value.selectedDate.plusDays(1).toString(),
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
}
