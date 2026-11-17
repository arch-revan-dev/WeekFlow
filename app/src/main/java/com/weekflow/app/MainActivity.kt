package com.weekflow.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.weekflow.app.core.designsystem.WeekFlowTheme
import com.weekflow.app.core.localization.LocalAppProfile
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.model.UserPreferences
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.presentation.navigation.WeekFlowNavHost
import com.weekflow.app.widget.WeekFlowWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: PreferencesRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val actionState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    notificationScheduler.rescheduleAllNotifications()
                } catch (_: Exception) {}
            }
        }
    }

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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WeekFlowWidgetUpdater.forceUpdateAll(applicationContext)
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }

        setContent {
            val preferences by preferencesRepository.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val currentAction by actionState

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LaunchedEffect(Unit) {
                    delay(800L)
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission && !NotificationScheduler.wasNotificationPermissionRequested(this@MainActivity)) {
                        NotificationScheduler.markNotificationPermissionRequested(this@MainActivity)
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WeekFlowWidgetUpdater.forceUpdateAll(applicationContext)
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                WeekFlowWidgetUpdater.forceUpdateAll(applicationContext)
                notificationScheduler.rescheduleAllNotifications()
            } catch (_: Exception) {}
        }
    }
}
