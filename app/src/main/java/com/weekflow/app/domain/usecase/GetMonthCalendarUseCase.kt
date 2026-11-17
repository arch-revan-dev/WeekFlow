package com.weekflow.app.domain.usecase

import com.weekflow.app.data.local.holidays.RussianHolidaysProvider
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarDayUiModel(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val holidayTitle: String?,
    val isVacation: Boolean,
    val slotsCount: Int,
    val eventsCount: Int,
    val pendingTasksCount: Int,
    val dotColorsHex: List<String>
)

class GetMonthCalendarUseCase(
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase
) {

    operator fun invoke(
        yearMonth: YearMonth,
        preferences: UserPreferences,
        activities: List<Activity>,
        slots: List<Slot>,
        events: List<CalendarEvent>,
        tasks: List<Task>,
        vacations: List<Vacation> = emptyList()
    ): List<CalendarDayUiModel> {
        val today = LocalDate.now()
        val firstOfMonth = yearMonth.atDay(1)
        val lastOfMonth = yearMonth.atEndOfMonth()

        val startGridDate = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endGridDate = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val activityColorMap = activities.associate { it.id to it.colorHex }

        val result = mutableListOf<CalendarDayUiModel>()
        var cursor = startGridDate

        while (!cursor.isAfter(endGridDate)) {
            val date = cursor
            val dateIso = date.toString()
            val isCurrentMonth = date.monthValue == yearMonth.monthValue

            // Праздники и каникулы
            var isHoliday = false
            var holidayTitle: String? = null
            if (preferences.showRussianHolidays) {
                holidayTitle = RussianHolidaysProvider.getHolidayTitle(date)
                if (holidayTitle != null) {
                    isHoliday = true
                }
            }
            val isVacation = vacations.any { it.contains(date) }

            // Слоты
            val activeCycle = calculateWeekCycleUseCase(date, preferences)
            val dayOfWeekVal = date.dayOfWeek.value
            val matchingSlots = slots.filter { slot ->
                slot.dayOfWeek == dayOfWeekVal &&
                    (!preferences.isTwoWeekCycleEnabled ||
                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                        slot.weekCycle == activeCycle)
            }

            // События
            val dayEvents = events.filter { it.occursOnDate(date) }

            // Задачи
            val dayTasks = tasks.filter { it.dueDateIso == dateIso && !it.isCompleted }

            // Цветовые точки для мини-индикаторов (до 4 точек)
            val dotColors = mutableListOf<String>()
            for (slot in matchingSlots) {
                val color = activityColorMap[slot.activityTypeId] ?: "#007AFF"
                if (!dotColors.contains(color)) {
                    dotColors.add(color)
                }
                if (dotColors.size >= 3) break
            }
            for (event in dayEvents) {
                if (!dotColors.contains(event.colorHex)) {
                    dotColors.add(event.colorHex)
                }
                if (dotColors.size >= 4) break
            }
            if (dayTasks.isNotEmpty() && dotColors.size < 4) {
                dotColors.add("#FF9500") // Оранжевая точка дедлайна
            }

            result.add(
                CalendarDayUiModel(
                    date = date,
                    isCurrentMonth = isCurrentMonth,
                    isToday = date == today,
                    isHoliday = isHoliday,
                    holidayTitle = holidayTitle,
                    isVacation = isVacation,
                    slotsCount = matchingSlots.size,
                    eventsCount = dayEvents.size,
                    pendingTasksCount = dayTasks.size,
                    dotColorsHex = dotColors.take(4)
                )
            )

            cursor = cursor.plusDays(1)
        }

        return result
    }
}
