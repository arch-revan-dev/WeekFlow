package com.weekflow.app.domain.usecase

import com.weekflow.app.domain.model.NextLessonMatch
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class DetectedActivity(
    val slot: Slot,
    val isOngoingOrUpcoming: Boolean,
    val nextLessonMatch: NextLessonMatch
)

class DetectActiveOrRecentActivityUseCase(
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase,
    private val calculateNextLessonDateUseCase: CalculateNextLessonDateUseCase
) {
    /**
     * Автоматически определяет текущее или только что завершившееся занятие
     * и подбирает дату следующего такого занятия в расписании.
     * Никаких предвыборов будущего (еще не начавшегося) урока во время перемен!
     */
    operator fun invoke(
        allSlots: List<Slot>,
        preferences: UserPreferences,
        vacations: List<Vacation> = emptyList(),
        currentDate: LocalDate = LocalDate.now(),
        currentTime: LocalTime = LocalTime.now()
    ): DetectedActivity? {
        if (allSlots.isEmpty()) return null

        val currentCycle = calculateWeekCycleUseCase(currentDate, preferences)
        val todaySlots = allSlots.filter { slot ->
            slot.dayOfWeek == currentDate.dayOfWeek.value &&
                (!preferences.isTwoWeekCycleEnabled ||
                    slot.weekCycle == WeekCycle.EVERY_WEEK ||
                    slot.weekCycle == currentCycle)
        }.sortedBy { it.parseStartTime() }

        if (todaySlots.isEmpty()) return null

        // Шаг 1: Урок идет прямо сейчас (now >= start && now <= end)
        val ongoingSlot = todaySlots.firstOrNull { slot ->
            val start = slot.parseStartTime()
            val end = slot.parseEndTime()
            !currentTime.isBefore(start) && !currentTime.isAfter(end)
        }

        if (ongoingSlot != null) {
            val nextMatch = calculateNextLessonDateUseCase(
                activityId = ongoingSlot.activityTypeId,
                referenceDate = currentDate,
                referenceTime = ongoingSlot.parseEndTime(),
                preferences = preferences,
                allSlots = allSlots,
                vacations = vacations
            )
            return DetectedActivity(
                slot = ongoingSlot,
                isOngoingOrUpcoming = true,
                nextLessonMatch = nextMatch
            )
        }

        // Шаг 2: Сейчас перемена между уроками или уроки закончились.
        // Ищем среди всех слотов сегодняшнего дня те, которые уже начались ранее (start <= now),
        // и берем самый последний из них (maxByOrNull { it.endTime }) в пределах 4 часов (240 минут)
        val endedOrStartedEarlier = todaySlots.filter { slot ->
            val start = slot.parseStartTime()
            val end = slot.parseEndTime()
            if (!currentTime.isBefore(start)) {
                val diffMinutes = Duration.between(end, currentTime).toMinutes()
                diffMinutes in 0..240
            } else {
                false
            }
        }.maxByOrNull { it.parseEndTime() }

        if (endedOrStartedEarlier != null) {
            val nextMatch = calculateNextLessonDateUseCase(
                activityId = endedOrStartedEarlier.activityTypeId,
                referenceDate = currentDate,
                referenceTime = endedOrStartedEarlier.parseEndTime(),
                preferences = preferences,
                allSlots = allSlots,
                vacations = vacations
            )
            return DetectedActivity(
                slot = endedOrStartedEarlier,
                isOngoingOrUpcoming = false,
                nextLessonMatch = nextMatch
            )
        }

        return null
    }
}
