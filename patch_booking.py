import sys

def replace_between(filename, start_marker, end_marker, replacement):
    with open(filename, 'r') as f:
        content = f.read()
    
    start_idx = content.find(start_marker)
    if start_idx == -1:
        print("Start marker not found")
        return
        
    end_idx = content.find(end_marker, start_idx)
    if end_idx == -1:
        print("End marker not found")
        return
        
    end_idx += len(end_marker)
    
    new_content = content[:start_idx] + replacement + content[end_idx:]
    
    with open(filename, 'w') as f:
        f.write(new_content)
    print("Success")

replacement = """    fun createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) {
        if (booking.isRecurring && booking.recurrenceRule != "NONE") {
            createRecurringBookings(booking, onResult)
            return
        }
        val bId = booking.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        val bNum = booking.bookingNumber.ifEmpty { "YEM-${(10000..99999).random()}" }
        val bPass = booking.bookingPassword.ifEmpty { "${(1000..9999).random()}" }
        val finalized = booking.copy(
            id = bId,
            bookingNumber = bNum,
            bookingPassword = bPass,
            createdAt = if (booking.createdAt == 0L) System.currentTimeMillis() else booking.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        safeFirestoreCallWithCallback(
            operation = { onSuccess, onFailure ->
                db.collection("bookings").document(bId).set(finalized)
                    .addOnSuccessListener {
                        _bookings.value = _bookings.value + finalized
                        val custPhone = finalized.customerPhone.ifEmpty { finalized.clientPhone }
                        val custName = finalized.customerName.ifEmpty { finalized.clientName.ifEmpty { "العميل" } }
                        val provPhone = finalized.providerPhone.ifEmpty {
                            getProviders?.invoke()?.find { it.id == finalized.providerId || it.name.trim() == finalized.providerName.trim() }?.phone?.trim() ?: finalized.providerId
                        }
                        
                        // 1. User notification"""

replace_between('app/src/main/java/com/example/ui/viewmodels/BookingViewModel.kt',
                '    fun createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) {',
                '                        // 1. User notification',
                replacement)
