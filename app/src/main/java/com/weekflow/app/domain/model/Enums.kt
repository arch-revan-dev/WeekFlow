package com.weekflow.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileStepTemplate(
    val chipLabel: String,
    val title: String,
    val sets: Int? = null,
    val reps: String? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int? = null,
    val weightOrNote: String? = null
)

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
    val bySubjectTabLabel: String,
    val shortItemBadge: String,
    val unnumberedLabel: String,
    val instructorTerm: String,
    val instructorPlaceholder: String,
    val locationTerm: String,
    val locationPlaceholder: String,
    val defaultDurationMinutes: Int,
    val defaultBreakMinutes: Int,
    val defaultUsePeriodNumbers: Boolean,
    val defaultLessonTypes: List<String>,
    val defaultCategory: ActivityCategory,
    val defaultBellTimes: List<Pair<String, String>>,
    val sessionSectionTitle: String,
    val sessionItemTerm: String,
    val startSessionLabel: String,
    val stepPlaceholder: String,
    val weightOrNoteLabel: String,
    val weightOrNotePlaceholder: String,
    val defaultStepTemplates: List<ProfileStepTemplate>
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
        bySubjectTabLabel = "По предметам",
        shortItemBadge = "урок",
        unnumberedLabel = "Вне уроков",
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
        ),
        sessionSectionTitle = "📚 План урока / Тренажёр заданий",
        sessionItemTerm = "Задание / Билет",
        startSessionLabel = "▶ Тренажёр",
        stepPlaceholder = "например, Билет №5 / Номера 810–814",
        weightOrNoteLabel = "Формула / Подсказка",
        weightOrNotePlaceholder = "например, Без калькулятора / Стр. 42",
        defaultStepTemplates = listOf(
            ProfileStepTemplate("+ Параграф (15м)", "Чтение и разбор параграфа", durationSeconds = 15 * 60),
            ProfileStepTemplate("+ Задачи (20м)", "Решение номеров по теме", sets = 3, reps = "2 номера", durationSeconds = 20 * 60, restSeconds = 120),
            ProfileStepTemplate("+ Правила (10м)", "Повторение формул и правил", sets = 2, reps = "1 круг", durationSeconds = 10 * 60, restSeconds = 60),
            ProfileStepTemplate("+ Тренировка билета", "Разбор экзаменационного билета", durationSeconds = 15 * 60, weightOrNote = "Тезисы + формулы")
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
        bySubjectTabLabel = "Дисциплины",
        shortItemBadge = "пара",
        unnumberedLabel = "Вне сетки пар",
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
        ),
        sessionSectionTitle = "🎓 План пары / Лабораторные этапы",
        sessionItemTerm = "Этап / Вопрос",
        startSessionLabel = "▶ Практика",
        stepPlaceholder = "например, Допуск к лабе / Серия замеров",
        weightOrNoteLabel = "Баллы БРС / Критерий",
        weightOrNotePlaceholder = "например, 5 баллов / Отчёт в PDF",
        defaultStepTemplates = listOf(
            ProfileStepTemplate("+ Допуск (15м)", "Теория и допуск к лабораторной", durationSeconds = 15 * 60, weightOrNote = "2 балла"),
            ProfileStepTemplate("+ Замеры (3 серии)", "Снятие экспериментальных данных", sets = 3, reps = "5 замеров", durationSeconds = 30 * 60, restSeconds = 60),
            ProfileStepTemplate("+ Расчёт и графики", "Обработка результатов и погрешностей", durationSeconds = 25 * 60),
            ProfileStepTemplate("+ Защита отчёта", "Сдача и защита работы", durationSeconds = 15 * 60, weightOrNote = "8 баллов")
        )
    ),
    WORK(
        title = "Работа и проекты",
        subjectTerm = "Проект / Направление",
        subjectPlaceholder = "например, Разработка / Маркетинг",
        shortSubjectPlaceholder = "например, Dev",
        itemTerm = "Встреча",
        itemTermGenitive = "встречи",
        plural = "Рабочие блоки",
        pluralGenitive = "встреч и блоков",
        taskTerm = "Задачи",
        bySubjectTabLabel = "По проектам",
        shortItemBadge = "",
        unnumberedLabel = "",
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
        ),
        sessionSectionTitle = "💼 Повестка встречи / Таймбоксы",
        sessionItemTerm = "Пункт повестки",
        startSessionLabel = "▶ Повестка",
        stepPlaceholder = "например, Синк по релизам / Демо",
        weightOrNoteLabel = "Докладчик / Итог",
        weightOrNotePlaceholder = "например, Тимлид / Решение по API",
        defaultStepTemplates = listOf(
            ProfileStepTemplate("+ Дейли-синк (15м)", "Статус команды и блокеры", durationSeconds = 15 * 60, weightOrNote = "По 2 мин на участника"),
            ProfileStepTemplate("+ Deep Work (50м)", "Фокус-спринт без отвлечений", sets = 2, reps = "1 спринт", durationSeconds = 50 * 60, restSeconds = 10 * 60),
            ProfileStepTemplate("+ Код-ревью (25м)", "Проверка PR и архитектуры", durationSeconds = 25 * 60),
            ProfileStepTemplate("+ Итоги и Action Items", "Фиксация договорённостей и дедлайнов", durationSeconds = 10 * 60)
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
        taskTerm = "Упражнения",
        bySubjectTabLabel = "По видам",
        shortItemBadge = "",
        unnumberedLabel = "",
        instructorTerm = "Тренер",
        instructorPlaceholder = "например, тренер Дмитрий",
        locationTerm = "Зал / Площадка",
        locationPlaceholder = "например, Фитнес-клуб / Зал 2",
        defaultDurationMinutes = 60,
        defaultBreakMinutes = 15,
        defaultUsePeriodNumbers = false,
        defaultLessonTypes = listOf("Тренировка", "Силовая", "Кардио", "Растяжка", "Разминка", "Микро-пауза", "Укрепление", "Восстановление"),
        defaultCategory = ActivityCategory.HEALTH,
        defaultBellTimes = listOf(
            "08:00" to "09:00",
            "10:00" to "11:00",
            "12:00" to "13:00",
            "16:00" to "17:00",
            "18:00" to "19:30",
            "20:00" to "21:30"
        ),
        sessionSectionTitle = "🏋️ Программа упражнений (подходы, повторы, таймер)",
        sessionItemTerm = "Упражнение",
        startSessionLabel = "▶ Тренировка",
        stepPlaceholder = "например, Жим лёжа / Планка / Сведение лопаток",
        weightOrNoteLabel = "Вес / Инвентарь / Техника",
        weightOrNotePlaceholder = "например, 60 кг / Гантели / Плавный темп",
        defaultStepTemplates = listOf(
            ProfileStepTemplate("+ Разминка (7м)", "Суставная разминка и разогрев", durationSeconds = 7 * 60, restSeconds = 30, weightOrNote = "Сверху вниз"),
            ProfileStepTemplate("+ Силовой сет (3×12)", "Базовое упражнение", sets = 3, reps = "12 повт", restSeconds = 90, weightOrNote = "Рабочий вес"),
            ProfileStepTemplate("+ Статика / Планка (3×60с)", "Удержание статики", sets = 3, reps = "60 сек", durationSeconds = 60, restSeconds = 45, weightOrNote = "Свой вес"),
            ProfileStepTemplate("+ Табата HIIT (8×20с)", "Интервальный блок высокой интенсивности", sets = 8, reps = "20 сек", durationSeconds = 20, restSeconds = 10, weightOrNote = "Макс темп"),
            ProfileStepTemplate("+ Растяжка (5м)", "Заминка и МФР", durationSeconds = 5 * 60, weightOrNote = "Глубокое дыхание")
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
        bySubjectTabLabel = "По курсам",
        shortItemBadge = "зан.",
        unnumberedLabel = "Без номера",
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
        ),
        sessionSectionTitle = "⚡ Этапы занятия и таймер",
        sessionItemTerm = "Этап / Упражнение",
        startSessionLabel = "▶ Старт",
        stepPlaceholder = "например, Разминка / Практика / Повторение",
        weightOrNoteLabel = "Заметка / Параметр",
        weightOrNotePlaceholder = "например, Ссылка / Инвентарь",
        defaultStepTemplates = listOf(
            ProfileStepTemplate("+ Фокус-блок (25м)", "Помодоро-сессия", sets = 2, reps = "25 мин", durationSeconds = 25 * 60, restSeconds = 5 * 60),
            ProfileStepTemplate("+ Практика (3×10)", "Отработка упражнения", sets = 3, reps = "10 повт", restSeconds = 60),
            ProfileStepTemplate("+ Разбор / Чтение (20м)", "Изучение материала", durationSeconds = 20 * 60)
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
