package com.example.hybrid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationHelper
import org.json.JSONObject

class WebAppInterface(
    private val context: Context,
    private val notificationHelper: NotificationHelper,
    private val onSwitchPortal: (String) -> Unit,
    private val onRequestNativePermissions: () -> Unit,
    private val onCallStateChanged: (callerName: String, isIncoming: Boolean) -> Unit,
    private val onToggleFullscreen: (Boolean) -> Unit
) {

    @JavascriptInterface
    fun isHybridApp(): Boolean = true

    @JavascriptInterface
    fun getPlatform(): String = "Android-AkiyaHybrid-v2.0"

    // 1. Incoming Call Notification
    @JavascriptInterface
    fun showCallNotification(callerName: String?, callType: String?, callId: String?) {
        val name = callerName ?: "Caller"
        val type = callType ?: "Voice/Video Call"
        val id = callId ?: System.currentTimeMillis().toString()

        notificationHelper.showIncomingCallNotification(
            callerName = name,
            callType = type,
            callId = id
        )
        onCallStateChanged(name, true)
    }

    // 2. Ongoing Call Notification
    @JavascriptInterface
    fun showOngoingCall(callerName: String?, callType: String?, callId: String?) {
        val name = callerName ?: "Active Call"
        val type = callType ?: "Voice Call"
        val id = callId ?: System.currentTimeMillis().toString()
        notificationHelper.showOngoingCallNotification(name, type, id)
        onCallStateChanged(name, false)
    }

    // 3. Missed Call Notification
    @JavascriptInterface
    fun showMissedCall(callerName: String?, callType: String?) {
        val name = callerName ?: "Unknown Caller"
        val type = callType ?: "Call"
        notificationHelper.showMissedCallNotification(name, type)
    }

    // 4. Chat Message Notification
    @JavascriptInterface
    fun showChatMessage(sender: String?, message: String?, chatId: String?) {
        val s = sender ?: "New Message"
        val m = message ?: ""
        val id = chatId ?: System.currentTimeMillis().toString()
        notificationHelper.showChatMessageNotification(s, m, id)
    }

    // 5. Web Push Notification
    @JavascriptInterface
    fun showNotification(title: String?, message: String?, url: String?) {
        val t = title ?: "Akiya Notification"
        val m = message ?: ""
        notificationHelper.showPushNotification(title = t, message = m, targetUrl = url)
    }

    // 6. Download Notification
    @JavascriptInterface
    fun showDownloadNotification(fileName: String?, progress: Int, isCompleted: Boolean) {
        val f = fileName ?: "downloaded_file"
        notificationHelper.showDownloadNotification(f, progress, isCompleted)
    }

    // Generic Notification Dispatcher
    @JavascriptInterface
    fun notifyAll(title: String?, message: String?, type: String?) {
        val t = title ?: "Akiya Alert"
        val m = message ?: ""
        when (type?.lowercase()) {
            "call" -> showCallNotification(t, "Incoming Call", null)
            "ongoing" -> showOngoingCall(t, "Active Call", null)
            "missed" -> showMissedCall(t, "Voice Call")
            "chat" -> showChatMessage(t, m, null)
            "download" -> showDownloadNotification(t, 100, true)
            else -> showNotification(t, m, null)
        }
    }

    @JavascriptInterface
    fun dismissCallNotification() {
        notificationHelper.dismissCallNotification()
        onCallStateChanged("", false)
    }

    @JavascriptInterface
    fun dismissAllNotifications() {
        notificationHelper.dismissAllNotifications()
    }

    @JavascriptInterface
    fun requestFullscreen() {
        onToggleFullscreen(true)
    }

    @JavascriptInterface
    fun exitFullscreen() {
        onToggleFullscreen(false)
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        val duration = if (durationMs in 1..5000) durationMs else 300L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors if not permitted
        }
    }

    @JavascriptInterface
    fun switchPortal(portalName: String?) {
        if (!portalName.isNullOrBlank()) {
            onSwitchPortal(portalName.trim())
        }
    }

    @JavascriptInterface
    fun requestNativePermissions() {
        onRequestNativePermissions()
    }

    @JavascriptInterface
    fun requestAllPermissions() {
        onRequestNativePermissions()
    }

    @JavascriptInterface
    fun checkPermissions(): String = checkAllPermissions()

    @JavascriptInterface
    fun checkAllPermissions(): String {
        val json = JSONObject()
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        json.put("camera", hasCamera)
        json.put("microphone", hasAudio)
        json.put("notifications", hasNotifications)
        json.put("location", hasLocation)
        json.put("bluetooth", hasBluetooth)
        json.put("allGranted", hasCamera && hasAudio && hasNotifications && hasLocation && hasBluetooth)
        return json.toString()
    }
}
