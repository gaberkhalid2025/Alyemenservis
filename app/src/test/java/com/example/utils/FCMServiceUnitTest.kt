package com.example.utils

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.FCMService
import com.google.firebase.messaging.RemoteMessage
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.Shadows.shadowOf

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
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        mockkObject(ChatNotificationHelper)
        every {
            ChatNotificationHelper.showChatMessageNotification(
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Unit

        every {
            ChatNotificationHelper.showUrgentRequestNotification(
                any(), any(), any(), any(), any(), any()
            )
        } returns Unit

        fcmService = spyk(FCMService())
        every { fcmService.applicationContext } returns context
        every { fcmService.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { fcmService.getSharedPreferences(any(), any()) } answers {
            context.getSharedPreferences(firstArg(), secondArg())
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
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
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val dataMap = mapOf(
            "type" to "CHAT",
            "channelId" to "channel_test_456",
            "senderId" to "user_sender_1",
            "senderName" to "محمد اليماني",
            "message" to "مرحباً، هل الخدمة متاحة؟",
            "mediaType" to "TEXT"
        )
        every { mockRemoteMessage.data } returns dataMap
        every { mockRemoteMessage.notification } returns null

        fcmService.onMessageReceived(mockRemoteMessage)

        verify(exactly = 1) {
            ChatNotificationHelper.showChatMessageNotification(
                context = any(),
                notificationId = any(),
                channelId = "channel_test_456",
                senderId = "user_sender_1",
                senderName = "محمد اليماني",
                messageText = "مرحباً، هل الخدمة متاحة؟",
                mediaType = "TEXT",
                mediaUrl = null
            )
        }
    }

    @Test
    fun `test onMessageReceived handles urgent request payload`() {
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val dataMap = mapOf(
            "type" to "URGENT",
            "requestCode" to "URG-7788",
            "title" to "عطل طارئ في شبكة الكهرباء",
            "description" to "انقطاع كامل في المبنى",
            "city" to "صنعاء"
        )
        every { mockRemoteMessage.data } returns dataMap
        every { mockRemoteMessage.notification } returns null

        fcmService.onMessageReceived(mockRemoteMessage)

        verify(exactly = 1) {
            ChatNotificationHelper.showUrgentRequestNotification(
                context = any(),
                notificationId = any(),
                requestCode = "URG-7788",
                title = "عطل طارئ في شبكة الكهرباء",
                description = "انقطاع كامل في المبنى",
                city = "صنعاء"
            )
        }
    }

    @Test
    fun `test onMessageReceived handles password recovery critical notification`() {
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val dataMap = mapOf(
            "type" to "PASSWORD_RECOVERY",
            "requestId" to "req_pwd_123",
            "phone" to "+967771234567",
            "name" to "صالح أحمد",
            "accountType" to "فني"
        )
        every { mockRemoteMessage.data } returns dataMap
        every { mockRemoteMessage.notification } returns null

        fcmService.onMessageReceived(mockRemoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        val notif = notifications.first()
        assertEquals(NotificationHelper.CHANNEL_ADMIN_CRITICAL, notif.channelId)
    }

    @Test
    fun `test onMessageReceived fallback general notification`() {
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val dataMap = mapOf(
            "title" to "عرض خاص اليوم",
            "body" to "خصم 20% على جميع خدمات الصيانة",
            "targetScreen" to "OFFERS"
        )
        every { mockRemoteMessage.data } returns dataMap
        every { mockRemoteMessage.notification } returns null

        fcmService.onMessageReceived(mockRemoteMessage)

        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
    }
}
