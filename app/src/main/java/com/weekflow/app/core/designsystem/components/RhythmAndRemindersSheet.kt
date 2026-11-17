package com.weekflow.app.core.designsystem.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.notifications.NotificationHelper
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.RegularReminder
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val MAX_INTERVAL_MINUTES = 779
private val NEXT_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val NEXT_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

private val DAY_LABELS = listOf(
    1 to "Пн",
    2 to "Вт",
    3 to "Ср",
    4 to "Чт",
    5 to "Пт",
    6 to "Сб",
    7 to "Вс"
)

private fun formatDaysBadge(daysOfWeek: List<Int>): String {
    if (daysOfWeek.isEmpty() || daysOfWeek.size >= 7) return "каждый день"
    val map = DAY_LABELS.toMap()
    return daysOfWeek.sorted().mapNotNull { map[it] }.joinToString(", ")
}

private fun formatIntervalLabel(minutes: Int): String {
    val safe = minutes.coerceIn(1, MAX_INTERVAL_MINUTES)
    val hrs = safe / 60
    val rem = safe % 60
    return when {
        hrs > 0 && rem == 0 -> "$hrs ч"
        hrs > 0 -> "$hrs ч $rem мин"
        else -> "$safe мин"
    }
}

private fun formatNextTriggerText(next: LocalDateTime, now: LocalDateTime): String {
    val today = now.toLocalDate()
    val nextDate = next.toLocalDate()
    val timeStr = next.format(NEXT_TIME_FMT)
    return when (nextDate) {
        today -> "Следующее: сегодня в $timeStr"
        today.plusDays(1) -> "Следующее: завтра в $timeStr"
        else -> {
            val dayLabel = DAY_LABELS.firstOrNull { it.first == next.dayOfWeek.value }?.second ?: ""
            val dateStr = next.format(NEXT_DATE_FMT)
            "Следующее: $dayLabel $dateStr в $timeStr"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularRemindersSheet(
    reminders: List<RegularReminder>,
    onUpdateReminders: (List<RegularReminder>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationHelper = remember(context) { NotificationHelper(context) }
    val alarmManager = remember(context) { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    var hasNotifPermission by remember { mutableStateOf(notificationHelper.hasNotificationPermission()) }
    var canExactAlarm by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = notificationHelper.hasNotificationPermission()
                canExactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var lastPromptLaunchAtMs by remember { mutableLongStateOf(0L) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val nowGranted = granted || notificationHelper.hasNotificationPermission()
        hasNotifPermission = nowGranted
        canExactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (!nowGranted &&
            NotificationScheduler.shouldOpenNotificationSettingsInsteadOfPrompt(context) &&
            System.currentTimeMillis() - lastPromptLaunchAtMs < 400L
        ) {
            NotificationScheduler.openAppNotificationSettings(context)
        }
    }

    var nowTick by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowTick = LocalDateTime.now()
        }
    }

    val hoursOptions = remember { (0..12).toList() }
    val minutesOptions = remember { (0..59).toList() }

    var editingReminder by remember { mutableStateOf<RegularReminder?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var editIsIntervalMode by remember { mutableStateOf(true) }
    var editIntervalMinutes by remember { mutableIntStateOf(60) }
    var isCustomIntervalInputOpen by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("60") }
    var minWheelResetKey by remember { mutableIntStateOf(0) }
    var editStartTime by remember { mutableStateOf("09:00") }
    var editEndTime by remember { mutableStateOf("21:00") }
    var editExactTime by remember { mutableStateOf("09:00") }
    var editDaysOfWeek by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun openCreateEditor() {
        editingReminder = null
        isCreatingNew = true
        editTitle = ""
        editIsIntervalMode = true
        editIntervalMinutes = 60
        customMinutesText = "60"
        isCustomIntervalInputOpen = false
        editStartTime = "09:00"
        editEndTime = "21:00"
        editExactTime = "09:00"
        editDaysOfWeek = emptyList()
    }

    fun openEditEditor(reminder: RegularReminder) {
        isCreatingNew = false
        editingReminder = reminder
        editTitle = reminder.title
        editIsIntervalMode = reminder.isIntervalMode
        val safeInterval = reminder.intervalMinutes.coerceIn(1, MAX_INTERVAL_MINUTES)
        editIntervalMinutes = safeInterval
        customMinutesText = safeInterval.toString()
        isCustomIntervalInputOpen = false
        editStartTime = reminder.startTime
        editEndTime = reminder.endTime
        editExactTime = reminder.exactTime
        editDaysOfWeek = reminder.daysOfWeek.sorted()
    }

    fun closeEditor() {
        editingReminder = null
        isCreatingNew = false
        isCustomIntervalInputOpen = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Регулярные напоминания",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(IosBlue.copy(alpha = 0.14f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                openCreateEditor()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = IosBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Добавить",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosBlue
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = iosColors.textSecondary
                        )
                    }
                }
            }

            // Permission / Exact Alarm Warning Banner
            if (!hasNotifPermission || !canExactAlarm) {
                val warningText = if (!hasNotifPermission) {
                    "Нет разрешения на уведомления"
                } else {
                    "Нет разрешения на точные будильники"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosOrange.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, IosOrange.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = IosOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = warningText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = {
                            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (NotificationScheduler.shouldOpenNotificationSettingsInsteadOfPrompt(context)) {
                                    NotificationScheduler.openAppNotificationSettings(context)
                                } else {
                                    NotificationScheduler.markNotificationPermissionRequested(context)
                                    lastPromptLaunchAtMs = System.currentTimeMillis()
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Разрешить",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }
                }
            }

            // Inline Create / Edit Card
            AnimatedVisibility(visible = isCreatingNew || editingReminder != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(iosColors.cardSecondary)
                        .border(BorderStroke(1.dp, IosBlue.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    IosSegmentedControl(
                        items = listOf("Интервальное", "Фиксированное время"),
                        selectedIndex = if (editIsIntervalMode) 0 else 1,
                        onItemSelected = { idx -> editIsIntervalMode = idx == 0 }
                    )

                    if (editIsIntervalMode) {
                        val selectedHours = (editIntervalMinutes / 60).coerceIn(0, 12)
                        val selectedMinutes = (editIntervalMinutes % 60).coerceIn(0, 59)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(iosColors.card)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Интервал",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IosBlue.copy(alpha = 0.12f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (!isCustomIntervalInputOpen) {
                                                customMinutesText = editIntervalMinutes.toString()
                                            }
                                            isCustomIntervalInputOpen = !isCustomIntervalInputOpen
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${formatIntervalLabel(editIntervalMinutes)} ✎",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosBlue
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isCustomIntervalInputOpen) {
                                OutlinedTextField(
                                    value = customMinutesText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        customMinutesText = digits
                                        val parsed = digits.toIntOrNull()
                                        if (parsed != null) {
                                            editIntervalMinutes = parsed.coerceIn(1, MAX_INTERVAL_MINUTES)
                                        }
                                    },
                                    label = { Text("Минуты (1–$MAX_INTERVAL_MINUTES)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val parsed = customMinutesText.toIntOrNull()
                                                if (parsed != null) {
                                                    editIntervalMinutes = parsed.coerceIn(1, MAX_INTERVAL_MINUTES)
                                                }
                                                customMinutesText = editIntervalMinutes.toString()
                                                isCustomIntervalInputOpen = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Готово",
                                                tint = IosBlue
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Часы",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosColors.textSecondary
                                    )
                                    IosWheelPicker(
                                        items = hoursOptions,
                                        selectedIndex = selectedHours,
                                        onItemSelected = { _, hrs ->
                                            val nextTotal = (hrs * 60 + selectedMinutes).coerceIn(1, MAX_INTERVAL_MINUTES)
                                            editIntervalMinutes = nextTotal
                                            customMinutesText = nextTotal.toString()
                                        },
                                        itemLabel = { hrs -> "$hrs ч" },
                                        visibleItemsCount = 3,
                                        itemHeight = 36.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Минуты",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = iosColors.textSecondary
                                    )
                                    key(minWheelResetKey) {
                                        IosWheelPicker(
                                            items = minutesOptions,
                                            selectedIndex = selectedMinutes,
                                            onItemSelected = { _, mins ->
                                                val raw = selectedHours * 60 + mins
                                                if (raw == 0) {
                                                    minWheelResetKey++
                                                }
                                                val nextTotal = raw.coerceIn(1, MAX_INTERVAL_MINUTES)
                                                editIntervalMinutes = nextTotal
                                                customMinutesText = nextTotal.toString()
                                            },
                                            itemLabel = { mins -> "$mins мин" },
                                            visibleItemsCount = 3,
                                            itemHeight = 36.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IosTimePickerButton(
                                label = "С",
                                timeString = editStartTime,
                                onTimeSelected = { editStartTime = it },
                                modifier = Modifier.weight(1f)
                            )
                            IosTimePickerButton(
                                label = "До",
                                timeString = editEndTime,
                                onTimeSelected = { editEndTime = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        IosTimePickerButton(
                            label = "Время",
                            timeString = editExactTime,
                            onTimeSelected = { editExactTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Days of week chips (empty = every day)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Дни недели",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textSecondary
                            )
                            Text(
                                text = formatDaysBadge(editDaysOfWeek),
                                fontSize = 11.5.sp,
                                color = if (editDaysOfWeek.isEmpty()) iosColors.textSecondary else IosBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DAY_LABELS.forEach { (day, label) ->
                                val isSelected = day in editDaysOfWeek
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IosBlue else iosColors.card)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            editDaysOfWeek = if (isSelected) {
                                                editDaysOfWeek - day
                                            } else {
                                                (editDaysOfWeek + day).sorted()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else iosColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingReminder != null) {
                            IconButton(
                                onClick = {
                                    val targetId = editingReminder!!.id
                                    onUpdateReminders(reminders.filterNot { it.id == targetId })
                                    closeEditor()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Удалить",
                                    tint = iosColors.textSecondary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = ::closeEditor) {
                                Text("Отмена", color = iosColors.textSecondary)
                            }
                            Button(
                                onClick = {
                                    val cleanTitle = editTitle.trim()
                                    if (cleanTitle.isNotEmpty()) {
                                        val finalInterval = editIntervalMinutes.coerceIn(1, MAX_INTERVAL_MINUTES)
                                        if (isCreatingNew) {
                                            val created = RegularReminder(
                                                id = UUID.randomUUID().toString(),
                                                title = cleanTitle,
                                                isEnabled = true,
                                                isIntervalMode = editIsIntervalMode,
                                                intervalMinutes = finalInterval,
                                                startTime = editStartTime,
                                                endTime = editEndTime,
                                                exactTime = editExactTime,
                                                daysOfWeek = editDaysOfWeek
                                            )
                                            onUpdateReminders(reminders + created)
                                        } else if (editingReminder != null) {
                                            val targetId = editingReminder!!.id
                                            val updated = reminders.map { item ->
                                                if (item.id == targetId) {
                                                    item.copy(
                                                        title = cleanTitle,
                                                        isIntervalMode = editIsIntervalMode,
                                                        intervalMinutes = finalInterval,
                                                        startTime = editStartTime,
                                                        endTime = editEndTime,
                                                        exactTime = editExactTime,
                                                        daysOfWeek = editDaysOfWeek
                                                    )
                                                } else item
                                            }
                                            onUpdateReminders(updated)
                                        }
                                        closeEditor()
                                    }
                                },
                                enabled = editTitle.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = iosPrimaryButtonColors(IosBlue)
                            ) {
                                Text("Сохранить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Empty State or Configured Reminders List
            if (reminders.isEmpty() && !isCreatingNew) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(iosColors.cardSecondary)
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = iosColors.textSecondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "Нет напоминаний",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEach { reminder ->
                        val scheduleSummary = if (reminder.isIntervalMode) {
                            "Каждые ${formatIntervalLabel(reminder.intervalMinutes)} • ${reminder.startTime}–${reminder.endTime}"
                        } else {
                            "В ${reminder.exactTime}"
                        }
                        val daysSuffix = formatDaysBadge(reminder.daysOfWeek)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (reminder.isEnabled) IosBlue.copy(alpha = 0.08f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (reminder.isEnabled) 1.dp else 0.dp,
                                        color = if (reminder.isEnabled) IosBlue.copy(alpha = 0.35f) else Color.Transparent
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { openEditEditor(reminder) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = reminder.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (reminder.isEnabled) iosColors.textPrimary else iosColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$scheduleSummary • $daysSuffix",
                                    fontSize = 12.sp,
                                    color = if (reminder.isEnabled) IosBlue else iosColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (reminder.isEnabled) {
                                    val nextTrigger = remember(reminder, nowTick) {
                                        NotificationScheduler.computeNextTrigger(reminder, nowTick)
                                    }
                                    if (nextTrigger != null) {
                                        Text(
                                            text = formatNextTriggerText(nextTrigger, nowTick),
                                            fontSize = 11.sp,
                                            color = iosColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = reminder.isEnabled,
                                    onCheckedChange = { enabled ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val updated = reminders.map {
                                            if (it.id == reminder.id) it.copy(isEnabled = enabled) else it
                                        }
                                        onUpdateReminders(updated)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IosBlue,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = if (iosColors.isDark) Color(0xFF48484C) else Color(0xFFE5E5EA),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (editingReminder?.id == reminder.id) closeEditor()
                                        onUpdateReminders(reminders.filterNot { it.id == reminder.id })
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Удалить",
                                        tint = iosColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
