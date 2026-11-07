package com.weekflow.app.domain.usecase

import com.weekflow.app.data.local.holidays.RussianHolidaysProvider
import com.weekflow.app.domain.model.NextLessonMatch
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import java.time.LocalDate
import java.time.LocalTime

class CalculateNextLessonDateUseCase(
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase
) {

    fun isOffDay(
        date: LocalDate,
        preferences: UserPreferences,
        vacations: List<Vacation>
    ): Boolean {
        if (!preferences.skipHolidaysInSmartHomework) return false
        if (date.dayOfWeek.value == 7) return true
        if (preferences.isSaturdayWeekend && date.dayOfWeek.value == 6) return true
        if (preferences.showRussianHolidays && RussianHolidaysProvider.isHoliday(date)) return true
        if (vacations.any { it.contains(date) }) return true
        return false
    }

    fun findNextValidStudyDay(
        startDate: LocalDate,
        preferences: UserPreferences,
        vacations: List<Vacation>
    ): LocalDate {
        for (offset in 0..180) {
            val candidate = startDate.plusDays(offset.toLong())
            if (!isOffDay(candidate, preferences, vacations)) {
                return candidate
            }
        }
        return startDate
    }

    /**
     * Интеллектуальный поиск ближайшего следующего занятия по предмету (Умный движок задач).
     * Автоматически учитывает двухнедельный цикл (чет/нечет) и пропускает выходные, праздники и каникулы.
     */
    operator fun invoke(
        activityId: String,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime? = null,
        preferences: UserPreferences,
        allSlots: List<Slot>,
        vacations: List<Vacation> = emptyList()
    ): NextLessonMatch {
        val subjectSlots = allSlots.filter { it.activityTypeId == activityId }
        if (subjectSlots.isEmpty()) {
            return NextLessonMatch(
                dueDate = findNextValidStudyDay(referenceDate.plusDays(1), preferences, vacations),
                linkedSlotId = null,
                slot = null
            )
        }

        // Сканируем до 120 дней вперед (чтобы гарантированно перешагнуть любые каникулы/отпуска)
        for (offset in 0..120) {
            val candidateDate = referenceDate.plusDays(offset.toLong())

            if (isOffDay(candidateDate, preferences, vacations)) {
                continue
            }

            val activeCycle = calculateWeekCycleUseCase(candidateDate, preferences)

            val matchingSlots = subjectSlots
                .filter { slot ->
                    slot.dayOfWeek == candidateDate.dayOfWeek.value &&
                        (!preferences.isTwoWeekCycleEnabled ||
                            slot.weekCycle == WeekCycle.EVERY_WEEK ||
                            slot.weekCycle == activeCycle)
                }
                .sortedBy { it.parseStartTime() }

            if (offset == 0) {
                val futureSlots = if (referenceTime != null) {
                    matchingSlots.filter { it.parseStartTime().isAfter(referenceTime) }
                } else {
                    matchingSlots
                }
                val best = futureSlots.firstOrNull()
                if (best != null) {
                    return NextLessonMatch(
                        dueDate = candidateDate,
                        linkedSlotId = best.id,
                        slot = best
                    )
                }
            } else {
                val best = matchingSlots.firstOrNull()
                if (best != null) {
                    return NextLessonMatch(
                        dueDate = candidateDate,
                        linkedSlotId = best.id,
                        slot = best
                    )
                }
            }
        }

        return NextLessonMatch(
            dueDate = findNextValidStudyDay(referenceDate.plusDays(1), preferences, vacations),
            linkedSlotId = null,
            slot = null
        )
    }
}
