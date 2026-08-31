package com.example.data.repositories

import android.content.Context
import android.util.Log
import com.example.data.BookingEntity
import com.example.data.LocalAppCacheManager
import com.example.security.BookingSecurityHelper
import com.example.utils.BookingUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * 📦 BookingRepository
 * Offline-First repository implementation for bookings.
 * Synchronizes with Firestore while caching locally via LocalAppCacheManager and Moshi.
 * Enforces security validations, 8-hour countdown rule, and SHA-256 PIN hashing.
 */
class BookingRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val cacheManager = LocalAppCacheManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val _cachedBookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val cachedBookings: StateFlow<List<BookingEntity>> = _cachedBookings.asStateFlow()

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
            Log.e("BookingRepository", "Error reading local cache", e)
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
            Log.e("BookingRepository", "Error saving local cache", e)
        }
    }

    /**
     * Realtime flow of all bookings for a user with offline fallback.
     */
    fun getUserBookings(userId: String, pageLimit: Long = 50): Flow<List<BookingEntity>> = getBookingsFlow(userId, isProvider = false)

    /**
     * Realtime flow of all bookings for a provider with offline fallback.
     */
    fun getProviderBookings(providerId: String, pageLimit: Long = 50): Flow<List<BookingEntity>> = getBookingsFlow(providerId, isProvider = true)

    /**
     * Realtime flow of all bookings for a user or provider with offline fallback.
     */
    fun getBookingsFlow(userId: String, isProvider: Boolean = false): Flow<List<BookingEntity>> = callbackFlow {
        // Emit cache immediately for instant offline rendering
        val local = loadFromCache()
        if (local.isNotEmpty()) {
            val filtered = if (userId.isNotBlank()) {
                local.filter { if (isProvider) it.providerId == userId else it.clientId == userId || it.customerPhone.isNotBlank() }
            } else local
            trySend(filtered)
        }

        val collection = firestore.collection("bookings")
        val query = if (userId.isNotBlank()) {
            if (isProvider) collection.whereEqualTo("providerId", userId)
            else collection.whereEqualTo("clientId", userId)
        } else {
            collection.orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
        }

        val listener: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("BookingRepository", "Firestore listener failed: ${error.message}")
                trySend(loadFromCache())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(BookingEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                saveToCache(list)
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Creates a new booking with auto-generated booking code and hashed PIN.
     */
    fun createBooking(
        booking: BookingEntity,
        rawPasswordPin: String = "",
        onSuccess: (BookingEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val docId = if (booking.id.isNotBlank()) booking.id else firestore.collection("bookings").document().id
            val finalCode = if (booking.bookingNumber.isNotBlank()) booking.bookingNumber else BookingUtils.generateBookingNumber()
            val rawPin = if (rawPasswordPin.isNotBlank()) rawPasswordPin else BookingUtils.generateBookingPassword()
            val hashedPin = BookingSecurityHelper.hashPin(rawPin)

            val scheduledTs = if (booking.scheduledAt > 0) booking.scheduledAt
            else BookingUtils.parseScheduledTimestamp(booking.date.ifBlank { booking.dateString }, booking.time.ifBlank { booking.timeString })

            val finalBooking = booking.copy(
                id = docId,
                bookingNumber = finalCode,
                bookingCode = finalCode,
                bookingPassword = rawPin, // Kept locally/securely
                pinCode = hashedPin,
                scheduledAt = scheduledTs,
                status = if (booking.status.isBlank()) "PENDING" else booking.status,
                createdAt = if (booking.createdAt > 0) booking.createdAt else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save immediately in local cache
            val current = _cachedBookings.value.toMutableList()
            current.removeAll { it.id == docId }
            current.add(0, finalBooking)
            saveToCache(current)

            // Sync to Firestore
            firestore.collection("bookings").document(docId)
                .set(finalBooking)
                .addOnSuccessListener {
                    // Write Notification payloads to "notifications" collection
                    val userNotifId = UUID.randomUUID().toString()
                    val userNotif = mapOf(
                        "id" to userNotifId,
                        "title" to "📅 تم إنشاء حجزك بنجاح",
                        "message" to "مرحباً! تم استلام طلب حجزك برقم #${finalBooking.bookingNumber} وهو قيد المراجعة.",
                        "targetType" to "USER",
                        "targetValue" to finalBooking.customerPhone,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("notifications").document(userNotifId).set(userNotif)

                    if (finalBooking.providerPhone.isNotBlank()) {
                        val providerNotifId = UUID.randomUUID().toString()
                        val providerNotif = mapOf(
                            "id" to providerNotifId,
                            "title" to "🔔 طلب حجز جديد",
                            "message" to "لديك طلب حجز جديد برقم #${finalBooking.bookingNumber} من العميل ${finalBooking.customerName}.",
                            "targetType" to "PROVIDER",
                            "targetValue" to finalBooking.providerPhone,
                            "timestamp" to System.currentTimeMillis()
                        )
                        firestore.collection("notifications").document(providerNotifId).set(providerNotif)
                    }

                    val adminNotifId = UUID.randomUUID().toString()
                    val adminNotif = mapOf(
                        "id" to adminNotifId,
                        "title" to "🚨 حجز جديد في الدليل",
                        "message" to "تم إنشاء حجز جديد #${finalBooking.bookingNumber} للخدمة ${finalBooking.serviceName}.",
                        "targetType" to "SUPERVISOR",
                        "targetValue" to "ALL",
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("notifications").document(adminNotifId).set(adminNotif)

                    onSuccess(finalBooking)
                }
                .addOnFailureListener { ex ->
                    // Queue for offline sync
                    cacheManager.queueOfflineAction(
                        LocalAppCacheManager.OfflineSyncAction(
                            type = "CREATE_BOOKING",
                            payloadJson = moshi.adapter(BookingEntity::class.java).toJson(finalBooking)
                        )
                    )
                    // Still treat as locally created
                    onSuccess(finalBooking)
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "فشل إنشاء الحجز")
        }
    }

    /**
     * Updates status of a booking (e.g. APPROVED, IN_PROGRESS, COMPLETED).
     */
    fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis(),
            if (newStatus == "COMPLETED") "completedAt" to System.currentTimeMillis() else "updatedAt" to System.currentTimeMillis()
        )

        // Optimistic local update
        val current = _cachedBookings.value.map {
            if (it.id == bookingId) it.copy(status = newStatus, updatedAt = System.currentTimeMillis()) else it
        }
        saveToCache(current)

        firestore.collection("bookings").document(bookingId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل تحديث حالة الحجز") }
    }

    /**
     * Cancels booking enforcing 8-hour rule and PIN verification.
     */
    fun cancelBookingWithSecurity(
        booking: BookingEntity,
        inputPinOrPassword: String,
        cancellationReason: String,
        cancelledBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // 1. Check if locked out (after 3 failed attempts)
        if (BookingSecurityHelper.isBookingLocked(context, booking.id)) {
            val remainingSec = BookingSecurityHelper.getRemainingLockoutSeconds(context, booking.id)
            onError("تم قفل هذا الحجز مؤقتاً بسبب 3 محاولات خاطئة. يرجى المحاولة بعد ${remainingSec / 60} دقيقة و ${remainingSec % 60} ثانية.")
            return
        }

        // 2. Check 8-hour rule
        val canCancel = BookingUtils.canModifyOrCancelBooking(
            scheduledAtTimestamp = booking.scheduledAt,
            dateString = booking.date.ifBlank { booking.dateString },
            timeString = booking.time.ifBlank { booking.timeString }
        )
        if (!canCancel) {
            onError("عذراً، تنص سياسة الخدمة على عدم إمكانية تعديل أو إلغاء الحجز إذا تبقى أقل من 8 ساعات على الموعد المحدد.")
            return
        }

        // 3. Verify Password / PIN
        val expectedTarget = if (booking.pinCode.isNotBlank()) booking.pinCode else booking.bookingPassword
        val isVerified = BookingSecurityHelper.verifyPassword(inputPinOrPassword, expectedTarget)

        if (!isVerified) {
            val attemptsLeft = BookingSecurityHelper.recordFailedAttempt(context, booking.id)
            if (attemptsLeft == 0) {
                onError("رمز التحقق غير صحيح. تم استنفاد 3 محاولات وقفل الحجز لمدة 5 دقائق لأسباب أمنية.")
            } else {
                onError("رمز التحقق غير صحيح. متبقي لديك $attemptsLeft محاولة فقط قبل القفل المؤقت.")
            }
            return
        }

        // Verification successful -> reset attempts and perform cancellation
        BookingSecurityHelper.resetAttempts(context, booking.id)

        val updates = mapOf(
            "status" to "CANCELLED",
            "cancellationReason" to cancellationReason,
            "cancelledAt" to System.currentTimeMillis(),
            "cancelledBy" to cancelledBy,
            "updatedAt" to System.currentTimeMillis()
        )

        // Optimistic local update
        val current = _cachedBookings.value.map {
            if (it.id == booking.id) it.copy(
                status = "CANCELLED",
                cancellationReason = cancellationReason,
                cancelledAt = System.currentTimeMillis(),
                cancelledBy = cancelledBy,
                updatedAt = System.currentTimeMillis()
            ) else it
        }
        saveToCache(current)

        firestore.collection("bookings").document(booking.id)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل إلغاء الحجز") }
    }

    /**
     * Direct cancellation of a booking by ID.
     */
    fun cancelBooking(
        bookingId: String,
        cancellationReason: String = "إلغاء من قبل المستخدم",
        cancelledBy: String = "USER",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val updates = mapOf(
            "status" to "CANCELLED",
            "cancellationReason" to cancellationReason,
            "cancelledAt" to System.currentTimeMillis(),
            "cancelledBy" to cancelledBy,
            "updatedAt" to System.currentTimeMillis()
        )
        val current = _cachedBookings.value.map {
            if (it.id == bookingId) it.copy(
                status = "CANCELLED",
                cancellationReason = cancellationReason,
                cancelledAt = System.currentTimeMillis(),
                cancelledBy = cancelledBy,
                updatedAt = System.currentTimeMillis()
            ) else it
        }
        saveToCache(current)

        firestore.collection("bookings").document(bookingId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل إلغاء الحجز") }
    }

    /**
     * Updates booking details (date, time, address, details) with 8-hour check.
     */
    fun updateBookingDetails(
        updatedBooking: BookingEntity,
        inputPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (BookingSecurityHelper.isBookingLocked(context, updatedBooking.id)) {
            val remainingSec = BookingSecurityHelper.getRemainingLockoutSeconds(context, updatedBooking.id)
            onError("الحجز مقفل مؤقتاً. انتظر ${remainingSec / 60} دقيقة.")
            return
        }

        val canModify = BookingUtils.canModifyOrCancelBooking(
            scheduledAtTimestamp = updatedBooking.scheduledAt,
            dateString = updatedBooking.date.ifBlank { updatedBooking.dateString },
            timeString = updatedBooking.time.ifBlank { updatedBooking.timeString }
        )
        if (!canModify) {
            onError("لا يمكن تعديل الحجز عند بقاء أقل من 8 ساعات على الموعد.")
            return
        }

        val expectedTarget = if (updatedBooking.pinCode.isNotBlank()) updatedBooking.pinCode else updatedBooking.bookingPassword
        val isVerified = BookingSecurityHelper.verifyPassword(inputPin, expectedTarget)

        if (!isVerified) {
            val left = BookingSecurityHelper.recordFailedAttempt(context, updatedBooking.id)
            onError("رمز التحقق غير صحيح. متبقي $left محاولات.")
            return
        }

        BookingSecurityHelper.resetAttempts(context, updatedBooking.id)

        val itemToSave = updatedBooking.copy(updatedAt = System.currentTimeMillis())
        val current = _cachedBookings.value.map { if (it.id == itemToSave.id) itemToSave else it }
        saveToCache(current)

        firestore.collection("bookings").document(itemToSave.id)
            .set(itemToSave)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل تحديث البيانات") }
    }

    /**
     * Deletes booking from database.
     */
    fun deleteBooking(bookingId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _cachedBookings.value.filter { it.id != bookingId }
        saveToCache(current)

        firestore.collection("bookings").document(bookingId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل حذف الحجز") }
    }
}
