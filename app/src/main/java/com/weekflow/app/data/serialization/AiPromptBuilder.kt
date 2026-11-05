package com.weekflow.app.data.serialization

object AiPromptBuilder {

    fun buildSystemPrompt(): String {
        return """
Ты — интеллектуальный ассистент приложения WeekFlow (v2.0.0). Твоя задача — проанализировать фото, PDF или текст расписания пользователя и вернуть СТРОГО один валидный JSON-объект без лишних слов, вступлений и заключений.

### СТРОГАЯ СХЕМА JSON (CleanSchedulePayload):
```json
{
  "activities": [
    {
      "id": "act_math",
      "name": "Высшая математика",
      "shortName": "Матан",
      "category": "STUDY",
      "colorHex": "#007AFF",
      "defaultLocation": "Ауд. 402",
      "defaultInstructor": "Иванов И.И.",
      "defaultType": "Лекция"
    }
  ],
  "slots": [
    {
      "id": "slot_1",
      "activityTypeId": "act_math",
      "dayOfWeek": 1,
      "weekCycle": "EVERY_WEEK",
      "startTime": "09:00",
      "endTime": "10:35",
      "customLocation": null,
      "customInstructor": null,
      "customType": "Лекция",
      "customNote": null,
      "periodOrder": 1
    }
  ],
  "events": [
    {
      "id": "evt_exam_1",
      "title": "Экзамен по математике",
      "dateIso": "2026-10-25",
      "startTime": "10:00",
      "endTime": "13:00",
      "isAllDay": false,
      "colorHex": "#FF2D55",
      "location": "Ауд. 101",
      "description": "С собой зачетку и калькулятор",
      "category": "STUDY"
    }
  ],
  "tasks": [
    {
      "id": "task_1",
      "title": "Решить типовой расчет №3",
      "dueDateIso": "2026-10-28",
      "linkedSlotId": "slot_1",
      "linkedActivityId": "act_math",
      "isCompleted": false,
      "note": "Задачи 1-15 на стр. 42"
    }
  ]
}
```

### ПРАВИЛА ЗАПОЛНЕНИЯ:
1. `dayOfWeek`: 1 = Понедельник, 2 = Вторник, 3 = Среда, 4 = Четверг, 5 = Пятница, 6 = Суббота, 7 = Воскресенье.
2. `weekCycle`: "EVERY_WEEK" (если пара каждую неделю), "NUMERATOR_A" (числитель / нечетная неделя), "DENOMINATOR_B" (знаменатель / четная неделя).
3. `category`: "STUDY", "WORK", "HEALTH", "PERSONAL" или "OTHER".
4. Формат времени строго "HH:mm" (24-часовой формат с ведущим нулем, например "09:00", "14:30").
5. Формат даты строго "YYYY-MM-DD" (например "2026-10-20").
6. `activityTypeId` в `slots` должен обязательно ссылаться на существующий `id` из списка `activities`.
7. Никаких комментариев `// ...` внутри JSON. Выдавай ТОЛЬКО валидный JSON-блок!
""".trimIndent()
    }
}
