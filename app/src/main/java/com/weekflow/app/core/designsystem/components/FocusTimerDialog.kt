package com.weekflow.app.core.designsystem.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.LocalIosColors
import kotlinx.coroutines.delay

@Composable
fun FocusTimerDialog(
    initialTitle: String = "Фокус-сессия",
    defaultMinutes: Int = 25,
    onDismiss: () -> Unit,
    onSessionCompleted: ((Int) -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current

    var selectedMinutes by remember { mutableIntStateOf(defaultMinutes) }
    var totalSeconds by remember { mutableIntStateOf(defaultMinutes * 60) }
    var secondsRemaining by remember { mutableIntStateOf(defaultMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
            if (secondsRemaining == 0) {
                isRunning = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSessionCompleted?.invoke(selectedMinutes)
            }
        }
    }

    fun setTimerMinutes(mins: Int) {
        selectedMinutes = mins
        totalSeconds = mins * 60
        secondsRemaining = mins * 60
        isRunning = false
    }

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) 1f - (secondsRemaining.toFloat() / totalSeconds.toFloat()) else 0f,
        label = "FocusProgress"
    )

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = iosColors.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🍅 Фокус-таймер",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = initialTitle,
                            fontSize = 13.sp,
                            color = iosColors.textSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Закрыть", tint = iosColors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preset cards (2-line layout to prevent text clipping)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(15, "15 мин", "Короткий"),
                        Triple(25, "25 мин", "Помодоро"),
                        Triple(45, "45 мин", "Урок")
                    ).forEach { (mins, topLabel, subLabel) ->
                        val isSelected = selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) IosBlue.copy(alpha = 0.15f) else iosColors.cardSecondary)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) IosBlue else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    setTimerMinutes(mins)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = topLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) IosBlue else iosColors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = subLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) IosBlue else iosColors.textSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Circular timer display
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(190.dp),
                        color = iosColors.cardSecondary,
                        strokeWidth = 10.dp
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(190.dp),
                        color = if (isRunning) IosOrange else IosBlue,
                        strokeWidth = 10.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        Text(
                            text = if (isRunning) "Идет концентрация" else if (secondsRemaining == 0) "Завершено! 🎉" else "На паузе",
                            fontSize = 12.sp,
                            color = if (isRunning) IosOrange else iosColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            setTimerMinutes(selectedMinutes)
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
                            isRunning = !isRunning
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) IosOrange else IosBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(150.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Пауза" else "Старт",
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
