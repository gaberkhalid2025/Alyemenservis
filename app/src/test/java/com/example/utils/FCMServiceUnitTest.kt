package com.example.utils

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.FCMService
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.Shadows.shadowOf

/**
 * 🔔 FCMServiceUnitTest
 * Unit tests for Firebase Cloud Messaging service verifying payload parsing,
 * critical notifications, chat and urgent dispatch, and token caching without MockK.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FCMServiceUnitTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager
    private lateinit var fcmService: FCMService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        // Statically build the service using Robolectric
        fcmService = Robolectric.buildService(FCMService::class.java).create().get()
    }

    @Test
    fun `test onNewToken saves backup token in shared preferences`() {
        val testToken = "fcm_mock_token_sample_12345"
        fcmService.onNewToken(testToken)

        val sp: SharedPreferences = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
        val savedToken = sp.getString("fcm_token_backup", null)
        assertEquals(testToken, savedToken)
    }

    @Test
    fun `test onMessageReceived handles chat message payload`() {
        val message = RemoteMessage.Builder("sender")
            .addData("type", "CHAT")
            .addData("channelId", "channel_test_456")
            .addData("senderId", "user_sender_1")
            .addData("senderName", "محمد اليماني")
            .addData("message", "مرحباً، هل الخدمة متاحة؟")
            .addData("mediaType", "TEXT")
            .build()

        fcmService.onMessageReceived(message)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
    }

    @Test
    fun `test onMessageReceived handles urgent request payload`() {
        val message = RemoteMessage.Builder("sender")
            .addData("type", "URGENT")
            .addData("requestCode", "URG-7788")
            .addData("title", "عطل طارئ في شبكة الكهرباء")
            .addData("description", "انقطاع كامل في المبنى")
            .addData("city", "صنعاء")
            .build()

        fcmService.onMessageReceived(message)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
    }

    @Test
    fun `test onMessageReceived handles password recovery critical notification`() {
        val message = RemoteMessage.Builder("sender")
            .addData("type", "PASSWORD_RECOVERY")
            .addData("requestId", "req_pwd_123")
            .addData("phone", "+967771234567")
            .addData("name", "صالح أحمد")
            .addData("accountType", "فني")
            .build()

        fcmService.onMessageReceived(message)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        val notif = notifications.first()
        assertEquals(NotificationHelper.CHANNEL_ADMIN_CRITICAL, notif.channelId)
    }

    @Test
    fun `test onMessageReceived fallback general notification`() {
        val message = RemoteMessage.Builder("sender")
            .addData("title", "عرض خاص اليوم")
            .addData("body", "خصم 20% على جميع خدمات الصيانة")
            .addData("targetScreen", "OFFERS")
            .build()

        fcmService.onMessageReceived(message)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
    }
}
