package com.weekflow.app.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weekflow.app.core.notifications.NotificationHelper
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.data.serialization.AiPromptBuilder
import com.weekflow.app.data.serialization.ValidationResult
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.AppThemeMode
import com.weekflow.app.domain.model.CardDensity
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.ScheduleCardStyle
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.Vacation
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.repository.UndoSnapshotInfo
import com.weekflow.app.domain.usecase.ImportExportScheduleUseCase
import com.weekflow.app.domain.usecase.ImportMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val presets: List<SchedulePreset> = listOf(SchedulePreset.DEFAULT_PRESET),
    val activePreset: SchedulePreset = SchedulePreset.DEFAULT_PRESET,
    val activities: List<Activity> = emptyList(),
    val rawAllActivities: List<Activity> = emptyList(),
    val rawAllSlots: List<Slot> = emptyList(),
    val rawAllTasks: List<Task> = emptyList(),
    val vacations: List<Vacation> = emptyList(),
    val isImportDialogVisible: Boolean = false,
    val importInputText: String = "",
    val importValidationResult: ValidationResult? = null,
    val selectedImportMode: ImportMode = ImportMode.REPLACE
)

sealed interface SettingsUiEffect {
    data class ShowToast(val message: String) : SettingsUiEffect
    data class ShareExportJson(val jsonText: String) : SettingsUiEffect
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val importExportScheduleUseCase: ImportExportScheduleUseCase,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private suspend fun rescheduleNotifications() {
        try {
            notificationScheduler.rescheduleAllNotifications()
        } catch (_: Exception) {}
    }

    private val _uiEffect = MutableSharedFlow<SettingsUiEffect>()
    val uiEffect: SharedFlow<SettingsUiEffect> = _uiEffect.asSharedFlow()

    private val _isImportDialogVisible = MutableStateFlow(false)
    private val _importInputText = MutableStateFlow("")
    private val _importValidationResult = MutableStateFlow<ValidationResult?>(null)
    private val _selectedImportMode = MutableStateFlow(ImportMode.REPLACE)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.userPreferences,
        scheduleRepository.getAllPresets(),
        scheduleRepository.getAllActivities(),
        scheduleRepository.getAllSlots(),
        scheduleRepository.getAllTasks(),
        scheduleRepository.getAllVacations(),
        _isImportDialogVisible,
        _importInputText,
        _importValidationResult,
        _selectedImportMode
    ) { params ->
        val preferences = params[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val presets = params[1] as List<SchedulePreset>
        @Suppress("UNCHECKED_CAST")
        val rawActivities = params[2] as List<Activity>
        @Suppress("UNCHECKED_CAST")
        val rawSlots = params[3] as List<Slot>
        @Suppress("UNCHECKED_CAST")
        val rawTasks = params[4] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val vacations = params[5] as List<Vacation>
        val isImportDialogVisible = params[6] as Boolean
        val importInputText = params[7] as String
        val validationResult = params[8] as ValidationResult?
        val importMode = params[9] as ImportMode

        val activePreset = presets.firstOrNull { it.id == preferences.activePresetId }
            ?: presets.firstOrNull()
            ?: SchedulePreset.DEFAULT_PRESET
        val activities = rawActivities.filter { it.presetId == activePreset.id }

        SettingsUiState(
            preferences = preferences,
            presets = presets,
            activePreset = activePreset,
            activities = activities,
            rawAllActivities = rawActivities,
            rawAllSlots = rawSlots,
            rawAllTasks = rawTasks,
            vacations = vacations,
            isImportDialogVisible = isImportDialogVisible,
            importInputText = importInputText,
            importValidationResult = validationResult,
            selectedImportMode = importMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun selectPreset(preset: SchedulePreset) {
        viewModelScope.launch {
            preferencesRepository.updateActivePresetId(preset.id)
        }
    }

    fun savePreset(preset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.upsertPreset(preset)
        }
    }

    fun duplicatePreset(sourcePresetId: String, newPreset: SchedulePreset) {
        viewModelScope.launch {
            scheduleRepository.duplicatePreset(sourcePresetId, newPreset)
            preferencesRepository.updateActivePresetId(newPreset.id)
        }
    }

    fun deletePreset(presetId: String) {
        if (presetId == SchedulePreset.DEFAULT_ID) return
        viewModelScope.launch {
            scheduleRepository.deletePreset(presetId)
            if (uiState.value.activePreset.id == presetId) {
                preferencesRepository.updateActivePresetId(SchedulePreset.DEFAULT_ID)
            }
        }
    }

    fun toggleShowAllPresetsTasks(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowAllPresetsTasks(show)
        }
    }

    fun onProfileChanged(profile: AppProfile) {
        viewModelScope.launch {
            preferencesRepository.updateAppProfile(profile)
        }
    }

    fun onThemeChanged(themeMode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(themeMode)
        }
    }

    fun onToggleTwoWeekCycle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateTwoWeekCycleEnabled(enabled)
        }
    }

    fun onSemesterStartDateChanged(dateIso: String) {
        viewModelScope.launch {
            preferencesRepository.updateSemesterStartDate(dateIso)
        }
    }

    fun onToggleInvertWeekCycle() {
        viewModelScope.launch {
            preferencesRepository.toggleInvertWeekCycle()
        }
    }

    fun onToggleRussianHolidays(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowRussianHolidays(show)
        }
    }

    fun onToggleSkipHolidaysInHomework(skip: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSkipHolidaysInSmartHomework(skip)
        }
    }

    fun onToggleSaturdayWeekend(isWeekend: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSaturdayWeekend(isWeekend)
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteActivity(activityId)
        }
    }

    fun addVacation(title: String, startDateIso: String, endDateIso: String) {
        viewModelScope.launch {
            scheduleRepository.upsertVacation(
                Vacation(
                    title = title,
                    startDateIso = startDateIso,
                    endDateIso = endDateIso
                )
            )
        }
    }

    fun deleteVacation(vacationId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteVacation(vacationId)
        }
    }

    // AI & JSON Features
    fun copyAiPrompt(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WeekFlow AI Prompt", AiPromptBuilder.buildSystemPrompt())
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _uiEffect.emit(SettingsUiEffect.ShowToast("Системный промпт скопирован в буфер обмена!"))
        }
    }

    fun openImportDialog(initialText: String = "") {
        _importInputText.value = initialText
        if (initialText.isNotBlank()) {
            _importValidationResult.value = importExportScheduleUseCase.validateInputJson(initialText)
        } else {
            _importValidationResult.value = null
        }
        _isImportDialogVisible.value = true
    }

    fun closeImportDialog() {
        _isImportDialogVisible.value = false
        _importInputText.value = ""
        _importValidationResult.value = null
    }

    fun onImportInputChanged(text: String) {
        _importInputText.value = text
        if (text.isNotBlank()) {
            _importValidationResult.value = importExportScheduleUseCase.validateInputJson(text)
        } else {
            _importValidationResult.value = null
        }
    }

    fun setImportMode(mode: ImportMode) {
        _selectedImportMode.value = mode
    }

    fun executeImport() {
        val validation = _importValidationResult.value
        if (validation is ValidationResult.Success) {
            val targetPresetId = uiState.value.activePreset.id
            val presetName = uiState.value.activePreset.name
            viewModelScope.launch {
                importExportScheduleUseCase.executeImport(
                    payload = validation.payload,
                    mode = _selectedImportMode.value,
                    presetId = targetPresetId
                )
                rescheduleNotifications()
                closeImportDialog()
                _uiEffect.emit(SettingsUiEffect.ShowToast("Импортировано в пресет «$presetName»!"))
            }
        }
    }

    val lastUndoSnapshot: StateFlow<UndoSnapshotInfo?> = scheduleRepository.lastUndoSnapshot.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun restoreLastUndo() {
        viewModelScope.launch {
            val restored = scheduleRepository.restoreLastUndoSnapshot()
            if (restored) {
                rescheduleNotifications()
                _uiEffect.emit(SettingsUiEffect.ShowToast("Предыдущее состояние восстановлено!"))
            }
        }
    }

    fun dismissUndo() {
        viewModelScope.launch {
            scheduleRepository.dismissUndoSnapshot()
        }
    }

    fun exportSchedule() {
        val targetPresetId = uiState.value.activePreset.id
        viewModelScope.launch {
            val jsonText = importExportScheduleUseCase.exportCleanJson(targetPresetId)
            _uiEffect.emit(SettingsUiEffect.ShareExportJson(jsonText))
        }
    }

    // Appearance & Card Customization
    fun onCardStyleChanged(style: ScheduleCardStyle) {
        viewModelScope.launch {
            preferencesRepository.updateCardStyle(style)
        }
    }

    fun onCardDensityChanged(density: CardDensity) {
        viewModelScope.launch {
            preferencesRepository.updateCardDensity(density)
        }
    }

    fun onCornerRadiusChanged(radiusDp: Int) {
        viewModelScope.launch {
            preferencesRepository.updateCornerRadiusDp(radiusDp)
        }
    }

    fun onToggleShowBreaks(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowBreaks(show)
        }
    }

    fun onToggleShowProgressBar(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowProgressBar(show)
        }
    }

    // Notifications
    fun onToggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
            rescheduleNotifications()
        }
    }

    fun onNotifyMinutesBeforeChanged(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateNotifyMinutesBefore(minutes)
            rescheduleNotifications()
        }
    }

    fun onToggleEveningSummary(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateEveningSummaryEnabled(enabled)
            rescheduleNotifications()
        }
    }

    fun onEveningSummaryTimeChanged(time: String) {
        viewModelScope.launch {
            preferencesRepository.updateEveningSummaryTime(time)
            rescheduleNotifications()
        }
    }

    fun onToggleTaskReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateTaskRemindersEnabled(enabled)
            rescheduleNotifications()
        }
    }

    fun sendTestNotification() {
        notificationHelper.showTestNotification()
        viewModelScope.launch {
            _uiEffect.emit(SettingsUiEffect.ShowToast("Тестовое уведомление отправлено!"))
        }
    }
}
