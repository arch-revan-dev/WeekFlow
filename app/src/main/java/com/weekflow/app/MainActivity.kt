package com.weekflow.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.weekflow.app.core.designsystem.WeekFlowTheme
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.presentation.navigation.WeekFlowNavHost
import com.weekflow.app.widget.WeekFlowWidgetUpdater
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: PreferencesRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val actionState = mutableStateOf<String?>(null)

    private fun extractAction(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == "com.weekflow.app.ACTION_ADD_TASK") return intent.action
        val paramAction = intent.getStringExtra("action")
        if (paramAction == "com.weekflow.app.ACTION_ADD_TASK") return paramAction
        return intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        actionState.value = extractAction(intent)

        lifecycleScope.launch {
            try {
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }

        setContent {
            val preferences by preferencesRepository.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val currentAction by actionState

            WeekFlowTheme(themeMode = preferences.themeMode) {
                CompositionLocalProvider(
                    LocalAppProfile provides preferences.appProfile
                ) {
                    WeekFlowNavHost(
                        initialAction = currentAction,
                        onActionHandled = { actionState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        actionState.value = extractAction(intent)

        lifecycleScope.launch {
            try {
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            try {
                WeekFlowWidgetUpdater.forceUpdateAll(applicationContext)
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }
}
