package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentWalletEntity
import com.example.ui.*
import com.example.utils.VisualThemePalette

@Composable
internal fun AdminPaymentAndWalletDialogs(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    // 1. Add/Edit Wallet Dialog
    if (state.showAddWalletDialog) {
        AlertDialog(
            onDismissRequest = { state.showAddWalletDialog = false },
            title = {
                Text(
                    text = if (state.editingWalletObj == null) "➕ إضافة محفظة استقبال أموال جديدة" else "✏️ تعديل بيانات المحفظة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("مزود المحفظة المصرفية / الجوالة:", fontSize = 11.sp, color = Color.LightGray)
                        val providersList = listOf(
                            Pair("jeeb", "محفظة جيب 📱"),
                            Pair("alKarimi", "الكريمي ام فلوس / حساب 🏦"),
                            Pair("jawaly", "محفظة جوالي 📲"),
                            Pair("floosi", "ون كاش / فلوسي 💳"),
                            Pair("cashExchange", "حوالة صرافة نقدية (النجم/العمقي/المميز) 💸"),
                            Pair("foreignCurrency", "محفظة عملات أجنبية (USD/SAR) 🌐"),
                            Pair("yemenMobile", "يمن موبايل كاش 🇾🇪"),
                            Pair("mtc", "محفظة MTC ⚡"),
                            Pair("sabafon", "سبأ كاش 📞"),
                            Pair("other", "تحويلات مصرفية أخرى 🌐")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(providersList) { prov ->
                                    val isSelected = state.walletProviderInput == prov.first
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) themeColors.accent else Color.DarkGray,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { state.walletProviderInput = prov.first }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(prov.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("عملة المحفظة الحالية:", fontSize = 11.sp, color = Color.LightGray)
                        val currenciesList = listOf(
                            Pair("YER", "🇾🇪 ريال يمني YER"),
                            Pair("USD", "💵 دولار أمريكي USD"),
                            Pair("SAR", "🇸🇦 ريال سعودي SAR")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currenciesList.forEach { cur ->
                                val isSelected = state.walletCurrencyInput == cur.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { state.walletCurrencyInput = cur.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cur.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("غرض/استخدام المحفظة:", fontSize = 11.sp, color = Color.LightGray)
                        val typesList = listOf(
                            Pair("BOTH", "🔄 إيداع وسحب معاً"),
                            Pair("DEPOSIT", "📥 إيداع فقط (استقبال)"),
                            Pair("WITHDRAWAL", "📤 سحب فقط (تحويل للعميل)")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            typesList.forEach { typeItem ->
                                val isSelected = state.walletTypeInput == typeItem.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(6.dp))
                                        .clickable { state.walletTypeInput = typeItem.first }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(typeItem.second, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = state.walletNumberInput,
                            onValueChange = { state.walletNumberInput = it },
                            label = { Text("رقم المحفظة / رقم الحساب", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.walletAccountNameArInput,
                            onValueChange = { state.walletAccountNameArInput = it },
                            label = { Text("اسم صاحب الحساب (بالعربية)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.walletDescriptionInput,
                            onValueChange = { state.walletDescriptionInput = it },
                            label = { Text("وصف قصير / تعليمات سريعة للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إظهار المحفظة في خيارات الدفع للعميل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = state.walletIsVisibleInput,
                                onCheckedChange = { state.walletIsVisibleInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تعيين كافتراضية بالدليل", fontSize = 12.sp, color = Color.White)
                            Switch(
                                checked = state.walletIsDefaultInput,
                                onCheckedChange = { state.walletIsDefaultInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = state.walletDisplayOrderInput,
                            onValueChange = { state.walletDisplayOrderInput = it },
                            label = { Text("الترتيب في العرض للعميل", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        if (state.walletNumberInput.isBlank() ||
                            (state.walletAccountNameArInput.isBlank() && state.walletAccountNameInput.isBlank())
                        ) {
                            android.widget.Toast.makeText(
                                context,
                                "يرجى إدخال رقم المحفظة واسم صاحب الحساب",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        val wallet = PaymentWalletEntity(
                            id = state.editingWalletObj?.id ?: "",
                            provider = state.walletProviderInput,
                            walletNumber = state.walletNumberInput.trim(),
                            accountName = state.walletAccountNameInput.ifBlank { state.walletAccountNameArInput }.trim(),
                            accountNameAr = state.walletAccountNameArInput.trim(),
                            description = state.walletDescriptionInput.trim(),
                            walletType = state.walletTypeInput,
                            currency = state.walletCurrencyInput,
                            isVisibleToUsers = state.walletIsVisibleInput,
                            isDefault = state.walletIsDefaultInput,
                            displayOrder = state.walletDisplayOrderInput.toIntOrNull() ?: 0,
                            status = state.walletStatusInput
                        )
                        if (state.editingWalletObj == null) {
                            viewModel.addPaymentWallet(wallet)
                        } else {
                            viewModel.updatePaymentWallet(wallet)
                        }
                        state.showAddWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ المحفظة 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showAddWalletDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 1b. Internal Wallet Transaction Dialog
    if (state.showWalletTxDialog) {
        state.selectedWalletForTx?.let { wallet ->
            val context = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { state.showWalletTxDialog = false },
                title = {
                    Text("💸 معاملة محفظة داخلية (${wallet.ownerName})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نوع الحساب: ${wallet.ownerType} | الرقم: ${wallet.ownerPhone}", fontSize = 11.sp, color = Color.LightGray)
                    Text("الرصيد الحالي: ${wallet.balance} ريال يمني 🇾🇪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Text("اختر نوع العملية المالية:", fontSize = 11.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { state.txTypeInput = "DEPOSIT" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (state.txTypeInput == "DEPOSIT") Color(0xFF10B981) else Color.DarkGray)
                        ) {
                            Text("إيداع ➕", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { state.txTypeInput = "WITHDRAWAL" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (state.txTypeInput == "WITHDRAWAL") Color.Red else Color.DarkGray)
                        ) {
                            Text("سحب ➖", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { state.txTypeInput = "TRANSFER" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (state.txTypeInput == "TRANSFER") themeColors.accent else Color.DarkGray)
                        ) {
                            Text("تحويل 🔄", fontSize = 10.sp, color = if (state.txTypeInput == "TRANSFER") Color.Black else Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = state.txAmountInput,
                        onValueChange = { state.txAmountInput = it },
                        label = { Text("المبلغ بالريال اليمني", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = state.txNoteInput,
                        onValueChange = { state.txNoteInput = it },
                        label = { Text("ملاحظات/سبب العملية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = state.txAmountInput.trim().toDoubleOrNull()
                            if (amt == null || !amt.isFinite() || amt <= 0.0) {
                                android.widget.Toast.makeText(
                                    context,
                                    "يرجى إدخال مبلغ صحيح أكبر من الصفر",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            viewModel.performWalletTransaction(
                                walletId = wallet.id,
                                ownerName = wallet.ownerName,
                                ownerPhone = wallet.ownerPhone,
                                ownerType = wallet.ownerType,
                                type = state.txTypeInput,
                                amount = amt,
                                note = state.txNoteInput
                            )
                            state.showWalletTxDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                    ) {
                        Text("تنفيذ العملية ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { state.showWalletTxDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }
    }

    // 2. Verify Payment Dialog
    state.verifyingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { state.verifyingPaymentObj = null },
            title = { Text("✅ تأكيد واعتماد الدفع الوارد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت على وشك اعتماد وتأكيد دفعة بقيمة ${payment.amount} ريال يمني والمحولة عبر ${payment.walletProvider}.", fontSize = 11.sp, color = Color.LightGray)
                    Text("سيعمل هذا التفعيل على تفعيل/اعتماد طلب الحجز المرتبط وتنشيط حساب المعاملة تلقائياً.", fontSize = 11.sp, color = themeColors.accent)

                    OutlinedTextField(
                        value = state.adminVerifyPaymentNote,
                        onValueChange = { state.adminVerifyPaymentNote = it },
                        label = { Text("ملاحظة وتوضيح (اختياري)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, true, state.adminVerifyPaymentNote, "مدير الدليل 🛡️")
                        state.verifyingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("اعتماد وتأكيد ✅", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.verifyingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 3. Reject Payment Dialog
    state.rejectingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { state.rejectingPaymentObj = null },
            title = { Text("❌ رفض المعاملة وإثبات الدفع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى توضيح سبب رفض إثبات التحويل للعميل:", fontSize = 11.sp, color = Color.LightGray)

                    OutlinedTextField(
                        value = state.adminRejectPaymentNote,
                        onValueChange = { state.adminRejectPaymentNote = it },
                        label = { Text("سبب الرفض المالي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment.id, false, state.adminRejectPaymentNote, "مدير الدليل 🛡️")
                        state.rejectingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.rejectingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    // 4. Refund Payment Dialog
    state.refundingPaymentObj?.let { payment ->
        AlertDialog(
            onDismissRequest = { state.refundingPaymentObj = null },
            title = { Text("🔄 إرجاع واسترداد المبلغ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنبيه: سيتم تسجيل هذه العملية كـ 'تم الاسترداد' وإلغاء فاعليتها المالية في إحصائيات الدليل.", fontSize = 11.sp, color = Color.LightGray)
                    Text("يرجى كتابة سبب الاسترداد للتسجيل والمتابعة:", fontSize = 11.sp, color = Color.LightGray)

                    OutlinedTextField(
                        value = state.refundReasonInput,
                        onValueChange = { state.refundReasonInput = it },
                        label = { Text("سبب إرجاع المبلغ", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundPayment(payment.id, state.refundReasonInput.ifBlank { "استرداد بناء على طلب الأطراف" })
                        state.refundingPaymentObj = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("تأكيد الاسترداد 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.refundingPaymentObj = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}
