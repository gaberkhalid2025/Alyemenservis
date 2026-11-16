package com.example.ui.screens.map

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🗨️ NavigationDialogsHost
 * استضافة جميع الحوارات المنبثقة لشاشة الملاحة:
 * تأكيد الخروج، تأكيد إضافة محطة، البحث عن محطة توقف، إعداد الصوت واللهجة، وتحميل حزمة الأوفلاين
 */
@Composable
internal fun NavigationDialogsHost(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    userLat: Double,
    userLng: Double,
    nearbyProviders: List<ProviderEntity>,
    nearbyStores: List<StoreEntity>,
    downloadUrl: String,
    showExitConfirm: Boolean,
    onDismissExitConfirm: () -> Unit,
    onConfirmFinishNavigation: () -> Unit,
    pendingStop: NavWaypoint?,
    onDismissPendingStop: () -> Unit,
    onAddWaypoint: (NavWaypoint) -> Unit,
    showAddStopDialog: Boolean,
    onDismissAddStopDialog: () -> Unit,
    showVoiceSetupDialog: Boolean,
    onDismissVoiceSetupDialog: () -> Unit,
    voiceState: ArabicNavigationVoiceEngine.VoiceEngineState,
    dialectVoice: Boolean,
    onToggleDialect: (Boolean) -> Unit,
    voicePrefs: SharedPreferences,
    voiceEngine: ArabicNavigationVoiceEngine,
    showDownloadDialog: Boolean,
    onDismissDownloadDialog: () -> Unit,
    onTilesDownloaded: () -> Unit
) {
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = onDismissExitConfirm,
            containerColor = Color(0xFF0F172A),
            title = { Text("إنهاء الملاحة؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("سيتوقف التوجيه الصوتي وتُحذف مسارات الملاحة النشطة.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismissExitConfirm()
                    onConfirmFinishNavigation()
                }) {
                    Text("إنهاء", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitConfirm) {
                    Text("متابعة", color = Color(0xFF10B981))
                }
            }
        )
    }

    pendingStop?.let { stop ->
        AlertDialog(
            onDismissRequest = onDismissPendingStop,
            containerColor = Color(0xFF0F172A),
            title = { Text("إضافة محطة؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("إضافة «${stop.name}» كمحطة في نهاية الرحلة؟", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(onClick = {
                    onAddWaypoint(stop)
                    onDismissPendingStop()
                    scope.launch { snackbarHostState.showSnackbar("تمت إضافة المحطة: ${stop.name}") }
                }) {
                    Text("إضافة", color = Color(0xFF10B981))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPendingStop) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showAddStopDialog) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<RealValhallaNavigator.SearchAddressResult>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        val offlineSuggestions = remember(userLat, userLng) {
            OfflineYemenGazetteer.suggestionsNear(userLat, userLng, limit = 8)
        }
        LaunchedEffect(query) {
            val q = query.trim()
            if (q.length < 2) {
                results = emptyList()
                message = null
                searching = false
                return@LaunchedEffect
            }
            delay(400)
            searching = true
            results = RealValhallaNavigator.searchAddressAndPlaces(
                query = q,
                nearbyProviders = nearbyProviders,
                nearbyStores = nearbyStores,
                userLat = userLat,
                userLng = userLng,
                context = context
            )
            message = if (results.isEmpty()) (RealValhallaNavigator.lastSearchError ?: "لا توجد نتائج لـ «$q»") else null
            searching = false
        }
        AlertDialog(
            onDismissRequest = onDismissAddStopDialog,
            containerColor = Color(0xFF0F172A),
            title = { Text("إضافة محطة توقف جديدة", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("ابحث عن مدينة، شارع، جولة، أو فني (مثال: حدة، الزبيري)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (searching) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00E5FF))
                    message?.let { Text(it, color = Color(0xFFF59E0B), fontSize = 12.sp) }
                    val listToShow = if (query.trim().length < 2) offlineSuggestions else results
                    if (query.trim().length < 2 && listToShow.isNotEmpty()) {
                        Text("وجهات سريعة قريبة (تعمل بدون إنترنت):", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listToShow) { res ->
                            Surface(
                                onClick = {
                                    onAddWaypoint(NavWaypoint(name = res.shortName, latitude = res.latitude, longitude = res.longitude))
                                    onDismissAddStopDialog()
                                    scope.launch { snackbarHostState.showSnackbar("تمت إضافة المحطة: ${res.shortName}") }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(res.icon, fontSize = 16.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(res.shortName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(res.displayName, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissAddStopDialog) { Text("إغلاق", color = Color(0xFF00E5FF)) } },
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showVoiceSetupDialog) {
        AlertDialog(
            onDismissRequest = onDismissVoiceSetupDialog,
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "إعداد الصوت العربي واللهجة اليمنية",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (voiceState.isArabicReady) Color(0xFF064E3B) else Color(0xFF451A03),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (voiceState.isArabicReady) {
                                "✅ الصوت العربي جاهز (${voiceState.activeEngineLabel})"
                            } else {
                                "🔔 تعمل الآن نغمات التوجيه الذكية (أوفلاين) لتنبيهك عند المنعطفات."
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = dialectVoice,
                            onClick = {
                                onToggleDialect(true)
                                voicePrefs.edit().putBoolean("dialect", true).apply()
                            },
                            label = { Text("لهجة يمنية 🇾🇪", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !dialectVoice,
                            onClick = {
                                onToggleDialect(false)
                                voicePrefs.edit().putBoolean("dialect", false).apply()
                            },
                            label = { Text("عربية فصحى", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "ملاحظة: في إعدادات النظام، اختر «العربية» فقط (لا توجد لغة باسم يمنية في أندرويد)، والتطبيق ينطقها تلقائياً باللهجة اليمنية عند تفعيلها أعلاه. على أجهزة سامسونج يُنصح باختيار محرك Google Speech Services.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = {
                            val sample = if (dialectVoice) "بعد ميتين متر، لف يمين" else "بعد مئتي متر، انعطف يميناً"
                            voiceEngine.speak(sample, ManeuverType.RIGHT, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔊 تجربة الصوت الآن", fontWeight = FontWeight.Bold)
                    }

                    if (!voiceState.isArabicReady) {
                        if (voiceState.googleTtsInstalled) {
                            OutlinedButton(
                                onClick = { ArabicNavigationVoiceEngine.openGoogleTtsVoiceDownload(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تنزيل حزمة العربية عبر Google", color = Color(0xFF00E5FF))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { ArabicNavigationVoiceEngine.openGoogleTtsPlayStore(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تثبيت محرك Google للنطق", color = Color(0xFF00E5FF))
                            }
                        }
                        OutlinedButton(
                            onClick = { ArabicNavigationVoiceEngine.openSystemTtsSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("فتح إعدادات النطق بالنظام", color = Color.White)
                        }
                    }

                    TextButton(
                        onClick = {
                            voiceEngine.initialize()
                            scope.launch { snackbarHostState.showSnackbar("جاري إعادة فحص محركات النطق...") }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("🔄 إعادة فحص المحركات بعد التثبيت", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissVoiceSetupDialog) {
                    Text("إغلاق", color = Color(0xFF00E5FF))
                }
            }
        )
    }

    if (showDownloadDialog) {
        var isDownloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var downloadStatusText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!isDownloading) onDismissDownloadDialog() },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(36.dp)) },
            title = { Text("تحميل حزمة الملاحة (أوفلاين)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "لتفعيل الملاحة بدون إنترنت يحتاج التطبيق إلى تحميل حزمة خرائط اليمن.\nحجم التنزيل: ~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} ميجابايت مضغوطة (تُفكّ إلى ~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} ميجابايت على الجهاز).",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    if (isDownloading) {
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF10B981), trackColor = Color(0xFF334155))
                        Text(downloadStatusText, fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            isDownloading = true
                            scope.launch {
                                val r = ValhallaTileDownloader.downloadAndExtractTiles(context, downloadUrl) { p, s -> downloadProgress = p; downloadStatusText = s }
                                isDownloading = false
                                if (r.isSuccess) {
                                    onTilesDownloaded()
                                    onDismissDownloadDialog()
                                    snackbarHostState.showSnackbar("تم تثبيت حزمة الملاحة الأوفلاين (~${ValhallaTileDownloader.EXPECTED_INSTALLED_MB} MB) ✓")
                                } else {
                                    snackbarHostState.showSnackbar("فشل التحميل: ${r.exceptionOrNull()?.message ?: "تحقق من الاتصال"}")
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nav_download_offline_confirm_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("تحميل الآن (~${ValhallaTileDownloader.EXPECTED_DOWNLOAD_MB} MB)", fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { if (!isDownloading) TextButton(onClick = onDismissDownloadDialog) { Text("لاحقاً", color = Color(0xFF94A3B8)) } },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp)
        )
    }
}
