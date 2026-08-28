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
 * 🏷️ Classifications helpers to ensure absolute separation between categories
 */
fun StoreEntity.isRestaurantOrCafe(): Boolean {
    val lowerName = name.lowercase()
    val lowerSec = sectionId.lowercase()
    val lowerCat = categoryId.lowercase()
    val lowerType = providerType.lowercase()

    if (lowerSec in listOf("restaurants", "restaurant", "food", "cafe", "cafes", "dining", "sweets", "bakery")) return true
    if (lowerType in listOf("restaurant", "cafe", "food", "dining")) return true
    if (lowerCat in listOf("restaurants", "restaurant", "cafe", "food", "dining", "sweets", "bakery")) return true

    val foodKeywords = listOf(
        "مطعم", "مطاعم", "كافيه", "كافية", "مقهى", "بوفيه", "وجبات", "مشويات", "شاورما",
        "بيتزا", "حلويات", "مخبز", "مخابز", "عصائر", "عصير", "دجاج", "حنيذ", "زربيان",
        "شبيات", "برجر", "قهوة", "مأكولات", "سندوتشات", "اكلات", "أكلات", "بحرية", "بحريات"
    )
    return foodKeywords.any { lowerName.contains(it) }
}

fun StoreEntity.isMedicalCenter(): Boolean {
    val lowerName = name.lowercase()
    val lowerSec = sectionId.lowercase()
    val lowerCat = categoryId.lowercase()
    val lowerType = providerType.lowercase()

    if (medicalLicenseNo.isNotBlank()) return true
    if (lowerSec in listOf("medical", "clinics", "clinic", "hospital", "hospitals", "pharmacy", "pharmacies", "lab", "labs")) return true
    if (lowerType in listOf("medical", "clinic", "hospital", "doctor", "pharmacy", "lab")) return true
    if (lowerCat in listOf("medical", "clinics", "hospitals", "pharmacies", "labs", "dental")) return true

    val medicalKeywords = listOf(
        "مستشفى", "مستشفيات", "عياده", "عيادة", "عيادات", "مركز طبي", "مجمع طبي",
        "طبي", "طبيب", "صيدلية", "صيدليه", "مختبر", "مختبرات", "بصريات", "أسنان",
        "اسنان", "علاج", "دكتور", "صيدليات", "تشخيص"
    )
    return medicalKeywords.any { lowerName.contains(it) }
}

fun StoreEntity.isCommercialStore(): Boolean {
    return !isMedicalCenter() && !isRestaurantOrCafe()
}

/**
 * 🛍️ StoresSectionView - عرض المحلات والمتاجر التجارية فقط
 */
@Composable
fun StoresSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateStoreClick: () -> Unit
) {
    val allStores by viewModel.stores.collectAsState()
    val commercialStores = remember(allStores) { allStores.filter { it.isCommercialStore() } }
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🛍️", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(store.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (store.isVip) {
                                    Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(4.dp)) {
                                        Text("VIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("📍 ${store.cityId} - ${store.localNeighborhood}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (store.workingHours.isNotEmpty()) {
                                Text("⏱️ ${store.workingHours}", fontSize = 9.5.sp, color = Color.Gray)
                            }
                        }
                        Text("⭐ ${store.rating}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 🍔 RestaurantsSectionView - عرض المطاعم والكافيهات فقط
 */
@Composable
fun RestaurantsSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateRestaurantClick: () -> Unit
) {
    val allStores by viewModel.stores.collectAsState()
    val restaurantsList = remember(allStores) { allStores.filter { it.isRestaurantOrCafe() } }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "🍖 مطاعم ومأكولات شعبية",
        "☕ كافيهات ومقاهي",
        "🍕 وجبات سريعة وبرجر",
        "🍰 حلويات ومخابز",
        "🧃 عصائر وبوفيهات",
        "🍣 مأكولات بحرية"
    )

    val filteredList = remember(restaurantsList, selectedSubCategory) {
        if (selectedSubCategory == "الكل") restaurantsList
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            restaurantsList.filter { 
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
                Text("🍽️ المطاعم والكافيهات:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح أشهر المطاعم والوجبات والكافيهات والمخابز باليمن", fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateRestaurantClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مطعم / كافيه", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("لا توجد مطاعم أو كافيهات مسجلة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { store ->
                Card(
                    onClick = { onStoreClick(store) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🍔", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(store.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (store.isVip) {
                                    Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(4.dp)) {
                                        Text("VIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("📍 ${store.cityId} - ${store.localNeighborhood}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (store.workingHours.isNotEmpty()) {
                                Text("⏱️ أوقات العمل: ${store.workingHours}", fontSize = 9.5.sp, color = Color.Gray)
                            }
                        }
                        Text("⭐ ${store.rating}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 🏥 MedicalCentersSectionView - عرض المراكز والعيادات والمستشفيات الطبية فقط
 */
@Composable
fun MedicalCentersSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onCreateMedicalClick: () -> Unit
) {
    val allStores by viewModel.stores.collectAsState()
    val medicalList = remember(allStores) { allStores.filter { it.isMedicalCenter() } }
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
            medicalList.filter { 
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
                Text("🏥 المراكز والمجمعـات الطبية:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح المستشفيات والعيادات والمختبرات والصيدليات المعتمدة", fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateMedicalClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مركز طبي", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("لا توجد مراكز طبية أو عيادات مسجلة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { store ->
                Card(
                    onClick = { onStoreClick(store) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🩺", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(store.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (store.isVerified) {
                                    Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                                        Text("معتمد", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("📍 ${store.cityId} - ${store.localNeighborhood}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (store.medicalLicenseNo.isNotEmpty()) {
                                Text("📜 ترخيص طبي: ${store.medicalLicenseNo}", fontSize = 9.5.sp, color = themeColors.accent)
                            }
                        }
                        Text("⭐ ${store.rating}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 🏠 PropertiesSectionView - عرض العقارات والشقق المخصصة فقط
 */
@Composable
fun PropertiesSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onPropertyClick: (PropertyEntity) -> Unit,
    onCreatePropertyClick: () -> Unit
) {
    val properties by viewModel.properties.collectAsState()
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "🏢 شقق للإيجار",
        "🏠 فلل للبيع",
        "🏞️ أراضي واستثمارات",
        "🏬 مكاتب ومحلات",
        "🏘️ عماير وأبراج"
    )

    val filteredList = remember(properties, selectedSubCategory) {
        if (selectedSubCategory == "الكل") properties
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            properties.filter { 
                it.title.contains(key) || it.propertyType.contains(key) || it.type.contains(key)
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
                Text("🏢 العقـارات والاستثمارات:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح الشقق والفلل والأراضي والمحلات المعروضة باليمن", fontSize = 10.sp, color = Color.Gray)
            }
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
                Text("لا توجد عقارات معروضة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { property ->
                Card(
                    onClick = { onPropertyClick(property) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏡", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(property.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("📍 ${property.cityId} - ${property.localNeighborhood}", fontSize = 10.5.sp, color = Color.LightGray)
                            Text("💵 ${property.price.toInt()} ${property.currency}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 💼 JobsSectionView - عرض إعلانات الوظائف والفرص المتاحة فقط
 */
@Composable
fun JobsSectionView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onJobClick: (JobEntity) -> Unit,
    onCreateJobClick: () -> Unit
) {
    val jobsList by viewModel.jobs.collectAsState()
    val activeJobs = remember(jobsList) { jobsList.filter { !it.isDeleted && (it.isApproved || it.isActive) } }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "📊 وظائف إدارية",
        "💻 هندسة وتقنية",
        "📢 تسويق ومبيعات",
        "👨‍🏫 تدريس وتعليم",
        "🛡️ حراسة وخدمات",
        "🛠️ مهن وحرف"
    )

    val filteredList = remember(activeJobs, selectedSubCategory) {
        if (selectedSubCategory == "الكل") activeJobs
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            activeJobs.filter { 
                it.title.contains(key) || it.companyName.contains(key) || it.jobType.contains(key)
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
                Text("💼 إعلانات الوظائـف الشاغرة:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح أحدث فرص العمل والوظائف الشاغرة باليمن", fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateJobClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نشر إعلان وظيفة", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("لا توجد إعلانات وظائف معروضة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { job ->
                Card(
                    onClick = { onJobClick(job) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍💼", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("🏢 ${job.companyName} - 📍 ${job.cityId}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (job.salary.isNotEmpty()) {
                                Text("💰 الراتب: ${job.salary}", fontSize = 10.sp, color = themeColors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

