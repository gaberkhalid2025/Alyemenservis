package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_channels ORDER BY lastMessageTime DESC")
    fun getAllChannelsFlow(): Flow<List<ChatChannelRoomEntity>>

    @Query("SELECT * FROM chat_channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChatChannelRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChatChannelRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChatChannelRoomEntity)

    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    fun getMessagesPaged(channelId: String, limit: Int, offset: Int): Flow<List<ChatMessageRoomEntity>>

    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesFlow(channelId: String): Flow<List<ChatMessageRoomEntity>>

    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    suspend fun getMessagesList(channelId: String): List<ChatMessageRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageRoomEntity)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE chat_messages SET syncStatus = :syncStatus WHERE id = :messageId")
    suspend fun updateMessageSyncStatus(messageId: String, syncStatus: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chat_messages WHERE channelId = :channelId")
    suspend fun clearChannelMessages(channelId: String)

    @Query("DELETE FROM chat_channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Transaction
    suspend fun replaceChannelWithMessages(channel: ChatChannelRoomEntity, messages: List<ChatMessageRoomEntity>) {
        insertChannel(channel)
        insertMessages(messages)
    }
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getBookingsPaged(limit: Int, offset: Int): Flow<List<BookingRoomEntity>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId OR customerPhone = :phone ORDER BY createdAt DESC")
    fun getBookingsForUser(providerId: String, phone: String): Flow<List<BookingRoomEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: String): BookingRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingRoomEntity)

    @Query("UPDATE bookings SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookingStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBooking(id: String)
}

@Dao
interface RequestDao {
    @Query("SELECT * FROM instant_requests ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getRequestsPaged(limit: Int, offset: Int): Flow<List<InstantRequestRoomEntity>>

    @Query("SELECT * FROM instant_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRequestsForUser(userId: String): Flow<List<InstantRequestRoomEntity>>

    @Query("SELECT * FROM instant_requests WHERE id = :id")
    suspend fun getRequestById(id: String): InstantRequestRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<InstantRequestRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: InstantRequestRoomEntity)

    @Query("UPDATE instant_requests SET status = :status WHERE id = :id")
    suspend fun updateRequestStatus(id: String, status: String)

    @Query("DELETE FROM instant_requests WHERE id = :id")
    suspend fun deleteRequest(id: String)
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM request_offers WHERE requestId = :requestId ORDER BY createdAt DESC")
    fun getOffersForRequest(requestId: String): Flow<List<RequestOfferRoomEntity>>

    @Query("SELECT * FROM request_offers WHERE requestId = :requestId")
    suspend fun getOffersListForRequest(requestId: String): List<RequestOfferRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<RequestOfferRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: RequestOfferRoomEntity)

    @Query("UPDATE request_offers SET status = :status WHERE id = :id")
    suspend fun updateOfferStatus(id: String, status: String)

    @Query("DELETE FROM request_offers WHERE id = :id")
    suspend fun deleteOffer(id: String)
}
