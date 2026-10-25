package com.example.ui.screens.map

import android.speech.tts.TextToSpeech
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
import androidx.compose.ui.graphics.Color
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
import com.example.utils.RealValhallaNavigator
import com.example.utils.ValhallaTileDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 🧭 NavigationScreen
 * شاشة الملاحة اليمنية التفاعلية الذكية:
 * - شريط بحث متطور لتحديد الوجهة بدقة (بين المدن أو الشوارع)
 * - توجيه صوتي ذكي (TTS) باللغة العربية ونطق الإرشادات عند 150 متر وحين الوصول
 * - التبديل التلقائي الذكي بين ملاحة المدن والملاحة الدقيقة للأحياء والشوارع عبر Valhalla عند تحميل الملف
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

    // استخراج بيانات الوجهة الأولية
    val initialTargetName = when (targetEntity) {
        is ProviderEntity -> targetEntity.name
        is StoreEntity -> targetEntity.name
        is PropertyEntity -> targetEntity.title
        else -> "صنعاء / التحرير"
    }

    val initialTargetLat = when (targetEntity) {
        is ProviderEntity -> targetEntity.latitude
        is StoreEntity -> targetEntity.latitude
        is PropertyEntity -> targetEntity.latitude
        else -> 15.3694
    }

    val initialTargetLng = when (targetEntity) {
        is ProviderEntity -> targetEntity.longitude
        is StoreEntity -> targetEntity.longitude
        is PropertyEntity -> targetEntity.longitude
        else -> 44.1910
    }

    // حالات الوجهة والبحث (المرحلة الثانية)
    var searchQuery by remember { mutableStateOf(initialTargetName) }
    var currentDestName by remember { mutableStateOf(initialTargetName) }
    var currentDestLat by remember { mutableDoubleStateOf(initialTargetLat) }
    var currentDestLng by remember { mutableDoubleStateOf(initialTargetLng) }

    // TextToSpeech initialization (المرحلة الثالثة)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsRef: TextToSpeech? = null
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = ttsRef?.setLanguage(Locale("ar")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                isTtsInitialized = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }
        ttsRef = ttsInstance
        tts = ttsInstance
        onDispose {
            ttsRef?.stop()
            ttsRef?.shutdown()
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // 1. التحقق من حزمة الملاحة المحلية (Valhalla)
    var isTilesDownloaded by remember {
        mutableStateOf(ValhallaTileDownloader.isTilesDownloaded(context) || RealValhallaNavigator.hasTileFile(context))
    }

    // 2. حساب المسار (التبديل الذكي بين RealValhallaNavigator و MockValhallaNavigator)
    val routeResult = remember(userLat, userLng, currentDestLat, currentDestLng, currentDestName, isTilesDownloaded) {
        if (isTilesDownloaded) {
            val realRes = RealValhallaNavigator.calculateRealRoute(context, userLat, userLng, currentDestLat, currentDestLng, currentDestName)
            MockValhallaNavigator.NavigationRouteResult(
                totalDistanceMeters = realRes.totalDistanceMeters,
                totalDurationSeconds = realRes.totalDurationSeconds,
                formattedDistance = realRes.formattedDistance,
                formattedDuration = realRes.formattedDuration,
                steps = realRes.steps.map { MockValhallaNavigator.NavigationStep(it.instruction, it.distanceText, it.distanceMeters, it.iconType) },
                coordinates = realRes.coordinates,
                routeTitle = realRes.routeTitle
            )
        } else {
            MockValhallaNavigator.calculateRoute(
                context = context,
                startLat = userLat,
                startLng = userLng,
                destLat = currentDestLat,
                destLng = currentDestLng,
                destName = currentDestName
            )
        }
    }

    val totalDistanceMeters = routeResult.totalDistanceMeters.coerceAtLeast(300.0)
    var remainingMeters by remember(routeResult) { mutableDoubleStateOf(totalDistanceMeters) }
    var currentSpeedKmh by remember { mutableIntStateOf(50) }
    var currentStepIndex by remember(routeResult) { mutableIntStateOf(0) }
    var hasSpokenArrival by remember(routeResult) { mutableStateOf(false) }
    var hasSpoken150m by remember(routeResult) { mutableStateOf(false) }

    val steps = routeResult.steps.ifEmpty {
        listOf(
            MockValhallaNavigator.NavigationStep("اتجه مباشرة نحو الوجهة", "300 م", 300.0, "STRAIGHT"),
            MockValhallaNavigator.NavigationStep("لقد وصلت إلى وجهتك في $currentDestName", "0 م", 0.0, "DESTINATION")
        )
    }

    // حالة نافذة تحميل الحزمة
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // محاكاة الملاحة اللحظية والمراقبة الصوتية
    LaunchedEffect(routeResult) {
        while (remainingMeters > 15) {
            delay(1500L)
            remainingMeters = (remainingMeters - 160).coerceAtLeast(10.0)

            // نطق التوجيه عند 150 متر
            if (remainingMeters <= 150.0 && !hasSpoken150m) {
                hasSpoken150m = true
                val currentStep = steps.getOrNull(currentStepIndex) ?: steps.first()
                speak("انتبه، ${currentStep.instruction}")
            }

            val progressFraction = 1.0 - (remainingMeters / totalDistanceMeters)
            val stepThreshold = (currentStepIndex + 1).toDouble() / steps.size.toDouble()
            if (progressFraction >= stepThreshold && currentStepIndex < steps.size - 1) {
                currentStepIndex++
                val nextStep = steps[currentStepIndex]
                speak(nextStep.instruction)
            }
        }

        // الوصول إلى الوجهة
        if (!hasSpokenArrival) {
            hasSpokenArrival = true
            remainingMeters = 0.0
            speak("لقد وصلت إلى وجهتك في $currentDestName. الحمد لله على السلامة.")
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
        var zoomScale by remember { mutableFloatStateOf(1.3f) }
        var panOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

        // الخريطة التفاعلية للملاحة
        OfflineInteractiveMap(
            userCoords = Pair(userLat, userLng),
            nearbyProviders = emptyList(),
            nearbyStores = emptyList(),
            nearbyProperties = emptyList(),
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

        // لوحة الإرشادات والبحث العلوية
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // شريط البحث لتحديد الوجهة (المرحلة الثانية)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن شارع، مدينة، أو وجهة...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                currentDestName = searchQuery
                                // تعديل الإحداثيات افتراضياً أو مطابقتها مع المدن اليمنية الشهيرة
                                when {
                                    searchQuery.contains("عدن") -> { currentDestLat = 12.7855; currentDestLng = 45.0187 }
                                    searchQuery.contains("تعز") -> { currentDestLat = 13.5795; currentDestLng = 44.0209 }
                                    searchQuery.contains("الحديد") -> { currentDestLat = 14.7978; currentDestLng = 42.9545 }
                                    searchQuery.contains("إب") -> { currentDestLat = 13.9667; currentDestLng = 44.1833 }
                                    searchQuery.contains("مأرب") -> { currentDestLat = 15.4542; currentDestLng = 45.3253 }
                                    else -> { currentDestLat += 0.01; currentDestLng += 0.01 }
                                }
                                remainingMeters = MapDistanceCalculator.calculateDistanceMeters(userLat, userLng, currentDestLat, currentDestLng).coerceAtLeast(400.0)
                                currentStepIndex = 0
                                hasSpokenArrival = false
                                hasSpoken150m = false
                                speak("تم تحديث الوجهة إلى $currentDestName. جاري إعادة حساب المسار.")
                                Toast.makeText(context, "📍 تم تحديث الوجهة إلى: $currentDestName", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("انتقال", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // شارة حالة الملاحة وزر التنزيل
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, if (isTilesDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            text = if (isTilesDownloaded) "🗺️ ملاحة الشوارع الدقيقة (Valhalla مفعل)" else "🧭 ملاحة بين المدن اليمنية الذكية",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isTilesDownloaded) {
                        Button(
                            onClick = { showDownloadDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحميل الخرائط (25MB)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // بطاقة التوجيه الرئيسي خطوة بخطوة
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
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
                                fontSize = 12.5.sp,
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "الوجهة: $currentDestName",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    // زر نطق الإرشادات يدوياً (المرحلة الثالثة)
                    IconButton(
                        onClick = { speak(currentStep.instruction) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "نطق الإرشادات",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // الشريط السفلي للملاحة والوصول
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
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = remainingEtaText,
                                color = Color(0xFF10B981),
                                fontSize = 20.sp,
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

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF334155)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                speak("لقد وصلت إلى وجهتك في $currentDestName")
                                Toast.makeText(context, "🏁 تم إعلان الوصول إلى: $currentDestName", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعلان الوصول", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onFinishNavigation,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("end_navigation_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إنهاء الملاحة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // نافذة تحميل حزمة الملاحة الدقيقة
        if (showDownloadDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDownloading) showDownloadDialog = false },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp)) },
                title = { Text(text = "تحميل حزمة خريطة الشوارع (25 MB)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "تحميل حزمة بلاطات Valhalla الأوفلاين يتيح لك الانتقال من ملاحة المدن إلى ملاحة الأحياء والشوارع الدقيقة بدون إنترنت.",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 20.sp
                        )
                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Text(text = downloadStatusText, fontSize = 11.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
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
                                        Toast.makeText(context, "✅ تم تثبيت خريطة الشوارع والأحياء بنجاح! 🇾🇪", Toast.LENGTH_LONG).show()
                                    } else {
                                        val err = result.exceptionOrNull()?.localizedMessage ?: "خطأ"
                                        Toast.makeText(context, "❌ فشل التحميل: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تحميل مباشر (25MB)", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { showDownloadDialog = false }) {
                            Text("إلغاء", color = Color(0xFF94A3B8))
                        }
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
