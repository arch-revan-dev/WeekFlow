package com.weekflow.app.presentation.editors

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weekflow.app.core.designsystem.IosBlue
import com.weekflow.app.core.designsystem.IosRed
import com.weekflow.app.core.designsystem.LocalIosColors
import com.weekflow.app.core.designsystem.components.ColorPalettePicker
import com.weekflow.app.core.designsystem.iosPrimaryButtonColors
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.domain.model.Activity
import com.weekflow.app.domain.model.ActivityCategory
import com.weekflow.app.domain.model.SchedulePreset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityEditorSheet(
    initialActivity: Activity?,
    onDismiss: () -> Unit,
    onSave: (Activity) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    presetId: String = SchedulePreset.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iosColors = LocalIosColors.current
    val appProfile = LocalAppProfile.current

    var name by remember(initialActivity?.id) { mutableStateOf(initialActivity?.name ?: "") }
    var shortName by remember(initialActivity?.id) { mutableStateOf(initialActivity?.shortName ?: "") }
    var selectedCategory by remember(initialActivity?.id) { mutableStateOf(initialActivity?.category ?: ActivityCategory.STUDY) }
    var selectedColorHex by remember(initialActivity?.id) { mutableStateOf(initialActivity?.colorHex ?: selectedCategory.defaultColorHex) }
    var defaultLocation by remember(initialActivity?.id) { mutableStateOf(initialActivity?.defaultLocation ?: "") }
    var defaultInstructor by remember(initialActivity?.id) { mutableStateOf(initialActivity?.defaultInstructor ?: "") }
    var defaultType by remember(initialActivity?.id) { mutableStateOf(initialActivity?.defaultType ?: "") }

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
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialActivity == null) "Новый предмет" else "Редактировать предмет",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosColors.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = iosColors.textSecondary)
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название предмета") },
                placeholder = { Text("например, Высшая математика") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Short Name
            OutlinedTextField(
                value = shortName,
                onValueChange = { shortName = it },
                label = { Text("Сокращение для бейджей") },
                placeholder = { Text("например, Матан") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Chips
            Text(
                text = "Категория",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ActivityCategory.entries) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IosBlue else iosColors.cardSecondary)
                            .clickable {
                                selectedCategory = cat
                                if (initialActivity == null) {
                                    selectedColorHex = cat.defaultColorHex
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else iosColors.textPrimary
                        )
                    }
                }
            }

            // Color Palette
            Text(
                text = "Цвет предмета",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosColors.textSecondary
            )
            ColorPalettePicker(
                selectedColorHex = selectedColorHex,
                onColorSelected = { selectedColorHex = it }
            )

            // Default Location
            OutlinedTextField(
                value = defaultLocation,
                onValueChange = { defaultLocation = it },
                label = { Text(appProfile.locationTerm) },
                placeholder = { Text("например, Ауд. 402") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Default Instructor
            OutlinedTextField(
                value = defaultInstructor,
                onValueChange = { defaultInstructor = it },
                label = { Text(appProfile.instructorTerm) },
                placeholder = { Text("например, Иванов И.И.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Default Lesson Type
            OutlinedTextField(
                value = defaultType,
                onValueChange = { defaultType = it },
                label = { Text("Тип занятия по умолчанию") },
                placeholder = { Text("например, Лекция / Семинар") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val act = Activity(
                            id = initialActivity?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            shortName = shortName.trim(),
                            category = selectedCategory,
                            colorHex = selectedColorHex,
                            defaultLocation = defaultLocation.trim().takeIf { it.isNotBlank() },
                            defaultInstructor = defaultInstructor.trim().takeIf { it.isNotBlank() },
                            defaultType = defaultType.trim().takeIf { it.isNotBlank() },
                            presetId = initialActivity?.presetId ?: presetId
                        )
                        onSave(act)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = iosPrimaryButtonColors(IosBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Сохранить",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (initialActivity != null && onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete(initialActivity.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Удалить предмет")
                }
            }
        }
    }
}
