package com.example.ui

fun MainViewModel.createPayment(
    userId: String,
    providerId: String,
    amount: Double,
    method: String,
    bookingId: String = "",
    isLinkedToBooking: Boolean = false,
    bookingServiceType: String = ""
) = adminViewModel.createPayment(userId, providerId, amount, method, bookingId, isLinkedToBooking, bookingServiceType)

fun MainViewModel.addNewBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
    adminViewModel.addNewBanner(title, url, redirect, type, size, duration, displayTime)

fun MainViewModel.addBanner(title: String, url: String, redirect: String, type: String, size: String, duration: Int, displayTime: String = "طوال اليوم") =
    adminViewModel.addBanner(title, url, redirect, type, size, duration, displayTime)
