package com.weekflow.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class ScheduleAlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_ALARM = "com.weekflow.app.ACTION_SCHEDULE_ALARM"

        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val ALARM_TYPE_LESSON = 1
        const val ALARM_TYPE_EVENING_SUMMARY = 2
        const val ALARM_TYPE_TASK_DEADLINES = 3
        const val ALARM_TYPE_REGULAR_REMINDER = 4

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TASKS_COUNT = "extra_tasks_count"
    }

    private val notificationHelper: NotificationHelper by inject()
    private val scheduleRepository: ScheduleRepository by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val calculateWeekCycleUseCase: CalculateWeekCycleUseCase by inject()
    private val notificationScheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getIntExtra(EXTRA_ALARM_TYPE, 0)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (alarmType) {
                    ALARM_TYPE_LESSON -> {
                        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1)
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Напоминание о занятии"
                        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Скоро начнется занятие"
                        val tasksCount = intent.getIntExtra(EXTRA_TASKS_COUNT, 0)

                        notificationHelper.showLessonReminderNotification(
                            id = notifId,
                            title = title,
                            message = message,
                            tasksCount = tasksCount
                        )
                    }

                    ALARM_TYPE_EVENING_SUMMARY -> {
                        val preferences = preferencesRepository.userPreferences.first()
                        if (preferences.notificationsEnabled && preferences.eveningSummaryEnabled) {
                            val tomorrow = LocalDate.now().plusDays(1)
                            val allPresets = scheduleRepository.getAllPresets().first()
                            val (tomorrowPreset, _) = SchedulePreset.resolveForDate(
                                date = tomorrow,
                                presets = allPresets,
                                activePresetId = preferences.activePresetId
                            )
                            val tomorrowCycle = calculateWeekCycleUseCase(tomorrow, preferences)
                            val allSlots = scheduleRepository.getAllSlots().first()
                            val tomorrowSlots = allSlots.filter { slot ->
                                slot.presetId == tomorrowPreset.id &&
                                    slot.dayOfWeek == tomorrow.dayOfWeek.value &&
                                    (!preferences.isTwoWeekCycleEnabled ||
                                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                                        slot.weekCycle == tomorrowCycle)
                            }.sortedBy { it.startTime }

                            val allTasks = scheduleRepository.getAllTasks().first()
                            val tomorrowTasks = allTasks.filter {
                                it.presetId == tomorrowPreset.id &&
                                    !it.isCompleted &&
                                    (it.dueDateIso == tomorrow.toString() || it.dueDateIso == null)
                            }

                            val title = "План на завтра (${tomorrowPreset.iconEmoji} ${tomorrowPreset.name})"
                            val message = if (tomorrowSlots.isNotEmpty()) {
                                val firstTime = tomorrowSlots.first().startTime
                                "План на завтра: ${tomorrowSlots.size} занятий (первое в $firstTime) • Невыполненных задач: ${tomorrowTasks.size}"
                            } else {
                                "Завтра занятий нет • Активных задач: ${tomorrowTasks.size}"
                            }

                            notificationHelper.showEveningSummaryNotification(title, message)
                        }
                    }

                    ALARM_TYPE_TASK_DEADLINES -> {
                        val preferences = preferencesRepository.userPreferences.first()
                        if (preferences.notificationsEnabled && preferences.taskRemindersEnabled) {
                            val today = LocalDate.now()
                            val allPresets = scheduleRepository.getAllPresets().first()
                            val (todayPreset, _) = SchedulePreset.resolveForDate(
                                date = today,
                                presets = allPresets,
                                activePresetId = preferences.activePresetId
                            )
                            val allTasks = scheduleRepository.getAllTasks().first()
                            val todayTasks = allTasks.filter {
                                (preferences.showAllPresetsTasks || it.presetId == todayPreset.id) &&
                                    !it.isCompleted &&
                                    it.dueDateIso == today.toString()
                            }

                            if (todayTasks.isNotEmpty()) {
                                val title = "📌 Задачи на сегодня"
                                val message = "Не забудьте: сегодня срок сдачи ${todayTasks.size} задач!"
                                notificationHelper.showTaskDeadlinesNotification(title, message)
                            }
                        }
                    }

                    ALARM_TYPE_REGULAR_REMINDER -> {
                        val preferences = preferencesRepository.userPreferences.first()
                        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
                        val reminder = if (reminderId != null) {
                            preferences.regularReminders.firstOrNull { it.id == reminderId && it.isEnabled }
                        } else {
                            null
                        }
                        if (preferences.notificationsEnabled && (reminderId == null || reminder != null)) {
                            val title = reminder?.title?.ifBlank { "Напоминание" }
                                ?: intent.getStringExtra(EXTRA_TITLE)
                                ?: "Напоминание"
                            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Регулярное напоминание"
                            notificationHelper.showTaskDeadlinesNotification(title, message)
                        }
                    }
                }

                // Guaranteed daily rescheduling of all alarms for the next 48h
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
