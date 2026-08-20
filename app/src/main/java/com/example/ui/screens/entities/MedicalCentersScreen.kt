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
import com.example.data.ProviderEntity
import com.example.rememberBase64Bitmap
import com.example.ui.MainViewModel
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = listOf("الكل", "مستشفيات", "عيادات تخصصية", "صيدليات", "مختبرات تحاليل", "مراكز أشعة")

    val medicalProviders = remember(providers, searchQuery, selectedCategory) {
        providers.filter { provider ->
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
                    provider.profession.contains(selectedCategory, ignoreCase = true)

            isMedical && matchesSearch && matchesCat
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
            placeholder = { Text("بحث عن مركز طبي، عيادة، أو طبيب... 🏥", fontSize = 11.sp, color = Color.Gray) },
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
            Text("🏥 المراكز الطبية والعيادات التخصصية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Text("${medicalProviders.size} مركز", fontSize = 11.sp, color = Color.LightGray)
        }

        if (medicalProviders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏥", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد مراكز طبية مطابقة للبحث", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(medicalProviders, key = { it.id }) { provider ->
                    MedicalCenterCard(
                        provider = provider,
                        themeColors = themeColors,
                        onClick = { onMedicalCenterClick(provider) },
                        onChatClick = { onChatClick(provider) },
                        onBookAppointmentClick = { onBookAppointmentClick(provider) }
                    )
                }
            }
        }
    }
}

@Composable
fun MedicalCenterCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onBookAppointmentClick: () -> Unit
) {
    val imageSource = provider.coverImage.ifBlank { provider.profileImage }
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
                        contentDescription = provider.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageSource.startsWith("http")) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = provider.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🏥", fontSize = 32.sp)
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
                        Text("${provider.rating}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

                Text(
                    text = "📍 ${provider.localNeighborhood.ifBlank { provider.area.ifBlank { "اليمن" } }}",
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
                        onClick = onBookAppointmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text("حجز موعد 🩺", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "استشارة", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
