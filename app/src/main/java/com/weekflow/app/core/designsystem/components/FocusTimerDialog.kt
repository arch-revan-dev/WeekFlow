package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.focus.FocusTimerManager

@Composable
fun FocusTimerDialog(
    initialTitle: String = "Фокус-сессия",
    defaultMinutes: Int = 25,
    onDismiss: () -> Unit,
    onSessionCompleted: ((Int) -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val timerState by FocusTimerManager.state.collectAsState()

    LaunchedEffect(initialTitle, defaultMinutes) {
        FocusTimerManager.prepareIfIdle(
            context = context,
            title = initialTitle,
            defaultMinutes = defaultMinutes,
            onCompleted = onSessionCompleted
        )
    }

    val progress by animateFloatAsState(
        targetValue = if (timerState.totalSeconds > 0) {
            1f - (timerState.secondsRemaining.toFloat() / timerState.totalSeconds.toFloat())
        } else 0f,
        label = "FocusProgress"
    )

    val minutes = timerState.secondsRemaining / 60
    val seconds = timerState.secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val minuteChoices = remember { (1..180).toList() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = iosColors.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🍅 Фокус-таймер",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = timerState.title,
                            fontSize = 13.sp,
                            color = iosColors.textSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Свернуть (таймер продолжит работу)",
                            tint = iosColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Wheel Duration Picker when timer is not actively running
                val isIdleAtStart = !timerState.isRunning && timerState.secondsRemaining == timerState.totalSeconds
                AnimatedVisibility(visible = isIdleAtStart) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Прокрутите для выбора времени (1–180 мин)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = iosColors.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(iosColors.cardSecondary.copy(alpha = 0.65f))
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        ) {
                            IosWheelPicker(
                                items = minuteChoices,
                                selectedIndex = (timerState.selectedMinutes - 1).coerceIn(0, minuteChoices.lastIndex),
                                onItemSelected = { _, mins ->
                                    FocusTimerManager.setDurationMinutes(context, mins, initialTitle)
                                },
                                itemLabel = { mins ->
                                    val hrs = mins / 60
                                    val rem = mins % 60
                                    if (hrs > 0) "$mins мин (${hrs} ч ${rem} м)" else "$mins мин"
                                },
                                visibleItemsCount = 3,
                                itemHeight = 36.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Circular timer display
                Box(
                    modifier = Modifier.size(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(176.dp),
                        color = iosColors.cardSecondary,
                        strokeWidth = 9.dp
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(176.dp),
                        color = if (timerState.isRunning) IosOrange else if (timerState.isCompleted) IosGreen else IosBlue,
                        strokeWidth = 9.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = when {
                                timerState.isRunning -> "Работает в фоне 🔔"
                                timerState.isCompleted -> "Завершено! 🎉"
                                timerState.secondsRemaining < timerState.totalSeconds -> "На паузе"
                                else -> "Готов к старту"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (timerState.isRunning) IosOrange else iosColors.textSecondary
                        )
                    }
                }

                if (timerState.isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = IosOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Можно закрыть окно — таймер виден в шторке уведомлений",
                            fontSize = 10.5.sp,
                            color = IosOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            FocusTimerManager.reset(context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Сброс",
                            tint = iosColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            FocusTimerManager.toggleStartPause(context, initialTitle)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState.isRunning) IosOrange else IosBlue
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .width(150.dp)
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (timerState.isRunning) "Пауза" else "Старт",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
