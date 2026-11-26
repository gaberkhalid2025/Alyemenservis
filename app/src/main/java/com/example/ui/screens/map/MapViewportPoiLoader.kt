package com.example.ui.screens.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import com.example.ui.screens.map.components.MapLandmarkFilter
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
internal fun rememberViewportOsmPoisJson(
    context: Context,
    poiLat: Double,
    poiLng: Double,
    hybridLabels: Boolean,
    state: MapScreenState
): String? {
    var osmPoisJson by remember { mutableStateOf<String?>(null) }
    val poiGridKey = remember(poiLat, poiLng) {
        if (LandmarkAddress.isValid(poiLat, poiLng)) {
            String.format(Locale.US, "%.3f_%.3f", poiLat, poiLng)
        } else ""
    }

    LaunchedEffect(
        poiGridKey,
        hybridLabels,
        state.mapLayer,
        state.isRadarMode,
        state.currentMapMode,
        state.visibleLandmarkFilters
    ) {
        val showPois = !state.isRadarMode &&
            state.currentMapMode != MapMode.AR_VPS &&
            state.mapLayer != MapLayer.VALHALLA_OFFLINE
        if (!showPois || poiGridKey.isBlank()) {
            osmPoisJson = null
            return@LaunchedEffect
        }

        fun isCategoryAllowed(kind: String): Boolean {
            val filters = state.visibleLandmarkFilters
            return when {
                kind == "شارع" -> filters.contains(MapLandmarkFilter.STREETS)
                kind in listOf("جامع", "مسجد", "دار عبادة") -> filters.contains(MapLandmarkFilter.MOSQUES)
                kind in listOf("مدرسة", "جامعة", "معهد", "كلية") -> filters.contains(MapLandmarkFilter.SCHOOLS)
                kind in listOf("صالة أفراح", "قاعة") -> filters.contains(MapLandmarkFilter.HALLS)
                kind in listOf("مطعم", "كافيه", "بوفيه") -> filters.contains(MapLandmarkFilter.RESTAURANTS)
                kind in listOf("سوق", "مول") -> filters.contains(MapLandmarkFilter.STORES)
                kind in listOf("مستشفى", "عيادة", "صيدلية") -> filters.contains(MapLandmarkFilter.MEDICAL)
                else -> filters.contains(MapLandmarkFilter.PROVIDERS)
            }
        }

        // 1) تحميل المعالم المحلية فوراً (مساجد، صالات، مدارس، مستشفيات، شوارع) لتظهر فوراً حتى بدون إنترنت
        withContext(Dispatchers.IO) {
            val localArr = JSONArray()
            val seenKeys = mutableSetOf<String>()

            val nearbyPlaces = OfflineYemenGazetteer.PLACES.filter { p ->
                val dist = HaversinePrecisionNavigator.calculateDistance(poiLat, poiLng, p.latitude, p.longitude)
                dist <= 20_000.0 && isCategoryAllowed(p.category)
            }
            nearbyPlaces.forEach { p ->
                val dedupeKey = "${p.name.trim()}_${String.format(Locale.US, "%.3f_%.3f", p.latitude, p.longitude)}"
                if (seenKeys.add(dedupeKey)) {
                    val (ic, col) = when (p.category) {
                        "جامع", "مسجد" -> "🕌" to "#10B981"
                        "صالة أفراح" -> "💒" to "#EC4899"
                        "مدرسة" -> "🏫" to "#3B82F6"
                        "جامعة", "معهد" -> "🎓" to "#2563EB"
                        "مستشفى", "عيادة" -> "🏥" to "#EF4444"
                        "صيدلية" -> "💊" to "#DC2626"
                        "سوق" -> "🛒" to "#0EA5E9"
                        "صرافة", "بنك" -> "🏦" to "#6366F1"
                        "حي" -> "🏘️" to "#14B8A6"
                        "شارع" -> "🛣️" to "#38BDF8"
                        "جولة" -> "🔄" to "#F59E0B"
                        else -> p.icon to "#64748B"
                    }
                    localArr.put(JSONObject().apply {
                        put("name", p.name)
                        put("kind", p.category)
                        put("icon", ic)
                        put("color", col)
                        put("lat", p.latitude)
                        put("lng", p.longitude)
                    })
                }
            }

            val custom = CustomLandmarkStore.nearby(context, poiLat, poiLng, maxDistanceM = 15_000.0)
                .filter { isCategoryAllowed(it.kind) }
            custom.forEach { c ->
                val dedupeKey = "${c.name.trim()}_${String.format(Locale.US, "%.3f_%.3f", c.lat, c.lng)}"
                if (seenKeys.add(dedupeKey)) {
                    localArr.put(JSONObject().apply {
                        put("name", c.name)
                        put("kind", c.kind)
                        put("icon", "📍")
                        put("color", "#06B6D4")
                        put("lat", c.lat)
                        put("lng", c.lng)
                    })
                }
            }

            if (localArr.length() > 0) {
                withContext(Dispatchers.Main) {
                    osmPoisJson = localArr.toString()
                }
            }

            // 2) إذا توفر الإنترنت، جلب المعالم الإضافية من Overpass في الخلفية ودمجها مع المعالم الحالية
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val hasNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (hasNet) {
                try {
                    val remotePois = LandmarkFinder.fetchViewportPois(poiLat, poiLng, radiusM = 2500)
                        .filter { isCategoryAllowed(it.kind) }
                    if (remotePois.isNotEmpty()) {
                        val mergedArr = JSONArray(localArr.toString())
                        val toCache = mutableListOf<RealValhallaNavigator.SearchAddressResult>()
                        remotePois.forEach { p ->
                            val dedupeKey = "${p.name.trim()}_${String.format(Locale.US, "%.3f_%.3f", p.lat, p.lng)}"
                            if (seenKeys.add(dedupeKey)) {
                                val (ic, col) = when (p.kind) {
                                    "مسجد" -> "🕌" to "#10B981"
                                    "مدرسة" -> "🏫" to "#3B82F6"
                                    "جامعة" -> "🎓" to "#2563EB"
                                    "صالة أفراح" -> "💒" to "#EC4899"
                                    "مستشفى", "عيادة" -> "🏥" to "#EF4444"
                                    "صيدلية" -> "💊" to "#DC2626"
                                    "مطعم" -> "🍴" to "#F97316"
                                    "كافيه" -> "☕" to "#D97706"
                                    "سوق" -> "🛒" to "#0EA5E9"
                                    "بنك" -> "🏦" to "#6366F1"
                                    "محطة وقود" -> "⛽" to "#EAB308"
                                    "فندق" -> "🏨" to "#8B5CF6"
                                    "حي" -> "🏘️" to "#14B8A6"
                                    else -> "📍" to "#64748B"
                                }
                                mergedArr.put(JSONObject().apply {
                                    put("name", p.name)
                                    put("kind", p.kind)
                                    put("icon", ic)
                                    put("color", col)
                                    put("lat", p.lat)
                                    put("lng", p.lng)
                                })
                                toCache.add(
                                    RealValhallaNavigator.SearchAddressResult(
                                        displayName = "${p.name} (${p.kind})",
                                        shortName = p.name,
                                        latitude = p.lat,
                                        longitude = p.lng,
                                        type = "osm_poi",
                                        icon = ic
                                    )
                                )
                            }
                        }
                        if (toCache.isNotEmpty()) {
                            OfflineYemenGazetteer.cacheOnlineResults(context.applicationContext, toCache)
                        }
                        withContext(Dispatchers.Main) {
                            osmPoisJson = mergedArr.toString()
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    return osmPoisJson
}
