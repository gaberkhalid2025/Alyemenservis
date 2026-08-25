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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedCityId by remember { mutableStateOf("الكل") }
    var selectedMinRating by remember { mutableStateOf(0.0f) }

    val categories = listOf("الكل", "وجبات شعبية", "وجبات سريعة", "مشويات", "حلويات وعصائر", "كافيهات")

    val isLoading = remember(stores) { stores.isEmpty() }

    val restaurantStores = remember(stores, searchQuery, selectedCategory, selectedCityId, selectedMinRating) {
        stores.filter { store ->
            val isRestaurant = store.sectionId == "restaurants" ||
                    store.categoryId.contains("rest", ignoreCase = true) ||
                    store.categoryId.contains("food", ignoreCase = true) ||
                    store.categoryId.contains("مطعم", ignoreCase = true) ||
                    store.categoryId.contains("وجب", ignoreCase = true) ||
                    store.name.contains("مطعم", ignoreCase = true) ||
                    store.name.contains("كافيه", ignoreCase = true) ||
                    store.name.contains("وجبات", ignoreCase = true)

            val matchesSearch = searchQuery.isBlank() ||
                    store.name.contains(searchQuery, ignoreCase = true) ||
                    store.description.contains(searchQuery, ignoreCase = true) ||
                    store.localNeighborhood.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    store.categoryId.contains(selectedCategory, ignoreCase = true) ||
                    store.name.contains(selectedCategory, ignoreCase = true)

            val matchesCity = selectedCityId == "الكل" || store.cityId == selectedCityId

            val matchesRating = store.rating >= selectedMinRating

            isRestaurant && matchesSearch && matchesCat && matchesCity && matchesRating
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن مطعم، كافيه، أو وجبة... 🍔", fontSize = 11.sp, color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.accent,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = themeColors.surface,
                unfocusedContainerColor = themeColors.surface
            )
        )

        // 🏙️ City Filter LazyRow
        Text("🏙️ اختر المدينة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCityId == "الكل",
                    onClick = { selectedCityId = "الكل" },
                    label = { Text("كل المدن 🇾🇪", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.accent,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            items(cities) { city ->
                FilterChip(
                    selected = selectedCityId == city.id,
                    onClick = { selectedCityId = city.id },
                    label = { Text("${city.icon} ${city.nameAr}", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.accent,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // ⭐ Rating Filter LazyRow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⭐ التقييم الأدنى:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
            listOf(0.0f, 3.0f, 4.0f, 4.5f).forEach { stars ->
                FilterChip(
                    selected = selectedMinRating == stars,
                    onClick = { selectedMinRating = stars },
                    label = { Text(if (stars == 0.0f) "الكل" else "⭐ $stars+", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.accent,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // 🏷️ Category Filter LazyRow
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) themeColors.accent else themeColors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🍔 المطاعم والكافيهات اليمنيّة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Text("${restaurantStores.size} مطعم", fontSize = 11.sp, color = Color.LightGray)
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColors.accent)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("جاري تحميل قائمة المطاعم... 🍔", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        } else if (restaurantStores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍔", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد مطاعم مطابقة للبحث", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = "الكل"
                            selectedCityId = "الكل"
                            selectedMinRating = 0.0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = themeColors.accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة تعيين الفلاتر", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(restaurantStores, key = { it.id }) { rest ->
                    RestaurantCard(
                        restaurant = rest,
                        themeColors = themeColors,
                        onClick = { onRestaurantClick(rest) },
                        onChatClick = { onChatClick(rest) },
                        onOrderMealClick = { onOrderMealClick(rest) }
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: StoreEntity,
    themeColors: VisualThemePalette,
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onOrderMealClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Text("اطلب وجبتك 🍕", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "محادثة", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
