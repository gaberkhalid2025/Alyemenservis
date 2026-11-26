@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens.map

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.map.components.MapBottomSheet
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.theme.MapUi
import com.example.utils.NavWaypoint
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.RealValhallaNavigator
import com.example.utils.SosDispatcher
import com.example.utils.VisualThemePalette
import com.example.utils.WaypointOptimizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BoxScope.MapWaypointsAndBottomCards(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    hasRealLocation: Boolean,
    safeUserLat: Double,
    safeUserLng: Double,
    isTilesDownloaded: Boolean,
    searchedPlace: RealValhallaNavigator.SearchAddressResult?,
    onDismissSearchedPlace: () -> Unit,
    pinnedPoint: Pair<Double, Double>?,
    onDismissPinnedPoint: () -> Unit,
    onAddLandmarkForPin: (Pair<Double, Double>) -> Unit,
    onOpenLandmarkForPin: (Pair<Double, Double>) -> Unit,
    onOpenProviderDetails: (ProviderEntity) -> Unit,
    onOpenStoreDetails: (StoreEntity) -> Unit,
    onOpenPropertyDetails: (PropertyEntity) -> Unit,
    onRequestBooking: (Any) -> Unit
) {
    // 📍 8.5. Multi-Stop Waypoints Management Bar
    if (state.selectedWaypoints.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B0F19).copy(alpha = 0.90f),
            border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.selectedEntity != null) 250.dp else 90.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "محطات التوقف المحددة (${state.selectedWaypoints.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.selectedWaypoints.size >= 3 && hasRealLocation) {
                            TextButton(onClick = {
                                val origin = safeUserLat to safeUserLng
                                val before = WaypointOptimizer.pathLength(origin, state.selectedWaypoints.toList())
                                val opt = WaypointOptimizer.optimize(origin, state.selectedWaypoints.toList())
                                val after = WaypointOptimizer.pathLength(origin, opt)
                                if (after + 1.0 < before) {
                                    state.selectedWaypoints.clear()
                                    state.selectedWaypoints.addAll(opt)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم الترتيب: توفير ≈ ${MapDistanceCalculator.formatDistance(before - after)} (بالخط المستقيم)")
                                    }
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("ترتيبك الحالي هو الأقرب") }
                                }
                            }) {
                                Text("ترتيب أذكى", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { state.clearWaypoints() }) {
                            Text("مسح الكل", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(state.selectedWaypoints) { wp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(wp.name, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { state.removeWaypoint(wp) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 📋 9. Bottom Sheet for clicked place
    state.selectedEntity?.let { entity ->
        MapBottomSheet(
            entity = entity,
            userLat = if (hasRealLocation) safeUserLat else 0.0,
            userLng = if (hasRealLocation) safeUserLng else 0.0,
            onDismiss = { state.selectedEntity = null },
            onAddStop = { target ->
                state.addWaypoint(target)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("📍 تمت إضافة المحطة للمسار")
                }
            },
            onRequestBooking = { target ->
                state.selectedEntity = null
                when (target) {
                    is ProviderEntity -> state.bookingProviderTarget = target
                    is StoreEntity -> onRequestBooking(target)
                    is PropertyEntity -> onRequestBooking(target)
                    else -> onRequestBooking(target)
                }
            },
            onOpenDetails = { ent ->
                state.selectedEntity = null
                when (ent) {
                    is ProviderEntity -> onOpenProviderDetails(ent)
                    is StoreEntity -> onOpenStoreDetails(ent)
                    is PropertyEntity -> onOpenPropertyDetails(ent)
                }
            },
            themeColors = themeColors,
            onStartNavigation = { target ->
                when {
                    !hasRealLocation -> {
                        viewModel.startLocationUpdates()
                        coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                    }
                    !state.startNavigation(target) -> {
                        coroutineScope.launch { snackbarHostState.showSnackbar("موقع هذا المكان غير محدد بدقة، لا يمكن الملاحة إليه") }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    // 📍 بطاقة المكان المختار من البحث
    searchedPlace?.let { place ->
        val dist = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, place.latitude, place.longitude)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B0F19).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(place.shortName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(place.displayName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (isTilesDownloaded)
                        "المسافة المباشرة: ${MapDistanceCalculator.formatDistance(dist)} • توجيه أوفلاين جاهز ✓"
                    else
                        "المسافة المباشرة: ${MapDistanceCalculator.formatDistance(dist)} (تقريبي)",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val wp = NavWaypoint(name = place.shortName, latitude = place.latitude, longitude = place.longitude)
                            if (!hasRealLocation) {
                                viewModel.startLocationUpdates()
                                coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS وانتظر تحديد موقعك") }
                            } else if (state.startNavigation(wp)) {
                                onDismissSearchedPlace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MapUi.RoyalMid),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ابدأ الملاحة", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            state.addWaypoint(NavWaypoint(name = place.shortName, latitude = place.latitude, longitude = place.longitude))
                            coroutineScope.launch { snackbarHostState.showSnackbar("تمت إضافة المحطة للمسار") }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("إضافة كمحطة")
                    }
                    TextButton(onClick = onDismissSearchedPlace, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("إغلاق", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    // 📌 بطاقة النقطة المحددة يدوياً
    pinnedPoint?.let { (pLat, pLng) ->
        if (state.selectedEntity == null && searchedPlace == null) {
            val dist = MapDistanceCalculator.calculateDistanceMeters(safeUserLat, safeUserLng, pLat, pLng)
            var refSummary by remember(pLat, pLng) { mutableStateOf("") }
            LaunchedEffect(pLat, pLng) {
                refSummary = withContext(Dispatchers.IO) {
                    OfflineYemenGazetteer.nearestReferenceSummary(context, pLat, pLng)
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.97f),
                border = BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("📌 نقطة محددة يدوياً", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "\u200E${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)}\u200E",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onDismissPinnedPoint,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق النقطة", tint = Color(0xFF94A3B8))
                        }
                    }
                    if (refSummary.isNotBlank()) {
                        Text("أقرب مرجع: $refSummary", color = Color(0xFF10B981), fontSize = 11.5.sp)
                    }
                    if (hasRealLocation) {
                        Text(
                            "المسافة من موقعك: ${MapDistanceCalculator.formatDistance(dist)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val wp = NavWaypoint(name = "نقطة محددة", latitude = pLat, longitude = pLng)
                                if (!hasRealLocation) {
                                    viewModel.startLocationUpdates()
                                    coroutineScope.launch { snackbarHostState.showSnackbar("فعّل GPS أو حدد موقعك يدوياً من «اعتمد كموقعي» للبدء") }
                                } else if (state.startNavigation(wp)) {
                                    onDismissPinnedPoint()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("🧭 ابدأ الملاحة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                state.addWaypoint(NavWaypoint(name = "نقطة (${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)})", latitude = pLat, longitude = pLng))
                                coroutineScope.launch { snackbarHostState.showSnackbar("📍 تمت إضافة النقطة كمحطة") }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("➕ كمحطة", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.setManualUserLocation(pLat, pLng)
                                onDismissPinnedPoint()
                                coroutineScope.launch { snackbarHostState.showSnackbar("✓ تم اعتماد هذه النقطة كموقعك اليدوي للانطلاق والتوصيل") }
                            },
                            label = { Text("📍 اعتمد كموقعي", fontSize = 11.sp, color = Color(0xFF00E5FF)) }
                        )
                        AssistChip(
                            onClick = { onAddLandmarkForPin(pLat to pLng) },
                            label = { Text("➕ احفظ كمعلم", fontSize = 11.sp, color = Color(0xFFFBBF24)) }
                        )
                        AssistChip(
                            onClick = {
                                onOpenLandmarkForPin(pLat to pLng)
                                onDismissPinnedPoint()
                            },
                            label = { Text("🏠 عنوان بالمعلم", fontSize = 11.sp, color = Color(0xFF10B981)) }
                        )
                        AssistChip(
                            onClick = {
                                val shareText = "موقعي المحدد:\n$refSummary\n${OfflineYemenGazetteer.formatCoordinates(pLat, pLng)}\n${SosDispatcher.mapLink(pLat, pLng)}"
                                SosDispatcher.safeStart(context, SosDispatcher.shareIntent(shareText))
                            },
                            label = { Text("📤 مشاركة", fontSize = 11.sp, color = Color.White) }
                        )
                    }
                }
            }
        }
    }
}
