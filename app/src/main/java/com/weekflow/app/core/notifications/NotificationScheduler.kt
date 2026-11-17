package com.weekflow.app.core.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationScheduler(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase
) {
    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private const val PERMISSION_PREFS_NAME = "weekflow_notif_prefs"
        private const val KEY_POST_NOTIF_REQUESTED = "post_notifications_requested"

        private tailrec fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

        internal fun wasNotificationPermissionRequested(context: Context): Boolean {
            return context.getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_POST_NOTIF_REQUESTED, false)
        }

        internal fun markNotificationPermissionRequested(context: Context) {
            context.getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_POST_NOTIF_REQUESTED, true)
                .apply()
        }

        internal fun shouldOpenNotificationSettingsInsteadOfPrompt(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
            if (!wasNotificationPermissionRequested(context)) return false
            val activity = context.findActivity() ?: return true
            return !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        internal fun openAppNotificationSettings(context: Context) {
            val notifIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(notifIntent)
            } catch (_: Exception) {
                try {
                    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(detailsIntent)
                } catch (_: Exception) {}
            }
        }

        fun computeNextTrigger(
            reminder: RegularReminder,
            now: LocalDateTime = LocalDateTime.now()
        ): LocalDateTime? {
            val allowedDays = reminder.daysOfWeek.filter { it in 1..7 }.toSet()
            return if (reminder.isIntervalMode) {
                val startT = runCatching { LocalTime.parse(reminder.startTime, TIME_FORMATTER) }
                    .getOrDefault(LocalTime.of(9, 0))
                val endT = runCatching { LocalTime.parse(reminder.endTime, TIME_FORMATTER) }
                    .getOrDefault(LocalTime.of(21, 0))
                val intervalMin = reminder.intervalMinutes.coerceIn(1, 779).toLong()
                val intervalSec = intervalMin * 60L

                for (dayOffset in -1..7) {
                    if (dayOffset == -1 && endT.isAfter(startT)) continue
                    val date = now.toLocalDate().plusDays(dayOffset.toLong())
                    if (allowedDays.isNotEmpty() && date.dayOfWeek.value !in allowedDays) continue

                    val windowStart = LocalDateTime.of(date, startT)
                    val windowEnd = if (!endT.isAfter(startT)) {
                        LocalDateTime.of(date.plusDays(1), endT)
                    } else {
                        LocalDateTime.of(date, endT)
                    }

                    val candidate = when {
                        now.isBefore(windowStart) -> windowStart
                        !now.isAfter(windowEnd) -> {
                            val elapsedSec = Duration.between(windowStart, now).seconds.coerceAtLeast(0L)
                            val k = (elapsedSec / intervalSec) + 1L
                            windowStart.plusSeconds(k * intervalSec).takeIf { !it.isAfter(windowEnd) }
                        }
                        else -> null
                    }
                    if (candidate != null && candidate.isAfter(now)) {
                        return candidate
                    }
                }
                null
            } else {
                val exactT = runCatching { LocalTime.parse(reminder.exactTime, TIME_FORMATTER) }
                    .getOrDefault(LocalTime.of(9, 0))
                for (dayOffset in 0..7) {
                    val date = now.toLocalDate().plusDays(dayOffset.toLong())
                    if (allowedDays.isNotEmpty() && date.dayOfWeek.value !in allowedDays) continue
                    val candidate = LocalDateTime.of(date, exactT)
                    if (candidate.isAfter(now)) {
                        return candidate
                    }
                }
                null
            }
        }

        private fun formatIntervalForMessage(minutes: Int): String {
            val safe = minutes.coerceIn(1, 779)
            val hrs = safe / 60
            val rem = safe % 60
            return when {
                hrs > 0 && rem == 0 -> "$hrs ч"
                hrs > 0 -> "$hrs ч $rem мин"
                else -> "$safe мин"
            }
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAllNotifications(minLeadSeconds: Long = 0L) {
        scheduleAll(minLeadSeconds = minLeadSeconds)
    }

    suspend fun scheduleAll(minLeadSeconds: Long = 0L) {
        val preferences = preferencesRepository.userPreferences.first()
        if (!preferences.notificationsEnabled) {
            cancelAll(preferences.regularReminders)
            return
        }

        // 1. Schedule lesson and event reminders for today and tomorrow (48 hours ahead)
        scheduleUpcomingLessons(preferences.notifyMinutesBefore)

        // 2. Schedule daily evening summary (20:00)
        if (preferences.eveningSummaryEnabled) {
            scheduleDailyEveningSummary()
        } else {
            cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY)
        }

        // 3. Schedule morning task reminders (08:00)
        if (preferences.taskRemindersEnabled) {
            scheduleMorningTaskReminder()
        } else {
            cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_TASK_DEADLINES)
        }

        // 4. Schedule regular reminders (interval mode and fixed-time mode)
        scheduleRegularReminders(minLeadSeconds = minLeadSeconds)
    }

    suspend fun scheduleRegularReminders(minLeadSeconds: Long = 0L) {
        val preferences = preferencesRepository.userPreferences.first()
        val now = LocalDateTime.now().plusSeconds(minLeadSeconds.coerceAtLeast(0L))

        preferences.regularReminders.forEach { reminder ->
            val reqCode = ("regular_" + reminder.id).hashCode()
            val title = reminder.title.ifBlank { "Напоминание" }
            val message = if (reminder.isIntervalMode) {
                "Каждые ${formatIntervalForMessage(reminder.intervalMinutes)} (${reminder.startTime}–${reminder.endTime})"
            } else {
                "Запланировано на ${reminder.exactTime}"
            }
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_ALARM
                data = Uri.parse("weekflow://alarm/regular/${reminder.id}")
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_REGULAR_REMINDER)
                putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
                putExtra(ScheduleAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, title)
                putExtra(ScheduleAlarmReceiver.EXTRA_MESSAGE, message)
            }
            if (!preferences.notificationsEnabled || !reminder.isEnabled) {
                cancelRegularReminderIntent(reqCode, intent)
            } else {
                val nextTriggerDateTime = computeNextTrigger(reminder, now)
                if (nextTriggerDateTime != null) {
                    val triggerMillis = nextTriggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setAlarm(triggerMillis, pi)
                } else {
                    cancelRegularReminderIntent(reqCode, intent)
                }
            }
        }
    }

    private fun cancelRegularReminderIntent(reqCode: Int, intent: Intent) {
        val cancelPi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (cancelPi != null) {
            alarmManager.cancel(cancelPi)
            cancelPi.cancel()
        }
    }

    suspend fun scheduleUpcomingLessons(minutesBefore: Int) {
        val preferences = preferencesRepository.userPreferences.first()
        val nowDateTime = LocalDateTime.now()
        val allPresets = scheduleRepository.getAllPresets().first()
        val allSlots = scheduleRepository.getAllSlots().first()
        val allActivities = scheduleRepository.getAllActivities().first().associateBy { it.id }
        val allTasks = scheduleRepository.getAllTasks().first()
        val allEvents = scheduleRepository.getAllEvents().first()
        val allVacations = scheduleRepository.getAllVacations().first()

        val datesToSchedule = listOf(LocalDate.now(), LocalDate.now().plusDays(1))

        for (targetDate in datesToSchedule) {
            val (dayPreset, _) = SchedulePreset.resolveForDate(
                date = targetDate,
                presets = allPresets,
                activePresetId = preferences.activePresetId
            )
            val isVacation = allVacations.any { it.contains(targetDate) }
            val dayOfWeek = targetDate.dayOfWeek.value
            val isWeekend = dayOfWeek == 7 || (preferences.isSaturdayWeekend && dayOfWeek == 6)

            // 1. Slots reminders
            if (!isVacation && !isWeekend) {
                val cycle = calculateWeekCycleUseCase(targetDate, preferences)
                val daySlots = allSlots.filter { slot ->
                    slot.presetId == dayPreset.id &&
                        slot.dayOfWeek == dayOfWeek &&
                        (!preferences.isTwoWeekCycleEnabled ||
                            slot.weekCycle == WeekCycle.EVERY_WEEK ||
                            slot.weekCycle == cycle)
                }

                for (slot in daySlots) {
                    val startTime = slot.parseStartTime()
                    val reminderTime = startTime.minusMinutes(minutesBefore.toLong())
                    val triggerDateTime = LocalDateTime.of(targetDate, reminderTime)

                    if (triggerDateTime.isAfter(nowDateTime)) {
                        val activity = allActivities[slot.activityTypeId]
                        val subjectName = activity?.name ?: "Занятие"
                        val location = slot.customLocation?.takeIf { it.isNotBlank() } ?: activity?.defaultLocation ?: ""
                        val locStr = if (location.isNotBlank()) " • $location" else ""

                        val tasksForSlot = allTasks.count { task ->
                            task.presetId == dayPreset.id &&
                                !task.isCompleted &&
                                task.dueDateIso == targetDate.toString() &&
                                (task.linkedSlotId == slot.id || (task.linkedSlotId == null && task.linkedActivityId == slot.activityTypeId))
                        }

                        val title = "Через $minutesBefore мин: $subjectName"
                        val message = "$subjectName (${slot.startTime}–${slot.endTime})$locStr"

                        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val requestCode = (slot.id + targetDate.toString()).hashCode()

                        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                            action = ScheduleAlarmReceiver.ACTION_ALARM
                            putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_LESSON)
                            putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                            putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, title)
                            putExtra(ScheduleAlarmReceiver.EXTRA_MESSAGE, message)
                            putExtra(ScheduleAlarmReceiver.EXTRA_TASKS_COUNT, tasksForSlot)
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        setAlarm(triggerMillis, pendingIntent)
                    }
                }
            }

            // 2. Calendar Events reminders
            val dayEvents = allEvents.filter {
                it.presetId == dayPreset.id && it.occursOnDate(targetDate) && !it.isAllDay && it.startTime != null
            }
            for (event in dayEvents) {
                val eventStart = event.parseStartTime() ?: continue
                val reminderTime = eventStart.minusMinutes(minutesBefore.toLong())
                val triggerDateTime = LocalDateTime.of(targetDate, reminderTime)

                if (triggerDateTime.isAfter(nowDateTime)) {
                    val title = "Через $minutesBefore мин: ${event.title}"
                    val timeStr = "${event.startTime}–${event.endTime ?: ""}".trimEnd('–')
                    val locStr = if (!event.location.isNullOrBlank()) " • ${event.location}" else ""
                    val message = "${event.title} ($timeStr)$locStr"

                    val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val requestCode = (event.id + targetDate.toString()).hashCode()

                    val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                        action = ScheduleAlarmReceiver.ACTION_ALARM
                        putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_LESSON)
                        putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                        putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, title)
                        putExtra(ScheduleAlarmReceiver.EXTRA_MESSAGE, message)
                        putExtra(ScheduleAlarmReceiver.EXTRA_TASKS_COUNT, 0)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setAlarm(triggerMillis, pendingIntent)
                }
            }
        }
    }

    suspend fun scheduleDailyEveningSummary() {
        val preferences = preferencesRepository.userPreferences.first()
        val summaryTimeStr = preferences.eveningSummaryTime // e.g. "20:00"
        val summaryTime = try {
            LocalTime.parse(summaryTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            LocalTime.of(20, 0)
        }

        val now = LocalDateTime.now()
        var targetDateTime = LocalDateTime.of(LocalDate.now(), summaryTime)
        if (now.isAfter(targetDateTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_ALARM
            putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(triggerMillis, pendingIntent)
    }

    suspend fun scheduleMorningTaskReminder() {
        val preferences = preferencesRepository.userPreferences.first()
        val morningTimeStr = preferences.taskRemindersTime
        val morningTime = try {
            LocalTime.parse(morningTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            LocalTime.of(8, 0)
        }
        val now = LocalDateTime.now()
        var targetDateTime = LocalDateTime.of(LocalDate.now(), morningTime)
        if (now.isAfter(targetDateTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_ALARM
            putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_TASK_DEADLINES)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ScheduleAlarmReceiver.ALARM_TYPE_TASK_DEADLINES,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(triggerMillis, pendingIntent)
    }

    private fun setAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        val safeTriggerMillis = maxOf(triggerMillis, System.currentTimeMillis() + 500L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, safeTriggerMillis, pendingIntent)
                }
            } catch (_: Exception) {}
        }
    }

    private fun cancelAlarmByType(alarmType: Int) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmType,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAll(regularReminders: List<RegularReminder> = emptyList()) {
        cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY)
        cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_TASK_DEADLINES)
        regularReminders.forEach { reminder ->
            val reqCode = ("regular_" + reminder.id).hashCode()
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_ALARM
                data = Uri.parse("weekflow://alarm/regular/${reminder.id}")
                putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_REGULAR_REMINDER)
                putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
                putExtra(ScheduleAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            }
            cancelRegularReminderIntent(reqCode, intent)
        }
    }
}
