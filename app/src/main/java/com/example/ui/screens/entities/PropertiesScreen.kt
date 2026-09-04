package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GenericEntityReviewsDialog
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun PropertiesScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onPropertyClick: (PropertyEntity) -> Unit,
    onChatClick: (PropertyEntity) -> Unit,
    onRequestInspectionClick: (PropertyEntity) -> Unit
) {
    val properties by viewModel.properties.collectAsState()
    val cities by viewModel.cities.collectAsState()

    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole == "ADMIN" || adminRole == "SUPER_ADMIN" || adminRole == "MAIN_ADMIN" || adminRole == "OWNER"
    val isLoggedIn = currentUserId.isNotBlank() && currentUserId != "guest"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedCityId by remember { mutableStateOf("الكل") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") } // rent, sale
    var selectedMinRating by remember { mutableStateOf(0.0f) }
    var showCreatePropertyDialog by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "شقة", "بيت ومستقل", "محل تجاري", "أرض")

    // Determine loading state from properties list presence
    val isLoading = remember(properties) { properties.isEmpty() }

    val filteredProperties = remember(properties, searchQuery, selectedCategory, selectedCityId, selectedTypeFilter, selectedMinRating, currentUserId, adminRole) {
        properties.filter { prop ->
            val isApprovedOrOwner = prop.isApproved || prop.ownerId == currentUserId || isAdminUser
            val matchesSearch = searchQuery.isBlank() ||
                    prop.title.contains(searchQuery, ignoreCase = true) ||
                    prop.description.contains(searchQuery, ignoreCase = true) ||
                    prop.localNeighborhood.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    (selectedCategory == "شقة" && prop.propertyType.contains("apartment", ignoreCase = true)) ||
                    (selectedCategory == "بيت ومستقل" && prop.propertyType.contains("house", ignoreCase = true)) ||
                    (selectedCategory == "محل تجاري" && prop.propertyType.contains("shop", ignoreCase = true)) ||
                    (selectedCategory == "أرض" && prop.propertyType.contains("land", ignoreCase = true)) ||
                    prop.propertyType.contains(selectedCategory, ignoreCase = true)

            val matchesType = selectedTypeFilter == "الكل" || prop.type == selectedTypeFilter

            val matchesCity = selectedCityId == "الكل" || prop.cityId == selectedCityId

            val matchesRating = prop.rating >= selectedMinRating

            isApprovedOrOwner && !prop.isDeleted && matchesSearch && matchesCat && matchesType && matchesCity && matchesRating
        }
    }

    var showGuestDialog by remember { mutableStateOf(false) }

    if (showCreatePropertyDialog) {
        com.example.PropertyCreateEditDialog(
            property = null,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showCreatePropertyDialog = false }
        )
    }

    if (showGuestDialog) {
        com.example.ui.screens.register.GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showGuestDialog = false },
            onRegisterCompleted = { _, _, _, _ -> showGuestDialog = false }
        )
    }

    GenericSectionView(
        themeColors = themeColors,
        items = filteredProperties,
        isLoading = isLoading,
        title = "العقارات والأراضي اليمنيّة",
        titleIcon = "🏠",
        searchPlaceholder = "بحث عن شقة، أرض، بيت للإيجار أو البيع... 🏠",
        categories = categories,
        cities = cities,
        onSearchQueryChanged = { searchQuery = it },
        onCategorySelected = { selectedCategory = it },
        onCitySelected = { selectedCityId = it },
        onMinRatingSelected = { selectedMinRating = it },
        emptyMessage = "لا توجد عقارات مطابقة للتصفية الحالية",
        extraHeaderContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.REGISTER_FORM) },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("➕ تسجيل وإضافة إعلان عقاري جديد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                // 🔑 Listing Type Filter (Rent vs Sale)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Text("🏷️ طبيعة العقد:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                listOf(
                    "الكل" to "الكل",
                    "rent" to "للإيجار 🔑",
                    "sale" to "للبيع والتمليك 📜"
                ).forEach { (typeVal, typeLabel) ->
                    FilterChip(
                        selected = selectedTypeFilter == typeVal,
                        onClick = { selectedTypeFilter = typeVal },
                        label = { Text(typeLabel, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.accent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
    },
        itemContent = { prop ->
            PropertyCard(
                property = prop,
                themeColors = themeColors,
                isLoggedIn = isLoggedIn,
                onClick = { onPropertyClick(prop) },
                onChatClick = {
                    if (!isLoggedIn) {
                        showGuestDialog = true
                    } else {
                        onChatClick(prop)
                    }
                },
                onRequestInspectionClick = { onRequestInspectionClick(prop) }
            )
        }
    )
}

@Composable
fun PropertyCard(
    property: PropertyEntity,
    themeColors: VisualThemePalette,
    isLoggedIn: Boolean = true,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onRequestInspectionClick: () -> Unit
) {
    val context = LocalContext.current
    var showReviewsDialog by remember { mutableStateOf(false) }

    val imageSource = property.images.firstOrNull() ?: ""
    val isRent = property.type == "rent"
    val isVerified = property.isVerified || property.isApproved || property.isVip

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, if (isVerified) themeColors.accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Cover Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(Color(0xFF1E293B))
            ) {
                if (imageSource.isNotBlank()) {
                    SmartAsyncImage(
                        model = imageSource,
                        contentDescription = property.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏠", fontSize = 34.sp)
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 35f
                            )
                        )
                )

                // Rent / Sale Badge & Rating Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isRent) Color(0xFF3B82F6) else Color(0xFF10B981),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isRent) "🔑 للإيجار" else "🏷️ للبيع",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { showReviewsDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(String.format("%.1f", property.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // 2. Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = property.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${property.price.toInt()} ${property.currency}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                }

                // Type & Description
                val typeName = when (property.propertyType) {
                    "apartment" -> "🏢 شقة"
                    "house" -> "🏡 منزل"
                    "villa" -> "🏰 فيلا"
                    "shop" -> "🏬 محل تجاري"
                    "land" -> "📐 قطعة أرض"
                    else -> "🏠 ${property.propertyType}"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeName,
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        maxLines = 1
                    )
                    if (property.description.isNotBlank()) {
                        Text(
                            text = property.description,
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                        )
                    }
                }

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    val locText = property.localNeighborhood.ifBlank { "اليمن" }
                    Text(
                        text = locText,
                        fontSize = 9.5.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Action Buttons Row: [التفاصيل] [التقييمات] [طلب معاينة]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("التفاصيل 📋", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showReviewsDialog = true },
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("التقييمات ⭐", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onRequestInspectionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("معاينة 👁️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReviewsDialog) {
        GenericEntityReviewsDialog(
            title = property.title,
            rating = property.rating,
            numReviews = property.numReviews,
            themeColors = themeColors,
            onDismiss = { showReviewsDialog = false }
        )
    }
}
