package com.weekflow.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.AppThemeMode
import com.weekflow.app.domain.model.CardDensity
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.ScheduleCardStyle
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.ScheduleViewMode
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.widget.WeekFlowWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val reminderJsonParser = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class UserPreferencesRepository(context: Context) : PreferencesRepository {

    private val appContext: Context = context.applicationContext

    private suspend fun notifyWidget() {
        try {
            WeekFlowWidgetUpdater.forceUpdateAll(appContext)
        } catch (_: Exception) {}
    }

    private object PreferencesKeys {
        val APP_PROFILE = stringPreferencesKey("app_profile")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_TWO_WEEK_CYCLE_ENABLED = booleanPreferencesKey("is_two_week_cycle_enabled")
        val SEMESTER_START_DATE_ISO = stringPreferencesKey("semester_start_date_iso")
        val INVERT_WEEK_CYCLE = booleanPreferencesKey("invert_week_cycle")
        val SHOW_RUSSIAN_HOLIDAYS = booleanPreferencesKey("show_russian_holidays")
        val SKIP_HOLIDAYS_IN_SMART_HOMEWORK = booleanPreferencesKey("skip_holidays_in_smart_homework")
        val IS_FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("is_first_launch_completed")

        val CARD_STYLE = stringPreferencesKey("card_style")
        val CARD_DENSITY = stringPreferencesKey("card_density")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val SHOW_BREAKS = booleanPreferencesKey("show_breaks")
        val SHOW_PROGRESS_BAR = booleanPreferencesKey("show_progress_bar")
        val SCHEDULE_VIEW_MODE = stringPreferencesKey("schedule_view_mode")

        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_MINUTES_BEFORE = intPreferencesKey("notify_minutes_before")
        val EVENING_SUMMARY_ENABLED = booleanPreferencesKey("evening_summary_enabled")
        val EVENING_SUMMARY_TIME = stringPreferencesKey("evening_summary_time")
        val TASK_REMINDERS_ENABLED = booleanPreferencesKey("task_reminders_enabled")
        val TASK_REMINDERS_TIME = stringPreferencesKey("task_reminders_time")
        val IS_SATURDAY_WEEKEND = booleanPreferencesKey("is_saturday_weekend")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        val SHOW_ALL_PRESETS_TASKS = booleanPreferencesKey("show_all_presets_tasks")
        val FOCUS_SESSIONS_COMPLETED = intPreferencesKey("focus_sessions_completed")
        val FOCUS_MINUTES_TOTAL = intPreferencesKey("focus_minutes_total")
        val REGULAR_REMINDERS_JSON = stringPreferencesKey("regular_reminders_json")
    }

    override val userPreferences: Flow<UserPreferences> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val profileStr = preferences[PreferencesKeys.APP_PROFILE] ?: AppProfile.STUDY.name
            val profile = try { AppProfile.valueOf(profileStr) } catch (_: Exception) { AppProfile.STUDY }

            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val themeMode = try { AppThemeMode.valueOf(themeModeStr) } catch (_: Exception) { AppThemeMode.SYSTEM }

            val isTwoWeekCycleEnabled = preferences[PreferencesKeys.IS_TWO_WEEK_CYCLE_ENABLED] ?: false
            val semesterStartDateIso = preferences[PreferencesKeys.SEMESTER_START_DATE_ISO] ?: "2026-09-01"
            val invertWeekCycle = preferences[PreferencesKeys.INVERT_WEEK_CYCLE] ?: false
            val showRussianHolidays = preferences[PreferencesKeys.SHOW_RUSSIAN_HOLIDAYS] ?: true
            val skipHolidaysInSmartHomework = preferences[PreferencesKeys.SKIP_HOLIDAYS_IN_SMART_HOMEWORK] ?: true
            val isFirstLaunchCompleted = preferences[PreferencesKeys.IS_FIRST_LAUNCH_COMPLETED] ?: false

            val cardStyleStr = preferences[PreferencesKeys.CARD_STYLE] ?: ScheduleCardStyle.OUTLINED.name
            val cardStyle = try { ScheduleCardStyle.valueOf(cardStyleStr) } catch (_: Exception) { ScheduleCardStyle.OUTLINED }

            val cardDensityStr = preferences[PreferencesKeys.CARD_DENSITY] ?: CardDensity.STANDARD.name
            val cardDensity = try { CardDensity.valueOf(cardDensityStr) } catch (_: Exception) { CardDensity.STANDARD }

            val cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: 14
            val showBreaks = preferences[PreferencesKeys.SHOW_BREAKS] ?: true
            val showProgressBar = preferences[PreferencesKeys.SHOW_PROGRESS_BAR] ?: true

            val scheduleViewModeStr = preferences[PreferencesKeys.SCHEDULE_VIEW_MODE] ?: ScheduleViewMode.DAY_LIST.name
            val scheduleViewMode = try { ScheduleViewMode.valueOf(scheduleViewModeStr) } catch (_: Exception) { ScheduleViewMode.DAY_LIST }

            val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            val notifyMinutesBefore = preferences[PreferencesKeys.NOTIFY_MINUTES_BEFORE] ?: 10
            val eveningSummaryEnabled = preferences[PreferencesKeys.EVENING_SUMMARY_ENABLED] ?: true
            val eveningSummaryTime = preferences[PreferencesKeys.EVENING_SUMMARY_TIME] ?: "20:00"
            val taskRemindersEnabled = preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] ?: true
            val taskRemindersTime = preferences[PreferencesKeys.TASK_REMINDERS_TIME] ?: "08:00"
            val isSaturdayWeekend = preferences[PreferencesKeys.IS_SATURDAY_WEEKEND] ?: false
            val activePresetId = (preferences[PreferencesKeys.ACTIVE_PRESET_ID] ?: SchedulePreset.DEFAULT_ID)
                .ifBlank { SchedulePreset.DEFAULT_ID }
            val showAllPresetsTasks = preferences[PreferencesKeys.SHOW_ALL_PRESETS_TASKS] ?: false
            val focusSessionsCompleted = preferences[PreferencesKeys.FOCUS_SESSIONS_COMPLETED] ?: 0
            val focusMinutesTotal = preferences[PreferencesKeys.FOCUS_MINUTES_TOTAL] ?: 0
            val regularReminders = preferences[PreferencesKeys.REGULAR_REMINDERS_JSON]?.let { raw ->
                try { reminderJsonParser.decodeFromString<List<RegularReminder>>(raw) } catch (_: Exception) { null }
            } ?: emptyList()

            UserPreferences(
                appProfile = profile,
                themeMode = themeMode,
                isTwoWeekCycleEnabled = isTwoWeekCycleEnabled,
                semesterStartDateIso = semesterStartDateIso,
                invertWeekCycle = invertWeekCycle,
                showRussianHolidays = showRussianHolidays,
                skipHolidaysInSmartHomework = skipHolidaysInSmartHomework,
                isFirstLaunchCompleted = isFirstLaunchCompleted,
                cardStyle = cardStyle,
                cardDensity = cardDensity,
                cornerRadiusDp = cornerRadiusDp,
                showBreaks = showBreaks,
                showProgressBar = showProgressBar,
                scheduleViewMode = scheduleViewMode,
                notificationsEnabled = notificationsEnabled,
                notifyMinutesBefore = notifyMinutesBefore,
                eveningSummaryEnabled = eveningSummaryEnabled,
                eveningSummaryTime = eveningSummaryTime,
                taskRemindersEnabled = taskRemindersEnabled,
                taskRemindersTime = taskRemindersTime,
                isSaturdayWeekend = isSaturdayWeekend,
                activePresetId = activePresetId,
                showAllPresetsTasks = showAllPresetsTasks,
                focusSessionsCompleted = focusSessionsCompleted,
                focusMinutesTotal = focusMinutesTotal,
                regularReminders = regularReminders
            )
        }

    override suspend fun updateAppProfile(profile: AppProfile) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_PROFILE] = profile.name
        }
    }

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun updateTwoWeekCycleEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_TWO_WEEK_CYCLE_ENABLED] = enabled
        }
        notifyWidget()
    }

    override suspend fun updateSemesterStartDate(dateIso: String) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEMESTER_START_DATE_ISO] = dateIso
        }
        notifyWidget()
    }

    override suspend fun toggleInvertWeekCycle() {
        appContext.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.INVERT_WEEK_CYCLE] ?: false
            preferences[PreferencesKeys.INVERT_WEEK_CYCLE] = !current
        }
        notifyWidget()
    }

    override suspend fun updateShowRussianHolidays(show: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_RUSSIAN_HOLIDAYS] = show
        }
        notifyWidget()
    }

    override suspend fun updateSkipHolidaysInSmartHomework(skip: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIP_HOLIDAYS_IN_SMART_HOMEWORK] = skip
        }
        notifyWidget()
    }

    override suspend fun setFirstLaunchCompleted(completed: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH_COMPLETED] = completed
        }
    }

    override suspend fun updateCardStyle(style: ScheduleCardStyle) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CARD_STYLE] = style.name
        }
    }

    override suspend fun updateCardDensity(density: CardDensity) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CARD_DENSITY] = density.name
        }
    }

    override suspend fun updateCornerRadiusDp(radius: Int) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CORNER_RADIUS_DP] = radius
        }
    }

    override suspend fun updateShowBreaks(show: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BREAKS] = show
        }
    }

    override suspend fun updateShowProgressBar(show: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PROGRESS_BAR] = show
        }
    }

    override suspend fun updateScheduleViewMode(mode: ScheduleViewMode) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULE_VIEW_MODE] = mode.name
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun updateNotifyMinutesBefore(minutes: Int) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFY_MINUTES_BEFORE] = minutes
        }
    }

    override suspend fun updateEveningSummaryEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENING_SUMMARY_ENABLED] = enabled
        }
    }

    override suspend fun updateEveningSummaryTime(time: String) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENING_SUMMARY_TIME] = time
        }
    }

    override suspend fun updateTaskRemindersEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun updateTaskRemindersTime(time: String) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_REMINDERS_TIME] = time
        }
    }

    override suspend fun updateSaturdayWeekend(isWeekend: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SATURDAY_WEEKEND] = isWeekend
        }
        notifyWidget()
    }

    override suspend fun updateActivePresetId(presetId: String) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PRESET_ID] = presetId.ifBlank { SchedulePreset.DEFAULT_ID }
        }
        notifyWidget()
    }

    override suspend fun updateShowAllPresetsTasks(show: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ALL_PRESETS_TASKS] = show
        }
        notifyWidget()
    }

    override suspend fun recordCompletedFocusSession(minutes: Int) {
        appContext.dataStore.edit { preferences ->
            val currentSessions = preferences[PreferencesKeys.FOCUS_SESSIONS_COMPLETED] ?: 0
            val currentMinutes = preferences[PreferencesKeys.FOCUS_MINUTES_TOTAL] ?: 0
            preferences[PreferencesKeys.FOCUS_SESSIONS_COMPLETED] = currentSessions + 1
            preferences[PreferencesKeys.FOCUS_MINUTES_TOTAL] = currentMinutes + minutes.coerceAtLeast(1)
        }
    }

    override suspend fun updateRegularReminders(reminders: List<RegularReminder>) {
        appContext.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REGULAR_REMINDERS_JSON] =
                try { reminderJsonParser.encodeToString(reminders) } catch (_: Exception) { "[]" }
        }
    }
}
