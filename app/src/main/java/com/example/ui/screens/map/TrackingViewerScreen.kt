package com.example.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.TrackingShareManager
import com.example.utils.TrackingSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TrackingViewerScreen(token: String, onClose: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    var snap by remember { mutableStateOf<TrackingSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var cooldown by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            loading = true
            val s = TrackingShareManager.fetch(db, token)
            snap = s
            failed = s == null
            loading = false
            cooldown = 15
            while (cooldown > 0) {
                delay(1000L)
                cooldown--
            }
        }
    }
    LaunchedEffect(token) { load() }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0F19))) {
        val s = snap
        RealLeafletMapView(
            userCoords = Pair(0.0, 0.0),
            nearbyProviders = emptyList(),
            nearbyStores = emptyList(),
            nearbyProperties = emptyList(),
            dynamicOffsets = emptyMap(),
            currentMapMode = MapMode.OSM,
            selectedCity = "الكل",
            selectedEntity = null,
            onProviderSelected = {},
            onStoreSelected = {},
            onPropertySelected = {},
            onDeselect = {},
            waypointMarkers = if (s != null) listOf(Triple(s.label.ifBlank { "الموقع" }, s.lat, s.lng)) else emptyList(),
            focusPoint = s?.let { Triple(it.label.ifBlank { "الموقع" }, it.lat, it.lng) },
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.97f),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    loading && s == null -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                    failed -> Text("الرمز غير صالح أو انتهت المشاركة.", color = Color(0xFFF59E0B), fontSize = 14.sp)
                    s != null -> {
                        val ageMin = ((System.currentTimeMillis() - s.updatedAt) / 60_000L).coerceAtLeast(0)
                        Text("آخر تحديث: منذ $ageMin دقيقة", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (ageMin >= 5) Text("الموقع قديم؛ اضغط «تحديث» أو اتصل بالمرسل.", color = Color(0xFFF59E0B), fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { load() }, enabled = !loading && cooldown == 0, modifier = Modifier.weight(1f)) {
                        Text(if (cooldown > 0) "تحديث ($cooldown)" else "تحديث")
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("إغلاق") }
                }
                Text("هذه المشاركة مؤقتة وتنتهي تلقائياً.", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}
