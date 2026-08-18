package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PropertyEntity
import com.example.rememberBase64Bitmap
import com.example.ui.MainViewModel
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = listOf("الكل", "للإيجار", "للبيع", "شقق", "منازل وفيلا", "أراضي ومحلات")

    val filteredProperties = remember(properties, searchQuery, selectedCategory) {
        properties.filter { prop ->
            val matchesSearch = searchQuery.isBlank() ||
                    prop.title.contains(searchQuery, ignoreCase = true) ||
                    prop.description.contains(searchQuery, ignoreCase = true) ||
                    prop.localNeighborhood.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "الكل" ||
                    (selectedCategory == "للإيجار" && prop.type == "rent") ||
                    (selectedCategory == "للبيع" && prop.type == "sale") ||
                    prop.propertyType.contains(selectedCategory, ignoreCase = true)

            matchesSearch && matchesCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories.size) { idx ->
                val cat = categories[idx]
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

        if (filteredProperties.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏠", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد عقارات مطابقة للبحث", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
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
    val bitmap = rememberBase64Bitmap(imageSource)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
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
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = property.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageSource.startsWith("http")) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = property.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
                        Text("${property.rating}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    text = "${property.price} ${property.currency}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
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
                        modifier = Modifier.weight(1f).height(30.dp)
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
