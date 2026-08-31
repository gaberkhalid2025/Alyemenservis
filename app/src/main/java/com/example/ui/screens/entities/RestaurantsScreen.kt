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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

@Composable
fun RestaurantsScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onRestaurantClick: (StoreEntity) -> Unit,
    onChatClick: (StoreEntity) -> Unit,
    onOrderMealClick: (StoreEntity) -> Unit
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
    var showCreateRestaurantDialog by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "وجبات شعبية", "وجبات سريعة", "مشويات", "حلويات وعصائر", "كافيهات")

    val isLoading = remember(stores) { stores.isEmpty() }

    val restaurantStores = remember(stores, searchQuery, selectedCategory, selectedCityId, selectedMinRating, currentUserId, adminRole) {
        stores.filter { store ->
            val isApprovedOrOwner = store.isApproved || store.ownerId == currentUserId || isAdminUser
            val isRestaurant = (store.sectionId == "restaurants" ||
                    store.categoryId.contains("rest", ignoreCase = true) ||
                    store.categoryId.contains("food", ignoreCase = true) ||
                    store.categoryId.contains("مطعم", ignoreCase = true) ||
                    store.categoryId.contains("وجب", ignoreCase = true) ||
                    store.name.contains("مطعم", ignoreCase = true) ||
                    store.name.contains("كافيه", ignoreCase = true) ||
                    store.name.contains("وجبات", ignoreCase = true)) && !store.isDeleted

            val matchesSearch = searchQuery.isBlank() ||
                    store.name.contains(searchQuery, ignoreCase = true) ||
                    store.description.contains(searchQuery, ignoreCase = true) ||
                    store.localNeighborhood.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    store.categoryId.contains(selectedCategory, ignoreCase = true) ||
                    store.name.contains(selectedCategory, ignoreCase = true)

            val matchesCity = selectedCityId == "الكل" || store.cityId == selectedCityId

            val matchesRating = store.rating >= selectedMinRating

            isApprovedOrOwner && isRestaurant && matchesSearch && matchesCat && matchesCity && matchesRating
        }
    }

    var showGuestDialog by remember { mutableStateOf(false) }

    if (showCreateRestaurantDialog) {
        com.example.StoreCreateEditDialog(
            store = null,
            viewModel = viewModel,
            themeColors = themeColors,
            sectionId = "restaurants",
            onDismiss = { showCreateRestaurantDialog = false }
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
        items = restaurantStores,
        isLoading = isLoading,
        title = "المطاعم والكافيهات اليمنيّة",
        titleIcon = "🍔",
        searchPlaceholder = "بحث عن مطعم، كافيه، أو وجبة... 🍔",
        categories = categories,
        cities = cities,
        onSearchQueryChanged = { searchQuery = it },
        onCategorySelected = { selectedCategory = it },
        onCitySelected = { selectedCityId = it },
        onMinRatingSelected = { selectedMinRating = it },
        emptyMessage = "لا توجد مطاعم مطابقة للبحث",
        extraHeaderContent = {
            Button(
                onClick = { showCreateRestaurantDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Text("➕ تسجيل وإضافة مطعم / كافيه جديد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        itemContent = { rest ->
            RestaurantCard(
                restaurant = rest,
                themeColors = themeColors,
                onClick = { onRestaurantClick(rest) },
                onChatClick = {
                    if (!isLoggedIn) {
                        showGuestDialog = true
                    } else {
                        onChatClick(rest)
                    }
                },
                onOrderMealClick = { onOrderMealClick(rest) }
            )
        }
    )
}

@Composable
fun RestaurantCard(
    restaurant: StoreEntity,
    themeColors: VisualThemePalette,
    isLoggedIn: Boolean = true,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onOrderMealClick: () -> Unit
) {
    val imageSource = restaurant.coverImage.ifBlank { restaurant.logoImage }

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
                        contentDescription = restaurant.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🍔", fontSize = 32.sp)
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(String.format("%.1f", restaurant.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    text = restaurant.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "⏰ الدوام: " + restaurant.workingHours,
                    fontSize = 9.5.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "📍 ${restaurant.localNeighborhood.ifBlank { "اليمن" }}",
                    fontSize = 10.sp,
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
                        onClick = onOrderMealClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("اطلب وجبتك 🍕", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (restaurant.phone.isNotBlank()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${restaurant.phone}"))
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
