package com.weekflow.app.domain.repository

import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.Vacation
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getAllPresets(): Flow<List<SchedulePreset>>
    suspend fun getPresetById(id: String): SchedulePreset?
    suspend fun upsertPreset(preset: SchedulePreset)
    suspend fun deletePreset(id: String)
    suspend fun duplicatePreset(sourcePresetId: String, newPreset: SchedulePreset)

    fun getAllActivities(): Flow<List<Activity>>
    suspend fun getActivityById(id: String): Activity?
    suspend fun upsertActivity(activity: Activity)
    suspend fun deleteActivity(id: String)

    fun getAllSlots(): Flow<List<Slot>>
    fun getSlotsByDay(dayOfWeek: Int): Flow<List<Slot>>
    suspend fun getSlotById(id: String): Slot?
    suspend fun upsertSlot(slot: Slot)
    suspend fun deleteSlot(id: String)

    fun getAllEvents(): Flow<List<CalendarEvent>>
    fun getEventsByDate(dateIso: String): Flow<List<CalendarEvent>>
    suspend fun getEventById(id: String): CalendarEvent?
    suspend fun upsertEvent(event: CalendarEvent)
    suspend fun deleteEvent(id: String)

    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByDueDate(dateIso: String): Flow<List<Task>>
    fun getTasksByActivity(activityId: String): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun upsertTask(task: Task)
    suspend fun toggleTaskCompleted(id: String, isCompleted: Boolean)
    suspend fun deleteTask(id: String)
    suspend fun deleteCompletedTasks(presetId: String? = null)

    fun getAllVacations(): Flow<List<Vacation>>
    suspend fun upsertVacation(vacation: Vacation)
    suspend fun deleteVacation(id: String)

    suspend fun replaceAllSchedule(payload: CleanSchedulePayload, presetId: String = SchedulePreset.DEFAULT_ID)
    suspend fun mergeSchedule(payload: CleanSchedulePayload, presetId: String = SchedulePreset.DEFAULT_ID)
    suspend fun exportCleanSchedule(presetId: String = SchedulePreset.DEFAULT_ID): CleanSchedulePayload
}
