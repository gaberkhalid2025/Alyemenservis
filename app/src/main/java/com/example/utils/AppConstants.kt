package com.example.utils

/**
 * 📊 AppConstants
 * المرجع الموحد لجميع الثوابت والقيم الرقمية في التطبيق
 */
object AppConstants {
    // Pagination & Loading
    const val DEFAULT_PAGE_SIZE = 20
    const val CHAT_PAGE_SIZE = 50
    const val SEARCH_DELAY_MS = 300L
    const val ANIMATION_DURATION_MS = 300
    const val ANIMATION_DELAY_MS = 5000L
    
    // Security & Logic
    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_NAME_LENGTH = 3
    const val MAX_IMAGE_SIZE_BYTES = 500_000L
    
    // Map & Geolocation
    const val MAP_DEFAULT_ZOOM = 15f
    const val NEARBY_THRESHOLD_METERS = 5000.0
    
    // Firestore Collections
    const val COL_PROVIDERS = "providers"
    const val COL_STORES = "stores"
    const val COL_PROPERTIES = "properties"
    const val COL_JOBS = "jobs"
    const val COL_NOTIFICATIONS = "notifications"
    const val COL_USERS = "registered_users"
    const val COL_PENDING = "pending_approvals"
}
