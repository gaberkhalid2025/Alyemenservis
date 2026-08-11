package com.example.data

enum class BookingDistributionMode {
    ALL_PROVIDERS,
    NEAREST_PROVIDER,
    SPECIFIC_PROVIDER
}

enum class BookingStatus {
    PENDING,
    APPROVED,
    REJECTED,
    STARTED,
    COMPLETED,
    CANCELLED
}

data class BookingFormFields(
    val enableClientAddress: Boolean = true,
    val enableClientPhone: Boolean = true,
    val enableServiceDetails: Boolean = true,
    val enableAppointmentTime: Boolean = true,
    val enableNotes: Boolean = true
)

data class CardSettings(
    val showPhone: Boolean = true,
    val showAddress: Boolean = true,
    val showRating: Boolean = true,
    val showStatus: Boolean = true,
    val primaryColor: String = "#0088FF"
)
