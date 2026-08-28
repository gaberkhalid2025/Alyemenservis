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
 */
@Composable
fun MapFilterBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
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
                placeholder = { 
                    Text(
                        text = "بحث عن خدمة، فني، مركز أو متجر...", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "بحث", 
                        tint = Color(0xFF00E5FF), 
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Clear, 
                                contentDescription = "مسح", 
                                tint = Color(0xFF00E5FF), 
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00E5FF),
                    focusedPlaceholderColor = Color(0xFF94A3B8),
                    unfocusedPlaceholderColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
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
                    DropdownMenuItem(
                        text = { Text("جميع المدن والمحافظات", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onCitySelected("الكل")
                            isCityMenuExpanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        }
                    )
                    OfflineMapManager.MAJOR_YEMENI_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.nameAr, color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                onCitySelected(city.nameAr)
                                isCityMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
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
    }
}
