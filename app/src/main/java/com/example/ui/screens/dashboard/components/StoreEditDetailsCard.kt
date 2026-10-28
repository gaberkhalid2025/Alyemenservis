package com.example.ui.screens.dashboard.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.ui.*
import com.example.ui.components.SmartAsyncImage
import com.example.utils.FirebaseStorageUploader
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📝 StoreEditDetailsCard - بطاقة التحكم والإدارة لصاحب المحل / الفني (Vendor Portal)
 * تدعم:
 * - تغيير صورة الغلاف وصورة الشعار مباشرة من الاستوديو
 * - ضغط الصور التلقائي إلى WebP بحجم 60KB لتوفير الباقة وسعة Firebase
 * - تعديل بيانات الاسم والوصف والعنوان ورقم الهاتف
 */
@Composable
fun StoreEditDetailsCard(
    store: StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var editName by remember(store) { mutableStateOf(store.name) }
    var editDesc by remember(store) { mutableStateOf(store.description) }
    var editAddress by remember(store) { mutableStateOf(store.localNeighborhood) }
    var editPhone by remember(store) { mutableStateOf(store.phone) }
    var editLogo by remember(store) { mutableStateOf(store.logoImage) }
    var editCover by remember(store) { mutableStateOf(store.coverImage) }

    var isUploadingCover by remember { mutableStateOf(false) }
    var isUploadingLogo by remember { mutableStateOf(false) }

    // لاقط صورة الغلاف مع الضغط الذكي إلى WebP 60KB
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingCover = true
                val path = "stores/${store.id.ifEmpty { "store" }}_cover_${System.currentTimeMillis()}.webp"
                val result = FirebaseStorageUploader.uploadImageUri(
                    context = context,
                    uri = uri,
                    storagePath = path,
                    maxDimension = 640,
                    maxSizeBytes = 60 * 1024L
                )
                isUploadingCover = false
                result.onSuccess { url ->
                    editCover = url
                    Toast.makeText(context, "تم ضغط ورفع الغلاف بنجاح بصيغة WebP (60KB) ✅", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "فشل رفع الغلاف: ${err.message ?: "خطأ في الشبكة"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // لاقط صورة الشعار مع الضغط الذكي إلى WebP 50KB
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingLogo = true
                val path = "stores/${store.id.ifEmpty { "store" }}_logo_${System.currentTimeMillis()}.webp"
                val result = FirebaseStorageUploader.uploadImageUri(
                    context = context,
                    uri = uri,
                    storagePath = path,
                    maxDimension = 400,
                    maxSizeBytes = 50 * 1024L
                )
                isUploadingLogo = false
                result.onSuccess { url ->
                    editLogo = url
                    Toast.makeText(context, "تم ضغط ورفع الشعار بنجاح بصيغة WebP (50KB) ✅", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "فشل رفع الشعار: ${err.message ?: "خطأ في الشبكة"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "🛠️ إدارة صور وهوية المنشأة (WebP 60KB):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )

            // 1. معاينة وتحديث الغلاف والشعار
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
            ) {
                if (editCover.isNotBlank()) {
                    SmartAsyncImage(
                        model = editCover,
                        contentDescription = "غلاف المتجر",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("📷 لا يوجد صورة غلاف حالياً", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        coverPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    if (isUploadingCover) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("تغيير الغلاف 📷", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, themeColors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (editLogo.isNotBlank()) {
                        SmartAsyncImage(
                            model = editLogo,
                            contentDescription = "شعار المتجر",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    logoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploadingLogo) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColors.accent, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(16.dp))
                        Text("تغيير الشعار 🖼️ (نظام الضغط الذكي 50KB)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Text("📝 تعديل بيانات وموقع المتجر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("اسم المتجر / المحل التجاري") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
            )

            OutlinedTextField(
                value = editDesc,
                onValueChange = { editDesc = it },
                label = { Text("وصف النشاط والخدمات والمنتجات المقدمة") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
            )

            OutlinedTextField(
                value = editAddress,
                onValueChange = { editAddress = it },
                label = { Text("العنوان بالتفصيل (المحافظة - المديرية - الشارع)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
            )

            OutlinedTextField(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = { Text("رقم الهاتف أو الواتساب") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
            )

            Button(
                onClick = {
                    if (editName.trim().isEmpty() || editPhone.trim().isEmpty()) {
                        viewModel.triggerNotification("⚠️ الاسم والهاتف حقول إجبارية!")
                    } else {
                        viewModel.updateStoreEntity(
                            store.copy(
                                name = editName.trim(),
                                description = editDesc.trim(),
                                localNeighborhood = editAddress.trim(),
                                phone = editPhone.trim(),
                                coverImage = editCover,
                                logoImage = editLogo
                            )
                        )
                        Toast.makeText(context, "✅ تم حفظ التغييرات وصور المتجر بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("حفظ التحديثات والصور 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
