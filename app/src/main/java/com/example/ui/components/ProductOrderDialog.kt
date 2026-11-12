package com.example.ui

import android.content.Intent
import com.example.ui.*
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.ui.components.ManualCoordinateDialog
import com.example.ui.components.buildFormattedLocationAddress
import com.example.ui.screens.map.LandmarkAddressScreen
import com.example.utils.LandmarkAddress
import com.example.utils.LandmarkAddressStore
import com.example.utils.OfflineYemenGazetteer
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StoreProductOrderDialog(
    product: ProductEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val stores by viewModel.stores.collectAsState()

    val targetStore = remember(stores, product.storeId) {
        stores.find { it.id == product.storeId }
    }

    var customerName by remember(currentUserName, currentUserPhone) { 
        mutableStateOf(currentUserName.ifEmpty { if (currentUserPhone.isNotEmpty()) "عميل ($currentUserPhone)" else "" }) 
    }
    var customerPhone by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var customerArea by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    val isRealLoc by viewModel.isUserLocationReal.collectAsState()
    val hasGps = isRealLoc && LandmarkAddress.isValid(userLat, userLng)

    val landmarkStore = remember { LandmarkAddressStore(context.applicationContext) }
    var savedAddresses by remember { mutableStateOf<List<LandmarkAddress>>(emptyList()) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showCoordDialog by remember { mutableStateOf(false) }
    var showLandmarkWizard by remember { mutableStateOf(false) }
    LaunchedEffect(showLandmarkWizard) {
        savedAddresses = withContext(Dispatchers.IO) { landmarkStore.getAll() }
    }

    val totalAmount = product.price * quantity

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.background),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .border(1.5.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛍️ طلب استفسار وشراء مباشر",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }

                // Trust Note Banner
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛡️", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "تسوق آمن ومباشر بدون عمولات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "الدفع والتسليم يتم يداً بيد أو بالاتفاق المباشر لضمان المعاينة والمصداقية.",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Product Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📦", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("السعر: ${product.price.toInt()} YER", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            if (targetStore != null) {
                                Text("المتجر: ${targetStore.name}", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("الاسم الكامل للتواصل", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("order_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("رقم الهاتف اليمني", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = customerArea,
                    onValueChange = { customerArea = it },
                    label = { Text("المدينة / الحي / موقع التوصيل الدقيق", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("📍 من الخريطة", fontSize = 10.5.sp, color = Color(0xFF10B981), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showCoordDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("🔢 إحداثيات", fontSize = 10.5.sp, color = Color(0xFF00E5FF), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showLandmarkWizard = true },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("🏠 بالمعلم", fontSize = 10.5.sp, color = Color(0xFFFBBF24), maxLines = 1)
                    }
                }

                if (savedAddresses.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(savedAddresses.size) { idx ->
                            val addr = savedAddresses[idx]
                            SuggestionChip(
                                onClick = {
                                    customerArea = "${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})"
                                },
                                label = { Text("🏠 ${addr.label}", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية أو تفاصيل التوصيل (اختياري)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الكمية المطلوبة:", fontSize = 11.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (quantity > 1) quantity-- },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("-", color = Color.White, fontSize = 16.sp) }
                        Text("$quantity", modifier = Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Button(
                            onClick = { quantity++ },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) { Text("+", color = Color.White, fontSize = 16.sp) }
                    }
                }

                Surface(
                    color = themeColors.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي التكلفة التقديرية:", fontSize = 11.sp, color = Color.White)
                        Text("${totalAmount.toInt()} YER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    }
                }

                // Direct Contact Fast Buttons
                val storePhone = targetStore?.phone.orEmpty()
                if (storePhone.isNotEmpty()) {
                    Text("📞 أو تواصل مباشرة مع المنشأة الآن:", fontSize = 10.5.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clean = storePhone.replace("+", "").replace(" ", "")
                                val msgText = "السلام عليكم، أود الاستفسار عن طلب: ${product.name} بعدد ($quantity) بسعر إجمالي تقديري ($totalAmount YER)."
                                val uri = Uri.parse("https://wa.me/$clean?text=${Uri.encode(msgText)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("💬 واتساب مباشر", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val uri = Uri.parse("tel:$storePhone")
                                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال هاتفي", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (customerName.isNotEmpty() && customerPhone.isNotEmpty()) {
                            val newOrder = OrderEntity(
                                id = "",
                                storeId = product.storeId,
                                productId = product.id,
                                productName = "${product.name} (كمية: $quantity)",
                                customerPhone = customerPhone,
                                customerName = customerName,
                                customerArea = customerArea.ifEmpty { "طلب مباشر" },
                                price = product.price,
                                quantity = quantity,
                                totalAmount = totalAmount,
                                paymentId = "DIRECT_CONTACT",
                                paymentStatus = "COD_PENDING",
                                status = "PENDING",
                                notes = notes.trim()
                            )
                            viewModel.placeOrder(newOrder)
                            Toast.makeText(context, "✅ تم إرسال الطلب وإشعار التاجر بنجاح!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "يرجى كتابة الاسم ورقم الهاتف", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال طلب الاستفسار والشراء", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showMapPicker) {
        InteractiveMapLocationPickerDialog(
            initialLat = if (hasGps) userLat else 15.3694,
            initialLng = if (hasGps) userLng else 44.1910,
            hasGps = hasGps,
            gpsLat = userLat,
            gpsLng = userLng,
            title = "📍 حدد موقع التوصيل على الخريطة",
            onDismiss = { showMapPicker = false },
            onLocationPicked = { res ->
                customerArea = res.formattedAddress
                if (res.note.isNotBlank() && notes.isBlank()) notes = res.note
                showMapPicker = false
            }
        )
    }

    if (showCoordDialog) {
        ManualCoordinateDialog(
            initialLat = if (hasGps) userLat else 15.3694,
            initialLng = if (hasGps) userLng else 44.1910,
            gpsLat = userLat,
            gpsLng = userLng,
            hasGps = hasGps,
            title = "🔢 إحداثيات موقع التوصيل",
            onDismiss = { showCoordDialog = false },
            onConfirm = { lat, lng, summary ->
                customerArea = buildFormattedLocationAddress(lat, lng, nearestSummary = summary)
                showCoordDialog = false
            }
        )
    }

    if (showLandmarkWizard) {
        Dialog(
            onDismissRequest = { showLandmarkWizard = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LandmarkAddressScreen(
                centerLat = if (hasGps) userLat else 15.3694,
                centerLng = if (hasGps) userLng else 44.1910,
                hasRealLocation = hasGps,
                onSaved = { addr ->
                    customerArea = "${addr.describe()} (${OfflineYemenGazetteer.formatCoordinates(addr.pointLat, addr.pointLng)})"
                },
                onClose = { showLandmarkWizard = false }
            )
        }
    }
}
