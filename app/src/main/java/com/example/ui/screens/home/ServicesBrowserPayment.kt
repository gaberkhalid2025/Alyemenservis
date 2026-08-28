package com.example.ui.screens.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BookingEntity
import com.example.data.PaymentWalletEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 💳 ServicesBrowserPayment - نافذة السداد والدفع الإلكتروني لخدمات دليل اليمن
 * يدعم: الكريمي، جوال، فلوسك، ون كاش، بنكيلي، والسداد النقدي المباشر
 */
@Composable
fun ServicesBrowserPaymentDialog(
    booking: BookingEntity,
    wallets: List<PaymentWalletEntity>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var transferNumber by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 سداد حجز الخدمة إلكترونياً",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Red)
                    }
                }

                // Booking Info Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("الخدمة: ${booking.serviceType.ifBlank { booking.serviceDetails.ifBlank { "طلب خدمة" } }}", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        val amount = if (booking.totalAmount > 0) booking.totalAmount else if (booking.advancePayment > 0) booking.advancePayment else 5000.0
                        Text("المبلغ المطلوب: ${amount.toInt()} ريال يمني", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                        Text("مقدم الخدمة: ${booking.providerName}", fontSize = 10.5.sp, color = Color.LightGray)
                    }
                }

                Text("اختر المحفظة أو الحساب البنكي:", fontSize = 11.sp, color = Color.LightGray)

                // Wallets List
                if (wallets.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🏦 محفظة الكريمي إكسبرس (حساب رقم: 3001234567)\nأو جوالي (رقم: 777123456)",
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    wallets.forEach { wallet ->
                        val isSelected = selectedWallet?.id == wallet.id
                        Card(
                            onClick = { selectedWallet = wallet },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) themeColors.accent.copy(alpha = 0.2f) else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.Transparent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(wallet.accountName.ifBlank { wallet.bankName }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("رقم الحساب/المحفظة: ${wallet.walletNumber.ifBlank { wallet.bankAccountNumber }}", fontSize = 11.sp, color = themeColors.accent)
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedWallet = wallet },
                                    colors = RadioButtonDefaults.colors(selectedColor = themeColors.accent)
                                )
                            }
                        }
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = transferNumber,
                    onValueChange = { transferNumber = it },
                    label = { Text("رقم الحوالة أو إشعار التحويل *", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("اسم المحوّل الثلاثي *", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Submit Button
                Button(
                    onClick = {
                        if (transferNumber.isBlank() || senderName.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال رقم الحوالة واسم المحول", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val amount = if (booking.totalAmount > 0) booking.totalAmount else 5000.0
                        viewModel.createPayment(
                            userId = booking.clientId.ifBlank { booking.customerPhone },
                            providerId = booking.providerId,
                            amount = amount,
                            method = "mobileWallet",
                            bookingId = booking.id,
                            isLinkedToBooking = true,
                            bookingServiceType = booking.serviceType
                        )
                        isSubmitting = false
                        Toast.makeText(context, "✅ تم إرسال إشعار السداد بنجاح وهو قيد التأكيد!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("تأكيد وإرسال إشعار الدفع 🚀", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
