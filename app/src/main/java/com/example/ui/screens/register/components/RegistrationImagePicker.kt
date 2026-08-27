package com.example.ui.screens.register.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.utils.VisualThemePalette

/**
 * 📷 RegistrationImagePicker - مكون اختيار ومعاينة الصور مع نسبة التقدم ورفع الحساب
 */
@Composable
fun RegistrationImagePicker(
    title: String = "صور المعرض / الهوية / الترخيص",
    subtitle: String = "يمكنك إضافة حتى 5 صور معتمدة لتوثيق ملفك",
    imagesUris: List<Uri>,
    onImagesSelected: (List<Uri>) -> Unit,
    onImageRemoved: (Int) -> Unit,
    maxImages: Int = 5,
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val combined = (imagesUris + uris).take(maxImages)
            onImagesSelected(combined)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text(subtitle, fontSize = 10.5.sp, color = Color.Gray)
            }
            Text(
                "${imagesUris.size}/$maxImages",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
        }

        if (isUploading) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { uploadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = themeColors.accent,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    "جاري رفع الصور إلى السحابة... ${(uploadProgress * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = themeColors.accent
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imagesUris.size < maxImages) {
                item {
                    OutlinedCard(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF0F172A)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(themeColors.accent)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة صورة", tint = themeColors.accent)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("إضافة", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            itemsIndexed(imagesUris) { index, uri ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "معاينة صورة $index",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { onImageRemoved(index) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "حذف الصورة", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
