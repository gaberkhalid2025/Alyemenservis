package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.VisualThemePalette

/**
 * 🔍 MapFilterBar
 * Interactive top filter bar containing:
 * - Search text field
 * - City selector dropdown (Sana'a, Aden, Taiz, etc.)
 * - Categorical filter chips (All, Technicians, Stores, Restaurants, Medical, Properties)
 * - Advanced distance range and rating score filters
 */
@Composable
fun MapFilterBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    maxDistanceKm: Float,
    onMaxDistanceChange: (Float) -> Unit,
    minRating: Float,
    onMinRatingChange: (Float) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var isCityMenuExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        Triple("ALL", "الكل", "🌐"),
        Triple("PROVIDERS", "فنيون 👷", "👷"),
        Triple("STORES", "متاجر 🏪", "🏪"),
        Triple("RESTAURANTS", "مطاعم 🍔", "🍔"),
        Triple("MEDICAL", "طبية 🏥", "🏥"),
        Triple("PROPERTIES", "عقارات 🏠", "🏠")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search & City Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث عن خدمة أو فني...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("map_search_input")
            )

            // City Selector Box
            Box {
                Surface(
                    onClick = { isCityMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        Text(
                            text = selectedCity.ifEmpty { "المدينة" },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }

                DropdownMenu(
                    expanded = isCityMenuExpanded,
                    onDismissRequest = { isCityMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    OfflineMapManager.MAJOR_YEMENI_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.nameAr, color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                onCitySelected(city.nameAr)
                                isCityMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { (catKey, catLabel, _) ->
                val isSelected = selectedCategory == catKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(catKey) },
                    label = {
                        Text(
                            catLabel,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        containerColor = Color(0xFF1E293B),
                        selectedLabelColor = Color(0xFF0F172A),
                        labelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_chip_$catKey")
                )
            }
        }

        // Distance & Rating Filter Row (Mobile friendly)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📍 المسافة:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            val distanceOptions = listOf(
                Pair(100.0f, "الكل"),
                Pair(5.0f, "5 كم 🏃"),
                Pair(15.0f, "15 كم 🚗"),
                Pair(30.0f, "30 كم 🚘")
            )
            
            distanceOptions.forEach { (dist, label) ->
                val isSelected = maxDistanceKm == dist
                FilterChip(
                    selected = isSelected,
                    onClick = { onMaxDistanceChange(dist) },
                    label = { Text(label, fontSize = 11.sp, color = if (isSelected) Color(0xFF0F172A) else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        containerColor = Color(0xFF1E293B)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.width(6.dp))

            Text("⭐️ التقييم:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            val ratingOptions = listOf(
                Pair(0.0f, "الكل"),
                Pair(4.0f, "4.0+ ⭐️"),
                Pair(4.5f, "4.5+ ⭐️"),
                Pair(4.8f, "4.8+ ⭐️")
            )

            ratingOptions.forEach { (rating, label) ->
                val isSelected = minRating == rating
                FilterChip(
                    selected = isSelected,
                    onClick = { onMinRatingChange(rating) },
                    label = { Text(label, fontSize = 11.sp, color = if (isSelected) Color(0xFF0F172A) else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        containerColor = Color(0xFF1E293B)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
