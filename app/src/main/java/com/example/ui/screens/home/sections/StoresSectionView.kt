package com.example.ui.screens.home.sections

import androidx.compose.foundation.BorderStroke
import com.example.viewmodels.AuthViewModel
import com.example.viewmodels.AdminViewModel
import com.example.viewmodels.StoreViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity

import com.example.ui.screens.home.extensions.isCommercialStore
import com.example.utils.VisualThemePalette

/**
 * 🛍️ StoresSectionView - عرض المحلات والمتاجر التجارية فقط
 */
@Composable
fun StoresSectionView(
    storeViewModel: StoreViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateStoreClick: () -> Unit
) {
    val allStores by storeViewModel.stores.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState(initial = "")
    val adminRole by adminViewModel.adminRole.collectAsState()
    val isAdminUser = adminRole == "ADMIN" || adminRole == "SUPER_ADMIN" || adminRole == "MAIN_ADMIN" || adminRole == "OWNER"

    val commercialStores = remember(allStores, currentUserId, adminRole) {
        allStores.filter { 
            it.isCommercialStore() && !it.isDeleted && (it.isApproved || it.ownerId == currentUserId || isAdminUser)
        }
    }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "👔 ملابس وأزياء",
        "📱 إلكترونيات وهواتف",
        "📺 أجهزة منزلية",
        "🛒 سوبرماركت ومواد",
        "💄 عطور وتجميل",
        "🚗 قطع غيار ومستلزمات"
    )

    val filteredList = remember(commercialStores, selectedSubCategory) {
        if (selectedSubCategory == "الكل") commercialStores
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            commercialStores.filter { 
                it.name.contains(key) || it.description.contains(key) || it.categoryId.contains(key)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🏪 المحلات والمتاجر التجارية:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح جميع المحلات والمعارض والمتاجر المعتمدة باليمن", fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateStoreClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة متجر تجاري", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Subcategories row
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
                        subCat,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("لا توجد محلات تجارية مسجلة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { store ->
                Card(
                    onClick = { onStoreClick(store) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(themeColors.accent.copy(alpha = 0.6f), Color.Transparent))),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = themeColors.primary.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, themeColors.accent.copy(alpha = 0.5f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🛍️", fontSize = 24.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(store.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (store.isVip) {
                                    Surface(
                                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFFFD700))
                                    ) {
                                        Text("⭐ VIP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("📍 ${store.cityId} - ${store.localNeighborhood}", fontSize = 11.5.sp, color = Color.LightGray)
                            if (store.workingHours.isNotEmpty()) {
                                Text("⏱️ ${store.workingHours}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = BorderStroke(0.5.dp, themeColors.accent)
                        ) {
                            Text("⭐ ${store.rating}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
