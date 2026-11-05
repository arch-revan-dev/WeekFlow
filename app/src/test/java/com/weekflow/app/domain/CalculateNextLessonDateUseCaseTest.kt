package com.weekflow.app.domain

import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.usecase.CalculateNextLessonDateUseCase
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculateNextLessonDateUseCaseTest {

    private lateinit var useCase: CalculateNextLessonDateUseCase

    @Before
    fun setUp() {
        val weekCycleUseCase = CalculateWeekCycleUseCase()
        useCase = CalculateNextLessonDateUseCase(weekCycleUseCase)
    }

    @Test
    fun `when activity has no slots, returns next day with null linkedSlotId`() {
        val prefs = UserPreferences()
        val referenceDate = LocalDate.of(2026, 10, 19)

        val match = useCase(
            activityId = "non_existent_subject",
            referenceDate = referenceDate,
            referenceTime = null,
            preferences = prefs,
            allSlots = emptyList()
        )

        assertEquals(referenceDate.plusDays(1), match.dueDate)
        assertNull(match.linkedSlotId)
        assertNull(match.slot)
    }

    @Test
    fun `finds next lesson later on the same day if time is before slot start`() {
        val prefs = UserPreferences()
        val today = LocalDate.of(2026, 10, 19) // Monday (1)

        val slot = Slot(
            id = "math_slot_1",
            activityTypeId = "math",
            dayOfWeek = 1,
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "14:00",
            endTime = "15:35"
        )

        val match = useCase(
            activityId = "math",
            referenceDate = today,
            referenceTime = LocalTime.of(10, 0), // 10:00 is before 14:00
            preferences = prefs,
            allSlots = listOf(slot)
        )

        assertEquals(today, match.dueDate)
        assertEquals("math_slot_1", match.linkedSlotId)
        assertNotNull(match.slot)
    }

    @Test
    fun `finds next lesson in future days when current day slot has passed`() {
        val prefs = UserPreferences()
        val today = LocalDate.of(2026, 10, 19) // Monday (1)

        val mondaySlot = Slot(
            id = "math_mon",
            activityTypeId = "math",
            dayOfWeek = 1,
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "09:00",
            endTime = "10:35"
        )
        val wednesdaySlot = Slot(
            id = "math_wed",
            activityTypeId = "math",
            dayOfWeek = 3, // Wednesday
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "10:50",
            endTime = "12:25"
        )

        val match = useCase(
            activityId = "math",
            referenceDate = today,
            referenceTime = LocalTime.of(11, 0), // 11:00 is after Monday 09:00
            preferences = prefs,
            allSlots = listOf(mondaySlot, wednesdaySlot)
        )

        // Wednesday is Oct 21
        assertEquals(LocalDate.of(2026, 10, 21), match.dueDate)
        assertEquals("math_wed", match.linkedSlotId)
    }

    @Test
    fun `skips vacation dates when skipHolidaysInSmartHomework is enabled`() {
        val prefs = UserPreferences(skipHolidaysInSmartHomework = true)
        val monday = LocalDate.of(2026, 10, 19) // Day 1

        val slot = Slot(
            id = "math_slot",
            activityTypeId = "math",
            dayOfWeek = 3, // Wednesday
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "09:00",
            endTime = "10:35"
        )

        // User vacation covers this Wednesday (2026-10-21)
        val vacations = listOf(
            Vacation(
                title = "Осенние каникулы",
                startDateIso = "2026-10-20",
                endDateIso = "2026-10-25"
            )
        )

        val match = useCase(
            activityId = "math",
            referenceDate = monday,
            referenceTime = null,
            preferences = prefs,
            allSlots = listOf(slot),
            vacations = vacations
        )

        // Wednesday Oct 21 was skipped because of vacation; should find Wednesday of NEXT week (Oct 28)
        assertEquals(LocalDate.of(2026, 10, 28), match.dueDate)
        assertEquals("math_slot", match.linkedSlotId)
    }
}
