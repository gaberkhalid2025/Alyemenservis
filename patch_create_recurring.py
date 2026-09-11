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

replacement = """    private fun createRecurringBookings(baseBooking: BookingEntity, onResult: (Boolean) -> Unit) {
        val parentId = java.util.UUID.randomUUID().toString()
        val dates = com.example.utils.ScheduleManager.calculateRecurringDates(baseBooking.dateString, baseBooking.recurrenceRule)
        
        val batch = db.batch()
        val newBookings = mutableListOf<BookingEntity>()
        
        dates.forEachIndexed { index, dateStr ->
            val bId = "${parentId}_${index}"
            val bNum = "YEM-${(10000..99999).random()}"
            val bPass = "${(1000..9999).random()}"
            val b = baseBooking.copy(
                id = bId,
                parentId = parentId,
                date = dateStr,
                dateString = dateStr,
                bookingNumber = bNum,
                bookingPassword = bPass,
                isRecurring = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newBookings.add(b)
            val docRef = db.collection("bookings").document(bId)
            batch.set(docRef, b)
        }
        
        safeFirestoreCallWithCallback(
            operation = { onSuccess, onFailure ->
                batch.commit().addOnSuccessListener {
                    _bookings.value = _bookings.value + newBookings
                    
                    val custPhone = baseBooking.customerPhone.ifEmpty { baseBooking.clientPhone }
                    val custName = baseBooking.customerName.ifEmpty { baseBooking.clientName.ifEmpty { "العميل" } }
                    val provPhone = baseBooking.providerPhone.ifEmpty {
                        getProviders?.invoke()?.find { it.id == baseBooking.providerId || it.name.trim() == baseBooking.providerName.trim() }?.phone?.trim() ?: baseBooking.providerId
                    }
                    
                    // Notify provider about the batch
                    val msg = "لديك سلسلة حجوزات جديدة متكررة (${newBookings.size} مواعيد) من $custName برقم $custPhone"
                    com.example.utils.PushNotificationHandler.sendPushNotification(context, provPhone, "سلسلة حجوزات جديدة", msg)
                    
                    // Log to admin
                    val auditLog = mapOf(
                        "id" to java.util.UUID.randomUUID().toString(),
                        "action" to "CREATE_RECURRING_BOOKING",
                        "details" to "تم إنشاء ${newBookings.size} حجوزات للمزود ${baseBooking.providerName}",
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("security_audit_logs").document(auditLog["id"].toString()).set(auditLog)
                    
                    onSuccess()
                    onResult(true)
                }.addOnFailureListener { e ->
                    onFailure(e)
                    onResult(false)
                }
            },
            onSuccess = { triggerToast("✅ تم إنشاء السلسلة المتكررة بنجاح") },
            onError = { triggerToast("⚠️ خطأ في المزامنة، يرجى التحقق من اتصالك") },
            errorMessage = "فشل إنشاء الحجوزات المتكررة"
        )
    }

    fun updateBookingFormFields"""

replace_between('app/src/main/java/com/example/ui/viewmodels/BookingViewModel.kt',
                '    fun updateBookingFormFields',
                '    fun updateBookingFormFields',
                replacement)
