package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminBookingsManagementTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val bookings = state.bookings
    val settingsState = state.settingsState

    item {
        Text("📅 إدارة حجوزات الصيانة والطلبات والتحكم بالاستمارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))

        val bookingSubTabs = listOf(
            Triple(
                "SERVICES",
                "🔧 الخدمات والمهن",
                bookings.count {
                    it.providerId.contains("SERVICES") || it.providerId == "ALL" ||
                        (!it.providerId.contains("PROPERTIES") && !it.providerId.contains("STORES") &&
                            !it.providerId.contains("MEDICAL") && !it.providerId.contains("RESTAURANTS") && !it.providerId.contains("JOBS"))
                }
            ),
            Triple("PROPERTIES", "🏠 العقارات", bookings.count { it.providerId.contains("PROPERTIES") }),
            Triple("STORES", "🏪 المراكز والمحلات", bookings.count { it.providerId.contains("STORES") }),
            Triple("MEDICAL", "🏥 المراكز الطبية", bookings.count { it.providerId.contains("MEDICAL") }),
            Triple("RESTAURANTS", "🍔 المطاعم والكافيهات", bookings.count { it.providerId.contains("RESTAURANTS") }),
            Triple("JOBS", "💼 الوظائف", bookings.count { it.providerId.contains("JOBS") })
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(bookingSubTabs.size) { index ->
                val st = bookingSubTabs[index]
                val isSel = state.adminBookingSubTab == st.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.adminBookingSubTab = st.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚙️ لوحة التحكم بمسار وحقول استمارة الحجز الشاملة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                val entityTitle = when (state.adminBookingSubTab) {
                    "STORES" -> "المراكز والمحلات"
                    "MEDICAL" -> "المراكز الطبية والعيادات"
                    "RESTAURANTS" -> "المطاعم والكافيهات"
                    "PROPERTIES" -> "العقارات والأملاك"
                    "JOBS" -> "الوظائف والشركات"
                    else -> "الخدمات والصيانة"
                }
                val entityBothLabel = when (state.adminBookingSubTab) {
                    "STORES" -> "الأدمن والمتجر 👥"
                    "MEDICAL" -> "الأدمن والمركز الطبي 👥"
                    "RESTAURANTS" -> "الأدمن والمطعم 👥"
                    "PROPERTIES" -> "الأدمن وصاحب العقار 👥"
                    "JOBS" -> "الأدمن وصاحب العمل 👥"
                    else -> "الأدمن والفني 👥"
                }
                val entityProviderLabel = when (state.adminBookingSubTab) {
                    "STORES" -> "المتجر مباشرة 🏪"
                    "MEDICAL" -> "المركز الطبي مباشرة 🏥"
                    "RESTAURANTS" -> "المطعم مباشرة 🍔"
                    "PROPERTIES" -> "المعلن مباشرة 🏠"
                    "JOBS" -> "جهة العمل مباشرة 💼"
                    else -> "الفني مباشرة 🛠️"
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📍 توجيه حجوزات ($entityTitle) الواردة من العملاء:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val rModes = listOf(
                            Pair("BOTH", entityBothLabel),
                            Pair("ADMIN", "الأدمن فقط 👮"),
                            Pair("PROVIDER", entityProviderLabel)
                        )
                        rModes.forEach { mode ->
                            val isSel = settingsState.bookingRouting == mode.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                    .clickable {
                                        viewModel.saveCustomSettingsState(settingsState.copy(bookingRouting = mode.first))
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.second,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عرض أيقونة الحجوزات للجمهور:", fontSize = 11.sp, color = themeColors.textSecondary)
                        Switch(
                            checked = settingsState.isBookingsIconVisible,
                            onCheckedChange = { viewModel.saveCustomSettingsState(settingsState.copy(isBookingsIconVisible = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                        )
                    }

                    Text("🔒 صلاحية الدخول وتصفح الحجوزات والمواعيد:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val accessOptions = listOf(
                            Pair("ALL", "الجميع 🌍"),
                            Pair("REGISTERED_ONLY", "المسجلين فقط 🔒"),
                            Pair("DISABLED", "معطلة 🚫")
                        )
                        accessOptions.forEach { opt ->
                            val isSel = settingsState.bookingsAccessControl == opt.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeColors.accent else Color.Gray.copy(alpha = 0.2f))
                                    .clickable {
                                        viewModel.saveCustomSettingsState(settingsState.copy(bookingsAccessControl = opt.first))
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.second,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = settingsState.blockedUsersForBookings,
                        onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(blockedUsersForBookings = it)) },
                        label = { Text("أرقام الهواتف المحظورة من نظام الحجوزات (مثال: 777644, 73...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                OutlinedTextField(
                    value = settingsState.bookingTerms,
                    onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingTerms = it)) },
                    label = { Text("شروط الحجز المعروضة للعميل (شروط وأحكام)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("✏️ تخصيص وتعديل حقول استمارة طلب الحجز (اسم الحقل):", fontSize = 11.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = settingsState.bookingLabelName,
                        onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelName = it)) },
                        label = { Text("حقل اسم العميل") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = settingsState.bookingLabelPhone,
                        onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelPhone = it)) },
                        label = { Text("حقل رقم الهاتف") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = settingsState.bookingLabelArea,
                        onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelArea = it)) },
                        label = { Text("حقل العنوان والحي") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = settingsState.bookingLabelService,
                        onValueChange = { viewModel.saveCustomSettingsState(settingsState.copy(bookingLabelService = it)) },
                        label = { Text("حقل نوع الخدمة") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }
        }
    }

    item {
        val pendingCount = bookings.count { it.status == "PENDING" }
        val inProgressCount = bookings.count { it.status == "IN_PROGRESS" }
        val completedCount = bookings.count { it.status == "COMPLETED" || it.status == "APPROVED" }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⏳ قيد الانتظار", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pendingCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚡ جاري العمل", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(inProgressCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅ مكتملة", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(completedCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    val filteredBookings = bookings.filter { b ->
        when (state.adminBookingSubTab) {
            "SERVICES" -> b.providerId.contains("SERVICES") || b.providerId == "ALL" ||
                (!b.providerId.contains("PROPERTIES") && !b.providerId.contains("STORES") &&
                    !b.providerId.contains("MEDICAL") && !b.providerId.contains("RESTAURANTS") && !b.providerId.contains("JOBS"))
            "PROPERTIES" -> b.providerId.contains("PROPERTIES")
            "STORES" -> b.providerId.contains("STORES")
            "MEDICAL" -> b.providerId.contains("MEDICAL")
            "RESTAURANTS" -> b.providerId.contains("RESTAURANTS")
            "JOBS" -> b.providerId.contains("JOBS")
            else -> true
        }
    }

    if (filteredBookings.isEmpty()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                Text("لا توجد طلبات حجز مطابقة في هذا القسم حالياً.", fontSize = 11.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
            }
        }
    } else {
        items(filteredBookings, key = { it.id }) { b ->
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, if (b.status == "PENDING") themeColors.accent else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("اسم العميل: ${b.customerName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val bColor = when (b.status) {
                            "APPROVED" -> Color.Green
                            "COMPLETED" -> Color(0xFF10B981)
                            "IN_PROGRESS" -> Color(0xFF3B82F6)
                            "REJECTED" -> Color.Red
                            else -> themeColors.accent
                        }
                        Text(
                            text = when (b.status) {
                                "APPROVED" -> "معتمد"
                                "COMPLETED" -> "مكتمل"
                                "IN_PROGRESS" -> "جاري التنفيذ"
                                "REJECTED" -> "مرفوض"
                                else -> "بانتظار الموافقة"
                            },
                            fontSize = 11.sp,
                            color = bColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("هاتف العميل: ${b.customerPhone}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Text("منطقة السكن والحي: ${b.customerArea}", fontSize = 11.sp, color = themeColors.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📅 تاريخ الحجز: ${b.dateString}", fontSize = 11.sp, color = Color.White)
                        Text("🕒 وقت الحجز: ${b.timeString}", fontSize = 11.sp, color = Color.White)
                    }
                    if (b.serviceType.isNotEmpty()) {
                        Text("نوع الخدمة المطلوبة: ${b.serviceType}", fontSize = 11.sp, color = Color.Yellow)
                    }
                    Text("اسم الفني المستهدف: ${b.providerName}", fontSize = 11.sp, color = themeColors.accent)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (b.status == "PENDING" || b.status == "UNDER_REVIEW") {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("قبول وبدء الحجز", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        state.bookingRejectionReasonInput = ""
                                        state.showRejectionReasonDialogId = b.id
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("رفض الحجز", color = Color.White, fontSize = 10.sp)
                                }
                            } else if (b.status == "APPROVED") {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("بدء التنفيذ", color = Color.White, fontSize = 10.sp)
                                }
                            } else if (b.status == "IN_PROGRESS") {
                                Button(
                                    onClick = { viewModel.updateBookingStatus(b.id, "COMPLETED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("إكمال الخدمة (مكتمل)", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.openOrCreateChatChannel(
                                        targetId = b.clientId.takeIf { it.isNotBlank() } ?: "user_${b.customerPhone}",
                                        targetType = "CLIENT",
                                        targetName = b.customerName,
                                        targetPhone = b.customerPhone,
                                        relatedEntityId = b.id,
                                        relatedEntityType = "BOOKING"
                                    ) { created ->
                                        created?.let { state.showActiveChatChannelObj = it }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "دردشة فورية للتنسيق", tint = Color.Green, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { state.redirectingBookingObj = b }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "توجيه الحجز", tint = Color.Cyan, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { state.editingBookingObj = b }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الحجز", tint = Color.Green, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { state.showDeleteBookingConfirmId = b.id }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
