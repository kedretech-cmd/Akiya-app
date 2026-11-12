package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Akiya Web", appName)
  }

  @Test
  fun `notification channels initialized`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = NotificationHelper(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_CALLS_ID))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_ONGOING_CALLS_ID))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_MISSED_CALLS_ID))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_CHAT_ID))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_PUSH_ID))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_DOWNLOADS_ID))
  }
}
