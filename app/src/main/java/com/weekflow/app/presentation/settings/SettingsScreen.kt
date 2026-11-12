package com.weekflow.app.presentation.settings

import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.IosCalendarDatePickerDialog
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.components.IosSegmentedControl
import com.weekflow.app.core.designsystem.components.IosWheelTimePickerDialog
import com.weekflow.app.core.designsystem.components.LessonSlotCard
import com.weekflow.app.core.designsystem.components.RegularRemindersSheet
import com.weekflow.app.core.designsystem.components.UndoIncidentBanner
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.data.serialization.ValidationResult
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.AppThemeMode
import com.weekflow.app.domain.model.CardDensity
import com.weekflow.app.domain.model.ScheduleCardStyle
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.SlotWithDetails
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.usecase.ImportMode
import com.weekflow.app.presentation.editors.PresetManagerSheet
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAddNewActivity: () -> Unit,
    onEditActivity: (activityId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoSnapshot by viewModel.lastUndoSnapshot.collectAsState()
    val iosColors = LocalIosColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAddVacationDialog by remember { mutableStateOf(false) }
    var showSemesterDateDialog by remember { mutableStateOf(false) }
    var showEveningTimeDialog by remember { mutableStateOf(false) }
    var showMorningTaskTimeDialog by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showRhythmSheet by remember { mutableStateOf(false) }

    if (showRhythmSheet) {
        RegularRemindersSheet(
            reminders = uiState.preferences.regularReminders,
            onUpdateReminders = { viewModel.updateRegularReminders(it) },
            onDismiss = { showRhythmSheet = false }
        )
    }

    if (showPresetSheet) {
        PresetManagerSheet(
            presets = uiState.presets,
            activePresetId = uiState.activePreset.id,
            allActivities = uiState.rawAllActivities,
            allSlots = uiState.rawAllSlots,
            allTasks = uiState.rawAllTasks,
            showAllPresetsTasks = uiState.preferences.showAllPresetsTasks,
            onSelectPreset = { preset ->
                viewModel.selectPreset(preset)
                showPresetSheet = false
            },
            onSavePreset = { preset -> viewModel.savePreset(preset) },
            onDuplicatePreset = { srcId, newPreset -> viewModel.duplicatePreset(srcId, newPreset) },
            onDeletePreset = { presetId -> viewModel.deletePreset(presetId) },
            onToggleShowAllPresetsTasks = { show -> viewModel.toggleShowAllPresetsTasks(show) },
            onDismiss = { showPresetSheet = false }
        )
    }

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var canScheduleExact by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Effects collector for Toasts and Share Sheet
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is SettingsUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsUiEffect.ShareExportJson -> {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.jsonText)
                        type = "application/json"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Экспорт расписания (JSON)")
                    context.startActivity(shareIntent)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosColors.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets,
                title = {
                    Text(
                        text = "Настройки",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosColors.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (undoSnapshot != null) {
                item(key = "undo_banner") {
                    UndoIncidentBanner(
                        snapshot = undoSnapshot!!,
                        onUndoClick = { viewModel.restoreLastUndo() },
                        onDismissClick = { viewModel.dismissUndo() }
                    )
                }
            }
            // 1. Персонализация и Профиль
            item(key = "section_personalization") {
                SettingsSection(title = "ПЕРСОНАЛИЗАЦИЯ И ПРОФИЛЬ") {
                    SettingsRow(
                        label = "Активный пресет",
                        value = "${uiState.activePreset.iconEmoji} ${uiState.activePreset.name}",
                        onClick = { showPresetSheet = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "Профиль пресета",
                        value = uiState.activePreset.appProfile.title,
                        onClick = { showProfileDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "Тема оформления",
                        value = uiState.preferences.themeMode.displayName,
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // 2. Оформление и кастомизация карточек (Weeklie Style)
            item(key = "section_appearance") {
                val previewDetails = remember {
                    SlotWithDetails(
                        slot = Slot(
                            id = "preview_slot",
                            activityTypeId = "preview_act",
                            dayOfWeek = 1,
                            startTime = "09:00",
                            endTime = "10:30",
                            customLocation = "Ауд. 312",
                            customInstructor = "Доц. Смирнов А.В.",
                            customType = "Семинар",
                            periodOrder = 1
                        ),
                        activity = Activity(
                            id = "preview_act",
                            name = "Информационные технологии",
                            shortName = "Информатика",
                            colorHex = "#007AFF"
                        ),
                        tasks = listOf(
                            Task(
                                id = "preview_task",
                                title = "Лабораторная работа №3",
                                dueDateIso = LocalDate.now().toString(),
                                priority = TaskPriority.HIGH
                            )
                        )
                    )
                }

                SettingsSection(title = "ОФОРМЛЕНИЕ И КАРТОЧКИ") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Предпросмотр карточки",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textSecondary
                        )

                        // Live Card Preview
                        LessonSlotCard(
                            details = previewDetails,
                            cardStyle = uiState.preferences.cardStyle,
                            cardDensity = uiState.preferences.cardDensity,
                            cornerRadiusDp = uiState.preferences.cornerRadiusDp,
                            showProgressBar = uiState.preferences.showProgressBar,
                            isTwoWeekCycleEnabled = false,
                            onCardClick = {},
                            onAddTaskClick = {},
                            onTaskToggle = {}
                        )

                        // 1. Card Style Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Стиль карточки",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textPrimary
                            )
                            val styleOptions = listOf(
                                "Обводка" to ScheduleCardStyle.OUTLINED,
                                "Полоса" to ScheduleCardStyle.LEFT_ACCENT,
                                "Заливка" to ScheduleCardStyle.SOFT_TINT,
                                "Минимал" to ScheduleCardStyle.MINIMAL
                            )
                            val selectedStyleIndex = styleOptions.indexOfFirst { it.second == uiState.preferences.cardStyle }.coerceAtLeast(0)
                            IosSegmentedControl(
                                items = styleOptions.map { it.first },
                                selectedIndex = selectedStyleIndex,
                                onItemSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onCardStyleChanged(styleOptions[index].second)
                                }
                            )
                        }

                        // 2. Card Density Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Плотность сетки",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textPrimary
                            )
                            val densityOptions = listOf(
                                "Компакт" to CardDensity.COMPACT,
                                "Стандарт" to CardDensity.STANDARD,
                                "Простор" to CardDensity.COMFORTABLE
                            )
                            val selectedDensityIndex = densityOptions.indexOfFirst { it.second == uiState.preferences.cardDensity }.coerceAtLeast(0)
                            IosSegmentedControl(
                                items = densityOptions.map { it.first },
                                selectedIndex = selectedDensityIndex,
                                onItemSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onCardDensityChanged(densityOptions[index].second)
                                }
                            )
                        }

                        // 3. Corner Radius Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Скругление углов",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textPrimary
                            )
                            val radiusOptions = listOf(8, 14, 20)
                            val selectedRadiusIndex = radiusOptions.indexOf(uiState.preferences.cornerRadiusDp).let { if (it >= 0) it else 1 }
                            IosSegmentedControl(
                                items = listOf("8 dp", "14 dp", "20 dp"),
                                selectedIndex = selectedRadiusIndex,
                                onItemSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onCornerRadiusChanged(radiusOptions[index])
                                }
                            )
                        }
                    }

                    SettingsDivider()
                    SettingsSwitchRow(
                        label = "Показывать перерывы",
                        checked = uiState.preferences.showBreaks,
                        onCheckedChange = { viewModel.onToggleShowBreaks(it) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = "Полоса прогресса",
                        checked = uiState.preferences.showProgressBar,
                        onCheckedChange = { viewModel.onToggleShowProgressBar(it) }
                    )
                }
            }

            // 3. Уведомления и напоминания
            item(key = "section_notifications") {
                SettingsSection(title = "УВЕДОМЛЕНИЯ И НАПОМИНАНИЯ") {
                    SettingsSwitchRow(
                        label = "Уведомления",
                        checked = uiState.preferences.notificationsEnabled,
                        onCheckedChange = { viewModel.onToggleNotifications(it) }
                    )

                    if (uiState.preferences.notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                            SettingsDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IosOrange.copy(alpha = 0.09f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = IosOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Нет разрешения на точные будильники",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosColors.textPrimary
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Text("Разрешить", fontWeight = FontWeight.SemiBold, color = IosBlue)
                                }
                            }
                        }

                        SettingsDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Напоминать до начала",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textPrimary
                            )
                            val minutesOptions = listOf(5, 10, 15, 30)
                            val selectedMinIndex = minutesOptions.indexOf(uiState.preferences.notifyMinutesBefore).let { if (it >= 0) it else 1 }
                            IosSegmentedControl(
                                items = listOf("5 мин", "10 мин", "15 мин", "30 мин"),
                                selectedIndex = selectedMinIndex,
                                onItemSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onNotifyMinutesBeforeChanged(minutesOptions[index])
                                }
                            )
                        }

                        SettingsDivider()
                        SettingsSwitchRow(
                            label = "Вечерняя сводка",
                            checked = uiState.preferences.eveningSummaryEnabled,
                            onCheckedChange = { viewModel.onToggleEveningSummary(it) },
                            trailingBadge = if (uiState.preferences.eveningSummaryEnabled) uiState.preferences.eveningSummaryTime else null,
                            onTrailingBadgeClick = { showEveningTimeDialog = true }
                        )

                        SettingsDivider()
                        SettingsSwitchRow(
                            label = "Утренние задачи",
                            checked = uiState.preferences.taskRemindersEnabled,
                            onCheckedChange = { viewModel.onToggleTaskReminders(it) },
                            trailingBadge = if (uiState.preferences.taskRemindersEnabled) uiState.preferences.taskRemindersTime else null,
                            onTrailingBadgeClick = { showMorningTaskTimeDialog = true }
                        )

                        SettingsDivider()
                        val activeReminderCount = uiState.preferences.regularReminders.count { it.isEnabled }
                        SettingsButtonRow(
                            icon = Icons.Rounded.Notifications,
                            label = "Регулярные напоминания",
                            sublabel = if (activeReminderCount > 0) "Активно: $activeReminderCount" else null,
                            iconTint = IosBlue,
                            onClick = { showRhythmSheet = true }
                        )
                    }
                }
            }

            // 4. Настройка недельного цикла
            item(key = "section_week_cycle") {
                SettingsSection(title = "НЕДЕЛЬНЫЙ ЦИКЛ") {
                    SettingsSwitchRow(
                        label = "Суббота — выходной",
                        checked = uiState.preferences.isSaturdayWeekend,
                        onCheckedChange = { viewModel.onToggleSaturdayWeekend(it) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = "Двухнедельное расписание",
                        checked = uiState.preferences.isTwoWeekCycleEnabled,
                        onCheckedChange = { viewModel.onToggleTwoWeekCycle(it) }
                    )
                    if (uiState.preferences.isTwoWeekCycleEnabled) {
                        SettingsDivider()
                        SettingsRow(
                            label = "Дата начала семестра",
                            value = uiState.preferences.semesterStartDateIso,
                            onClick = {
                                showSemesterDateDialog = true
                            }
                        )
                        SettingsDivider()
                        SettingsButtonRow(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "Поменять текущую неделю",
                            sublabel = if (uiState.preferences.invertWeekCycle) "Инвертировано" else null,
                            iconTint = IosBlue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onToggleInvertWeekCycle()
                            }
                        )
                    }
                }
            }

            // 3. ИИ и данные (JSON)
            item(key = "section_ai_json") {
                SettingsSection(title = "ИИ И ДАННЫЕ (JSON)") {
                    SettingsButtonRow(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Скопировать промпт для ИИ",
                        iconTint = IosBlue,
                        onClick = { viewModel.copyAiPrompt(context) }
                    )
                    SettingsDivider()
                    SettingsButtonRow(
                        icon = Icons.Rounded.Download,
                        label = "Импортировать JSON",
                        iconTint = IosBlue,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            viewModel.openImportDialog(clipText)
                        }
                    )
                    SettingsDivider()
                    SettingsButtonRow(
                        icon = Icons.Rounded.Share,
                        label = "Экспортировать JSON",
                        iconTint = IosBlue,
                        onClick = { viewModel.exportSchedule() }
                    )
                    if (undoSnapshot != null) {
                        SettingsDivider()
                        SettingsButtonRow(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            label = "Отменить: ${undoSnapshot!!.actionLabel}",
                            iconTint = IosBlue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restoreLastUndo()
                            }
                        )
                    }
                }
            }

            // 4. Праздники РФ и Каникулы
            item(key = "section_holidays") {
                SettingsSection(title = "ПРАЗДНИКИ И КАНИКУЛЫ") {
                    SettingsSwitchRow(
                        label = "Праздники РФ",
                        checked = uiState.preferences.showRussianHolidays,
                        onCheckedChange = { viewModel.onToggleRussianHolidays(it) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        label = "Пропускать праздники в задачах",
                        checked = uiState.preferences.skipHolidaysInSmartHomework,
                        onCheckedChange = { viewModel.onToggleSkipHolidaysInHomework(it) }
                    )
                    SettingsDivider()
                    SettingsButtonRow(
                        icon = Icons.Rounded.Add,
                        label = "Добавить каникулы / отпуск",
                        iconTint = IosBlue,
                        onClick = { showAddVacationDialog = true }
                    )
                }
            }

            // List of custom vacations if any
            if (uiState.vacations.isNotEmpty()) {
                item(key = "vacations_header") {
                    Text(
                        text = "ПЕРИОДЫ КАНИКУЛ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                items(uiState.vacations, key = { "vac_${it.id}" }) { vac ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(iosColors.card)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vac.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textPrimary
                                )
                                Text(
                                    text = "${vac.startDateIso} — ${vac.endDateIso}",
                                    fontSize = 12.sp,
                                    color = iosColors.textSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteVacation(vac.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Удалить",
                                    tint = IosRed
                                )
                            }
                        }
                    }
                }
            }

            // 5. Каталог предметов
            item(key = "section_activities_header") {
                val activeProfile = uiState.activePreset.appProfile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "КАТАЛОГ (${activeProfile.subjectTerm.uppercase()}: ${uiState.activities.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosBlue.copy(alpha = 0.12f))
                            .clickable { onAddNewActivity() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+ ${activeProfile.subjectTerm}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosBlue
                        )
                    }
                }
            }

            items(uiState.activities, key = { "act_${it.id}" }) { act ->
                val actColor = ColorParser.parseHexColor(act.colorHex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(iosColors.card)
                        .clickable { onEditActivity(act.id) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(actColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = act.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listOfNotNull(
                                        act.shortName.takeIf { it.isNotBlank() },
                                        act.defaultLocation?.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    fontSize = 12.sp,
                                    color = iosColors.textSecondary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteActivity(act.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Удалить",
                                tint = iosColors.textSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Profile Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Профиль пресета «${uiState.activePreset.name}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppProfile.entries.forEach { profile ->
                        val isSelected = uiState.activePreset.appProfile == profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) IosBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewModel.onProfileChanged(profile)
                                    showProfileDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) IosBlue else iosColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = IosBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Modal Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Тема оформления") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { theme ->
                        val isSelected = uiState.preferences.themeMode == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) IosBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewModel.onThemeChanged(theme)
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme.displayName,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) IosBlue else iosColors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = IosBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Modal Evening Summary Time Wheel Dialog
    if (showEveningTimeDialog) {
        IosWheelTimePickerDialog(
            title = "Время вечерней сводки",
            initialTime = uiState.preferences.eveningSummaryTime,
            onDismiss = { showEveningTimeDialog = false },
            onConfirm = { newTime ->
                viewModel.onEveningSummaryTimeChanged(newTime)
                showEveningTimeDialog = false
            }
        )
    }

    // Modal Morning Task Reminder Time Wheel Dialog
    if (showMorningTaskTimeDialog) {
        IosWheelTimePickerDialog(
            title = "Время напоминания о задачах",
            initialTime = uiState.preferences.taskRemindersTime,
            onDismiss = { showMorningTaskTimeDialog = false },
            onConfirm = { newTime ->
                viewModel.onTaskRemindersTimeChanged(newTime)
                showMorningTaskTimeDialog = false
            }
        )
    }

    // Modal Semester Start Date Calendar Dialog
    if (showSemesterDateDialog) {
        IosCalendarDatePickerDialog(
            title = "Дата начала семестра",
            initialDateIso = uiState.preferences.semesterStartDateIso,
            onDismiss = { showSemesterDateDialog = false },
            onConfirm = { newDateIso ->
                viewModel.onSemesterStartDateChanged(newDateIso)
                showSemesterDateDialog = false
            }
        )
    }

    // Modal Add Vacation Dialog
    if (showAddVacationDialog) {
        var vacTitle by remember { mutableStateOf("Осенние каникулы") }
        var vacStart by remember { mutableStateOf(LocalDate.now().toString()) }
        var vacEnd by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }

        AlertDialog(
            onDismissRequest = { showAddVacationDialog = false },
            title = { Text("Добавить каникулы") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vacTitle,
                        onValueChange = { vacTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    IosDatePickerButton(
                        label = "Дата начала",
                        dateIso = vacStart,
                        onDateSelected = { vacStart = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    IosDatePickerButton(
                        label = "Дата окончания",
                        dateIso = vacEnd,
                        onDateSelected = { vacEnd = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (vacTitle.isNotBlank() && vacStart.isNotBlank() && vacEnd.isNotBlank()) {
                            viewModel.addVacation(vacTitle.trim(), vacStart.trim(), vacEnd.trim())
                            showAddVacationDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVacationDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Modal JSON Import Dialog
    if (uiState.isImportDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.closeImportDialog() },
            title = { Text("Импорт расписания") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.importInputText,
                        onValueChange = { viewModel.onImportInputChanged(it) },
                        placeholder = { Text("Вставьте JSON или текст от нейросети...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    // Validation status preview
                    when (val result = uiState.importValidationResult) {
                        is ValidationResult.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IosGreen.copy(alpha = 0.12f))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Готово к импорту:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosGreen
                                    )
                                    Text(
                                        text = "Предметов: ${result.activitiesCount}, занятий: ${result.slotsCount}, событий: ${result.eventsCount}, задач: ${result.tasksCount}",
                                        fontSize = 12.sp,
                                        color = iosColors.textPrimary
                                    )
                                }
                            }
                        }
                        is ValidationResult.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IosRed.copy(alpha = 0.12f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = result.message,
                                    fontSize = 12.sp,
                                    color = IosRed
                                )
                            }
                        }
                        null -> {}
                    }

                    // Mode selection: Replace vs Merge
                    Column {
                        Text(
                            text = "Режим импорта:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosColors.textSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uiState.selectedImportMode == ImportMode.REPLACE,
                                onClick = { viewModel.setImportMode(ImportMode.REPLACE) },
                                colors = RadioButtonDefaults.colors(selectedColor = IosBlue)
                            )
                            Text(text = "Заменить всё", fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uiState.selectedImportMode == ImportMode.MERGE,
                                onClick = { viewModel.setImportMode(ImportMode.MERGE) },
                                colors = RadioButtonDefaults.colors(selectedColor = IosBlue)
                            )
                            Text(text = "Объединить (Upsert)", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.executeImport() },
                    enabled = uiState.importValidationResult is ValidationResult.Success
                ) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeImportDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val iosColors = LocalIosColors.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = iosColors.textSecondary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(iosColors.card)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val iosColors = LocalIosColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = iosColors.textPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = iosColors.textSecondary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = iosColors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsButtonRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    sublabel: String? = null
) {
    val iosColors = LocalIosColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textPrimary
            )
            if (!sublabel.isNullOrBlank()) {
                Text(
                    text = sublabel,
                    fontSize = 12.sp,
                    color = iosColors.textSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = iosColors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sublabel: String? = null,
    trailingBadge: String? = null,
    onTrailingBadgeClick: (() -> Unit)? = null
) {
    val iosColors = LocalIosColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iosColors.textPrimary
            )
            if (!sublabel.isNullOrBlank()) {
                Text(
                    text = sublabel,
                    fontSize = 12.sp,
                    color = iosColors.textSecondary
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!trailingBadge.isNullOrBlank() && onTrailingBadgeClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosBlue.copy(alpha = 0.14f))
                        .clickable { onTrailingBadgeClick() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosBlue
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = iosSwitchColors()
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    val iosColors = LocalIosColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(iosColors.separator)
    )
}
