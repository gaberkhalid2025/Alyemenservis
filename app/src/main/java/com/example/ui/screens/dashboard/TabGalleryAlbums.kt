package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📸 Modular Tab: Gallery & Photo Albums (إدارة ألبومات الصور والمعرض المرئي)
 */
@Composable
fun TabGalleryAlbums(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadStatus by remember { mutableStateOf("") }
    
    // Extra visual photos for custom albums
    var albumPhotos by remember { 
        mutableStateOf<List<String>>(
            account.rawStore?.images ?: emptyList()
        ) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "📸 ألبوم الصور ومعرض أعمال المنشأة",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )
        
        Text(
            text = "يمكنك رفع صور مخصصة إضافية لمقر المنشأة، التجهيزات الطبية، منيو المطعم، أو تصاميم ونماذج العقارات والأعمال لعرضها لعملائك مباشرة.",
            fontSize = 10.5.sp,
            color = Color.LightGray,
            lineHeight = 15.sp
        )

        UnifiedImagePicker(
            label = "📤 إضافة صورة جديدة للألبوم",
            imageUrl = "",
            onImageSelected = { uri ->
                uploadStatus = "جاري معالجة الصورة..."
                scope.launch {
                    try {
                        val path = com.example.util.FirebaseStorageUploader.getStorePhotoPath(account.id, System.currentTimeMillis().toInt())
                        val url = viewModel.uploadImageStringOrUri(context, uri.toString(), path)
                        if (url.isNotEmpty() && url.startsWith("http")) {
                            val updatedPhotos = albumPhotos + url
                            albumPhotos = updatedPhotos
                            uploadStatus = "🎉 تم رفع الصورة وإضافتها للألبوم بنجاح!"
                            // Persist store update
                            val raw = account.rawStore
                            if (raw != null) {
                                viewModel.saveStore(raw.copy(images = updatedPhotos))
                            }
                        } else {
                            uploadStatus = "❌ فشل رفع الصورة، يرجى التحقق من اتصال الإنترنت."
                        }
                    } catch (e: Exception) {
                        uploadStatus = "❌ خطأ أثناء الرفع: ${e.localizedMessage}"
                    }
                }
            },
            themeColors = themeColors
        )

        if (uploadStatus.isNotEmpty()) {
            Text(
                text = uploadStatus,
                fontSize = 11.sp,
                color = if (uploadStatus.startsWith("❌")) Color(0xFFEF5350) else themeColors.accent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        if (albumPhotos.isEmpty()) {
            UnifiedEmptyState(
                icon = "📸",
                title = "المعرض فارغ حالياً",
                description = "لم تقم برفع أي صور للمقر أو المعرض الإضافي بعد. قم برفع صورة لتعزيز ثقة عملائك بك.",
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(albumPhotos) { img ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = img,
                            contentDescription = "صورة في الألبوم",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Simple delete button overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable {
                                    val filtered = albumPhotos.filter { it != img }
                                    albumPhotos = filtered
                                    val raw = account.rawStore
                                    if (raw != null) {
                                        viewModel.saveStore(raw.copy(images = filtered))
                                    }
                                    Toast
                                        .makeText(context, "🗑️ تم إزالة الصورة بنجاح!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗑️ حذف", color = Color(0xFFEF5350), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
