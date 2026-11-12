package com.weekflow.app.data.serialization

import com.weekflow.app.domain.model.SchedulePreset

object AiPromptBuilder {

    fun buildSystemPrompt(activePreset: SchedulePreset = SchedulePreset.DEFAULT_PRESET): String {
        val profile = activePreset.appProfile
        val defaultPeriod = if (profile.defaultUsePeriodNumbers) 1 else 0
        val defaultLessonType = profile.defaultLessonTypes.firstOrNull() ?: "Занятие"

        val slotSessionItemsBlock = if (profile.supportsWorkouts) {
            """,
      "sessionItems": [
        {
          "id": "step_1",
          "title": "Жим гантелей / Планка",
          "isDone": false,
          "sets": 3,
          "reps": "12 повт",
          "durationSeconds": 60,
          "restSeconds": 60,
          "weightOrNote": "${profile.weightOrNotePlaceholder}"
        }
      ]"""
        } else {
            ""
        }

        val subtaskExampleBlock = if (profile.supportsWorkouts) {
            """        {
          "id": "sub_1",
          "title": "Разминка 10 мин",
          "isDone": false,
          "sets": 2,
          "reps": "12 повт",
          "durationSeconds": 60,
          "restSeconds": 60,
          "weightOrNote": "${profile.weightOrNotePlaceholder}"
        }"""
        } else {
            """        {
          "id": "sub_1",
          "title": "Подпункт задачи",
          "isDone": false
        }"""
        }

        val sessionItemsRule = if (profile.supportsWorkouts) {
            """4. `sessionItems` (внутри `slots` — ТОЛЬКО для тренировок профиля «Спорт и тренировки»): список упражнений с `title`, `sets` (подходы), `reps` (повторы), `durationSeconds` (таймер в сек), `restSeconds` (отдых в сек), `weightOrNote` (вес/техника)."""
        } else {
            """4. Для задач используй `subtasks` как простой чек-лист (`id`, `title`, `isDone`). Поле `sessionItems` в слотах не заполняй (оставляй `[]` или не указывай)."""
        }

        return """
Ты — ассистент WeekFlow (v4.4.1). Преобразуй фото, PDF или текст расписания и задач для пресета «${activePreset.name}» (профиль: «${profile.title}») СТРОГО в один валидный JSON-объект без лишнего текста.

### СХЕМА JSON (CleanSchedulePayload):
```json
{
  "activities": [
    {
      "id": "act_main_1",
      "name": "${profile.subjectPlaceholder}",
      "shortName": "${profile.shortSubjectPlaceholder}",
      "category": "${profile.defaultCategory.name}",
      "colorHex": "#007AFF",
      "defaultLocation": "${profile.locationPlaceholder}",
      "defaultInstructor": "${profile.instructorPlaceholder}",
      "defaultType": "$defaultLessonType"
    }
  ],
  "slots": [
    {
      "id": "slot_1",
      "activityTypeId": "act_main_1",
      "dayOfWeek": 1,
      "weekCycle": "EVERY_WEEK",
      "startTime": "09:00",
      "endTime": "10:00",
      "customLocation": null,
      "customInstructor": null,
      "customType": "$defaultLessonType",
      "customNote": null,
      "periodOrder": $defaultPeriod,
      "grade": null$slotSessionItemsBlock
    }
  ],
  "events": [
    {
      "id": "evt_1",
      "title": "Событие / Контрольная / Дедлайн",
      "dateIso": "2026-11-15",
      "startTime": "10:00",
      "endTime": "12:00",
      "isAllDay": false,
      "colorHex": "#FF2D55",
      "location": "${profile.locationPlaceholder}",
      "description": "Детали события",
      "category": "${profile.defaultCategory.name}"
    }
  ],
  "tasks": [
    {
      "id": "task_1",
      "title": "Название задачи / задания",
      "dueDateIso": "2026-11-12",
      "linkedSlotId": "slot_1",
      "linkedActivityId": "act_main_1",
      "isCompleted": false,
      "note": "Описание",
      "priority": "HIGH",
      "attachments": [],
      "subtasks": [
$subtaskExampleBlock
      ]
    }
  ]
}
```

### ПРАВИЛА (v4.4.1):
1. `dayOfWeek`: `1` (Пн) .. `7` (Вс).
2. `weekCycle`: `"EVERY_WEEK"`, `"NUMERATOR_A"` или `"DENOMINATOR_B"`.
3. `periodOrder`: ${if (profile.defaultUsePeriodNumbers) "номер урока/пары (`1..15`) или `0` вне сетки звонков" else "всегда `0`"}.
$sessionItemsRule
5. Поле `grade` в `slots` (оценка/балл за занятие, например `"5"`, `"Зачёт"`, `"92/100"` или `null`) и поле `attachments` в `tasks` (по умолчанию `[]`) опциональны.
6. `category`: `"STUDY"`, `"WORK"`, `"HEALTH"`, `"PERSONAL"` или `"OTHER"`.
7. `priority`: `"HIGH"`, `"MEDIUM"`, `"LOW"`, `"NONE"`.
8. Время строго `"HH:mm"`, дата строго `"YYYY-MM-DD"`. `activityTypeId` и `linkedActivityId` должны ссылаться на существующий `id` из `activities`.
9. Никаких комментариев `//` в JSON. Выдавай ТОЛЬКО валидный JSON.
""".trimIndent()
    }
}
