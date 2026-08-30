package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.BookingEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📊 BookingExportHelper
 * تصدير تقارير الحجوزات إلى ملفات CSV و PDF منسقة ومشاركتها عبر التطبيقات
 */
object BookingExportHelper {

    fun exportBookingsToCsv(context: Context, bookings: List<BookingEntity>): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "YemenServices_Bookings_$timeStamp.csv")

            FileWriter(file).use { writer ->
                // UTF-8 BOM for Excel Arabic compatibility
                writer.write("\uFEFF")
                // Headers
                writer.write("رقم الحجز,الخدمة,العميل,الهاتف,المزود,المدينة,التاريخ,الوقت,السعر,العملة,الحالة,رمز الأمان\n")

                bookings.forEach { b ->
                    val line = listOf(
                        "\"${b.bookingCode.ifBlank { b.id }}\"",
                        "\"${b.serviceName}\"",
                        "\"${b.userName}\"",
                        "\"${b.userPhone}\"",
                        "\"${b.providerName}\"",
                        "\"${b.userCity} - ${b.userNeighborhood}\"",
                        "\"${b.date}\"",
                        "\"${b.time}\"",
                        "\"${b.price}\"",
                        "\"${b.currency.ifBlank { "YER" }}\"",
                        "\"${b.status}\"",
                        "\"${b.secretPin}\""
                    ).joinToString(",")
                    writer.write(line + "\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportedFile(context: Context, file: File, title: String = "مشاركة تقرير الحجوزات") {
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
            // Fallback plain share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "تم حفظ تقرير الحجوزات: ${file.name}")
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}
