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
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.components.GenericEntityReviewsDialog
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun MedicalCentersScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onMedicalCenterClick: (ProviderEntity) -> Unit,
    onChatClick: (ProviderEntity) -> Unit,
    onBookAppointmentClick: (ProviderEntity) -> Unit
) {
    val providers by viewModel.providers.collectAsState()
    val isProvidersLoading by viewModel.isProvidersLoading.collectAsState()
    val cities by viewModel.cities.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedCityId by remember { mutableStateOf("الكل") }
    var selectedMinRating by remember { mutableStateOf(0.0f) }

    val categories = listOf("الكل", "مستشفيات", "عيادات تخصصية", "صيدليات", "مختبرات تحاليل", "مراكز أشعة")

    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isAdminUser = adminRole == "ADMIN" || adminRole == "SUPER_ADMIN" || adminRole == "MAIN_ADMIN" || adminRole == "OWNER"
    val isLoggedIn = currentUserId.isNotBlank() && currentUserId != "guest"

    var showCreateMedicalDialog by remember { mutableStateOf(false) }

    val medicalProviders = remember(providers, searchQuery, selectedCategory, selectedCityId, selectedMinRating, currentUserId, adminRole) {
        providers.filter { provider ->
            val isApprovedOrOwner = provider.subscriptionStatus == "APPROVED" || provider.isVerified || provider.phone == currentUserId || isAdminUser
            val isMedical = provider.categoryId == "medical" ||
                    provider.categoryId == "health" ||
                    provider.categoryId.contains("med", ignoreCase = true) ||
                    provider.categoryId.contains("health", ignoreCase = true) ||
                    provider.categoryId.contains("طبي", ignoreCase = true) ||
                    provider.categoryId.contains("مستشفى", ignoreCase = true) ||
                    provider.categoryId.contains("صيدل", ignoreCase = true) ||
                    provider.profession.contains("طب", ignoreCase = true) ||
                    provider.profession.contains("صيدل", ignoreCase = true) ||
                    provider.profession.contains("مستشفى", ignoreCase = true) ||
                    provider.profession.contains("عيادة", ignoreCase = true) ||
                    provider.profession.contains("مختبر", ignoreCase = true) ||
                    provider.profession.contains("أشعة", ignoreCase = true) ||
                    provider.specialization.contains("طب", ignoreCase = true) ||
                    provider.specialization.contains("صيدل", ignoreCase = true) ||
                    provider.specialization.contains("عيادة", ignoreCase = true) ||
                    provider.customCategoryName.contains("طبي", ignoreCase = true) ||
                    provider.customCategoryName.contains("مستشفى", ignoreCase = true) ||
                    provider.customCategoryName.contains("صيدلية", ignoreCase = true)

            val matchesSearch = searchQuery.isBlank() ||
                    provider.name.contains(searchQuery, ignoreCase = true) ||
                    provider.profession.contains(searchQuery, ignoreCase = true) ||
                    provider.specialization.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    provider.specialization.contains(selectedCategory, ignoreCase = true) ||
                    provider.profession.contains(selectedCategory, ignoreCase = true) ||
                    provider.customCategoryName.contains(selectedCategory, ignoreCase = true)

            val matchesCity = selectedCityId == "الكل" || provider.cityId == selectedCityId

            val matchesRating = provider.rating >= selectedMinRating

            isApprovedOrOwner && isMedical && matchesSearch && matchesCat && matchesCity && matchesRating
        }
    }

    var showGuestDialog by remember { mutableStateOf(false) }

    if (showCreateMedicalDialog) {
        com.example.StoreCreateEditDialog(
            store = null,
            viewModel = viewModel,
            themeColors = themeColors,
            sectionId = "medical",
            onDismiss = { showCreateMedicalDialog = false }
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
        items = medicalProviders,
        isLoading = isProvidersLoading,
        title = "المراكز الطبية والعيادات التخصصية",
        titleIcon = "🏥",
        searchPlaceholder = "بحث عن مركز طبي، عيادة، أو طبيب... 🏥",
        categories = categories,
        cities = cities,
        onSearchQueryChanged = { searchQuery = it },
        onCategorySelected = { selectedCategory = it },
        onCitySelected = { selectedCityId = it },
        onMinRatingSelected = { selectedMinRating = it },
        emptyMessage = "لا توجد مراكز طبية مطابقة للتصفية الحالية",
        extraHeaderContent = {
            Button(
                onClick = { showCreateMedicalDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text("➕ تسجيل وإضافة مركز طبي / صيدلية", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        itemContent = { provider ->
            MedicalCenterCard(
                provider = provider,
                themeColors = themeColors,
                onClick = { onMedicalCenterClick(provider) },
                onChatClick = {
                    if (!isLoggedIn) {
                        showGuestDialog = true
                    } else {
                        onChatClick(provider)
                    }
                },
                onBookAppointmentClick = { onBookAppointmentClick(provider) }
            )
        }
    )
}

@Composable
fun MedicalCenterCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    isLoggedIn: Boolean = true,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onBookAppointmentClick: () -> Unit
) {
    val context = LocalContext.current
    var showReviewsDialog by remember { mutableStateOf(false) }

    val isVerified = provider.isVerified || provider.subscriptionStatus == "APPROVED" || provider.isVip
    val coverImg = provider.coverImage.ifBlank { "" }
    val logoImg = provider.profileImage.ifBlank { "" }

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
                        contentDescription = provider.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏥", fontSize = 34.sp)
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
                    if (isVerified || provider.isVip) {
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
                                    text = if (provider.isVip) "👑 VIP" else "مركز موثق ✓",
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
                            Text(String.format("%.1f", provider.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                contentDescription = provider.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🏥", fontSize = 24.sp)
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
                                text = provider.name,
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
                        val specText = provider.specialization.ifBlank { provider.profession.ifBlank { "مركز طبي متكامل" } }
                        Text(
                            text = specText,
                            fontSize = 10.sp,
                            color = themeColors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Location & Availability Row
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
                        val locText = provider.localNeighborhood.ifBlank { provider.area.ifBlank { "اليمن" } }
                        Text(
                            text = locText,
                            fontSize = 9.5.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (provider.isAvailable) "🟢 متاح الآن" else "⚪ مغلق مؤقتاً",
                        fontSize = 9.5.sp,
                        color = if (provider.isAvailable) Color(0xFF10B981) else Color.LightGray,
                        maxLines = 1
                    )
                }

                // 3. Action Buttons Row: [التفاصيل] [التقييمات] [حجز موعد]
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

                    Button(
                        onClick = onBookAppointmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("حجز موعد 🩺", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReviewsDialog) {
        GenericEntityReviewsDialog(
            title = provider.name,
            rating = provider.rating,
            numReviews = provider.numReviews,
            themeColors = themeColors,
            onDismiss = { showReviewsDialog = false }
        )
    }
}
