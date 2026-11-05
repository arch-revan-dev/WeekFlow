package com.weekflow.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.weekflow.app.data.local.db.entity.ActivityEntity
import com.weekflow.app.data.local.db.entity.EventEntity
import com.weekflow.app.data.local.db.entity.PresetEntity
import com.weekflow.app.data.local.db.entity.SlotEntity
import com.weekflow.app.data.local.db.entity.TaskEntity
import com.weekflow.app.data.local.db.entity.VacationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY createdAt ASC")
    suspend fun getAllList(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: String): PresetEntity?

    @Upsert
    suspend fun upsert(preset: PresetEntity)

    @Upsert
    suspend fun upsertAll(presets: List<PresetEntity>)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: String): Int
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY name ASC")
    fun getAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities")
    suspend fun getAllList(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE presetId = :presetId ORDER BY name ASC")
    suspend fun getByPresetList(presetId: String): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: String): ActivityEntity?

    @Upsert
    suspend fun upsert(activity: ActivityEntity)

    @Upsert
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM activities WHERE presetId = :presetId")
    suspend fun deleteByPresetId(presetId: String): Int

    @Query("DELETE FROM activities")
    suspend fun clearAll(): Int
}

@Dao
interface SlotDao {
    @Query("SELECT * FROM slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAll(): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots")
    suspend fun getAllList(): List<SlotEntity>

    @Query("SELECT * FROM slots WHERE presetId = :presetId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getByPresetList(presetId: String): List<SlotEntity>

    @Query("SELECT * FROM slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getByDay(dayOfWeek: Int): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getByDayList(dayOfWeek: Int): List<SlotEntity>

    @Query("SELECT * FROM slots WHERE id = :id")
    suspend fun getById(id: String): SlotEntity?

    @Query("SELECT * FROM slots WHERE activityTypeId = :activityId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getByActivity(activityId: String): List<SlotEntity>

    @Upsert
    suspend fun upsert(slot: SlotEntity)

    @Upsert
    suspend fun upsertAll(slots: List<SlotEntity>)

    @Query("DELETE FROM slots WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM slots WHERE presetId = :presetId")
    suspend fun deleteByPresetId(presetId: String): Int

    @Query("DELETE FROM slots")
    suspend fun clearAll(): Int
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateIso ASC, startTime ASC")
    fun getAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllList(): List<EventEntity>

    @Query("SELECT * FROM events WHERE dateIso = :dateIso ORDER BY isAllDay DESC, startTime ASC")
    fun getByDate(dateIso: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE dateIso = :dateIso ORDER BY isAllDay DESC, startTime ASC")
    suspend fun getByDateList(dateIso: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM events WHERE presetId = :presetId")
    suspend fun deleteByPresetId(presetId: String): Int

    @Query("DELETE FROM events")
    suspend fun clearAll(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateIso ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllList(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dueDateIso = :dateIso ORDER BY isCompleted ASC")
    fun getByDueDate(dateIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDateIso = :dateIso ORDER BY isCompleted ASC")
    suspend fun getByDueDateList(dateIso: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE linkedActivityId = :activityId ORDER BY isCompleted ASC, dueDateIso ASC")
    fun getByActivity(activityId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE linkedSlotId = :slotId ORDER BY isCompleted ASC")
    fun getBySlot(slotId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompleted(): Int

    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND presetId = :presetId")
    suspend fun deleteCompletedByPreset(presetId: String): Int

    @Query("DELETE FROM tasks WHERE presetId = :presetId")
    suspend fun deleteByPresetId(presetId: String): Int

    @Query("DELETE FROM tasks")
    suspend fun clearAll(): Int
}

@Dao
interface VacationDao {
    @Query("SELECT * FROM vacations ORDER BY startDateIso ASC")
    fun getAll(): Flow<List<VacationEntity>>

    @Query("SELECT * FROM vacations")
    suspend fun getAllList(): List<VacationEntity>

    @Query("SELECT * FROM vacations WHERE id = :id")
    suspend fun getById(id: String): VacationEntity?

    @Upsert
    suspend fun upsert(vacation: VacationEntity)

    @Upsert
    suspend fun upsertAll(vacations: List<VacationEntity>)

    @Query("DELETE FROM vacations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM vacations")
    suspend fun clearAll(): Int
}
