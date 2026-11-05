package com.weekflow.app.domain.usecase

import com.weekflow.app.data.local.holidays.RussianHolidaysProvider
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.DayScheduleUiModel
import com.weekflow.app.domain.model.LiveLessonStatus
import com.weekflow.app.domain.model.ScheduleTimelineItem
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SlotWithDetails
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GetDayScheduleUseCase(
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase
) {

    operator fun invoke(
        targetDate: LocalDate,
        currentTime: LocalTime? = if (targetDate == LocalDate.now()) LocalTime.now() else null,
        preferences: UserPreferences,
        activities: List<Activity>,
        slots: List<Slot>,
        events: List<CalendarEvent>,
        tasks: List<Task>,
        vacations: List<Vacation> = emptyList()
    ): DayScheduleUiModel {
        val dateIso = targetDate.toString()
        val activityMap = activities.associateBy { it.id }

        // 1. Проверяем праздники и каникулы
        var holidayOrVacationTitle: String? = null
        var isHoliday = false
        var isVacation = false

        if (preferences.showRussianHolidays) {
            val holidayTitle = RussianHolidaysProvider.getHolidayTitle(targetDate)
            if (holidayTitle != null) {
                holidayOrVacationTitle = holidayTitle
                isHoliday = true
            }
        }

        if (holidayOrVacationTitle == null) {
            val vacation = vacations.firstOrNull { it.contains(targetDate) }
            if (vacation != null) {
                holidayOrVacationTitle = vacation.title
                isVacation = true
            }
        }

        // 2. Определяем цикл недели
        val activeCycle = calculateWeekCycleUseCase(targetDate, preferences)

        // 3. Фильтруем слоты на этот день недели и недельный цикл
        val dayOfWeekValue = targetDate.dayOfWeek.value
        val daySlots = slots
            .filter { slot ->
                slot.dayOfWeek == dayOfWeekValue &&
                    (!preferences.isTwoWeekCycleEnabled ||
                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                        slot.weekCycle == activeCycle)
            }
            .sortedBy { it.parseStartTime() }

        // 4. Задачи строго на этот конкретный день (targetDate)
        val dayTasks = tasks.filter { it.dueDateIso == dateIso }
        val daySlotIds = daySlots.map { it.id }.toSet()
        val firstSlotIdByActivity = daySlots
            .groupBy { it.activityTypeId }
            .mapValues { (_, list) -> list.first().id }

        // 5. Обогащаем слоты деталями предметов и привязанными задачами строго на дату targetDate
        val enrichedSlots = daySlots.map { slot ->
            val act = activityMap[slot.activityTypeId] ?: Activity(
                id = slot.activityTypeId,
                name = "Неизвестный предмет"
            )
            val slotTasks = dayTasks.filter { task ->
                when {
                    task.linkedSlotId == slot.id -> true
                    task.linkedSlotId == null || task.linkedSlotId !in daySlotIds ->
                        task.linkedActivityId == slot.activityTypeId &&
                            firstSlotIdByActivity[slot.activityTypeId] == slot.id
                    else -> false
                }
            }
            SlotWithDetails(
                slot = slot,
                activity = act,
                tasks = slotTasks
            )
        }

        val assignedTaskIds = enrichedSlots.flatMap { it.tasks }.map { it.id }.toSet()
        val unassignedDayTasks = dayTasks.filter { it.id !in assignedTaskIds }

        // 6. События на этот день
        val dayEvents = events.filter { it.dateIso == dateIso }
        val allDayEvents = dayEvents.filter { it.isAllDay }
        val timedEvents = dayEvents.filter { !it.isAllDay }.sortedBy { it.parseStartTime() ?: LocalTime.MIN }

        // 7. Сборка таймлайна с окнами (перерывами)
        val timelineItems = mutableListOf<ScheduleTimelineItem>()
        var previousEndTime: LocalTime? = null

        val allItems = (enrichedSlots.map { TimelineTempItem.LessonItem(it) } +
            timedEvents.map { TimelineTempItem.EventItem(it) })
            .sortedBy { it.startTime }

        for (item in allItems) {
            val itemStart = item.startTime
            if (previousEndTime != null) {
                val gapMinutes = ChronoUnit.MINUTES.between(previousEndTime, itemStart)
                if (gapMinutes >= 25) {
                    timelineItems.add(
                        ScheduleTimelineItem.BreakWindow(
                            durationMinutes = gapMinutes,
                            startTime = previousEndTime.toString().take(5),
                            endTime = itemStart.toString().take(5)
                        )
                    )
                }
            }
            when (item) {
                is TimelineTempItem.LessonItem -> timelineItems.add(ScheduleTimelineItem.Lesson(item.details))
                is TimelineTempItem.EventItem -> timelineItems.add(ScheduleTimelineItem.TimedEvent(item.event))
            }
            previousEndTime = item.endTime
        }

        // 8. Live-статус текущего занятия (строго если выбран сегодняшний день)
        val isSelectedDateToday = targetDate == LocalDate.now()
        val liveStatus = if (isSelectedDateToday && currentTime != null && enrichedSlots.isNotEmpty()) {
            calculateLiveStatus(currentTime, enrichedSlots)
        } else {
            null
        }

        return DayScheduleUiModel(
            date = targetDate,
            holidayOrVacationTitle = holidayOrVacationTitle,
            isHoliday = isHoliday,
            isVacation = isVacation,
            allDayEvents = allDayEvents,
            timelineItems = timelineItems,
            pendingTasks = dayTasks.filter { !it.isCompleted },
            unassignedTasks = unassignedDayTasks,
            liveStatus = liveStatus,
            activeWeekCycle = activeCycle
        )
    }

    private fun calculateLiveStatus(
        now: LocalTime,
        slots: List<SlotWithDetails>
    ): LiveLessonStatus {
        val sorted = slots.sortedBy { it.slot.parseStartTime() }

        // Идет ли сейчас какой-либо урок?
        for (slotWithDetails in sorted) {
            val start = slotWithDetails.slot.parseStartTime()
            val end = slotWithDetails.slot.parseEndTime()

            if (!now.isBefore(start) && !now.isAfter(end)) {
                val totalDuration = Duration.between(start, end).toMinutes().coerceAtLeast(1)
                val elapsed = Duration.between(start, now).toMinutes()
                val progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                val minutesLeft = Duration.between(now, end).toMinutes().coerceAtLeast(0)

                val nextSlot = sorted.firstOrNull { it.slot.parseStartTime().isAfter(end) }

                return LiveLessonStatus(
                    currentSlot = slotWithDetails,
                    nextSlot = nextSlot,
                    progress = progress,
                    minutesLeftInCurrent = minutesLeft,
                    minutesUntilNext = 0,
                    isBreakNow = false
                )
            }
        }

        // Если сейчас перерыв или время до первой пары
        val nextSlot = sorted.firstOrNull { it.slot.parseStartTime().isAfter(now) }
        if (nextSlot != null) {
            val minutesUntilNext = ChronoUnit.MINUTES.between(now, nextSlot.slot.parseStartTime())
            // Показываем плашку, если до занятия меньше 120 минут
            if (minutesUntilNext in 0..120) {
                return LiveLessonStatus(
                    currentSlot = null,
                    nextSlot = nextSlot,
                    progress = 0f,
                    minutesLeftInCurrent = 0,
                    minutesUntilNext = minutesUntilNext,
                    isBreakNow = true
                )
            }
        }

        return LiveLessonStatus()
    }
}

private sealed interface TimelineTempItem {
    val startTime: LocalTime
    val endTime: LocalTime

    data class LessonItem(val details: SlotWithDetails) : TimelineTempItem {
        override val startTime: LocalTime = details.slot.parseStartTime()
        override val endTime: LocalTime = details.slot.parseEndTime()
    }

    data class EventItem(val event: CalendarEvent) : TimelineTempItem {
        override val startTime: LocalTime = event.parseStartTime() ?: LocalTime.MIN
        override val endTime: LocalTime = event.parseEndTime() ?: startTime.plusHours(1)
    }
}

