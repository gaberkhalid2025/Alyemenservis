package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.models.InstantRequestEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📈 ReportExporter
 * وحدة تصدير التقارير المتعددة (الطلبات العاجلة، المعاملات المالية، الإحصائيات)
 * بصيغة CSV المتوافقة تماماً مع Excel باللغة العربية ومشاركتها فوراً.
 */
object ReportExporter {

    fun exportUrgentRequestsToCsv(context: Context, requests: List<InstantRequestEntity>): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "Urgent_Requests_$timeStamp.csv")

            FileWriter(file).use { writer ->
                writer.write("\uFEFF") // UTF-8 BOM
                writer.write("رمز الطلب,الخدمة,المدينة,الحي,الحالة,العميل,الهاتف,الوقت المتبقي (دقيقة),التاريخ\n")

                val now = System.currentTimeMillis()
                requests.forEach { req ->
                    val remainingMinutes = (((req.expiresAt - now) / 1000) / 60).coerceAtLeast(0)
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(req.createdAt))

                    val line = listOf(
                        "\"${req.requestCode}\"",
                        "\"${req.serviceTitle}\"",
                        "\"${req.userCity}\"",
                        "\"${req.userNeighborhood}\"",
                        "\"${req.status}\"",
                        "\"${req.userName}\"",
                        "\"${req.userPhone}\"",
                        "\"$remainingMinutes\"",
                        "\"$dateFormatted\""
                    ).joinToString(",")
                    writer.write(line + "\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportFinancialReportToCsv(
        context: Context,
        accountName: String,
        totalIncome: Double,
        currency: String,
        items: List<Pair<String, Double>>
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "Financial_Report_${accountName}_$timeStamp.csv")

            FileWriter(file).use { writer ->
                writer.write("\uFEFF")
                writer.write("تقرير المبيعات والنمو المالي للحساب: $accountName\n")
                writer.write("إجمالي الإيرادات: $totalIncome $currency\n\n")
                writer.write("البند / الخدمة,المبلغ ($currency)\n")

                items.forEach { (item, amount) ->
                    writer.write("\"$item\",\"$amount\"\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportedFile(context: Context, file: File, title: String = "مشاركة التقرير") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "تم تصدير التقرير: ${file.name}")
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}
