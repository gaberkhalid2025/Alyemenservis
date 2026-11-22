package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.*
import com.example.utils.SecureHasher
import com.example.utils.VisualThemePalette
import com.example.utils.getAreaCoords
import com.example.utils.getDistance
import com.example.utils.getProviderCoords

@Composable
internal fun AdminOperationsAndBookingDialogs(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context

    // 3. Delete reservation confirmation Dialog
    state.showDeleteBookingConfirmId?.let { bId ->
        AlertDialog(
            onDismissRequest = { state.showDeleteBookingConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل تقصد حذف الحجز نهائياً؟", color = Color.White) },
            text = { Text("حذف هذا الحجز سيزيله من جدول حجوزات الدعم والمتابعة مباشرة.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(bId)
                        state.showDeleteBookingConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، امسح تماماً", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { state.showDeleteBookingConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // Booking Rejection Reason Dialog
    state.showRejectionReasonDialogId?.let { bId ->
        AlertDialog(
            onDismissRequest = { state.showRejectionReasonDialogId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("❌ رفض طلب الحجز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرجاء توضيح سبب رفض طلب الحجز ليتم إرساله للعميل مباشرة في التنبيهات:", color = Color.White, fontSize = 11.sp)
                    OutlinedTextField(
                        value = state.bookingRejectionReasonInput,
                        onValueChange = { state.bookingRejectionReasonInput = it },
                        label = { Text("سبب الرفض (مثال: جدول الأعمال ممتلئ، الموقع خارج التغطية)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (state.bookingRejectionReasonInput.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء كتابة سبب الرفض لتنبيه العميل به", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBookingStatus(bId, "REJECTED", state.bookingRejectionReasonInput)
                            state.showRejectionReasonDialogId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض وإرسال السبب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { state.showRejectionReasonDialogId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 11.sp)
                }
            }
        )
    }

    // 4. Delete targeted notifications confirmation Dialog
    state.showDeleteNotifConfirmId?.let { nId ->
        AlertDialog(
            onDismissRequest = { state.showDeleteNotifConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ تأكيد حذف الإشعار الموجه", color = Color.White) },
            text = { Text("هذا الإشعار سيوضع كمنشور تالف وسيتم محوه من قاعدة بيانات الهواتف المحلية والمخزن.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(nId)
                        state.showDeleteNotifConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف الذكرى")
                }
            },
            dismissButton = {
                Button(onClick = { state.showDeleteNotifConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("تراجع")
                }
            }
        )
    }

    // 5. Active Chat Channel logs visualizer dialog and direct replies
    state.showActiveChatChannelObj?.let { ch ->
        Dialog(onDismissRequest = { state.showActiveChatChannelObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                border = BorderStroke(1.dp, themeColors.accent)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val partnerName = if (ch.isProvider) "مقدم الخدمة: ${ch.userName}" else "مستخدم الدليل: ${ch.userName}"
                    Text("💬 مراقبة الشات: $partnerName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ch.messages, key = { it.id }) { msg ->
                                val alignment = if (msg.senderId == "admin") Alignment.End else Alignment.Start
                                val bubbleBg = if (msg.senderId == "admin") themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bubbleBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(msg.message, fontSize = 11.sp, color = Color.White)
                                    }
                                    Text(msg.senderName, fontSize = 9.sp, color = themeColors.textSecondary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.adminChatReplyInput,
                        onValueChange = { state.adminChatReplyInput = it },
                        label = { Text("اكتب رد المشرف الصريح والكامل هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (state.adminChatReplyInput.trim().isNotEmpty()) {
                                    viewModel.replyToChatChannel(ch.id, "admin", state.adminChatReplyInput.trim(), "مشرف الدعم")
                                    val updatedCh = viewModel.chatChannels.value.find { it.id == ch.id }
                                    if (updatedCh != null) {
                                        state.showActiveChatChannelObj = updatedCh
                                    }
                                    state.adminChatReplyInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إرسال الرد الموثق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.toggleBlockChatChannel(ch.id)
                                val updatedCh = viewModel.chatChannels.value.find { it.id == ch.id }
                                if (updatedCh != null) {
                                    state.showActiveChatChannelObj = updatedCh
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (ch.isBlocked) "إلغاء الحظر" else "حظر الطرفين", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 6. Delete communication log dialog
    state.showDeleteChatConfirmId?.let { chId ->
        AlertDialog(
            onDismissRequest = { state.showDeleteChatConfirmId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("⚠️ هل أنت متأكد من مسح ملفات السجل صراحة؟", color = Color.White) },
            text = { Text("سيتم اقتطاع وحذف قنوات الشات من قاعدة الداتا فوراً دون أي إمكانية استرجاع.", color = themeColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatChannel(chId)
                        state.showDeleteChatConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف ذيل المحادثة كاملة")
                }
            },
            dismissButton = {
                Button(onClick = { state.showDeleteChatConfirmId = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 8. Editing Booking Dialog Control
    state.editingBookingObj?.let { booking ->
        var editCustName by rememberSaveable(booking.id) { mutableStateOf(booking.customerName) }
        var editCustPhone by rememberSaveable(booking.id) { mutableStateOf(booking.customerPhone) }
        var editCustArea by rememberSaveable(booking.id) { mutableStateOf(booking.customerArea) }
        var editCustService by rememberSaveable(booking.id) { mutableStateOf(booking.serviceType) }
        var editCustDate by rememberSaveable(booking.id) { mutableStateOf(booking.dateString) }
        var editCustTime by rememberSaveable(booking.id) { mutableStateOf(booking.timeString) }
        var editCustStatus by rememberSaveable(booking.id) { mutableStateOf(booking.status) }
        var editCustPassword by rememberSaveable(booking.id) {
            mutableStateOf(if (booking.pinCode.isNotBlank()) "••••" else "")
        }

        Dialog(onDismissRequest = { state.editingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✏️ تعديل بيانات استمارة الحجز", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editCustName,
                        onValueChange = { editCustName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPhone,
                        onValueChange = { editCustPhone = it },
                        label = { Text("الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustArea,
                        onValueChange = { editCustArea = it },
                        label = { Text("مكان الإقامة والحي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustService,
                        onValueChange = { editCustService = it },
                        label = { Text("نوع الخدمة المطلوبة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustDate,
                        onValueChange = { editCustDate = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustTime,
                        onValueChange = { editCustTime = it },
                        label = { Text("الوقت أو الساعة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editCustPassword,
                        onValueChange = { editCustPassword = it },
                        label = { Text("🔑 رمز المرور السري (تغيير/إعادة تعيين)") },
                        placeholder = { Text("اتركه فارغاً للإبقاء على الرمز الحالي", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("حدد حالة الحجز:", color = themeColors.textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("PENDING", "APPROVED", "IN_PROGRESS", "COMPLETED", "REJECTED")
                        statuses.forEach { st ->
                            val isSel = editCustStatus == st
                            val color = when (st) {
                                "PENDING" -> Color(0xFFF59E0B)
                                "APPROVED" -> Color.Green
                                "IN_PROGRESS" -> Color(0xFF3B82F6)
                                "COMPLETED" -> Color(0xFF10B981)
                                else -> Color.Red
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) color else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSel) Color.White else color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { editCustStatus = st }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (st) {
                                        "PENDING" -> "تعليق"
                                        "APPROVED" -> "قبول"
                                        "IN_PROGRESS" -> "عمل"
                                        "COMPLETED" -> "تم"
                                        else -> "رفض"
                                    },
                                    color = if (isSel) Color.White else color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editCustName.trim().isNotEmpty() && editCustPhone.trim().isNotEmpty()) {
                                    val updatedB = booking.copy(
                                        customerName = editCustName.trim(),
                                        customerPhone = editCustPhone.trim(),
                                        customerArea = editCustArea.trim(),
                                        serviceType = editCustService.trim(),
                                        dateString = editCustDate.trim(),
                                        timeString = editCustTime.trim(),
                                        status = editCustStatus,
                                        pinCode = if (editCustPassword.trim() == "••••" || editCustPassword.trim().contains(":")) booking.pinCode else if (editCustPassword.trim().isNotBlank()) SecureHasher.hashPin(editCustPassword.trim()) else booking.pinCode
                                    )
                                    viewModel.updateBooking(updatedB)
                                    state.editingBookingObj = null
                                } else {
                                    viewModel.triggerNotification("⚠️ يجب ملء الاسم والهاتف بالحد الأدنى!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 حفظ ونشر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { state.editingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء الأمر", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 8.5 Redirect Booking Dialog Control
    state.redirectingBookingObj?.let { booking ->
        val providersList = state.activatedProviders

        val nearestProvider = remember(booking, providersList) {
            val userCoords = getAreaCoords(booking.customerArea)
            providersList.minByOrNull { tech ->
                val techCoords = getProviderCoords(tech)
                getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
            }
        }

        var selectedTargetType by remember { mutableStateOf("SPECIFIC") }
        var selectedOtherProviderId by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { state.redirectingBookingObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔄 توجيه الحجز إلى مسؤول جديد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Text("العميل: ${booking.customerName} - الحي: ${booking.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text("الفني الحالي: ${booking.providerName}", fontSize = 11.sp, color = themeColors.accent)

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "ADMIN" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "ADMIN"), onClick = { selectedTargetType = "ADMIN" })
                            Text("المدير (الأدمن) 👑", fontSize = 12.sp, color = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "SPECIFIC" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "SPECIFIC"), onClick = { selectedTargetType = "SPECIFIC" })
                            Text("الفني المذكور: ${booking.providerName} 👷", fontSize = 12.sp, color = Color.White)
                        }

                        nearestProvider?.let { np ->
                            val userCoords = getAreaCoords(booking.customerArea)
                            val techCoords = getProviderCoords(np)
                            val distance = getDistance(userCoords.first, userCoords.second, techCoords.first, techCoords.second)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "NEAREST" }
                            ) {
                                RadioButton(selected = (selectedTargetType == "NEAREST"), onClick = { selectedTargetType = "NEAREST" })
                                Text("الفني الأقرب للمستخدم: ${np.name} (يبعد ${String.format("%.1f", distance)} كم) 📍", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedTargetType = "OTHER" }
                        ) {
                            RadioButton(selected = (selectedTargetType == "OTHER"), onClick = { selectedTargetType = "OTHER" })
                            Text("توجيه لفني آخر من الدليل 🔎", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (selectedTargetType == "OTHER") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedProvName = providersList.find { it.id == selectedOtherProviderId }?.name ?: "اختر الفني الآخر..."
                            Button(
                                onClick = { dropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedProvName, color = Color.White, fontSize = 11.sp)
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.8f)
                            ) {
                                providersList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov.name + " (${prov.customCategoryName.ifBlank { "فني" }})", color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            selectedOtherProviderId = prov.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetProvider = when (selectedTargetType) {
                                    "ADMIN" -> Pair("admin", "إدارة الدليل")
                                    "SPECIFIC" -> Pair(booking.providerId, booking.providerName)
                                    "NEAREST" -> Pair(nearestProvider?.id ?: "admin", nearestProvider?.name ?: "إدارة الدليل")
                                    "OTHER" -> {
                                        val op = providersList.find { it.id == selectedOtherProviderId }
                                        Pair(op?.id ?: "admin", op?.name ?: "إدارة الدليل")
                                    }
                                    else -> Pair(booking.providerId, booking.providerName)
                                }

                                val updatedB = booking.copy(
                                    providerId = targetProvider.first,
                                    providerName = targetProvider.second
                                )
                                viewModel.updateBooking(updatedB)

                                viewModel.addNotification(
                                    title = "🔄 تم تعديل وتوجيه حجزك",
                                    message = "عزيزي العميل، تم توجيه حجزك رقم ${booking.id} بنجاح ومسؤولية تنفيذ الخدمة انتقلت إلى: ${targetProvider.second}.",
                                    targetType = "USER",
                                    targetValue = booking.customerPhone
                                )

                                if (targetProvider.first != "admin") {
                                    val targetPhone = providersList.find { it.id == targetProvider.first }?.phone ?: ""
                                    viewModel.addNotification(
                                        title = "📅 تم توجيه حجز جديد لك",
                                        message = "تم توجيه حجز جديد لك من الإدارة.\nالعميل: ${booking.customerName}\nالهاتف: ${booking.customerPhone}\nالمنطقة: ${booking.customerArea}\nالخدمة: ${booking.serviceType}\nيرجى التواصل معه مباشرة للتنسيق.",
                                        targetType = "PROVIDER",
                                        targetValue = targetPhone,
                                        customerPhone = booking.customerPhone,
                                        customerName = booking.customerName
                                    )
                                }

                                state.redirectingBookingObj = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توجيه 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { state.redirectingBookingObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
