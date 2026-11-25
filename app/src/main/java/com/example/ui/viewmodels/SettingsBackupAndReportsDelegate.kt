package com.example.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.example.utils.DateFormatter
import com.example.utils.ReportExporter
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal class SettingsBackupAndReportsDelegate(private val vm: SettingsViewModel) {

    fun exportComplaintsToCSV() {
        val ctx = vm.appContext
        val reportsList = vm.appState.reports.value
        if (ctx == null) {
            vm.mainViewModel.triggerNotification("❌ تعذر الوصول إلى سياق التطبيق للتصدير")
            return
        }
        vm.viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                val ts = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
                val file = File(exportDir, "Complaints_Report_$ts.csv")
                fun esc(v: Any?): String {
                    var s = v?.toString().orEmpty().replace("\"", "\"\"")
                    if (s.isNotEmpty() && s[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) s = "'$s"
                    return "\"$s\""
                }
                file.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(listOf(esc("المعرف"), esc("المرسل"), esc("الهاتف"), esc("النوع"), esc("التفاصيل"), esc("الحالة"), esc("التاريخ")).joinToString(",") + "\n")
                    reportsList.forEach { r ->
                        writer.write(
                            listOf(
                                esc(r.id), esc(r.reporterName), esc(r.reporterPhone),
                                esc(r.targetType.ifBlank { r.reason }),
                                esc(r.explanation.ifBlank { r.content }.ifBlank { r.reason }),
                                esc(r.status),
                                esc(DateFormatter.formatDateTime(r.timestamp))
                            ).joinToString(",") + "\n"
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    ReportExporter.shareExportedFile(ctx, file, "تقرير البلاغات والشكاوى (CSV)")
                    vm.mainViewModel.triggerNotification("📁 تم تصدير ${reportsList.size} بلاغاً بصيغة CSV")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    vm.mainViewModel.triggerNotification("❌ فشل تصدير ملف CSV: ${e.localizedMessage ?: "خطأ"}")
                }
            }
        }
    }

    fun exportComplaintsToPDF() {
        val ctx = vm.appContext
        val reportsList = vm.appState.reports.value
        if (ctx == null) {
            vm.mainViewModel.triggerNotification("❌ تعذر الوصول إلى سياق التطبيق للتصدير")
            return
        }
        vm.viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                val ts = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
                val file = File(exportDir, "Complaints_Report_$ts.pdf")
                val pdfDoc = PdfDocument()
                val titlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = 15f
                    isFakeBoldText = true
                    color = Color.BLACK
                }
                val bodyPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = 11f
                    color = Color.DKGRAY
                }
                val contentWidth = 515
                var pageNumber = 1
                var page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                var y = 44f

                fun drawRtlBlock(canvas: android.graphics.Canvas, text: String, paint: TextPaint, topY: Float): Float {
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.RTL)
                        .setLineSpacing(2f, 1.15f)
                        .build()
                    canvas.save()
                    canvas.translate(40f, topY)
                    layout.draw(canvas)
                    canvas.restore()
                    return layout.height.toFloat()
                }

                y += drawRtlBlock(page.canvas, "تقرير البلاغات والشكاوى - صفحة $pageNumber", titlePaint, y) + 14f
                if (reportsList.isEmpty()) {
                    drawRtlBlock(page.canvas, "لا توجد بلاغات مسجلة حالياً.", bodyPaint, y)
                } else {
                    reportsList.forEachIndexed { idx, r ->
                        val detailsText = r.explanation.ifBlank { r.content }.ifBlank { r.reason }.take(120)
                        val typeLabel = r.targetType.ifBlank { r.reason }
                        val line = "${idx + 1}. [${r.status}] $typeLabel - ${r.reporterName} (${r.reporterPhone}): $detailsText"
                        val blockLayout = StaticLayout.Builder.obtain(line, 0, line.length, bodyPaint, contentWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setTextDirection(TextDirectionHeuristics.RTL)
                            .setLineSpacing(2f, 1.15f)
                            .build()
                        if (y + blockLayout.height > 790f) {
                            pdfDoc.finishPage(page)
                            pageNumber++
                            page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                            y = 44f
                            y += drawRtlBlock(page.canvas, "تقرير البلاغات والشكاوى - صفحة $pageNumber", titlePaint, y) + 14f
                        }
                        page.canvas.save()
                        page.canvas.translate(40f, y)
                        blockLayout.draw(page.canvas)
                        page.canvas.restore()
                        y += blockLayout.height + 10f
                    }
                }
                pdfDoc.finishPage(page)
                file.outputStream().use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تقرير البلاغات والشكاوى (PDF)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "مشاركة تقرير البلاغات PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    vm.mainViewModel.triggerNotification("📃 تم إنشاء ومشاركة تقرير البلاغات PDF")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    vm.mainViewModel.triggerNotification("❌ فشل تصدير PDF: ${e.localizedMessage ?: "خطأ"}")
                }
            }
        }
    }

    fun exportPerformanceReportToPDF() {
        val ctx = vm.appContext
        if (ctx == null) {
            vm.mainViewModel.triggerNotification("❌ تعذر الوصول إلى سياق التطبيق للتصدير")
            return
        }
        val provCount = vm.mainViewModel.homeViewModel._providers.value.size
        val storeCount = vm.mainViewModel.adminViewModel._stores.value.size
        val bookingList = vm.mainViewModel.bookingViewModel._bookings.value
        val completedCount = bookingList.count { it.status == "COMPLETED" }
        vm.viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                val ts = DateFormatter.formatCustom(System.currentTimeMillis(), "yyyyMMdd_HHmmss")
                val file = File(exportDir, "Performance_Report_$ts.pdf")
                val pdfDoc = PdfDocument()
                val titlePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = 16f
                    isFakeBoldText = true
                    color = Color.BLACK
                }
                val bodyPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = 12f
                    color = Color.DKGRAY
                }
                val page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                val canvas = page.canvas
                var y = 48f
                fun drawRtlLine(text: String, paint: TextPaint, gap: Float) {
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 515)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.RTL)
                        .setLineSpacing(2f, 1.2f)
                        .build()
                    canvas.save()
                    canvas.translate(40f, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height + gap
                }
                drawRtlLine("تقرير أداء شبكة الفنيين والمنصة - دليل خدمات اليمن", titlePaint, 16f)
                drawRtlLine("التاريخ: ${DateFormatter.formatDateTime(System.currentTimeMillis())}", bodyPaint, 10f)
                drawRtlLine("إجمالي مقدمي الخدمات: $provCount", bodyPaint, 10f)
                drawRtlLine("إجمالي المتاجر والمنشآت: $storeCount", bodyPaint, 10f)
                drawRtlLine("إجمالي الحجوزات: ${bookingList.size} | المكتملة: $completedCount", bodyPaint, 10f)
                pdfDoc.finishPage(page)
                file.outputStream().use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "تقرير أداء المنصة PDF")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "مشاركة تقرير الأداء PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    vm.mainViewModel.triggerNotification("📊 تم إنشاء ومشاركة تقرير أداء المنصة PDF")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    vm.mainViewModel.triggerNotification("❌ فشل تصدير تقرير الأداء: ${e.localizedMessage ?: "خطأ"}")
                }
            }
        }
    }

    fun createSystemBackup(onComplete: (Boolean, String) -> Unit) {
        try {
            val root = JSONObject()

            val provArray = JSONArray()
            vm.mainViewModel.homeViewModel._providers.value.forEach { prov ->
                val obj = JSONObject()
                obj.put("id", prov.id)
                obj.put("name", prov.name)
                obj.put("phone", prov.phone)
                obj.put("customCategoryName", prov.customCategoryName)
                obj.put("cityId", prov.cityId)
                obj.put("localNeighborhood", prov.localNeighborhood)
                obj.put("isAvailable", prov.isAvailable)
                obj.put("rating", prov.rating.toDouble())
                obj.put("numReviews", prov.numReviews)
                provArray.put(obj)
            }
            root.put("providers", provArray)

            val bookArray = JSONArray()
            vm.mainViewModel.bookingViewModel._bookings.value.forEach { b ->
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("customerPhone", b.customerPhone)
                obj.put("customerName", b.customerName)
                obj.put("customerArea", b.customerArea)
                obj.put("providerId", b.providerId)
                obj.put("providerName", b.providerName)
                obj.put("dateString", b.dateString)
                obj.put("timeString", b.timeString)
                obj.put("serviceType", b.serviceType)
                obj.put("status", b.status)
                bookArray.put(obj)
            }
            root.put("bookings", bookArray)

            val catArray = JSONArray()
            vm.mainViewModel.homeViewModel._categories.value.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("icon", c.icon)
                obj.put("parentId", c.parentId)
                obj.put("isMainCategory", c.isMainCategory)
                catArray.put(obj)
            }
            root.put("categories", catArray)

            val storeArray = JSONArray()
            vm.mainViewModel.adminViewModel._stores.value.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("description", s.description)
                obj.put("phone", s.phone)
                obj.put("ownerName", s.ownerName)
                obj.put("cityId", s.cityId)
                obj.put("localNeighborhood", s.localNeighborhood)
                obj.put("isActive", s.isActive)
                storeArray.put(obj)
            }
            root.put("stores", storeArray)

            val propArray = JSONArray()
            vm.mainViewModel.adminViewModel._properties.value.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("title", p.title)
                obj.put("description", p.description)
                obj.put("price", p.price)
                obj.put("phone", p.phone)
                obj.put("cityId", p.cityId)
                obj.put("ownerName", p.ownerName)
                obj.put("isActive", p.isActive)
                propArray.put(obj)
            }
            root.put("properties", propArray)

            val jsonStr = root.toString(2)

            val backupId = "backup_" + System.currentTimeMillis()
            val backupData = hashMapOf(
                "id" to backupId,
                "timestamp" to Timestamp.now(),
                "data" to jsonStr,
                "summary" to "Providers: ${vm.mainViewModel._providers.value.size}, Bookings: ${vm.mainViewModel._bookings.value.size}, Categories: ${vm.mainViewModel._categories.value.size}, Stores: ${vm.mainViewModel._stores.value.size}"
            )
            vm.db.collection("database_backups").document(backupId).set(backupData)
                .addOnSuccessListener {
                    vm.mainViewModel.triggerNotification("💾 تم إنشاء النسخة الاحتياطية الدورية السحابية الأسبوعية وحفظها بنجاح!")
                    onComplete(true, jsonStr)
                }
                .addOnFailureListener {
                    onComplete(true, jsonStr)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.message ?: "Unknown error")
        }
    }

    fun restoreSystemFromBackup(jsonStr: String, onComplete: (Boolean, String) -> Unit) {
        try {
            val root = JSONObject(jsonStr)

            if (root.has("providers")) {
                val array = root.getJSONArray("providers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val phone = obj.optString("phone", id)
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "phone" to phone,
                        "customCategoryName" to obj.optString("customCategoryName", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "localNeighborhood" to obj.optString("localNeighborhood", ""),
                        "isAvailable" to obj.optBoolean("isAvailable", true),
                        "rating" to obj.optDouble("rating", 4.5).toFloat(),
                        "numReviews" to obj.optInt("numReviews", 1)
                    )
                    vm.db.collection("providers").document(id).set(data)
                }
            }

            if (root.has("bookings")) {
                val array = root.getJSONArray("bookings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "customerPhone" to obj.optString("customerPhone", ""),
                        "customerName" to obj.optString("customerName", ""),
                        "customerArea" to obj.optString("customerArea", ""),
                        "providerId" to obj.optString("providerId", ""),
                        "providerName" to obj.optString("providerName", ""),
                        "dateString" to obj.optString("dateString", ""),
                        "timeString" to obj.optString("timeString", ""),
                        "serviceType" to obj.optString("serviceType", ""),
                        "status" to obj.optString("status", "PENDING")
                    )
                    vm.db.collection("bookings").document(id).set(data)
                }
            }

            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "icon" to obj.optString("icon", ""),
                        "parentId" to obj.optString("parentId", ""),
                        "isMainCategory" to obj.optBoolean("isMainCategory", true)
                    )
                    vm.db.collection("categories").document(id).set(data)
                }
            }

            if (root.has("stores")) {
                val array = root.getJSONArray("stores")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "name" to obj.optString("name", ""),
                        "description" to obj.optString("description", ""),
                        "phone" to obj.optString("phone", ""),
                        "ownerName" to obj.optString("ownerName", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "localNeighborhood" to obj.optString("localNeighborhood", ""),
                        "isActive" to obj.optBoolean("isActive", true)
                    )
                    vm.db.collection("stores").document(id).set(data)
                }
            }

            if (root.has("properties")) {
                val array = root.getJSONArray("properties")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val data = hashMapOf(
                        "id" to id,
                        "title" to obj.optString("title", ""),
                        "description" to obj.optString("description", ""),
                        "price" to obj.optDouble("price", 0.0),
                        "phone" to obj.optString("phone", ""),
                        "cityId" to obj.optString("cityId", ""),
                        "ownerName" to obj.optString("ownerName", ""),
                        "isActive" to obj.optBoolean("isActive", true)
                    )
                    vm.db.collection("properties").document(id).set(data)
                }
            }

            vm.mainViewModel.triggerNotification("💚 تم استعادة قاعدة البيانات الشاملة بنجاح ومزامنتها سحابياً!")
            onComplete(true, "Success")
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, e.message ?: "Unknown parsing error")
        }
    }

    fun exportSelectedCollectionsAsJson(selectedCollections: List<String>, onResult: (String) -> Unit) {
        vm.viewModelScope.launch {
            val rootJson = JSONObject()
            if (selectedCollections.isEmpty()) {
                onResult("{}")
                return@launch
            }
            var completedCount = 0
            selectedCollections.forEach { col ->
                vm.db.collection(col).limit(500).get().addOnSuccessListener { snapshot ->
                    val arr = JSONArray()
                    snapshot?.documents?.forEach { doc ->
                        val obj = JSONObject()
                        doc.data?.forEach { (k, v) ->
                            if (v != null) {
                                obj.put(k, v.toString())
                            }
                        }
                        obj.put("id", doc.id)
                        arr.put(obj)
                    }
                    rootJson.put(col, arr)
                    completedCount++
                    if (completedCount == selectedCollections.size) {
                        onResult(rootJson.toString(4))
                    }
                }.addOnFailureListener {
                    completedCount++
                    if (completedCount == selectedCollections.size) {
                        onResult(rootJson.toString(4))
                    }
                }
            }
        }
    }

    fun saveBackupToLocalStorage(context: Context, jsonStr: String, fileName: String): String {
        val safeName = if (fileName.endsWith(".json")) fileName else "$fileName.json"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/YemenServicesBackups")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    }
                    return "${Environment.DIRECTORY_DOWNLOADS}/YemenServicesBackups/$safeName"
                }
            } catch (_: Exception) {
            }
        }
        return try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val backupFolder = File(baseDir, "YemenServicesBackups").apply { mkdirs() }
            val targetFile = File(backupFolder, safeName)
            targetFile.writeText(jsonStr, Charsets.UTF_8)
            targetFile.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    fun setSecondaryFirebaseConfig(
        projectId: String,
        apiKey: String,
        appId: String,
        storageBucket: String,
        isEnabled: Boolean
    ) {
        val map = mapOf(
            "secondary_projectId" to projectId,
            "secondary_apiKey" to apiKey,
            "secondary_appId" to appId,
            "secondary_storageBucket" to storageBucket,
            "secondary_enabled" to isEnabled
        )
        vm.db.collection("admin_settings").document("secondary_firebase").set(map)
        vm.mainViewModel.triggerNotification(
            if (isEnabled) "🟢 تم تفعيل المزامنة المزدوجة مع حساب Firebase الثانوي بنجاح!" else "🔴 تم إيقاف المزامنة مع الحساب الثانوي"
        )
    }
}
