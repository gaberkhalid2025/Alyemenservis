package com.example.viewmodels

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingEntity
import com.example.data.ChatMessageEntity
import com.example.util.BookingNotificationManager
import com.example.util.BookingStateMachine
import com.example.util.OfflineQueueManager
import com.example.util.OfflineRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Keep
data class BookingTimeline(
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val changedBy: String = "SYSTEM"
)

@Keep
data class BookingNote(
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String = "",
    val note: String = "",
    val createdBy: String = "USER",
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class TimeSlot(
    val time: String,
    val isAvailable: Boolean = true
)

/**
 * 📅 BookingViewModel
 * إدارة كاملة لمنطق نظام الحجوزات الذكي، التقويم وحساب الفترات المتاحة والعطلات، الحماية والصلاحيات.
 */
class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val notificationManager: BookingNotificationManager by lazy { BookingNotificationManager(getApplication()) }
    private val offlineQueueManager: OfflineQueueManager by lazy { OfflineQueueManager(getApplication()) }

    private val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookings: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    private val _selectedBooking = MutableStateFlow<BookingEntity?>(null)
    val selectedBooking: StateFlow<BookingEntity?> = _selectedBooking.asStateFlow()

    private val _bookingNotes = MutableStateFlow<Map<String, List<BookingNote>>>(emptyMap())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    companion object {
        private const val TAG = "BookingViewModel"
    }

    init {
        loadAllBookings()
    }

    /**
     * تحميل جميع الحجوزات
     */
    fun loadAllBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("bookings")
                    .addSnapshotListener { snapshot, error ->
                        _isLoading.value = false
                        if (error != null) {
                            Log.w(TAG, "Bookings listen failed: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { it.toObject(BookingEntity::class.java) }
                            _bookings.value = list
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e(TAG, "Error loading bookings: ${e.message}")
            }
        }
    }

    /**
     * 1. إنشاء حجز جديد
     */
    fun createBooking(booking: BookingEntity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            val bookingId = if (booking.id.isNotBlank()) booking.id else UUID.randomUUID().toString()
            val bookingNumber = if (booking.bookingNumber.isNotBlank()) booking.bookingNumber else "BK-${System.currentTimeMillis().toString().takeLast(8)}"
            val now = System.currentTimeMillis()

            val finalBooking = booking.copy(
                id = bookingId,
                bookingNumber = bookingNumber,
                createdAt = if (booking.createdAt == 0L) now else booking.createdAt,
                updatedAt = now,
                status = if (booking.status.isBlank()) "PENDING" else booking.status
            )

            // إرسال للسحابة
            firestore.collection("bookings").document(bookingId).set(finalBooking, SetOptions.merge())
                .addOnSuccessListener {
                    _isLoading.value = false
                    _successMessage.value = "تم إنشاء الحجز بنجاح!"
                    notificationManager.notifyBookingCreated(finalBooking)
                    _bookings.value = listOf(finalBooking) + _bookings.value.filterNot { it.id == bookingId }
                    onResult(true, bookingId)
                }
                .addOnFailureListener { e ->
                    // حفظ في قائمة الانتظار أوفلاين
                    val mapData = mapOf(
                        "id" to finalBooking.id,
                        "bookingNumber" to finalBooking.bookingNumber,
                        "customerName" to finalBooking.customerName,
                        "customerPhone" to finalBooking.customerPhone,
                        "serviceType" to finalBooking.serviceType,
                        "providerId" to finalBooking.providerId,
                        "providerName" to finalBooking.providerName,
                        "dateString" to finalBooking.dateString,
                        "timeString" to finalBooking.timeString,
                        "status" to finalBooking.status,
                        "bookingPassword" to finalBooking.bookingPassword,
                        "createdAt" to finalBooking.createdAt
                    )
                    offlineQueueManager.addToQueue(
                        OfflineRequest(
                            id = finalBooking.id,
                            type = "BOOKING",
                            data = mapData,
                            priority = 1
                        )
                    )
                    _isLoading.value = false
                    _bookings.value = listOf(finalBooking) + _bookings.value.filterNot { it.id == bookingId }
                    _successMessage.value = "تم حفظ الحجز محلياً وسيتم إرساله عند الاتصال."
                    onResult(true, bookingId)
                }
        }
    }

    /**
     * 2. تحديث حالة الحجز
     */
    fun updateBookingStatus(bookingId: String, status: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val booking = _bookings.value.find { it.id == bookingId } ?: return@launch
            if (!BookingStateMachine.canTransition(booking.status, status)) {
                _errorMessage.value = "انتقال غير مسموح من ${booking.status} إلى $status"
                onResult?.invoke(false)
                return@launch
            }

            val updates = hashMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            if (status == "COMPLETED") {
                updates["completedAt"] = System.currentTimeMillis()
            }

            firestore.collection("bookings").document(bookingId).update(updates)
                .addOnSuccessListener {
                    val updatedBooking = booking.copy(status = status, updatedAt = System.currentTimeMillis())
                    _bookings.value = _bookings.value.map { if (it.id == bookingId) updatedBooking else it }
                    if (_selectedBooking.value?.id == bookingId) {
                        _selectedBooking.value = updatedBooking
                    }
                    notificationManager.sendBookingNotification(updatedBooking, status)
                    onResult?.invoke(true)
                }
                .addOnFailureListener {
                    onResult?.invoke(false)
                }
        }
    }

    /**
     * 3. إلغاء الحجز من قِبل المستخدم مع التحقق من كلمة المرور
     */
    fun cancelBooking(bookingId: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val booking = _bookings.value.find { it.id == bookingId }
            if (booking == null) {
                onResult(false, "الحجز غير موجود")
                return@launch
            }

            if (!canCancelBooking(booking)) {
                onResult(false, "لا يمكن إلغاء الحجز حالياً (قيد التنفيذ أو تجاوز مهلة الإلغاء)")
                return@launch
            }

            val expectedPass = booking.bookingPassword.ifEmpty { booking.pinCode }
            if (expectedPass.isNotBlank() && password.trim() != expectedPass.trim()) {
                val nextAttempts = booking.cancellationAttempts + 1
                if (nextAttempts >= 3) {
                    lockBookingAfterFailedAttempts(bookingId)
                    onResult(false, "تم قفل الحجز بعد 3 محاولات خاطئة")
                } else {
                    firestore.collection("bookings").document(bookingId).update("cancellationAttempts", nextAttempts)
                    onResult(false, "كلمة المرور غير صحيحة! متبقي ${3 - nextAttempts} محاولات")
                }
                return@launch
            }

            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledBy" to "CLIENT",
                "cancelledAt" to System.currentTimeMillis(),
                "cancellationReason" to "تم الإلغاء بواسطة العميل"
            )

            firestore.collection("bookings").document(bookingId).update(updates)
                .addOnSuccessListener {
                    notificationManager.notifyBookingCancelled(booking, "العميل", "طلب العميل")
                    onResult(true, "تم إلغاء الحجز بنجاح")
                }
                .addOnFailureListener { err ->
                    onResult(false, "فشل الإلغاء: ${err.message}")
                }
        }
    }

    /**
     * 4. إلغاء الحجز بواسطة الفني
     */
    fun cancelBookingByTechnician(bookingId: String, reason: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val booking = _bookings.value.find { it.id == bookingId }
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledBy" to "PROVIDER",
                "cancelledAt" to System.currentTimeMillis(),
                "cancellationReason" to reason
            )

            firestore.collection("bookings").document(bookingId).update(updates)
                .addOnSuccessListener {
                    if (booking != null) notificationManager.notifyBookingCancelled(booking, "الفني", reason)
                    onResult?.invoke(true)
                }
                .addOnFailureListener {
                    onResult?.invoke(false)
                }
        }
    }

    /**
     * 5. إلغاء الحجز بواسطة الأدمن
     */
    fun cancelBookingByAdmin(bookingId: String, reason: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val booking = _bookings.value.find { it.id == bookingId }
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelledBy" to "ADMIN",
                "cancelledAt" to System.currentTimeMillis(),
                "cancellationReason" to reason
            )

            firestore.collection("bookings").document(bookingId).update(updates)
                .addOnSuccessListener {
                    if (booking != null) notificationManager.notifyBookingCancelled(booking, "الإدارة", reason)
                    onResult?.invoke(true)
                }
                .addOnFailureListener {
                    onResult?.invoke(false)
                }
        }
    }

    /**
     * 6. الحصول على حجوزات مستخدم معين
     */
    fun getBookingsForUser(userId: String): List<BookingEntity> {
        val clean = userId.trim()
        return _bookings.value.filter {
            it.clientId == clean || it.customerPhone == clean || it.clientPhone == clean
        }
    }

    /**
     * 7. الحصول على حجوزات فني معين
     */
    fun getBookingsForProvider(providerId: String): List<BookingEntity> {
        val clean = providerId.trim()
        return _bookings.value.filter { it.providerId == clean }
    }

    /**
     * 8. تفاصيل حجز معين
     */
    fun getBookingDetails(bookingId: String): BookingEntity? {
        val b = _bookings.value.find { it.id == bookingId }
        if (b != null) _selectedBooking.value = b
        return b
    }

    /**
     * 9. التحقق من كلمة مرور الحجز
     */
    fun validateBookingPassword(bookingId: String, password: String): Boolean {
        val b = _bookings.value.find { it.id == bookingId } ?: return false
        val expected = b.bookingPassword.ifEmpty { b.pinCode }
        return expected.isNotBlank() && expected.trim() == password.trim()
    }

    /**
     * 10. مسمى الحالة
     */
    fun getBookingStatusLabel(status: String): String = BookingStateMachine.getStatusLabel(status)

    /**
     * 11. كود لون الحالة
     */
    fun getBookingStatusColor(status: String): String = BookingStateMachine.getStatusColor(status)

    /**
     * 12. نسبة التقدم للحالة
     */
    fun getBookingProgress(status: String): Float {
        return when (status.uppercase()) {
            "PENDING" -> 0.15f
            "UNDER_REVIEW" -> 0.30f
            "ACCEPTED", "APPROVED" -> 0.50f
            "IN_PROGRESS" -> 0.75f
            "COMPLETED", "PAID", "CLOSED" -> 1.0f
            else -> 0.0f
        }
    }

    /**
     * 13. هل يمكن إلغاء الحجز
     */
    fun canCancelBooking(booking: BookingEntity): Boolean = BookingStateMachine.canCancel(booking)

    /**
     * 14. عدد المحاولات المتبقية
     */
    fun getRemainingAttempts(booking: BookingEntity): Int {
        return (3 - booking.cancellationAttempts).coerceAtLeast(0)
    }

    /**
     * 15. قفل الحجز بعد 3 محاولات فاشلة
     */
    fun lockBookingAfterFailedAttempts(bookingId: String) {
        firestore.collection("bookings").document(bookingId).update(
            mapOf(
                "isLocked" to true,
                "cancellationAttempts" to 3,
                "lockedUntil" to System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            )
        )
    }

    /**
     * 16. حساب الفترات الزمنية المتاحة لليوم المحدد
     */
    fun getAvailableSlots(dateString: String, providerId: String): List<String> {
        val allSlots = listOf(
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM",
            "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM"
        )
        if (isHoliday(dateString)) return emptyList()

        val bookedSlots = _bookings.value
            .filter { it.providerId == providerId && it.dateString == dateString && it.status != "CANCELLED" && it.status != "REJECTED" }
            .map { it.timeString.ifEmpty { it.time } }

        return allSlots.filterNot { bookedSlots.contains(it) }
    }

    /**
     * 17. التحقق من العطل الرسمية
     */
    fun isHoliday(dateString: String): Boolean {
        // الجمعة عطلة أسبوعية
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val date = sdf.parse(dateString) ?: return false
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            if (dayOfWeek == java.util.Calendar.FRIDAY) return true
        } catch (e: Exception) {
            // fallback
        }
        return false
    }

    /**
     * 18. قائمة الفترات الزمنية مع حالة التوفر
     */
    fun getTimeSlotsWithAvailability(dateString: String, providerId: String): List<TimeSlot> {
        val allSlots = listOf(
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM",
            "05:00 PM", "06:00 PM", "07:00 PM", "08:00 PM"
        )
        val holiday = isHoliday(dateString)
        val bookedSlots = _bookings.value
            .filter { it.providerId == providerId && it.dateString == dateString && it.status != "CANCELLED" && it.status != "REJECTED" }
            .map { it.timeString.ifEmpty { it.time } }

        return allSlots.map { slot ->
            TimeSlot(
                time = slot,
                isAvailable = !holiday && !bookedSlots.contains(slot)
            )
        }
    }

    /**
     * 19. عدد الحجوزات حسب الحالة
     */
    fun getBookingCountByStatus(status: String): Int {
        return _bookings.value.count { it.status.equals(status, ignoreCase = true) }
    }

    /**
     * 20. عدد حجوزات اليوم
     */
    fun getBookingCountForToday(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return _bookings.value.count { it.dateString == today }
    }

    /**
     * 21. عدد حجوزات الأسبوع
     */
    fun getBookingCountForWeek(): Int {
        val oneWeekAgo = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7
        return _bookings.value.count { it.createdAt >= oneWeekAgo }
    }

    /**
     * 22. عدد حجوزات الشهر
     */
    fun getBookingCountForMonth(): Int {
        val oneMonthAgo = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30
        return _bookings.value.count { it.createdAt >= oneMonthAgo }
    }

    /**
     * 23. الحجوزات القادمة لمستخدم معين
     */
    fun getUpcomingBookings(userId: String): List<BookingEntity> {
        val clean = userId.trim()
        return _bookings.value.filter {
            (it.clientId == clean || it.customerPhone == clean) &&
                    (it.status == "PENDING" || it.status == "ACCEPTED" || it.status == "IN_PROGRESS" || it.status == "APPROVED")
        }
    }

    /**
     * 24. الحجوزات السابقة والمنتهية
     */
    fun getPastBookings(userId: String): List<BookingEntity> {
        val clean = userId.trim()
        return _bookings.value.filter {
            (it.clientId == clean || it.customerPhone == clean) &&
                    (it.status == "COMPLETED" || it.status == "CANCELLED" || it.status == "REJECTED" || it.status == "PAID")
        }
    }

    /**
     * 25. الحجوزات حسب التاريخ
     */
    fun getBookingsByDate(date: String): List<BookingEntity> {
        return _bookings.value.filter { it.dateString == date }
    }

    /**
     * 26. الحجوزات حسب مقدم الخدمة
     */
    fun getBookingsByProvider(providerId: String): List<BookingEntity> {
        return _bookings.value.filter { it.providerId == providerId }
    }

    /**
     * 27. الحجوزات حسب العميل
     */
    fun getBookingsByCustomer(customerId: String): List<BookingEntity> {
        return _bookings.value.filter { it.clientId == customerId || it.customerPhone == customerId }
    }

    /**
     * 28. البحث في الحجوزات
     */
    fun searchBookings(query: String): List<BookingEntity> {
        if (query.isBlank()) return _bookings.value
        val q = query.trim().lowercase()
        return _bookings.value.filter {
            it.bookingNumber.lowercase().contains(q) ||
                    it.customerName.lowercase().contains(q) ||
                    it.providerName.lowercase().contains(q) ||
                    it.serviceType.lowercase().contains(q) ||
                    it.customerPhone.contains(q)
        }
    }

    /**
     * 29. تصدير الحجوزات
     */
    fun exportBookings(format: String = "CSV"): String {
        return buildString {
            appendLine("BookingNumber,CustomerName,Phone,ProviderName,ServiceType,Date,Status,Price")
            _bookings.value.forEach { b ->
                appendLine("${b.bookingNumber},\"${b.customerName}\",${b.customerPhone},\"${b.providerName}\",\"${b.serviceType}\",${b.dateString},${b.status},${b.totalAmount}")
            }
        }
    }

    /**
     * 30. الخط الزمني للحجز
     */
    fun getBookingTimeline(bookingId: String): List<BookingTimeline> {
        val b = _bookings.value.find { it.id == bookingId }
        val list = mutableListOf<BookingTimeline>()
        if (b != null) {
            list.add(BookingTimeline(status = "PENDING", timestamp = b.createdAt, note = "تم إنشاء الحجز بنجاح", changedBy = "CLIENT"))
            if (b.status == "ACCEPTED" || b.status == "APPROVED" || b.status == "IN_PROGRESS" || b.status == "COMPLETED") {
                list.add(BookingTimeline(status = "ACCEPTED", timestamp = b.updatedAt, note = "تم قبول الحجز من الفني", changedBy = "PROVIDER"))
            }
            if (b.status == "IN_PROGRESS" || b.status == "COMPLETED") {
                list.add(BookingTimeline(status = "IN_PROGRESS", timestamp = b.updatedAt, note = "الخدمة قيد التنفيذ", changedBy = "PROVIDER"))
            }
            if (b.status == "COMPLETED") {
                list.add(BookingTimeline(status = "COMPLETED", timestamp = b.completedAt ?: b.updatedAt, note = "تم اكتمال تنفيذ الخدمة", changedBy = "PROVIDER"))
            }
            if (b.status == "CANCELLED") {
                list.add(BookingTimeline(status = "CANCELLED", timestamp = b.cancelledAt ?: b.updatedAt, note = (b.cancellationReason ?: "").ifEmpty { "تم إلغاء الحجز" }, changedBy = (b.cancelledBy ?: "").ifEmpty { "CLIENT" }))
            }
        }
        return list
    }

    /**
     * 31. إضافة ملاحظة للحجز
     */
    fun addBookingNote(bookingId: String, note: String) {
        if (note.isBlank()) return
        val newNote = BookingNote(
            id = UUID.randomUUID().toString(),
            bookingId = bookingId,
            note = note,
            createdBy = "ADMIN",
            createdAt = System.currentTimeMillis()
        )
        val currentNotes = _bookingNotes.value[bookingId] ?: emptyList()
        _bookingNotes.value = _bookingNotes.value + (bookingId to (currentNotes + newNote))
    }

    /**
     * 32. ملاحظات الحجز
     */
    fun getBookingNotes(bookingId: String): List<BookingNote> {
        return _bookingNotes.value[bookingId] ?: emptyList()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
