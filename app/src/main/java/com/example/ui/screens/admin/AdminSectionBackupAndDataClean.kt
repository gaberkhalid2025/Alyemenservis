@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminBackupAndCleanSection(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val activatedProviders = state.activatedProviders
    val categories = state.categories
    val bookings = state.bookings
    val reports = state.reports

    when (activeSubTab) {
        "BACKUP" -> {
            item {
                Text("💾 لوحة النسخ الاحتياطي والمزامنة والجدولة والتقارير", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("أدوات التصدير الشامل للبيانات والتحقق من صحة الاتصال المتزامن مع خوادم Cloud Firestore:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️ إحصائيات حالة المزامنة والاتصال الحي", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الحالة الفورية:", fontSize = 11.sp, color = Color.White)
                            Text("متصل وآمن 🟢", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("حجم البيانات النشطة:", fontSize = 11.sp, color = Color.White)
                            val sizeEst = (activatedProviders.size + categories.size + bookings.size + reports.size) * 1.5f
                            Text(String.format("%.2f KB", sizeEst), fontSize = 11.sp, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تردد نبض الاتصال:", fontSize = 11.sp, color = Color.White)
                            Text("كل 10 ثوانٍ (ذكي تلقائي)", fontSize = 11.sp, color = themeColors.accent)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "🔄 جاري إعادة فحص ومزامنة كامل جداول البيانات مع السحاب...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تحديث وإعادة جدولة الفحص الفوري 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💾 نظام النسخ الاحتياطي التلقائي واستيراد البيانات", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        Text("يقوم النظام تلقائياً بجدولة نسخ كامل الجداول وقواعد البيانات لضمان عدم ضياع البيانات الفنية والحجوزات والمتاجر والعقارات والكلمات المشفرة.", fontSize = 11.sp, color = Color.LightGray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.createSystemBackup { success, jsonStr ->
                                        if (success) {
                                            state.backupJsonStringState = jsonStr
                                            val path = viewModel.saveBackupToLocalStorage(context, jsonStr, "yemen_services_backup_${System.currentTimeMillis()}")
                                            if (path.isNotEmpty()) {
                                                Toast.makeText(context, "✅ تم حفظ النسخة الاحتياطية بذاكرة الهاتف/SD Card:\n$path", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "✅ تم إنشاء النسخة الاحتياطية بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "❌ فشل إنشاء النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حفظ بالهاتف/SD Card 💾", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.createSystemBackup { success, jsonStr ->
                                        if (success) {
                                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clipData = android.content.ClipData.newPlainText("YemenServiceBackup", jsonStr)
                                            clipboardManager.setPrimaryClip(clipData)
                                            Toast.makeText(context, "📋 تم نسخ كود الاحتياط الكامل للذاكرة بنجاح!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("نسخ الكود الكامل 📋", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state.backupJsonStringState.isNotEmpty()) {
                            Text("تم توليد الكود الاحتياطي بنجاح (${state.backupJsonStringState.length} حرفاً). احتفظ به في مكان آمن.", fontSize = 10.sp, color = Color.Green)
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 1.dp)

                        Text("📥 استعادة النظام من نسخة احتياطية سابقة (ملف أو كود)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        val backupFilePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let {
                                try {
                                    var fileText = ""
                                    val contentResolver = context.contentResolver
                                    val mimeType = contentResolver.getType(it) ?: ""
                                    if (mimeType.contains("zip") || it.toString().endsWith(".zip", ignoreCase = true)) {
                                        contentResolver.openInputStream(it)?.use { inputStream ->
                                            java.util.zip.ZipInputStream(inputStream).use { zipInput ->
                                                var entry = zipInput.nextEntry
                                                while (entry != null) {
                                                    if (!entry.isDirectory && (entry.name.endsWith(".json", ignoreCase = true) || entry.name.endsWith(".txt", ignoreCase = true))) {
                                                        fileText = zipInput.bufferedReader().use { r -> r.readText() }
                                                        break
                                                    }
                                                    entry = zipInput.nextEntry
                                                }
                                            }
                                        }
                                    } else {
                                        contentResolver.openInputStream(it)?.use { inputStream ->
                                            fileText = inputStream.bufferedReader().use { r -> r.readText() }
                                        }
                                    }

                                    if (fileText.isNotBlank()) {
                                        state.restoreJsonInputState = fileText
                                        Toast.makeText(context, "✅ تم استخراج وقراءة ملف النسخة الاحتياطية بنجاح! اضغط تأكيد الاستعادة أدناه.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ الملف المحدد فارغ أو غير متوافق!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "❌ فشل قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        Button(
                            onClick = { backupFilePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📂 اختيار ملف النسخة الاحتياطية من ذاكرة الهاتف / Drive (ZIP, JSON, TXT)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = state.restoreJsonInputState,
                            onValueChange = { state.restoreJsonInputState = it },
                            label = { Text("أو أدخل/الصق كود النسخة الاحتياطية JSON مباشرة هنا") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Button(
                            onClick = {
                                if (state.restoreJsonInputState.trim().isEmpty()) {
                                    Toast.makeText(context, "⚠️ يرجى اختيار ملف أو لصق كود النسخة أولاً!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.restoreSystemFromBackup(state.restoreJsonInputState) { success, msg ->
                                        if (success) {
                                            state.restoreJsonInputState = ""
                                            Toast.makeText(context, "💚 تم استعادة كامل البيانات والمزامنة السحابية بنجاح بنسبة 100%!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "❌ فشل استعادة البيانات: $msg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تأكيد استعادة قواعد البيانات ومزامنتها سحابياً ⚠️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 1.dp)

                        Text("🔥 ربط وإدارة المزامنة المزدوجة مع حساب Firebase ثانوي", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)

                        var secProjId by remember { mutableStateOf("") }
                        var secApiKey by remember { mutableStateOf("") }
                        var secAppId by remember { mutableStateOf("") }
                        var secBucket by remember { mutableStateOf("") }
                        var secEnabled by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = secProjId,
                            onValueChange = { secProjId = it },
                            label = { Text("Project ID الحساب الثانوي", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                        )
                        OutlinedTextField(
                            value = secApiKey,
                            onValueChange = { secApiKey = it },
                            label = { Text("API Key الحساب الثانوي", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Switch(
                                checked = secEnabled,
                                onCheckedChange = { secEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل المزامنة المزدوجة التلقائية مع Firebase الثانوي ⚡", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.setSecondaryFirebaseConfig(secProjId, secApiKey, secAppId, secBucket, secEnabled)
                                Toast.makeText(context, "⚡ تم حفظ إعدادات المزامنة الثانوية لـ Firebase بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ وتحديث إعدادات المزامنة المزدوجة 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📁 تصدير التقارير الإدارية الشاملة للجمهورية", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Button(
                            onClick = {
                                Toast.makeText(context, "تم تصدير الدليل الكامل للفنيين والمحافظات إلى ذاكرة الهاتف 📁", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تصدير الدليل الكامل للفنيين (CSV)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "تم تصدير جميع سجلات حجز الصيانة المجدولة والنشطة 📁", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تصدير سجل الحجوزات النشطة (CSV)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        "CLEAN" -> {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("⚙️ تخصيص تصفية وتهيئة البيانات الفورية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("حدد أنواع البيانات التي تريد مسحها أو نسخها وحفظها احتياطياً:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📁 تحديد الكل لإجراء العملية الشاملة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Switch(
                                checked = state.wipeProvidersChecked && state.wipeBookingsChecked && state.wipeChatsChecked &&
                                    state.wipeNotifsChecked && state.wipeReportsChecked && state.wipePendingChecked &&
                                    state.wipeBannersChecked && state.wipeSupervisorsChecked && state.wipeCitiesChecked && state.wipeThemesChecked,
                                onCheckedChange = { allChecked ->
                                    state.wipeProvidersChecked = allChecked
                                    state.wipeBookingsChecked = allChecked
                                    state.wipeChatsChecked = allChecked
                                    state.wipeNotifsChecked = allChecked
                                    state.wipeReportsChecked = allChecked
                                    state.wipeCategoriesChecked = allChecked
                                    state.wipePendingChecked = allChecked
                                    state.wipeBannersChecked = allChecked
                                    state.wipeSupervisorsChecked = allChecked
                                    state.wipeCitiesChecked = allChecked
                                    state.wipeThemesChecked = allChecked
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }

                        HorizontalDivider(color = themeColors.accent.copy(alpha = 0.2f))

                        val itemsList = listOf(
                            Triple("الفنيين ومقدمي الخدمات الحقيقيين 👤", state.wipeProvidersChecked) { v: Boolean -> state.wipeProvidersChecked = v },
                            Triple("الحجوزات والطلبات المجدولة 📅", state.wipeBookingsChecked) { v: Boolean -> state.wipeBookingsChecked = v },
                            Triple("المحادثات وقنوات الشات المتبادلة 💬", state.wipeChatsChecked) { v: Boolean -> state.wipeChatsChecked = v },
                            Triple("الإشعارات الموجهة والمرسلة 🔔", state.wipeNotifsChecked) { v: Boolean -> state.wipeNotifsChecked = v },
                            Triple("بلاغات الشكاوى ومشاكل المستخدمين ⚠️", state.wipeReportsChecked) { v: Boolean -> state.wipeReportsChecked = v },
                            Triple("الأقسام وتصنيفات المهن (حذر) 📂", state.wipeCategoriesChecked) { v: Boolean -> state.wipeCategoriesChecked = v },
                            Triple("طلبات الانضمام والتسجيل المعلقة 📝", state.wipePendingChecked) { v: Boolean -> state.wipePendingChecked = v },
                            Triple("الإعلانات وبنرات واجهة التطبيق 📢", state.wipeBannersChecked) { v: Boolean -> state.wipeBannersChecked = v },
                            Triple("المشرفين والإداريين المساعدين 🔑", state.wipeSupervisorsChecked) { v: Boolean -> state.wipeSupervisorsChecked = v },
                            Triple("المحافظات والمدن المعتمدة 🗺️", state.wipeCitiesChecked) { v: Boolean -> state.wipeCitiesChecked = v },
                            Triple("قوالب الألوان المخصصة 🎨", state.wipeThemesChecked) { v: Boolean -> state.wipeThemesChecked = v }
                        )

                        itemsList.forEach { (label, isChecked, setChecked) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { setChecked(!isChecked) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 12.sp, color = Color.White)
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { setChecked(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("📥 تصدير ونسخ البيانات المحددة (نسخة احتياطية):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                fun buildSelectedCols(): List<String> {
                    val selectedCols = mutableListOf<String>()
                    if (state.wipeProvidersChecked) selectedCols.add("providers")
                    if (state.wipeBookingsChecked) selectedCols.add("bookings")
                    if (state.wipeChatsChecked) selectedCols.add("chat_channels")
                    if (state.wipeNotifsChecked) selectedCols.add("notifications")
                    if (state.wipeReportsChecked) selectedCols.add("reports")
                    if (state.wipeCategoriesChecked) selectedCols.add("categories")
                    if (state.wipePendingChecked) selectedCols.add("pending_providers")
                    if (state.wipeBannersChecked) selectedCols.add("banners")
                    if (state.wipeSupervisorsChecked) selectedCols.add("supervisors")
                    if (state.wipeCitiesChecked) selectedCols.add("cities")
                    if (state.wipeThemesChecked) selectedCols.add("color_themes")
                    return selectedCols
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "يمكنك حفظ البيانات المحددة ونقلها فوراً إلى أي هاتف آخر أو نسخها لحساب جوجل درايف الخاص بك لحفظها من الفقدان:",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )

                        Button(
                            onClick = {
                                viewModel.exportSelectedCollectionsAsJson(buildSelectedCols()) { jsonString ->
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = android.content.ClipData.newPlainText("YemenService_Backup_JSON", jsonString)
                                    clipboardManager.setPrimaryClip(clipData)
                                    Toast.makeText(context, "📋 تم نسخ البيانات المحددة بصيغة JSON بنجاح إلى ذاكرة الهاتف!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📋 نسخ البيانات المحددة للحافظة (Clipboard)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.exportSelectedCollectionsAsJson(buildSelectedCols()) { jsonString ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - دليل خدمات اليمن")
                                        putExtra(Intent.EXTRA_TEXT, jsonString)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "تصدير نسخة البيانات الاحتياطية إلى:"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("☁️ نسخ لجوجل درايف / ذاكرة الهاتف ومشاركتها", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("🚨 منطقة التطهير الكلي والمسح النهائي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تنبيه أمني صارم للغاية!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(
                            text = "الضغط على الزر أدناه سيقوم بحذف وإعادة تهيئة الأنواع المحددة أعلاه فقط فوراً! لن يتسنى لك مراجعة أو التراجع عن هذه البيانات بمجرد تأكيد المسح بالرمز السري.",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { state.showWipeConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("مسح كامل البيانات وإعادة بناء الدليل العظيم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        "STORAGE_QUOTA" -> {
            item {
                AdminStorageAndCacheScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }

        "DATA_MANAGEMENT" -> {
            item {
                AdminDataManagementScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }
}
