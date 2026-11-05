package com.weekflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppProfile(
    val title: String,
    val itemTerm: String,
    val itemTermGenitive: String,
    val plural: String,
    val pluralGenitive: String,
    val taskTerm: String,
    val instructorTerm: String,
    val locationTerm: String
) {
    STUDY(
        title = "Учеба (Школа / ВУЗ)",
        itemTerm = "Урок / Пара",
        itemTermGenitive = "урока / пары",
        plural = "Занятия",
        pluralGenitive = "занятий",
        taskTerm = "Задания",
        instructorTerm = "Преподаватель",
        locationTerm = "Аудитория / Кабинет"
    ),
    WORK(
        title = "Работа и бизнес",
        itemTerm = "Смена / Встреча",
        itemTermGenitive = "смены / встречи",
        plural = "Рабочий график",
        pluralGenitive = "смен и встреч",
        taskTerm = "Дедлайны",
        instructorTerm = "Коллега / Лид",
        locationTerm = "Переговорная / Локация"
    ),
    HEALTH(
        title = "Спорт и ритм дня",
        itemTerm = "Тренировка / Дело",
        itemTermGenitive = "тренировки",
        plural = "План на день",
        pluralGenitive = "тренировок и дел",
        taskTerm = "Привычки",
        instructorTerm = "Тренер",
        locationTerm = "Зал / Место"
    ),
    UNIVERSAL(
        title = "Универсальный",
        itemTerm = "Событие",
        itemTermGenitive = "события",
        plural = "Расписание",
        pluralGenitive = "событий",
        taskTerm = "Задачи",
        instructorTerm = "Контакт",
        locationTerm = "Место"
    )
}

@Serializable
enum class ActivityCategory(
    val displayName: String,
    val defaultColorHex: String
) {
    STUDY("Учеба", "#007AFF"),
    WORK("Работа", "#34C759"),
    HEALTH("Спорт и здоровье", "#FF2D55"),
    PERSONAL("Личное", "#FF9500"),
    OTHER("Прочее", "#5856D6")
}

@Serializable
enum class WeekCycle(val displayName: String, val shortBadge: String) {
    EVERY_WEEK("Каждую неделю", ""),
    NUMERATOR_A("Числитель / Нечетная (A)", "I • Числ"),
    DENOMINATOR_B("Знаменатель / Четная (B)", "II • Знам")
}

@Serializable
enum class AppThemeMode(val displayName: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Темная")
}

@Serializable
enum class TaskPriority(val displayName: String, val colorHex: String) {
    HIGH("Высокий", "#FF3B30"),
    MEDIUM("Средний", "#FF9500"),
    LOW("Низкий", "#007AFF"),
    NONE("Обычный", "#8E8E93")
}

@Serializable
enum class ScheduleCardStyle(val displayName: String) {
    OUTLINED("Обводка (Weeklie)"),
    LEFT_ACCENT("Полоса слева"),
    SOFT_TINT("Мягкий тинт"),
    MINIMAL("Минимализм")
}

@Serializable
enum class ScheduleViewMode(val displayName: String) {
    DAY_LIST("День"),
    WEEK_TABLE_GRID("Таблица"),
    TIMELINE_GRID("Сетка")
}

@Serializable
enum class CardDensity(val displayName: String, val verticalPaddingDp: Int) {
    COMPACT("Компактная", 6),
    STANDARD("Стандартная", 10),
    COMFORTABLE("Просторная", 14)
}
