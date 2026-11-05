package com.weekflow.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.weekflow.app.data.local.db.WeekFlowDatabase
import com.weekflow.app.data.local.db.entity.ActivityEntity
import com.weekflow.app.data.local.db.entity.EventEntity
import com.weekflow.app.data.local.db.entity.PresetEntity
import com.weekflow.app.data.local.db.entity.SlotEntity
import com.weekflow.app.data.local.db.entity.TaskEntity
import com.weekflow.app.data.local.db.entity.VacationEntity
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.ActivityDto
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.EventDto
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SlotDto
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskDto
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.widget.WeekFlowWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ScheduleRepositoryImpl(
    private val database: WeekFlowDatabase,
    private val context: Context? = null
) : ScheduleRepository {

    private val presetDao = database.presetDao()
    private val activityDao = database.activityDao()
    private val slotDao = database.slotDao()
    private val eventDao = database.eventDao()
    private val taskDao = database.taskDao()
    private val vacationDao = database.vacationDao()

    private suspend fun notifyWidget() {
        context?.let { ctx ->
            WeekFlowWidgetUpdater.forceUpdateAll(ctx)
        }
    }

    override fun getAllPresets(): Flow<List<SchedulePreset>> {
        return presetDao.getAll().map { list ->
            val domainList = list.map { it.toDomain() }
            if (domainList.none { it.id == SchedulePreset.DEFAULT_ID }) {
                listOf(SchedulePreset.DEFAULT_PRESET) + domainList
            } else {
                domainList
            }
        }
    }

    override suspend fun getPresetById(id: String): SchedulePreset? {
        return presetDao.getById(id)?.toDomain()
            ?: if (id == SchedulePreset.DEFAULT_ID) SchedulePreset.DEFAULT_PRESET else null
    }

    override suspend fun upsertPreset(preset: SchedulePreset) {
        presetDao.upsert(PresetEntity.fromDomain(preset))
        notifyWidget()
    }

    override suspend fun deletePreset(id: String) {
        if (id == SchedulePreset.DEFAULT_ID) return
        database.withTransaction {
            taskDao.deleteByPresetId(id)
            eventDao.deleteByPresetId(id)
            slotDao.deleteByPresetId(id)
            activityDao.deleteByPresetId(id)
            presetDao.deleteById(id)
        }
        notifyWidget()
    }

    override suspend fun duplicatePreset(sourcePresetId: String, newPreset: SchedulePreset) {
        database.withTransaction {
            presetDao.upsert(PresetEntity.fromDomain(newPreset))

            val sourceActivities = activityDao.getByPresetList(sourcePresetId)
            val sourceSlots = slotDao.getByPresetList(sourcePresetId)

            val activityIdMap = mutableMapOf<String, String>()
            val clonedActivities = sourceActivities.map { act ->
                val newActId = UUID.randomUUID().toString()
                activityIdMap[act.id] = newActId
                act.copy(
                    id = newActId,
                    presetId = newPreset.id
                )
            }
            activityDao.upsertAll(clonedActivities)

            val clonedSlots = sourceSlots.mapNotNull { slot ->
                val mappedActId = activityIdMap[slot.activityTypeId] ?: return@mapNotNull null
                slot.copy(
                    id = UUID.randomUUID().toString(),
                    activityTypeId = mappedActId,
                    presetId = newPreset.id
                )
            }
            slotDao.upsertAll(clonedSlots)
        }
        notifyWidget()
    }

    override fun getAllActivities(): Flow<List<Activity>> {
        return activityDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActivityById(id: String): Activity? {
        return activityDao.getById(id)?.toDomain()
    }

    override suspend fun upsertActivity(activity: Activity) {
        activityDao.upsert(ActivityEntity.fromDomain(activity))
        notifyWidget()
    }

    override suspend fun deleteActivity(id: String) {
        activityDao.deleteById(id)
        notifyWidget()
    }

    override fun getAllSlots(): Flow<List<Slot>> {
        return slotDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getSlotsByDay(dayOfWeek: Int): Flow<List<Slot>> {
        return slotDao.getByDay(dayOfWeek).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSlotById(id: String): Slot? {
        return slotDao.getById(id)?.toDomain()
    }

    override suspend fun upsertSlot(slot: Slot) {
        slotDao.upsert(SlotEntity.fromDomain(slot))
        notifyWidget()
    }

    override suspend fun deleteSlot(id: String) {
        slotDao.deleteById(id)
        notifyWidget()
    }

    override fun getAllEvents(): Flow<List<CalendarEvent>> {
        return eventDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getEventsByDate(dateIso: String): Flow<List<CalendarEvent>> {
        return eventDao.getByDate(dateIso).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEventById(id: String): CalendarEvent? {
        return eventDao.getById(id)?.toDomain()
    }

    override suspend fun upsertEvent(event: CalendarEvent) {
        eventDao.upsert(EventEntity.fromDomain(event))
        notifyWidget()
    }

    override suspend fun deleteEvent(id: String) {
        eventDao.deleteById(id)
        notifyWidget()
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByDueDate(dateIso: String): Flow<List<Task>> {
        return taskDao.getByDueDate(dateIso).map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByActivity(activityId: String): Flow<List<Task>> {
        return taskDao.getByActivity(activityId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getById(id)?.toDomain()
    }

    override suspend fun upsertTask(task: Task) {
        taskDao.upsert(TaskEntity.fromDomain(task))
        notifyWidget()
    }

    override suspend fun toggleTaskCompleted(id: String, isCompleted: Boolean) {
        taskDao.updateCompleted(id, isCompleted)
        notifyWidget()
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteById(id)
        notifyWidget()
    }

    override suspend fun deleteCompletedTasks(presetId: String?) {
        if (presetId.isNullOrBlank()) {
            taskDao.deleteCompleted()
        } else {
            taskDao.deleteCompletedByPreset(presetId)
        }
        notifyWidget()
    }

    override fun getAllVacations(): Flow<List<Vacation>> {
        return vacationDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun upsertVacation(vacation: Vacation) {
        vacationDao.upsert(VacationEntity.fromDomain(vacation))
        notifyWidget()
    }

    override suspend fun deleteVacation(id: String) {
        vacationDao.deleteById(id)
        notifyWidget()
    }

    private fun scopeId(rawId: String, presetId: String): String {
        val targetPreset = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        if (targetPreset == SchedulePreset.DEFAULT_ID) return rawId
        val prefix = "${targetPreset}_"
        return if (rawId.startsWith(prefix)) rawId else "$prefix$rawId"
    }

    override suspend fun replaceAllSchedule(payload: CleanSchedulePayload, presetId: String) {
        val targetPreset = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        database.withTransaction {
            // Очищаем данные только выбранного пресета
            slotDao.deleteByPresetId(targetPreset)
            activityDao.deleteByPresetId(targetPreset)
            if (payload.events.isNotEmpty()) {
                eventDao.deleteByPresetId(targetPreset)
            }
            if (payload.tasks.isNotEmpty()) {
                taskDao.deleteByPresetId(targetPreset)
            }

            // Вставляем новые сущности с привязкой к пресету
            val actEntities = payload.activities.map { act ->
                ActivityEntity(
                    id = scopeId(act.id, targetPreset),
                    name = act.name,
                    shortName = act.shortName,
                    category = act.category.name,
                    colorHex = act.colorHex,
                    defaultLocation = act.defaultLocation,
                    defaultInstructor = act.defaultInstructor,
                    defaultType = act.defaultType,
                    presetId = targetPreset
                )
            }
            activityDao.upsertAll(actEntities)

            val slotEntities = payload.slots.map { slot ->
                SlotEntity(
                    id = scopeId(slot.id, targetPreset),
                    activityTypeId = scopeId(slot.activityTypeId, targetPreset),
                    dayOfWeek = slot.dayOfWeek,
                    weekCycle = slot.weekCycle.name,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    customLocation = slot.customLocation,
                    customInstructor = slot.customInstructor,
                    customType = slot.customType,
                    customNote = slot.customNote,
                    periodOrder = slot.periodOrder,
                    presetId = targetPreset
                )
            }
            slotDao.upsertAll(slotEntities)

            val eventEntities = payload.events.map { evt ->
                EventEntity(
                    id = scopeId(evt.id, targetPreset),
                    title = evt.title,
                    dateIso = evt.dateIso,
                    startTime = evt.startTime,
                    endTime = evt.endTime,
                    isAllDay = evt.isAllDay,
                    colorHex = evt.colorHex,
                    location = evt.location,
                    description = evt.description,
                    category = evt.category.name,
                    presetId = targetPreset
                )
            }
            eventDao.upsertAll(eventEntities)

            val taskEntities = payload.tasks.map { task ->
                TaskEntity.fromDomain(
                    Task(
                        id = scopeId(task.id, targetPreset),
                        title = task.title,
                        dueDateIso = task.dueDateIso,
                        linkedSlotId = task.linkedSlotId?.let { scopeId(it, targetPreset) },
                        linkedActivityId = task.linkedActivityId?.let { scopeId(it, targetPreset) },
                        isCompleted = task.isCompleted,
                        note = task.note,
                        priority = task.priority,
                        subtasks = task.subtasks,
                        presetId = targetPreset
                    )
                )
            }
            taskDao.upsertAll(taskEntities)
        }
        notifyWidget()
    }

    override suspend fun mergeSchedule(payload: CleanSchedulePayload, presetId: String) {
        val targetPreset = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        database.withTransaction {
            val actEntities = payload.activities.map { act ->
                ActivityEntity(
                    id = scopeId(act.id, targetPreset),
                    name = act.name,
                    shortName = act.shortName,
                    category = act.category.name,
                    colorHex = act.colorHex,
                    defaultLocation = act.defaultLocation,
                    defaultInstructor = act.defaultInstructor,
                    defaultType = act.defaultType,
                    presetId = targetPreset
                )
            }
            activityDao.upsertAll(actEntities)

            val slotEntities = payload.slots.map { slot ->
                SlotEntity(
                    id = scopeId(slot.id, targetPreset),
                    activityTypeId = scopeId(slot.activityTypeId, targetPreset),
                    dayOfWeek = slot.dayOfWeek,
                    weekCycle = slot.weekCycle.name,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    customLocation = slot.customLocation,
                    customInstructor = slot.customInstructor,
                    customType = slot.customType,
                    customNote = slot.customNote,
                    periodOrder = slot.periodOrder,
                    presetId = targetPreset
                )
            }
            slotDao.upsertAll(slotEntities)

            val eventEntities = payload.events.map { evt ->
                EventEntity(
                    id = scopeId(evt.id, targetPreset),
                    title = evt.title,
                    dateIso = evt.dateIso,
                    startTime = evt.startTime,
                    endTime = evt.endTime,
                    isAllDay = evt.isAllDay,
                    colorHex = evt.colorHex,
                    location = evt.location,
                    description = evt.description,
                    category = evt.category.name,
                    presetId = targetPreset
                )
            }
            eventDao.upsertAll(eventEntities)

            val taskEntities = payload.tasks.map { task ->
                TaskEntity.fromDomain(
                    Task(
                        id = scopeId(task.id, targetPreset),
                        title = task.title,
                        dueDateIso = task.dueDateIso,
                        linkedSlotId = task.linkedSlotId?.let { scopeId(it, targetPreset) },
                        linkedActivityId = task.linkedActivityId?.let { scopeId(it, targetPreset) },
                        isCompleted = task.isCompleted,
                        note = task.note,
                        priority = task.priority,
                        subtasks = task.subtasks,
                        presetId = targetPreset
                    )
                )
            }
            taskDao.upsertAll(taskEntities)
        }
        notifyWidget()
    }

    override suspend fun exportCleanSchedule(presetId: String): CleanSchedulePayload {
        val targetPreset = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        val activities = activityDao.getAllList()
            .map { it.toDomain() }
            .filter { it.presetId == targetPreset }
            .map { domain ->
                ActivityDto(
                    id = domain.id,
                    name = domain.name,
                    shortName = domain.shortName,
                    category = domain.category,
                    colorHex = domain.colorHex,
                    defaultLocation = domain.defaultLocation,
                    defaultInstructor = domain.defaultInstructor,
                    defaultType = domain.defaultType
                )
            }

        val slots = slotDao.getAllList()
            .map { it.toDomain() }
            .filter { it.presetId == targetPreset }
            .map { domain ->
                SlotDto(
                    id = domain.id,
                    activityTypeId = domain.activityTypeId,
                    dayOfWeek = domain.dayOfWeek,
                    weekCycle = domain.weekCycle,
                    startTime = domain.startTime,
                    endTime = domain.endTime,
                    customLocation = domain.customLocation,
                    customInstructor = domain.customInstructor,
                    customType = domain.customType,
                    customNote = domain.customNote,
                    periodOrder = domain.periodOrder
                )
            }

        val events = eventDao.getAllList()
            .map { it.toDomain() }
            .filter { it.presetId == targetPreset }
            .map { domain ->
                EventDto(
                    id = domain.id,
                    title = domain.title,
                    dateIso = domain.dateIso,
                    startTime = domain.startTime,
                    endTime = domain.endTime,
                    isAllDay = domain.isAllDay,
                    colorHex = domain.colorHex,
                    location = domain.location,
                    description = domain.description,
                    category = domain.category
                )
            }

        val tasks = taskDao.getAllList()
            .map { it.toDomain() }
            .filter { it.presetId == targetPreset }
            .map { domain ->
                TaskDto(
                    id = domain.id,
                    title = domain.title,
                    dueDateIso = domain.dueDateIso,
                    linkedSlotId = domain.linkedSlotId,
                    linkedActivityId = domain.linkedActivityId,
                    isCompleted = domain.isCompleted,
                    note = domain.note,
                    priority = domain.priority,
                    subtasks = domain.subtasks
                )
            }

        return CleanSchedulePayload(
            activities = activities,
            slots = slots,
            events = events,
            tasks = tasks
        )
    }
}
