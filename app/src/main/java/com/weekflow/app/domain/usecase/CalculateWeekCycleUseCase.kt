package com.weekflow.app.domain.usecase

import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.WeekCycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class CalculateWeekCycleUseCase {

    /**
     * Вычисляет активный недельный цикл для указанной даты [targetDate].
     * Если двухнедельный режим выключен в настройках — всегда возвращает [WeekCycle.EVERY_WEEK].
     */
    operator fun invoke(targetDate: LocalDate, preferences: UserPreferences): WeekCycle {
        if (!preferences.isTwoWeekCycleEnabled) {
            return WeekCycle.EVERY_WEEK
        }

        val semesterStart = preferences.parseSemesterStartDate()
        val startMonday = semesterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetMonday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val weeksBetween = ChronoUnit.WEEKS.between(startMonday, targetMonday)
        val mod = Math.floorMod(weeksBetween, 2)

        val isNumerator = if (preferences.invertWeekCycle) mod == 1 else mod == 0

        return if (isNumerator) WeekCycle.NUMERATOR_A else WeekCycle.DENOMINATOR_B
    }

    /**
     * Проверяет, активно ли расписание слота [slot] в конкретную дату [targetDate].
     */
    fun isSlotActiveOnDate(slot: Slot, targetDate: LocalDate, preferences: UserPreferences): Boolean {
        if (slot.dayOfWeek != targetDate.dayOfWeek.value) {
            return false
        }
        if (!preferences.isTwoWeekCycleEnabled || slot.weekCycle == WeekCycle.EVERY_WEEK) {
            return true
        }
        val activeCycle = invoke(targetDate, preferences)
        return slot.weekCycle == activeCycle
    }
}
