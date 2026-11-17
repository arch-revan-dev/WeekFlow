package com.weekflow.app.domain

import com.weekflow.app.data.serialization.JsonImportExportManager
import com.weekflow.app.data.serialization.ValidationResult
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.EventRepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarEventRecurrenceTest {

    private fun event(
        startIso: String = "2026-11-16", // Monday
        repeatType: EventRepeatType = EventRepeatType.NONE,
        repeatDays: List<Int> = emptyList(),
        repeatEndDateIso: String? = null
    ) = CalendarEvent(
        id = "evt-1",
        title = "Школа",
        dateIso = startIso,
        startTime = "08:00",
        endTime = "16:00",
        repeatType = repeatType,
        repeatDays = repeatDays,
        repeatEndDateIso = repeatEndDateIso
    )

    @Test
    fun `NONE occurs strictly on start date`() {
        val evt = event(startIso = "2026-11-16", repeatType = EventRepeatType.NONE)
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 15)))
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 16)))
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 17)))
    }

    @Test
    fun `DAILY occurs every day within inclusive start and repeatEnd bounds`() {
        val evt = event(
            startIso = "2026-11-16",
            repeatType = EventRepeatType.DAILY,
            repeatEndDateIso = "2026-11-18"
        )
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 15)))
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 16)))
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 17)))
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 18)))
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 19)))
    }

    @Test
    fun `WEEKDAYS occurs Monday through Friday only`() {
        val evt = event(startIso = "2026-11-16", repeatType = EventRepeatType.WEEKDAYS)
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 16)))  // Mon
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 20)))  // Fri
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 21))) // Sat
        assertFalse(evt.occursOnDate(LocalDate.of(2026, 11, 22))) // Sun
        assertTrue(evt.occursOnDate(LocalDate.of(2026, 11, 23)))  // Next Mon
    }

    @Test
    fun `WEEKLY matches selected days and falls back to start weekday when repeatDays empty`() {
        val monWedFri = event(
            startIso = "2026-11-16",
            repeatType = EventRepeatType.WEEKLY,
            repeatDays = listOf(1, 3, 5)
        )
        assertTrue(monWedFri.occursOnDate(LocalDate.of(2026, 11, 16)))  // Mon
        assertFalse(monWedFri.occursOnDate(LocalDate.of(2026, 11, 17))) // Tue
        assertTrue(monWedFri.occursOnDate(LocalDate.of(2026, 11, 18)))  // Wed
        assertTrue(monWedFri.occursOnDate(LocalDate.of(2026, 11, 25)))  // Next Wed

        val fallback = event(
            startIso = "2026-11-17", // Tuesday
            repeatType = EventRepeatType.WEEKLY,
            repeatDays = emptyList()
        )
        assertFalse(fallback.occursOnDate(LocalDate.of(2026, 11, 18)))
        assertTrue(fallback.occursOnDate(LocalDate.of(2026, 11, 24))) // Next Tue
    }

    @Test
    fun `MONTHLY clamps 31st day to month length cleanly`() {
        val monthly31 = event(startIso = "2026-01-31", repeatType = EventRepeatType.MONTHLY)
        assertTrue(monthly31.occursOnDate(LocalDate.of(2026, 1, 31)))
        assertTrue(monthly31.occursOnDate(LocalDate.of(2026, 2, 28))) // Non-leap Feb
        assertFalse(monthly31.occursOnDate(LocalDate.of(2026, 2, 27)))
        assertTrue(monthly31.occursOnDate(LocalDate.of(2026, 4, 30))) // 30-day Apr
        assertTrue(monthly31.occursOnDate(LocalDate.of(2026, 5, 31)))
    }

    @Test
    fun `JsonImportExportManager nullifies repeatEndDateIso when earlier than start date`() {
        val json = """
            {
              "events": [
                {
                  "id": "evt-bad-end",
                  "title": "Школа",
                  "dateIso": "2026-12-01",
                  "startTime": "08:00",
                  "endTime": "16:00",
                  "repeatType": "WEEKDAYS",
                  "repeatEndDateIso": "2026-11-10"
                }
              ]
            }
        """.trimIndent()
        val result = JsonImportExportManager.validateAndSanitize(json)
        assertTrue(result is ValidationResult.Success)
        val evt = (result as ValidationResult.Success).payload.events.single()
        assertNull(evt.repeatEndDateIso)
        assertEquals(EventRepeatType.WEEKDAYS, evt.repeatType)
    }
}
