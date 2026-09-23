package com.example.utils

import com.example.data.BookingEntity
import com.example.data.models.*
import com.example.data.repositories.IBookingRepository
import com.example.data.repositories.IChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * 🧪 Fake implementations for unit testing repositories without external dependencies.
 */
class FakeBookingRepository : IBookingRepository {
    val bookings = mutableListOf<BookingEntity>()
    private val _cached = MutableStateFlow<List<BookingEntity>>(emptyList())
    override val cachedBookings: StateFlow<List<BookingEntity>> = _cached.asStateFlow()

    override fun getUserBookings(userId: String, pageLimit: Long): Flow<List<BookingEntity>> =
        flowOf(bookings.filter { it.clientId == userId || it.clientPhone == userId || it.customerPhone == userId })

    override fun getProviderBookings(providerId: String, pageLimit: Long): Flow<List<BookingEntity>> =
        flowOf(bookings.filter { it.providerId == providerId })

    override fun getBookingsFlow(userId: String, isProvider: Boolean): Flow<List<BookingEntity>> =
        flowOf(bookings)

    override fun createBooking(
        booking: BookingEntity,
        rawPasswordPin: String,
        onSuccess: (BookingEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        bookings.add(booking)
        _cached.value = bookings.toList()
        onSuccess(booking)
    }

    override fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val idx = bookings.indexOfFirst { it.id == bookingId }
        if (idx != -1) {
            bookings[idx] = bookings[idx].copy(status = newStatus)
            _cached.value = bookings.toList()
            onSuccess()
        } else {
            onError("Booking not found")
        }
    }

    override fun cancelBookingWithSecurity(
        booking: BookingEntity,
        inputPinOrPassword: String,
        cancellationReason: String,
        cancelledBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateBookingStatus(booking.id, "CANCELLED", onSuccess, onError)
    }

    override fun deleteBooking(bookingId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        bookings.removeAll { it.id == bookingId }
        _cached.value = bookings.toList()
        onSuccess()
    }

    override fun updateBookingDetails(
        updatedBooking: BookingEntity,
        inputPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val idx = bookings.indexOfFirst { it.id == updatedBooking.id }
        if (idx != -1) {
            bookings[idx] = updatedBooking
            _cached.value = bookings.toList()
            onSuccess()
        } else {
            onError("Not found")
        }
    }
}

class FakeChatRepository : IChatRepository {
    val messages = mutableListOf<ChatMessage>()
    val channels = mutableListOf<ChatChannel>()

    override suspend fun getOrCreateChannel(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String,
        type: ChannelType,
        relatedEntityId: String?,
        relatedEntityType: String?
    ): AppResult<ChatChannel> {
        val ch = ChatChannel(id = "ch_test", participants = listOf(currentUserId, otherUserId))
        channels.add(ch)
        return AppResult.Success(ch)
    }

    override suspend fun getChannelById(channelId: String): AppResult<ChatChannel?> =
        AppResult.Success(channels.firstOrNull { it.id == channelId })

    override fun getUserChannels(userId: String): Flow<List<ChatChannel>> = flowOf(channels)

    override fun getChannelMessages(channelId: String, currentUserId: String, limit: Int): Flow<List<ChatMessage>> =
        flowOf(messages.filter { it.channelId == channelId })

    override suspend fun sendMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        mediaType: MediaType,
        mediaUrl: String,
        replyToId: String?,
        replyToText: String?,
        attachment: ChatAttachment?
    ): AppResult<ChatMessage> {
        val msg = ChatMessage(
            id = "msg_${messages.size + 1}",
            channelId = channelId,
            senderId = senderId,
            message = messageText
        )
        messages.add(msg)
        return AppResult.Success(msg)
    }

    override suspend fun retryPendingMessages(currentUserId: String): AppResult<Int> = AppResult.Success(0)
    override suspend fun markChannelAsRead(channelId: String, currentUserId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun setTyping(channelId: String, userId: String, isTyping: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleBlockUser(channelId: String, userIdToBlock: String, isBlocked: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun editMessage(channelId: String, messageId: String, newText: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteMessage(channelId: String, messageId: String, forEveryone: Boolean, currentUserId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleReaction(channelId: String, messageId: String, userId: String, emoji: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteChannel(channelId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAllChannels(channelsList: List<ChatChannel>): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun setUserPresence(userId: String, isOnline: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override fun getUserPresence(userId: String): Flow<UserPresence?> = flowOf(null)
    override fun getTypingStatus(channelId: String, userId: String): Flow<Boolean> = flowOf(false)
    override suspend fun syncChannelDelta(channelId: String): AppResult<Int> = AppResult.Success(0)
}

/**
 * Factory to provide common mock and fake objects for unit tests.
 */
object TestMockFactory {
    fun createBookingRepository(): IBookingRepository = FakeBookingRepository()
    fun createChatRepository(): IChatRepository = FakeChatRepository()
}
