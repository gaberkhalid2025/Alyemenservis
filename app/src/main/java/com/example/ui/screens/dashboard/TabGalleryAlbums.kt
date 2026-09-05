package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.entities.GalleryAlbumEntity
import com.example.ui.components.SmartAsyncImage
import com.example.ui.screens.dashboard.components.UnifiedEmptyState
import com.example.ui.screens.dashboard.components.UnifiedImagePicker
import com.example.utils.VisualThemePalette

@Composable
fun TabGalleryAlbums(
    albums: List<GalleryAlbumEntity>,
    themeColors: VisualThemePalette,
    onAddPhoto: (imageUrl: String) -> Unit,
    onDeletePhoto: (id: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newPhotoUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📸 معرض الصور والأعمال (${albums.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إضافة صورة 📸", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (albums.isEmpty()) {
            UnifiedEmptyState(
                title = "معرض الصور فارغ",
                description = "أضف صوراً لأعمالك السابقة لجذب ثقة العملاء.",
                iconText = "📸",
                actionLabel = "رفع صورة جديدة 📸",
                onActionClick = { showAddDialog = true },
                themeColors = themeColors
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                items(albums, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SmartAsyncImage(
                                model = item.imageUrls.firstOrNull() ?: "",
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { onDeletePhoto(item.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Text(text = "🗑️", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة صورة جديدة للمعرض", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                UnifiedImagePicker(
                    currentImageUrl = newPhotoUrl,
                    label = "اختر صورة العمل من المعرض",
                    themeColors = themeColors,
                    onImageSelected = { newPhotoUrl = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhotoUrl.isNotBlank()) {
                            onAddPhoto(newPhotoUrl)
                            newPhotoUrl = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ الصورة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
