package com.weekflow.app.domain

import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CalculateWeekCycleUseCaseTest {

    private lateinit var useCase: CalculateWeekCycleUseCase

    @Before
    fun setUp() {
        useCase = CalculateWeekCycleUseCase()
    }

    @Test
    fun `when two-week cycle is disabled, returns EVERY_WEEK for any date`() {
        val prefs = UserPreferences(isTwoWeekCycleEnabled = false)
        val date = LocalDate.of(2026, 9, 15)

        val cycle = useCase(date, prefs)
        assertEquals(WeekCycle.EVERY_WEEK, cycle)
    }

    @Test
    fun `when two-week cycle is enabled, calculates correct week parity from semester start`() {
        // Semester start: Monday Sep 1, 2026 (or Monday Aug 31)
        val prefs = UserPreferences(
            isTwoWeekCycleEnabled = true,
            semesterStartDateIso = "2026-09-01", // Tuesday Sep 1 -> Start monday is Aug 31
            invertWeekCycle = false
        )

        // Week 0: Aug 31 .. Sep 6 -> NUMERATOR_A
        val week0Date = LocalDate.of(2026, 9, 2) // Wednesday
        assertEquals(WeekCycle.NUMERATOR_A, useCase(week0Date, prefs))

        // Week 1: Sep 7 .. Sep 13 -> DENOMINATOR_B
        val week1Date = LocalDate.of(2026, 9, 8) // Tuesday
        assertEquals(WeekCycle.DENOMINATOR_B, useCase(week1Date, prefs))

        // Week 2: Sep 14 .. Sep 20 -> NUMERATOR_A
        val week2Date = LocalDate.of(2026, 9, 15)
        assertEquals(WeekCycle.NUMERATOR_A, useCase(week2Date, prefs))
    }

    @Test
    fun `when invertWeekCycle is true, inverts numerator and denominator`() {
        val prefs = UserPreferences(
            isTwoWeekCycleEnabled = true,
            semesterStartDateIso = "2026-09-01",
            invertWeekCycle = true
        )

        // Inverted: Week 0 should now be DENOMINATOR_B
        val week0Date = LocalDate.of(2026, 9, 2)
        assertEquals(WeekCycle.DENOMINATOR_B, useCase(week0Date, prefs))

        // Inverted: Week 1 should now be NUMERATOR_A
        val week1Date = LocalDate.of(2026, 9, 8)
        assertEquals(WeekCycle.NUMERATOR_A, useCase(week1Date, prefs))
    }

    @Test
    fun `isSlotActiveOnDate correctly filters slots based on day and cycle`() {
        val prefs = UserPreferences(
            isTwoWeekCycleEnabled = true,
            semesterStartDateIso = "2026-09-01"
        )
        val targetWednesday = LocalDate.of(2026, 9, 2) // Day 3 (Wednesday), Numerator A

        val slotEveryWeek = Slot(
            activityTypeId = "math",
            dayOfWeek = 3,
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "09:00",
            endTime = "10:35"
        )
        val slotNumerator = Slot(
            activityTypeId = "cs",
            dayOfWeek = 3,
            weekCycle = WeekCycle.NUMERATOR_A,
            startTime = "10:50",
            endTime = "12:25"
        )
        val slotDenominator = Slot(
            activityTypeId = "physics",
            dayOfWeek = 3,
            weekCycle = WeekCycle.DENOMINATOR_B,
            startTime = "10:50",
            endTime = "12:25"
        )
        val slotWrongDay = Slot(
            activityTypeId = "sport",
            dayOfWeek = 4, // Thursday
            weekCycle = WeekCycle.EVERY_WEEK,
            startTime = "09:00",
            endTime = "10:35"
        )

        assertTrue(useCase.isSlotActiveOnDate(slotEveryWeek, targetWednesday, prefs))
        assertTrue(useCase.isSlotActiveOnDate(slotNumerator, targetWednesday, prefs))
        assertFalse(useCase.isSlotActiveOnDate(slotDenominator, targetWednesday, prefs))
        assertFalse(useCase.isSlotActiveOnDate(slotWrongDay, targetWednesday, prefs))
    }
}
