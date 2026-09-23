package com.example.utils

import com.example.data.BookingEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.UserEntity

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

    val validProvider = ProviderEntity(
        id = "prov_fixture_202",
        name = "المهندس أحمد علي",
        phone = "770000111",
        categoryId = "كهرباء",
        cityId = "صنعاء",
        rating = 4.9f,
        isVerified = true
    )

    val validStore = StoreEntity(
        id = "store_fixture_303",
        name = "متجر الأمل للالكترونيات",
        phone = "773333444",
        cityId = "عدن",
        categoryId = "إلكترونيات",
        isApproved = true
    )

    val validUser = UserEntity(
        id = "usr_fixture_404",
        name = "محمد سعيد",
        email = "mohammed@yemen.services.com",
        phone = "775555666",
        role = "CLIENT"
    )
}
