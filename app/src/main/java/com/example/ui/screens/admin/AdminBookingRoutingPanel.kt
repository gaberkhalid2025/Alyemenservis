@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminBookingRoutingPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_BOOKING_ROUTING")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("⚙️ لوحة التحكم بمسار وحقول استمارة الحجز الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Routing control
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📍 توجيه الحجوزات الواردة من العملاء:", fontSize = 11.sp, color = themeColors.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val rModes = listOf(
                            Pair("BOTH", "الأدمن والفني 👥"),
                            Pair("ADMIN", "الأدمن فقط 👮"),
                            Pair("PROVIDER", "الفني مباشرة 🛠️")
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

                // Booking Accessibility and Icon visibility controls
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

                // Booking terms text field
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

                Button(
                    onClick = {
                        viewModel.saveCustomSettingsState(settingsState)
                        Toast.makeText(context, "✅ تم حفظ ومزامنة مسارات وإعدادات الحجوزات بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ ومزامنة توجيه وإعدادات الحجز 💾", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
