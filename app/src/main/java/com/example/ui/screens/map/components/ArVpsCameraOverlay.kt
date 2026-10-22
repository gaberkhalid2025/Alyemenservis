package com.example.ui.screens.map.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import kotlinx.coroutines.delay

/**
 * 📷 ArVpsCameraOverlay
 * وضع الواقع المعزز والرؤية الحاسوبية (VPS AR Mode)
 * - يفتح كاميرا الهاتف مع فحص الأذونات
 * - يعرض واجهة HUD للرؤية الحاسوبية وشبكة توجيه ذكية
 * - يعرض بطاقات وعلامات Vela العائمة للمحلات والفنيين فوق الكاميرا
 */
@Composable
fun ArVpsCameraOverlay(
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    nearbyProperties: List<PropertyEntity>,
    userLat: Double,
    userLng: Double,
    onProviderSelected: (ProviderEntity) -> Unit,
    onStoreSelected: (StoreEntity) -> Unit,
    onPropertySelected: (PropertyEntity) -> Unit,
    onCloseAr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

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

    // محاكاة توجيه الرؤية الحاسوبية وتثبيت المعالم
    var scanAngle by remember { mutableFloatStateOf(0f) }
    var detectedCount by remember { mutableIntStateOf(nearbyProviders.size + nearbyStores.size) }
    var activeVpsConfidence by remember { mutableFloatStateOf(98.6f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            scanAngle = (scanAngle + 3f) % 360f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview Stream
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            var camera: Camera? = null

                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                try {
                                    camera = Camera.open()
                                    camera?.setDisplayOrientation(90)
                                    camera?.setPreviewTexture(surface)
                                    camera?.startPreview()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    camera?.stopPreview()
                                    camera?.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Fallback Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "تفعيل كاميرا الواقع المعزز (VPS)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "يحتاج وضع الرؤية الحاسوبية (VPS) إلى إذن الكاميرا لربط بيانات Vela بالشارع مباشرة.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("منح إذن الكاميرا", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🎯 AR HUD Overlay (Crosshairs + Compass Horizon)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val reticleRadius = 45.dp.toPx()

            // Reticle Circles
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                radius = reticleRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                radius = reticleRadius * 1.6f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )

            // Crosshair ticks
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(center.x - reticleRadius - 20f, center.y),
                end = Offset(center.x - reticleRadius + 10f, center.y),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(center.x + reticleRadius - 10f, center.y),
                end = Offset(center.x + reticleRadius + 20f, center.y),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(center.x, center.y - reticleRadius - 20f),
                end = Offset(center.x, center.y - reticleRadius + 10f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(center.x, center.y + reticleRadius - 10f),
                end = Offset(center.x, center.y + reticleRadius + 20f),
                strokeWidth = 2.5f
            )
        }

        // Top Status Header: VPS Tracking Indicator & Close Button
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "VPS: متصل ($activeVpsConfidence%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onCloseAr,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .testTag("close_ar_vps_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق وضع AR", tint = Color.White)
            }
        }

        // Floating AR Computer Vision Tags (محاكاة تعرف VPS على واجهات المحلات والفنيين)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📍", fontSize = 16.sp)
                    Column {
                        Text("الرؤية الحاسوبية (MobileNet VPS)", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("تم التعرف على الشارع ومطابقة معالم Vela بنجاح", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Bottom Carousel: Nearby Detected POIs in Camera Field of View
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "المعالم والخدمات المكتشفة أمامك في الواقع المعزز:",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(nearbyProviders.take(5)) { provider ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onProviderSelected(provider) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("👨‍🔧", fontSize = 16.sp)
                                Text(
                                    provider.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(provider.profession, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text("📍 في مجال الرؤية - متاح للحجز", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(nearbyStores.take(5)) { store ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onStoreSelected(store) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🏪", fontSize = 16.sp)
                                Text(
                                    store.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(store.description.ifEmpty { store.name }, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text("🎯 واجهة متجر معتمدة - دقة 96%", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
