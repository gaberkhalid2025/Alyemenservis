package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
/ ⚙️ AdminBookingSettingsPanel
/ لوحة التحكم الشاملة بإعدادات الحجوزات ونظام التوجيه وصلاحيات الدفع
/ - إظهار / إخفاء زر الحجز على بطاقات التقديم.
/ - توجيه الحجوزات (أدمن فقط / أدمن ومحكم / أدمن ومزود الخدمة).
/ - تفعيل / تعطيل / إخفاء نظام الدفع المالي وربطه بالحجز.
/ - تجاوز شرط الـ 8 ساعات للإدمن عند التعديل والإلغاء.
 */
@Composable
fun AdminBookingSettingsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val currentSettings by viewModel.bookingSystemSettings.collectAsState()

    var showBookingButton by remember(currentSettings) { mutableStateOf(currentSettings.showBookingButtonOnCards) }
    var routingMode by remember(currentSettings) { mutableStateOf(currentSettings.bookingRoutingMode) }
    var paymentEnabled by remember(currentSettings) { mutableStateOf(currentSettings.paymentSystemEnabled) }
    var paymentLinkedToBooking by remember(currentSettings) { mutableStateOf(currentSettings.paymentSystemLinkedToBooking) }
    var paymentHidden by remember(currentSettings) { mutableStateOf(currentSettings.paymentSystemHidden) }
    var adminBypass by remember(currentSettings) { mutableStateOf(currentSettings.adminBypass8HourRestriction) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = themeColors.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "⚙️ إعدادات الحجوزات ونظام الدفع",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "التحكم الكامل بظهور أزرار الحجز، قواعد التوجيه، ونظام الدفع المالي",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Section 1: Show/Hide Booking Buttons on Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📌 عرض أزرار الحجز المباشر", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.accent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إظهار زر 'حجز موعد' على البطاقات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("عند الإيقاف، سيختفي زر الحجز من بطاقات المزودين والمحلات والعقارات.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = showBookingButton,
                        onCheckedChange = { showBookingButton = it }
                    )
                }
            }
        }

        // Section 2: Booking Routing Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔀 توجيه وإدارة الحجوزات", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.accent)
                Text("حدد الأطراف المصرح لها باستلام والتحكم بالحجوزات الجديدة:", fontSize = 11.sp, color = Color.Gray)

                val routingOptions = listOf(
                    Triple("ADMIN_ONLY", "🛡️ إدارة المنصة فقط", "تصل الحجوزات إلى لوحة الإدارة فقط ويتم توزيعها يدويًا"),
                    Triple("ADMIN_AND_MODERATOR", "👥 الإدارة + المشرفين", "تستلم الإدارة والمشرفون المعتمدون الحجوزات لتوجيهها"),
                    Triple("ADMIN_AND_PROVIDER", "🛠️ الإدارة + مقدم الخدمة (تلقائي)", "يصل الحجز مباشرة للفني/المحل مع إشعار فوري للإدارة")
                )

                routingOptions.forEach { (code, title, desc) ->
                    val isSelected = routingMode == code
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) themeColors.accent.copy(alpha = 0.15f) else Color(0xFF0F172A))
                            .border(1.dp, if (isSelected) themeColors.accent else Color.DarkGray, RoundedCornerShape(12.dp))
                            .clickable { routingMode = code }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { routingMode = code }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) themeColors.accent else Color.White)
                                Text(desc, fontSize = 10.5.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Payment System Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("💳 خيارات ونظام الدفع المالي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.accent)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تفعيل نظام الدفع المالي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("السماح بالدفع الإلكتروني وسندات التحويل المالي", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = paymentEnabled, onCheckedChange = { paymentEnabled = it })
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ربط نظام الدفع بالحجز المباشر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("إلزام العميل برفق إثبات الدفع لتأكيد الحجز", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = paymentLinkedToBooking, onCheckedChange = { paymentLinkedToBooking = it }, enabled = paymentEnabled)
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إخفاء نظام الدفع بالكامل من الواجهة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("إخفاء أيقونة وتفاصيل الدفع عن الزوار والعملاء", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = paymentHidden, onCheckedChange = { paymentHidden = it })
                }
            }
        }

        // Section 4: Admin Bypass Policy
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚡ صلاحيات الإدارة المطلقة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.accent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تجاوز شرط الـ 8 ساعات للإدمن", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("السماح للإدارة بتعديل أو إلغاء أو حذف أي حجز فوراً دون أي تقييد زمني.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = adminBypass, onCheckedChange = { adminBypass = it })
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                val updated = MainViewModel.BookingSystemSettings(
                    showBookingButtonOnCards = showBookingButton,
                    bookingRoutingMode = routingMode,
                    paymentSystemEnabled = paymentEnabled,
                    paymentSystemLinkedToBooking = paymentLinkedToBooking,
                    paymentSystemHidden = paymentHidden,
                    adminBypass8HourRestriction = adminBypass
                )
                viewModel.updateBookingSystemSettings(updated)
                Toast.makeText(context, "✅ تم حفظ إعدادات الحجوزات والنظام بنجاح!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ التعديلات والإعدادات 💾", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
