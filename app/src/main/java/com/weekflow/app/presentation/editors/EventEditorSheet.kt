package com.weekflow.app.presentation.editors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosGreen
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.ColorPalettePicker
import com.weekflow.app.core.designsystem.components.IosDatePickerButton
import com.weekflow.app.core.designsystem.components.IosTimePickerButton
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.designsystem.iosSwitchColors
import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.CalendarEvent
import com.weekflow.app.domain.model.SchedulePreset
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    initialEvent: CalendarEvent?,
    defaultDateIso: String,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    presetId: String = SchedulePreset.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current

    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title ?: "") }
    var dateIso by remember(initialEvent?.id) { mutableStateOf(initialEvent?.dateIso ?: defaultDateIso) }
    var isAllDay by remember(initialEvent?.id) { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var startTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.startTime ?: "10:00") }
    var endTime by remember(initialEvent?.id) { mutableStateOf(initialEvent?.endTime ?: "11:30") }
    var colorHex by remember(initialEvent?.id) { mutableStateOf(initialEvent?.colorHex ?: "#007AFF") }
    var location by remember(initialEvent?.id) { mutableStateOf(initialEvent?.location ?: "") }
    var description by remember(initialEvent?.id) { mutableStateOf(initialEvent?.description ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosColors.card,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEvent == null) "Новое событие" else "Редактировать событие",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название события") },
                placeholder = { Text("например, Экзамен по физике") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date (Calendar Picker)
            IosDatePickerButton(
                label = "Дата проведения",
                dateIso = dateIso,
                onDateSelected = { dateIso = it },
                modifier = Modifier.fillMaxWidth()
            )

            // All Day switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Весь день", fontSize = 15.sp, color = iosColors.textPrimary)
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it },
                    colors = iosSwitchColors()
                )
            }

            // Times (if not all day) — Wheel Time Pickers
            AnimatedVisibility(visible = !isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosTimePickerButton(
                        label = "Начало",
                        timeString = startTime,
                        onTimeSelected = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    IosTimePickerButton(
                        label = "Конец",
                        timeString = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Color Picker
            Text(
                text = "Цвет маркера",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            ColorPalettePicker(
                selectedColorHex = colorHex,
                onColorSelected = { colorHex = it }
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Место проведения / Аудитория") },
                placeholder = { Text("например, Ауд. 301") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание / Заметки") },
                placeholder = { Text("Дополнительные детали...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Duplicate button if editing
            if (initialEvent != null) {
                OutlinedButton(
                    onClick = {
                        val duplicated = CalendarEvent(
                            id = UUID.randomUUID().toString(),
                            title = "${title.trim()} (копия)",
                            dateIso = try { LocalDate.parse(dateIso).plusDays(1).toString() } catch (_: Exception) { dateIso },
                            startTime = if (isAllDay) null else startTime.trim(),
                            endTime = if (isAllDay) null else endTime.trim(),
                            isAllDay = isAllDay,
                            colorHex = colorHex,
                            location = location.trim(),
                            description = description.trim(),
                            category = ActivityCategory.PERSONAL,
                            presetId = initialEvent.presetId.ifBlank { presetId }
                        )
                        onSave(duplicated)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Дублировать на следующий день")
                }
            }

            // Save and Delete
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val event = CalendarEvent(
                            id = initialEvent?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            dateIso = dateIso.trim(),
                            startTime = if (isAllDay) null else startTime.trim(),
                            endTime = if (isAllDay) null else endTime.trim(),
                            isAllDay = isAllDay,
                            colorHex = colorHex,
                            location = location.trim(),
                            description = description.trim(),
                            category = ActivityCategory.PERSONAL,
                            presetId = initialEvent?.presetId?.ifBlank { presetId } ?: presetId
                        )
                        onSave(event)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                colors = iosPrimaryButtonColors(IosBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сохранить событие", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (initialEvent != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete(initialEvent.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить событие")
                }
            }
        }
    }
}
