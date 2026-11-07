package com.weekflow.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.weekflow.app.MainActivity
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Task
import com.weekflow.app.domain.model.TaskPriority
import com.weekflow.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val TASK_ID_PARAM = ActionParameters.Key<String>("widget_task_id")

class CompleteTaskFromWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_PARAM] ?: return
        try {
            val db = WeekFlowGlanceWidget.getDatabase(context.applicationContext)
            db.taskDao().updateCompleted(taskId, true)
            WeekFlowWidgetUpdater.forceUpdateAll(context.applicationContext)
        } catch (_: Exception) {}
    }
}

data class WidgetTaskItem(
    val task: Task,
    val subjectName: String?,
    val subjectColor: Color,
    val dueLabel: String,
    val isOverdue: Boolean,
    val isDueToday: Boolean
)

class WeekFlowTasksWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val today = LocalDate.now()

        var presetName = "Задачи"
        var pendingItems = emptyList<WidgetTaskItem>()

        try {
            val db = WeekFlowGlanceWidget.getDatabase(appContext)
            val prefsRepo = WeekFlowGlanceWidget.getPreferencesRepo(appContext)
            val preferences = runCatching { prefsRepo.userPreferences.first() }.getOrDefault(UserPreferences())
            val allPresets = runCatching {
                db.presetDao().getAllList().map { it.toDomain() }
            }.getOrDefault(listOf(SchedulePreset.DEFAULT_PRESET))

            val (effectivePreset, _) = SchedulePreset.resolveForDate(
                date = today,
                presets = allPresets,
                activePresetId = preferences.activePresetId,
                manualOverridePresetId = null
            )
            presetName = if (preferences.showAllPresetsTasks) "Все пресеты" else effectivePreset.name

            val allActivities = db.activityDao().getAllList().map { it.toDomain() }
            val activityMap = allActivities.associateBy { it.id }

            val allTasks = db.taskDao().getAllList().map { it.toDomain() }
            val filteredTasks = if (preferences.showAllPresetsTasks) {
                allTasks.filter { !it.isCompleted }
            } else {
                allTasks.filter { !it.isCompleted && (it.presetId == effectivePreset.id || it.presetId.isBlank()) }
            }

            pendingItems = filteredTasks
                .sortedWith(
                    compareBy<Task> { it.parseDueDate() ?: LocalDate.MAX }
                        .thenBy { it.priority.ordinal }
                )
                .take(15)
                .map { task ->
                    val act = task.linkedActivityId?.let { activityMap[it] }
                    val dueDate = task.parseDueDate()
                    val isOverdue = dueDate != null && dueDate.isBefore(today)
                    val isDueToday = dueDate != null && dueDate.isEqual(today)
                    val dueLabel = when {
                        dueDate == null -> "Без даты"
                        isOverdue -> "Просрочено (${DateTimeUtils.formatShortDayMonth(dueDate)})"
                        isDueToday -> "Сегодня"
                        dueDate.isEqual(today.plusDays(1)) -> "Завтра"
                        else -> "${DateTimeUtils.getDayOfWeekShort(dueDate.dayOfWeek.value)}, ${DateTimeUtils.formatShortDayMonth(dueDate)}"
                    }
                    WidgetTaskItem(
                        task = task,
                        subjectName = act?.shortName?.ifBlank { act.name } ?: act?.name,
                        subjectColor = act?.let { ColorParser.parseHexColor(it.colorHex) } ?: Color(0xFF007AFF),
                        dueLabel = dueLabel,
                        isOverdue = isOverdue,
                        isDueToday = isDueToday
                    )
                }
        } catch (_: Exception) {}

        provideContent {
            val prefs = currentState<Preferences>()
            val _refreshTick = prefs[WeekFlowWidgetUpdater.FORCE_REFRESH_KEY] ?: 0L

            GlanceTheme {
                TasksWidgetContent(
                    presetName = presetName,
                    items = pendingItems
                )
            }
        }
    }

    @Composable
    private fun TasksWidgetContent(
        presetName: String,
        items: List<WidgetTaskItem>
    ) {
        val bgColor = Color(0xFF141418)
        val cardColor = Color(0xFF222228)
        val accentBlue = Color(0xFF0A84FF)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(bgColor)
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ЗАДАЧИ",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(accentBlue)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(6.dp)
                                .background(Color(0xFF282830))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${items.size}",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(Color.White)
                                )
                            )
                        }
                    }
                    Text(
                        text = presetName,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        ),
                        maxLines = 1
                    )
                }

                // Refresh button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(8.dp)
                        .background(Color(0xFF25252D))
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFAEAEB2))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Quick Add Task button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(8.dp)
                        .background(accentBlue)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(
                                    ActionParameters.Key<String>("action") to "com.weekflow.app.ACTION_ADD_TASK"
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 Все задачи выполнены!",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF8E8E93))
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(items, itemId = { it.task.id.hashCode().toLong() }) { item ->
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 5.dp)) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(10.dp)
                                    .background(cardColor)
                                    .clickable(actionStartActivity<MainActivity>())
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // One-tap check button
                                val priorityColor = when (item.task.priority) {
                                    TaskPriority.HIGH -> Color(0xFFFF3B30)
                                    TaskPriority.MEDIUM -> Color(0xFFFF9500)
                                    TaskPriority.LOW -> Color(0xFF007AFF)
                                    TaskPriority.NONE -> Color(0xFF8E8E93)
                                }
                                Box(
                                    modifier = GlanceModifier
                                        .size(20.dp)
                                        .cornerRadius(10.dp)
                                        .background(Color(0xFF32323A))
                                        .clickable(
                                            actionRunCallback<CompleteTaskFromWidgetAction>(
                                                actionParametersOf(TASK_ID_PARAM to item.task.id)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "○",
                                        style = TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(priorityColor)
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.width(8.dp))

                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = item.task.title,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ColorProvider(Color.White)
                                        ),
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!item.subjectName.isNullOrBlank()) {
                                            Text(
                                                text = item.subjectName,
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ColorProvider(item.subjectColor)
                                                ),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = " • ",
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    color = ColorProvider(Color(0xFF8E8E93))
                                                )
                                            )
                                        }
                                        val dueColor = when {
                                            item.isOverdue -> Color(0xFFFF453A)
                                            item.isDueToday -> Color(0xFFFF9F0A)
                                            else -> Color(0xFF98989F)
                                        }
                                        Text(
                                            text = item.dueLabel,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ColorProvider(dueColor)
                                            ),
                                            maxLines = 1
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
}

class WeekFlowTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekFlowTasksWidget()
}
