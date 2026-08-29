package com.example.ui.screens.dashboard.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.MainViewModel
import com.example.utils.convertUriToBase64
import com.example.utils.VisualThemePalette
import java.util.UUID

/**
 * 📦 StoreAddProductDialog - نافذة إضافة منتج جديد للكتالوج
 */
@Composable
fun StoreAddProductDialog(
    storeId: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    context: Context,
    onDismiss: () -> Unit
) {
    var prodName by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodImageBase64 by remember { mutableStateOf("") }

    val prodUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { prodImageBase64 = convertUriToBase64(context, it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📦 إضافة منتج جديد للكتالوج", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        containerColor = themeColors.secondary,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prodName,
                    onValueChange = { prodName = it },
                    label = { Text("اسم المنتج") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = prodDesc,
                    onValueChange = { prodDesc = it },
                    label = { Text("وصف ومواصفات المنتج التفصيلية") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = prodPrice,
                    onValueChange = { prodPrice = it },
                    label = { Text("سعر المنتج (ريال يمني)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("🖼️ صورة المنتج:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                val pBitmap = remember(prodImageBase64) {
                    if (prodImageBase64.isNotEmpty()) {
                        try {
                            val bytes = android.util.Base64.decode(prodImageBase64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch(e: Exception) { null }
                    } else null
                }

                if (pBitmap != null) {
                    Image(
                        bitmap = pBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Crop
                    )
                }

                Button(
                    onClick = { prodUriPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اختر صورة للمنتج 📸", color = Color.White, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dPrice = prodPrice.toDoubleOrNull() ?: 0.0
                    if (prodName.trim().isEmpty() || dPrice <= 0.0) {
                        Toast.makeText(context, "⚠️ يرجى تعبئة الحقول والأسعار بطريقة صحيحة!", Toast.LENGTH_SHORT).show()
                    } else {
                        val newProduct = ProductEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = storeId,
                            name = prodName.trim(),
                            description = prodDesc.trim(),
                            price = dPrice,
                            imageUrl = prodImageBase64
                        )
                        viewModel.saveProduct(newProduct)
                        onDismiss()
                        Toast.makeText(context, "✅ تم إضافة المنتج ونشره بكتالوجك بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("حفظ ونشر المنتج 📢", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("إلغاء", color = Color.White, fontSize = 11.sp)
            }
        }
    )
}
