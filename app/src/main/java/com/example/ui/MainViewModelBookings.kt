package com.example.ui

import com.example.data.*
import com.example.ui.*
import com.example.data.models.*

fun MainViewModel.addBookingImpl(
    name: String, 
    phone: String, 
    area: String, 
    serviceType: String, 
    providerId: String, 
    providerName: String, 
    dateString: String = "2026-06-20", 
    timeString: String = "12:00 م",
    couponCode: String = "",
    pinCode: String = "",
    customBookingId: String = "",
    customPassword: String = ""
) {
    bookingViewModel.addBookingImpl(
        name = name,
        phone = phone,
        area = area,
        serviceType = serviceType,
        providerId = providerId,
        providerName = providerName,
        dateString = dateString,
        timeString = timeString,
        couponCode = couponCode,
        pinCode = pinCode,
        customBookingId = customBookingId,
        customPassword = customPassword
    )
}

fun MainViewModel.updateBookingStatusImpl(bookingId: String, newStatus: String, rejectionReason: String = "") {
    bookingViewModel.updateBookingStatusImpl(bookingId, newStatus, rejectionReason)
}

fun MainViewModel.deleteBookingImpl(bookingId: String) {
    bookingViewModel.deleteBookingImpl(bookingId)
}

fun MainViewModel.deleteAllBookingsImpl(customerPhone: String) {
    bookingViewModel.deleteAllBookingsImpl(customerPhone)
}

fun MainViewModel.updateBookingImpl(booking: BookingEntity) {
    bookingViewModel.updateBookingImpl(booking)
}









fun MainViewModel.getBookingStatusColorImpl(status: String): String {
    return bookingViewModel.getBookingStatusColor(status)
}

fun MainViewModel.getBookingStatusLabelImpl(status: String): String {
    return bookingViewModel.getBookingStatusLabel(status)
}

fun MainViewModel.getBookingProgressImpl(status: String): Float {
    return bookingViewModel.getBookingProgress(status)
}

fun MainViewModel.createBooking(booking: BookingEntity, onResult: (Boolean) -> Unit = {}) {
    bookingViewModel.createBooking(booking, onResult)
}

fun MainViewModel.createBookingDirectly(
    provider: ProviderEntity,
    notes: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    bookingViewModel.createBookingDirectly(provider, notes, onSuccess, onError)
}

fun MainViewModel.attemptCancelBookingImpl(
    bookingId: String, 
    input: String, 
    reason: String = "ملغي بطلب العميل", 
    cancelledByParam: String = "USER",
    onResult: (Boolean, String) -> Unit
) {
    bookingViewModel.attemptCancelBookingImpl(bookingId, input, reason, cancelledByParam, onResult)
}
