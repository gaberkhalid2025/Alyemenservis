package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.models.InstantRequestEntity
import com.example.data.models.Transaction
import com.example.utils.DateFormatter
import java.io.File
import java.io.FileWriter
import java.util.Locale

/**
 * 📈 ReportExporter
 * وحدة تصدير التقارير المتعددة (الطلبات العاجلة، المعاملات المالية، الإحصائيات)
 * بصيغة CSV المتوافقة تماماً مع Excel باللغة العربية ومشاركتها فوراً.
 */
object ReportExporter {

    private fun csvField(v: Any?): String {
        var s = v?.toString().orEmpty().replace("\"", "\"\"")
        if (s.isNotEmpty() && s[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            s = "'$s"
        }
        return "\"$s\""
    }

    private fun cleanOldExports(exportDir: File) {
        try {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            exportDir.listFiles()?.filter { it.isFile && it.lastModified() < oneDayAgo }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun exportUrgentRequestsToCsv(context: Context, requests: List<InstantRequestEntity>): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            cleanOldExports(exportDir)

            val timeStamp = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
            val file = File(exportDir, "Urgent_Requests_$timeStamp.csv")

            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF") // UTF-8 BOM
                writer.write(listOf(
                    csvField("رمز الطلب"), csvField("الخدمة"), csvField("المدينة"),
                    csvField("الحي"), csvField("الحالة"), csvField("العميل"),
                    csvField("الهاتف"), csvField("الوقت المتبقي (دقيقة)"), csvField("التاريخ")
                ).joinToString(",") + "\n")

                val now = System.currentTimeMillis()
                requests.forEach { req ->
                    val remainingMinutes = (((req.expiresAt - now) / 1000) / 60).coerceAtLeast(0)
                    val dateFormatted = DateFormatter.formatDateTime(req.createdAt)

                    val line = listOf(
                        csvField(req.requestCode),
                        csvField(req.serviceTitle),
                        csvField(req.userCity),
                        csvField(req.userNeighborhood),
                        csvField(req.status),
                        csvField(req.userName),
                        csvField(req.userPhone),
                        csvField(remainingMinutes),
                        csvField(dateFormatted)
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
            cleanOldExports(exportDir)

            val timeStamp = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
            val file = File(exportDir, "Financial_Report_${accountName}_$timeStamp.csv")

            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF") // UTF-8 BOM
                writer.write(csvField("تقرير المبيعات والنمو المالي للحساب: $accountName") + "\n")
                writer.write(csvField("إجمالي الإيرادات: $totalIncome $currency") + "\n\n")
                writer.write(listOf(csvField("البند / الخدمة"), csvField("المبلغ ($currency)")).joinToString(",") + "\n")

                items.forEach { (item, amount) ->
                    writer.write(listOf(csvField(item), csvField(amount)).joinToString(",") + "\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportTransactionsToCsv(
        context: Context,
        accountName: String,
        transactions: List<Transaction>
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            cleanOldExports(exportDir)

            val timeStamp = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
            val file = File(exportDir, "Transactions_${accountName}_$timeStamp.csv")

            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF") // UTF-8 BOM
                writer.write(csvField("سجل العمليات المالية للحساب: $accountName") + "\n")
                writer.write(listOf(
                    csvField("رمز العملية"), csvField("النوع"), csvField("المبلغ"),
                    csvField("العملة"), csvField("الحالة"), csvField("الملاحظات"),
                    csvField("التاريخ")
                ).joinToString(",") + "\n")

                transactions.forEach { tx ->
                    val dateFormatted = DateFormatter.formatDateTime(tx.timestamp)
                    val typeAr = when (tx.type.uppercase()) {
                        "DEPOSIT" -> "إيداع"
                        "WITHDRAWAL" -> "سحب"
                        "TRANSFER" -> "تحويل"
                        "PAYMENT" -> "دفع"
                        "REFUND" -> "استرداد"
                        else -> tx.type
                    }
                    val statusAr = when (tx.status.uppercase()) {
                        "COMPLETED" -> "مكتملة"
                        "PENDING" -> "قيد الانتظار"
                        "FAILED" -> "فشلت"
                        "CANCELLED" -> "ملغاة"
                        else -> tx.status
                    }
                    val line = listOf(
                        csvField(tx.id),
                        csvField(typeAr),
                        csvField(tx.amount),
                        csvField(tx.currency),
                        csvField(statusAr),
                        csvField(tx.note),
                        csvField(dateFormatted)
                    ).joinToString(",")
                    writer.write(line + "\n")
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
