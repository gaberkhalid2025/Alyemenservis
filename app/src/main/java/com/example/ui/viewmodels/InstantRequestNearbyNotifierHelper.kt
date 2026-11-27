package com.example.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.data.NotificationEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.models.InstantRequestEntity
import com.example.utils.AppConstants
import com.example.utils.HaversinePrecisionNavigator
import com.example.utils.LandmarkAddress
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal fun normalizeInstantSectionCode(raw: String): String {
    val v = raw.trim().uppercase()
    return when {
        v == "SERVICES" || v.contains("خدم") || v.contains("فني") -> "SERVICES"
        v == "STORES" || v.contains("متجر") || v.contains("متاجر") || v.contains("محل") || v.contains("تجار") || v.contains("سلع") -> "STORES"
        v == "RESTAURANTS" || v.contains("مطعم") || v.contains("مطاعم") || v.contains("كافيه") -> "RESTAURANTS"
        v == "MEDICAL" || v.contains("طب") || v.contains("صيدل") || v.contains("عياد") -> "MEDICAL"
        v == "PROPERTIES" || v.contains("عقار") || v.contains("شقق") -> "PROPERTIES"
        v == "GENERAL" || v.contains("عام") -> "GENERAL"
        else -> v.ifBlank { "GENERAL" }
    }
}

internal fun isWithinEffectiveRadiusHelper(
    requestLat: Double,
    requestLng: Double,
    targetLat: Double,
    targetLng: Double,
    radiusKm: Int,
    cityMatched: Boolean
): Boolean {
    if (!LandmarkAddress.isValid(requestLat, requestLng)) return true
    if (!LandmarkAddress.isValid(targetLat, targetLng)) return cityMatched
    val isDefaultSanaa = kotlin.math.abs(targetLat - 15.3694) < 0.0005 && kotlin.math.abs(targetLng - 44.1910) < 0.0005
    val isRequestNearSanaa = HaversinePrecisionNavigator.calculateDistance(requestLat, requestLng, 15.3694, 44.1910) <= radiusKm * 1000.0
    if (isDefaultSanaa && !isRequestNearSanaa && cityMatched) return true
    return HaversinePrecisionNavigator.calculateDistance(requestLat, requestLng, targetLat, targetLng) <= radiusKm * 1000.0
}

internal fun InstantRequestViewModel.sendUrgentRequestNotificationToNearbyProvidersHelper(
    request: InstantRequestEntity,
    radiusKm: Int
) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val section = normalizeInstantSectionCode(request.sectionType.ifBlank { request.categoryId })
            val cityQuery = request.userCity.trim()
            val matchedCity = appState._cities.value.find {
                it.nameAr.equals(cityQuery, ignoreCase = true) ||
                    it.nameEn.equals(cityQuery, ignoreCase = true) ||
                    it.id.equals(cityQuery, ignoreCase = true)
            }
            val cityKeys = listOfNotNull(
                cityQuery.takeIf { it.isNotBlank() },
                matchedCity?.id?.trim()?.takeIf { it.isNotBlank() },
                matchedCity?.nameAr?.trim()?.takeIf { it.isNotBlank() },
                matchedCity?.nameEn?.trim()?.takeIf { it.isNotBlank() }
            ).distinct().take(10)

            val targets = linkedMapOf<String, String>()

            if (section == "SERVICES" || section == "GENERAL") {
                val provQuery: Query = if (cityKeys.isNotEmpty()) {
                    firestore.collection(AppConstants.COL_PROVIDERS).whereIn("cityId", cityKeys).limit(120)
                } else {
                    firestore.collection(AppConstants.COL_PROVIDERS).limit(120)
                }
                val provSnap = runCatching { provQuery.get().await() }.getOrNull()
                provSnap?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(ProviderEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                }?.filter { provider ->
                    provider.isAvailable &&
                        !provider.isDeleted &&
                        !provider.isBlocked &&
                        !provider.isNotificationsDisabled &&
                        provider.subscriptionStatus.uppercase() in listOf("APPROVED", "ACCEPTED", "ACTIVE") &&
                        isWithinEffectiveRadiusHelper(
                            request.latitude, request.longitude,
                            provider.latitude, provider.longitude,
                            radiusKm,
                            cityKeys.isEmpty() || cityKeys.any { it.equals(provider.cityId, ignoreCase = true) }
                        )
                }?.forEach { provider ->
                    val key = provider.phone.ifBlank { provider.id }
                    if (key.isNotBlank()) targets.putIfAbsent(key, provider.phone)
                }
            }

            if (section in listOf("STORES", "RESTAURANTS", "MEDICAL", "GENERAL")) {
                val storeQuery: Query = if (cityKeys.isNotEmpty()) {
                    firestore.collection(AppConstants.COL_STORES).whereIn("cityId", cityKeys).limit(120)
                } else {
                    firestore.collection(AppConstants.COL_STORES).limit(120)
                }
                val storeSnap = runCatching { storeQuery.get().await() }.getOrNull()
                storeSnap?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(StoreEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                }?.filter { store ->
                    val storeSection = normalizeInstantSectionCode(store.sectionId.ifBlank { store.providerType.ifBlank { "STORES" } })
                    val sectionMatches = section == "GENERAL" ||
                        storeSection == section ||
                        (section == "STORES" && store.sectionId.isBlank())
                    store.isActive &&
                        store.isApproved &&
                        !store.isDeleted &&
                        !store.isBlocked &&
                        !store.isNotificationsDisabled &&
                        sectionMatches &&
                        isWithinEffectiveRadiusHelper(
                            request.latitude, request.longitude,
                            store.latitude, store.longitude,
                            radiusKm,
                            cityKeys.isEmpty() || cityKeys.any { it.equals(store.cityId, ignoreCase = true) }
                        )
                }?.forEach { store ->
                    val key = store.phone.ifBlank { store.ownerId.ifBlank { store.id } }
                    if (key.isNotBlank()) targets.putIfAbsent(key, store.phone)
                }
            }

            if (section == "PROPERTIES" || section == "GENERAL") {
                val propQuery: Query = if (cityKeys.isNotEmpty()) {
                    firestore.collection(AppConstants.COL_PROPERTIES).whereIn("cityId", cityKeys).limit(120)
                } else {
                    firestore.collection(AppConstants.COL_PROPERTIES).limit(120)
                }
                val propSnap = runCatching { propQuery.get().await() }.getOrNull()
                propSnap?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(PropertyEntity::class.java)?.copy(id = doc.id) }.getOrNull()
                }?.filter { prop ->
                    prop.isActive &&
                        prop.isApproved &&
                        !prop.isDeleted &&
                        !prop.isBlocked &&
                        !prop.isNotificationsDisabled &&
                        isWithinEffectiveRadiusHelper(
                            request.latitude, request.longitude,
                            prop.latitude, prop.longitude,
                            radiusKm,
                            cityKeys.isEmpty() || cityKeys.any { it.equals(prop.cityId, ignoreCase = true) }
                        )
                }?.forEach { prop ->
                    val key = prop.phone.ifBlank { prop.ownerId.ifBlank { prop.id } }
                    if (key.isNotBlank()) targets.putIfAbsent(key, prop.phone)
                }
            }

            val now = System.currentTimeMillis()
            val entries = targets.entries.toList()
            entries.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { (targetPhoneOrId, phone) ->
                    val safeKey = targetPhoneOrId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val notifId = "URG_PROV_${request.id}_$safeKey"
                    val notif = NotificationEntity(
                        id = notifId,
                        title = "🚨 طلب عاجل في ${request.userCity}",
                        message = "طلب (${request.requestCode}) - ${request.categoryName.ifBlank { section }}: ${request.serviceTitle} في ${request.userNeighborhood.ifBlank { request.userCity }}. قدّم عرضك الآن!",
                        customerPhone = phone,
                        targetType = "PROVIDER",
                        targetValue = targetPhoneOrId,
                        notificationType = "URGENT_REQUEST",
                        relatedRequestId = request.id,
                        fcmSent = false,
                        timestamp = now,
                        createdAt = now,
                        dedupKey = notifId
                    )
                    batch.set(firestore.collection("notifications").document(notifId), notif)
                }
                runCatching { batch.commit().await() }
            }

            addNotification?.invoke(
                "🚨 طلب عاجل جديد",
                "تم إرسال طلب عاجل في ${request.userCity} - القسم: ${request.categoryName.ifBlank { section }}",
                "PROVIDER",
                section
            )
        } catch (e: Exception) {
            android.util.Log.e("InstantRequestVM", "Failed to notify nearby providers", e)
        }
    }
}
