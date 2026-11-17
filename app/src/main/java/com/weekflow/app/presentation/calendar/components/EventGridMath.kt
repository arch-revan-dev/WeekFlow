package com.weekflow.app.presentation.calendar.components

import com.weekflow.app.domain.model.CalendarEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MINUTES_PER_DAY = 24 * 60
private const val MAX_MINUTE_OF_DAY = 23 * 60 + 59
private const val MIN_VISUAL_DURATION_MINUTES = 25
private const val MIN_BAND_DURATION_MINUTES = 30
private const val MAX_BAND_DURATION_MINUTES = 180
private const val DEFAULT_BAND_DURATION_MINUTES = 60

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

enum class EventTimeBand(
    val label: String,
    val subtitle: String,
    val defaultStart: String,
    val defaultEnd: String,
    val isAllDay: Boolean
) {
    ALL_DAY("Весь день", "24 ч", "09:00", "10:00", true),
    MORNING("Утро", "< 12:00", "09:00", "10:00", false),
    AFTERNOON("День", "12:00–18:00", "14:00", "15:00", false),
    EVENING("Вечер", "≥ 18:00", "19:00", "20:00", false)
}

data class PositionedEvent<T>(
    val item: T,
    val startMinutes: Int,
    val endMinutes: Int,
    val column: Int,
    val totalColumns: Int
)

fun formatMinutesOfDay(minutes: Int): String {
    val clamped = minutes.coerceIn(0, MAX_MINUTE_OF_DAY)
    return String.format(Locale.US, "%02d:%02d", clamped / 60, clamped % 60)
}

fun CalendarEvent.toTimeBand(): EventTimeBand {
    if (isAllDay) return EventTimeBand.ALL_DAY
    val hour = parseStartTime()?.hour ?: 12
    return when {
        hour < 12 -> EventTimeBand.MORNING
        hour < 18 -> EventTimeBand.AFTERNOON
        else -> EventTimeBand.EVENING
    }
}

fun computeMovedBandTimes(
    event: CalendarEvent,
    targetBand: EventTimeBand
): Pair<String?, String?> {
    if (targetBand.isAllDay) return null to null
    val originalBand = event.toTimeBand()
    if (originalBand == targetBand && !event.isAllDay && !event.startTime.isNullOrBlank()) {
        return event.startTime to event.endTime
    }

    val start = event.parseStartTime()
    val end = event.parseEndTime()
    val durationMin = if (!event.isAllDay && start != null && end != null) {
        val startTotal = start.hour * 60 + start.minute
        val endTotal = end.hour * 60 + end.minute
        val rawMinutes = when {
            endTotal > startTotal -> endTotal - startTotal
            endTotal < startTotal -> (MINUTES_PER_DAY - startTotal) + endTotal
            else -> DEFAULT_BAND_DURATION_MINUTES
        }
        rawMinutes.coerceIn(MIN_BAND_DURATION_MINUTES, MAX_BAND_DURATION_MINUTES)
    } else {
        DEFAULT_BAND_DURATION_MINUTES
    }

    val defaultStart = LocalTime.parse(targetBand.defaultStart, TIME_FORMATTER)
    val startMin = defaultStart.hour * 60 + defaultStart.minute
    val endMin = (startMin + durationMin).coerceAtMost(MAX_MINUTE_OF_DAY)
    return formatMinutesOfDay(startMin) to formatMinutesOfDay(endMin)
}

fun computeEventDurationMinutes(startMinutes: Int, endMinutes: Int): Int {
    val clampedStart = startMinutes.coerceIn(0, MAX_MINUTE_OF_DAY)
    val clampedEnd = endMinutes.coerceIn(0, MINUTES_PER_DAY)
    val raw = if (clampedEnd >= clampedStart) {
        clampedEnd - clampedStart
    } else {
        (MINUTES_PER_DAY - clampedStart) + clampedEnd
    }
    return raw.coerceIn(1, MAX_MINUTE_OF_DAY)
}

fun computeClampedDraggedEventTimes(
    initialStartMin: Int,
    initialActualDurationMin: Int,
    deltaSteps15: Int
): Pair<Int, Int> {
    val duration = initialActualDurationMin.coerceIn(1, MAX_MINUTE_OF_DAY)
    val maxStart = (MAX_MINUTE_OF_DAY - duration).coerceAtLeast(0)
    val newStart = (initialStartMin + deltaSteps15 * 15).coerceIn(0, maxStart)
    val newEnd = (newStart + duration).coerceAtMost(MAX_MINUTE_OF_DAY)
    return newStart to newEnd
}

private data class LayoutEntry<T>(
    val item: T,
    val realStart: Int,
    val realEnd: Int,
    val visualStart: Int,
    val visualEnd: Int
)

fun <T> layoutOverlappingEvents(
    items: List<Pair<T, Pair<Int, Int>>>
): List<PositionedEvent<T>> {
    if (items.isEmpty()) return emptyList()

    val sorted = items.map { (item, bounds) ->
        val realStart = bounds.first
        val realEnd = bounds.second
        val effectiveDayEnd = if (realEnd < realStart) MINUTES_PER_DAY else realEnd
        val visualEnd = maxOf(realStart + MIN_VISUAL_DURATION_MINUTES, effectiveDayEnd)
        LayoutEntry(
            item = item,
            realStart = realStart,
            realEnd = realEnd,
            visualStart = realStart,
            visualEnd = visualEnd
        )
    }.sortedWith(compareBy({ it.visualStart }, { -(it.visualEnd - it.visualStart) }))

    val clusters = mutableListOf<MutableList<LayoutEntry<T>>>()
    var currentCluster = mutableListOf<LayoutEntry<T>>()
    var clusterEnd = -1

    for (entry in sorted) {
        if (currentCluster.isEmpty()) {
            currentCluster.add(entry)
            clusterEnd = entry.visualEnd
        } else if (entry.visualStart < clusterEnd) {
            currentCluster.add(entry)
            clusterEnd = maxOf(clusterEnd, entry.visualEnd)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(entry)
            clusterEnd = entry.visualEnd
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    val result = mutableListOf<PositionedEvent<T>>()
    for (cluster in clusters) {
        val columnEndTimes = mutableListOf<Int>()
        val clusterPositions = mutableListOf<Pair<LayoutEntry<T>, Int>>()

        for (entry in cluster) {
            var placedCol = -1
            for (col in columnEndTimes.indices) {
                if (columnEndTimes[col] <= entry.visualStart) {
                    placedCol = col
                    columnEndTimes[col] = entry.visualEnd
                    break
                }
            }
            if (placedCol == -1) {
                placedCol = columnEndTimes.size
                columnEndTimes.add(entry.visualEnd)
            }
            clusterPositions.add(entry to placedCol)
        }

        val numCols = maxOf(1, columnEndTimes.size)
        for ((entry, colIndex) in clusterPositions) {
            result.add(
                PositionedEvent(
                    item = entry.item,
                    startMinutes = entry.realStart,
                    endMinutes = entry.realEnd,
                    column = colIndex,
                    totalColumns = numCols
                )
            )
        }
    }

    return result
}
