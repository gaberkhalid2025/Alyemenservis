package com.example.ui.screens.map

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.NavWaypoint

enum class MapMode { OSM, HOT, TOPO, SATELLITE, VELA_3D, AR_VPS, ALEPH_ASSISTANT }

@Stable
class MapScreenState(
    isRadarMode: Boolean = false,
    isHeatmapActive: Boolean = false,
    selectedCategory: String = "ALL",
    selectedCity: String = "الكل",
    searchQuery: String = "",
    maxRangeKm: Float = 25.0f,
    initialMapMode: MapMode = MapMode.SATELLITE
) {
    var isRadarMode by mutableStateOf(isRadarMode)
    var isHeatmapActive by mutableStateOf(isHeatmapActive)
    var isOfflineCacheActive by mutableStateOf(true)
    var showStreetAlertDialog by mutableStateOf(false)
    var currentMapMode by mutableStateOf(initialMapMode)
    var previousMapMode by mutableStateOf(initialMapMode)
    var nightMode by mutableStateOf(false)
    var gibsEnabled by mutableStateOf(false)
    var gibsProduct by mutableStateOf("truecolor")
    var eoxEnabled by mutableStateOf(false)
    var quakesEnabled by mutableStateOf(false)
    var rainEnabled by mutableStateOf(false)
    var reliabilityEnabled by mutableStateOf(false)
    var peakEnabled by mutableStateOf(false)
    var coverageEnabled by mutableStateOf(false)
    var hazardEnabled by mutableStateOf(false)
    var zoomInTick by mutableIntStateOf(0)
    var zoomOutTick by mutableIntStateOf(0)

    fun toggleMapMode(target: MapMode) {
        if (currentMapMode == target) {
            val back = if (previousMapMode != target) previousMapMode else MapMode.SATELLITE
            currentMapMode = back
        } else {
            previousMapMode = currentMapMode
            currentMapMode = target
        }
    }

    var isAlephAssistantOpen by mutableStateOf(false)
    var showLayerDialog by mutableStateOf(false)
    var selectedCategory by mutableStateOf(selectedCategory)
    var selectedCity by mutableStateOf(selectedCity)
    var searchQuery by mutableStateOf(searchQuery)
    var maxRangeKm by mutableFloatStateOf(maxRangeKm)
    var zoomScale by mutableFloatStateOf(1.0f)
    var panOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var selectedEntity by mutableStateOf<Any?>(null)
    var bookingProviderTarget by mutableStateOf<ProviderEntity?>(null)
    var isNavigating by mutableStateOf(false)
    var reloadTilesTrigger by mutableIntStateOf(0)
    val dynamicOffsets = mutableStateMapOf<String, Pair<Double, Double>>()
    val selectedWaypoints = mutableStateListOf<NavWaypoint>()

    /** القائمة النهائية التي تُمرَّر إلى NavigationScreen (تُثبَّت لحظة بدء الملاحة). */
    var navWaypoints by mutableStateOf<List<NavWaypoint>>(emptyList())

    /** يحوّل كياناً (مزود/متجر/عقار/نقطة) إلى محطة، أو null إن كانت الإحداثيات غير صالحة. */
    fun entityToWaypoint(entity: Any?): NavWaypoint? {
        val triple = when (entity) {
            is NavWaypoint -> return if (isValid(entity.latitude, entity.longitude)) entity else null
            is ProviderEntity -> Triple(entity.name, entity.latitude, entity.longitude)
            is StoreEntity -> Triple(entity.name, entity.latitude, entity.longitude)
            is PropertyEntity -> Triple(entity.title, entity.latitude, entity.longitude)
            else -> return null
        }
        val (name, lat, lng) = triple
        return if (isValid(lat, lng)) NavWaypoint(name = name.ifBlank { "وجهة" }, latitude = lat, longitude = lng) else null
    }

    private fun isValid(lat: Double, lng: Double) =
        !lat.isNaN() && !lng.isNaN() && lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)

    private fun same(a: NavWaypoint, b: NavWaypoint) =
        kotlin.math.abs(a.latitude - b.latitude) < 1e-5 && kotlin.math.abs(a.longitude - b.longitude) < 1e-5

    fun addWaypoint(entity: Any) {
        val wp = entityToWaypoint(entity) ?: return
        if (selectedWaypoints.none { same(it, wp) }) selectedWaypoints.add(wp)
    }

    fun removeWaypoint(waypoint: NavWaypoint) { selectedWaypoints.remove(waypoint) }
    fun clearWaypoints() { selectedWaypoints.clear() }

    /**
     * يبدأ الملاحة: المحطات المختارة + الوجهة (target) كمحطة أخيرة.
     * يعيد false إن لم توجد أي وجهة صالحة (فلا تبدأ ملاحة وهمية).
     */
    fun startNavigation(target: Any?): Boolean {
        val list = selectedWaypoints.toMutableList()
        entityToWaypoint(target)?.let { dest -> if (list.none { same(it, dest) }) list.add(dest) }
        if (list.isEmpty()) return false
        navWaypoints = list
        isNavigating = true
        return true
    }

    /** يبدأ الملاحة إلى نقطة العنوان مروراً بالمعلم أولاً (المعلم معروف ودقيق على الخريطة). */
    fun startNavigationViaLandmark(addr: com.example.utils.LandmarkAddress): Boolean {
        val home = NavWaypoint(name = addr.label, latitude = addr.pointLat, longitude = addr.pointLng)
        selectedWaypoints.clear()
        if (addr.distanceMeters > 60.0) {
            selectedWaypoints.add(NavWaypoint(name = addr.landmarkName, latitude = addr.landmarkLat, longitude = addr.landmarkLng))
        }
        return startNavigation(home)
    }

    fun stopNavigation() {
        isNavigating = false
        navWaypoints = emptyList()
        selectedWaypoints.clear()
    }

    /** لا تمسح المحطات المختارة (كان خطأ: إغلاق النافذة السفلية يمحو القائمة). */
    fun resetSelection() {
        selectedEntity = null
        bookingProviderTarget = null
    }

    fun clearOffsets() { dynamicOffsets.clear() }

    companion object {
        private fun encode(list: List<NavWaypoint>): String =
            list.joinToString(";;") { "${it.name.replace(";", " ").replace("|", " ")}|${it.latitude}|${it.longitude}" }

        private fun decode(s: String): List<NavWaypoint> =
            s.split(";;").filter { it.isNotBlank() }.mapNotNull { part ->
                val p = part.split("|")
                if (p.size != 3) return@mapNotNull null
                val lat = p[1].toDoubleOrNull() ?: return@mapNotNull null
                val lng = p[2].toDoubleOrNull() ?: return@mapNotNull null
                NavWaypoint(name = p[0], latitude = lat, longitude = lng)
            }

        val Saver: Saver<MapScreenState, *> = listSaver(
            save = {
                try {
                    listOf(
                        it.isRadarMode, it.isHeatmapActive, it.selectedCategory, it.selectedCity, it.searchQuery,
                        it.maxRangeKm, it.currentMapMode.name, it.isNavigating, encode(it.navWaypoints)
                    )
                } catch (_: Throwable) {
                    emptyList()
                }
            },
            restore = { list ->
                try {
                    val rawMode = try { MapMode.valueOf(list.getOrNull(6) as? String ?: "SATELLITE") } catch (_: Throwable) { MapMode.SATELLITE }
                    val mode = if (rawMode == MapMode.VELA_3D) MapMode.SATELLITE else rawMode
                    MapScreenState(
                        isRadarMode = (list.getOrNull(0) as? Boolean) ?: false,
                        isHeatmapActive = (list.getOrNull(1) as? Boolean) ?: false,
                        selectedCategory = (list.getOrNull(2) as? String) ?: "ALL",
                        selectedCity = (list.getOrNull(3) as? String) ?: "الكل",
                        searchQuery = (list.getOrNull(4) as? String) ?: "",
                        maxRangeKm = (list.getOrNull(5) as? Number)?.toFloat() ?: 25.0f,
                        initialMapMode = mode
                    ).also { st ->
                        val wps = decode(list.getOrNull(8) as? String ?: "")
                        if ((list.getOrNull(7) as? Boolean) == true && wps.isNotEmpty()) {
                            st.navWaypoints = wps
                            st.isNavigating = true
                        }
                    }
                } catch (_: Throwable) {
                    MapScreenState()
                }
            }
        )
    }
}

@Composable
fun rememberMapScreenState(): MapScreenState = rememberSaveable(saver = MapScreenState.Saver) { MapScreenState() }
