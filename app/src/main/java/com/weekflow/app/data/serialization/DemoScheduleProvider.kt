package com.weekflow.app.data.serialization

import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.ActivityDto
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.EventDto
import com.weekflow.app.domain.model.SlotDto
import com.weekflow.app.domain.model.TaskDto
import com.weekflow.app.domain.model.WeekCycle
import java.time.LocalDate

object DemoScheduleProvider {

    fun getDemoPayload(): CleanSchedulePayload {
        val today = LocalDate.now()
        val examDate = today.plusDays(5).toString()
        val taskDueDate1 = today.plusDays(2).toString()
        val taskDueDate2 = today.plusDays(7).toString()

        val activities = listOf(
            ActivityDto(
                id = "act_math",
                name = "Высшая математика",
                shortName = "Математика",
                category = ActivityCategory.STUDY,
                colorHex = "#007AFF",
                defaultLocation = "Ауд. 402",
                defaultInstructor = "Проф. Петров А.В.",
                defaultType = "Лекция"
            ),
            ActivityDto(
                id = "act_cs",
                name = "Компьютерные науки и алгоритмы",
                shortName = "Информатика",
                category = ActivityCategory.STUDY,
                colorHex = "#34C759",
                defaultLocation = "Лаб. 312",
                defaultInstructor = "Смирнов К.С.",
                defaultType = "Лабораторная"
            ),
            ActivityDto(
                id = "act_physics",
                name = "Общая физика",
                shortName = "Физика",
                category = ActivityCategory.STUDY,
                colorHex = "#FF9500",
                defaultLocation = "Ауд. 210",
                defaultInstructor = "Васильева Е.М.",
                defaultType = "Практика"
            ),
            ActivityDto(
                id = "act_english",
                name = "Деловой английский язык",
                shortName = "Английский",
                category = ActivityCategory.STUDY,
                colorHex = "#5856D6",
                defaultLocation = "Ауд. 505",
                defaultInstructor = "Джонсон М.",
                defaultType = "Семинар"
            ),
            ActivityDto(
                id = "act_sport",
                name = "Физическая культура",
                shortName = "Физкультура",
                category = ActivityCategory.HEALTH,
                colorHex = "#FF2D55",
                defaultLocation = "Спорткомплекс",
                defaultInstructor = "Кузнецов Д.А.",
                defaultType = "Тренировка"
            )
        )

        val slots = listOf(
            // Понедельник (1)
            SlotDto(
                id = "slot_mon_1",
                activityTypeId = "act_math",
                dayOfWeek = 1,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "09:00",
                endTime = "10:35",
                customLocation = "Ауд. 402",
                customInstructor = "Проф. Петров А.В.",
                customType = "Лекция",
                periodOrder = 1
            ),
            SlotDto(
                id = "slot_mon_2",
                activityTypeId = "act_cs",
                dayOfWeek = 1,
                weekCycle = WeekCycle.NUMERATOR_A,
                startTime = "10:50",
                endTime = "12:25",
                customLocation = "Лаб. 312",
                customInstructor = "Смирнов К.С.",
                customType = "Лабораторная (A)",
                periodOrder = 2
            ),
            SlotDto(
                id = "slot_mon_2_b",
                activityTypeId = "act_physics",
                dayOfWeek = 1,
                weekCycle = WeekCycle.DENOMINATOR_B,
                startTime = "10:50",
                endTime = "12:25",
                customLocation = "Ауд. 210",
                customInstructor = "Васильева Е.М.",
                customType = "Семинар (B)",
                periodOrder = 2
            ),
            // Вторник (2)
            SlotDto(
                id = "slot_tue_1",
                activityTypeId = "act_english",
                dayOfWeek = 2,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "09:00",
                endTime = "10:35",
                customLocation = "Ауд. 505",
                customInstructor = "Джонсон М.",
                customType = "Семинар",
                periodOrder = 1
            ),
            SlotDto(
                id = "slot_tue_2",
                activityTypeId = "act_math",
                dayOfWeek = 2,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "10:50",
                endTime = "12:25",
                customLocation = "Ауд. 404",
                customInstructor = "Проф. Петров А.В.",
                customType = "Практика",
                periodOrder = 2
            ),
            // Среда (3)
            SlotDto(
                id = "slot_wed_1",
                activityTypeId = "act_physics",
                dayOfWeek = 3,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "09:00",
                endTime = "10:35",
                customLocation = "Ауд. 210",
                customInstructor = "Васильева Е.М.",
                customType = "Лекция",
                periodOrder = 1
            ),
            SlotDto(
                id = "slot_wed_2",
                activityTypeId = "act_sport",
                dayOfWeek = 3,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "13:00",
                endTime = "14:35",
                customLocation = "Спорткомплекс",
                customInstructor = "Кузнецов Д.А.",
                customType = "Тренировка",
                periodOrder = 3
            ),
            // Четверг (4)
            SlotDto(
                id = "slot_thu_1",
                activityTypeId = "act_cs",
                dayOfWeek = 4,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "09:00",
                endTime = "10:35",
                customLocation = "Лаб. 312",
                customInstructor = "Смирнов К.С.",
                customType = "Лабораторная",
                periodOrder = 1
            ),
            SlotDto(
                id = "slot_thu_2",
                activityTypeId = "act_english",
                dayOfWeek = 4,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "10:50",
                endTime = "12:25",
                customLocation = "Ауд. 505",
                customInstructor = "Джонсон М.",
                customType = "Семинар",
                periodOrder = 2
            ),
            // Пятница (5)
            SlotDto(
                id = "slot_fri_1",
                activityTypeId = "act_math",
                dayOfWeek = 5,
                weekCycle = WeekCycle.EVERY_WEEK,
                startTime = "09:00",
                endTime = "10:35",
                customLocation = "Ауд. 402",
                customInstructor = "Проф. Петров А.В.",
                customType = "Коллоквиум",
                periodOrder = 1
            ),
            SlotDto(
                id = "slot_fri_2",
                activityTypeId = "act_physics",
                dayOfWeek = 5,
                weekCycle = WeekCycle.NUMERATOR_A,
                startTime = "10:50",
                endTime = "12:25",
                customLocation = "Лаб. 102",
                customInstructor = "Васильева Е.М.",
                customType = "Лаб. работа (A)",
                periodOrder = 2
            )
        )

        val events = listOf(
            EventDto(
                id = "evt_demo_1",
                title = "Консультация перед зачетом",
                dateIso = examDate,
                startTime = "15:00",
                endTime = "16:30",
                isAllDay = false,
                colorHex = "#FF2D55",
                location = "Ауд. 402",
                description = "Разбор типовых ошибок и расчетных заданий",
                category = ActivityCategory.STUDY
            )
        )

        val tasks = listOf(
            TaskDto(
                id = "task_demo_1",
                title = "Решить задачи 1-10 из главы 4",
                dueDateIso = taskDueDate1,
                linkedSlotId = "slot_mon_1",
                linkedActivityId = "act_math",
                isCompleted = false,
                note = "Обратить внимание на интегралы по частям"
            ),
            TaskDto(
                id = "task_demo_2",
                title = "Сдать отчет по лабораторной работе №2",
                dueDateIso = taskDueDate2,
                linkedSlotId = "slot_thu_1",
                linkedActivityId = "act_cs",
                isCompleted = false,
                note = "Загрузить на GitHub и в личный кабинет"
            )
        )

        return CleanSchedulePayload(
            activities = activities,
            slots = slots,
            events = events,
            tasks = tasks
        )
    }
}
