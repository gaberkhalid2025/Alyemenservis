package com.example.ui.screens.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.ProviderCard
import com.example.ui.components.ProviderListSkeleton
import com.example.utils.VisualThemePalette

/**
 * 📱 ServicesBrowserContent - المحتوى التفاعلي وعرض قوائم الفنيين والمتاجر والأقسام
 */
@Composable
fun ServicesBrowserMainContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    displayProviders: List<ProviderEntity>,
    isProvidersLoading: Boolean,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    providersLimit: Int,
    onLoadMore: () -> Unit,
    onStoreClick: (StoreEntity) -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onChatOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Categories Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isAll = selectedCategoryId.isNullOrEmpty()
            Surface(
                onClick = { onCategorySelected(null) },
                shape = RoundedCornerShape(16.dp),
                color = if (isAll) themeColors.accent else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isAll) themeColors.accent else Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    "⭐ كل الأقسام",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAll) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            categories.forEach { cat ->
                val isSelected = selectedCategoryId == cat.id
                Surface(
                    onClick = { onCategorySelected(cat.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        "${cat.icon.ifBlank { "🔧" }} ${cat.name}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Providers List Section
        if (isProvidersLoading) {
            ProviderListSkeleton()
        } else if (displayProviders.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔍 لا توجد نتائج مطابقة لبحثك", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("جرب تغيير معايير البحث أو اختيار قسم آخر لعرض المتاحين باليمن.", fontSize = 10.5.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                }
            }
        } else {
            val limitedProviders = displayProviders.take(providersLimit)
            limitedProviders.forEach { provider ->
                ProviderCard(
                    provider = provider,
                    themeColors = themeColors,
                    viewModel = viewModel,
                    onChatOpen = onChatOpen
                )
            }

            if (displayProviders.size > providersLimit) {
                Button(
                    onClick = onLoadMore,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("عرض المزيد من الفنيين (${displayProviders.size - providersLimit} متبقي) ⬇️", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 🛍️ StoresSectionView - عرض المتاجر والمطاعم
 */
@Composable
fun StoresSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateStoreClick: () -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏪 المتاجر والمطاعم المعتمدة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = onCreateStoreClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة متجرك", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (stores.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("لا توجد متاجر معروضة حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            stores.forEach { store ->
                Card(
                    onClick = { onStoreClick(store) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🏪", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(store.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${store.cityId} - ${store.localNeighborhood}", fontSize = 10.5.sp, color = Color.LightGray)
                        }
                        Text("⭐ ${store.rating}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 🏠 PropertiesSectionView - عرض العقارات والاستثمارات
 */
@Composable
fun PropertiesSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onPropertyClick: (PropertyEntity) -> Unit,
    onCreatePropertyClick: () -> Unit
) {
    val properties by viewModel.properties.collectAsState()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏢 العقارات والشقق المعروضة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = onCreatePropertyClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة عقار", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (properties.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("لا توجد عقارات معروضة حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            properties.forEach { property ->
                Card(
                    onClick = { onPropertyClick(property) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🏡", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(property.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${property.price.toInt()} ${property.currency} - ${property.cityId}", fontSize = 10.5.sp, color = themeColors.accent)
                        }
                    }
                }
            }
        }
    }
}
