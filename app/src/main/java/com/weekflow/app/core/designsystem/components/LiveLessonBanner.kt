package com.weekflow.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.LiveLessonStatus

@Composable
fun LiveLessonBanner(
    liveStatus: LiveLessonStatus,
    modifier: Modifier = Modifier
) {
    val iosColors = LocalIosColors.current

    // Case 1: Lesson in progress
    val currentSlot = liveStatus.currentSlot
    if (currentSlot != null) {
        val accentColor = ColorParser.parseHexColor(currentSlot.activity.colorHex, fallback = IosBlue)
        val animatedProgress by animateFloatAsState(
            targetValue = liveStatus.progress,
            label = "LiveProgress"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(iosColors.card)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pulsing green live indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IosGreen)
                        )
                        Text(
                            text = "СЕЙЧАС ИДЕТ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosGreen,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Осталось ${DateTimeUtils.formatMinutesDuration(liveStatus.minutesLeftInCurrent)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentSlot.activity.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                currentSlot.effectiveLocation?.let { loc ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = loc,
                        fontSize = 13.sp,
                        color = iosColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(iosColors.separator)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }
            }
        }
        return
    }

    // Case 2: Break or upcoming lesson
    val nextSlot = liveStatus.nextSlot
    if (nextSlot != null && liveStatus.isBreakNow) {
        val accentColor = ColorParser.parseHexColor(nextSlot.activity.colorHex, fallback = IosBlue)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(iosColors.card)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Следующее занятие через ${DateTimeUtils.formatMinutesDuration(liveStatus.minutesUntilNext)} (${nextSlot.slot.startTime})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = iosColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nextSlot.activity.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    nextSlot.effectiveLocation?.let { loc ->
                        Text(
                            text = loc,
                            fontSize = 12.sp,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}
