package com.example.ui.screens.map

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.components.OfflineInteractiveMap
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.MockValhallaNavigator
import com.example.utils.ValhallaTileDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🧭 NavigationScreen
 * شاشة الملاحة اليمنية التفاعلية الذكية:
 * - تدعم التبديل الذكي التلقائي بين "محاكي الملاحة السريعة" و"حزمة الملاحة الأوفلاين الدقيقة"
 * - حساب دقيق لمسافات الطرق بين المدن اليمنية (صنعاء، عدن، تعز، الحديدة، إب، ذمار، مأرب، حضرموت...)
 * - خيار تحميل حزمة الـ 25 ميجابايت داخل التطبيق مع شريط تقدم مباشر
 * - توجيهات الشوارع خطوة بخطوة باللغة العربية
 */
@Composable
fun NavigationScreen(
    targetEntity: Any?,
    userLat: Double,
    userLng: Double,
    onFinishNavigation: () -> Unit,
    downloadUrl: String = ValhallaTileDownloader.DEFAULT_DOWNLOAD_URL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // استخراج بيانات الوجهة بدقة
    val targetName = when (targetEntity) {
        is ProviderEntity -> targetEntity.name
        is StoreEntity -> targetEntity.name
        is PropertyEntity -> targetEntity.title
        else -> "الوجهة المحددة"
    }

    val targetLat = when (targetEntity) {
        is ProviderEntity -> targetEntity.latitude
        is StoreEntity -> targetEntity.latitude
        is PropertyEntity -> targetEntity.latitude
        else -> 15.3694
    }

    val targetLng = when (targetEntity) {
        is ProviderEntity -> targetEntity.longitude
        is StoreEntity -> targetEntity.longitude
        is PropertyEntity -> targetEntity.longitude
        else -> 44.1910
    }

    // 1. التحقق من وجود حزمة الملاحة الدقيقة (المرحلة الثالثة: التبديل التلقائي)
    var isTilesDownloaded by remember {
        mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context))
    }

    // 2. حساب المسار عبر المحاكي المتقدم أو المحرك الدقيق (المرحلة الأولى)
    val routeResult = remember(userLat, userLng, targetLat, targetLng, targetName, isTilesDownloaded) {
        MockValhallaNavigator.calculateRoute(
            context = context,
            startLat = userLat,
            startLng = userLng,
            destLat = targetLat,
            destLng = targetLng,
            destName = targetName
        )
    }

    val totalDistanceMeters = routeResult.totalDistanceMeters.coerceAtLeast(350.0)
    var remainingMeters by remember(routeResult) { mutableDoubleStateOf(totalDistanceMeters) }
    var currentSpeedKmh by remember { mutableIntStateOf(55) }
    var currentStepIndex by remember(routeResult) { mutableIntStateOf(0) }

    val steps = routeResult.steps.ifEmpty {
        listOf(
            MockValhallaNavigator.NavigationStep("اتجه مباشرة نحو الوجهة", "300 م", 300.0, "STRAIGHT"),
            MockValhallaNavigator.NavigationStep("لقد وصلت إلى $targetName", "0 م", 0.0, "DESTINATION")
        )
    }

    // حالة نافذة تحميل الحزمة (المرحلة الثانية)
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // محاكاة التقدم في الملاحة اللحظية
    LaunchedEffect(routeResult) {
        while (remainingMeters > 40) {
            delay(1500L)
            remainingMeters = (remainingMeters - 180).coerceAtLeast(20.0)
            val progressFraction = 1.0 - (remainingMeters / totalDistanceMeters)
            val stepThreshold = (currentStepIndex + 1).toDouble() / steps.size.toDouble()
            if (progressFraction >= stepThreshold && currentStepIndex < steps.size - 1) {
                currentStepIndex++
            }
        }
    }

    val progress = (1.0 - (remainingMeters / totalDistanceMeters)).toFloat().coerceIn(0.05f, 1.0f)
    val remainingDistanceText = MapDistanceCalculator.formatDistance(remainingMeters)
    val remainingEtaText = MapDistanceCalculator.computeEta(remainingMeters)

    val currentStep = steps.getOrNull(currentStepIndex) ?: steps.last()
    val stepIcon = when (currentStep.iconType) {
        "LEFT" -> Icons.Default.TurnLeft
        "RIGHT" -> Icons.Default.TurnRight
        "HIGHWAY" -> Icons.Default.DirectionsCar
        "DESTINATION" -> Icons.Default.CheckCircle
        else -> Icons.Default.Straight
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
    ) {
        var zoomScale by remember { mutableFloatStateOf(1.2f) }
        var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        // خلفية الخريطة الحية للملاحة
        OfflineInteractiveMap(
            userCoords = Pair(userLat, userLng),
            nearbyProviders = emptyList<ProviderEntity>(),
            nearbyStores = emptyList<StoreEntity>(),
            nearbyProperties = emptyList<PropertyEntity>(),
            dynamicOffsets = emptyMap(),
            selectedCity = "الكل",
            zoomScale = zoomScale,
            onZoomScaleChange = { zoomScale = it },
            panOffset = panOffset,
            onPanOffsetChange = { panOffset = it },
            selectedEntity = targetEntity,
            onProviderSelected = {},
            onStoreSelected = {},
            onPropertySelected = {},
            onDeselect = {},
            modifier = Modifier.fillMaxSize()
        )

        // 1. لوحة الإرشادات العلوية (Turn-by-turn Navigation Instruction Header)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // شارة حالة وضع الملاحة وزر التنزيل
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = if (isTilesDownloaded) "🌟 ملاحة أوفلاين دقيقة (Valhalla 🇾🇪)" else "⚡ محاكي الملاحة السريع (${routeResult.routeTitle})",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isTilesDownloaded) {
                        Button(
                            onClick = { showDownloadDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحميل الخرائط (25MB)", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // بطاقة التوجيه الرئيسي خطوة بخطوة
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // أيقونة التوجيه المضيئة
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = "توجيه المسار",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "بعد ${currentStep.distanceText}",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "· $currentSpeedKmh كم/س",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentStep.instruction,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "نحو: $targetName (إجمالي: ${routeResult.formattedDistance})",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                    }

                    // زر إنهاء / خروج
                    IconButton(
                        onClick = onFinishNavigation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("exit_navigation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إنهاء الملاحة",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // 2. الشريط السفلي لمؤشرات الملاحة والوقت والمسافة المتبقية
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // تفاصيل الوقت والمسافة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = remainingEtaText,
                                color = Color(0xFF10B981),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الوقت المتوقع للوصول",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = remainingDistanceText,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المسافة المتبقية",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // شريط التقدم المرئي
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF334155)
                    )

                    // زر إنهاء الملاحة
                    Button(
                        onClick = onFinishNavigation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("end_navigation_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنهاء الملاحة والعودة للخريطة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. نافذة تحميل الملاحة الكاملة (المرحلة الثانية)
        if (showDownloadDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isDownloading) showDownloadDialog = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "تحميل حزمة الملاحة الكاملة (أوفلاين)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "لتفعيل الملاحة الدقيقة خطوة بخطوة داخل الشوارع والأزقة اليمنية بدون إنترنت، يحتاج التطبيق إلى تحميل ملف الخرائط (25 ميجابايت). يُنصح باستخدام اتصال Wi-Fi.",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 20.sp
                        )

                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Text(
                                text = downloadStatusText,
                                fontSize = 11.5.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    if (!isDownloading) {
                        Button(
                            onClick = {
                                isDownloading = true
                                coroutineScope.launch {
                                    val result = ValhallaTileDownloader.downloadAndExtractTiles(
                                        context = context,
                                        downloadUrl = downloadUrl,
                                        onProgress = { p, status ->
                                            downloadProgress = p
                                            downloadStatusText = status
                                        }
                                    )
                                    isDownloading = false
                                    if (result.isSuccess) {
                                        isTilesDownloaded = true
                                        showDownloadDialog = false
                                        Toast.makeText(context, "✅ تم تثبيت حزمة الملاحة الأوفلاين بنجاح! 🇾🇪", Toast.LENGTH_LONG).show()
                                    } else {
                                        val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ في الاتصال"
                                        Toast.makeText(context, "❌ فشل التحميل: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تحميل الآن (25 MB)", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(
                            onClick = { showDownloadDialog = false }
                        ) {
                            Text("لاحقاً", color = Color(0xFF94A3B8))
                        }
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
