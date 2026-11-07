package com.weekflow.app.core.focus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.weekflow.app.MainActivity
import com.weekflow.app.R
import com.weekflow.app.core.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FocusTimerState(
    val title: String = "Фокус-сессия",
    val selectedMinutes: Int = 25,
    val totalSeconds: Int = 25 * 60,
    val secondsRemaining: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false
)

object FocusTimerManager {
    private const val CHANNEL_FOCUS = "channel_weekflow_focus_timer"
    private const val NOTIFICATION_ID_FOCUS = 10077

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var appContext: Context? = null
    private var onSessionCompletedCallback: ((Int) -> Unit)? = null

    private val _state = MutableStateFlow(FocusTimerState())
    val state: StateFlow<FocusTimerState> = _state.asStateFlow()

    private fun ensureChannel(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_FOCUS) == null) {
                val ch = NotificationChannel(
                    CHANNEL_FOCUS,
                    "Фокус-таймер (в реальном времени)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Отображает обратный отсчет фокус-таймера в уведомлениях"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun prepareIfIdle(context: Context, title: String, defaultMinutes: Int, onCompleted: ((Int) -> Unit)?) {
        ensureChannel(context)
        if (onCompleted != null) {
            onSessionCompletedCallback = onCompleted
        }
        val cur = _state.value
        if (!cur.isRunning && cur.secondsRemaining == cur.totalSeconds) {
            _state.value = FocusTimerState(
                title = title,
                selectedMinutes = defaultMinutes,
                totalSeconds = defaultMinutes * 60,
                secondsRemaining = defaultMinutes * 60,
                isRunning = false,
                isCompleted = false
            )
        } else if (title.isNotBlank() && title != "Фокус-сессия" && !cur.isRunning) {
            _state.value = cur.copy(title = title)
        }
    }

    fun setDurationMinutes(context: Context, minutes: Int, title: String? = null) {
        ensureChannel(context)
        val clamped = minutes.coerceIn(1, 180)
        tickerJob?.cancel()
        val cur = _state.value
        _state.value = cur.copy(
            title = title ?: cur.title,
            selectedMinutes = clamped,
            totalSeconds = clamped * 60,
            secondsRemaining = clamped * 60,
            isRunning = false,
            isCompleted = false
        )
        cancelNotification()
    }

    fun toggleStartPause(context: Context, title: String? = null) {
        ensureChannel(context)
        val cur = _state.value
        if (cur.isRunning) {
            pause()
        } else {
            val startSecs = if (cur.secondsRemaining <= 0) cur.selectedMinutes * 60 else cur.secondsRemaining
            val updated = cur.copy(
                title = title ?: cur.title,
                totalSeconds = if (cur.secondsRemaining <= 0) cur.selectedMinutes * 60 else cur.totalSeconds,
                secondsRemaining = startSecs,
                isRunning = true,
                isCompleted = false
            )
            _state.value = updated
            startTicker()
        }
    }

    fun pause() {
        tickerJob?.cancel()
        val cur = _state.value
        if (cur.isRunning) {
            _state.value = cur.copy(isRunning = false)
            postOngoingNotification(_state.value)
        }
    }

    fun reset(context: Context) {
        ensureChannel(context)
        tickerJob?.cancel()
        val cur = _state.value
        _state.value = cur.copy(
            totalSeconds = cur.selectedMinutes * 60,
            secondsRemaining = cur.selectedMinutes * 60,
            isRunning = false,
            isCompleted = false
        )
        cancelNotification()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        val initialRemaining = _state.value.secondsRemaining
        val targetElapsedMs = SystemClock.elapsedRealtime() + initialRemaining * 1000L

        postOngoingNotification(_state.value)

        tickerJob = scope.launch {
            while (_state.value.isRunning) {
                delay(1000L)
                val nowElapsed = SystemClock.elapsedRealtime()
                val remSec = ((targetElapsedMs - nowElapsed + 500L) / 1000L).toInt().coerceAtLeast(0)
                val current = _state.value
                if (remSec > 0) {
                    val nextState = current.copy(secondsRemaining = remSec)
                    _state.value = nextState
                    postOngoingNotification(nextState)
                } else {
                    val finishedState = current.copy(
                        secondsRemaining = 0,
                        isRunning = false,
                        isCompleted = true
                    )
                    _state.value = finishedState
                    onSessionCompletedCallback?.invoke(finishedState.selectedMinutes)
                    postCompletedNotification(finishedState)
                    break
                }
            }
        }
    }

    private fun hasNotificationPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun postOngoingNotification(st: FocusTimerState) {
        val ctx = appContext ?: return
        if (!hasNotificationPermission(ctx)) return

        val mins = st.secondsRemaining / 60
        val secs = st.secondsRemaining % 60
        val formatted = String.format("%02d:%02d", mins, secs)
        val statusLabel = if (st.isRunning) "Осталось $formatted" else "На паузе • $formatted"

        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx,
            77,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsed = (st.totalSeconds - st.secondsRemaining).coerceAtLeast(0)
        val notification = NotificationCompat.Builder(ctx, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🍅 ${st.title} ($formatted)")
            .setContentText(statusLabel)
            .setOngoing(st.isRunning)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(st.totalSeconds.coerceAtLeast(1), elapsed, false)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID_FOCUS, notification)
        } catch (_: SecurityException) {}
    }

    private fun postCompletedNotification(st: FocusTimerState) {
        val ctx = appContext ?: return
        if (!hasNotificationPermission(ctx)) return

        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx,
            77,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, NotificationHelper.CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Фокус-сессия завершена!")
            .setContentText("«${st.title}» (${st.selectedMinutes} мин) успешно выполнена.")
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID_FOCUS, notification)
        } catch (_: SecurityException) {}
    }

    private fun cancelNotification() {
        val ctx = appContext ?: return
        try {
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID_FOCUS)
        } catch (_: Exception) {}
    }
}
