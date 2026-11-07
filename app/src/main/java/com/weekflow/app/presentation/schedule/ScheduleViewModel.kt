package com.weekflow.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.DayScheduleUiModel
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.ScheduleViewMode
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.repository.UndoSnapshotInfo
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import com.weekflow.app.domain.usecase.GetDayScheduleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<LocalDate> = emptyList(),
    val dayIndicators: Map<LocalDate, List<String>> = emptyMap(),
    val taskCountsByDate: Map<LocalDate, Int> = emptyMap(),
    val daySchedule: DayScheduleUiModel? = null,
    val preferences: UserPreferences = UserPreferences(),
    val activeWeekCycle: WeekCycle = WeekCycle.EVERY_WEEK,
    val isToday: Boolean = true,
    val allPresets: List<SchedulePreset> = listOf(SchedulePreset.DEFAULT_PRESET),
    val activePreset: SchedulePreset = SchedulePreset.DEFAULT_PRESET,
    val dateScheduledPreset: SchedulePreset? = null,
    val isPresetAutoScheduled: Boolean = false,
    val allActivities: List<Activity> = emptyList(),
    val allSlots: List<Slot> = emptyList(),
    val allEvents: List<CalendarEvent> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val allVacations: List<Vacation> = emptyList(),
    val rawAllActivities: List<Activity> = emptyList(),
    val rawAllSlots: List<Slot> = emptyList(),
    val rawAllTasks: List<Task> = emptyList()
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getDayScheduleUseCase: GetDayScheduleUseCase,
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _manualOverridePresetId = MutableStateFlow<String?>(null)

    // 15-second live ticker for real-time lesson progress and countdowns
    private val timeTicker = flow {
        while (true) {
            emit(LocalTime.now())
            delay(15_000)
        }
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        _selectedDate,
        _manualOverridePresetId,
        timeTicker,
        preferencesRepository.userPreferences,
        scheduleRepository.getAllPresets(),
        scheduleRepository.getAllActivities(),
        scheduleRepository.getAllSlots(),
        scheduleRepository.getAllEvents(),
        scheduleRepository.getAllTasks(),
        scheduleRepository.getAllVacations()
    ) { params ->
        val selectedDate = params[0] as LocalDate
        val manualOverrideId = params[1] as String?
        val currentTime = params[2] as LocalTime
        val preferences = params[3] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val presets = params[4] as List<SchedulePreset>
        @Suppress("UNCHECKED_CAST")
        val rawActivities = params[5] as List<Activity>
        @Suppress("UNCHECKED_CAST")
        val rawSlots = params[6] as List<Slot>
        @Suppress("UNCHECKED_CAST")
        val rawEvents = params[7] as List<CalendarEvent>
        @Suppress("UNCHECKED_CAST")
        val rawTasks = params[8] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val vacations = params[9] as List<Vacation>

        val (effectivePreset, dateScheduledPreset) = SchedulePreset.resolveForDate(
            date = selectedDate,
            presets = presets,
            activePresetId = preferences.activePresetId,
            manualOverridePresetId = manualOverrideId
        )

        val activities = rawActivities.filter { it.presetId == effectivePreset.id }
        val slots = rawSlots.filter { it.presetId == effectivePreset.id }
        val events = rawEvents.filter { it.presetId == effectivePreset.id }
        val tasks = if (preferences.showAllPresetsTasks) {
            rawTasks
        } else {
            rawTasks.filter { it.presetId == effectivePreset.id }
        }

        // Calculate week range (Monday..Sunday)
        val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

        // Indicators for week strip
        val activityColorMap = activities.associate { it.id to it.colorHex }
        val dayIndicators = mutableMapOf<LocalDate, List<String>>()
        val taskCountsByDate = mutableMapOf<LocalDate, Int>()
        for (day in weekDays) {
            val cycle = calculateWeekCycleUseCase(day, preferences)
            val daySlots = slots.filter { slot ->
                slot.dayOfWeek == day.dayOfWeek.value &&
                    (!preferences.isTwoWeekCycleEnabled ||
                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                        slot.weekCycle == cycle)
            }
            val colors = daySlots.mapNotNull { activityColorMap[it.activityTypeId] }.distinct()
            dayIndicators[day] = colors

            val dayIso = day.toString()
            val pendingCount = tasks.count { it.dueDateIso == dayIso && !it.isCompleted }
            if (pendingCount > 0) {
                taskCountsByDate[day] = pendingCount
            }
        }

        val daySchedule = getDayScheduleUseCase(
            targetDate = selectedDate,
            currentTime = if (selectedDate == LocalDate.now()) currentTime else null,
            preferences = preferences,
            activities = activities,
            slots = slots,
            events = events,
            tasks = tasks,
            vacations = vacations
        )

        val activeCycle = calculateWeekCycleUseCase(selectedDate, preferences)

        ScheduleUiState(
            selectedDate = selectedDate,
            weekDays = weekDays,
            dayIndicators = dayIndicators,
            taskCountsByDate = taskCountsByDate,
            daySchedule = daySchedule,
            preferences = preferences,
            activeWeekCycle = activeCycle,
            isToday = selectedDate == LocalDate.now(),
            allPresets = presets,
            activePreset = effectivePreset,
            dateScheduledPreset = dateScheduledPreset,
            isPresetAutoScheduled = effectivePreset.matchesDate(selectedDate),
            allActivities = activities,
            allSlots = slots,
            allEvents = events,
            allTasks = tasks,
            allVacations = vacations,
            rawAllActivities = rawActivities,
            rawAllSlots = rawSlots,
            rawAllTasks = rawTasks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState()
    )

    fun setScheduleViewMode(mode: ScheduleViewMode) {
        viewModelScope.launch {
            preferencesRepository.updateScheduleViewMode(mode)
        }
    }

    fun selectPreset(preset: SchedulePreset) {
        _manualOverridePresetId.value = preset.id
        viewModelScope.launch {
            preferencesRepository.updateActivePresetId(preset.id)
        }
    }

    fun applyDateScheduledPreset() {
        _manualOverridePresetId.value = null
    }

    fun savePreset(preset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.upsertPreset(preset)
        }
    }

    fun duplicatePreset(sourcePresetId: String, newPreset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.duplicatePreset(sourcePresetId, newPreset)
            _manualOverridePresetId.value = newPreset.id
            preferencesRepository.updateActivePresetId(newPreset.id)
        }
    }

    fun deletePreset(presetId: String) {
        if (presetId == SchedulePreset.DEFAULT_ID) return
        viewModelScope.launch {
            scheduleRepository.deletePreset(presetId)
            if (uiState.value.activePreset.id == presetId) {
                _manualOverridePresetId.value = SchedulePreset.DEFAULT_ID
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

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun onNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun onPreviousWeek() {
        _selectedDate.value = _selectedDate.value.minusWeeks(1)
    }

    fun onNextWeek() {
        _selectedDate.value = _selectedDate.value.plusWeeks(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
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
            scheduleRepository.upsertTask(
                task.copy(dueDateIso = tomorrowIso, linkedSlotId = null)
            )
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

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteSlot(slotId)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteEvent(eventId)
        }
    }

    fun moveSlotToCell(slot: Slot, targetDayOfWeek: Int, targetPeriodOrder: Int) {
        if (slot.dayOfWeek == targetDayOfWeek && slot.periodOrder == targetPeriodOrder) return
        val existingSlots = uiState.value.allSlots
        val profile = uiState.value.activePreset.appProfile

        val (newStart, newEnd) = if (targetPeriodOrder > 0) {
            val sameDayPeriod = existingSlots.firstOrNull {
                it.dayOfWeek == targetDayOfWeek && it.periodOrder == targetPeriodOrder && it.id != slot.id
            }
            val anyDayPeriod = existingSlots
                .filter { it.periodOrder == targetPeriodOrder && it.id != slot.id }
                .groupingBy { it.startTime to it.endTime }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            when {
                sameDayPeriod != null -> sameDayPeriod.startTime to sameDayPeriod.endTime
                anyDayPeriod != null -> anyDayPeriod
                targetPeriodOrder == slot.periodOrder -> slot.startTime to slot.endTime
                else -> {
                    profile.defaultBellTimes.getOrElse(targetPeriodOrder - 1) {
                        val durationMin = Duration.between(slot.parseStartTime(), slot.parseEndTime()).toMinutes().coerceAtLeast(30)
                        val startMin = 8 * 60 + 30 + (targetPeriodOrder - 1) * (profile.defaultDurationMinutes + profile.defaultBreakMinutes)
                        val start = LocalTime.of((startMin / 60) % 24, startMin % 60)
                        val end = start.plusMinutes(durationMin)
                        String.format("%02d:%02d", start.hour, start.minute) to String.format("%02d:%02d", end.hour, end.minute)
                    }
                }
            }
        } else {
            slot.startTime to slot.endTime
        }

        viewModelScope.launch {
            scheduleRepository.upsertSlot(
                slot.copy(
                    dayOfWeek = targetDayOfWeek,
                    periodOrder = targetPeriodOrder,
                    startTime = newStart,
                    endTime = newEnd
                )
            )
        }
    }

    fun updateSlotTime(slot: Slot, newStartTime: String, newEndTime: String) {
        if (slot.startTime == newStartTime && slot.endTime == newEndTime) return
        viewModelScope.launch {
            scheduleRepository.upsertSlot(
                slot.copy(
                    startTime = newStartTime,
                    endTime = newEndTime
                )
            )
        }
    }
}
