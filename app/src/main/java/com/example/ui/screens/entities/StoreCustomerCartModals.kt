package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.ProductEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.ui.helpers.AppPreferenceHelper
import com.example.utils.BookingStatus
import com.example.utils.VisualThemePalette

@Composable
internal fun StoreFloatingCartBar(
    cartCount: Int,
    cartTotal: Double,
    themeColors: VisualThemePalette,
    onClearCart: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Surface(
        color = themeColors.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🛒 السلة ($cartCount ${if (cartCount == 1) "صنف" else "أصناف"})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
                Text(
                    text = "الإجمالي: ${cartTotal.toInt()} ريال",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearCart) {
                    Text("تفريغ", color = Color(0xFFEF4444), fontSize = 11.sp)
                }
                Button(
                    onClick = onCheckoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إتمام الطلب ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProductItemCardTwoColumn(
    product: ProductEntity,
    themeColors: VisualThemePalette,
    quantityInCart: Int = 0,
    onCardClick: () -> Unit,
    onAddToCart: () -> Unit,
    onDecrementCart: () -> Unit = {}
) {
    val hasDiscount = product.isOffer || product.discountPercent > 0 || (product.oldPrice > product.price && product.price > 0)
    val discountPercent = if (product.discountPercent > 0) {
        product.discountPercent
    } else if (product.oldPrice > product.price && product.price > 0) {
        (((product.oldPrice - product.price) / product.oldPrice) * 100).toInt()
    } else 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.8.dp, themeColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(themeColors.background.copy(alpha = 0.5f))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    SmartAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = themeColors.textSecondary, modifier = Modifier.size(36.dp))
                    }
                }

                if (hasDiscount && discountPercent > 0) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "-$discountPercent%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val specText = product.description.ifBlank { "سلعة متوفرة بجودة عالية" }
                Text(
                    text = specText,
                    fontSize = 10.sp,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (hasDiscount && product.oldPrice > product.price) {
                            Text(
                                text = "${product.oldPrice.toInt()} ر.ي",
                                fontSize = 9.sp,
                                color = themeColors.textSecondary,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        Text(
                            text = "${product.price.toInt()} ريال",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    if (quantityInCart == 0) {
                        Surface(
                            onClick = onAddToCart,
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة للسلة", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = onDecrementCart,
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Text("$quantityInCart", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = themeColors.textPrimary)
                            Surface(
                                onClick = onAddToCart,
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatYemenWhatsAppPhone(rawPhone: String): String {
    val clean = AppPreferenceHelper.normalizePhoneNumber(rawPhone)
    return if (clean.startsWith("967")) clean else "967$clean"
}

@Composable
fun CartCheckoutModal(
    cartItems: List<Pair<ProductEntity, Int>>,
    store: StoreEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val context = LocalContext.current
    val savedPhone by viewModel.currentUserPhone.collectAsState()
    val savedName by viewModel.currentUserName.collectAsState()
    var customerPhoneInput by remember(savedPhone) { mutableStateOf(savedPhone) }
    var customerNameInput by remember(savedName) { mutableStateOf(savedName) }
    var notesInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val total = cartItems.sumOf { (p, q) -> p.price * q }
    val summaryText = cartItems.joinToString(" + ") { (p, q) -> "${p.name} × $q" }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text("إتمام طلب السلة (${cartItems.sumOf { it.second }} أصناف) 🛒", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summaryText, fontSize = 11.5.sp)
                Text("الإجمالي: ${total.toInt()} ريال يمني", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                OutlinedTextField(value = customerNameInput, onValueChange = { customerNameInput = it }, label = { Text("اسمك الكريم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = customerPhoneInput,
                    onValueChange = { customerPhoneInput = it },
                    label = { Text("رقم هاتفك للتواصل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = notesInput, onValueChange = { notesInput = it }, label = { Text("العنوان والملاحظات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val waNum = formatYemenWhatsAppPhone(store.phone)
                        if (waNum.isNotBlank()) {
                            val itemsLines = cartItems.joinToString("\n") { (p, q) -> "• ${p.name} × $q (${(p.price * q).toInt()} ر.ي)" }
                            val msg = "مرحباً ${store.name}، أرغب في طلب:\n$itemsLines\nالإجمالي: ${total.toInt()} ريال يمني\n${if (customerNameInput.isNotBlank()) "العميل: $customerNameInput\n" else ""}${if (notesInput.isNotBlank()) "العنوان/ملاحظات: $notesInput" else ""}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=${Uri.encode(msg)}")))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onOrderPlaced()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("واتساب 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        if (customerPhoneInput.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتفك أولاً", Toast.LENGTH_SHORT).show()
                        } else if (!isSubmitting) {
                            isSubmitting = true
                            val booking = BookingEntity(
                                customerName = customerNameInput.trim().ifBlank { "عميل المتجر" },
                                customerPhone = customerPhoneInput.trim(),
                                customerArea = notesInput.trim().ifBlank { store.localNeighborhood.ifBlank { store.cityId } },
                                serviceType = summaryText,
                                serviceDetails = notesInput.trim(),
                                providerId = store.id,
                                providerName = store.name,
                                providerPhone = store.phone,
                                category = store.categoryId.ifBlank { "STORE" },
                                totalAmount = total,
                                price = total,
                                status = BookingStatus.PENDING.code
                            )
                            viewModel.bookingViewModel.createBooking(booking) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "تم تسجيل طلبك وإرساله للمتجر بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    onOrderPlaced()
                                } else {
                                    Toast.makeText(context, "تعذر إرسال الطلب، يرجى التحقق من الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48))
                ) {
                    Text(if (isSubmitting) "جاري الإرسال..." else "تأكيد الطلب ✅", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, enabled = !isSubmitting) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickProductOrderModal(
    product: ProductEntity,
    store: StoreEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedPhone by viewModel.currentUserPhone.collectAsState()
    val savedName by viewModel.currentUserName.collectAsState()
    var customerPhoneInput by remember(savedPhone) { mutableStateOf(savedPhone) }
    var customerNameInput by remember(savedName) { mutableStateOf(savedName) }
    var notesInput by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF10B981))
                Text("طلب سلعة: ${product.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "السعر الإجمالي: ${(product.price * quantity).toInt()} ريال يمني",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("الكمية:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.size(48.dp)) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$quantity", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { quantity++ }, modifier = Modifier.size(48.dp)) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = customerNameInput,
                    onValueChange = { customerNameInput = it },
                    label = { Text("اسمك الكريم") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhoneInput,
                    onValueChange = { customerPhoneInput = it },
                    label = { Text("رقم هاتفك للتواصل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("ملاحظات إضافية أو العنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val waNum = formatYemenWhatsAppPhone(store.phone)
                        val message = "مرحباً ${store.name}، أرغب في طلب:\n" +
                            "• السلعة: ${product.name}\n" +
                            "• الكمية: $quantity\n" +
                            "• الإجمالي: ${(product.price * quantity).toInt()} ريال يمني\n" +
                            (if (customerNameInput.isNotBlank()) "• العميل: $customerNameInput\n" else "") +
                            (if (notesInput.isNotBlank()) "• ملاحظات: $notesInput\n" else "") +
                            "\nتم الإرسال عبر دليل خدمات اليمن 🇾🇪"
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum?text=${Uri.encode(message)}"))
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق الواتساب", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("واتساب 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        if (customerPhoneInput.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتفك أولاً", Toast.LENGTH_SHORT).show()
                        } else if (!isSubmitting) {
                            isSubmitting = true
                            val orderTotal = product.price * quantity
                            val booking = BookingEntity(
                                customerName = customerNameInput.trim().ifBlank { "عميل المتجر" },
                                customerPhone = customerPhoneInput.trim(),
                                customerArea = notesInput.trim().ifBlank { store.localNeighborhood.ifBlank { store.cityId } },
                                serviceType = "${product.name} × $quantity",
                                serviceDetails = notesInput.trim(),
                                providerId = store.id,
                                providerName = store.name,
                                providerPhone = store.phone,
                                category = store.categoryId.ifBlank { "STORE" },
                                totalAmount = orderTotal,
                                price = orderTotal,
                                status = BookingStatus.PENDING.code
                            )
                            viewModel.bookingViewModel.createBooking(booking) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "تم تسجيل طلبك وإرساله للمتجر بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "تعذر إرسال الطلب، يرجى التحقق من الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48))
                ) {
                    Text(if (isSubmitting) "جاري الإرسال..." else "تأكيد الطلب 🛒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, enabled = !isSubmitting) {
                Text("إلغاء")
            }
        }
    )
}
