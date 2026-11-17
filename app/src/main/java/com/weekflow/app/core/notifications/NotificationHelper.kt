package com.weekflow.app.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.weekflow.app.MainActivity
import com.weekflow.app.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_LESSONS = "channel_weekflow_lessons"
        const val CHANNEL_SUMMARIES = "channel_weekflow_summaries"
        const val CHANNEL_TASKS = "channel_weekflow_tasks"
        const val CHANNEL_REGULAR_REMINDERS = "channel_weekflow_regular_reminders"

        const val NOTIFICATION_ID_TEST = 10001
        const val NOTIFICATION_ID_EVENING = 10002
        const val NOTIFICATION_ID_MORNING = 10003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val lessonChannel = NotificationChannel(
                CHANNEL_LESSONS,
                "Напоминания о событиях и занятиях",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления за несколько минут до начала уроков и событий"
                enableVibration(true)
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARIES,
                "Вечерняя сводка расписания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "План на завтра и количество задач"
            }

            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Дедлайны и задачи",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о невыполненных задачах на сегодня"
                enableVibration(true)
            }

            val regularRemindersChannel = NotificationChannel(
                CHANNEL_REGULAR_REMINDERS,
                "Регулярные напоминания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Периодические и запланированные пользовательские напоминания"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(lessonChannel, summaryChannel, tasksChannel, regularRemindersChannel)
            )
        }
    }

    private fun getLaunchPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showLessonReminderNotification(
        id: Int,
        title: String,
        message: String,
        tasksCount: Int = 0
    ) {
        if (!hasNotificationPermission()) return

        val text = if (tasksCount > 0) {
            "$message\n📌 Задач к занятию: $tasksCount"
        } else {
            message
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_LESSONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {}
    }

    fun showEveningSummaryNotification(
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARIES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EVENING, notification)
        } catch (_: SecurityException) {}
    }

    fun showTaskDeadlinesNotification(
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
        } catch (_: SecurityException) {}
    }

    fun showRegularReminderNotification(
        id: Int,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_REGULAR_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {}
    }

    fun showTestNotification() {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_LESSONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 Тестовое уведомление WeekFlow")
            .setContentText("Через 10 мин: Высшая математика (09:00–10:35) • ауд. 302")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Через 10 мин: Высшая математика (09:00–10:35) • ауд. 302\n📌 Задач к занятию: 2\n\nСистема уведомлений WeekFlow настроена и работает идеально!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
        } catch (_: SecurityException) {}
    }
}
