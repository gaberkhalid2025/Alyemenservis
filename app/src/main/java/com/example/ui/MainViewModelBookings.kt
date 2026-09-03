package com.example.ui

import com.example.data.*
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

fun MainViewModel.cancelBookingByUserImpl(bookingId: String) {
    bookingViewModel.cancelBookingByUserImpl(bookingId)
}

fun MainViewModel.attemptCancelBookingImpl(
    bookingId: String, 
    input: String, 
    reason: String = "ملغي بطلب العميل", 
    onResult: (Boolean, String) -> Unit
) {
    bookingViewModel.attemptCancelBookingImpl(bookingId, input, reason, onResult)
}

fun MainViewModel.cancelBookingByTechnicianImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
    bookingViewModel.cancelBookingByTechnicianImpl(bookingId, reason, onComplete)
}

fun MainViewModel.cancelBookingByAdminImpl(bookingId: String, reason: String, onComplete: () -> Unit = {}) {
    bookingViewModel.cancelBookingByAdminImpl(bookingId, reason, onComplete)
}

fun MainViewModel.getBookingStatusColorImpl(status: String): String {
    return bookingViewModel.getBookingStatusColorImpl(status)
}

fun MainViewModel.getBookingStatusLabelImpl(status: String): String {
    return bookingViewModel.getBookingStatusLabelImpl(status)
}

fun MainViewModel.getBookingProgressImpl(status: String): Float {
    return bookingViewModel.getBookingProgressImpl(status)
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
