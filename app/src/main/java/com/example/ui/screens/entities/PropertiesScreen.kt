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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedCityId by remember { mutableStateOf("الكل") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") } // rent, sale
    var selectedMinRating by remember { mutableStateOf(0.0f) }

    val categories = listOf("الكل", "شقة", "بيت ومستقل", "محل تجاري", "أرض")

    // Determine loading state from properties list presence
    val isLoading = remember(properties) { properties.isEmpty() }

    val filteredProperties = remember(properties, searchQuery, selectedCategory, selectedCityId, selectedTypeFilter, selectedMinRating) {
        properties.filter { prop ->
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

            matchesSearch && matchesCat && matchesType && matchesCity && matchesRating
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
            placeholder = { Text("بحث عن شقة، أرض، بيت للإيجار أو البيع... 🏠", fontSize = 11.sp, color = Color.Gray) },
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
        Text("🏙️ اختر المدينة العقارية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCityId == "الكل",
                    onClick = { selectedCityId = "الكل" },
                    label = { Text("كل المحافظات 🇾🇪", fontSize = 10.sp) },
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

        // 🏷️ Property Type Filter LazyRow
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
            Text("🏠 العقارات والأراضي اليمنيّة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Text("${filteredProperties.size} عقار", fontSize = 11.sp, color = Color.LightGray)
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
                    Text("جاري تحميل قائمة العقارات... 🏡", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        } else if (filteredProperties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏡", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد عقارات مطابقة للتصفية الحالية", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = "الكل"
                            selectedCityId = "الكل"
                            selectedTypeFilter = "الكل"
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
                items(filteredProperties, key = { it.id }) { prop ->
                    PropertyCard(
                        property = prop,
                        themeColors = themeColors,
                        onClick = { onPropertyClick(prop) },
                        onChatClick = { onChatClick(prop) },
                        onRequestInspectionClick = { onRequestInspectionClick(prop) }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyEntity,
    themeColors: VisualThemePalette,
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onRequestInspectionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(30.dp)
                    ) {
                        Text("طلب معاينة 👁️", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "تواصل", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
