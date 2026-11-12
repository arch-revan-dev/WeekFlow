package com.weekflow.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskAttachment
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val subtaskJsonParser = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconEmoji: String,
    val colorHex: String,
    val autoStartDateIso: String?,
    val autoEndDateIso: String?,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'STUDY'")
    val appProfile: String = AppProfile.STUDY.name
) {
    fun toDomain(): SchedulePreset {
        val parsedProfile = try {
            AppProfile.valueOf(appProfile)
        } catch (_: Exception) {
            if (iconEmoji == "🏋️" || name.contains("Упр", ignoreCase = true) || name.contains("Спорт", ignoreCase = true)) {
                AppProfile.HEALTH
            } else {
                AppProfile.STUDY
            }
        }
        return SchedulePreset(
            id = id,
            name = name,
            iconEmoji = iconEmoji,
            colorHex = colorHex,
            autoStartDateIso = autoStartDateIso,
            autoEndDateIso = autoEndDateIso,
            createdAt = createdAt,
            appProfile = parsedProfile
        )
    }

    companion object {
        fun fromDomain(domain: SchedulePreset): PresetEntity = PresetEntity(
            id = domain.id,
            name = domain.name,
            iconEmoji = domain.iconEmoji,
            colorHex = domain.colorHex,
            autoStartDateIso = domain.autoStartDateIso,
            autoEndDateIso = domain.autoEndDateIso,
            createdAt = domain.createdAt,
            appProfile = domain.appProfile.name
        )
    }
}

@Entity(
    tableName = "activities",
    indices = [
        Index("presetId")
    ]
)
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val shortName: String,
    val category: String, // ActivityCategory name
    val colorHex: String,
    val defaultLocation: String?,
    val defaultInstructor: String?,
    val defaultType: String?,
    @ColumnInfo(defaultValue = "'default'")
    val presetId: String = SchedulePreset.DEFAULT_ID
) {
    fun toDomain(): Activity = Activity(
        id = id,
        name = name,
        shortName = shortName,
        category = try { ActivityCategory.valueOf(category) } catch (_: Exception) { ActivityCategory.STUDY },
        colorHex = colorHex,
        defaultLocation = defaultLocation,
        defaultInstructor = defaultInstructor,
        defaultType = defaultType,
        presetId = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
    )

    companion object {
        fun fromDomain(domain: Activity): ActivityEntity = ActivityEntity(
            id = domain.id,
            name = domain.name,
            shortName = domain.shortName,
            category = domain.category.name,
            colorHex = domain.colorHex,
            defaultLocation = domain.defaultLocation,
            defaultInstructor = domain.defaultInstructor,
            defaultType = domain.defaultType,
            presetId = domain.presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        )
    }
}

@Entity(
    tableName = "slots",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dayOfWeek"),
        Index("activityTypeId"),
        Index("presetId")
    ]
)
data class SlotEntity(
    @PrimaryKey
    val id: String,
    val activityTypeId: String,
    val dayOfWeek: Int,
    val weekCycle: String, // WeekCycle name
    val startTime: String,
    val endTime: String,
    val customLocation: String?,
    val customInstructor: String?,
    val customType: String?,
    val customNote: String?,
    val periodOrder: Int,
    @ColumnInfo(defaultValue = "'default'")
    val presetId: String = SchedulePreset.DEFAULT_ID,
    @ColumnInfo(defaultValue = "'[]'")
    val sessionItemsJson: String = "[]"
) {
    fun toDomain(): Slot {
        val parsedItems = try {
            if (sessionItemsJson.isBlank()) emptyList()
            else subtaskJsonParser.decodeFromString<List<SubtaskItem>>(sessionItemsJson)
        } catch (_: Exception) {
            emptyList()
        }
        return Slot(
            id = id,
            activityTypeId = activityTypeId,
            dayOfWeek = dayOfWeek,
            weekCycle = try { WeekCycle.valueOf(weekCycle) } catch (_: Exception) { WeekCycle.EVERY_WEEK },
            startTime = startTime,
            endTime = endTime,
            customLocation = customLocation,
            customInstructor = customInstructor,
            customType = customType,
            customNote = customNote,
            periodOrder = periodOrder,
            presetId = presetId.ifBlank { SchedulePreset.DEFAULT_ID },
            sessionItems = parsedItems
        )
    }

    companion object {
        fun fromDomain(domain: Slot): SlotEntity = SlotEntity(
            id = domain.id,
            activityTypeId = domain.activityTypeId,
            dayOfWeek = domain.dayOfWeek,
            weekCycle = domain.weekCycle.name,
            startTime = domain.startTime,
            endTime = domain.endTime,
            customLocation = domain.customLocation,
            customInstructor = domain.customInstructor,
            customType = domain.customType,
            customNote = domain.customNote,
            periodOrder = domain.periodOrder,
            presetId = domain.presetId.ifBlank { SchedulePreset.DEFAULT_ID },
            sessionItemsJson = try { subtaskJsonParser.encodeToString(domain.sessionItems) } catch (_: Exception) { "[]" }
        )
    }
}

@Entity(
    tableName = "events",
    indices = [
        Index("dateIso"),
        Index("presetId")
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val dateIso: String,
    val startTime: String?,
    val endTime: String?,
    val isAllDay: Boolean,
    val colorHex: String,
    val location: String,
    val description: String,
    val category: String, // ActivityCategory name
    @ColumnInfo(defaultValue = "'default'")
    val presetId: String = SchedulePreset.DEFAULT_ID
) {
    fun toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        title = title,
        dateIso = dateIso,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        colorHex = colorHex,
        location = location,
        description = description,
        category = try { ActivityCategory.valueOf(category) } catch (_: Exception) { ActivityCategory.PERSONAL },
        presetId = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
    )

    companion object {
        fun fromDomain(domain: CalendarEvent): EventEntity = EventEntity(
            id = domain.id,
            title = domain.title,
            dateIso = domain.dateIso,
            startTime = domain.startTime,
            endTime = domain.endTime,
            isAllDay = domain.isAllDay,
            colorHex = domain.colorHex,
            location = domain.location,
            description = domain.description,
            category = domain.category.name,
            presetId = domain.presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        )
    }
}

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedActivityId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedSlotId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dueDateIso"),
        Index("linkedActivityId"),
        Index("linkedSlotId"),
        Index("isCompleted"),
        Index("presetId")
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val dueDateIso: String?,
    val linkedSlotId: String?,
    val linkedActivityId: String?,
    val isCompleted: Boolean,
    val note: String?,
    val priority: String = "NONE", // TaskPriority name
    @ColumnInfo(defaultValue = "'[]'")
    val subtasksJson: String = "[]",
    @ColumnInfo(defaultValue = "'default'")
    val presetId: String = SchedulePreset.DEFAULT_ID,
    val grade: String? = null,
    @ColumnInfo(defaultValue = "'[]'")
    val attachmentsJson: String = "[]"
) {
    fun toDomain(): Task {
        val parsedSubtasks = try {
            if (subtasksJson.isBlank() || subtasksJson == "[]") emptyList()
            else subtaskJsonParser.decodeFromString<List<SubtaskItem>>(subtasksJson)
        } catch (_: Exception) {
            emptyList()
        }
        val parsedAttachments = try {
            if (attachmentsJson.isBlank() || attachmentsJson == "[]") emptyList()
            else subtaskJsonParser.decodeFromString<List<TaskAttachment>>(attachmentsJson)
        } catch (_: Exception) {
            emptyList()
        }
        return Task(
            id = id,
            title = title,
            dueDateIso = dueDateIso,
            linkedSlotId = linkedSlotId,
            linkedActivityId = linkedActivityId,
            isCompleted = isCompleted,
            note = note,
            priority = try { TaskPriority.valueOf(priority) } catch (_: Exception) { TaskPriority.NONE },
            subtasks = parsedSubtasks,
            presetId = presetId.ifBlank { SchedulePreset.DEFAULT_ID },
            grade = grade,
            attachments = parsedAttachments
        )
    }

    companion object {
        fun fromDomain(domain: Task): TaskEntity {
            val serializedSubtasks = try {
                if (domain.subtasks.isEmpty()) "[]"
                else subtaskJsonParser.encodeToString(domain.subtasks)
            } catch (_: Exception) {
                "[]"
            }
            val serializedAttachments = try {
                if (domain.attachments.isEmpty()) "[]"
                else subtaskJsonParser.encodeToString(domain.attachments)
            } catch (_: Exception) {
                "[]"
            }
            return TaskEntity(
                id = domain.id,
                title = domain.title,
                dueDateIso = domain.dueDateIso,
                linkedSlotId = domain.linkedSlotId,
                linkedActivityId = domain.linkedActivityId,
                isCompleted = domain.isCompleted,
                note = domain.note,
                priority = domain.priority.name,
                subtasksJson = serializedSubtasks,
                presetId = domain.presetId.ifBlank { SchedulePreset.DEFAULT_ID },
                grade = domain.grade,
                attachmentsJson = serializedAttachments
            )
        }
    }
}

@Entity(tableName = "vacations")
data class VacationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val startDateIso: String,
    val endDateIso: String
) {
    fun toDomain(): Vacation = Vacation(
        id = id,
        title = title,
        startDateIso = startDateIso,
        endDateIso = endDateIso
    )

    companion object {
        fun fromDomain(domain: Vacation): VacationEntity = VacationEntity(
            id = domain.id,
            title = domain.title,
            startDateIso = domain.startDateIso,
            endDateIso = domain.endDateIso
        )
    }
}
