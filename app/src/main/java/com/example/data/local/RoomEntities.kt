package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.BookingEntity
import com.example.data.models.*

@Entity(
    tableName = "chat_channels",
    indices = [
        Index(value = ["lastMessageTime"]),
        Index(value = ["updatedAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class ChatChannelRoomEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val participantsJson: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val lastMessageSenderId: String,
    val unreadCountJson: String,
    val syncStatus: String,
    val updatedAt: Long
)

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["timestamp"]),
        Index(value = ["senderId"]),
        Index(value = ["status"]),
        Index(value = ["syncStatus"]),
        Index(value = ["channelId", "timestamp"])
    ]
)
data class ChatMessageRoomEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderPhoto: String,
    val message: String,
    val mediaType: String,
    val mediaUrl: String,
    val status: String,
    val isEncrypted: Boolean,
    val timestamp: Long,
    val syncStatus: String
)

@Entity(
    tableName = "bookings",
    indices = [
        Index(value = ["providerId"]),
        Index(value = ["customerPhone"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["scheduledAt"]),
        Index(value = ["providerId", "status"])
    ]
)
data class BookingRoomEntity(
    @PrimaryKey val id: String,
    val customerName: String,
    val customerPhone: String,
    val customerArea: String,
    val serviceType: String,
    val providerId: String,
    val providerName: String,
    val dateString: String,
    val timeString: String,
    val status: String,
    val pinCode: String,
    val bookingNumber: String,
    val totalAmount: Double,
    val advancePayment: Double,
    val paymentStatus: String,
    val scheduledAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "instant_requests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["requestCode"])
    ]
)
data class InstantRequestRoomEntity(
    @PrimaryKey val id: String,
    val requestCode: String,
    val secretPin: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userCity: String,
    val serviceTitle: String,
    val description: String,
    val status: String,
    val acceptedPrice: Double,
    val createdAt: Long,
    val expiresAt: Long,
    val offersCount: Int
)

@Entity(
    tableName = "request_offers",
    indices = [
        Index(value = ["requestId"]),
        Index(value = ["requestCode"]),
        Index(value = ["technicianId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class RequestOfferRoomEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val requestCode: String,
    val technicianId: String,
    val technicianName: String,
    val technicianPhone: String,
    val price: Double,
    val estimatedArrivalTime: String,
    val status: String,
    val createdAt: Long
)
