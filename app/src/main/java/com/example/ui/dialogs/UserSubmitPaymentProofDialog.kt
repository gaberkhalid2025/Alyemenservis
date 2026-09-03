@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.dialogs




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
import com.example.ui.screens.notifications.*
import com.example.ui.screens.dashboard.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun UserSubmitPaymentProofDialog(
    booking: com.example.data.BookingEntity,
    viewModel: MainViewModel,
    paymentWallets: List<com.example.data.PaymentWalletEntity>,
    settingsState: com.example.data.AdminSettingsEntity,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    var selectedWallet by remember { mutableStateOf(paymentWallets.firstOrNull { it.status == "active" }) }
    var transferIdInput by remember { mutableStateOf("") }
    var accountNameInput by remember { mutableStateOf("") }
    var photoInput by remember { mutableStateOf("") }

    val proofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoInput = uri.toString()
            viewModel.triggerNotification("📸 تم اختيار صورة الإثبات من المعرض بنجاح!")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💳 سداد رسوم الحجز والخدمة بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("يرجى اختيار أحد الحسابات / المحافظ التالية والتحويل إليها بقيمة تكلفة المعاينة والصيانة:", fontSize = 11.sp, color = Color.LightGray)

                if (paymentWallets.isEmpty()) {
                    Text("⚠️ عذراً، لا توجد محافظ دفع مفعلة حالياً بالمنصة للتسديد. يرجى مراجعة المشرفين.", fontSize = 11.sp, color = Color.Red)
                } else {
                    Text("المحافظ والحسابات المتاحة للتحويل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentWallets.filter { it.status == "active" && it.isVisibleToUsers && (it.walletType == "DEPOSIT" || it.walletType == "BOTH") }.forEach { wallet ->
                            val isSel = selectedWallet?.id == wallet.id
                            val name = when (wallet.provider) {
                                "jeeb" -> "جيب 📱"
                                "alKarimi" -> "الكريمي 🏦"
                                "jawaly" -> "جوالي 📲"
                                "floosi" -> "فلوسي 💳"
                                "cashExchange" -> "حوالة 💸"
                                "foreignCurrency" -> "عملات 🌐"
                                "yemenMobile" -> "يمن كاش 🇾🇪"
                                else -> wallet.accountNameAr.ifBlank { wallet.accountName }.take(10)
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) themeColors.accent else Color.DarkGray,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedWallet = wallet }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(name, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                selectedWallet?.let { wallet ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("رقم الحساب/المحفظة للتحويل: ${wallet.walletNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("اسم صاحب الحساب المستلم: ${wallet.accountNameAr}", fontSize = 11.sp, color = Color.White)
                            if (wallet.description.isNotEmpty()) {
                                Text("تعليمات: ${wallet.description}", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))
                Text("يرجى تعبئة بيانات التحويل بعد إرسال المبلغ المالي:", fontSize = 11.sp, color = Color.LightGray)

                OutlinedTextField(
                    value = transferIdInput,
                    onValueChange = { transferIdInput = it },
                    label = { Text("رقم الحوالة المرجعي / رقم العملية (الـ ID)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = accountNameInput,
                    onValueChange = { accountNameInput = it },
                    label = { Text("اسم المرسل الكامل (صاحب المحفظة المحوِلة)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Button(
                    onClick = {
                        proofPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (photoInput.isNotEmpty()) Color(0xFF10B981) else themeColors.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(
                        if (photoInput.isNotEmpty()) "✅ تم اختيار صورة الإثبات (اضغط لتغييرها)" else "📷 رفع صورة الإثبات أو لقطة الشاشة من الهاتف",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (transferIdInput.isBlank() || accountNameInput.isBlank()) {
                                viewModel.triggerNotification("❌ يرجى ملء رقم الحوالة واسم مرسل الحوالة كاملاً")
                                return@Button
                            }
                            if (settingsState.requirePaymentProofImage && photoInput.isBlank()) {
                                viewModel.triggerNotification("❌ الإدارة تتطلب إرفاق صورة الإثبات أو لقطة الشاشة للتحقق!")
                                return@Button
                            }
                            val wallet = selectedWallet ?: return@Button
                            
                            val docRef = viewModel.db.collection("payments").document()
                            val payment = com.example.data.PaymentEntity(
                                id = docRef.id,
                                userId = booking.customerPhone,
                                providerId = booking.providerId,
                                bookingId = booking.id,
                                type = "service",
                                method = "mobileWallet",
                                status = "PROCESSING",
                                amount = 1000.0,
                                advanceAmount = 0.0,
                                remainingAmount = 1000.0,
                                commission = 0.0,
                                providerShare = 1000.0,
                                currency = "YER",
                                isLinkedToBooking = true,
                                transferId = transferIdInput,
                                transferPhoto = photoInput,
                                walletProvider = wallet.provider,
                                verificationNote = "بانتظار مراجعة وتأكيد الإدارة"
                            )
                            
                            docRef.set(payment)
                            viewModel.triggerNotification("✅ تم إرسال إثبات التحويل بنجاح! جاري مراجعته من الإدارة.")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إرسال إثبات التحويل 📤", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("إلغاء", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
