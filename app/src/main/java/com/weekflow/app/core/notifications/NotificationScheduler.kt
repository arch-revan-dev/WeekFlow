package com.weekflow.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        }

        // 3. Schedule morning task reminders (08:00)
        if (preferences.taskRemindersEnabled) {
            scheduleMorningTaskReminder()
        }

        // 4. Schedule interval rhythm reminders and quick one-shot reminders
        scheduleRhythmAndQuickReminders()
    }

    suspend fun scheduleRhythmAndQuickReminders() {
        val preferences = preferencesRepository.userPreferences.first()
        val now = LocalDateTime.now()
        val nowMillis = System.currentTimeMillis()
        val fmt = DateTimeFormatter.ofPattern("HH:mm")

        // Schedule Interval Rhythm Reminders (Next-Tick Chaining within active window)
        preferences.rhythmReminders.forEach { reminder ->
            val reqCode = ("rhythm_" + reminder.id).hashCode()
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_ALARM
                putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_RHYTHM_REMINDER)
                putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
                putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, "${reminder.iconEmoji} ${reminder.title}")
                putExtra(
                    ScheduleAlarmReceiver.EXTRA_MESSAGE,
                    reminder.subtitle.ifBlank { "Регулярное напоминание (каждые ${reminder.intervalMinutes} мин)" }
                )
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
                val startT = runCatching { LocalTime.parse(reminder.startTime, fmt) }.getOrDefault(LocalTime.of(9, 0))
                val endT = runCatching { LocalTime.parse(reminder.endTime, fmt) }.getOrDefault(LocalTime.of(21, 30))
                val intervalMin = reminder.intervalMinutes.coerceIn(15, 720).toLong()

                val todayStart = LocalDateTime.of(now.toLocalDate(), startT)
                val todayEnd = if (endT.isAfter(startT)) {
                    LocalDateTime.of(now.toLocalDate(), endT)
                } else {
                    LocalDateTime.of(now.toLocalDate().plusDays(1), endT)
                }

                val nextTriggerDateTime = when {
                    now.isBefore(todayStart) -> todayStart
                    !now.isAfter(todayEnd) -> {
                        val candidate = now.plusMinutes(intervalMin)
                        if (!candidate.isAfter(todayEnd)) candidate else todayStart.plusDays(1)
                    }
                    else -> todayStart.plusDays(1)
                }

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

        // Schedule Quick One-Shot Reminders
        preferences.quickReminders.forEach { quick ->
            val reqCode = ("quick_" + quick.id).hashCode()
            if (quick.triggerAtEpochMillis > nowMillis) {
                val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                    action = ScheduleAlarmReceiver.ACTION_ALARM
                    putExtra(ScheduleAlarmReceiver.EXTRA_ALARM_TYPE, ScheduleAlarmReceiver.ALARM_TYPE_QUICK_REMINDER)
                    putExtra(ScheduleAlarmReceiver.EXTRA_NOTIFICATION_ID, reqCode)
                    putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, "${quick.iconEmoji} Напоминание")
                    putExtra(ScheduleAlarmReceiver.EXTRA_MESSAGE, "${quick.title} (${quick.targetTimeFormatted})")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(quick.triggerAtEpochMillis, pi)
            }
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

    fun cancelAll() {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ScheduleAlarmReceiver.ALARM_TYPE_EVENING_SUMMARY,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
