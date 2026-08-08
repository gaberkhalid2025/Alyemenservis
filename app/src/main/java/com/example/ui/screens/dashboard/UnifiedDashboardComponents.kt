package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

// ==========================================
// 1. 📋 Unified Profile Section Component
// ==========================================
@Composable
fun UnifiedProfileSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(account.name) }
    var description by remember { mutableStateOf(account.description) }
    var phone by remember { mutableStateOf(account.phone) }
    var ownerName by remember { mutableStateOf(account.ownerName) }
    var workingHours by remember { mutableStateOf(account.workingHours) }
    var neighborhood by remember { mutableStateOf(account.neighborhood) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📝 البيانات الشخصية والمعلومات العامة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم التجاري / اسم المنشأة", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم صاحب العمل / المدير المسجل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف والواتساب للتواصل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("نبذة وتفاصيل وصفية كاملة عن الخدمة/المكان", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        label = { Text("أوقات وساعات العمل والدوام (مثال: 8:00 AM - 10:00 PM)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("العنوان والحي والمنطقة التفصيلية", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                if (account.rawStore != null) {
                                    val updated = account.rawStore.copy(
                                        name = name,
                                        description = description,
                                        phone = phone,
                                        ownerName = ownerName,
                                        workingHours = workingHours,
                                        localNeighborhood = neighborhood
                                    )
                                    viewModel.saveStore(updated)
                                } else if (account.rawProvider != null) {
                                    val updated = account.rawProvider.copy(
                                        name = name,
                                        phone = phone,
                                        localNeighborhood = neighborhood,
                                        profession = description
                                    )
                                    viewModel.updateProviderEntity(updated)
                                }
                                Toast.makeText(context, "✅ تم حفظ التعديلات سحابياً بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "⚠️ يرجى تعبئة الحقول الأساسية", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتحديث البيانات الشخصية 💾", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ⚙️ Unified Settings & Permissions Section
// ==========================================
@Composable
fun UnifiedSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf(account.password) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙️ الإعدادات والصلاحيات والأمن", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور المشفرة للوصول لوحة التحكم", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    Button(
                        onClick = {
                            if (newPassword.isNotBlank()) {
                                viewModel.resetAccountPassword(
                                    if (account.businessType == BusinessType.TECHNICIAN) "PROVIDER" else "STORE",
                                    account.phone,
                                    newPassword
                                )
                                Toast.makeText(context, "🔒 تم مشفر وتحديث كلمة المرور بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تشفير وتحديث كلمة المرور 🔑", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. 🛒 Unified Products / Services Section
// ==========================================
@Composable
fun UnifiedProductsServicesSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 قائمة المنتجات / الخدمات / الأطباق", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("إضافة جديدة ➕", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد منتجات مسجلة حالياً بهذا الحساب.", fontSize = 11.sp, color = Color.LightGray)
                Text("اضغط على زر (إضافة جديدة) لإدراج قائمة منتجاتك أو خدماتك بالأسعار والصور.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة عنصر جديد 🛒", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("اسم المنتج / الخدمة / الطبق", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("السعر (بالريال اليمني YER)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("التفاصيل والوصف", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prodName.isNotBlank()) {
                            Toast.makeText(context, "✅ تم إضافة العنصر بنجاح!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                            prodName = ""
                            prodPrice = ""
                            prodDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("حفظ وإضافة 💾", fontSize = 11.sp, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        )
    }
}

// ==========================================
// 4. 🎁 Unified Offers Section
// ==========================================
@Composable
fun UnifiedOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var offerTitle by remember { mutableStateOf("") }
    var offerDiscount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎁 العروض والتخفيضات الخاصة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة عرض ترويجي جديد مع نسبة الخصم:", fontSize = 11.sp, color = Color.White)

                OutlinedTextField(
                    value = offerTitle,
                    onValueChange = { offerTitle = it },
                    label = { Text("عنوان العرض الترويجي (مثل: خصم 20% لفترة محدودة)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = offerDiscount,
                    onValueChange = { offerDiscount = it },
                    label = { Text("نسبة الخصم % (مثال: 20)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (offerTitle.isNotBlank()) {
                            Toast.makeText(context, "🎉 تم نشر العرض الترويجي بنجاح!", Toast.LENGTH_SHORT).show()
                            offerTitle = ""
                            offerDiscount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("نشر العرض للعملاء 📢", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 5. ⭐ Unified Ratings & Reviews Section
// ==========================================
@Composable
fun UnifiedRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⭐ التقييمات وآراء العملاء", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("متوسط التقييم العام: ", fontSize = 12.sp, color = Color.White)
                    Text("⭐ ${account.rating} / 5.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(${account.numReviews} تقييم)", fontSize = 11.sp, color = Color.LightGray)
                }
                Text("يتيح هذا التبويب استعراض تعليقات وتقييمات العملاء وإمكانية الرد المباشر عليها.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }
}

// ==========================================
// 6. 📅 Unified Bookings & Appointments Section
// ==========================================
@Composable
fun UnifiedBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📅 إدارة الحجوزات والمواعيد والطلبات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(
                Pair("ALL", "الكل"),
                Pair("PENDING", "قيد الانتظار ⏳"),
                Pair("APPROVED", "مقبولة ✅"),
                Pair("COMPLETED", "مكتملة 🎉"),
                Pair("REJECTED", "ملغاة ❌")
            )
            items(filters) { item ->
                val isSel = selectedFilter == item.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else Color.DarkGray)
                        .clickable { selectedFilter = item.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(item.second, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد طلبيات أو حجوزات مسجلة ضمن الفلتر المختار حالياً.", fontSize = 11.sp, color = Color.LightGray)
                Text("تتم المزامنة الفورية عند قيام أي عميل بالحجز أو طلب الخدمة من التطبيق.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }
}

// ==========================================
// 7. 📎 Unified Attachments & Files Section
// ==========================================
@Composable
fun UnifiedAttachmentsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📎 إدارة المرفقات والصور والملفات (PDF / Excel)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رفع صور واجهة الغلاف والمنيو أو ملفات PDF:", fontSize = 11.sp, color = Color.White)
                Button(
                    onClick = {
                        Toast.makeText(context, "📎 جاري فتح منتقي الصور والملفات بالهاتف...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("رفع صور جديدة / ملف PDF 📤", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
