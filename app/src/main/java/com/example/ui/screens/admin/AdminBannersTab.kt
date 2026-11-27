package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.utils.compressAndResizeImageUri

internal fun LazyListScope.adminBannersTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val bannersList = state.bannersList
    val categories = state.categories

    item {
        Spacer(modifier = Modifier.height(18.dp))
        Text("🖼️ إدارة وترتيب بنرات الإعلانات الترويجية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("➕ إضافة بنر ترويجي جديد ومتقدم:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                var bannerTitle by remember { mutableStateOf("") }
                var bannerType by remember { mutableStateOf("IMAGE") }
                var bannerUrl by remember { mutableStateOf("") }
                var bannerRedirect by remember { mutableStateOf("") }
                var bannerDuration by remember { mutableStateOf(5) }

                Text("نوع البنر الترويجي:", fontSize = 10.sp, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("IMAGE" to "🖼️ صورة", "VIDEO" to "🎥 فيديو", "TEXT" to "🔤 نصي فقط").forEach { (typeKey, label) ->
                        val isSelected = bannerType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeColors.primary else Color.White.copy(alpha = 0.05f))
                                .clickable { bannerType = typeKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 10.sp, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = bannerTitle,
                    onValueChange = { bannerTitle = it },
                    label = { Text("عنوان أو نص الإعلان") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                if (bannerType != "TEXT") {
                    OutlinedTextField(
                        value = bannerUrl,
                        onValueChange = { bannerUrl = it },
                        label = { Text(if (bannerType == "IMAGE") "رابط صورة الإعلان (URL أو مسار)" else "رابط فيديو الإعلان (URL أو مسار)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    val galleryLauncherForBannerSelection = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            try {
                                val contentResolver = context.contentResolver
                                val mimeType = contentResolver.getType(uri) ?: ""
                                if (mimeType.startsWith("image/")) {
                                    val base64Str = compressAndResizeImageUri(context, uri, 800, 70)
                                    if (base64Str.isNotEmpty()) {
                                        bannerUrl = "data:image/jpeg;base64,$base64Str"
                                        bannerType = "IMAGE"
                                        Toast.makeText(context, "📸 تم اختيار وضغط الصورة بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    bannerUrl = uri.toString()
                                    bannerType = "VIDEO"
                                    Toast.makeText(context, "🎥 تم اختيار الفيديو بنجاح وتجهيز المزامنة!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "❌ فشل قراءة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (bannerType == "IMAGE") {
                                galleryLauncherForBannerSelection.launch("image/*")
                            } else {
                                galleryLauncherForBannerSelection.launch("video/*")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (bannerType == "IMAGE") "🖼️ اختيار صورة الإعلان من المعرض" else "🎥 اختيار فيديو الإعلان من المعرض",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎯 توجيه البنر عند نقر العميل (اختر القسم المستهدف):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )

                    val predefinedSections = listOf(
                        Triple("", "🌐 عام (بدون توجيه)", "الرئيسية"),
                        Triple("services", "🔧 الخدمات والمهن الفنية", "الفنيين والصيانة"),
                        Triple("stores", "🏪 المراكز والمحلات التجارية", "المتاجر والتسوق"),
                        Triple("restaurants", "🍔 المطاعم والكافيهات", "المأكولات والمشروبات"),
                        Triple("medical", "🏥 المراكز والعيادات الطبية", "الصحة والطب"),
                        Triple("properties", "🏠 العقارات والأملاك", "بيع وإيجار العقارات"),
                        Triple("jobs", "💼 الوظائف والشركات", "فرص العمل والتوظيف"),
                        Triple("quick_service", "⚡ اطلب خدمتك الفورية", "الخدمة السريعة"),
                        Triple("bookings", "📅 استمارة حجز المواعيد", "نظام الحجوزات"),
                        Triple("maps", "🗺️ خريطة المنشآت والمواقع", "الخرائط التفاعلية"),
                        Triple("chat", "💬 المحادثات الفورية", "الدردشة والدعم"),
                        Triple("join", "📝 طلبات الانضمام والتسجيل", "استمارات التسجيل")
                    )

                    val allAvailableRedirects = predefinedSections + categories.map {
                        Triple(it.id, "📂 ${it.name}", "قسم مخصص")
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allAvailableRedirects.size) { rIndex ->
                            val item = allAvailableRedirects[rIndex]
                            val isSelected = bannerRedirect.trim() == item.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) themeColors.accent else themeColors.surface)
                                    .border(1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { bannerRedirect = item.first }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.second,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSelectedName = allAvailableRedirects.firstOrNull { it.first == bannerRedirect.trim() }?.second
                            ?: if (bannerRedirect.isBlank()) "🌐 عام (بدون توجيه)" else "🏷️ مخصص: $bannerRedirect"
                        Text(
                            text = "القسم المحدد حالياً: $currentSelectedName",
                            fontSize = 10.sp,
                            color = Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (bannerRedirect.isNotEmpty()) {
                            Text(
                                text = "إلغاء التوجيه ✖",
                                fontSize = 9.sp,
                                color = Color.Red,
                                modifier = Modifier.clickable { bannerRedirect = "" }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bannerRedirect,
                        onValueChange = { bannerRedirect = it },
                        label = { Text("رمز أو معرف القسم المستهدف (أو اكتب رمزاً مخصصاً)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("مدة عرض البنر: $bannerDuration ثوانٍ", fontSize = 10.sp, color = Color.White)
                    Slider(
                        value = bannerDuration.toFloat(),
                        onValueChange = { bannerDuration = it.toInt() },
                        valueRange = 3f..20f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = themeColors.accent,
                            activeTrackColor = themeColors.primary
                        )
                    )
                }

                Button(
                    onClick = {
                        if (bannerTitle.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة عنوان الإعلان", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (bannerType != "TEXT" && bannerUrl.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة رابط أو مسار ملف الإعلان", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.addNewBanner(
                            title = bannerTitle.trim(),
                            url = if (bannerType == "TEXT") "" else bannerUrl.trim(),
                            redirect = bannerRedirect.trim(),
                            type = bannerType,
                            size = "LARGE",
                            duration = bannerDuration
                        )
                        bannerTitle = ""
                        bannerUrl = ""
                        bannerRedirect = ""
                        bannerDuration = 5
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة البنر الإعلاني المطور 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    item {
        Text("📋 قائمة البنرات النشطة (ادعم الترتيب بالسحب والإفلات والمبادلة):", fontSize = 12.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))
    }

    itemsIndexed(bannersList) { index, b ->
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "سحب لنقل الترتيب",
                        tint = themeColors.accent,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                if (index > 0) {
                                    val mutableBanners = bannersList.toMutableList()
                                    val prev = mutableBanners[index - 1]
                                    mutableBanners[index - 1] = b
                                    mutableBanners[index] = prev
                                    viewModel.reorderBanners(mutableBanners)
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val typeIcon = when (b.type.uppercase()) {
                                "IMAGE" -> "🖼️"
                                "VIDEO" -> "🎥"
                                else -> "🔤"
                            }
                            Text("$typeIcon ${b.title}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("مدة العرض: ${b.duration} ثوانٍ | القسم: ${b.redirectCategory.ifEmpty { "عام" }}", fontSize = 9.sp, color = themeColors.accent)
                        if (b.url.isNotEmpty()) {
                            Text(b.url, fontSize = 9.sp, color = themeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (index > 0) {
                                val mutableBanners = bannersList.toMutableList()
                                val prev = mutableBanners[index - 1]
                                mutableBanners[index - 1] = b
                                mutableBanners[index] = prev
                                viewModel.reorderBanners(mutableBanners)
                            }
                        },
                        enabled = index > 0
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                    }
                    IconButton(
                        onClick = {
                            if (index < bannersList.size - 1) {
                                val mutableBanners = bannersList.toMutableList()
                                val next = mutableBanners[index + 1]
                                mutableBanners[index + 1] = b
                                mutableBanners[index] = next
                                viewModel.reorderBanners(mutableBanners)
                            }
                        },
                        enabled = index < bannersList.size - 1
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = if (index < bannersList.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                    }
                    IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                    }
                }
            }
        }
    }
}
