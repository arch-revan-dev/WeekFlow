package com.weekflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CleanSchedulePayload(
    val activities: List<ActivityDto> = emptyList(),
    val slots: List<SlotDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList()
)

@Serializable
data class ActivityDto(
    val id: String,
    val name: String,
    val shortName: String = "",
    val category: ActivityCategory = ActivityCategory.STUDY,
    val colorHex: String = "#007AFF",
    val defaultLocation: String? = null,
    val defaultInstructor: String? = null,
    val defaultType: String? = null
)

@Serializable
data class SlotDto(
    val id: String,
    val activityTypeId: String,
    val dayOfWeek: Int, // 1 = Понедельник .. 7 = Воскресенье
    val weekCycle: WeekCycle = WeekCycle.EVERY_WEEK,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val customLocation: String? = null,
    val customInstructor: String? = null,
    val customType: String? = null,
    val customNote: String? = null,
    val periodOrder: Int = 1,
    val sessionItems: List<SubtaskItem> = emptyList(),
    val gradesByDate: Map<String, String> = emptyMap(),
    val grade: String? = null
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val dateIso: String, // "YYYY-MM-DD"
    val startTime: String? = null, // "HH:mm" or null
    val endTime: String? = null,   // "HH:mm" or null
    val isAllDay: Boolean = false,
    val colorHex: String = "#007AFF",
    val location: String = "",
    val description: String = "",
    val category: ActivityCategory = ActivityCategory.PERSONAL
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val dueDateIso: String? = null, // "YYYY-MM-DD" or null
    val linkedSlotId: String? = null,
    val linkedActivityId: String? = null,
    val isCompleted: Boolean = false,
    val note: String? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val subtasks: List<SubtaskItem> = emptyList(),
    val attachments: List<TaskAttachment> = emptyList()
)
