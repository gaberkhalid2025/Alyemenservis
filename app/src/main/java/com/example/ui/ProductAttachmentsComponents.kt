package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VisualThemePalette
import com.example.data.ProductAttachment
import java.util.UUID

@Composable
fun ProductAttachmentsSection(
    attachments: List<ProductAttachment>,
    onAttachmentsChanged: (List<ProductAttachment>) -> Unit,
    mode: String, // "REGISTRATION", "MANAGEMENT", "VISITOR_VIEW"
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("PDF") }
    var customFileName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = customFileName.ifBlank { "attachment_${System.currentTimeMillis()}.${selectedType.lowercase()}" }
            val newAtt = ProductAttachment(
                id = UUID.randomUUID().toString(),
                type = selectedType,
                url = uri.toString(),
                fileName = name,
                size = 1024 * 750L,
                mimeType = when (selectedType) {
                    "PDF" -> "application/pdf"
                    "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "CSV" -> "text/csv"
                    "IMAGE" -> "image/jpeg"
                    "JSON" -> "application/json"
                    else -> "*/*"
                }
            )
            onAttachmentsChanged(attachments + newAtt)
            showAddDialog = false
            Toast.makeText(context, "تم رفع وتحديث الملف بنجاح 📁", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (mode) {
                "REGISTRATION" -> {
                    Text(
                        "📁 مرفقات المنتجات/الخدمات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text(
                        "اختر صيغة ملف لمنتجاتك/خدماتك. سيظهر هذا الملف في ملفك الشخصي بعد الموافقة.",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )

                    // Formats Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصيغة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("الاستخدام", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("مثال", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("حد أقصى", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📊 Excel/CSV", fontSize = 9.sp, color = Color.White)
                            Text("جدول المنتجات والأسعار", fontSize = 8.sp, color = Color.LightGray)
                            Text("products.xlsx", fontSize = 8.sp, color = Color.Gray)
                            Text("10 ملفات", fontSize = 8.sp, color = themeColors.accent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📄 PDF", fontSize = 9.sp, color = Color.White)
                            Text("منيو/كتالوج (عرض ثابت)", fontSize = 8.sp, color = Color.LightGray)
                            Text("menu.pdf", fontSize = 8.sp, color = Color.Gray)
                            Text("5 ملفات", fontSize = 8.sp, color = themeColors.accent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🖼️ صور", fontSize = 9.sp, color = Color.White)
                            Text("صور المنتجات (عرض مرئي)", fontSize = 8.sp, color = Color.LightGray)
                            Text("product1.jpg", fontSize = 8.sp, color = Color.Gray)
                            Text("10 صور", fontSize = 8.sp, color = themeColors.accent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📋 JSON", fontSize = 9.sp, color = Color.White)
                            Text("استيراد تلقائي", fontSize = 8.sp, color = Color.LightGray)
                            Text("products.json", fontSize = 8.sp, color = Color.Gray)
                            Text("5 ملفات", fontSize = 8.sp, color = themeColors.accent)
                        }
                    }

                    Text(
                        "🔹 شروط:\n• حجم الملف: <10MB (Excel/PDF/JSON) | <5MB (صور).\n• امتدادات مدعومة: XLSX, CSV, PDF, JPG, PNG, JSON.",
                        fontSize = 9.sp,
                        color = Color(0xFFFBBF24)
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("👉 [+ رفع ملف]", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text(
                        "📌 الملفات المرفوعة (${attachments.size}/10):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (attachments.isEmpty()) {
                        Text("(لا يوجد ملفات بعد)", fontSize = 9.sp, color = Color.Gray)
                    } else {
                        attachments.forEach { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        when (att.type) {
                                            "EXCEL", "CSV" -> "📊"
                                            "PDF" -> "📄"
                                            "IMAGE" -> "🖼️"
                                            else -> "📋"
                                        },
                                        fontSize = 14.sp
                                    )
                                    Column {
                                        Text(att.fileName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${att.type} - ${(att.size / 1024)}KB", fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(
                                    onClick = { onAttachmentsChanged(attachments.filter { it.id != att.id }) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                "MANAGEMENT" -> {
                    Text(
                        "📦 إدارة مرفقات المنتجات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text(
                        "قم بإضافة/تعديل/حذف ملفات منتجاتك. هذه الملفات ستظهر للزوار في صفحة ملفك الشخصي.",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📁 [+ رفع ملف جديد]", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text(
                        "📂 ملفاتي الحالية (${attachments.size}/10):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (attachments.isEmpty()) {
                        Text("لا توجد ملفات مرفوعة حالياً. اضغط على الزر أعلاه لإضافة المنيو أو الكتالوج.", fontSize = 10.sp, color = Color.Gray)
                    } else {
                        attachments.forEach { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        when (att.type) {
                                            "EXCEL", "CSV" -> "📊"
                                            "PDF" -> "📄"
                                            "IMAGE" -> "🖼️"
                                            else -> "📋"
                                        },
                                        fontSize = 16.sp
                                    )
                                    Column {
                                        Text(att.fileName, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${att.type} • ${att.size / 1024}KB", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    TextButton(
                                        onClick = {
                                            Toast.makeText(context, "👁️ جاري فتح الملف: ${att.fileName}", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("[👁️ عرض]", fontSize = 9.sp, color = themeColors.accent)
                                    }
                                    TextButton(
                                        onClick = {
                                            Toast.makeText(context, "📥 جاري تحميل الملف: ${att.fileName}", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("[📥 تحميل]", fontSize = 9.sp, color = Color(0xFF38BDF8))
                                    }
                                    TextButton(
                                        onClick = { onAttachmentsChanged(attachments.filter { it.id != att.id }) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("[🗑️ حذف]", fontSize = 9.sp, color = Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚠️ ملاحظة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("• ملفات Excel/JSON ستظهر كجدول قابل للتصفح في ملفك الشخصي.", fontSize = 9.sp, color = Color.LightGray)
                            Text("• ملفات PDF ستظهر كملف قابل للتحميل.", fontSize = 9.sp, color = Color.LightGray)
                            Text("• الصور ستظهر في معرض الصور الخاص بمنتجاتك.", fontSize = 9.sp, color = Color.LightGray)
                        }
                    }
                }

                "VISITOR_VIEW" -> {
                    Text(
                        "🛒 منتجاتنا/خدماتنا",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Text("تعرف على قائمة منتجاتنا/خدماتنا:", fontSize = 10.sp, color = Color.LightGray)

                    val pdfs = attachments.filter { it.type == "PDF" }
                    val excels = attachments.filter { it.type == "EXCEL" || it.type == "CSV" }
                    val images = attachments.filter { it.type == "IMAGE" }

                    if (pdfs.isNotEmpty() || excels.isNotEmpty()) {
                        Text("📄 الكتالوجات والملفات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        pdfs.forEach { p ->
                            Button(
                                onClick = {
                                    Toast.makeText(context, "📥 جاري تحميل المنيو (PDF)...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("👉 [تحميل المنيو (PDF)] (${p.fileName} - ${(p.size / 1024)}KB)", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                        excels.forEach { ex ->
                            Button(
                                onClick = {
                                    Toast.makeText(context, "📥 جاري تحميل قائمة الأسعار (Excel)...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("👉 [تحميل قائمة الأسعار (Excel)] (${ex.fileName} - ${(ex.size / 1024)}KB)", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("📋 قائمة المنتجات والخدمات (من Excel/JSON):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الصورة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("اسم المنتج", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("السعر", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("الفئة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                        listOf(
                            Triple("أرز بسمتي فاخر", "1,000 YER", "مواد غذائية"),
                            Triple("زيت طهي نقي", "1,500 YER", "مواد غذائية"),
                            Triple("سكر أبيض ممتاز", "800 YER", "مواد غذائية")
                        ).forEach { (pName, price, cat) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("🖼️", fontSize = 12.sp)
                                Text(pName, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(price, fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Text(cat, fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }

                    if (images.isNotEmpty()) {
                        Text("🖼️ معرض صور المنتجات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(images) { img ->
                                Box(
                                    modifier = Modifier
                                        .size(65.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🖼️", fontSize = 18.sp)
                                        Text(img.fileName, fontSize = 7.sp, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("💡 نصيحة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                            Text("• لتحميل الكتالوج الكامل (PDF)، اضغط على زر التحميل أعلاه.", fontSize = 9.sp, color = Color.LightGray)
                            Text("• لقراءة قائمة الأسعار (Excel)، يمكنك فتحها في إكسيل أو جوجل شيتس.", fontSize = 9.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("📁 رفع مرفق جديد", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر صيغة وصنف الملف المطلوب رفعـه:", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("EXCEL" to "📊 Excel", "PDF" to "📄 PDF", "IMAGE" to "🖼️ صورة", "JSON" to "📋 JSON").forEach { (type, lbl) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(lbl, fontSize = 9.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        label = { Text("اسم الملف (مثال: menu.pdf أو prices.xlsx)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mime = when (selectedType) {
                            "PDF" -> "application/pdf"
                            "EXCEL", "CSV" -> "*/*"
                            "IMAGE" -> "image/*"
                            else -> "*/*"
                        }
                        filePicker.launch(mime)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("اختر من هاتفـك 📱", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ==========================================
// 🔥 SPECIAL OFFERS & DISCOUNTS COMPONENT
// ==========================================
@Composable
fun SpecialOffersSection(
    offersJson: String,
    onOffersChanged: (String) -> Unit,
    isEditable: Boolean,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var offersList by remember(offersJson) { mutableStateOf(com.example.data.SpecialOfferEntity.parseList(offersJson)) }
    var showAddDialog by remember { mutableStateOf(false) }

    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var percentInput by remember { mutableStateOf("15") }
    var originalPriceInput by remember { mutableStateOf("10000") }
    var offerPriceInput by remember { mutableStateOf("8500") }
    var expiryInput by remember { mutableStateOf("2026-12-31") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔥 العروض والتخفيضات الحصرية",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                if (isEditable) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ إضافة عرض جديد", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (offersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد عروض أو تخفيضات مضافة حالياً 🏷️", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(offersList) { offer ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.Red,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "خصم ${offer.discountPercent}%",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (isEditable) {
                                        IconButton(
                                            onClick = {
                                                val updated = offersList.filter { it.id != offer.id }
                                                offersList = updated
                                                onOffersChanged(com.example.data.SpecialOfferEntity.serializeList(updated))
                                                Toast.makeText(context, "تم حذف العرض 🗑️", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text(
                                    offer.title.ifEmpty { "عرض خاص محدود" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                if (offer.description.isNotEmpty()) {
                                    Text(
                                        offer.description,
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        maxLines = 2
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (offer.originalPrice > 0) {
                                        Text(
                                            "${offer.originalPrice.toInt()} ر.ي",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                        )
                                    }
                                    Text(
                                        "${offer.offerPrice.toInt()} ر.ي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.accent
                                    )
                                }

                                if (offer.expiryDate.isNotEmpty()) {
                                    Text(
                                        "⏳ ينتهي في: ${offer.expiryDate}",
                                        fontSize = 9.sp,
                                        color = Color.Yellow
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔥 إضافة عرض أو تخفيض جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("عنوان العرض (مثال: خصم الصيف 20%)") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("تفاصيل العرض والشروط") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = percentInput,
                            onValueChange = { percentInput = it },
                            label = { Text("نسبة الخصم %") },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = expiryInput,
                            onValueChange = { expiryInput = it },
                            label = { Text("تاريخ الانتهاء") },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = originalPriceInput,
                            onValueChange = { originalPriceInput = it },
                            label = { Text("السعر الأصلي") },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = offerPriceInput,
                            onValueChange = { offerPriceInput = it },
                            label = { Text("السعر بعد الخصم") },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val newOffer = com.example.data.SpecialOfferEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = titleInput.ifBlank { "عرض خاص" },
                                    description = descInput,
                                    discountPercent = percentInput.toIntOrNull() ?: 10,
                                    originalPrice = originalPriceInput.toDoubleOrNull() ?: 0.0,
                                    offerPrice = offerPriceInput.toDoubleOrNull() ?: 0.0,
                                    expiryDate = expiryInput.ifBlank { "2026-12-31" }
                                )
                                val updated = offersList + newOffer
                                offersList = updated
                                onOffersChanged(com.example.data.SpecialOfferEntity.serializeList(updated))
                                showAddDialog = false
                                Toast.makeText(context, "✅ تم إضافة العرض بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("حفظ العرض 💾", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
