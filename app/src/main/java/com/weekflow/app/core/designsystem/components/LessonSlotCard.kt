package com.weekflow.app.core.designsystem.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.domain.model.AppProfile
import com.weekflow.app.domain.model.CardDensity
import com.weekflow.app.domain.model.ScheduleCardStyle
import com.weekflow.app.domain.model.SlotWithDetails
import com.weekflow.app.domain.model.SubtaskItem
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.WeekCycle
import java.time.Duration
import java.time.LocalTime

@Composable
fun LessonSlotCard(
    details: SlotWithDetails,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddTaskClick: () -> Unit = {},
    onTaskToggle: (Task) -> Unit = {},
    onTaskClick: ((Task) -> Unit)? = null,
    cardStyle: ScheduleCardStyle = ScheduleCardStyle.OUTLINED,
    cardDensity: CardDensity = CardDensity.STANDARD,
    cornerRadiusDp: Int = 14,
    showProgressBar: Boolean = true,
    isTwoWeekCycleEnabled: Boolean = false,
    isToday: Boolean = false,
    onStartFocusTimer: (() -> Unit)? = null,
    onStartInteractiveSession: (() -> Unit)? = null
) {
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current
    val isDark = iosColors.isDark
    val haptic = LocalHapticFeedback.current

    val activityColor = ColorParser.parseHexColor(details.activity.colorHex)
    val cornerShape = RoundedCornerShape(cornerRadiusDp.dp)

    // Calculate if slot is currently ongoing (strictly on today's date)
    val now = LocalTime.now()
    val startTime = details.slot.parseStartTime()
    val endTime = details.slot.parseEndTime()
    val isOngoing = isToday && !now.isBefore(startTime) && !now.isAfter(endTime)

    val progressFraction = if (isOngoing) {
        val totalMins = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(1)
        val elapsedMins = Duration.between(startTime, now).toMinutes().coerceIn(0, totalMins)
        elapsedMins.toFloat() / totalMins.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "LessonProgress")

    // Density padding
    val verticalPadding = cardDensity.verticalPaddingDp.dp
    val baseSurface = if (isDark) Color(0xFF222227) else Color(0xFFFFFFFF)

    // Style-specific card background and border
    val cardBackground: Color
    val cardBorder: BorderStroke?

    when (cardStyle) {
        ScheduleCardStyle.OUTLINED -> {
            cardBackground = activityColor
                .copy(alpha = if (isDark) 0.07f else 0.035f)
                .compositeOver(baseSurface)
            cardBorder = BorderStroke(
                width = 1.6.dp,
                color = activityColor.copy(alpha = if (isDark) 0.85f else 0.78f)
            )
        }
        ScheduleCardStyle.LEFT_ACCENT -> {
            cardBackground = activityColor
                .copy(alpha = if (isDark) 0.06f else 0.03f)
                .compositeOver(baseSurface)
            cardBorder = BorderStroke(
                width = 1.dp,
                color = activityColor.copy(alpha = if (isDark) 0.28f else 0.20f)
            )
        }
        ScheduleCardStyle.SOFT_TINT -> {
            cardBackground = activityColor
                .copy(alpha = if (isDark) 0.18f else 0.10f)
                .compositeOver(baseSurface)
            cardBorder = BorderStroke(
                width = 1.dp,
                color = activityColor.copy(alpha = if (isDark) 0.42f else 0.35f)
            )
        }
        ScheduleCardStyle.MINIMAL -> {
            cardBackground = if (isDark) Color(0xFF25252B) else Color(0xFFF8F8FC)
            cardBorder = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color(0xFF3A3A42) else Color(0xFFE2E2EC)
            )
        }
    }

    val durationMins = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(1)
    val durationBadge = if (durationMins >= 60 && durationMins % 60L == 0L) {
        "${durationMins / 60} ч"
    } else if (durationMins >= 90) {
        "${durationMins / 60}ч ${durationMins % 60}м"
    } else {
        "$durationMins мин"
    }

    val itemLabel = if (!appProfile.defaultUsePeriodNumbers) {
        durationBadge
    } else if (details.slot.periodOrder > 0) {
        "${details.slot.periodOrder} ${appProfile.shortItemBadge}"
    } else {
        "Без №"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cornerShape)
            .background(cardBackground)
            .then(
                if (cardStyle == ScheduleCardStyle.LEFT_ACCENT) {
                    Modifier.drawWithContent {
                        drawContent()
                        val barWidthPx = 6.dp.toPx()
                        drawRect(
                            color = activityColor,
                            topLeft = Offset.Zero,
                            size = Size(width = barWidthPx, height = size.height)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(if (cardBorder != null) Modifier.border(cardBorder, cornerShape) else Modifier)
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (cardStyle == ScheduleCardStyle.LEFT_ACCENT) 16.dp else 13.dp,
                    top = verticalPadding,
                    end = 13.dp,
                    bottom = verticalPadding
                )
        ) {
            // Header Row: Times + Subject + Add Task + Focus button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left time block tailored to each card style
                when (cardStyle) {
                    ScheduleCardStyle.OUTLINED -> {
                        // Outlined style: Weeklie-inspired capsule badge with crisp border
                        Column(
                            modifier = Modifier
                                .width(66.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(activityColor.copy(alpha = if (isDark) 0.18f else 0.11f))
                                .border(
                                    width = 1.dp,
                                    color = activityColor.copy(alpha = if (isDark) 0.38f else 0.26f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = details.slot.startTime,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = activityColor
                            )
                            Text(
                                text = details.slot.endTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = iosColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = itemLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activityColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                    ScheduleCardStyle.MINIMAL -> {
                        // Minimal style: Clean time stack + Apple Calendar vertical accent bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.width(54.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = details.slot.startTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iosColors.textPrimary
                                )
                                Text(
                                    text = details.slot.endTime,
                                    fontSize = 11.sp,
                                    color = iosColors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = itemLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = iosColors.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(activityColor)
                            )
                        }
                    }
                    else -> {
                        // LEFT_ACCENT & SOFT_TINT: Bold time + tinted period pill
                        Column(
                            modifier = Modifier.width(66.dp)
                        ) {
                            Text(
                                text = details.slot.startTime,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosColors.textPrimary
                            )
                            Text(
                                text = details.slot.endTime,
                                fontSize = 12.sp,
                                color = iosColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(activityColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = itemLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activityColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }

                // Middle Subject + Badges block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = details.activity.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        details.effectiveType?.let { typeName ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (cardStyle == ScheduleCardStyle.MINIMAL) {
                                            if (isDark) Color(0xFF34343C) else Color(0xFFEBEBF2)
                                        } else {
                                            activityColor.copy(alpha = if (isDark) 0.22f else 0.14f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = typeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (cardStyle == ScheduleCardStyle.MINIMAL) iosColors.textPrimary else activityColor
                                )
                            }
                        }

                        if (isTwoWeekCycleEnabled && details.slot.weekCycle != WeekCycle.EVERY_WEEK) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF34343C) else Color(0xFFEBEBF2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = details.slot.weekCycle.shortBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = iosColors.textSecondary
                                )
                            }
                        }
                    }

                    // Location and Instructor
                    if (!details.effectiveLocation.isNullOrBlank() || !details.effectiveInstructor.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    details.effectiveLocation?.let { location ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = iosColors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location,
                                fontSize = 12.sp,
                                color = iosColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    details.effectiveInstructor?.let { instructor ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = iosColors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = instructor,
                                fontSize = 12.sp,
                                color = iosColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top right actions: Interactive Session / Focus timer + Add Task button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onStartInteractiveSession != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(activityColor.copy(alpha = if (isDark) 0.22f else 0.14f))
                                .border(
                                    width = 0.8.dp,
                                    color = activityColor.copy(alpha = if (isDark) 0.40f else 0.26f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onStartInteractiveSession()
                                }
                                .padding(horizontal = 7.dp, vertical = 5.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = appProfile.startSessionLabel,
                                    tint = activityColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                if (details.slot.sessionItems.isNotEmpty() || appProfile == AppProfile.HEALTH) {
                                    Text(
                                        text = if (details.slot.sessionItems.isNotEmpty()) {
                                            "${details.slot.sessionItems.count { it.isDone }}/${details.slot.sessionItems.size}"
                                        } else {
                                            "Старт"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activityColor
                                    )
                                }
                            }
                        }
                    } else if (onStartFocusTimer != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(activityColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                                .border(
                                    width = 0.8.dp,
                                    color = activityColor.copy(alpha = if (isDark) 0.35f else 0.22f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onStartFocusTimer()
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = "Фокус-таймер",
                                tint = activityColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(activityColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                            .border(
                                width = 0.8.dp,
                                color = activityColor.copy(alpha = if (isDark) 0.35f else 0.22f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddTaskClick()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Добавить задачу",
                                tint = activityColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (appProfile == AppProfile.HEALTH) "Цель" else "Задача",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activityColor
                            )
                        }
                    }
                }
            }

            // Interactive Session Items (Exercises / Agenda / Practice steps) inside card
            if (details.slot.sessionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(activityColor.copy(alpha = if (isDark) 0.12f else 0.07f))
                        .border(
                            width = 0.8.dp,
                            color = activityColor.copy(alpha = if (isDark) 0.28f else 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStartInteractiveSession?.invoke() ?: onCardClick()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appProfile.sessionSectionTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activityColor
                            )
                            Text(
                                text = appProfile.startSessionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activityColor
                            )
                        }
                        details.slot.sessionItems.take(4).forEach { step ->
                            val summary = step.formatStepSummary()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (if (step.isDone) "✓ " else "• ") + step.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (step.isDone) iosColors.textSecondary else iosColors.textPrimary,
                                    textDecoration = if (step.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = summary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activityColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (details.slot.sessionItems.size > 4) {
                            Text(
                                text = "+ ещё ${details.slot.sessionItems.size - 4}...",
                                fontSize = 11.sp,
                                color = iosColors.textSecondary
                            )
                        }
                    }
                }
            }

            // Linked Tasks section inside card
            if (details.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(
                            if (cardStyle == ScheduleCardStyle.SOFT_TINT) {
                                activityColor.copy(alpha = if (isDark) 0.28f else 0.20f)
                            } else {
                                iosColors.separator
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    details.tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (onTaskClick != null) {
                                        onTaskClick(task)
                                    } else {
                                        onTaskToggle(task)
                                    }
                                }
                                .padding(vertical = 3.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular iOS checkbox (separate tap target to toggle completion)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTaskToggle(task)
                                    }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (task.isCompleted) {
                                                Modifier.background(activityColor)
                                            } else {
                                                Modifier
                                                    .background(Color.Transparent)
                                                    .border(
                                                        width = 1.6.dp,
                                                        color = activityColor.copy(alpha = 0.75f),
                                                        shape = CircleShape
                                                    )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (task.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    color = if (task.isCompleted) iosColors.textSecondary else iosColors.textPrimary,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                task.note?.takeIf { it.isNotBlank() }?.let { notePreview ->
                                    Text(
                                        text = notePreview,
                                        fontSize = 11.sp,
                                        color = iosColors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (task.subtasks.isNotEmpty()) {
                                val doneCount = task.subtasks.count { it.isDone }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isDark) Color(0xFF34343C) else Color(0xFFEBEBF2))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$doneCount/${task.subtasks.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iosColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live progress bar along the bottom of the card
        if (isOngoing && showProgressBar) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = activityColor,
                trackColor = activityColor.copy(alpha = 0.2f)
            )
        }
    }
}
