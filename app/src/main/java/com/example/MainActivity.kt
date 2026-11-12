package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.notifications.NotificationHelper
import com.example.ui.HybridWebViewScreen
import com.example.ui.PortalDomain
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var notificationHelper: NotificationHelper
    private var incomingAction by mutableStateOf<String?>(null)
    private var callerName by mutableStateOf<String?>(null)
    private var initialPortal by mutableStateOf(PortalDomain.AKIYA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationHelper = NotificationHelper(this)
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HybridWebViewScreen(
                        initialPortal = initialPortal,
                        incomingCallAction = incomingAction,
                        callerNameExtra = callerName,
                        notificationHelper = notificationHelper,
                        onToggleSystemFullscreen = { fullscreen ->
                            setSystemBarsFullscreen(fullscreen)
                        }
                    )
                }
            }
        }
    }

    fun setSystemBarsFullscreen(fullscreen: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        incomingAction = action

        if (action == NotificationHelper.ACTION_DECLINE_CALL || action == NotificationHelper.ACTION_END_CALL) {
            notificationHelper.dismissCallNotification()
        } else if (action == NotificationHelper.ACTION_ANSWER_CALL) {
            callerName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME)
        }

        // Handle Deep Linking or Alternate Portal launch
        val dataUri = intent.data
        if (dataUri != null) {
            val host = dataUri.host?.lowercase() ?: ""
            if (host.contains("akmel")) {
                initialPortal = PortalDomain.AKMEL
            } else if (host.contains("akiya")) {
                initialPortal = PortalDomain.AKIYA
            }
        }
    }
}
