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
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GenericEntityReviewsDialog
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun StoresScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onStoreClick: (StoreEntity) -> Unit,
    onChatClick: (StoreEntity) -> Unit,
    onRequestServiceClick: (StoreEntity) -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    val cities by viewModel.cities.collectAsState()

    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole == "ADMIN" || adminRole == "SUPER_ADMIN" || adminRole == "MAIN_ADMIN" || adminRole == "OWNER"
    val isLoggedIn = currentUserId.isNotBlank() && currentUserId != "guest"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedCityId by remember { mutableStateOf("الكل") }
    var selectedMinRating by remember { mutableStateOf(0.0f) }
    var showCreateStoreDialog by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "سوبرماركت", "إلكترونيات", "ملابس وموضة", "مواد بناء", "قطع غيار")

    val isLoading = remember(stores) { stores.isEmpty() }

    val filteredStores = remember(stores, searchQuery, selectedCategory, selectedCityId, selectedMinRating, currentUserId, adminRole) {
        stores.filter { store ->
            val isApprovedOrOwner = store.isApproved || store.ownerId == currentUserId || isAdminUser
            val isPureStore = store.sectionId != "restaurants" &&
                    !store.categoryId.contains("rest", ignoreCase = true) &&
                    !store.categoryId.contains("food", ignoreCase = true) &&
                    !store.name.contains("مطعم", ignoreCase = true) &&
                    !store.isDeleted

            val matchesSearch = searchQuery.isBlank() ||
                    store.name.contains(searchQuery, ignoreCase = true) ||
                    store.localNeighborhood.contains(searchQuery, ignoreCase = true) ||
                    store.description.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    store.categoryId.contains(selectedCategory, ignoreCase = true) ||
                    store.name.contains(selectedCategory, ignoreCase = true)

            val matchesCity = selectedCityId == "الكل" || store.cityId == selectedCityId

            val matchesRating = store.rating >= selectedMinRating

            isApprovedOrOwner && isPureStore && matchesSearch && matchesCat && matchesCity && matchesRating
        }
    }

    var showGuestDialog by remember { mutableStateOf(false) }

    if (showCreateStoreDialog) {
        com.example.StoreCreateEditDialog(
            store = null,
            viewModel = viewModel,
            themeColors = themeColors,
            sectionId = "stores",
            onDismiss = { showCreateStoreDialog = false }
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
        items = filteredStores,
        isLoading = isLoading,
        title = "المحلات التجارية والمتاجر",
        titleIcon = "🏪",
        searchPlaceholder = "بحث في المحلات والمتاجر... 🏪",
        categories = categories,
        cities = cities,
        onSearchQueryChanged = { searchQuery = it },
        onCategorySelected = { selectedCategory = it },
        onCitySelected = { selectedCityId = it },
        onMinRatingSelected = { selectedMinRating = it },
        emptyMessage = "لا توجد محلات تجارية مطابقة للبحث",
        extraHeaderContent = {
            Button(
                onClick = { viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.REGISTER_FORM) },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text("➕ تسجيل وإضافة متجر تجاري جديد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        itemContent = { store ->
            StoreItemCard(
                store = store,
                themeColors = themeColors,
                onClick = { onStoreClick(store) },
                onChatClick = {
                    if (!isLoggedIn) {
                        showGuestDialog = true
                    } else {
                        onChatClick(store)
                    }
                },
                onRequestServiceClick = { onRequestServiceClick(store) }
            )
        }
    )
}

@Composable
fun StoreItemCard(
    store: StoreEntity,
    themeColors: VisualThemePalette,
    isLoggedIn: Boolean = true,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    val context = LocalContext.current
    var showReviewsDialog by remember { mutableStateOf(false) }

    val isVerified = store.isVerified || store.isActive
    val coverImg = store.coverImage.ifBlank { "" }
    val logoImg = store.logoImage.ifBlank { "" }

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
                    .height(95.dp)
                    .background(Color(0xFF1E293B))
            ) {
                if (coverImg.isNotBlank()) {
                    SmartAsyncImage(
                        model = coverImg,
                        contentDescription = store.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏪", fontSize = 34.sp)
                    }
                }

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 30f
                            )
                        )
                )

                // Badges in Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVerified || store.isVip) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, themeColors.accent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (store.isVip) "👑 VIP" else "موثق ✓",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
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
                            Text(String.format("%.1f", store.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // 2. Overlapping Avatar & Content Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                // Header with Overlapping Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, themeColors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoImg.isNotBlank()) {
                            SmartAsyncImage(
                                model = logoImg,
                                contentDescription = store.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🏪", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = store.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("✔️", fontSize = 11.sp, color = themeColors.accent)
                            }
                        }
                        val descText = store.description.ifBlank { "متجر تجاري معتمد" }
                        Text(
                            text = descText,
                            fontSize = 10.sp,
                            color = themeColors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Location & Hours Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-10).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        val locText = store.localNeighborhood.ifBlank { "اليمن" }
                        Text(
                            text = locText,
                            fontSize = 9.5.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val hours = store.workingHours.ifBlank { "9:00 ص - 10:00 م" }
                    Text(
                        text = "⏰ $hours",
                        fontSize = 9.5.sp,
                        color = themeColors.textSecondary,
                        maxLines = 1
                    )
                }

                // 3. Action Buttons Row: [التفاصيل] [التقييمات] [اتصال / محادثة]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-4).dp)
                        .padding(bottom = 6.dp),
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

                    if (store.phone.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                        ) {
                            Text("اتصال 📞", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onChatClick,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                        ) {
                            Text("محادثة 💬", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showReviewsDialog) {
        GenericEntityReviewsDialog(
            title = store.name,
            rating = store.rating,
            numReviews = store.numReviews,
            themeColors = themeColors,
            onDismiss = { showReviewsDialog = false }
        )
    }
}
