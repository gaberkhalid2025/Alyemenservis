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

fun com.example.data.BookingEntity.toRoomEntity(): BookingRoomEntity {
    return BookingRoomEntity(
        id = id,
        customerName = customerName.ifBlank { clientName.ifBlank { fullName } },
        customerPhone = customerPhone.ifBlank { clientPhone.ifBlank { userPhone } },
        customerArea = customerArea.ifBlank { clientAddress.ifBlank { fullAddress } },
        serviceType = serviceType.ifBlank { serviceName.ifBlank { category } },
        providerId = providerId.ifBlank { technicianId },
        providerName = providerName.ifBlank { technicianName },
        dateString = date.ifBlank { dateString },
        timeString = time.ifBlank { timeString },
        status = status,
        pinCode = pinCode,
        bookingNumber = bookingNumber.ifBlank { bookingCode },
        totalAmount = totalAmount,
        advancePayment = advancePayment,
        paymentStatus = paymentStatus,
        scheduledAt = scheduledAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun BookingRoomEntity.toEntity(): com.example.data.BookingEntity {
    return com.example.data.BookingEntity(
        id = id,
        customerName = customerName,
        customerPhone = customerPhone,
        customerArea = customerArea,
        serviceType = serviceType,
        providerId = providerId,
        providerName = providerName,
        date = dateString,
        time = timeString,
        dateString = dateString,
        timeString = timeString,
        status = status,
        pinCode = pinCode,
        bookingNumber = bookingNumber,
        bookingCode = bookingNumber,
        totalAmount = totalAmount,
        advancePayment = advancePayment,
        paymentStatus = paymentStatus,
        scheduledAt = scheduledAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Entity(
    tableName = "instant_requests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userPhone"]),
        Index(value = ["userCity"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["expiresAt"]),
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

fun InstantRequestEntity.toRoomEntity(): InstantRequestRoomEntity {
    return InstantRequestRoomEntity(
        id = id,
        requestCode = requestCode,
        secretPin = effectivePinHash,
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        userCity = userCity,
        serviceTitle = serviceTitle.ifBlank { categoryName },
        description = description,
        status = status,
        acceptedPrice = acceptedPrice,
        createdAt = createdAt,
        expiresAt = expiresAt,
        offersCount = offersCount
    )
}

fun InstantRequestRoomEntity.toEntity(): InstantRequestEntity {
    return InstantRequestEntity(
        id = id,
        requestCode = requestCode,
        pinHash = secretPin,
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        userCity = userCity,
        serviceTitle = serviceTitle,
        description = description,
        status = status,
        acceptedPrice = acceptedPrice,
        createdAt = createdAt,
        expiresAt = expiresAt,
        offersCount = offersCount
    )
}

fun RequestOfferEntity.toRoomEntity(): RequestOfferRoomEntity {
    return RequestOfferRoomEntity(
        id = id,
        requestId = requestId,
        requestCode = requestCode,
        technicianId = technicianId,
        technicianName = technicianName,
        technicianPhone = technicianPhone,
        price = price,
        estimatedArrivalTime = estimatedArrivalTime,
        status = status,
        createdAt = createdAt
    )
}

fun RequestOfferRoomEntity.toEntity(): RequestOfferEntity {
    return RequestOfferEntity(
        id = id,
        requestId = requestId,
        requestCode = requestCode,
        technicianId = technicianId,
        technicianName = technicianName,
        technicianPhone = technicianPhone,
        price = price,
        estimatedArrivalTime = estimatedArrivalTime,
        status = status,
        createdAt = createdAt
    )
}

