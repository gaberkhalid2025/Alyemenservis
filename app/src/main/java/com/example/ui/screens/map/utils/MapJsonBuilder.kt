package com.example.ui.screens.map.utils

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.getPropertyCoords
import com.example.utils.getProviderCoords
import com.example.utils.getStoreCoords
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🗺️ MapJsonBuilder
 * Builds structured JSON strings for Leaflet WebView markers.
 */
object MapJsonBuilder {

    fun buildMarkersJson(
        nearbyProviders: List<ProviderEntity>,
        nearbyStores: List<StoreEntity>,
        nearbyProperties: List<PropertyEntity>,
        dynamicOffsets: Map<String, Pair<Double, Double>>,
        safeUserLat: Double,
        safeUserLng: Double
    ): String {
        val jsonArray = JSONArray()

        // 1. Providers
        nearbyProviders.forEachIndexed { idx, p ->
            val baseCoords = getProviderCoords(p)
            val walkOffset = dynamicOffsets[p.id] ?: Pair(0.0, 0.0)
            var rawLat = baseCoords.first + walkOffset.first
            var rawLng = baseCoords.second + walkOffset.second

            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + ((idx % 5) * 0.003 - 0.006)
                rawLng = safeUserLng + (((idx / 5) % 5) * 0.003 - 0.006)
            }

            val categoryEmoji = when {
                p.categoryId.contains("spaka") || p.profession.contains("سباك") -> "🔧"
                p.categoryId.contains("kahraba") || p.profession.contains("كهربا") -> "⚡"
                p.categoryId.contains("solar") || p.profession.contains("طاقة") -> "☀️"
                p.categoryId.contains("dehan") || p.profession.contains("دهان") -> "🎨"
                p.categoryId.contains("hadada") || p.profession.contains("حداد") -> "🔨"
                p.categoryId.contains("ac") || p.categoryId.contains("tabreed") || p.profession.contains("تكييف") -> "❄️"
                p.categoryId.contains("car") || p.categoryId.contains("mechanic") || p.profession.contains("ميكانيك") -> "🚗"
                p.categoryId.contains("carpentry") || p.categoryId.contains("najjara") || p.profession.contains("نجار") -> "🪚"
                else -> "👷"
            }

            val pSpec = p.customCategoryName.ifEmpty { p.specialization.ifEmpty { p.profession } }
            val cleanSpec = if (pSpec.isBlank()) "فني صيانة معتمد" else pSpec

            val obj = JSONObject().apply {
                put("id", p.id)
                put("type", "PROVIDER")
                put("name", p.name.ifBlank { "فني دليل اليمن" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", categoryEmoji)
                put("spec", cleanSpec)
                put("phone", p.phone)
                put("rating", if (p.rating > 0) String.format(java.util.Locale.US, "%.1f", p.rating) else "5.0")
                put("status", if (p.isAvailable) "متاح لاستقبال الطلبات 🟢" else "مشغول حالياً 🟡")
                put("badgeColor", "#00E5FF")
                put("serviceCategory", "فنيون صيانة")
            }
            jsonArray.put(obj)
        }

        // 2. Stores & Medical Centers & Restaurants
        nearbyStores.forEachIndexed { idx, s ->
            val coords = getStoreCoords(s)
            val isMedical = s.sectionId.contains("medical") || s.categoryId.contains("medical") || s.categoryId.contains("pharmacy") || s.medicalLicenseNo.isNotBlank() || s.name.contains("طبي") || s.name.contains("مستشفى") || s.name.contains("عيادة") || s.name.contains("صيدلية")
            val isRestaurant = !isMedical && (s.sectionId.contains("restaurant") || s.categoryId.contains("restaurant") || s.name.contains("مطعم") || s.name.contains("مأكولات") || s.name.contains("كافيه") || s.name.contains("شاورما"))

            val emoji = when {
                isMedical -> "🏥"
                isRestaurant -> "🍔"
                s.sectionId.contains("supermarket") || s.name.contains("بقالة") || s.name.contains("هايبر") -> "🛒"
                else -> "🏪"
            }

            var rawLat = coords.first
            var rawLng = coords.second
            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + (((idx + 2) % 6) * 0.0035 - 0.007)
                rawLng = safeUserLng + ((((idx + 2) / 6) % 6) * 0.0035 - 0.007)
            }

            val (badgeColor, categoryLabel) = when {
                isMedical -> Pair("#EC4899", "مراكز طبية وصيدليات")
                isRestaurant -> Pair("#F59E0B", "مطاعم وكافيهات")
                else -> Pair("#10B981", "متاجر وأسواق")
            }

            val cleanDesc = s.description.ifBlank { if (isMedical) "مركز طبي / صيدلية معتمدة" else "محل / متجر معتمد" }.take(60)

            val obj = JSONObject().apply {
                put("id", s.id)
                put("type", "STORE")
                put("name", s.name.ifBlank { "متجر معتمد" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", emoji)
                put("spec", cleanDesc)
                put("phone", s.phone)
                put("rating", if (s.rating > 0) String.format(java.util.Locale.US, "%.1f", s.rating) else "5.0")
                put("status", if (s.isActive) "مفتوح ويستقبل الطلبات 🟢" else "مغلق حالياً 🔴")
                put("badgeColor", badgeColor)
                put("serviceCategory", categoryLabel)
            }
            jsonArray.put(obj)
        }

        // 3. Properties
        nearbyProperties.forEachIndexed { idx, pr ->
            val coords = getPropertyCoords(pr)
            var rawLat = coords.first
            var rawLng = coords.second
            val isApproximate = (rawLat == 0.0 && rawLng == 0.0)
            if (isApproximate) {
                rawLat = safeUserLat + (((idx + 4) % 5) * 0.004 - 0.008)
                rawLng = safeUserLng + ((((idx + 4) / 5) % 5) * 0.004 - 0.008)
            }

            val priceFormatted = if (pr.price > 0) "${pr.price} ${pr.currency.ifEmpty { "ريال" }}" else "حسب الاتفاق"
            val cleanDesc = pr.description.ifBlank { "عقار معروض" }.take(60)

            val obj = JSONObject().apply {
                put("id", pr.id)
                put("type", "PROPERTY")
                put("name", pr.title.ifBlank { "عقار معروض" })
                put("lat", rawLat)
                put("lng", rawLng)
                put("isApprox", isApproximate)
                put("emoji", "🏠")
                put("spec", "$cleanDesc ($priceFormatted)")
                put("phone", pr.phone)
                put("rating", "5.0")
                put("status", "معروض للإيجار / البيع 🟢")
                put("badgeColor", "#8B5CF6")
                put("serviceCategory", "عقارات وشقق")
            }
            jsonArray.put(obj)
        }

        return jsonArray.toString()
    }
}
