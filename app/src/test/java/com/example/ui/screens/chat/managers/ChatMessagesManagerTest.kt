package com.example.ui.screens.chat.managers

import com.example.data.models.ChatMessage
import com.example.data.models.MediaType
import com.example.utils.CoroutineTestRule
import com.example.utils.TestMockFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessagesManagerTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val fakeRepo = TestMockFactory.createChatRepository()
    private lateinit var manager: ChatMessagesManager

    @Before
    fun setup() {
        manager = ChatMessagesManager(fakeRepo, kotlinx.coroutines.CoroutineScope(coroutineRule.testDispatcher))
    }



    @Test
    fun testUpdateMessagesList() {
        val initialList = listOf(
            ChatMessage(id = "msg1", channelId = "c1", message = "مرحبا"),
            ChatMessage(id = "msg2", channelId = "c1", message = "أهلا وسهلا")
        )
        manager.updateMessagesList(initialList)
        assertEquals(2, manager.messages.value.size)
        assertEquals("مرحبا", manager.messages.value[0].message)
    }

    @Test
    fun testSetSending() {
        assertFalse(manager.isSending.value)
        manager.setSending(true)
        assertTrue(manager.isSending.value)
        manager.setSending(false)
        assertFalse(manager.isSending.value)
    }

    @Test
    fun testSendMessageSuccess() = runTest {
        var successMessage: ChatMessage? = null
        var errorMessage: String? = null

        manager.sendMessage(
            channelId = "c1",
            senderId = "u1",
            senderName = "أحمد",
            text = "طلب خدمة صيانة",
            mediaType = MediaType.TEXT,
            onSuccess = { successMessage = it },
            onError = { errorMessage = it }
        )

        assertNotNull(successMessage)
        assertNull(errorMessage)
        assertEquals("طلب خدمة صيانة", successMessage?.message)
        assertEquals("c1", successMessage?.channelId)
    }
}
