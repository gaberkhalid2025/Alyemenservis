package com.example.data.repositories

import com.example.data.models.*
import com.example.utils.AppResult
import com.example.utils.CoroutineTestRule
import com.example.utils.TestMockFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var repository: IChatRepository

    @Before
    fun setup() {
        repository = TestMockFactory.createChatRepository()
    }

    @Test
    fun `test sendMessage - basics`() = runTest {
        val result = repository.sendMessage(
            channelId = "c1",
            senderId = "u1",
            senderName = "User 1",
            messageText = "مرحباً بك",
            mediaType = MediaType.TEXT,
            mediaUrl = "",
            replyToId = null,
            replyToText = null,
            attachment = null
        )

        assertTrue("Message send should succeed", result is AppResult.Success)
        val sent = (result as AppResult.Success).data
        assertEquals("مرحباً بك", sent.message)
        assertEquals("c1", sent.channelId)
        assertEquals("u1", sent.senderId)

        val messages = repository.getChannelMessages("c1", "u1").first()
        assertEquals(1, messages.size)
        assertEquals("مرحباً بك", messages[0].message)
    }

    @Test
    fun `test getOrCreateChannel - creates new channel`() = runTest {
        val res = repository.getOrCreateChannel(
            currentUserId = "user_a",
            currentUserName = "عميل 1",
            currentUserPhoto = "",
            otherUserId = "user_b",
            otherUserName = "مزود 1",
            otherUserPhoto = "",
            type = ChannelType.PRIVATE
        )

        assertTrue(res is AppResult.Success)
        val channel = (res as AppResult.Success).data
        assertTrue(channel.participants.contains("user_a"))
        assertTrue(channel.participants.contains("user_b"))
    }

    @Test
    fun `test markChannelAsRead - succeeds`() = runTest {
        val result = repository.markChannelAsRead("c1", "u1")
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `test setTyping - succeeds`() = runTest {
        val result = repository.setTyping("c1", "u1", true)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `test toggleBlockUser - succeeds`() = runTest {
        val result = repository.toggleBlockUser("c1", "u2", true)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `test sendMessage with attachment - succeeds`() = runTest {
        val attachment = ChatAttachment(
            url = "https://example.com/img.jpg",
            fileName = "image.jpg",
            mimeType = "image/jpeg"
        )
        val result = repository.sendMessage(
            channelId = "c2",
            senderId = "u1",
            senderName = "User 1",
            messageText = "صورة العطل",
            mediaType = MediaType.IMAGE,
            mediaUrl = "https://example.com/img.jpg",
            attachment = attachment
        )
        assertTrue(result is AppResult.Success)
        val msg = (result as AppResult.Success).data
        assertEquals("صورة العطل", msg.message)
    }

    @Test
    fun `test editMessage - succeeds`() = runTest {
        val result = repository.editMessage("c1", "msg_1", "رسالة معدلة")
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `test deleteMessage - succeeds`() = runTest {
        val result = repository.deleteMessage("c1", "msg_1", forEveryone = true, currentUserId = "u1")
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `test toggleReaction - succeeds`() = runTest {
        val result = repository.toggleReaction("c1", "msg_1", "u1", "👍")
        assertTrue(result is AppResult.Success)
    }
}
