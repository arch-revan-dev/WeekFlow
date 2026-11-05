package com.weekflow.app.data.local.holidays

import java.time.LocalDate

object RussianHolidaysProvider {

    /**
     * Возвращает официальное название государственного праздника РФ для указанной даты или null, если день рабочий.
     */
    fun getHolidayTitle(date: LocalDate): String? {
        val month = date.monthValue
        val day = date.dayOfMonth

        return when {
            month == 1 && day in 1..8 -> if (day == 7) "Рождество Христово" else "Новогодние каникулы"
            month == 2 && day == 23 -> "День защитника Отечества"
            month == 3 && day == 8 -> "Международный женский день"
            month == 5 && day == 1 -> "Праздник Весны и Труда"
            month == 5 && day == 9 -> "День Победы"
            month == 6 && day == 12 -> "День России"
            month == 11 && day == 4 -> "День народного единства"
            month == 12 && day == 31 -> "Новогодний предпраздничный день"
            else -> null
        }
    }

    /**
     * Проверяет, является ли дата государственным праздником РФ.
     */
    fun isHoliday(date: LocalDate): Boolean {
        return getHolidayTitle(date) != null
    }

    /**
     * Возвращает список всех праздников для указанного месяца и года.
     */
    fun getHolidaysForMonth(year: Int, month: Int): List<Pair<LocalDate, String>> {
        val firstDay = LocalDate.of(year, month, 1)
        val daysInMonth = firstDay.lengthOfMonth()
        val list = mutableListOf<Pair<LocalDate, String>>()

        for (day in 1..daysInMonth) {
            val date = LocalDate.of(year, month, day)
            val title = getHolidayTitle(date)
            if (title != null) {
                list.add(date to title)
            }
        }
        return list
    }
}
