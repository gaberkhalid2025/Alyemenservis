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
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
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
                    onClick = { showCreatePropertyDialog = true },
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
    val imageSource = property.images.firstOrNull() ?: ""

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
                        contentDescription = property.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🏠", fontSize = 32.sp)
                    }
                }

                Surface(
                    color = if (property.type == "rent") Color(0xFF3B82F6) else Color(0xFF10B981),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (property.type == "rent") "إيجار" else "بيع",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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
                        Text(String.format("%.1f", property.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    text = property.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${property.price.toInt()} ${property.currency}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )

                Text(
                    text = "🏘️ نوع: " + when(property.propertyType) {
                        "apartment" -> "شقة"
                        "house" -> "منزل"
                        "shop" -> "محل تجاري"
                        "land" -> "أرض"
                        else -> property.propertyType
                    },
                    fontSize = 9.5.sp,
                    color = Color.LightGray
                )

                Text(
                    text = "📍 ${property.localNeighborhood.ifBlank { "اليمن" }}",
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
                        onClick = onRequestInspectionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("طلب معاينة 👁️", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (property.phone.isNotBlank()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${property.phone}"))
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

                    if (isLoggedIn) {
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
}
