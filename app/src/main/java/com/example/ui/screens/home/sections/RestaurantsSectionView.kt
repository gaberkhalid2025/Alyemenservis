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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.screens.home.extensions.isRestaurantOrCafe
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.TajawalFontFamily
import com.example.utils.VisualThemePalette

/**
 * 🍔 RestaurantsSectionView - عرض المطاعم والكافيهات بالبطاقة الفاخرة والمكتملة
 */
@Composable
fun RestaurantsSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateRestaurantClick: () -> Unit
) {
    val context = LocalContext.current
    val allStores by viewModel.stores.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole.isAdmin()

    val restaurantsList = remember(allStores, currentUserId, adminRole) {
        allStores.filter { 
            it.isRestaurantOrCafe() && !it.isDeleted && (it.isApproved || it.ownerId == currentUserId || isAdminUser)
        }
    }

    data class SubCategoryItem(val label: String, val key: String?, val icon: String = "")

    val subCategories = listOf(
        SubCategoryItem("الكل", null, "🔍"),
        SubCategoryItem("مطاعم وشعبي", "مطاعم", "🍖"),
        SubCategoryItem("كافيهات ومقاهي", "كافيه", "☕"),
        SubCategoryItem("وجبات سريعة", "وجبات", "🍕"),
        SubCategoryItem("حلويات ومخابز", "حلويات", "🍰"),
        SubCategoryItem("عصائر وبوفيهات", "عصائر", "🧃"),
        SubCategoryItem("مأكولات بحرية", "بحرية", "🍣")
    )
    
    var selectedSubCat by remember { mutableStateOf(subCategories[0]) }

    val filteredList = remember(restaurantsList, selectedSubCat) {
        val key = selectedSubCat.key
        if (key == null) restaurantsList
        else {
            restaurantsList.filter { 
                it.name.contains(key, ignoreCase = true) || 
                it.description.contains(key, ignoreCase = true) || 
                it.categoryId.contains(key, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // رأس القسم وزر إضافة مطعم
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "🍽️ المطاعم والكافيهات الشهيرة:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CairoFontFamily,
                    color = Color.White
                )
                Text(
                    text = "تصفح أشهر المطاعم والوجبات والكافيهات والمخابز في اليمن",
                    fontSize = 10.5.sp,
                    fontFamily = TajawalFontFamily,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Button(
                onClick = onCreateRestaurantClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مطعم", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
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
                val isSelected = selectedSubCat == subCat
                Surface(
                    onClick = { selectedSubCat = subCat },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "${subCat.icon} ${subCat.label}",
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
                    text = "لا توجد مطاعم أو كافيهات مسجلة في هذا القسم حالياً.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // قائمة بطاقات المطاعم الفاخرة المكتملة
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // صورة الطبق / الغلاف مع تدرج داكن وشارات
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                val cover = store.coverImage.ifBlank { store.logoImage }
                                if (cover.isNotBlank()) {
                                    SmartAsyncImage(
                                        model = cover,
                                        contentDescription = store.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF97316)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🍲", fontSize = 48.sp)
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

                                // شارة التوصيل (20-35 دقيقة)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                        Text("20-35 دقيقة", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // شارة التقييم وVIP
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (store.isVip) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xFFFFD700).copy(alpha = 0.25f),
                                            border = BorderStroke(0.8.dp, Color(0xFFFFD700))
                                        ) {
                                            Text(
                                                text = "⭐ VIP",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD700),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "⭐ ${if (store.rating > 0f) store.rating else 4.9}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // بيانات وتفاصيل المطعم
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = store.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "متوسط 1,500 ر.ي",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                // شارات المأكولات والميزات
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.06f)
                                    ) {
                                        Text(
                                            text = "مأكولات طازجة",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "توصيل سريع",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // الموقع وساعات العمل
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
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.8.dp)

                                // أزرار التفاعل المباشر
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onStoreClick(store) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اطلب وجبتك / تصفح القائمة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
