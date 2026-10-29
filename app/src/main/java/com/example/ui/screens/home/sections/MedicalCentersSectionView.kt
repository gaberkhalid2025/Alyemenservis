package com.example.ui.screens.home.sections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.domain.isAdmin
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.ui.screens.home.extensions.isMedicalCenter
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.utils.VisualThemePalette

/**
 * 🏥 MedicalCentersSectionView - عرض المراكز الطبية والعيادات بالبطاقة الطبية المعتمدة
 */
@Composable
fun MedicalCentersSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateMedicalClick: () -> Unit
) {
    val context = LocalContext.current
    val allStores by viewModel.stores.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole.isAdmin()

    val medicalList = remember(allStores, currentUserId, adminRole) {
        allStores.filter { 
            it.isMedicalCenter() && !it.isDeleted && (it.isApproved || it.ownerId == currentUserId || isAdminUser)
        }
    }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "🏥 مستشفيات ومجمعات",
        "🩺 عيادات تخصصية",
        "💊 صيدليات ومستلزمات",
        "🔬 مختبرات وأشعة",
        "🦷 مراكز أسنان وبصريات"
    )

    val filteredList = remember(medicalList, selectedSubCategory) {
        if (selectedSubCategory == "الكل") medicalList
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            val keywords = key.split(" ").filter { it.length > 2 }
            medicalList.filter { store ->
                keywords.any { k -> 
                    store.name.contains(k, ignoreCase = true) || 
                    store.description.contains(k, ignoreCase = true) || 
                    store.categoryId.contains(k, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // رأس القسم وزر إضافة مركز
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "🏥 المراكز والمجمعات الطبية المعتمدة:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White
                )
                Text(
                    text = "تصفح المستشفيات والعيادات والمختبرات والصيدليات مع حجز المواعيد",
                    fontSize = 10.5.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Button(
                onClick = onCreateMedicalClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مركز", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        // شريط التصنيفات الفرعية
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subCategories.forEach { subCat ->
                val isSelected = selectedSubCategory == subCat
                Surface(
                    onClick = { selectedSubCategory = subCat },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = subCat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(
                    text = "لا توجد مراكز طبية أو عيادات مسجلة في هذا القسم حالياً.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // قائمة بطاقات المراكز والعيادات الطبية الفاخرة
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                filteredList.forEach { store ->
                    Card(
                        onClick = { onStoreClick(store) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // الصف العلوي: الشعار والاسم وشارة متاح اليوم
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F766E).copy(alpha = 0.25f))
                                            .border(1.2.dp, Color(0xFF2DD4BF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val logo = store.logoImage.ifBlank { store.coverImage }
                                        if (logo.isNotBlank()) {
                                            SmartAsyncImage(
                                                model = logo,
                                                contentDescription = store.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text("🩺", fontSize = 24.sp)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = store.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = CairoFontFamily,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (store.isVerified) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981)
                                                ) {
                                                    Text(
                                                        text = "معتمد ✓",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = store.description.ifBlank { "استشارات طبية ورعاية صحية متكاملة" },
                                            fontSize = 11.sp,
                                            color = Color(0xFF00E5FF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "متاح اليوم ✓",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // الموقع وساعات العمل ورقم الترخيص
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 ${store.cityId.ifBlank { "اليمن" }} - ${store.localNeighborhood.ifBlank { "وسط المدينة" }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                if (store.workingHours.isNotBlank()) {
                                    Text(
                                        text = "⏱️ ${store.workingHours}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            if (store.medicalLicenseNo.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.05f)
                                ) {
                                    Text(
                                        text = "📜 ترخيص طبي معتمد: ${store.medicalLicenseNo}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2DD4BF),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.8.dp)

                            // رسوم الكشفية وأزرار الحجز والتواصل
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("رسوم الكشفية", fontSize = 9.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "3,000 ر.ي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onStoreClick(store) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حجز موعد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    if (store.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF10B981), RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = { onStoreClick(store) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF0284C7), RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "محادثة", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
