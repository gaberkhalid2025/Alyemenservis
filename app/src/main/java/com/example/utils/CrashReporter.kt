package com.example.utils

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Date

/**
 * 🛠️ CrashReporter
 * تسجيل أي استثناء غير ملتقط لسهولة التشخيص والمراقبة بدون Logcat
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(app.filesDir, FILE).writeText(
                    "${Date()}\nthread=${thread.name}\n${android.util.Log.getStackTraceString(throwable)}"
                )
            } catch (_: Throwable) {}
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) { runCatching { File(context.filesDir, FILE).delete() } }
}

/**
 * نافذة حوارية تظهر عند وجود تقرير انهيار سابق مع زر نسخ فوري
 */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.read(context)) }
    val text = report ?: return

    AlertDialog(
        onDismissRequest = { },
        containerColor = Color(0xFF0F172A),
        title = { Text("تقرير آخر انهيار", color = Color.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text.take(4000), color = Color(0xFFCBD5E1), fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", text))
            }) { Text("نسخ", color = Color(0xFF10B981)) }
        },
        dismissButton = {
            TextButton(onClick = { CrashReporter.clear(context); report = null }) { Text("حذف", color = Color(0xFFEF4444)) }
        }
    )
}
