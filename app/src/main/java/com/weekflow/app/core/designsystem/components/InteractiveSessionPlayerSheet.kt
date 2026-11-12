package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosOrange
import com.weekflow.app.core.designsystem.IosPurple
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.SubtaskItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveSessionPlayerSheet(
    sessionTitle: String,
    sessionSubtitle: String,
    accentColor: Color,
    initialItems: List<SubtaskItem>,
    onUpdateItems: (List<SubtaskItem>) -> Unit,
    onAddFollowUpTask: ((String) -> Unit)? = null,
    onSessionCompleted: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current
    val haptic = LocalHapticFeedback.current
    val currentView = LocalView.current

    // Keep screen awake during interactive workout / session
    DisposableEffect(Unit) {
        val prevKeepScreenOn = currentView.keepScreenOn
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = prevKeepScreenOn
        }
    }

    var items by remember(initialItems) { mutableStateOf(initialItems) }
    var activeIndex by remember {
        mutableIntStateOf(initialItems.indexOfFirst { !it.isDone }.coerceAtLeast(0))
    }

    val activeItem = items.getOrNull(activeIndex)

    // Step Timer / Rest Timer state
    var isRestMode by remember(activeIndex) { mutableStateOf(false) }
    var isTimerRunning by remember(activeIndex) { mutableStateOf(false) }
    val defaultStepSec = (activeItem?.durationSeconds ?: 0).coerceAtLeast(0)
    var totalTimerSeconds by remember(activeIndex, activeItem?.durationSeconds) {
        mutableIntStateOf(if (defaultStepSec > 0) defaultStepSec else 60)
    }
    var remainingSeconds by remember(activeIndex, activeItem?.durationSeconds) {
        mutableIntStateOf(if (defaultStepSec > 0) defaultStepSec else 60)
    }
    var elapsedSessionSeconds by remember { mutableIntStateOf(0) }

    // Custom step builder state
    var showAddStepForm by remember { mutableStateOf(items.isEmpty()) }
    var newStepTitle by remember { mutableStateOf("") }
    var newStepSets by remember { mutableStateOf(if (appProfile == AppProfile.HEALTH) "3" else "") }
    var newStepReps by remember { mutableStateOf(if (appProfile == AppProfile.HEALTH) "12 повт" else "") }
    var newStepDurationSec by remember { mutableStateOf("") }
    var newStepRestSec by remember { mutableStateOf(if (appProfile == AppProfile.HEALTH) "60" else "") }
    var newStepNote by remember { mutableStateOf("") }

    var followUpText by remember { mutableStateOf("") }
    var showCustomTimerWheel by remember { mutableStateOf(false) }

    // 1-second ticker for active exercise timer or rest timer
    LaunchedEffect(isTimerRunning, remainingSeconds, isRestMode) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
            elapsedSessionSeconds += 1
            if (remainingSeconds == 0) {
                isTimerRunning = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isRestMode) {
                    isRestMode = false
                    val nextDur = activeItem?.durationSeconds ?: 0
                    if (nextDur > 0) {
                        totalTimerSeconds = nextDur
                        remainingSeconds = nextDur
                    }
                }
            }
        }
    }

    fun updateAndPersist(updated: List<SubtaskItem>) {
        items = updated
        onUpdateItems(updated)
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
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = appProfile.title,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        val doneCount = items.count { it.isDone }
                        if (items.isNotEmpty()) {
                            Text(
                                text = "Выполнено $doneCount из ${items.size}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = sessionTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sessionSubtitle.isNotBlank()) {
                        Text(
                            text = sessionSubtitle,
                            fontSize = 12.sp,
                            color = iosColors.textSecondary
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

            // Active Exercise / Step Interactive Stage Card
            if (activeItem != null) {
                val targetSets = (activeItem.sets ?: 1).coerceAtLeast(1)
                val currentSetNumber = (activeItem.completedSets + 1).coerceAtMost(targetSets)
                val progress = if (totalTimerSeconds > 0) {
                    remainingSeconds.toFloat() / totalTimerSeconds.toFloat()
                } else 0f
                val animatedProgress by animateFloatAsState(targetValue = progress, label = "StepTimerProgress")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isRestMode) IosOrange.copy(alpha = 0.12f)
                            else accentColor.copy(alpha = 0.10f)
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (isRestMode) IosOrange.copy(alpha = 0.55f) else accentColor.copy(alpha = 0.45f)
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Top badge row: Step index & Sets badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRestMode) IosOrange else accentColor)
                                    .padding(horizontal = 9.dp, vertical = 3.5.dp)
                            ) {
                                Text(
                                    text = if (isRestMode) {
                                        "⏸ ОТДЫХ МЕЖДУ ПОДХОДАМИ"
                                    } else if ((activeItem.sets ?: 0) > 1) {
                                        "ПОДХОД $currentSetNumber ИЗ $targetSets"
                                    } else {
                                        "${appProfile.sessionItemTerm.uppercase()} ${activeIndex + 1}/${items.size}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (!activeItem.reps.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(iosColors.card)
                                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                                ) {
                                    Text(
                                        text = "🎯 ${activeItem.reps}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iosColors.textPrimary
                                    )
                                }
                            }
                        }

                        // Active Step Title
                        Text(
                            text = if (isRestMode) "Восстановление дыхания • След.: ${activeItem.title}" else activeItem.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )

                        if (!activeItem.weightOrNote.isNullOrBlank()) {
                            Text(
                                text = "${appProfile.weightOrNoteLabel}: ${activeItem.weightOrNote}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }

                        // Set dots indicator when sets > 1
                        if (targetSets > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (setIdx in 1..targetSets) {
                                    val isSetCompleted = setIdx <= activeItem.completedSets
                                    val isCurrentSet = setIdx == currentSetNumber && !activeItem.isDone
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrentSet) 30.dp else 26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSetCompleted -> IosGreen
                                                    isCurrentSet -> accentColor.copy(alpha = 0.22f)
                                                    else -> iosColors.card
                                                }
                                            )
                                            .border(
                                                width = if (isCurrentSet) 2.dp else 1.dp,
                                                color = if (isSetCompleted) IosGreen else if (isCurrentSet) accentColor else iosColors.separator,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSetCompleted) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Text(
                                                text = setIdx.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentSet) accentColor else iosColors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Timer Block (always available for timed exercises, rest, or timeboxes)
                        val showTimerBlock = isRestMode || (activeItem.durationSeconds ?: 0) > 0 || showCustomTimerWheel || isTimerRunning
                        if (showTimerBlock) {
                            val mins = remainingSeconds / 60
                            val secs = remainingSeconds % 60
                            val formattedClock = String.format("%02d:%02d", mins, secs)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(iosColors.card)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier.size(52.dp),
                                            color = iosColors.separator.copy(alpha = 0.25f),
                                            strokeWidth = 5.dp
                                        )
                                        CircularProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier.size(52.dp),
                                            color = if (isRestMode) IosOrange else IosPurple,
                                            strokeWidth = 5.dp,
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text(
                                            text = formattedClock,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iosColors.textPrimary
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (isRestMode) "Таймер отдыха" else "Таймер упражнения / этапа",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isRestMode) IosOrange else iosColors.textPrimary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(iosColors.cardSecondary)
                                                    .clickable {
                                                        val nextVal = (remainingSeconds - 15).coerceAtLeast(5)
                                                        remainingSeconds = nextVal
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text("-15с", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = iosColors.textSecondary)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(iosColors.cardSecondary)
                                                    .clickable {
                                                        remainingSeconds += 15
                                                        if (remainingSeconds > totalTimerSeconds) totalTimerSeconds = remainingSeconds
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text("+15с", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
                                            }
                                            if (isRestMode) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(IosOrange.copy(alpha = 0.16f))
                                                        .clickable {
                                                            isTimerRunning = false
                                                            isRestMode = false
                                                            val nextDur = activeItem.durationSeconds ?: 60
                                                            totalTimerSeconds = nextDur
                                                            remainingSeconds = nextDur
                                                        }
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Пропустить", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosOrange)
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            isTimerRunning = false
                                            remainingSeconds = totalTimerSeconds
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Сброс",
                                            tint = iosColors.textSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isTimerRunning) IosOrange else IosBlue)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                isTimerRunning = !isTimerRunning
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isTimerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Старт/Пауза",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            // Button to open custom timer wheel for this exercise
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(iosColors.card)
                                    .clickable { showCustomTimerWheel = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = IosBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Включить секундомер / таймер для упражнения",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosBlue
                                )
                            }
                        }

                        // Primary Action Buttons: Complete Set / Complete Exercise & Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val newCompletedSets = activeItem.completedSets + 1
                                    if (newCompletedSets < targetSets) {
                                        // Advance set and start rest timer automatically!
                                        val updatedList = items.toMutableList()
                                        updatedList[activeIndex] = activeItem.copy(completedSets = newCompletedSets)
                                        updateAndPersist(updatedList)
                                        val restSec = (activeItem.restSeconds ?: 60).coerceAtLeast(15)
                                        isRestMode = true
                                        totalTimerSeconds = restSec
                                        remainingSeconds = restSec
                                        isTimerRunning = true
                                    } else {
                                        // Complete entire exercise/step and move to next!
                                        isTimerRunning = false
                                        isRestMode = false
                                        val updatedList = items.toMutableList()
                                        updatedList[activeIndex] = activeItem.copy(
                                            completedSets = targetSets,
                                            isDone = true
                                        )
                                        updateAndPersist(updatedList)
                                        val nextIncomplete = updatedList.indexOfFirst { !it.isDone }
                                        if (nextIncomplete >= 0) {
                                            activeIndex = nextIncomplete
                                        } else {
                                            onSessionCompleted?.invoke((elapsedSessionSeconds / 60).coerceAtLeast(1))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeItem.completedSets + 1 < targetSets) accentColor else IosGreen
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeItem.completedSets + 1 < targetSets) {
                                        "Завершить подход $currentSetNumber/$targetSets"
                                    } else {
                                        "Выполнено • Далее"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }

                            if (items.size > 1) {
                                IconButton(
                                    onClick = {
                                        isTimerRunning = false
                                        isRestMode = false
                                        activeIndex = (activeIndex + 1) % items.size
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(iosColors.card)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Следующее",
                                        tint = iosColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick 1-Tap Profile Templates Row
            Text(
                text = "Быстрые шаблоны (${appProfile.title}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                appProfile.defaultStepTemplates.forEach { tpl ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(9.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val created = SubtaskItem(
                                    title = tpl.title,
                                    sets = tpl.sets,
                                    reps = tpl.reps,
                                    durationSeconds = tpl.durationSeconds,
                                    restSeconds = tpl.restSeconds,
                                    weightOrNote = tpl.weightOrNote
                                )
                                val updated = items + created
                                updateAndPersist(updated)
                                if (items.isEmpty()) activeIndex = 0
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tpl.chipLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(IosBlue.copy(alpha = 0.14f))
                        .clickable { showAddStepForm = !showAddStepForm }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showAddStepForm) "Скрыть конструктор" else "+ Своё ${appProfile.sessionItemTerm.lowercase()}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue
                    )
                }
            }

            // Custom Exercise / Step Builder Form
            AnimatedVisibility(visible = showAddStepForm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(iosColors.cardSecondary)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Добавить ${appProfile.sessionItemTerm.lowercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary
                    )
                    OutlinedTextField(
                        value = newStepTitle,
                        onValueChange = { newStepTitle = it },
                        label = { Text(appProfile.sessionItemTerm) },
                        placeholder = { Text(appProfile.stepPlaceholder, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newStepSets,
                            onValueChange = { newStepSets = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Подходы") },
                            placeholder = { Text("3") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newStepReps,
                            onValueChange = { newStepReps = it },
                            label = { Text("Повторы / Объем") },
                            placeholder = { Text("12 повт") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newStepDurationSec,
                            onValueChange = { newStepDurationSec = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Таймер (сек)") },
                            placeholder = { Text("60") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newStepRestSec,
                            onValueChange = { newStepRestSec = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Отдых (сек)") },
                            placeholder = { Text("90") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newStepNote,
                        onValueChange = { newStepNote = it },
                        label = { Text(appProfile.weightOrNoteLabel) },
                        placeholder = { Text(appProfile.weightOrNotePlaceholder, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddStepForm = false }) {
                            Text("Отмена", color = iosColors.textSecondary)
                        }
                        Button(
                            onClick = {
                                if (newStepTitle.isNotBlank()) {
                                    val created = SubtaskItem(
                                        title = newStepTitle.trim(),
                                        sets = newStepSets.toIntOrNull()?.takeIf { it > 0 },
                                        reps = newStepReps.trim().takeIf { it.isNotBlank() },
                                        durationSeconds = newStepDurationSec.toIntOrNull()?.takeIf { it > 0 },
                                        restSeconds = newStepRestSec.toIntOrNull()?.takeIf { it > 0 },
                                        weightOrNote = newStepNote.trim().takeIf { it.isNotBlank() }
                                    )
                                    val updated = items + created
                                    updateAndPersist(updated)
                                    newStepTitle = ""
                                    showAddStepForm = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить")
                        }
                    }
                }
            }

            // Full List of Exercises / Session Steps
            if (items.isNotEmpty()) {
                Text(
                    text = appProfile.sessionSectionTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEachIndexed { idx, step ->
                        val isCurrent = idx == activeIndex
                        val summary = step.formatStepSummary()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) accentColor.copy(alpha = 0.12f)
                                    else iosColors.cardSecondary
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isCurrent) 1.5.dp else 0.dp,
                                        color = if (isCurrent) accentColor else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isTimerRunning = false
                                    isRestMode = false
                                    activeIndex = idx
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (step.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (step.isDone) IosGreen else if (isCurrent) accentColor else iosColors.textSecondary,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .clickable {
                                            val updated = items.toMutableList()
                                            val nextDone = !step.isDone
                                            updated[idx] = step.copy(
                                                isDone = nextDone,
                                                completedSets = if (nextDone) (step.sets ?: 1) else 0
                                            )
                                            updateAndPersist(updated)
                                        }
                                )
                                Column {
                                    Text(
                                        text = "${idx + 1}. ${step.title}",
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (step.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                        textDecoration = if (step.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (summary.isNotBlank()) {
                                        Text(
                                            text = summary,
                                            fontSize = 11.5.sp,
                                            color = if (isCurrent) accentColor else iosColors.textSecondary
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = {
                                    val updated = items.toMutableList().apply { removeAt(idx) }
                                    updateAndPersist(updated)
                                    if (activeIndex >= updated.size) {
                                        activeIndex = (updated.size - 1).coerceAtLeast(0)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Удалить",
                                    tint = iosColors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Follow-Up Task Creator at bottom of player
            if (onAddFollowUpTask != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(iosColors.cardSecondary)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("➕ Быстрая заметка / задача по итогам...", fontSize = 12.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (followUpText.isNotBlank()) {
                                onAddFollowUpTask(followUpText.trim())
                                followUpText = ""
                            }
                        },
                        enabled = followUpText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                    ) {
                        Text("+ В задачи", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
