package com.example.utils

import com.example.data.models.*

/**
 * 📦 TestFixtures
 * Shared test objects and model fixtures used across unit and integration tests.
 */
object TestFixtures {

    val validBooking = BookingEntity(
        id = "bk_fixture_101",
        customerName = "علي محمد الطالب",
        customerPhone = "771234567",
        customerArea = "صنعاء - السبعين",
        date = "2026-05-15",
        time = "10:00 AM",
        serviceName = "صيانة كهرباء منزلية",
        status = "PENDING",
        totalAmount = 5000.0,
        currency = "YER"
    )

    val validProvider = ServiceProvider(
        id = "prov_fixture_202",
        name = "المهندس أحمد علي",
        phone = "770000111",
        category = "كهرباء",
        city = "صنعاء",
        rating = 4.9,
        isVerified = true
    )

    val validStore = StoreEntity(
        id = "store_fixture_303",
        name = "متجر الأمل للالكترونيات",
        ownerPhone = "773333444",
        city = "عدن",
        category = "إلكترونيات",
        isApproved = true
    )

    val validUser = User(
        uid = "usr_fixture_404",
        name = "محمد سعيد",
        email = "mohammed@yemen.services.com",
        phone = "775555666",
        role = "CLIENT"
    )
}
