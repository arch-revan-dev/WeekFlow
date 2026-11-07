package com.weekflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppProfile(
    val title: String,
    val subjectTerm: String,
    val subjectPlaceholder: String,
    val shortSubjectPlaceholder: String,
    val itemTerm: String,
    val itemTermGenitive: String,
    val plural: String,
    val pluralGenitive: String,
    val taskTerm: String,
    val instructorTerm: String,
    val instructorPlaceholder: String,
    val locationTerm: String,
    val locationPlaceholder: String,
    val defaultDurationMinutes: Int,
    val defaultBreakMinutes: Int,
    val defaultUsePeriodNumbers: Boolean,
    val defaultLessonTypes: List<String>,
    val defaultCategory: ActivityCategory,
    val defaultBellTimes: List<Pair<String, String>>
) {
    STUDY(
        title = "Школа",
        subjectTerm = "Предмет",
        subjectPlaceholder = "например, Алгебра / Химия",
        shortSubjectPlaceholder = "например, Алг",
        itemTerm = "Урок",
        itemTermGenitive = "урока",
        plural = "Уроки",
        pluralGenitive = "уроков",
        taskTerm = "Задания",
        instructorTerm = "Учитель",
        instructorPlaceholder = "например, Иванова М.И.",
        locationTerm = "Кабинет",
        locationPlaceholder = "например, Каб. 304",
        defaultDurationMinutes = 45,
        defaultBreakMinutes = 10,
        defaultUsePeriodNumbers = true,
        defaultLessonTypes = listOf("Урок", "Контрольная", "Самостоятельная", "Лабораторная", "Факультатив", "Олимпиада", "Консультация"),
        defaultCategory = ActivityCategory.STUDY,
        defaultBellTimes = listOf(
            "08:15" to "09:00",
            "09:10" to "09:55",
            "10:15" to "11:00",
            "11:20" to "12:05",
            "12:25" to "13:10",
            "13:20" to "14:05",
            "14:15" to "15:00",
            "15:05" to "15:50"
        )
    ),
    UNIVERSITY(
        title = "ВУЗ / Колледж",
        subjectTerm = "Дисциплина",
        subjectPlaceholder = "например, Высшая математика",
        shortSubjectPlaceholder = "например, Матан",
        itemTerm = "Пара",
        itemTermGenitive = "пары",
        plural = "Пары",
        pluralGenitive = "пар",
        taskTerm = "Задания",
        instructorTerm = "Преподаватель",
        instructorPlaceholder = "например, проф. Смирнов А.В.",
        locationTerm = "Аудитория / Корпус",
        locationPlaceholder = "например, Ауд. 402",
        defaultDurationMinutes = 90,
        defaultBreakMinutes = 15,
        defaultUsePeriodNumbers = true,
        defaultLessonTypes = listOf("Лекция", "Семинар", "Практика", "Лабораторная", "Коллоквиум", "Консультация", "Зачёт", "Экзамен"),
        defaultCategory = ActivityCategory.STUDY,
        defaultBellTimes = listOf(
            "09:00" to "10:35",
            "10:45" to "12:20",
            "13:00" to "14:35",
            "14:45" to "16:20",
            "16:30" to "18:05",
            "18:15" to "19:50",
            "20:00" to "21:35"
        )
    ),
    WORK(
        title = "Работа и проекты",
        subjectTerm = "Проект / Направление",
        subjectPlaceholder = "например, Разработка / Маркетинг",
        shortSubjectPlaceholder = "например, Dev",
        itemTerm = "Встреча / Блок",
        itemTermGenitive = "встречи",
        plural = "Рабочие блоки",
        pluralGenitive = "встреч и блоков",
        taskTerm = "Задачи и дедлайны",
        instructorTerm = "Лид / Организатор",
        instructorPlaceholder = "например, тимлид Алексей",
        locationTerm = "Переговорная / Ссылка",
        locationPlaceholder = "например, Zoom / Переговорная B",
        defaultDurationMinutes = 60,
        defaultBreakMinutes = 0,
        defaultUsePeriodNumbers = false,
        defaultLessonTypes = listOf("Встреча", "Созвон", "Фокус-блок", "Планёрка", "Код-ревью", "Дедлайн", "Смена"),
        defaultCategory = ActivityCategory.WORK,
        defaultBellTimes = listOf(
            "09:00" to "10:00",
            "10:00" to "11:00",
            "11:00" to "12:00",
            "13:00" to "14:00",
            "14:00" to "15:00",
            "15:00" to "16:00",
            "16:00" to "17:00",
            "17:00" to "18:00"
        )
    ),
    HEALTH(
        title = "Спорт и тренировки",
        subjectTerm = "Вид активности",
        subjectPlaceholder = "например, Силовая / Бассейн",
        shortSubjectPlaceholder = "например, Зал",
        itemTerm = "Тренировка",
        itemTermGenitive = "тренировки",
        plural = "Тренировки",
        pluralGenitive = "тренировок",
        taskTerm = "Упражнения и цели",
        instructorTerm = "Тренер",
        instructorPlaceholder = "например, тренер Дмитрий",
        locationTerm = "Зал / Площадка",
        locationPlaceholder = "например, Фитнес-клуб / Зал 2",
        defaultDurationMinutes = 60,
        defaultBreakMinutes = 15,
        defaultUsePeriodNumbers = false,
        defaultLessonTypes = listOf("Тренировка", "Силовая", "Кардио", "Растяжка", "Спарринг", "ОФП", "Восстановление"),
        defaultCategory = ActivityCategory.HEALTH,
        defaultBellTimes = listOf(
            "08:00" to "09:00",
            "10:00" to "11:00",
            "12:00" to "13:00",
            "16:00" to "17:00",
            "18:00" to "19:30",
            "20:00" to "21:30"
        )
    ),
    UNIVERSAL(
        title = "Универсальный",
        subjectTerm = "Категория / Курс",
        subjectPlaceholder = "например, Курсы / Хобби",
        shortSubjectPlaceholder = "например, Курс",
        itemTerm = "Занятие",
        itemTermGenitive = "занятия",
        plural = "Расписание",
        pluralGenitive = "занятий",
        taskTerm = "Задачи",
        instructorTerm = "Контакт / Преподаватель",
        instructorPlaceholder = "например, Имя преподавателя",
        locationTerm = "Место / Кабинет",
        locationPlaceholder = "например, Онлайн / Каб. 12",
        defaultDurationMinutes = 60,
        defaultBreakMinutes = 10,
        defaultUsePeriodNumbers = true,
        defaultLessonTypes = listOf("Занятие", "Курс", "Встреча", "Практика", "Мастер-класс", "Дело"),
        defaultCategory = ActivityCategory.PERSONAL,
        defaultBellTimes = listOf(
            "09:00" to "10:00",
            "10:15" to "11:15",
            "11:30" to "12:30",
            "14:00" to "15:00",
            "15:15" to "16:15",
            "17:00" to "18:00",
            "18:30" to "20:00"
        )
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
