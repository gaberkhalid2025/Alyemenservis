package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
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
    val imageSource = provider.coverImage.ifBlank { provider.profileImage }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.DarkGray)
            ) {
                if (imageSource.isNotBlank()) {
                    SmartAsyncImage(
                        model = imageSource,
                        contentDescription = provider.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🏥", fontSize = 32.sp)
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(String.format("%.1f", provider.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = provider.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = provider.specialization.ifBlank { provider.profession.ifBlank { "مركز طبي متكامل" } },
                    fontSize = 10.sp,
                    color = themeColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⏱️ متاح للاستشارة",
                        fontSize = 9.sp,
                        color = if (provider.isAvailable) Color(0xFF10B981) else Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "📍 ${provider.localNeighborhood.ifBlank { provider.area.ifBlank { "اليمن" } }}",
                    fontSize = 9.5.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBookAppointmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("حجز موعد 🩺", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (provider.phone.isNotBlank()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Phone,
                                contentDescription = "اتصال",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "محادثة",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
