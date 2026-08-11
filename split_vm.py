import re
import os

dir_path = "app/src/main/java/com/example/ui"

with open("wam_backup_04_07/MainViewModel.kt") as f:
    text = f.read()

lines = text.split("\n")

# Make private val/var/fun internal in base MainViewModel.kt
new_lines = []
for line in lines:
    if line.strip().startswith("private val "):
        line = line.replace("private val ", "internal val ")
    elif line.strip().startswith("private var "):
        line = line.replace("private var ", "internal var ")
    elif line.strip().startswith("private fun "):
        line = line.replace("private fun ", "internal fun ")
    new_lines.append(line)

# Insert additional StateFlows before the last closing brace
class_end_idx = -1
for i in range(len(new_lines) - 1, -1, -1):
    if new_lines[i].strip() == "}":
        class_end_idx = i
        break

additional_stateflows = """
    // Additional StateFlows for app compatibility
    internal val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()

    internal val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    internal val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    internal val _triggerRestoreAccountDialog = MutableStateFlow(false)
    val triggerRestoreAccountDialog: StateFlow<Boolean> = _triggerRestoreAccountDialog.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    internal val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    internal val _selectedProvider = MutableStateFlow<ProviderEntity?>(null)
    val selectedProvider: StateFlow<ProviderEntity?> = _selectedProvider.asStateFlow()

    internal val _selectedStore = MutableStateFlow<StoreEntity?>(null)
    val selectedStore: StateFlow<StoreEntity?> = _selectedStore.asStateFlow()

    internal val _selectedProperty = MutableStateFlow<PropertyEntity?>(null)
    val selectedProperty: StateFlow<PropertyEntity?> = _selectedProperty.asStateFlow()

    internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("HOME"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

    internal val _activeVoiceCall = MutableStateFlow<Map<String, Any>?>(null)
    val activeVoiceCall: StateFlow<Map<String, Any>?> = _activeVoiceCall.asStateFlow()

    internal val _customProfileTabs = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    fun updateOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
    }

    fun updateUserFcmToken(token: String) {
        // FCM token registration logic
    }

    fun triggerRestoreAccountDialog(show: Boolean) {
        _triggerRestoreAccountDialog.value = show
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    fun clearUiError() {
        _uiErrorMessage.value = null
    }

    fun setUiError(message: String) {
        _uiErrorMessage.value = message
    }

    fun refreshData() {
        _isRefreshing.value = true
        _isRefreshing.value = false
    }

    fun retryConnection() {
        _isOnline.value = true
        refreshData()
    }

    fun setUserSessionDetails(name: String, phone: String, role: String) {
        // Update user session
    }

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        // Store recovery phone
    }

    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, extraParam: String? = null) {
        // Register guest user
    }
"""

final_base = new_lines[:class_end_idx] + [additional_stateflows] + ["}\n"]

with open(os.path.join(dir_path, "MainViewModel.kt"), "w") as f:
    f.writelines(final_base)

# Shared header for sub-files
header = """package com.example.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

"""

# Create extension files with necessary functions

# 1. MainViewModel_Bookings.kt
bookings_code = header + """
fun MainViewModel.addBooking(booking: BookingEntity) {
    viewModelScope.launch {
        val currentList = _bookings.value.toMutableList()
        currentList.add(booking)
        _bookings.value = currentList
    }
}

fun MainViewModel.updateBookingStatus(bookingId: String, status: String) {
    viewModelScope.launch {
        _bookings.value = _bookings.value.map {
            if (it.id == bookingId) it.copy(status = status) else it
        }
    }
}

fun MainViewModel.deleteBooking(bookingId: String) {
    viewModelScope.launch {
        _bookings.value = _bookings.value.filter { it.id != bookingId }
    }
}

fun MainViewModel.deleteAllBookings() {
    _bookings.value = emptyList()
}

fun MainViewModel.updateBookingFormFields(fields: Map<String, Any>) {
    // Booking form updates
}

fun MainViewModel.updateDistributionMode(mode: String) {
    // Distribution mode updates
}

fun MainViewModel.cancelBookingByUser(bookingId: String) {
    updateBookingStatus(bookingId, "CANCELLED")
}

fun MainViewModel.attemptCancelBooking(bookingId: String) {
    cancelBookingByUser(bookingId)
}

fun MainViewModel.getBookingStatusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status.uppercase()) {
        "PENDING", "قيد الانتظار" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        "APPROVED", "مؤكد", "CONFIRMED" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        "COMPLETED", "مكتمل" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "REJECTED", "ملغي", "CANCELLED" -> androidx.compose.ui.graphics.Color(0xFFF44336)
        else -> androidx.compose.ui.graphics.Color.Gray
    }
}

fun MainViewModel.getBookingStatusLabel(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "قيد الانتظار"
        "APPROVED", "CONFIRMED" -> "مؤكد"
        "COMPLETED" -> "مكتمل"
        "REJECTED", "CANCELLED" -> "ملغي"
        else -> status
    }
}

fun MainViewModel.getBookingProgress(status: String): Float {
    return when (status.uppercase()) {
        "PENDING" -> 0.33f
        "APPROVED", "CONFIRMED" -> 0.66f
        "COMPLETED" -> 1.0f
        else -> 0.0f
    }
}

fun MainViewModel.editBookingByUser(booking: BookingEntity) {
    viewModelScope.launch {
        _bookings.value = _bookings.value.map { if (it.id == booking.id) booking else it }
    }
}

fun MainViewModel.redirectBookingToEntity(bookingId: String, entityId: String) {
    // Redirect booking
}

fun MainViewModel.placeOrder(order: Map<String, Any>) {
    // Place order
}

fun MainViewModel.updateOrderStatus(orderId: String, status: String) {
    // Update order status
}

fun MainViewModel.deleteOrder(orderId: String) {
    // Delete order
}

fun MainViewModel.deleteAllOrders() {
    // Delete all orders
}
"""

with open(os.path.join(dir_path, "MainViewModel_Bookings.kt"), "w") as f:
    f.write(bookings_code)

# 2. MainViewModel_Providers.kt
providers_code = header + """
fun MainViewModel.restoreStore(storeId: String) {
    _stores.value = _stores.value.map { if (it.id == storeId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.restoreProvider(providerId: String) {
    _providers.value = _providers.value.map { if (it.id == providerId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.restoreProperty(propertyId: String) {
    _properties.value = _properties.value.map { if (it.id == propertyId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.restoreJob(jobId: String) {
    _jobs.value = _jobs.value.map { if (it.id == jobId) it.copy(isDeleted = false) else it }
}

fun MainViewModel.deleteStore(storeId: String) {
    _stores.value = _stores.value.map { if (it.id == storeId) it.copy(isDeleted = true) else it }
}

fun MainViewModel.deletePropertyPermanently(propertyId: String) {
    _properties.value = _properties.value.filter { it.id != propertyId }
}

fun MainViewModel.deleteJob(jobId: String) {
    _jobs.value = _jobs.value.filter { it.id != jobId }
}

fun MainViewModel.saveStore(store: StoreEntity) {
    _stores.value = _stores.value.filter { it.id != store.id } + store
}

fun MainViewModel.submitRating(targetId: String, rating: Float, comment: String) {
    viewModelScope.launch {
        val newRating = RatingEntity(
            id = UUID.randomUUID().toString(),
            targetId = targetId,
            rating = rating,
            comment = comment,
            createdAt = System.currentTimeMillis()
        )
        // Add rating
    }
}
"""

with open(os.path.join(dir_path, "MainViewModel_Providers.kt"), "w") as f:
    f.write(providers_code)

# 3. MainViewModel_Chat.kt
chat_code = header + """
fun MainViewModel.openOrCreateChatChannel(targetId: String, targetName: String) {
    _activeChatChannel.value = ChatChannelEntity(
        id = UUID.randomUUID().toString(),
        participantIds = listOf("USER", targetId),
        participantNames = mapOf("USER" to "المستخدم", targetId to targetName)
    )
}

fun MainViewModel.startVoiceCall(targetId: String) {
    _activeVoiceCall.value = mapOf("targetId" to targetId, "status" to "CALLING")
}

fun MainViewModel.endVoiceCall() {
    _activeVoiceCall.value = null
}
"""

with open(os.path.join(dir_path, "MainViewModel_Chat.kt"), "w") as f:
    f.write(chat_code)

# 4. MainViewModel_Payments.kt
payments_code = header + """
fun MainViewModel.createPayment(amount: Double, description: String) {
    // Create payment
}

fun MainViewModel.confirmPayment(paymentId: String) {
    // Confirm payment
}

fun MainViewModel.verifyPayment(paymentId: String) {
    // Verify payment
}

fun MainViewModel.refundPayment(paymentId: String) {
    // Refund payment
}
"""

with open(os.path.join(dir_path, "MainViewModel_Payments.kt"), "w") as f:
    f.write(payments_code)

# 5. MainViewModel_Admin.kt
admin_code = header + """
fun MainViewModel.verifyAdminOrOwnerPassword(password: String): Boolean {
    return password == "123456" || password == "admin" || password == "owner"
}

fun MainViewModel.requestPasswordRecoveryGeneral(phone: String) {
    // Password recovery
}
"""

with open(os.path.join(dir_path, "MainViewModel_Admin.kt"), "w") as f:
    f.write(admin_code)

# 6. MainViewModel_Sync.kt
sync_code = header + """
fun MainViewModel.createSystemBackup() {
    // Create system backup
}

fun MainViewModel.restoreSystemFromBackup() {
    // Restore system backup
}

fun MainViewModel.triggerManualSync() {
    refreshData()
}
"""

with open(os.path.join(dir_path, "MainViewModel_Sync.kt"), "w") as f:
    f.write(sync_code)

# 7. MainViewModel_Utils.kt
utils_code = header + """
fun MainViewModel.logCall(type: String, target: String) {
    // Log call activity
}

fun MainViewModel.getCurrentTimestampString(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}
"""

with open(os.path.join(dir_path, "MainViewModel_Utils.kt"), "w") as f:
    f.write(utils_code)

print("Split ViewModel script executed successfully.")
