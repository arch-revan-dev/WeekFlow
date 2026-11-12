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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.domain.model.SubtaskItem
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class WorkoutPhase {
    IDLE,
    WORK,
    REST,
    FINISHED
}

private fun pluralizeRu(count: Int, one: String, few: String, many: String): String {
    val absCount = abs(count)
    val mod100 = absCount % 100
    val mod10 = absCount % 10
    val word = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$count $word"
}

private fun formatDurationRu(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return when {
        mins > 0 && secs > 0 -> "$mins мин $secs сек"
        mins > 0 -> "$mins мин"
        else -> "$secs сек"
    }
}

private fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format("%02d:%02d", safe / 60, safe % 60)
}

private fun formatExerciseMeta(item: SubtaskItem): String {
    val parts = mutableListOf<String>()
    val sets = item.sets ?: 0
    if (sets > 0) {
        if (!item.reps.isNullOrBlank()) {
            parts.add("$sets × ${item.reps}")
        } else {
            parts.add(pluralizeRu(sets, "подход", "подхода", "подходов"))
        }
    } else if (!item.reps.isNullOrBlank()) {
        parts.add(item.reps)
    }
    if ((item.durationSeconds ?: 0) > 0) {
        parts.add("работа ${formatDurationRu(item.durationSeconds!!)}")
    }
    if ((item.restSeconds ?: 0) > 0) {
        parts.add("отдых ${formatDurationRu(item.restSeconds!!)}")
    }
    return parts.joinToString(" • ")
}

@Composable
private fun TimeWheelPairCard(
    title: String,
    minutes: Int,
    seconds: Int,
    minuteOptions: List<Int>,
    secondOptions: List<Int>,
    zeroBadgeText: String,
    accentColor: Color,
    onMinutesSelected: (Int) -> Unit,
    onSecondsSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current
    val totalSec = minutes * 60 + seconds
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(iosColors.card)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            Text(
                text = if (totalSec > 0) formatClock(totalSec) else zeroBadgeText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalSec > 0) accentColor else iosColors.textSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val minIdx = minuteOptions.indexOf(minutes).coerceAtLeast(0)
            val secIdx = secondOptions.indices.minByOrNull { abs(secondOptions[it] - seconds) } ?: 0
            IosWheelPicker(
                items = minuteOptions,
                selectedIndex = minIdx,
                onItemSelected = { _, m -> onMinutesSelected(m) },
                itemLabel = { m -> String.format("%02d", m) },
                visibleItemsCount = 3,
                itemHeight = 34.dp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ":",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = iosColors.textPrimary
            )
            IosWheelPicker(
                items = secondOptions,
                selectedIndex = secIdx,
                onItemSelected = { _, s -> onSecondsSelected(s) },
                itemLabel = { s -> String.format("%02d", s) },
                visibleItemsCount = 3,
                itemHeight = 34.dp,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("мин", fontSize = 10.5.sp, color = iosColors.textSecondary)
            Text("сек", fontSize = 10.5.sp, color = iosColors.textSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlayerSheet(
    sessionTitle: String,
    sessionSubtitle: String,
    accentColor: Color,
    initialItems: List<SubtaskItem>,
    onUpdateItems: (List<SubtaskItem>) -> Unit,
    onSessionCompleted: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val haptic = LocalHapticFeedback.current
    val currentView = LocalView.current

    DisposableEffect(Unit) {
        val prevKeepScreenOn = currentView.keepScreenOn
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = prevKeepScreenOn
        }
    }

    val setOptions = remember { (1..20).toList() }
    val repOptions = remember { (0..100).toList() }
    val workMinuteOptions = remember { (0..30).toList() }
    val restMinuteOptions = remember { (0..15).toList() }
    val secondOptions = remember { (0..55 step 5).toList() }

    var items by remember(initialItems) { mutableStateOf(initialItems) }
    var phase by remember { mutableStateOf(WorkoutPhase.IDLE) }
    var activeIndex by remember {
        mutableIntStateOf(initialItems.indexOfFirst { !it.isDone }.coerceAtLeast(0))
    }
    var currentSet by remember { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }

    // WORK timer state: countdown when workTotalSec > 0, manual stopwatch otherwise
    var workTotalSec by remember { mutableIntStateOf(0) }
    var workRemainingSec by remember { mutableIntStateOf(0) }
    var workElapsedSec by remember { mutableIntStateOf(0) }

    // REST timer state
    var restTotalSec by remember { mutableIntStateOf(0) }
    var restRemainingSec by remember { mutableIntStateOf(0) }
    var pendingNextIndex by remember { mutableIntStateOf(0) }
    var pendingNextSet by remember { mutableIntStateOf(1) }

    var totalElapsedSeconds by remember { mutableIntStateOf(0) }

    // Inline "+ Упражнение" / Edit exercise form state
    var showExerciseForm by remember { mutableStateOf(items.isEmpty()) }
    var editingExerciseId by remember { mutableStateOf<String?>(null) }
    var formTitle by remember { mutableStateOf("") }
    var formSets by remember { mutableIntStateOf(3) }
    var formReps by remember { mutableIntStateOf(0) }
    var formWorkMin by remember { mutableIntStateOf(0) }
    var formWorkSec by remember { mutableIntStateOf(0) }
    var formRestMin by remember { mutableIntStateOf(1) }
    var formRestSec by remember { mutableIntStateOf(0) }

    fun parseRepsInt(reps: String?): Int {
        if (reps.isNullOrBlank()) return 0
        val value = Regex("\\d+").find(reps)?.value?.toIntOrNull() ?: return 0
        return value.coerceIn(0, 100)
    }

    fun openAddForm() {
        editingExerciseId = null
        formTitle = ""
        formSets = 3
        formReps = 0
        formWorkMin = 0
        formWorkSec = 0
        formRestMin = 1
        formRestSec = 0
        showExerciseForm = true
    }

    fun openEditForm(item: SubtaskItem) {
        editingExerciseId = item.id
        formTitle = item.title
        formSets = (item.sets ?: 3).coerceIn(1, 20)
        formReps = parseRepsInt(item.reps)
        val work = (item.durationSeconds ?: 0).coerceAtLeast(0)
        formWorkMin = (work / 60).coerceIn(0, 30)
        formWorkSec = ((work % 60 + 2) / 5 * 5).coerceIn(0, 55)
        val rest = (item.restSeconds ?: 0).coerceAtLeast(0)
        formRestMin = (rest / 60).coerceIn(0, 15)
        formRestSec = ((rest % 60 + 2) / 5 * 5).coerceIn(0, 55)
        showExerciseForm = true
    }

    fun closeForm() {
        showExerciseForm = false
        editingExerciseId = null
    }

    fun persistItems(updated: List<SubtaskItem>) {
        items = updated
        onUpdateItems(updated)
    }

    fun startWorkAt(index: Int, setNumber: Int, sourceList: List<SubtaskItem>) {
        val target = sourceList.getOrNull(index) ?: return
        activeIndex = index
        currentSet = setNumber
        val dur = (target.durationSeconds ?: 0).coerceAtLeast(0)
        workTotalSec = dur
        workRemainingSec = dur
        workElapsedSec = 0
        isPaused = false
        phase = WorkoutPhase.WORK
    }

    fun startWorkout() {
        if (items.isEmpty()) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (items.all { it.isDone }) {
            val resetList = items.map { it.copy(isDone = false, completedSets = 0) }
            persistItems(resetList)
            startWorkAt(0, 1, resetList)
        } else {
            val firstIdx = items.indexOfFirst { !it.isDone }.coerceAtLeast(0)
            val firstItem = items[firstIdx]
            val targetSets = (firstItem.sets ?: 1).coerceAtLeast(1)
            val nextSet = (firstItem.completedSets + 1).coerceIn(1, targetSets)
            startWorkAt(firstIdx, nextSet, items)
        }
    }

    fun completeCurrentApproach() {
        val current = items.getOrNull(activeIndex) ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        val targetSets = (current.sets ?: 1).coerceAtLeast(1)
        val doneSets = currentSet.coerceIn(1, targetSets)
        val exerciseDone = doneSets >= targetSets

        val updated = items.toMutableList().apply {
            this[activeIndex] = current.copy(
                completedSets = doneSets,
                isDone = exerciseDone
            )
        }
        persistItems(updated)

        val nextIdx: Int
        val nextSet: Int
        if (!exerciseDone) {
            nextIdx = activeIndex
            nextSet = doneSets + 1
        } else {
            val foundNext = (activeIndex + 1 until updated.size).firstOrNull { !updated[it].isDone }
                ?: updated.indexOfFirst { !it.isDone }
            if (foundNext < 0) {
                isPaused = false
                phase = WorkoutPhase.FINISHED
                onSessionCompleted?.invoke((totalElapsedSeconds / 60).coerceAtLeast(1))
                return
            }
            nextIdx = foundNext
            val nextTargetSets = (updated[nextIdx].sets ?: 1).coerceAtLeast(1)
            nextSet = (updated[nextIdx].completedSets + 1).coerceIn(1, nextTargetSets)
        }

        val restSec = (current.restSeconds ?: 0).coerceAtLeast(0)
        if (restSec > 0) {
            pendingNextIndex = nextIdx
            pendingNextSet = nextSet
            restTotalSec = restSec
            restRemainingSec = restSec
            isPaused = false
            phase = WorkoutPhase.REST
        } else {
            startWorkAt(nextIdx, nextSet, updated)
        }
    }

    fun skipRest() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        startWorkAt(pendingNextIndex, pendingNextSet, items)
    }

    // 1-second state machine ticker
    LaunchedEffect(phase, isPaused, workRemainingSec, workElapsedSec, restRemainingSec) {
        if (isPaused) return@LaunchedEffect
        when (phase) {
            WorkoutPhase.WORK -> {
                delay(1_000L)
                totalElapsedSeconds += 1
                if (workTotalSec > 0) {
                    if (workRemainingSec > 1) {
                        workRemainingSec -= 1
                    } else {
                        workRemainingSec = 0
                        completeCurrentApproach()
                    }
                } else {
                    workElapsedSec += 1
                }
            }
            WorkoutPhase.REST -> {
                delay(1_000L)
                totalElapsedSeconds += 1
                if (restRemainingSec > 1) {
                    restRemainingSec -= 1
                } else {
                    restRemainingSec = 0
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    startWorkAt(pendingNextIndex, pendingNextSet, items)
                }
            }
            else -> Unit
        }
    }

    val activeItem = items.getOrNull(activeIndex)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sessionTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitleParts = buildList {
                        if (sessionSubtitle.isNotBlank()) add(sessionSubtitle)
                        if (items.isNotEmpty()) {
                            add("${items.count { it.isDone }}/${items.size}")
                        }
                    }
                    if (subtitleParts.isNotEmpty()) {
                        Text(
                            text = subtitleParts.joinToString(" • "),
                            fontSize = 12.5.sp,
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

            // State Machine Stage Card
            when (phase) {
                WorkoutPhase.IDLE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(iosColors.cardSecondary)
                            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (items.isEmpty()) {
                            Text(
                                text = "Добавьте упражнения ниже",
                                fontSize = 14.sp,
                                color = iosColors.textSecondary
                            )
                        } else {
                            val totalSetsCount = items.sumOf { (it.sets ?: 1).coerceAtLeast(1) }
                            val exLabel = pluralizeRu(items.size, "упражнение", "упражнения", "упражнений")
                            val setsLabel = pluralizeRu(totalSetsCount, "подход", "подхода", "подходов")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$exLabel • $setsLabel",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textPrimary
                                )
                                if (totalElapsedSeconds > 0) {
                                    Text(
                                        text = formatClock(totalElapsedSeconds),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                            Button(
                                onClick = ::startWorkout,
                                shape = RoundedCornerShape(12.dp),
                                colors = iosPrimaryButtonColors(accentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (items.all { it.isDone }) "Начать заново" else "Старт тренировки",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                WorkoutPhase.WORK -> {
                    if (activeItem != null) {
                        val targetSets = (activeItem.sets ?: 1).coerceAtLeast(1)
                        val isCountdown = workTotalSec > 0
                        val progressTarget = if (isCountdown) {
                            workRemainingSec.toFloat() / workTotalSec.toFloat()
                        } else 1f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressTarget,
                            label = "WorkProgress"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentColor.copy(alpha = 0.09f))
                                .border(BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor)
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Подход $currentSet из $targetSets",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${activeIndex + 1}/${items.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textSecondary
                                )
                            }

                            Text(
                                text = activeItem.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!activeItem.reps.isNullOrBlank()) {
                                Text(
                                    text = activeItem.reps,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }

                            if (targetSets > 1) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (s in 1..targetSets) {
                                        val isDoneSet = s < currentSet
                                        val isNow = s == currentSet
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isDoneSet -> accentColor
                                                        isNow -> accentColor.copy(alpha = 0.20f)
                                                        else -> iosColors.card
                                                    }
                                                )
                                                .border(
                                                    width = if (isNow) 1.5.dp else 1.dp,
                                                    color = if (isDoneSet || isNow) accentColor else iosColors.separator,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDoneSet) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = s.toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isNow) accentColor else iosColors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(96.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.size(96.dp),
                                    color = iosColors.separator.copy(alpha = 0.35f),
                                    strokeWidth = 6.dp
                                )
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(96.dp),
                                    color = if (isPaused) accentColor.copy(alpha = 0.45f) else accentColor,
                                    strokeWidth = 6.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = formatClock(if (isCountdown) workRemainingSec else workElapsedSec),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isPaused = !isPaused
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPaused) "Старт" else "Пауза",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }

                                Button(
                                    onClick = ::completeCurrentApproach,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = iosPrimaryButtonColors(accentColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Закончить подход",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                WorkoutPhase.REST -> {
                    val nextExercise = items.getOrNull(pendingNextIndex)
                    val nextTargetSets = (nextExercise?.sets ?: 1).coerceAtLeast(1)
                    val restProgressTarget = if (restTotalSec > 0) {
                        restRemainingSec.toFloat() / restTotalSec.toFloat()
                    } else 0f
                    val animatedRestProgress by animateFloatAsState(
                        targetValue = restProgressTarget,
                        label = "RestProgress"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(iosColors.cardSecondary)
                            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Перерыв",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        if (nextExercise != null) {
                            Text(
                                text = "Далее: ${nextExercise.title} • Подход $pendingNextSet из $nextTargetSets",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(96.dp),
                                color = iosColors.separator.copy(alpha = 0.35f),
                                strokeWidth = 6.dp
                            )
                            CircularProgressIndicator(
                                progress = { animatedRestProgress },
                                modifier = Modifier.size(96.dp),
                                color = accentColor,
                                strokeWidth = 6.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = formatClock(restRemainingSec),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isPaused = !isPaused },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, iosColors.separator),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null,
                                    tint = iosColors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = ::skipRest,
                                shape = RoundedCornerShape(12.dp),
                                colors = iosPrimaryButtonColors(accentColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Пропустить перерыв",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                WorkoutPhase.FINISHED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.09f))
                            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Тренировка завершена",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosColors.textPrimary
                        )
                        if (totalElapsedSeconds > 0) {
                            Text(
                                text = "Время: ${formatClock(totalElapsedSeconds)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val resetList = items.map { it.copy(isDone = false, completedSets = 0) }
                                    persistItems(resetList)
                                    totalElapsedSeconds = 0
                                    phase = WorkoutPhase.IDLE
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Заново", color = iosColors.textPrimary)
                            }
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                colors = iosPrimaryButtonColors(accentColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Готово", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Exercise List Header + Single "+ Упражнение" Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Упражнения",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .clickable {
                            if (showExerciseForm) closeForm() else openAddForm()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showExerciseForm) "Скрыть" else "+ Упражнение",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            // Compact Add / Edit Exercise Form with iOS Wheel Pickers
            AnimatedVisibility(visible = showExerciseForm) {
                key(editingExerciseId) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(iosColors.cardSecondary)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formTitle,
                            onValueChange = { formTitle = it },
                            label = { Text("Упражнение") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Sets Wheel
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iosColors.card)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Подходы",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textSecondary
                                    )
                                    Text(
                                        text = formSets.toString(),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                IosWheelPicker(
                                    items = setOptions,
                                    selectedIndex = (formSets - 1).coerceIn(0, setOptions.lastIndex),
                                    onItemSelected = { _, s -> formSets = s },
                                    itemLabel = { it.toString() },
                                    visibleItemsCount = 3,
                                    itemHeight = 34.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Reps Wheel (0 = "выкл", 1..100)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iosColors.card)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Повторы",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textSecondary
                                    )
                                    Text(
                                        text = if (formReps == 0) "выкл" else formReps.toString(),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (formReps == 0) iosColors.textSecondary else accentColor
                                    )
                                }
                                IosWheelPicker(
                                    items = repOptions,
                                    selectedIndex = formReps.coerceIn(0, repOptions.lastIndex),
                                    onItemSelected = { _, r -> formReps = r },
                                    itemLabel = { if (it == 0) "выкл" else it.toString() },
                                    visibleItemsCount = 3,
                                    itemHeight = 34.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeWheelPairCard(
                                title = "Работа",
                                minutes = formWorkMin,
                                seconds = formWorkSec,
                                minuteOptions = workMinuteOptions,
                                secondOptions = secondOptions,
                                zeroBadgeText = "таймер выкл",
                                accentColor = accentColor,
                                onMinutesSelected = { formWorkMin = it },
                                onSecondsSelected = { formWorkSec = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimeWheelPairCard(
                                title = "Отдых",
                                minutes = formRestMin,
                                seconds = formRestSec,
                                minuteOptions = restMinuteOptions,
                                secondOptions = secondOptions,
                                zeroBadgeText = "без отдыха",
                                accentColor = accentColor,
                                onMinutesSelected = { formRestMin = it },
                                onSecondsSelected = { formRestSec = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = ::closeForm) {
                                Text("Отмена", color = iosColors.textSecondary)
                            }
                            Button(
                                onClick = {
                                    val title = formTitle.trim()
                                    if (title.isNotEmpty()) {
                                        val workTotal = (formWorkMin * 60 + formWorkSec).takeIf { it > 0 }
                                        val restTotal = (formRestMin * 60 + formRestSec).takeIf { it > 0 }
                                        val repsClean = if (formReps == 0) null else pluralizeRu(formReps, "повтор", "повтора", "повторов")
                                        val targetEditId = editingExerciseId
                                        if (targetEditId != null) {
                                            val updated = items.map { existing ->
                                                if (existing.id == targetEditId) {
                                                    existing.copy(
                                                        title = title,
                                                        sets = formSets,
                                                        reps = repsClean,
                                                        durationSeconds = workTotal,
                                                        restSeconds = restTotal
                                                    )
                                                } else existing
                                            }
                                            persistItems(updated)
                                        } else {
                                            val created = SubtaskItem(
                                                title = title,
                                                sets = formSets,
                                                reps = repsClean,
                                                durationSeconds = workTotal,
                                                restSeconds = restTotal
                                            )
                                            persistItems(items + created)
                                        }
                                        closeForm()
                                    }
                                },
                                enabled = formTitle.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = iosPrimaryButtonColors(accentColor)
                            ) {
                                if (editingExerciseId == null) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (editingExerciseId == null) "Добавить" else "Сохранить",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Clean Exercise List
            if (items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEachIndexed { idx, exercise ->
                        val isCurrent = phase != WorkoutPhase.IDLE && idx == activeIndex
                        val meta = formatExerciseMeta(exercise)
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
                                        width = if (isCurrent) 1.2.dp else 0.dp,
                                        color = if (isCurrent) accentColor else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (phase == WorkoutPhase.IDLE) {
                                        activeIndex = idx
                                    } else {
                                        val targetSets = (exercise.sets ?: 1).coerceAtLeast(1)
                                        val nextSet = (exercise.completedSets + 1).coerceIn(1, targetSets)
                                        startWorkAt(idx, nextSet, items)
                                    }
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
                                    imageVector = if (exercise.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (exercise.isDone || isCurrent) accentColor else iosColors.textSecondary,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .clickable {
                                            val updated = items.toMutableList()
                                            val nextDone = !exercise.isDone
                                            updated[idx] = exercise.copy(
                                                isDone = nextDone,
                                                completedSets = if (nextDone) (exercise.sets ?: 1) else 0
                                            )
                                            persistItems(updated)
                                        }
                                )
                                Column {
                                    Text(
                                        text = "${idx + 1}. ${exercise.title}",
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (exercise.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                        textDecoration = if (exercise.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (meta.isNotBlank()) {
                                        Text(
                                            text = meta,
                                            fontSize = 11.5.sp,
                                            color = if (isCurrent) accentColor else iosColors.textSecondary
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { openEditForm(exercise) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Изменить",
                                        tint = iosColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val updated = items.toMutableList().apply { removeAt(idx) }
                                        if (editingExerciseId == exercise.id) closeForm()
                                        persistItems(updated)
                                        if (updated.isEmpty()) {
                                            phase = WorkoutPhase.IDLE
                                            activeIndex = 0
                                        } else if (activeIndex >= updated.size) {
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
            }
        }
    }
}
