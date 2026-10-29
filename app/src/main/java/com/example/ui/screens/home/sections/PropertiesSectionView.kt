package com.example.ui.screens.home.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.domain.isAdmin
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.utils.VisualThemePalette
import java.util.Locale

/**
 * 🏠 PropertiesSectionView - عرض العقارات والشقق المخصصة بالبطاقة العقارية الفاخرة
 */
@Composable
fun PropertiesSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onPropertyClick: (PropertyEntity) -> Unit,
    onCreatePropertyClick: () -> Unit
) {
    val properties by viewModel.properties.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole.isAdmin()

    val activeProperties = remember(properties, currentUserId, adminRole) {
        properties.filter { 
            !it.isDeleted && (it.isApproved || it.ownerId == currentUserId || isAdminUser)
        }
    }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "🏢 شقق للإيجار",
        "🏠 فلل للبيع",
        "🏞️ أراضي واستثمارات",
        "🏬 مكاتب ومحلات",
        "🏘️ عماير وأبراج"
    )

    val filteredList = remember(activeProperties, selectedSubCategory) {
        if (selectedSubCategory == "الكل") activeProperties
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            val keywords = key.split(" ").filter { it.length > 2 }
            activeProperties.filter { prop ->
                keywords.any { k ->
                    prop.title.contains(k, ignoreCase = true) || 
                    prop.propertyType.contains(k, ignoreCase = true) || 
                    prop.type.contains(k, ignoreCase = true) ||
                    prop.description.contains(k, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // رأس القسم وزر إضافة عقار
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "🏢 العقارات والفرص السكنية والاستثمارية:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White
                )
                Text(
                    text = "تصفح الشقق والفلل والأراضي والمحلات المتاحة للإيجار والبيع باليمن",
                    fontSize = 10.5.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Button(
                onClick = onCreatePropertyClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة عقار", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    text = "لا توجد عقارات معروضة في هذا القسم حالياً.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // قائمة بطاقات العقارات الفاخرة
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                filteredList.forEach { property ->
                    Card(
                        onClick = { onPropertyClick(property) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // صورة العقار مع شارة الإيجار/البيع وتدرج القراءة
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                            ) {
                                val firstImage = property.images.firstOrNull() ?: ""
                                if (firstImage.isNotBlank()) {
                                    SmartAsyncImage(
                                        model = firstImage,
                                        contentDescription = property.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏡", fontSize = 48.sp)
                                    }
                                }

                                // تدرج داكن للقراءة
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xFF141A2E).copy(alpha = 0.9f))
                                            )
                                        )
                                )

                                // شارة الإيجار أو البيع
                                val isSale = property.type.contains("بيع")
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSale) Color(0xFF2563EB) else Color(0xFF059669),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = if (isSale) "للبيع 🏷️" else "للإيجار 🔑",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }

                                // المدينة والحي
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "📍 ${property.cityId.ifBlank { "اليمن" }}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // بيانات وتفاصيل العقار
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = property.title.ifBlank { "عقار مميز في موقع استراتيجي" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoFontFamily,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // مواصفات: المساحة والغرف والحي
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (property.area > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.06f)
                                        ) {
                                            Text(
                                                text = "📐 ${property.area} م²",
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (property.rooms > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.06f)
                                        ) {
                                            Text(
                                                text = "🚪 ${property.rooms} غرف",
                                                fontSize = 10.sp,
                                                color = Color(0xFFCBD5E1),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = property.localNeighborhood.ifBlank { "وسط المدينة" },
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.8.dp)

                                // السعر وأزرار المعاينة والمحادثة
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("السعر المطلوب", fontSize = 8.5.sp, color = Color(0xFF64748B))
                                        Text(
                                            text = "${String.format(Locale.US, "%,.0f", property.price)} ${property.currency.ifBlank { "ر.ي" }}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onPropertyClick(property) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تفاصيل العقار", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = { onPropertyClick(property) },
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
}
