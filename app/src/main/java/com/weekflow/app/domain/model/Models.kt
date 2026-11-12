package com.weekflow.app.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class SchedulePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconEmoji: String = "🎓",
    val colorHex: String = "#007AFF",
    val autoStartDateIso: String? = null,
    val autoEndDateIso: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val appProfile: AppProfile = AppProfile.STUDY
) {
    fun matchesDate(date: LocalDate): Boolean {
        val start = autoStartDateIso?.takeIf { it.isNotBlank() }?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: return false
        val end = autoEndDateIso?.takeIf { it.isNotBlank() }?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: return false
        return !date.isBefore(start) && !date.isAfter(end)
    }

    companion object {
        const val DEFAULT_ID = "default"
        val DEFAULT_PRESET = SchedulePreset(
            id = DEFAULT_ID,
            name = "Основной",
            iconEmoji = "🎓",
            colorHex = "#007AFF",
            createdAt = 0L
        )

        fun resolveForDate(
            date: LocalDate,
            presets: List<SchedulePreset>,
            activePresetId: String,
            manualOverridePresetId: String? = null
        ): Pair<SchedulePreset, SchedulePreset?> {
            val all = if (presets.isEmpty()) listOf(DEFAULT_PRESET) else presets
            val dateScheduledPreset = all.firstOrNull { it.matchesDate(date) }
            val chosenId = manualOverridePresetId ?: activePresetId
            val chosen = all.firstOrNull { it.id == chosenId }
                ?: all.firstOrNull { it.id == activePresetId }
                ?: dateScheduledPreset
                ?: all.first()
            return Pair(chosen, dateScheduledPreset?.takeIf { it.id != chosen.id })
        }
    }
}

@Serializable
data class SubtaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false,
    val sets: Int? = null,             // Подходы / Серии / Круги
    val reps: String? = null,          // Повторы ("12 повт", "8-10") или номера
    val durationSeconds: Int? = null,  // Таймер выполнения (сек)
    val restSeconds: Int? = null,      // Таймер отдыха между подходами (сек)
    val weightOrNote: String? = null,  // Рабочий вес / Инвентарь / Формула / Баллы / Докладчик
    val completedSets: Int = 0
) {
    val isInteractiveStep: Boolean
        get() = sets != null || durationSeconds != null || restSeconds != null || !reps.isNullOrBlank() || !weightOrNote.isNullOrBlank()

    fun formatStepSummary(): String {
        val parts = mutableListOf<String>()
        if (sets != null && sets > 0) {
            if (!reps.isNullOrBlank()) {
                parts.add("$sets × $reps")
            } else {
                parts.add(pluralize(sets, "подход", "подхода", "подходов"))
            }
        } else if (!reps.isNullOrBlank()) {
            parts.add(reps)
        }
        if (durationSeconds != null && durationSeconds > 0) {
            parts.add("работа ${formatShortDuration(durationSeconds)}")
        }
        if (restSeconds != null && restSeconds > 0) {
            parts.add("отдых ${formatShortDuration(restSeconds)}")
        }
        if (!weightOrNote.isNullOrBlank()) {
            parts.add(weightOrNote)
        }
        return parts.joinToString(" • ")
    }
}

private fun formatShortDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return when {
        mins > 0 && secs == 0 -> "$mins мин"
        mins > 0 -> "$mins мин $secs сек"
        else -> "$secs сек"
    }
}

@Serializable
data class Activity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortName: String = "",
    val category: ActivityCategory = ActivityCategory.STUDY,
    val colorHex: String = category.defaultColorHex,
    val defaultLocation: String? = null,
    val defaultInstructor: String? = null,
    val defaultType: String? = null,
    val presetId: String = SchedulePreset.DEFAULT_ID
)

@Serializable
data class Slot(
    val id: String = UUID.randomUUID().toString(),
    val activityTypeId: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val weekCycle: WeekCycle = WeekCycle.EVERY_WEEK,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val customLocation: String? = null,
    val customInstructor: String? = null,
    val customType: String? = null,
    val customNote: String? = null,
    val periodOrder: Int = 1,
    val presetId: String = SchedulePreset.DEFAULT_ID,
    val sessionItems: List<SubtaskItem> = emptyList()
) {
    fun parseStartTime(): LocalTime = try {
        LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        LocalTime.of(9, 0)
    }

    fun parseEndTime(): LocalTime = try {
        LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        LocalTime.of(10, 30)
    }
}

@Serializable
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateIso: String, // "YYYY-MM-DD"
    val startTime: String? = null, // "HH:mm"
    val endTime: String? = null,   // "HH:mm"
    val isAllDay: Boolean = false,
    val colorHex: String = "#007AFF",
    val location: String = "",
    val description: String = "",
    val category: ActivityCategory = ActivityCategory.PERSONAL,
    val presetId: String = SchedulePreset.DEFAULT_ID
) {
    fun parseDate(): LocalDate = try {
        LocalDate.parse(dateIso)
    } catch (_: Exception) {
        LocalDate.now()
    }

    fun parseStartTime(): LocalTime? = startTime?.let {
        try {
            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }

    fun parseEndTime(): LocalTime? = endTime?.let {
        try {
            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class TaskAttachment(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long = 0L
)

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDateIso: String? = null, // "YYYY-MM-DD"
    val linkedSlotId: String? = null,
    val linkedActivityId: String? = null,
    val isCompleted: Boolean = false,
    val note: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val subtasks: List<SubtaskItem> = emptyList(),
    val presetId: String = SchedulePreset.DEFAULT_ID,
    val grade: String? = null,
    val attachments: List<TaskAttachment> = emptyList()
) {
    fun parseDueDate(): LocalDate? = dueDateIso?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class Vacation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startDateIso: String,
    val endDateIso: String
) {
    fun parseStartDate(): LocalDate = try {
        LocalDate.parse(startDateIso)
    } catch (_: Exception) {
        LocalDate.now()
    }

    fun parseEndDate(): LocalDate = try {
        LocalDate.parse(endDateIso)
    } catch (_: Exception) {
        LocalDate.now()
    }

    fun contains(date: LocalDate): Boolean {
        val start = parseStartDate()
        val end = parseEndDate()
        return !date.isBefore(start) && !date.isAfter(end)
    }
}

data class SlotWithDetails(
    val slot: Slot,
    val activity: Activity,
    val tasks: List<Task> = emptyList()
) {
    val effectiveLocation: String?
        get() = slot.customLocation?.takeIf { it.isNotBlank() } ?: activity.defaultLocation

    val effectiveInstructor: String?
        get() = slot.customInstructor?.takeIf { it.isNotBlank() } ?: activity.defaultInstructor

    val effectiveType: String?
        get() = slot.customType?.takeIf { it.isNotBlank() } ?: activity.defaultType
}

sealed interface ScheduleTimelineItem {
    data class Lesson(val details: SlotWithDetails) : ScheduleTimelineItem
    data class BreakWindow(
        val durationMinutes: Long,
        val startTime: String,
        val endTime: String
    ) : ScheduleTimelineItem
    data class TimedEvent(val event: CalendarEvent) : ScheduleTimelineItem
}

data class LiveLessonStatus(
    val currentSlot: SlotWithDetails? = null,
    val nextSlot: SlotWithDetails? = null,
    val progress: Float = 0f, // 0.0 .. 1.0
    val minutesLeftInCurrent: Long = 0,
    val minutesUntilNext: Long = 0,
    val isBreakNow: Boolean = false
)

data class DayScheduleUiModel(
    val date: LocalDate,
    val holidayOrVacationTitle: String? = null,
    val isHoliday: Boolean = false,
    val isVacation: Boolean = false,
    val allDayEvents: List<CalendarEvent> = emptyList(),
    val timelineItems: List<ScheduleTimelineItem> = emptyList(),
    val pendingTasks: List<Task> = emptyList(),
    val unassignedTasks: List<Task> = emptyList(),
    val liveStatus: LiveLessonStatus? = null,
    val activeWeekCycle: WeekCycle = WeekCycle.EVERY_WEEK
)

data class NextLessonMatch(
    val dueDate: LocalDate,
    val linkedSlotId: String?,
    val slot: Slot?
)

@Serializable
data class RegularReminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isEnabled: Boolean = true,
    val isIntervalMode: Boolean = true,
    val intervalMinutes: Int = 60,
    val startTime: String = "09:00",
    val endTime: String = "21:00",
    val exactTime: String = "09:00",
    val daysOfWeek: List<Int> = emptyList()
)

@Serializable
data class UserPreferences(
    val appProfile: AppProfile = AppProfile.STUDY,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isTwoWeekCycleEnabled: Boolean = false,
    val semesterStartDateIso: String = "2026-09-01",
    val invertWeekCycle: Boolean = false,
    val showRussianHolidays: Boolean = true,
    val skipHolidaysInSmartHomework: Boolean = true,
    val isFirstLaunchCompleted: Boolean = false,
    val cardStyle: ScheduleCardStyle = ScheduleCardStyle.OUTLINED,
    val cardDensity: CardDensity = CardDensity.STANDARD,
    val cornerRadiusDp: Int = 14,
    val showBreaks: Boolean = true,
    val showProgressBar: Boolean = true,
    val scheduleViewMode: ScheduleViewMode = ScheduleViewMode.DAY_LIST,
    val notificationsEnabled: Boolean = true,
    val notifyMinutesBefore: Int = 10,
    val eveningSummaryEnabled: Boolean = true,
    val eveningSummaryTime: String = "20:00",
    val taskRemindersEnabled: Boolean = true,
    val taskRemindersTime: String = "08:00",
    val isSaturdayWeekend: Boolean = false,
    val activePresetId: String = SchedulePreset.DEFAULT_ID,
    val showAllPresetsTasks: Boolean = false,
    val focusSessionsCompleted: Int = 0,
    val focusMinutesTotal: Int = 0,
    val regularReminders: List<RegularReminder> = emptyList()
) {
    fun parseSemesterStartDate(): LocalDate = try {
        LocalDate.parse(semesterStartDateIso)
    } catch (_: Exception) {
        LocalDate.of(2026, 9, 1)
    }
}

data class BellTime(
    val period: Int,
    val startTime: String,
    val endTime: String
)
