package com.example.ui.screens.entities

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.*
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

@Composable
internal fun ProfilePriceOfferDialog(
    entityId: String,
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    context: Context,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            var editPrice by remember {
                mutableStateOf(
                    (provider?.previewPrice ?: property?.price ?: 0.0).toString()
                )
            }
            var editOffer by remember {
                mutableStateOf(
                    provider?.specialOffersJson?.takeIf { it.isNotBlank() }
                        ?: store?.specialOffersJson?.takeIf { it.isNotBlank() }
                        ?: "خصم 15% للمعاينة الأولى والخدمات"
                )
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🏷️ إدارة الأسعار والعروض الترويجية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                if (store == null) {
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("السعر الأساسي / رسوم المعاينة والكشفية (ريال يمني)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            focusedLabelColor = themeColors.accent,
                            unfocusedLabelColor = themeColors.textSecondary
                        )
                    )
                } else {
                    Text(
                        text = "💡 لتعديل أسعار المنتجات الفردية افتح لوحة إدارة المتجر؛ وهنا يمكنك تحديث العرض العام للنشاط.",
                        fontSize = 11.sp,
                        color = themeColors.textSecondary
                    )
                }

                OutlinedTextField(
                    value = editOffer,
                    onValueChange = { editOffer = it },
                    label = { Text("تفاصيل العرض الترويجي والخصومات") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val parsedPrice = editPrice.trim().toDoubleOrNull()
                            if (store == null && editPrice.isNotBlank() && (parsedPrice == null || parsedPrice < 0.0)) {
                                Toast.makeText(context, "يرجى إدخال سعر صحيح", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val pVal = parsedPrice ?: 0.0
                            val trimmedOffer = editOffer.trim()
                            if (provider != null) {
                                val updated = provider.copy(previewPrice = pVal, specialOffersJson = trimmedOffer)
                                viewModel.updateProviderEntity(updated)
                            } else if (store != null) {
                                val updated = store.copy(specialOffersJson = trimmedOffer)
                                viewModel.updateStoreEntity(updated)
                            } else if (property != null) {
                                val updated = property.copy(price = pVal)
                                viewModel.updatePropertyEntity(updated)
                            }
                            if (trimmedOffer.isNotBlank()) {
                                val offerId = "offer_${entityId}"
                                val offerPayload = hashMapOf(
                                    "id" to offerId,
                                    "ownerId" to entityId,
                                    "providerId" to entityId,
                                    "title" to "عرض خاص - ${provider?.name ?: store?.name ?: property?.title ?: ""}".trim(),
                                    "description" to trimmedOffer,
                                    "discountPercent" to 15,
                                    "isActive" to true,
                                    "isEnabled" to true,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                                viewModel.db.collection("special_offers").document(offerId)
                                    .set(offerPayload)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "🏷️ تم تحديث الأسعار والعروض بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "فشل حفظ العرض: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "🏷️ تم تحديث الأسعار!", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("تحديث الأسعار 🏷️", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", color = themeColors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileAddProductDialog(
    entityId: String,
    context: Context,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            var prodTitle by remember { mutableStateOf("") }
            var prodPrice by remember { mutableStateOf("") }
            var prodDesc by remember { mutableStateOf("") }
            var prodImageUrl by remember { mutableStateOf("") }
            var isUploadingProdImg by remember { mutableStateOf(false) }
            val prodScope = rememberCoroutineScope()

            val prodImagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    prodScope.launch {
                        isUploadingProdImg = true
                        val path = "products/${entityId}/${System.currentTimeMillis()}.webp"
                        val result = com.example.utils.FirebaseStorageUploader.uploadImageToStorage(context, uri, path)
                        isUploadingProdImg = false
                        result.onSuccess { url ->
                            prodImageUrl = url
                            Toast.makeText(context, "تم رفع صورة الصنف بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "فشل رفع الصورة: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("➕ إضافة منتج / صنف / سلعة جديدة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = prodTitle,
                    onValueChange = { prodTitle = it },
                    label = { Text("اسم الصنف أو السلعة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                OutlinedTextField(
                    value = prodPrice,
                    onValueChange = { prodPrice = it },
                    label = { Text("السعر بالريال اليمني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                OutlinedTextField(
                    value = prodDesc,
                    onValueChange = { prodDesc = it },
                    label = { Text("الوصف والمواصفات") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = themeColors.textSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = prodImageUrl,
                        onValueChange = { prodImageUrl = it },
                        label = { Text("رابط الصورة (اختياري)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary,
                            focusedLabelColor = themeColors.accent,
                            unfocusedLabelColor = themeColors.textSecondary
                        )
                    )
                    Button(
                        onClick = { prodImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploadingProdImg,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        if (isUploadingProdImg) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Text("صورة 📷", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isUploadingProdImg) {
                                Toast.makeText(context, "يرجى الانتظار حتى يكتمل رفع الصورة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val cleanTitle = prodTitle.trim()
                            val parsedPrice = prodPrice.trim().toDoubleOrNull()
                            if (cleanTitle.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم الصنف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (parsedPrice == null || parsedPrice < 0.0) {
                                Toast.makeText(context, "يرجى إدخال سعر صحيح بالريال", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val docId = "prod_${java.util.UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
                            val productPayload = hashMapOf(
                                "id" to docId,
                                "ownerId" to entityId,
                                "storeId" to entityId,
                                "name" to cleanTitle,
                                "title" to cleanTitle,
                                "price" to parsedPrice,
                                "priceYer" to parsedPrice.toInt(),
                                "description" to prodDesc.trim(),
                                "imageUrl" to prodImageUrl.trim(),
                                "isAvailable" to true,
                                "isDeleted" to false,
                                "createdAt" to System.currentTimeMillis()
                            )
                            try {
                                viewModel.db.collection("products").document(docId).set(productPayload)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "📦 تم إضافة الصنف بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "فشل حفظ الصنف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "حدث خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إضافة الصنف ➕", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", color = themeColors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileDeleteEntityConfirmDialog(
    collectionName: String,
    entityId: String,
    context: Context,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ تأكيد حذف النشاط", fontWeight = FontWeight.Bold) },
        text = { Text("هل أنت متأكد من رغبتك في حذف هذا النشاط؟ سيتم إخفاؤه من قائمة الدليل والتطبيق، مع إمكانية استعادته لاحقاً من لوحة تحكم الأدمن.") },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        viewModel.db.collection(collectionName).document(entityId).update("isDeleted", true)
                            .addOnSuccessListener {
                                Toast.makeText(context, "🗑️ تم حذف النشاط وإخفاؤه بنجاح", Toast.LENGTH_LONG).show()
                                viewModel.navigateToScreen(AppScreens.USER_BROWSE)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "فشل حذف النشاط: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "حدث خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("نعم، حذف النشاط", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("تراجع")
            }
        }
    )
}
