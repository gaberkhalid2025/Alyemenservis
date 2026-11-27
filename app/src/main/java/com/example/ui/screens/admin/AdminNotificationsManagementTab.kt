@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminNotificationsManagementTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val notifications = state.notifications
    val activatedProviders = state.activatedProviders
    val supervisorsList = state.supervisorsList

    item {
        Spacer(modifier = Modifier.height(14.dp))
        Text("🔔 نظام الإشعارات الذكية الموجهة (Targeted)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إرسال إشعار فوري موجه:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = state.notifTitleInput,
                    onValueChange = { state.notifTitleInput = it },
                    label = { Text("عنوان الإشعار (مثال: خصم هائل اليوم!)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = state.notifMsgInput,
                    onValueChange = { state.notifMsgInput = it },
                    label = { Text("مضمون الرسالة بالتفصيل") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("نطاق ومجموعة المستهدفين بالبث الفوري:", fontSize = 11.sp, color = themeColors.textSecondary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    listOf(
                        Pair("ALL", "الجميع 🌍"),
                        Pair("USER", "العملاء 👤"),
                        Pair("PROVIDER", "الفنيون 🔧"),
                        Pair("STORE", "المتاجر 🏪"),
                        Pair("RESTAURANT", "المطاعم 🍔"),
                        Pair("MEDICAL", "المراكز الطبية 🏥"),
                        Pair("PROPERTY", "العقارات 🏢"),
                        Pair("JOB", "الوظائف 💼"),
                        Pair("SUPERVISOR", "المشرفون 👮"),
                        Pair("AREA", "محافظة 📍")
                    ).forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { state.notifTargetType = type }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = state.notifTargetType == type, onClick = { state.notifTargetType = type })
                            Text(label, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                if (state.notifTargetType != "ALL") {
                    OutlinedTextField(
                        value = state.notifTargetValue,
                        onValueChange = { state.notifTargetValue = it },
                        label = {
                            val lbl = when (state.notifTargetType) {
                                "USER" -> "رقم هاتف العميل للتوصيل"
                                "PROVIDER" -> "رقم هاتف أو معرّف الفني"
                                "SUPERVISOR" -> "معرّف/اسم المشرف المالي"
                                else -> "اسم المحافظة/المدينة المستهدفة"
                            }
                            Text(lbl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    if (state.notifTargetType == "PROVIDER" && activatedProviders.isNotEmpty()) {
                        Text("اختيار سريع من الفنيين المعتمدين بالمنصة:", fontSize = 10.sp, color = themeColors.accent)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(activatedProviders) { prov ->
                                AssistChip(
                                    onClick = { state.notifTargetValue = prov.phone },
                                    label = { Text(prov.name, fontSize = 10.sp, color = Color.White) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }

                    if (state.notifTargetType == "SUPERVISOR" && supervisorsList.isNotEmpty()) {
                        Text("اختيار سريع من المشرفين المتواجدين:", fontSize = 10.sp, color = themeColors.accent)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(supervisorsList) { sup ->
                                AssistChip(
                                    onClick = { state.notifTargetValue = sup.name },
                                    label = { Text(sup.name, fontSize = 10.sp, color = Color.White) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = themeColors.primary.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                }

                Text("⏱️ جدولة البث وتحديد مدة صلاحية الإشعار (اختياري):", fontSize = 11.sp, color = themeColors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.notifDelayHours,
                        onValueChange = { state.notifDelayHours = it },
                        label = { Text("تأخير الإرسال (ساعات)") },
                        placeholder = { Text("فوري = فارغ") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = state.notifValidityHours,
                        onValueChange = { state.notifValidityHours = it },
                        label = { Text("مدة الصلاحية (ساعات)") },
                        placeholder = { Text("دائم = فارغ") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Button(
                    onClick = {
                        if (state.notifTitleInput.trim().isEmpty() || state.notifMsgInput.trim().isEmpty()) {
                            Toast.makeText(context, "يرجى تعبئة العنوان ونص الرسالة", Toast.LENGTH_SHORT).show()
                        } else {
                            val delayH = state.notifDelayHours.toDoubleOrNull() ?: 0.0
                            val validH = state.notifValidityHours.toDoubleOrNull() ?: 0.0

                            val scheduledTime = if (delayH > 0) {
                                System.currentTimeMillis() + (delayH * 3600 * 1000).toLong()
                            } else 0L

                            val expiryTimestamp = if (validH > 0) {
                                (if (scheduledTime > 0) scheduledTime else System.currentTimeMillis()) + (validH * 3600 * 1000).toLong()
                            } else 0L

                            viewModel.addNotification(
                                title = state.notifTitleInput,
                                message = state.notifMsgInput,
                                targetType = state.notifTargetType,
                                targetValue = if (state.notifTargetType == "ALL") "" else state.notifTargetValue,
                                expiryTimestamp = expiryTimestamp,
                                scheduledTime = scheduledTime
                            )
                            state.notifTitleInput = ""
                            state.notifMsgInput = ""
                            state.notifTargetValue = ""
                            state.notifDelayHours = ""
                            state.notifValidityHours = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بث وإرسال الإشعار للمستهدفين المحددين 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("⚡ نماذج سريعة للإرسال والبث الفوري لجميع المستخدمين:", fontSize = 11.sp, color = themeColors.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.addNotification(
                                title = "🔥 عرض حصري محدود من إدارة المنصة",
                                message = "خصم يصل إلى 35% على خدمات التكييف، التمديدات وصيانة الأجهزة المنزلية اليوم فقط! احجز فنيك الآن عبر التطبيق.",
                                targetType = "ALL",
                                targetValue = ""
                            )
                            Toast.makeText(context, "تم بث عرض ترويجي للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("بث عرض ترويجي 📢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.addNotification(
                                title = "🛠️ تحديث فني هام للنظام وتطوير الأداء",
                                message = "عملائنا الأعزاء، نود إعلامكم بإطلاق تحديث جديد للبحث الجغرافي وحساب المسافات بأعلى دقة. نوصي بتحديث التطبيق الآن.",
                                targetType = "ALL",
                                targetValue = ""
                            )
                            Toast.makeText(context, "تم بث إعلان تحديث هام للجميع بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("بث تحديث فني ⚙️", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    item {
        Text("📋 تاريخ الرسائل البوش السابقة (${notifications.size}):", fontSize = 12.sp, color = themeColors.textSecondary)
    }

    items(notifications, key = { it.id }) { n ->
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(n.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = { state.showDeleteNotifConfirmId = n.id }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(n.message, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                val filterStr = when (n.targetType) {
                    "USER" -> "مستهدف صريح بالهاتف: ${n.targetValue}"
                    "AREA" -> "محافظة يمنية محددة: ${n.targetValue}"
                    else -> "جميع المشتركين بالمنصة"
                }
                Text("نطاق الاستهداف: $filterStr", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

                val isRecovery = n.title.contains("استعادة") || n.title.contains("طلب") || n.message.contains("يطلب") || n.message.contains("هاتف")
                if (isRecovery) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { state.selectedRecoveryNotif = n },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🔓 إدارة واستعادة كلمة المرور ومراسلة المستخدم", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
