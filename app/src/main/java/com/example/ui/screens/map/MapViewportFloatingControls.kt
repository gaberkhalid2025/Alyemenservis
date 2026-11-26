package com.example.ui.screens.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassIconButton
import com.example.ui.theme.MapUi
import com.example.utils.LandmarkAddress
import com.example.utils.OfflineYemenGazetteer

@Composable
internal fun BoxScope.MapViewportFloatingControls(
    state: MapScreenState,
    safeUserLat: Double,
    safeUserLng: Double,
    isPinPickMode: Boolean,
    centerCoords: Pair<Double, Double>?,
    areTilesFailing: Boolean,
    onTilesStatus: (Boolean) -> Unit,
    gpsWeak: Boolean,
    accuracyForBanner: Float,
    sosPending: Boolean,
    savedAddresses: List<LandmarkAddress>,
    onMapPicked: (Double, Double) -> Unit,
    onCancelPinPick: () -> Unit,
    onOpenLandmarkAddress: () -> Unit,
    onOpenSavedAddresses: () -> Unit
) {
    // 🌐 زر كرية زجاجية أنيقة لنمط الخريطة (قياسي / قمر صناعي / تضاريس / أوفلاين) مع قائمة منسدلة
    var showMapTypeDropdown by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 74.dp, end = 14.dp)
    ) {
        GlassIconButton(
            icon = when (state.currentMapMode) {
                MapMode.SATELLITE, MapMode.SATELLITE_CLARITY -> Icons.Default.SatelliteAlt
                MapMode.TOPO -> Icons.Default.Terrain
                else -> Icons.Default.Public
            },
            description = "نمط الخريطة",
            onClick = { showMapTypeDropdown = true },
            active = true,
            tint = when (state.currentMapMode) {
                MapMode.SATELLITE, MapMode.SATELLITE_CLARITY -> MapUi.RoyalTop
                MapMode.TOPO -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            },
            size = 48.dp,
            modifier = Modifier.testTag("map_type_dropdown_btn")
        )

        DropdownMenu(
            expanded = showMapTypeDropdown,
            onDismissRequest = { showMapTypeDropdown = false },
            containerColor = MapUi.SurfaceSolid,
            modifier = Modifier
                .border(1.dp, MapUi.BorderGlow, RoundedCornerShape(16.dp))
                .widthIn(min = 210.dp)
        ) {
            val isStandard = (state.currentMapMode == MapMode.OSM || state.currentMapMode == MapMode.VELA) &&
                state.mapLayer != MapLayer.VALHALLA_OFFLINE
            DropdownMenuItem(
                text = {
                    Text(
                        "خريطة قياسية (شوارع)",
                        fontWeight = if (isStandard) FontWeight.Bold else FontWeight.Normal,
                        color = if (isStandard) Color(0xFF10B981) else Color.White
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Public, contentDescription = null, tint = if (isStandard) Color(0xFF10B981) else Color(0xFF94A3B8))
                },
                trailingIcon = if (isStandard) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                } else null,
                onClick = {
                    state.currentMapMode = MapMode.OSM
                    state.mapLayer = MapLayer.STANDARD
                    showMapTypeDropdown = false
                }
            )

            val isSat = state.currentMapMode == MapMode.SATELLITE || state.currentMapMode == MapMode.SATELLITE_CLARITY
            DropdownMenuItem(
                text = {
                    Text(
                        "قمر صناعي (أقمار حقيقية)",
                        fontWeight = if (isSat) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSat) MapUi.RoyalTop else Color.White
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = if (isSat) MapUi.RoyalTop else Color(0xFF94A3B8))
                },
                trailingIcon = if (isSat) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = MapUi.RoyalTop, modifier = Modifier.size(16.dp)) }
                } else null,
                onClick = {
                    state.currentMapMode = MapMode.SATELLITE
                    showMapTypeDropdown = false
                }
            )

            val isTopo = state.currentMapMode == MapMode.TOPO
            DropdownMenuItem(
                text = {
                    Text(
                        "خريطة تضاريس (Topo)",
                        fontWeight = if (isTopo) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTopo) Color(0xFFF59E0B) else Color.White
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Terrain, contentDescription = null, tint = if (isTopo) Color(0xFFF59E0B) else Color(0xFF94A3B8))
                },
                trailingIcon = if (isTopo) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
                } else null,
                onClick = {
                    state.currentMapMode = MapMode.TOPO
                    showMapTypeDropdown = false
                }
            )

            val isOffline = state.mapLayer == MapLayer.VALHALLA_OFFLINE
            DropdownMenuItem(
                text = {
                    Text(
                        "وضع أوفلاين بدون إنترنت",
                        fontWeight = if (isOffline) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOffline) Color(0xFF10B981) else Color.White
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = if (isOffline) Color(0xFF10B981) else Color(0xFF94A3B8))
                },
                trailingIcon = if (isOffline) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) }
                } else null,
                onClick = {
                    state.mapLayer = if (isOffline) MapLayer.STANDARD else MapLayer.VALHALLA_OFFLINE
                    showMapTypeDropdown = false
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            DropdownMenuItem(
                text = {
                    Text(
                        if (state.showLabelsOnMap) "إخفاء أسماء الشوارع" else "إظهار أسماء الشوارع",
                        color = Color.White
                    )
                },
                leadingIcon = {
                    Icon(
                        if (state.showLabelsOnMap) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MapUi.Accent
                    )
                },
                onClick = {
                    state.showLabelsOnMap = !state.showLabelsOnMap
                    showMapTypeDropdown = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text("تصفية معالم الخريطة", color = Color.White)
                },
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MapUi.Accent)
                },
                onClick = {
                    state.showLandmarkFilterSheet = true
                    showMapTypeDropdown = false
                }
            )
        }
    }

    // ⚠️ حزمة تنبيهات الخريطة المكدسة بانتظام أسفل الشارات لمنع أي تداخل بصري
    val showTilesBanner = areTilesFailing && !state.isRadarMode && state.mapLayer != MapLayer.VALHALLA_OFFLINE
    if (showTilesBanner || gpsWeak || sosPending) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 124.dp, start = 16.dp, end = 86.dp)
        ) {
            if (sosPending) {
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🚨 نداء الفزعة محفوظ وقيد الإرسال تلقائياً…",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (showTilesBanner) {
                Surface(
                    color = Color(0xDD7F1D1D),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "تعذر جلب بلاطات الخريطة",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                onTilesStatus(false)
                                state.reloadTilesTrigger += 1
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("إعادة المحاولة", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (gpsWeak) {
                Surface(
                    color = Color(0xCC7C2D12),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "إشارة GPS ضعيفة (±${accuracyForBanner.toInt()} م). لعنوان دقيق استخدم «عنواني بالمعلم».",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onOpenLandmarkAddress) {
                            Text("فتح", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 🎯 وضع تحديد نقطة بدبوس المنتصف أو النقر
    if (isPinPickMode) {
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "مؤشر تحديد النقطة",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(42.dp).offset(y = (-16).dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF))
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 90.dp)
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val active = centerCoords ?: (safeUserLat to safeUserLng)
                Text("📌 تحديد نقطة يدوياً على الخريطة", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "حرّك الخريطة لوضع المؤشر على المكان المطلوب أو اضغط مباشرة فوقه (${OfflineYemenGazetteer.formatCoordinates(active.first, active.second)})",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onMapPicked(active.first, active.second) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("تثبيت هذه النقطة ✓", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCancelPinPick,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("إلغاء", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    // 🏠 زر كرية زجاجية «عنواني بالمعلم» (في الجانب المقابل لشريط الأدوات لتجنب التداخل)
    GlassIconButton(
        icon = Icons.Default.HomeWork,
        description = "عنواني بالمعلم",
        onClick = {
            if (savedAddresses.isEmpty()) onOpenLandmarkAddress()
            else onOpenSavedAddresses()
        },
        tint = MapUi.RoyalTop,
        size = 48.dp,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .navigationBarsPadding()
            .padding(start = 16.dp, bottom = 84.dp)
            .testTag("map_landmark_address_orb_btn")
    )
}
