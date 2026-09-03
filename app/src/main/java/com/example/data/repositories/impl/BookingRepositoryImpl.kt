package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.BookingEntity
import com.example.data.LocalAppCacheManager
import com.example.data.repositories.contracts.IBookingRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.example.security.BookingSecurityHelper
import com.example.utils.BookingUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class BookingRepositoryImpl(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IBookingRepository {

    private val cacheManager = LocalAppCacheManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private val _cachedBookings = MutableStateFlow<List<BookingEntity>>(emptyList())

    init {
        loadFromCache()
    }

    private fun loadFromCache(): List<BookingEntity> {
        return try {
            val raw = cacheManager.getBookingsCacheRaw()
            if (raw.isNotBlank() && raw != "[]") {
                val type = Types.newParameterizedType(List::class.java, BookingEntity::class.java)
                val adapter = moshi.adapter<List<BookingEntity>>(type)
                val list = adapter.fromJson(raw) ?: emptyList()
                _cachedBookings.value = list
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BookingRepositoryImpl", "Error reading local cache", e)
            emptyList()
        }
    }

    private fun saveToCache(list: List<BookingEntity>) {
        try {
            val type = Types.newParameterizedType(List::class.java, BookingEntity::class.java)
            val adapter = moshi.adapter<List<BookingEntity>>(type)
            val json = adapter.toJson(list)
            cacheManager.saveBookingsCache(json)
            _cachedBookings.value = list
        } catch (e: Exception) {
            Log.e("BookingRepositoryImpl", "Error saving local cache", e)
        }
    }

    override fun observeBookings(): Flow<List<BookingEntity>> = callbackFlow {
        trySend(loadFromCache())
        val listener = firestore.collection("bookings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                    }
                    saveToCache(list)
                    trySend(list)
                }
            }
        activeListeners.add(listener)
        awaitClose { listener.remove() }
    }

    override fun getBookingsFlow(userId: String, isProvider: Boolean): Flow<List<BookingEntity>> = callbackFlow {
        trySend(loadFromCache())
        val collection = firestore.collection("bookings")
        val query = if (userId.isNotBlank()) {
            if (isProvider) collection.whereEqualTo("providerId", userId)
            else collection.whereEqualTo("clientId", userId)
        } else {
            collection.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                }
                saveToCache(list)
                trySend(list)
            }
        }
        activeListeners.add(listener)
        awaitClose { listener.remove() }
    }

    override fun clearListeners() {
        try {
            activeListeners.forEach { it.remove() }
            activeListeners.clear()
        } catch (e: Exception) {
            Log.e("BookingRepositoryImpl", "Error clearing listeners", e)
        }
    }

    override suspend fun createBooking(booking: BookingEntity, rawPasswordPin: String): AppResult<BookingEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = booking.id.ifBlank { firestore.collection("bookings").document().id }
            val finalCode = booking.bookingNumber.ifBlank { BookingUtils.generateBookingNumber() }
            val rawPin = rawPasswordPin.ifBlank { BookingUtils.generateBookingPassword() }
            val hashedPin = BookingSecurityHelper.hashPin(rawPin)

            val scheduledTs = if (booking.scheduledAt > 0) booking.scheduledAt
            else BookingUtils.parseScheduledTimestamp(booking.date.ifBlank { booking.dateString }, booking.time.ifBlank { booking.timeString })

            val finalBooking = booking.copy(
                id = docId,
                bookingNumber = finalCode,
                bookingCode = finalCode,
                bookingPassword = rawPin,
                pinCode = hashedPin,
                scheduledAt = scheduledTs,
                status = booking.status.ifBlank { "PENDING" },
                createdAt = if (booking.createdAt > 0) booking.createdAt else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Cache locally
            val current = loadFromCache().toMutableList()
            current.removeAll { it.id == docId }
            current.add(0, finalBooking)
            saveToCache(current)

            // Save to Firestore
            firestore.collection("bookings").document(docId).set(finalBooking).await()

            // Trigger notification payloads in "notifications" collection
            val userNotifId = UUID.randomUUID().toString()
            firestore.collection("notifications").document(userNotifId).set(
                mapOf(
                    "id" to userNotifId,
                    "title" to "📅 تم إنشاء حجزك بنجاح",
                    "message" to "مرحباً! تم استلام طلب حجزك برقم #${finalBooking.bookingNumber} وهو قيد المراجعة.",
                    "targetType" to "USER",
                    "targetValue" to finalBooking.customerPhone,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            if (finalBooking.providerPhone.isNotBlank()) {
                val providerNotifId = UUID.randomUUID().toString()
                firestore.collection("notifications").document(providerNotifId).set(
                    mapOf(
                        "id" to providerNotifId,
                        "title" to "🔔 طلب حجز جديد",
                        "message" to "لديك طلب حجز جديد برقم #${finalBooking.bookingNumber} من العميل ${finalBooking.customerName}.",
                        "targetType" to "PROVIDER",
                        "targetValue" to finalBooking.providerPhone,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
            }

            Result.success(finalBooking)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إنشاء الحجز"))
        }
    }

    override suspend fun updateBookingStatus(bookingId: String, newStatus: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis(),
                if (newStatus == "COMPLETED") "completedAt" to System.currentTimeMillis() else "updatedAt" to System.currentTimeMillis()
            )

            // Optimistic local update
            val current = loadFromCache().map {
                if (it.id == bookingId) it.copy(status = newStatus, updatedAt = System.currentTimeMillis()) else it
            }
            saveToCache(current)

            firestore.collection("bookings").document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث حالة الحجز"))
        }
    }

    override suspend fun deleteBooking(bookingId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = loadFromCache().filter { it.id != bookingId }
            saveToCache(current)

            firestore.collection("bookings").document(bookingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف الحجز"))
        }
    }

    override suspend fun deleteAllBookings(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            saveToCache(emptyList())
            val snap = firestore.collection("bookings").get().await()
            val batch = firestore.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف جميع الحجوزات"))
        }
    }

    override suspend fun updateBooking(booking: BookingEntity, inputPin: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (BookingSecurityHelper.isBookingLocked(context, booking.id)) {
                val remainingSec = BookingSecurityHelper.getRemainingLockoutSeconds(context, booking.id)
                return@withContext Result.failure(AppError.ValidationError("PIN", "الحجز مقفل مؤقتاً. انتظر ${remainingSec / 60} دقيقة."))
            }

            val canModify = BookingUtils.canModifyOrCancelBooking(
                scheduledAtTimestamp = booking.scheduledAt,
                dateString = booking.date.ifBlank { booking.dateString },
                timeString = booking.time.ifBlank { booking.timeString }
            )
            if (!canModify) {
                return@withContext Result.failure(AppError.ValidationError("DATE", "لا يمكن تعديل الحجز عند بقاء أقل من 8 ساعات على الموعد."))
            }

            if (inputPin.isNotBlank()) {
                val expectedTarget = booking.pinCode.ifBlank { booking.bookingPassword }
                val isVerified = BookingSecurityHelper.verifyPassword(inputPin, expectedTarget)
                if (!isVerified) {
                    val left = BookingSecurityHelper.recordFailedAttempt(context, booking.id)
                    return@withContext Result.failure(AppError.ValidationError("PIN", "رمز التحقق غير صحيح. متبقي $left محاولات."))
                }
            }

            BookingSecurityHelper.resetAttempts(context, booking.id)

            val itemToSave = booking.copy(updatedAt = System.currentTimeMillis())
            val current = loadFromCache().map { if (it.id == itemToSave.id) itemToSave else it }
            saveToCache(current)

            firestore.collection("bookings").document(itemToSave.id).set(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تحديث بيانات الحجز"))
        }
    }

    override suspend fun cancelByUser(booking: BookingEntity, inputPin: String, cancellationReason: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (BookingSecurityHelper.isBookingLocked(context, booking.id)) {
                val remainingSec = BookingSecurityHelper.getRemainingLockoutSeconds(context, booking.id)
                return@withContext Result.failure(AppError.ValidationError("PIN", "تم قفل هذا الحجز مؤقتاً بسبب محاولات خاطئة. المحاولة بعد ${remainingSec / 60} دقيقة."))
            }

            val canCancel = BookingUtils.canModifyOrCancelBooking(
                scheduledAtTimestamp = booking.scheduledAt,
                dateString = booking.date.ifBlank { booking.dateString },
                timeString = booking.time.ifBlank { booking.timeString }
            )
            if (!canCancel) {
                return@withContext Result.failure(AppError.ValidationError("TIME", "عذراً، تنص سياسة الخدمة على عدم إمكانية إلغاء الحجز إذا تبقى أقل من 8 ساعات على الموعد."))
            }

            val expectedTarget = booking.pinCode.ifBlank { booking.bookingPassword }
            val isVerified = BookingSecurityHelper.verifyPassword(inputPin, expectedTarget)

            if (!isVerified) {
                val attemptsLeft = BookingSecurityHelper.recordFailedAttempt(context, booking.id)
                return@withContext Result.failure(AppError.ValidationError("PIN", "رمز التحقق غير صحيح. متبقي لديك $attemptsLeft محاولة فقط قبل القفل المؤقت."))
            }

            BookingSecurityHelper.resetAttempts(context, booking.id)

            val updates = mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to cancellationReason,
                "cancelledAt" to System.currentTimeMillis(),
                "cancelledBy" to "USER",
                "updatedAt" to System.currentTimeMillis()
            )

            val current = loadFromCache().map {
                if (it.id == booking.id) it.copy(
                    status = "CANCELLED",
                    cancellationReason = cancellationReason,
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "USER",
                    updatedAt = System.currentTimeMillis()
                ) else it
            }
            saveToCache(current)

            firestore.collection("bookings").document(booking.id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إلغاء الحجز"))
        }
    }

    override suspend fun attemptCancel(booking: BookingEntity, inputPin: String, cancellationReason: String): AppResult<Unit> {
        return cancelByUser(booking, inputPin, cancellationReason)
    }

    override suspend fun cancelByTechnician(bookingId: String, cancellationReason: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to cancellationReason,
                "cancelledAt" to System.currentTimeMillis(),
                "cancelledBy" to "PROVIDER",
                "updatedAt" to System.currentTimeMillis()
            )

            val current = loadFromCache().map {
                if (it.id == bookingId) it.copy(
                    status = "CANCELLED",
                    cancellationReason = cancellationReason,
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "PROVIDER",
                    updatedAt = System.currentTimeMillis()
                ) else it
            }
            saveToCache(current)

            firestore.collection("bookings").document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إلغاء الحجز من قبل الفني"))
        }
    }

    override suspend fun cancelByAdmin(bookingId: String, cancellationReason: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to cancellationReason,
                "cancelledAt" to System.currentTimeMillis(),
                "cancelledBy" to "ADMIN",
                "updatedAt" to System.currentTimeMillis()
            )

            val current = loadFromCache().map {
                if (it.id == bookingId) it.copy(
                    status = "CANCELLED",
                    cancellationReason = cancellationReason,
                    cancelledAt = System.currentTimeMillis(),
                    cancelledBy = "ADMIN",
                    updatedAt = System.currentTimeMillis()
                ) else it
            }
            saveToCache(current)

            firestore.collection("bookings").document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل إلغاء الحجز من قبل المسؤول"))
        }
    }

    override fun getStatusColor(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "#FFA500"
            "APPROVED", "ACCEPTED" -> "#008000"
            "IN_PROGRESS" -> "#0000FF"
            "COMPLETED" -> "#4CAF50"
            "CANCELLED", "REJECTED" -> "#FF0000"
            else -> "#808080"
        }
    }

    override fun getStatusLabel(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> "قيد المراجعة"
            "APPROVED" -> "مقبول"
            "ACCEPTED" -> "تم قبول الطلب"
            "IN_PROGRESS" -> "قيد التنفيذ"
            "COMPLETED" -> "مكتمل"
            "CANCELLED" -> "ملغي"
            "REJECTED" -> "مرفوض"
            else -> status
        }
    }

    override fun getProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING" -> 0.25f
            "APPROVED", "ACCEPTED" -> 0.50f
            "IN_PROGRESS" -> 0.75f
            "COMPLETED" -> 1.0f
            else -> 0f
        }
    }

    override suspend fun createDirectBooking(booking: BookingEntity): AppResult<BookingEntity> {
        return createBooking(booking, "")
    }

    override suspend fun updateBookingStatusEnum(bookingId: String, newStatus: String): AppResult<Unit> {
        return updateBookingStatus(bookingId, newStatus)
    }
}
