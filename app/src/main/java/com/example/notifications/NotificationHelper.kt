package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_CALLS_ID = "channel_incoming_calls"
        const val CHANNEL_ONGOING_CALLS_ID = "channel_ongoing_calls"
        const val CHANNEL_MISSED_CALLS_ID = "channel_missed_calls"
        const val CHANNEL_CHAT_ID = "channel_chat_messages"
        const val CHANNEL_PUSH_ID = "channel_web_push"
        const val CHANNEL_DOWNLOADS_ID = "channel_downloads"

        const val NOTIFICATION_CALL_ID = 2001
        const val NOTIFICATION_ONGOING_CALL_ID = 2002
        const val NOTIFICATION_MISSED_CALL_ID = 2003
        const val NOTIFICATION_CHAT_ID = 2004
        const val NOTIFICATION_DOWNLOAD_ID = 2005

        const val ACTION_ANSWER_CALL = "com.example.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.example.ACTION_DECLINE_CALL"
        const val ACTION_END_CALL = "com.example.ACTION_END_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_TARGET_URL = "extra_target_url"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            // 1. Incoming Call Channel (High Priority Heads-Up, Ringing, Vibration)
            val callChannel = NotificationChannel(
                CHANNEL_CALLS_ID,
                context.getString(R.string.call_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.call_notification_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Ongoing Active Call Channel
            val ongoingCallChannel = NotificationChannel(
                CHANNEL_ONGOING_CALLS_ID,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active call status and quick call controls"
                setShowBadge(false)
            }

            // 3. Missed Calls Channel
            val missedCallChannel = NotificationChannel(
                CHANNEL_MISSED_CALLS_ID,
                "Missed Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for missed voice and video calls"
                enableVibration(true)
            }

            // 4. Chat & Messaging Channel
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                "Chat & Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant chat messages and mentions"
                enableVibration(true)
            }

            // 5. General Web Push Channel
            val pushChannel = NotificationChannel(
                CHANNEL_PUSH_ID,
                context.getString(R.string.push_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.push_notification_channel_desc)
                enableVibration(true)
            }

            // 6. Downloads & Media Channel
            val downloadsChannel = NotificationChannel(
                CHANNEL_DOWNLOADS_ID,
                "Downloads & Files",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "File download progress and completed downloads"
            }

            notificationManager.createNotificationChannels(
                listOf(callChannel, ongoingCallChannel, missedCallChannel, chatChannel, pushChannel, downloadsChannel)
            )
        }
    }

    // 1. Incoming Call Notification
    fun showIncomingCallNotification(
        callerName: String,
        callType: String = "Voice Call",
        callId: String = System.currentTimeMillis().toString(),
        targetUrl: String? = null
    ) {
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ANSWER_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            targetUrl?.let { putExtra(EXTRA_TARGET_URL, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ANSWER_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            targetUrl?.let { putExtra(EXTRA_TARGET_URL, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALL_ID, callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_app_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setContentTitle(callerName.ifEmpty { "Incoming Call" })
            .setContentText("Incoming $callType on Akiya Hybrid Platform")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                context.getString(R.string.answer_call),
                answerPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.decline_call),
                declinePendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_CALL_ID, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission check
        }
    }

    // 2. Ongoing Active Call Notification
    fun showOngoingCallNotification(
        callerName: String,
        callType: String = "Voice Call",
        callId: String = System.currentTimeMillis().toString()
    ) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ANSWER_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CALLER_NAME, callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 10,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_END_CALL
            putExtra(EXTRA_CALL_ID, callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val endCallPendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode() + 11,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING_CALLS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call_forward)
            .setContentTitle("Active $callType with $callerName")
            .setContentText("Call in progress • Tap to return")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(clickPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ONGOING_CALL_ID, builder.build())
        } catch (e: SecurityException) {
            // Ignore if permission not granted
        }
    }

    // 3. Missed Call Notification
    fun showMissedCallNotification(
        callerName: String,
        callType: String = "Voice Call"
    ) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_MISSED_CALL_ID,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle("Missed Call")
            .setContentText("Missed $callType from $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(clickPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_MISSED_CALL_ID, builder.build())
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    // 4. Chat Message Notification
    fun showChatMessageNotification(
        sender: String,
        message: String,
        chatId: String = System.currentTimeMillis().toString()
    ) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(sender)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_CHAT_ID, builder.build())
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    // 5. Push Notification
    fun showPushNotification(
        title: String,
        message: String,
        targetUrl: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            targetUrl?.let { putExtra(EXTRA_TARGET_URL, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PUSH_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission check
        }
    }

    // 6. Download / File Notification
    fun showDownloadNotification(
        fileName: String,
        progress: Int = 100,
        isCompleted: Boolean = true
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS_ID)
            .setSmallIcon(
                if (isCompleted) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_sys_download
            )
            .setContentTitle(if (isCompleted) "Download Complete" else "Downloading File")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(isCompleted)

        if (!isCompleted) {
            builder.setProgress(100, progress, false)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_DOWNLOAD_ID, builder.build())
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    fun dismissCallNotification() {
        notificationManager.cancel(NOTIFICATION_CALL_ID)
        notificationManager.cancel(NOTIFICATION_ONGOING_CALL_ID)
    }

    fun dismissNotification(id: Int) {
        notificationManager.cancel(id)
    }

    fun dismissAllNotifications() {
        notificationManager.cancelAll()
    }
}
