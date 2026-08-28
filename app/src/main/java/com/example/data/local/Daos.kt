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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageRoomEntity)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
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
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM request_offers WHERE requestId = :requestId ORDER BY createdAt DESC")
    fun getOffersForRequest(requestId: String): Flow<List<RequestOfferRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<RequestOfferRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: RequestOfferRoomEntity)
}
