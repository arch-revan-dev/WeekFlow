package com.weekflow.app.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

object WeekFlowWidgetUpdater {
    val FORCE_REFRESH_KEY = longPreferencesKey("widget_force_refresh_timestamp")

    suspend fun forceUpdateAll(context: Context) {
        val now = System.currentTimeMillis()
        val manager = GlanceAppWidgetManager(context)

        try {
            val scheduleIds = manager.getGlanceIds(WeekFlowGlanceWidget::class.java)
            scheduleIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[FORCE_REFRESH_KEY] = now
                    }
                }
                WeekFlowGlanceWidget().update(context, glanceId)
            }
        } catch (_: Exception) {}

        try {
            val taskIds = manager.getGlanceIds(WeekFlowTasksWidget::class.java)
            taskIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[FORCE_REFRESH_KEY] = now
                    }
                }
                WeekFlowTasksWidget().update(context, glanceId)
            }
        } catch (_: Exception) {}
    }
}
