package com.example.ui.screens.map.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.ProviderEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.rememberHeadingDegrees

private fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dl = Math.toRadians(lon2 - lon1)
    val y = Math.sin(dl) * Math.cos(p2)
    val x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl)
    return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
}

private data class InView(val provider: ProviderEntity, val distanceM: Double, val relativeDeg: Double)

@Composable
private fun CameraXPreview(modifier: Modifier = Modifier, onError: (String) -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx).apply {
                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
            }
            val future = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                try {
                    val provider = future.get()
                    val preview = androidx.camera.core.Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                } catch (t: Throwable) {
                    onError(t.message ?: "تعذر فتح الكاميرا")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

/**
 * 📷 ArVpsCameraOverlay
 * واقع معزز صادق يعتمد على موقع GPS الفعلي وحساس البوصلة لعرض الخدمات القريبة أمام الكاميرا.
 */
@Composable
fun ArVpsCameraOverlay(
    userLat: Double,
    userLng: Double,
    hasUserLocation: Boolean = true,
    nearbyProviders: List<ProviderEntity>,
    onClose: () -> Unit,
    onProviderSelected: (ProviderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val heading = rememberHeadingDegrees(enabled = true, lat = userLat, lng = userLng)

    val inView = remember(nearbyProviders, userLat, userLng, heading, hasUserLocation) {
        if (!hasUserLocation) emptyList()
        else nearbyProviders.mapNotNull { p ->
            if (p.latitude == 0.0 || p.longitude == 0.0) return@mapNotNull null
            val d = MapDistanceCalculator.calculateDistanceMeters(userLat, userLng, p.latitude, p.longitude)
            if (d > 1500.0) return@mapNotNull null // نطاق الرؤية 1.5 كم
            val rel = ((bearingDeg(userLat, userLng, p.latitude, p.longitude) - heading + 540.0) % 360.0) - 180.0
            if (kotlin.math.abs(rel) > 35.0) null else InView(p, d, rel) // زاوية رؤية ±35°
        }.sortedBy { it.distanceM }.take(5)
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        if (hasCameraPermission) {
            if (cameraError != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(cameraError ?: "خطأ في الكاميرا", color = Color.White)
                }
            } else {
                CameraXPreview(modifier = Modifier.fillMaxSize(), onError = { cameraError = it })
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("يلزم إذن الكاميرا لتشغيل الواقع المعزز", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("منح الإذن")
                    }
                }
            }
        }

        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color(0xFF0F172A).copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
        }

        // Truthful Status Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "الواقع المعزز (بالموقع والبوصلة)",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasUserLocation) "تُعرض الأماكن القريبة في اتجاه الكاميرا (${heading.toInt()}°)"
                    else "فعّل GPS لعرض الأماكن حسب الاتجاه",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }

        // Crosshair reticle in center
        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .background(Color.Transparent)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {}
            Box(
                Modifier
                    .size(6.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFF00E5FF), CircleShape)
            )
        }

        // Bottom In-View Items Cards
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                if (inView.isNotEmpty()) "أماكن قريبة في اتجاه الكاميرا:" else "لا أماكن مسجلة أمامك ضمن 1.5 كم",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (inView.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(inView) { v ->
                        val p = v.provider
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .widthIn(min = 160.dp)
                                .clickable { onProviderSelected(p) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(p.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(p.profession, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${MapDistanceCalculator.formatDistance(v.distanceM)} • ${if (v.relativeDeg < 0) "يسارك" else "يمينك"}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
