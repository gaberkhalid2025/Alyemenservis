package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.ui.screens.map.utils.OfflineTileInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

enum class DiagStatus(val icon: String) { OK("✅"), FAIL("❌"), WARN("⚠️") }
data class DiagnosticItem(val title: String, val status: DiagStatus, val detail: String)

object MapDiagnostics {

    suspend fun run(context: Context, hasRealLocation: Boolean, accuracyM: Float?): List<DiagnosticItem> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<DiagnosticItem>()

            // 1) حزمة الملاحة
            val tilesOk = ValhallaTileDownloader.isTilesDownloaded(context)
            val tarFile = File(ValhallaTileDownloader.getValhallaDir(context), NavConfig.VALHALLA_TILES_FILE)
            out += if (tilesOk) DiagnosticItem("حزمة الملاحة الأوفلاين", DiagStatus.OK, "مثبّتة (${tarFile.length() / 1024 / 1024} MB)")
            else DiagnosticItem("حزمة الملاحة الأوفلاين", DiagStatus.FAIL, "غير مثبّتة أو تالفة")

            // 2) محرك Valhalla: مسار حقيقي صغير داخل صنعاء (يُقبل فقط إن جاء من Valhalla نفسه)
            if (tilesOk) {
                val origin = NavWaypoint(name = "أ", latitude = 15.3694, longitude = 44.1910, type = WaypointType.START)
                val dest = NavWaypoint(name = "ب", latitude = 15.3500, longitude = 44.2000, type = WaypointType.BREAK)
                val r = try { RealValhallaNavigator.calculateRoute(context, origin, listOf(dest)) } catch (t: Throwable) { null }
                val viaValhalla = r != null && r.error == null && r.coordinates.size >= 2 && r.engineSource.contains("Valhalla")
                out += if (viaValhalla) DiagnosticItem("محرك Valhalla", DiagStatus.OK, "مسار تجريبي ${r!!.totalDistanceMeters.toInt()} م")
                else DiagnosticItem("محرك Valhalla", DiagStatus.FAIL, NativeValhallaStatus.lastError ?: "لم يُنتج مساراً (المصدر: ${r?.engineSource ?: "—"})")
            }

            // 3) الاتصال بخادم التوجيه
            val netOk = try {
                (URL("https://router.project-osrm.org/").openConnection() as HttpURLConnection).run {
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
            val tts = arabicTtsAvailable(context)
            out += when (tts) {
                true -> DiagnosticItem("الصوت العربي", DiagStatus.OK, "متوفر")
                false -> DiagnosticItem("الصوت العربي", DiagStatus.FAIL, "بيانات الصوت العربي غير مثبتة")
                null -> DiagnosticItem("الصوت العربي", DiagStatus.WARN, "تعذر الفحص (مهلة)")
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

    private suspend fun arabicTtsAvailable(context: Context): Boolean? = withTimeoutOrNull(4000L) {
        suspendCancellableCoroutine<Boolean> { cont ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context) { status ->
                val r = if (status == TextToSpeech.SUCCESS) (engine?.setLanguage(Locale("ar")) ?: TextToSpeech.ERROR) else TextToSpeech.ERROR
                val ok = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED && r != TextToSpeech.ERROR
                engine?.shutdown()
                if (cont.isActive) cont.resume(ok)
            }
            cont.invokeOnCancellation { engine?.shutdown() }
        }
    }

    fun toText(items: List<DiagnosticItem>): String =
        items.joinToString("\n") { "${it.status.icon} ${it.title}: ${it.detail}" }
}

@Composable
fun MapDiagnosticsDialog(hasRealLocation: Boolean, accuracyM: Float?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<DiagnosticItem>?>(null) }
    LaunchedEffect(Unit) { items = MapDiagnostics.run(context, hasRealLocation, accuracyM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("فحص نظام الخرائط", color = Color.White) },
        text = {
            val list = items
            if (list == null) CircularProgressIndicator()
            else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(it.status.icon, fontSize = 14.sp)
                        Column {
                            Text(it.title, color = Color.White, fontSize = 13.sp)
                            Text(it.detail, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = items?.let { MapDiagnostics.toText(it) } ?: return@TextButton
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("map-diagnostics", text))
            }) { Text("نسخ التقرير", color = Color(0xFF10B981)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Color(0xFF94A3B8)) } }
    )
}
