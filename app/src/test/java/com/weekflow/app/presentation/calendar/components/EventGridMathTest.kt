package com.weekflow.app.presentation.calendar.components

import com.weekflow.app.domain.model.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventGridMathTest {

    private fun event(
        id: String = "e1",
        startTime: String? = "09:00",
        endTime: String? = "10:00",
        isAllDay: Boolean = false
    ) = CalendarEvent(
        id = id,
        title = "Event $id",
        dateIso = "2026-11-17",
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay
    )

    @Test
    fun `formatMinutesOfDay formats normal and clamped boundary values`() {
        assertEquals("00:00", formatMinutesOfDay(0))
        assertEquals("00:00", formatMinutesOfDay(-30))
        assertEquals("09:05", formatMinutesOfDay(9 * 60 + 5))
        assertEquals("12:00", formatMinutesOfDay(12 * 60))
        assertEquals("23:59", formatMinutesOfDay(23 * 60 + 59))
        assertEquals("23:59", formatMinutesOfDay(24 * 60 + 10))
    }

    @Test
    fun `toTimeBand classifies all-day, morning, afternoon, and evening boundaries`() {
        assertEquals(
            EventTimeBand.ALL_DAY,
            event(startTime = "09:00", endTime = "10:00", isAllDay = true).toTimeBand()
        )
        assertEquals(EventTimeBand.MORNING, event(startTime = "00:00").toTimeBand())
        assertEquals(EventTimeBand.MORNING, event(startTime = "11:59").toTimeBand())
        assertEquals(EventTimeBand.AFTERNOON, event(startTime = "12:00").toTimeBand())
        assertEquals(EventTimeBand.AFTERNOON, event(startTime = "17:59").toTimeBand())
        assertEquals(EventTimeBand.EVENING, event(startTime = "18:00").toTimeBand())
        assertEquals(EventTimeBand.EVENING, event(startTime = "23:59").toTimeBand())
        // Missing or unparseable startTime falls back to hour 12 -> AFTERNOON
        assertEquals(EventTimeBand.AFTERNOON, event(startTime = null).toTimeBand())
        assertEquals(EventTimeBand.AFTERNOON, event(startTime = "bad").toTimeBand())
    }

    @Test
    fun `computeMovedBandTimes returns null pair when moved to ALL_DAY`() {
        val moved = computeMovedBandTimes(
            event(startTime = "09:30", endTime = "11:00"),
            EventTimeBand.ALL_DAY
        )
        assertNull(moved.first)
        assertNull(moved.second)
    }

    @Test
    fun `computeMovedBandTimes preserves times when staying in same timed band`() {
        val moved = computeMovedBandTimes(
            event(startTime = "08:15", endTime = "09:45"),
            EventTimeBand.MORNING
        )
        assertEquals("08:15" to "09:45", moved)
    }

    @Test
    fun `computeMovedBandTimes clamps duration between 30 and 180 minutes and clamps end to 23_59`() {
        // Short 10m event -> clamped up to 30m
        val shortMoved = computeMovedBandTimes(
            event(startTime = "09:00", endTime = "09:10"),
            EventTimeBand.AFTERNOON
        )
        assertEquals("14:00" to "14:30", shortMoved)

        // Long 300m event -> clamped down to 180m
        val longMoved = computeMovedBandTimes(
            event(startTime = "07:00", endTime = "12:00"),
            EventTimeBand.EVENING
        )
        assertEquals("19:00" to "22:00", longMoved)
        assertTrue(longMoved.second!! <= "23:59")

        // Normal 90m overnight event (22:30 -> 00:00 = 90m) moved to MORNING
        val overnightMoved = computeMovedBandTimes(
            event(startTime = "22:30", endTime = "00:00"),
            EventTimeBand.MORNING
        )
        assertEquals("09:00" to "10:30", overnightMoved)

        // All-day event moved to AFTERNOON uses default 60m duration
        val fromAllDay = computeMovedBandTimes(
            event(startTime = null, endTime = null, isAllDay = true),
            EventTimeBand.AFTERNOON
        )
        assertEquals("14:00" to "15:00", fromAllDay)
    }

    @Test
    fun `computeEventDurationMinutes handles normal, under-25m, and overnight spans`() {
        assertEquals(90, computeEventDurationMinutes(9 * 60, 10 * 60 + 30))
        assertEquals(10, computeEventDurationMinutes(9 * 60, 9 * 60 + 10))
        assertEquals(1, computeEventDurationMinutes(9 * 60, 9 * 60))
        // Overnight: 23:00 (1380) -> 01:30 (90) = 150m
        assertEquals(150, computeEventDurationMinutes(23 * 60, 90))
        // Full day 00:00 -> 23:59
        assertEquals(23 * 60 + 59, computeEventDurationMinutes(0, 23 * 60 + 59))
    }

    @Test
    fun `computeClampedDraggedEventTimes handles 00_00-23_59, 23_30-23_55, and preserves under-25m duration`() {
        // 00:00-23:59 (1439m) never throws and stays 0..1439 regardless of drag steps
        assertEquals(0 to 1439, computeClampedDraggedEventTimes(0, 1439, 4))
        assertEquals(0 to 1439, computeClampedDraggedEventTimes(0, 1439, -4))

        // 23:30-23:55 (start 1410, duration 25) dragged forward clamps safely without negative range
        val lateClamped = computeClampedDraggedEventTimes(
            initialStartMin = 23 * 60 + 30,
            initialActualDurationMin = 25,
            deltaSteps15 = 3
        )
        assertEquals(1414 to 1439, lateClamped)
        assertTrue(lateClamped.first <= lateClamped.second)
        assertEquals(25, lateClamped.second - lateClamped.first)

        // Dragging backward past 00:00 clamps start at 0 while preserving duration
        val earlyClamped = computeClampedDraggedEventTimes(
            initialStartMin = 15,
            initialActualDurationMin = 25,
            deltaSteps15 = -4
        )
        assertEquals(0 to 25, earlyClamped)

        // <25m event preserves its actual duration (e.g. 10m) across drag
        val shortDragged = computeClampedDraggedEventTimes(
            initialStartMin = 9 * 60,
            initialActualDurationMin = 10,
            deltaSteps15 = 2
        )
        assertEquals(9 * 60 + 30 to 9 * 60 + 40, shortDragged)
        assertEquals(10, shortDragged.second - shortDragged.first)

        val shortLateClamped = computeClampedDraggedEventTimes(
            initialStartMin = 23 * 60 + 45,
            initialActualDurationMin = 10,
            deltaSteps15 = 4
        )
        assertEquals(1429 to 1439, shortLateClamped)
        assertEquals(10, shortLateClamped.second - shortLateClamped.first)
    }

    @Test
    fun `layoutOverlappingEvents separates independent clusters and reuses columns on transitive overlap`() {
        assertEquals(emptyList<PositionedEvent<String>>(), layoutOverlappingEvents<String>(emptyList()))

        // Separate non-overlapping clusters
        val separate = layoutOverlappingEvents(
            listOf(
                "A" to (9 * 60 to 10 * 60),
                "B" to (11 * 60 to 12 * 60)
            )
        ).associateBy { it.item }
        assertEquals(0, separate["A"]!!.column)
        assertEquals(1, separate["A"]!!.totalColumns)
        assertEquals(0, separate["B"]!!.column)
        assertEquals(1, separate["B"]!!.totalColumns)

        // Transitive overlap: A (09:00-10:00) overlaps B (09:15-11:00), B overlaps C (10:15-11:15)
        // A and C do not overlap, so C reuses column 0 and cluster width is 2 columns.
        val transitive = layoutOverlappingEvents(
            listOf(
                "A" to (9 * 60 to 10 * 60),
                "B" to (9 * 60 + 15 to 11 * 60),
                "C" to (10 * 60 + 15 to 11 * 60 + 15)
            )
        ).associateBy { it.item }

        assertEquals(0, transitive["A"]!!.column)
        assertEquals(2, transitive["A"]!!.totalColumns)
        assertEquals(1, transitive["B"]!!.column)
        assertEquals(2, transitive["B"]!!.totalColumns)
        assertEquals(0, transitive["C"]!!.column)
        assertEquals(2, transitive["C"]!!.totalColumns)

        // Visual min height (25m) triggers overlap for 09:00-09:05 and 09:15-09:30,
        // while keeping real startMinutes/endMinutes intact.
        val shortOverlap = layoutOverlappingEvents(
            listOf(
                "S1" to (9 * 60 to 9 * 60 + 5),
                "S2" to (9 * 60 + 15 to 9 * 60 + 30)
            )
        ).associateBy { it.item }
        assertEquals(2, shortOverlap["S1"]!!.totalColumns)
        assertEquals(2, shortOverlap["S2"]!!.totalColumns)
        assertTrue(shortOverlap["S1"]!!.column != shortOverlap["S2"]!!.column)
        assertEquals(9 * 60 + 5, shortOverlap["S1"]!!.endMinutes)
    }
}
