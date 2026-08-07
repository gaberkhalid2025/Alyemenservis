@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.dashboard

import com.example.ui.screens.dashboard.*
import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.ui.screens.chat.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
/* BookingsScreenLayout has been moved to com.example.ui.screens.bookings.BookingsScreenLayout */
fun StoreOwnerDashboardLayout(
    store: com.example.data.StoreEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    ratings: List<com.example.data.RatingEntity>
) {
    val context = LocalContext.current
    var editName by remember(store) { mutableStateOf(store.name) }
    var editDesc by remember(store) { mutableStateOf(store.description) }
    var editAddress by remember(store) { mutableStateOf(store.localNeighborhood) }
    var editPhone by remember(store) { mutableStateOf(store.phone) }

    // Product Dialog State
    var showAddProductDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodImageBase64 by remember { mutableStateOf("") }

    val prodUriPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { prodImageBase64 = convertUriToBase64(context, it) }
    }

    val products by viewModel.products.collectAsState()
    val storeProducts = remember(products, store.id) {
        products.filter { it.storeId == store.id }
    }

    val storeRatings = remember(ratings, store.id) {
        ratings.filter { it.targetId == store.id }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🏪", fontSize = 28.sp)
        }

        Text(
            text = "🎉 لوحة تحكم وإدارة متجرك: ${store.name}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Part 1: Edit Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📝 تعديل بيانات وموقع المتجر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("اسم المتجر / المحل التجارية") },
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
                            viewModel.saveStore(
                                store.copy(
                                    name = editName.trim(),
                                    description = editDesc.trim(),
                                    localNeighborhood = editAddress.trim(),
                                    phone = editPhone.trim()
                                )
                            )
                            android.widget.Toast.makeText(context, "✅ تم حفظ التغييرات بنجاح لمحلّك!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ التحديثات 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Part 2: Product Catalog management
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📦 كتالوج المنتجات المعروضة (${storeProducts.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = { showAddProductDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ إضافة منتج", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (storeProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد منتجات مضافة لهذا المحل حالياً.", fontSize = 10.sp, color = Color.Gray)
                    }
                } else {
                    storeProducts.forEach { prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                val pBitmap = remember(prod.imageUrl) {
                                    if (prod.imageUrl.isNotEmpty()) {
                                        try {
                                            val bytes = android.util.Base64.decode(prod.imageUrl, android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        } catch(e: Exception) { null }
                                    } else null
                                }
                                if (pBitmap != null) {
                                    Image(
                                        bitmap = pBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(36.dp).background(Color.DarkGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                        Text("📦", fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(prod.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("السعر: ${prod.price} ريال يمني", fontSize = 9.sp, color = themeColors.accent)
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteProduct(prod.id)
                                    android.widget.Toast.makeText(context, "🗑️ تم حذف المنتج بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Part 3: Reviews and Replies
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ تقييمات العملاء والرد المباشر عليها:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                if (storeRatings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد تقييمات أو تعليقات من العملاء حالياً.", fontSize = 10.sp, color = Color.Gray)
                    }
                } else {
                    storeRatings.forEach { r ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.userName.ifEmpty { "عميل مجهول" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("⭐".repeat(r.rating.toInt().coerceIn(1, 5)), fontSize = 10.sp, color = Color.Yellow)
                            }

                            Text(r.comment, fontSize = 11.sp, color = Color.LightGray)

                            if (r.reply.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .background(themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Text("💬 ردّك الحالي: ${r.reply}", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                var replyText by remember { mutableStateOf("") }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("اكتب ردك للعميل هنا...", fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    Button(
                                        onClick = {
                                            if (replyText.trim().isNotEmpty()) {
                                                viewModel.addRatingReply(r.id, replyText.trim())
                                                replyText = ""
                                                android.widget.Toast.makeText(context, "✅ تم إرسال ردّك بنجاح للعميل والنشره فوراً", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("رد ⚡", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        var showDeleteConfirm by remember { mutableStateOf(false) }

        Button(
            onClick = {
                viewModel.cancelOrResetJoinRequest(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🚪 تسجيل الخروج من لوحة التحكم", color = Color.White, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🗑️ حذف حساب المتجر نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("⚠️ تأكيد الحذف النهائي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف حساب متجرك بالكامل؟ سيتم حذف المتجر وكافة المنتجات المرفقة والتعليقات نهائياً من قاعدة البيانات ولا يمكن التراجع عن هذا الإجراء!", color = Color.LightGray, fontSize = 11.sp) },
                containerColor = themeColors.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStorePermanently(store.id)
                            viewModel.cancelOrResetJoinRequest(context)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("نعم، احذف نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("إلغاء", color = Color.White, fontSize = 11.sp)
                    }
                }
            )
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
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
                            android.widget.Toast.makeText(context, "⚠️ يرجى تعبئة الحقول والأسعار بطريقة صحيحة!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val newProduct = com.example.data.ProductEntity(
                                id = UUID.randomUUID().toString(),
                                storeId = store.id,
                                name = prodName.trim(),
                                description = prodDesc.trim(),
                                price = dPrice,
                                imageUrl = prodImageBase64
                            )
                            viewModel.saveProduct(newProduct)
                            
                            // Reset State
                            prodName = ""
                            prodDesc = ""
                            prodPrice = ""
                            prodImageBase64 = ""
                            showAddProductDialog = false
                            android.widget.Toast.makeText(context, "✅ تم إضافة المنتج ونشره بكتالوجك بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ ونشر المنتج 📢", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddProductDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }
}