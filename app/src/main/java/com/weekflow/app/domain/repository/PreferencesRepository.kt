package com.weekflow.app.domain.repository

import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.AppThemeMode
import com.weekflow.app.domain.model.CardDensity
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.ScheduleCardStyle
import com.weekflow.app.domain.model.ScheduleViewMode
import com.weekflow.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun updateAppProfile(profile: AppProfile)
    suspend fun updateThemeMode(themeMode: AppThemeMode)
    suspend fun updateTwoWeekCycleEnabled(enabled: Boolean)
    suspend fun updateSemesterStartDate(dateIso: String)
    suspend fun toggleInvertWeekCycle()
    suspend fun updateShowRussianHolidays(show: Boolean)
    suspend fun updateSkipHolidaysInSmartHomework(skip: Boolean)
    suspend fun setFirstLaunchCompleted(completed: Boolean)

    suspend fun updateCardStyle(style: ScheduleCardStyle)
    suspend fun updateCardDensity(density: CardDensity)
    suspend fun updateCornerRadiusDp(radius: Int)
    suspend fun updateShowBreaks(show: Boolean)
    suspend fun updateShowProgressBar(show: Boolean)
    suspend fun updateScheduleViewMode(mode: ScheduleViewMode)

    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateNotifyMinutesBefore(minutes: Int)
    suspend fun updateEveningSummaryEnabled(enabled: Boolean)
    suspend fun updateEveningSummaryTime(time: String)
    suspend fun updateTaskRemindersEnabled(enabled: Boolean)
    suspend fun updateTaskRemindersTime(time: String)
    suspend fun updateSaturdayWeekend(isWeekend: Boolean)

    suspend fun updateActivePresetId(presetId: String)
    suspend fun updateShowAllPresetsTasks(show: Boolean)
    suspend fun recordCompletedFocusSession(minutes: Int)

    suspend fun updateRegularReminders(reminders: List<RegularReminder>)
}

