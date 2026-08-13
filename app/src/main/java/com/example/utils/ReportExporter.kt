package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object ReportExporter {

    fun exportToCSV(context: Context, reportName: String, dataRows: List<List<String>>): Boolean {
        try {
            val fileName = "${reportName}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            
            for (row in dataRows) {
                writer.append(row.joinToString(",") { "\"$it\"" })
                writer.append("\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "تصدير تقرير CSV")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في تصدير التقرير: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    fun exportToPDFReport(context: Context, reportTitle: String, summaryText: String): Boolean {
        try {
            val fileName = "${reportTitle}_${System.currentTimeMillis()}.txt"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            writer.append("=== تقرير منصة اليمن للخدمات والشركاء ===\n")
            writer.append("العنوان: $reportTitle\n")
            writer.append("تاريخ الإصدار: ${java.util.Date()}\n")
            writer.append("----------------------------------------\n")
            writer.append(summaryText)
            writer.flush()
            writer.close()

            shareFile(context, file, "text/plain", "تصدير تقرير نصي / PDF")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في تصدير تقرير PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
