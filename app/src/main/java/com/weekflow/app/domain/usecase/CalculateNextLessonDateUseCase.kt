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

    /**
     * Интеллектуальный поиск ближайшего следующего занятия по предмету (Smart Homework Engine).
     * Автоматически учитывает двухнедельный цикл (чет/нечет) и опционально пропускает праздники и каникулы.
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
                dueDate = referenceDate.plusDays(1),
                linkedSlotId = null,
                slot = null
            )
        }

        // Сканируем 4 недели вперед (28 дней)
        for (offset in 0..28) {
            val candidateDate = referenceDate.plusDays(offset.toLong())

            // Пропуск праздников и каникул, если включена опция
            if (preferences.skipHolidaysInSmartHomework) {
                if (preferences.isSaturdayWeekend && candidateDate.dayOfWeek.value == 6) {
                    continue
                }
                if (preferences.showRussianHolidays && RussianHolidaysProvider.isHoliday(candidateDate)) {
                    continue
                }
                if (vacations.any { it.contains(candidateDate) }) {
                    continue
                }
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
                // Если сегодняшний день, берем только те занятия, которые начнутся ПОСЛЕ текущего времени
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
            dueDate = referenceDate.plusDays(1),
            linkedSlotId = null,
            slot = null
        )
    }
}
