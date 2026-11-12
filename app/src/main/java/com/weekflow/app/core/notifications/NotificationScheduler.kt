package com.weekflow.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.weekflow.app.domain.model.RegularReminder
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import kotlinx.coroutines.flow.first
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
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAllNotifications() {
        scheduleAll()
    }

    suspend fun scheduleAll() {
        val preferences = preferencesRepository.userPreferences.first()
        if (!preferences.notificationsEnabled) {
            cancelAll()
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
        scheduleRegularReminders()
    }

    suspend fun scheduleRegularReminders() {
        val preferences = preferencesRepository.userPreferences.first()
        val now = LocalDateTime.now()
        val fmt = DateTimeFormatter.ofPattern("HH:mm")

        preferences.regularReminders.forEach { reminder ->
            val reqCode = ("regular_" + reminder.id).hashCode()
            val title = reminder.title.ifBlank { "Напоминание" }
            val message = if (reminder.isIntervalMode) {
                "Каждые ${reminder.intervalMinutes} мин (${reminder.startTime}–${reminder.endTime})"
            } else {
                "Запланировано на ${reminder.exactTime}"
            }
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_ALARM
                putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_REGULAR_REMINDER)
                putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
                putExtra(ScheduleAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, title)
                putExtra(ScheduleAlarmReceiver.EXTRA_MESSAGE, message)
            }
            if (!reminder.isEnabled) {
                val cancelPi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (cancelPi != null) alarmManager.cancel(cancelPi)
            } else {
                val nextTriggerDateTime = computeNextRegularTrigger(reminder, now, fmt)
                if (nextTriggerDateTime != null) {
                    val triggerMillis = nextTriggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setAlarm(triggerMillis, pi)
                }
            }
        }
    }

    private fun computeNextRegularTrigger(
        reminder: RegularReminder,
        now: LocalDateTime,
        fmt: DateTimeFormatter
    ): LocalDateTime? {
        val allowedDays = reminder.daysOfWeek.filter { it in 1..7 }.toSet()
        return if (reminder.isIntervalMode) {
            val startT = runCatching { LocalTime.parse(reminder.startTime, fmt) }.getOrDefault(LocalTime.of(9, 0))
            val endT = runCatching { LocalTime.parse(reminder.endTime, fmt) }.getOrDefault(LocalTime.of(21, 0))
            val intervalMin = reminder.intervalMinutes.coerceIn(1, 720).toLong()

            for (dayOffset in -1..7) {
                if (dayOffset == -1 && !endT.isBefore(startT)) continue
                val date = now.toLocalDate().plusDays(dayOffset.toLong())
                if (allowedDays.isNotEmpty() && date.dayOfWeek.value !in allowedDays) continue

                val windowStart = LocalDateTime.of(date, startT)
                val windowEnd = if (endT.isBefore(startT)) {
                    LocalDateTime.of(date.plusDays(1), endT)
                } else {
                    LocalDateTime.of(date, endT)
                }

                val candidate = when {
                    now.isBefore(windowStart) -> windowStart
                    !now.isAfter(windowEnd) -> now.plusMinutes(intervalMin).takeIf { !it.isAfter(windowEnd) }
                    else -> null
                }
                if (candidate != null && candidate.isAfter(now)) {
                    return candidate
                }
            }
            null
        } else {
            val exactT = runCatching { LocalTime.parse(reminder.exactTime, fmt) }.getOrDefault(LocalTime.of(9, 0))
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
                it.presetId == dayPreset.id && it.dateIso == targetDate.toString() && !it.isAllDay && it.startTime != null
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
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
        }
    }

    fun cancelAll() {
        cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY)
        cancelAlarmByType(ScheduleAlarmReceiver.ALARM_TYPE_TASK_DEADLINES)
    }
}
