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
import androidx.room.Room
import com.weekflow.app.MainActivity
import com.weekflow.app.core.util.ColorParser
import com.weekflow.app.core.util.DateTimeUtils
import com.weekflow.app.data.local.datastore.UserPreferencesRepository
import com.weekflow.app.data.local.db.WeekFlowDatabase
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.model.Slot
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.model.WeekCycle
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.LocalTime

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WeekFlowWidgetUpdater.forceUpdateAll(context.applicationContext)
    }
}

data class WidgetSlotItem(
    val slot: Slot,
    val activityName: String,
    val activityColor: Color,
    val location: String,
    val type: String,
    val taskCount: Int,
    val isCurrent: Boolean = false
)

class WeekFlowGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    companion object {
        @Volatile
        private var fallbackDb: WeekFlowDatabase? = null

        fun getDatabase(context: Context): WeekFlowDatabase {
            val appContext = context.applicationContext
            GlobalContext.getOrNull()?.getOrNull<WeekFlowDatabase>()?.let { return it }
            return fallbackDb ?: synchronized(this) {
                fallbackDb ?: Room.databaseBuilder(
                    appContext,
                    WeekFlowDatabase::class.java,
                    WeekFlowDatabase.DATABASE_NAME
                )
                    .addMigrations(
                        WeekFlowDatabase.MIGRATION_2_3,
                        WeekFlowDatabase.MIGRATION_3_4,
                        WeekFlowDatabase.MIGRATION_4_5,
                        WeekFlowDatabase.MIGRATION_5_6,
                        WeekFlowDatabase.MIGRATION_6_7,
                        WeekFlowDatabase.MIGRATION_7_8,
                        WeekFlowDatabase.MIGRATION_8_9
                    )
                    .addCallback(WeekFlowDatabase.DEFAULT_PRESET_CALLBACK)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { fallbackDb = it }
            }
        }

        fun getPreferencesRepo(context: Context): PreferencesRepository {
            val appContext = context.applicationContext
            GlobalContext.getOrNull()?.getOrNull<PreferencesRepository>()?.let { return it }
            return UserPreferencesRepository(appContext)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val today = LocalDate.now()
        val nowTime = LocalTime.now()

        var isTargetToday = true
        var targetDate = today
        var cycleBadge = ""
        var targetItems = emptyList<WidgetSlotItem>()
        var pendingTasksForTargetDay = 0

        runCatching {
            val database = getDatabase(appContext)
            val prefRepo = getPreferencesRepo(appContext)
            val preferences = runCatching { prefRepo.userPreferences.first() }.getOrDefault(UserPreferences())
            val calculateWeekCycleUseCase = CalculateWeekCycleUseCase()

            val allPresets = database.presetDao().getAllList().map { it.toDomain() }
            val allActivities = database.activityDao().getAllList().associateBy { it.id }
            val allSlots = database.slotDao().getAllList().map { it.toDomain() }
            val allTasks = database.taskDao().getAllList().map { it.toDomain() }

            // Today's preset, cycle and slots
            val (todayPreset, _) = SchedulePreset.resolveForDate(today, allPresets, preferences.activePresetId)
            val todayCycle = calculateWeekCycleUseCase(today, preferences)
            val todaySlots = allSlots.filter { slot ->
                slot.presetId == todayPreset.id &&
                    slot.dayOfWeek == today.dayOfWeek.value &&
                    (!preferences.isTwoWeekCycleEnabled ||
                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                        slot.weekCycle == todayCycle)
            }.sortedBy { it.parseStartTime() }

            // Find active slot currently ongoing
            val activeSlotId = todaySlots.firstOrNull { slot ->
                val start = slot.parseStartTime()
                val end = slot.parseEndTime()
                !nowTime.isBefore(start) && nowTime.isBefore(end)
            }?.id

            // Remaining today's slots that haven't ended yet
            val remainingTodaySlots = todaySlots.filter { it.parseEndTime().isAfter(nowTime) }

            val tomorrow = today.plusDays(1)
            val (tomorrowPreset, _) = SchedulePreset.resolveForDate(tomorrow, allPresets, preferences.activePresetId)
            val tomorrowCycle = calculateWeekCycleUseCase(tomorrow, preferences)
            val tomorrowSlots = allSlots.filter { slot ->
                slot.presetId == tomorrowPreset.id &&
                    slot.dayOfWeek == tomorrow.dayOfWeek.value &&
                    (!preferences.isTwoWeekCycleEnabled ||
                        slot.weekCycle == WeekCycle.EVERY_WEEK ||
                        slot.weekCycle == tomorrowCycle)
            }.sortedBy { it.parseStartTime() }

            // Show Today if there are remaining lessons today, OR if tomorrow has no lessons while today had lessons
            isTargetToday = remainingTodaySlots.isNotEmpty() || (todaySlots.isNotEmpty() && tomorrowSlots.isEmpty()) || (todaySlots.isEmpty() && tomorrowSlots.isEmpty())
            targetDate = if (isTargetToday) today else tomorrow
            val targetPreset = if (isTargetToday) todayPreset else tomorrowPreset
            val targetCycle = if (isTargetToday) todayCycle else tomorrowCycle

            val slotsToShow = when {
                remainingTodaySlots.isNotEmpty() -> remainingTodaySlots
                isTargetToday -> todaySlots
                else -> tomorrowSlots
            }

            targetItems = slotsToShow.map { slot ->
                val act = allActivities[slot.activityTypeId]
                val tasksForSlot = allTasks.filter {
                    (preferences.showAllPresetsTasks || it.presetId == targetPreset.id) &&
                        !it.isCompleted &&
                        it.dueDateIso == targetDate.toString() &&
                        (it.linkedSlotId == slot.id || (it.linkedSlotId == null && it.linkedActivityId == slot.activityTypeId))
                }
                WidgetSlotItem(
                    slot = slot,
                    activityName = act?.name ?: "Занятие",
                    activityColor = act?.colorHex?.let { ColorParser.parseHexColor(it) } ?: Color(0xFF007AFF),
                    location = slot.customLocation?.takeIf { it.isNotBlank() } ?: act?.defaultLocation ?: "",
                    type = slot.customType?.takeIf { it.isNotBlank() } ?: act?.defaultType ?: "",
                    taskCount = tasksForSlot.size,
                    isCurrent = (slot.id == activeSlotId && isTargetToday)
                )
            }

            pendingTasksForTargetDay = allTasks.count {
                (preferences.showAllPresetsTasks || it.presetId == targetPreset.id) &&
                    it.dueDateIso == targetDate.toString() &&
                    !it.isCompleted
            }

            cycleBadge = if (preferences.isTwoWeekCycleEnabled && targetCycle != WeekCycle.EVERY_WEEK) {
                targetCycle.displayName
            } else {
                ""
            }
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val refreshTick = prefs[WeekFlowWidgetUpdater.FORCE_REFRESH_KEY] ?: 0L

            GlanceTheme {
                WidgetRootView(
                    targetDate = targetDate,
                    isToday = isTargetToday,
                    cycleBadge = cycleBadge,
                    items = targetItems,
                    pendingTasksCount = pendingTasksForTargetDay,
                    refreshTick = refreshTick
                )
            }
        }
    }

    @Composable
    private fun WidgetRootView(
        targetDate: LocalDate,
        isToday: Boolean,
        cycleBadge: String,
        items: List<WidgetSlotItem>,
        pendingTasksCount: Int,
        refreshTick: Long
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF141416))
                .cornerRadius(18.dp)
                .padding(8.dp)
        ) {
            // 1. Ultra-compact Header Row (~28-30dp)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title (Date + Today/Tomorrow)
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = DateTimeUtils.formatDateWithDay(targetDate)
                    val statusText = if (isToday) "Сегодня" else "Завтра"
                    val headerTitle = "$dateFormatted • $statusText"

                    Text(
                        text = headerTitle,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        ),
                        maxLines = 1
                    )

                    if (cycleBadge.isNotBlank()) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "($cycleBadge)",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(Color(0xFFAFAEB2))
                            )
                        )
                    }

                    if (pendingTasksCount > 0) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "• ✓ $pendingTasksCount",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFFF9500))
                            )
                        )
                    }
                }

                // Right: Two compact 24dp buttons (↻ and +)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ↻ Refresh Button (24x24 dp)
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .cornerRadius(12.dp)
                            .background(Color(0xFF2C2C2E))
                            .clickable(actionRunCallback<RefreshWidgetAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // + Add Task Button (24x24 dp)
                    val addTaskParams = actionParametersOf(
                        ActionParameters.Key<String>("action") to "com.weekflow.app.ACTION_ADD_TASK"
                    )
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .cornerRadius(12.dp)
                            .background(Color(0xFF007AFF))
                            .clickable(actionStartActivity<MainActivity>(addTaskParams)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White)
                            )
                        )
                    }
                }
            }

            // 2. Scrollable Schedule List (LazyColumn)
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Занятий нет. Отдыхайте! 🎉",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF8E8E93))
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    items(items, itemId = { (it.slot.id + "_$refreshTick").hashCode().toLong() }) { item ->
                        val rowBg = if (item.isCurrent) Color(0xFF2C2C2E) else Color(0xFF1E1E20)

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .cornerRadius(8.dp)
                                    .background(rowBg)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .clickable(actionStartActivity<MainActivity>()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left vertical color strip (3.dp x 26.dp)
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .cornerRadius(1.5.dp)
                                        .background(item.activityColor)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Time block (width 46.dp)
                                Column(modifier = GlanceModifier.width(46.dp)) {
                                    Text(
                                        text = item.slot.startTime,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(Color.White)
                                        )
                                    )
                                    Text(
                                        text = item.slot.endTime,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            color = ColorProvider(Color(0xFF8E8E93))
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Subject & Task indicator
                                Row(
                                    modifier = GlanceModifier.defaultWeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.activityName,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ColorProvider(Color.White)
                                        ),
                                        maxLines = 1
                                    )
                                    if (item.taskCount > 0) {
                                        Spacer(modifier = GlanceModifier.width(4.dp))
                                        Text(
                                            text = "📌${item.taskCount}",
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorProvider(Color(0xFFFF9500))
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Right: Current badge or Location
                                if (item.isCurrent) {
                                    Box(
                                        modifier = GlanceModifier
                                            .cornerRadius(4.dp)
                                            .background(Color(0xFF1F4E2C))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "● СЕЙЧАС",
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorProvider(Color(0xFF34C759))
                                            )
                                        )
                                    }
                                } else if (item.location.isNotBlank()) {
                                    Text(
                                        text = item.location,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(Color(0xFF8E8E93))
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

class WeekFlowGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekFlowGlanceWidget()
}
