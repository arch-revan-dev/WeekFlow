package com.weekflow.app.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateTimeUtils {

    private val russianMonthNamesNominative = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    private val russianMonthNamesGenitive = listOf(
        "янв.", "февр.", "марта", "апр.", "мая", "июня",
        "июля", "авг.", "сент.", "окт.", "нояб.", "дек."
    )

    fun getDayOfWeekShort(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Пн"
            2 -> "Вт"
            3 -> "Ср"
            4 -> "Чт"
            5 -> "Пт"
            6 -> "Сб"
            7 -> "Вс"
            else -> ""
        }
    }

    fun getDayOfWeekFull(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> ""
        }
    }

    fun formatMonthYearHeader(date: LocalDate): String {
        val monthName = russianMonthNamesNominative[date.monthValue - 1]
        return "$monthName ${date.year}"
    }

    fun formatDateWithDay(date: LocalDate): String {
        val dayOfWeek = getDayOfWeekShort(date.dayOfWeek.value)
        val monthGenitive = russianMonthNamesGenitive[date.monthValue - 1]
        return "$dayOfWeek, ${date.dayOfMonth} $monthGenitive"
    }

    fun formatShortDayMonth(date: LocalDate): String {
        val monthGenitive = russianMonthNamesGenitive[date.monthValue - 1]
        return "${date.dayOfMonth} $monthGenitive"
    }

    fun formatWeekRange(weekDays: List<LocalDate>): String {
        if (weekDays.isEmpty()) return ""
        val start = weekDays.first()
        val end = weekDays.last()
        val startMonth = russianMonthNamesGenitive[start.monthValue - 1]
        val endMonth = russianMonthNamesGenitive[end.monthValue - 1]
        return if (start.monthValue == end.monthValue) {
            "${start.dayOfMonth}–${end.dayOfMonth} $endMonth"
        } else {
            "${start.dayOfMonth} $startMonth – ${end.dayOfMonth} $endMonth"
        }
    }

    fun getRelativeWeekLabel(selectedDate: LocalDate, today: LocalDate = LocalDate.now()): String {
        val selectedMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val todayMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksDiff = ChronoUnit.WEEKS.between(todayMonday, selectedMonday)
        return when {
            weeksDiff == 0L -> "Эта неделя"
            weeksDiff == 1L -> "След. неделя"
            weeksDiff == -1L -> "Прошл. неделя"
            weeksDiff > 1L -> "Через $weeksDiff нед."
            else -> "${-weeksDiff} нед. назад"
        }
    }

    fun formatMinutesDuration(minutes: Long): String {
        if (minutes < 60) {
            return "$minutes мин"
        }
        val hours = minutes / 60
        val rem = minutes % 60
        return if (rem == 0L) {
            "$hours ч"
        } else {
            "$hours ч $rem мин"
        }
    }
}
