package com.weekflow.app.data.serialization

import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.ActivityDto
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.EventDto
import com.weekflow.app.domain.model.SlotDto
import com.weekflow.app.domain.model.TaskDto
import com.weekflow.app.domain.model.WeekCycle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

sealed interface ValidationResult {
    data class Success(
        val payload: CleanSchedulePayload,
        val activitiesCount: Int,
        val slotsCount: Int,
        val eventsCount: Int,
        val tasksCount: Int,
        val warnings: List<String> = emptyList()
    ) : ValidationResult

    data class Error(val message: String) : ValidationResult
}

object JsonImportExportManager {

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = true
        prettyPrint = true
        isLenient = true
        coerceInputValues = true
    }

    private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    private val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}$")

    /**
     * Извлекает чистый JSON-объект из сырого ответа нейросети или текстового файла:
     * отсекает Markdown-блоки ```json, пояснения до первой { и после последней }.
     */
    fun extractJsonPayload(rawInput: String): String {
        var text = rawInput.trim()
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        } else {
            if (text.startsWith("```")) {
                text = text.substringAfter("\n")
            }
            if (text.endsWith("```")) {
                text = text.substringBeforeLast("```").trim()
            }
        }
        return text.trim()
    }

    /**
     * Валидирует и санитизирует JSON строку перед записью в базу.
     */
    fun validateAndSanitize(rawInput: String): ValidationResult {
        val extractedJson = extractJsonPayload(rawInput)
        if (extractedJson.isBlank()) {
            return ValidationResult.Error("Введенный текст пуст или не содержит корректного JSON")
        }

        val decoded: CleanSchedulePayload = try {
            json.decodeFromString<CleanSchedulePayload>(extractedJson)
        } catch (e: Exception) {
            return ValidationResult.Error("Ошибка синтаксиса JSON: ${e.localizedMessage ?: e.message}")
        }

        if (decoded.activities.isEmpty() && decoded.slots.isEmpty() && decoded.events.isEmpty() && decoded.tasks.isEmpty()) {
            return ValidationResult.Error("JSON не содержит занятий, предметов, событий или задач")
        }

        val warnings = mutableListOf<String>()

        // 1. Санитизация activities
        val validActivities = mutableListOf<ActivityDto>()
        val existingActIds = mutableSetOf<String>()

        for (act in decoded.activities) {
            val actId = if (act.id.isBlank() || !existingActIds.add(act.id)) {
                "act_" + UUID.randomUUID().toString().take(8)
            } else {
                act.id
            }
            val validColor = normalizeColorHex(act.colorHex, act.category)
            validActivities.add(
                act.copy(
                    id = actId,
                    name = act.name.trim().ifBlank { "Предмет" },
                    shortName = act.shortName.trim(),
                    colorHex = validColor
                )
            )
        }

        // 2. Санитизация slots
        val validSlots = mutableListOf<SlotDto>()
        val createdFallbackActivities = mutableListOf<ActivityDto>()

        for (slot in decoded.slots) {
            // Валидация дня недели
            if (slot.dayOfWeek !in 1..7) {
                warnings.add("Слот '${slot.id}' пропущен: неверный день недели (${slot.dayOfWeek}, ожидается 1..7)")
                continue
            }

            // Валидация времени
            val normStartTime = normalizeTime(slot.startTime, fallback = "09:00")
            val normEndTime = normalizeTime(slot.endTime, fallback = "10:30")

            // Проверка существования предмета
            var targetActId = slot.activityTypeId.trim()
            if (targetActId.isBlank() || !existingActIds.contains(targetActId)) {
                val fallbackAct = ActivityDto(
                    id = "act_fallback_" + UUID.randomUUID().toString().take(6),
                    name = "Предмет без названия",
                    category = ActivityCategory.STUDY,
                    colorHex = ActivityCategory.STUDY.defaultColorHex
                )
                createdFallbackActivities.add(fallbackAct)
                existingActIds.add(fallbackAct.id)
                targetActId = fallbackAct.id
                warnings.add("Для слота '${slot.id}' создан предмет по умолчанию, так как указанный activityTypeId не найден")
            }

            val slotId = slot.id.ifBlank { "slot_" + UUID.randomUUID().toString().take(8) }

            validSlots.add(
                slot.copy(
                    id = slotId,
                    activityTypeId = targetActId,
                    startTime = normStartTime,
                    endTime = normEndTime,
                    periodOrder = slot.periodOrder.coerceAtLeast(1)
                )
            )
        }
        validActivities.addAll(createdFallbackActivities)

        // 3. Санитизация events
        val validEvents = mutableListOf<EventDto>()
        for (evt in decoded.events) {
            val cleanDate = if (dateRegex.matches(evt.dateIso.trim())) {
                evt.dateIso.trim()
            } else {
                warnings.add("Событие '${evt.title}' пропущено: неверный формат даты (${evt.dateIso})")
                continue
            }

            val cleanStart = evt.startTime?.let { normalizeTime(it, fallback = "10:00") }
            val cleanEnd = evt.endTime?.let { normalizeTime(it, fallback = "11:00") }

            validEvents.add(
                evt.copy(
                    id = evt.id.ifBlank { "evt_" + UUID.randomUUID().toString().take(8) },
                    title = evt.title.trim().ifBlank { "Событие" },
                    dateIso = cleanDate,
                    startTime = cleanStart,
                    endTime = cleanEnd,
                    colorHex = normalizeColorHex(evt.colorHex, evt.category)
                )
            )
        }

        // 4. Санитизация tasks
        val validTasks = mutableListOf<TaskDto>()
        for (task in decoded.tasks) {
            val cleanDueDate = task.dueDateIso?.let {
                if (dateRegex.matches(it.trim())) it.trim() else null
            }
            validTasks.add(
                task.copy(
                    id = task.id.ifBlank { "task_" + UUID.randomUUID().toString().take(8) },
                    title = task.title.trim().ifBlank { "Задача" },
                    dueDateIso = cleanDueDate,
                    linkedActivityId = task.linkedActivityId?.takeIf { existingActIds.contains(it) },
                    linkedSlotId = task.linkedSlotId?.takeIf { slotId -> validSlots.any { it.id == slotId } }
                )
            )
        }

        val sanitizedPayload = CleanSchedulePayload(
            activities = validActivities,
            slots = validSlots,
            events = validEvents,
            tasks = validTasks
        )

        return ValidationResult.Success(
            payload = sanitizedPayload,
            activitiesCount = validActivities.size,
            slotsCount = validSlots.size,
            eventsCount = validEvents.size,
            tasksCount = validTasks.size,
            warnings = warnings
        )
    }

    fun exportToJson(payload: CleanSchedulePayload): String {
        return json.encodeToString(payload)
    }

    private fun normalizeTime(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        val parts = trimmed.split(":")
        if (parts.size != 2) return fallback
        val h = parts[0].toIntOrNull() ?: return fallback
        val m = parts[1].toIntOrNull() ?: return fallback
        val clampedH = h.coerceIn(0, 23)
        val clampedM = m.coerceIn(0, 59)
        return String.format("%02d:%02d", clampedH, clampedM)
    }

    private fun normalizeColorHex(raw: String?, category: ActivityCategory): String {
        if (raw.isNullOrBlank()) return category.defaultColorHex
        var clean = raw.trim()
        if (!clean.startsWith("#")) {
            clean = "#$clean"
        }
        return if (hexColorRegex.matches(clean)) {
            clean
        } else {
            category.defaultColorHex
        }
    }
}
