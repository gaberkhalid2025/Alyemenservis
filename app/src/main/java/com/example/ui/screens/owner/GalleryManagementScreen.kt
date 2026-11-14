package com.example.ui.screens.owner

import android.widget.Toast
import com.example.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.data.BusinessType
import com.example.data.UnifiedBusinessAccount
import com.example.data.repositories.GalleryRepositoryImpl
import com.example.domain.entities.GalleryAlbumEntity
import com.example.rememberBase64Bitmap
import com.example.ui.MainViewModel
import com.example.utils.AppConstants
import com.example.utils.FirebaseStorageUploader
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class GalleryImageItem(
    val id: String,
    val urlOrBase64: String,
    val isPrimary: Boolean = false
)

@Composable
fun GalleryManagementScreen(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val galleryRepository = remember(appContext) { GalleryRepositoryImpl(appContext) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var showAddImageDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var primaryUrl by remember(account.coverImage, account.logoImage) {
        mutableStateOf(account.coverImage.ifBlank { account.logoImage })
    }
    val remoteAlbums by galleryRepository.getOwnerGallery(account.id).collectAsState(initial = emptyList())

    val galleryList = remember(remoteAlbums, primaryUrl) {
        val items = mutableListOf<GalleryImageItem>()
        if (primaryUrl.isNotBlank()) {
            items.add(GalleryImageItem("primary_cover", primaryUrl, isPrimary = true))
        }
        remoteAlbums.forEach { album ->
            album.imageUrls.filter { it.isNotBlank() && it != primaryUrl }.forEachIndexed { idx, url ->
                items.add(
                    GalleryImageItem(
                        id = if (idx == 0) album.id else "${album.id}_$idx",
                        urlOrBase64 = url,
                        isPrimary = false
                    )
                )
            }
        }
        items
    }

    val ownerCollection = remember(account.businessType) {
        when (account.businessType) {
            BusinessType.TECHNICIAN -> AppConstants.COL_PROVIDERS
            BusinessType.REAL_ESTATE -> AppConstants.COL_PROPERTIES
            BusinessType.JOB_POSTER -> AppConstants.COL_JOBS
            else -> AppConstants.COL_STORES
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val uploadRes = FirebaseStorageUploader.uploadImageUri(
                    context = appContext,
                    uri = uri,
                    storagePath = "galleries/${account.id}/${System.currentTimeMillis()}.webp"
                )
                uploadRes.onSuccess { downloadUrl ->
                    val album = GalleryAlbumEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        ownerId = account.id,
                        title = "صورة معرض",
                        imageUrls = listOf(downloadUrl)
                    )
                    galleryRepository.saveGalleryAlbum(album).onSuccess {
                        Toast.makeText(context, "تم رفع وحفظ الصورة في المعرض ✅", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "فشل حفظ سجل الصورة: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "فشل رفع الصورة: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddImageDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "رفع") },
                text = { Text("رفع صورة جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🖼️ معرض صور النشاط التجاري:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Surface(
                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${galleryList.size} صور",
                        fontSize = 11.sp,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (galleryList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🖼️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لم تقم برفع أي صور لمعرض النشاط التجاري بعد", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(galleryList, key = { it.id }) { item ->
                        GalleryGridCard(
                            item = item,
                            themeColors = themeColors,
                            onSetPrimary = {
                                firestore.collection(ownerCollection).document(account.id)
                                    .update("coverImage", item.urlOrBase64)
                                    .addOnSuccessListener {
                                        primaryUrl = item.urlOrBase64
                                        Toast.makeText(context, "تم تعيين وحفظ الصورة كغلاف رئيسي ✅", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "تعذر تحديث الغلاف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    if (item.id == "primary_cover") {
                                        firestore.collection(ownerCollection).document(account.id)
                                            .update("coverImage", "")
                                            .addOnSuccessListener {
                                                primaryUrl = ""
                                                Toast.makeText(context, "تم حذف الغلاف الرئيسي 🗑️", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        val albumId = item.id.substringBefore("_")
                                        galleryRepository.deleteGalleryAlbum(albumId).onSuccess {
                                            Toast.makeText(context, "تم حذف الصورة من المعرض 🗑️", Toast.LENGTH_SHORT).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "فشل الحذف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddImageDialog) {
        var imageUrlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddImageDialog = false },
            title = { Text("رفع / إضافة صورة جديدة للمعرض", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            showAddImageDialog = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isUploading) "جاري الرفع..." else "اختيار صورة من الجهاز 📸", fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("أو أدخل رابط الصورة (https://...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = imageUrlInput.trim()
                        if (cleanUrl.isNotBlank()) {
                            coroutineScope.launch {
                                val album = GalleryAlbumEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    ownerId = account.id,
                                    title = "صورة معرض",
                                    imageUrls = listOf(cleanUrl)
                                )
                                galleryRepository.saveGalleryAlbum(album).onSuccess {
                                    Toast.makeText(context, "تم حفظ الصورة في معرض الصور ✅", Toast.LENGTH_SHORT).show()
                                    showAddImageDialog = false
                                }.onFailure { e ->
                                    Toast.makeText(context, "فشل حفظ الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "يرجى اختيار صورة أو إدخال رابط صالح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("حفظ الرابط", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddImageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun GalleryGridCard(
    item: GalleryImageItem,
    themeColors: VisualThemePalette,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = rememberBase64Bitmap(item.urlOrBase64)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, if (item.isPrimary) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.DarkGray)
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "Gallery Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (item.urlOrBase64.startsWith("http")) {
                    AsyncImage(
                        model = item.urlOrBase64,
                        contentDescription = "Gallery Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🖼️", fontSize = 36.sp)
                    }
                }

                if (item.isPrimary) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("الغلاف الرئيسي ⭐", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.isPrimary) {
                    TextButton(onClick = onSetPrimary, contentPadding = PaddingValues(0.dp)) {
                        Text("تعيين رئيسية", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
