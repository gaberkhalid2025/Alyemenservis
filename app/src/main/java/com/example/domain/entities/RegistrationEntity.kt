package com.example.domain.entities

/**
 * 🏛️ Domain Entity: RegistrationEntity
 * Representing registration payload models across all 7 registration types.
 * Completely decoupled from Android dependencies.
 */
sealed class RegistrationEntity {

    data class Client(
        val fullName: String,
        val phone: String,
        val city: String,
        val passwordHash: String,
        val profileImageUrl: String = ""
    ) : RegistrationEntity()

    data class Provider(
        val fullName: String,
        val phone: String,
        val professionCategory: String,
        val city: String,
        val experienceYears: Int,
        val bio: String,
        val identityDocumentUrl: String = "",
        val licenseNumber: String = "",
        val workImages: List<String> = emptyList(),
        val passwordHash: String
    ) : RegistrationEntity()

    data class Store(
        val storeName: String,
        val ownerName: String,
        val phone: String,
        val storeCategory: String,
        val city: String,
        val addressDetails: String,
        val commercialRegisterNumber: String = "",
        val logoUrl: String = "",
        val storeImages: List<String> = emptyList(),
        val passwordHash: String
    ) : RegistrationEntity()

    data class Restaurant(
        val restaurantName: String,
        val ownerName: String,
        val phone: String,
        val cuisineType: String,
        val city: String,
        val addressDetails: String,
        val logoUrl: String = "",
        val menuImageUrls: List<String> = emptyList(),
        val passwordHash: String
    ) : RegistrationEntity()

    data class MedicalCenter(
        val centerName: String,
        val specialtyCategory: String,
        val doctorName: String,
        val phone: String,
        val city: String,
        val addressDetails: String,
        val licenseNumber: String = "",
        val logoUrl: String = "",
        val passwordHash: String
    ) : RegistrationEntity()

    data class Property(
        val title: String,
        val propertyType: String, // Sale, Rent
        val category: String, // Apartment, Land, Villa
        val ownerName: String,
        val phone: String,
        val city: String,
        val areaDetails: String,
        val priceYer: Double,
        val description: String,
        val imageUrls: List<String> = emptyList(),
        val passwordHash: String
    ) : RegistrationEntity()

    data class Job(
        val jobTitle: String,
        val companyName: String,
        val category: String,
        val contactPhone: String,
        val contactEmail: String,
        val city: String,
        val requirements: String,
        val salaryRange: String = "",
        val passwordHash: String
    ) : RegistrationEntity()
}

data class JoinStatusEntity(
    val requestId: String = "",
    val applicantName: String = "",
    val registrationType: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class AuthUserEntity(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "CLIENT",
    val token: String = "",
    val isVerified: Boolean = false
)
