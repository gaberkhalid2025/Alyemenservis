package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.ui.screens.map.utils.OfflineTileInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class DiagStatus(val icon: String) { OK("✅"), FAIL("❌"), WARN("⚠️") }
data class DiagnosticItem(val title: String, val status: DiagStatus, val detail: String)

object MapDiagnostics {

    suspend fun run(context: Context, hasRealLocation: Boolean, accuracyM: Float?): List<DiagnosticItem> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<DiagnosticItem>()

            // 1) حزمة الملاحة
            val tilesOk = ValhallaTileDownloader.isTilesDownloaded(context)
            val tarFile = ValhallaTileDownloader.resolveTilesFile(ValhallaTileDownloader.getValhallaDir(context))
            val tarMb = if (tarFile.exists()) String.format(Locale.US, "%.1f", tarFile.length() / 1024.0 / 1024.0) else "0"
            out += if (tilesOk) DiagnosticItem("حزمة الملاحة الأوفلاين", DiagStatus.OK, "مثبّتة وسليمة ($tarMb MB مفكوكة)")
            else DiagnosticItem("حزمة الملاحة الأوفلاين", DiagStatus.FAIL, "غير مثبّتة أو تالفة (المطلوب ~26 MB مضغوطة / ~65 MB مفكوكة)")

            // 2) محرك Valhalla: إصلاح الإعدادات تلقائياً عند الحاجة ثم مسار حقيقي داخل صنعاء
            if (tilesOk) {
                if (!ValhallaTileDownloader.isValhallaConfigValid(context)) {
                    ValhallaTileDownloader.repairConfig(context)
                }
                val origin = NavWaypoint(name = "أ", latitude = 15.3694, longitude = 44.1910, type = WaypointType.START)
                val dest = NavWaypoint(name = "ب", latitude = 15.3500, longitude = 44.2000, type = WaypointType.BREAK)
                val r = try { RealValhallaNavigator.calculateRoute(context, origin, listOf(dest)) } catch (t: Throwable) { null }
                val viaValhalla = r != null && r.error == null && r.coordinates.size >= 2 && r.engineSource.contains("Valhalla")
                out += if (viaValhalla) DiagnosticItem("محرك Valhalla (اختبار في صنعاء)", DiagStatus.OK, "مسار تجريبي ${r!!.totalDistanceMeters.toInt()} م (يعمل أوفلاين بنجاح)")
                else DiagnosticItem("محرك Valhalla (اختبار في صنعاء)", DiagStatus.FAIL, NativeValhallaStatus.lastError ?: "لم يُنتج مساراً (المصدر: ${r?.engineSource ?: "—"})")
            }

            // 3) الاتصال بخادم التوجيه
            val netOk = try {
                val hostUrl = NavConfig.OSRM_BASE_URL.substringBefore("/route")
                (URL(hostUrl).openConnection() as HttpURLConnection).run {
                    connectTimeout = 4000; readTimeout = 4000; requestMethod = "HEAD"
                    setRequestProperty("User-Agent", "AlYemenServicesApp/1.0")
                    val code = responseCode; disconnect(); code < 500
                }
            } catch (t: Throwable) { false }
            out += DiagnosticItem("الاتصال بخادم التوجيه", if (netOk) DiagStatus.OK else DiagStatus.WARN,
                if (netOk) "متاح" else "غير متاح (الأوفلاين وحده يعمل)")

            // 4) الموقع
            out += when {
                !hasRealLocation -> DiagnosticItem("الموقع", DiagStatus.FAIL, "لا يوجد موقع حقيقي حديث")
                accuracyM != null && accuracyM > 50f -> DiagnosticItem("الموقع", DiagStatus.WARN, "الدقة ضعيفة ≈ ${accuracyM.toInt()} م")
                else -> DiagnosticItem("الموقع", DiagStatus.OK, "دقة ≈ ${accuracyM?.toInt() ?: "؟"} م")
            }

            // 5) صوت عربي
            val (ttsOk, ttsEngine) = try {
                ArabicNavigationVoiceEngine.probeArabicSupport(context)
            } catch (_: Throwable) {
                false to null
            }
            out += when {
                ttsOk -> DiagnosticItem(
                    "الصوت العربي",
                    DiagStatus.OK,
                    "متوفر (${if (ttsEngine?.contains("google") == true) "Google TTS" else ttsEngine ?: "النظام"})"
                )
                ArabicNavigationVoiceEngine.isPackageInstalled(context, ArabicNavigationVoiceEngine.GOOGLE_TTS_PACKAGE) ->
                    DiagnosticItem("الصوت العربي", DiagStatus.WARN, "حزمة العربية غير منزّلة في Google TTS — تعمل نغمات التوجيه المدمجة بديلاً")
                else -> DiagnosticItem("الصوت العربي", DiagStatus.WARN, "لا توجد حزمة نطق عربي — تعمل نغمات التوجيه المدمجة بديلاً")
            }

            // 6) عرض الخريطة أوفلاين
            val mb = OfflineTileInterceptor.hasMbtiles(context)
            val cacheDir = OfflineMapManager.getTileCacheDir(context)
            val cacheFiles = cacheDir.walkTopDown().filter { it.isFile }.toList()
            val cacheMb = cacheFiles.sumOf { it.length() } / 1024.0 / 1024.0
            out += when {
                mb -> DiagnosticItem("بلاطات العرض الأوفلاين", DiagStatus.OK, "ملف MBTiles موجود")
                cacheFiles.isNotEmpty() -> DiagnosticItem("بلاطات العرض الأوفلاين", DiagStatus.WARN,
                    "كاش جزئي: ${cacheFiles.size} بلاطة (${String.format(Locale.US, "%.1f", cacheMb)} MB) — يعرض ما تصفحته فقط")
                else -> DiagnosticItem("بلاطات العرض الأوفلاين", DiagStatus.FAIL, "لا MBTiles ولا كاش: الخريطة تحتاج إنترنت")
            }
            out
        }

    fun toText(items: List<DiagnosticItem>): String =
        items.joinToString("\n") { "${it.status.icon} ${it.title}: ${it.detail}" }
}

@Composable
fun MapDiagnosticsDialog(
    hasRealLocation: Boolean,
    accuracyM: Float?,
    onTilesUpdated: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<DiagnosticItem>?>(null) }
    var isRepairing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var actionStatus by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        items = null
        items = MapDiagnostics.run(context, hasRealLocation, accuracyM)
    }
    val recentLogs = remember(refreshTrigger) { AppErrorLogManager.logs.value.take(5) }
    val valhallaFailed = items?.any { it.title.contains("Valhalla") && it.status == DiagStatus.FAIL } == true ||
        items?.any { it.title.contains("حزمة الملاحة") && it.status == DiagStatus.FAIL } == true

    AlertDialog(
        onDismissRequest = { if (!isDownloading && !isRepairing) onDismiss() },
        containerColor = Color(0xFF0F172A),
        title = { Text("فحص نظام الخرائط", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            val list = items
            if (list == null && !isDownloading) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF38BDF8))
                    Text("جاري فحص حزمة الأوفلاين ومحرك الملاحة...", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list?.forEach {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(it.status.icon, fontSize = 14.sp)
                            Column {
                                Text(it.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(it.detail, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    if (isDownloading) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                    actionStatus?.let { msg ->
                        Text(msg, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (!isDownloading && !isRepairing) {
                        Spacer(Modifier.height(4.dp))
                        if (valhallaFailed) {
                            Button(
                                onClick = {
                                    isRepairing = true
                                    actionStatus = "جاري إصلاح ملف إعدادات Valhalla..."
                                    scope.launch {
                                        val repaired = ValhallaTileDownloader.repairConfig(context)
                                        isRepairing = false
                                        actionStatus = if (repaired) "تم إصلاح الإعدادات — جاري إعادة الفحص..." else "تعذر الإصلاح، جرّب إعادة تنزيل الحزمة."
                                        onTilesUpdated()
                                        refreshTrigger++
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("diag_repair_valhalla_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("إصلاح إعدادات الأوفلاين وإعادة الفحص", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                isDownloading = true
                                downloadProgress = 0.02f
                                actionStatus = "جاري تنزيل حزمة خرائط اليمن (~26 MB)..."
                                scope.launch {
                                    val res = ValhallaTileDownloader.downloadAndExtractTiles(context) { p, status ->
                                        downloadProgress = p
                                        actionStatus = status
                                    }
                                    isDownloading = false
                                    if (res.isSuccess) {
                                        actionStatus = "✅ تم التحديث والتثبيت بنجاح"
                                        onTilesUpdated()
                                        refreshTrigger++
                                    } else {
                                        actionStatus = "❌ فشل التنزيل: ${res.exceptionOrNull()?.message ?: "تحقق من الإنترنت"}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("diag_redownload_valhalla_button")
                        ) {
                            Text("إعادة تنزيل الحزمة (~26 MB ← 65 MB)", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (recentLogs.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("سجل الأخطاء الأخير:", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        recentLogs.forEach { log ->
                            Text("• [${log.tag}] ${log.message}", color = Color(0xFFE2E8F0), fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isDownloading && !isRepairing,
                onClick = {
                    val reportText = buildString {
                        items?.let { append(MapDiagnostics.toText(it)) }
                        if (recentLogs.isNotEmpty()) {
                            append("\n\n--- Recent Error Logs ---\n")
                            recentLogs.forEach { log ->
                                append("[${log.tag}] ${log.message}\n")
                            }
                        }
                    }
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("map-diagnostics", reportText))
                }
            ) { Text("نسخ التقرير", color = Color(0xFF10B981)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isDownloading && !isRepairing,
                onClick = onDismiss
            ) { Text("إغلاق", color = Color(0xFF94A3B8)) }
        }
    )
}
