@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentAdminSettingsEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 💳 لوحة تحكم المدفوعات والاشتراكات وصلاحيات الأدمن (Admin Payment & Billing Panel)
 * تتيح التحكم بالنظام المالي، ربط أو إلغاء ربط القطاعات، وتعديل أرصدة المحافظ والمدفوعات
 */
@Composable
fun AdminPaymentPanel(
    viewModel: MainViewModel? = null,
    themeColors: VisualThemePalette? = null,
    onToggleSystem: ((Boolean) -> Unit)? = null,
    onUnlinkAll: (() -> Unit)? = null,
    onAdminOverride: ((String, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val primaryColor = themeColors?.primary ?: Color(0xFF0D9488)
    val cardBg = themeColors?.surface ?: Color(0xFF1E293B)
    val textColor = themeColors?.textPrimary ?: Color.White

    var systemEnabled by remember { mutableStateOf(true) }
    var linkBookings by remember { mutableStateOf(true) }
    var linkStores by remember { mutableStateOf(true) }
    var linkRestaurants by remember { mutableStateOf(true) }
    var linkMedical by remember { mutableStateOf(true) }
    var linkProperties by remember { mutableStateOf(true) }
    var linkJobs by remember { mutableStateOf(true) }

    var overrideTargetId by remember { mutableStateOf("") }
    var overrideAmount by remember { mutableStateOf("") }
    var showOverrideDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "لوحة تحكم المدفوعات والاشتراكات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "إدارة بوابات الدفع، ربط القطاعات، وتجاوزات الإدارة المالية",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = textColor.copy(alpha = 0.1f)
                    )

                    // Master Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (systemEnabled) primaryColor.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "تفعيل نظام الدفع الكلي",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 15.sp
                            )
                            Text(
                                if (systemEnabled) "النظام المالي نشط ويعمل بجميع الخدمات" else "النظام المالي معطل مؤقتاً (وضع الدفع المباشر)",
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = systemEnabled,
                            onCheckedChange = {
                                systemEnabled = it
                                onToggleSystem?.invoke(it)
                                Toast.makeText(
                                    context,
                                    if (it) "تم تفعيل نظام الدفع الكلي" else "تم تعطيل نظام الدفع الكلي",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔗 ربط منظومة الدفع بالقطاعات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentSectorToggleRow(
                        title = "خدمات الحجوزات والفنيين",
                        subtitle = "حساب العربون والعمولات تلقائياً",
                        icon = Icons.Default.DateRange,
                        checked = linkBookings,
                        onCheckedChange = { linkBookings = it }
                    )

                    PaymentSectorToggleRow(
                        title = "المتاجر والمنتجات",
                        subtitle = "الدفع عبر المحافظ الإلكترونية",
                        icon = Icons.Default.ShoppingCart,
                        checked = linkStores,
                        onCheckedChange = { linkStores = it }
                    )

                    PaymentSectorToggleRow(
                        title = "المطاعم والطلبات",
                        subtitle = "تسوية الفواتير والتوصيل",
                        icon = Icons.Default.ShoppingCart,
                        checked = linkRestaurants,
                        onCheckedChange = { linkRestaurants = it }
                    )

                    PaymentSectorToggleRow(
                        title = "المراكز الطبية والعيادات",
                        subtitle = "رسوم المعاينة والاستشارات",
                        icon = Icons.Default.Info,
                        checked = linkMedical,
                        onCheckedChange = { linkMedical = it }
                    )

                    PaymentSectorToggleRow(
                        title = "العقارات والإيجارات",
                        subtitle = "عربون حجز المعاينة أو الإيجار",
                        icon = Icons.Default.Home,
                        checked = linkProperties,
                        onCheckedChange = { linkProperties = it }
                    )

                    PaymentSectorToggleRow(
                        title = "بوابة الوظائف",
                        subtitle = "رسوم التقديم أو توثيق الحسابات",
                        icon = Icons.Default.Person,
                        checked = linkJobs,
                        onCheckedChange = { linkJobs = it }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚙️ إجراءات وتجاوزات الأدمن (Admin Overrides)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showOverrideDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعديل رصيد محفظة / اعتماد دفع يدوي")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            linkBookings = false
                            linkStores = false
                            linkRestaurants = false
                            linkMedical = false
                            linkProperties = false
                            linkJobs = false
                            onUnlinkAll?.invoke()
                            Toast.makeText(context, "تم إلغاء الربط الشامل لكل القطاعات", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إلغاء الربط الشامل لكل القطاعات")
                    }
                }
            }

    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("تعديل رصيد أو تجاوز دفع إداري") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = overrideTargetId,
                        onValueChange = { overrideTargetId = it },
                        label = { Text("معرف المستخدم / المحفظة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = overrideAmount,
                        onValueChange = { overrideAmount = it },
                        label = { Text("المبلغ (ريال يمني)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = overrideAmount.toDoubleOrNull() ?: 0.0
                        onAdminOverride?.invoke(overrideTargetId, amt)
                        Toast.makeText(context, "تم تسجيل التعديل الإداري بنجاح: $amt ر.ي", Toast.LENGTH_SHORT).show()
                        showOverrideDialog = false
                    }
                ) {
                    Text("تنفيذ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
}

@Composable
fun PaymentSectorToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
