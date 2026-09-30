package com.example.utils

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.FCMService
import com.google.firebase.messaging.RemoteMessage
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * 🔔 FCMServiceUnitTest
 * Unit tests for Firebase Cloud Messaging service verifying payload parsing,
 * critical notifications, chat and urgent dispatch, and token caching.
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
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        notificationManager.cancelAll()

        fcmService = Robolectric.buildService(FCMService::class.java).create().get()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
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
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setData(
                mapOf(
                    "type" to "CHAT",
                    "channelId" to "channel_test_456",
                    "senderId" to "user_sender_1",
                    "senderName" to "محمد اليماني",
                    "message" to "مرحباً، هل الخدمة متاحة؟",
                    "mediaType" to "TEXT"
                )
            )
            .build()

        fcmService.onMessageReceived(remoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        val notif = notifications.first()
        assertEquals(ChatNotificationHelper.CHANNEL_MESSAGES, notif.channelId)
    }

    @Test
    fun `test onMessageReceived handles urgent request payload`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setData(
                mapOf(
                    "type" to "URGENT",
                    "requestCode" to "URG-7788",
                    "title" to "عطل طارئ في شبكة الكهرباء",
                    "description" to "انقطاع كامل في المبنى",
                    "city" to "صنعاء"
                )
            )
            .build()

        fcmService.onMessageReceived(remoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        val notif = notifications.first()
        assertEquals(ChatNotificationHelper.CHANNEL_URGENT, notif.channelId)
    }

    @Test
    fun `test onMessageReceived handles password recovery critical notification`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setData(
                mapOf(
                    "type" to "PASSWORD_RECOVERY",
                    "requestId" to "req_pwd_123",
                    "phone" to "+967771234567",
                    "name" to "صالح أحمد",
                    "accountType" to "فني"
                )
            )
            .build()

        fcmService.onMessageReceived(remoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        val notif = notifications.first()
        assertEquals(NotificationHelper.CHANNEL_ADMIN_CRITICAL, notif.channelId)
    }

    @Test
    fun `test onMessageReceived fallback general notification`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setData(
                mapOf(
                    "title" to "عرض خاص اليوم",
                    "body" to "خصم 20% على جميع خدمات الصيانة",
                    "targetScreen" to "OFFERS"
                )
            )
            .build()

        fcmService.onMessageReceived(remoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
    }
}
