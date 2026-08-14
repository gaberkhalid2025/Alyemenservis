package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminVipPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_VIP")) {
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
    val activatedProviders by viewModel.providers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("💳 لوحة التحكم باشتراكات الفنيين والتجديد والترقيات VIP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("إدارة فترات الصلاحية وشارات الإعلانات، وبث إشعارات التحذير قبل الانتهاء بـ 48 ساعة:", fontSize = 11.sp, color = themeColors.textSecondary)

        // Global Alert Button
        Button(
            onClick = {
                var sentCount = 0
                val fortyEightHoursMs = 48L * 60 * 60 * 1000
                activatedProviders.forEach { p ->
                    val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                    if (timeLeft > 0 && timeLeft <= fortyEightHoursMs) {
                        viewModel.addNotification(
                            title = "تنبيه هام بفترة تجديد الاشتراك",
                            message = "عزيزنا الفني المعتمد ${p.name}، يرجى التنويه بأن اشتراكك الفني ينتهي خلال أقل من 48 ساعة. يرجى تجديد الاشتراك فوراً لتفادي تجميد حسابك.",
                            targetType = "USER",
                            targetValue = p.phone
                        )
                        sentCount++
                    }
                }
                if (sentCount > 0) {
                    Toast.makeText(context, "تم بث تنبيهات بوش تلقائية لعدد ($sentCount) فنيين اشتراكهم ينتهي خلال 48 ساعة!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "لم يتم العثور على أي فنيين اقترب انتهاء اشتراكهم (تحت 48 ساعة) في السجلات حالياً.", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚨 بث تلقائي لتنبيهات 48 ساعة لجميع الفنيين المستهدفين", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (activatedProviders.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("لا يوجد فنيين مسجلين في النظام حالياً.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            activatedProviders.forEach { p ->
                val timeLeft = p.subscriptionExpiry - System.currentTimeMillis()
                val daysLeft = (timeLeft / (24L * 60 * 60 * 1000)).toInt()
                val hoursLeft = ((timeLeft % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)).toInt()

                val timeString = if (timeLeft < 0) {
                    "منتهي الصلاحية ❌"
                } else if (daysLeft > 0) {
                    "متبقي $daysLeft يوم و$hoursLeft ساعة"
                } else {
                    "متبقي $hoursLeft ساعة فقط ⚠️"
                }

                val isNearExpiry = timeLeft > 0 && timeLeft <= (48L * 60 * 60 * 1000)

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    border = BorderStroke(1.dp, if (isNearExpiry) Color.Red else themeColors.accent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = timeString,
                                fontSize = 11.sp,
                                color = if (timeLeft < 0) Color.Red else if (isNearExpiry) Color.Yellow else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("رقم الهاتف: ${p.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                        Text("حالة الاشتراك الفني: ${p.subscriptionStatus}", fontSize = 11.sp, color = themeColors.accent)

                        if (!p.password.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("🔑 كلمة المرور: ${p.password}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val whatsappText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني في دليل خدمات اليمن هي: ${p.password}"
                                        val whatsappUrl = "https://wa.me/967${p.phone.trim().removePrefix("0").removePrefix("+967")}?text=${Uri.encode(whatsappText)}"
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "فشل فتح واتساب", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("🟢 واتساب", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val smsText = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني في دليل خدمات اليمن هي: ${p.password}"
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${p.phone}")).apply {
                                                putExtra("sms_body", smsText)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "فشل فتح SMS", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("💬 SMS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.addNotification(
                                            title = "🔑 تذكير بكلمة المرور الخاصة بك",
                                            message = "مرحباً يا غالي، كلمة المرور الخاصة بحسابك الفني هي: ${p.password}",
                                            targetType = "USER",
                                            targetValue = p.phone
                                        )
                                        Toast.makeText(context, "تم إرسال تذكير بكلمة المرور للفني بنجاح كإشعار داخلي", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("📱 إشعار", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    viewModel.extendProviderSubscription(p.id, 30L * 24 * 60 * 60 * 1000)
                                    Toast.makeText(context, "تم تجديد اشتراك ${p.name} لمدة 30 يوماً بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تجديد 30 يوم 🟢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.addNotification(
                                        title = "تنبيه هام بانتهاء صلاحية الاشتراك",
                                        message = "عزيزنا الفني ${p.name}، نود تذكيرك بأن اشتراكك ينتهي خلال 48 ساعة فقط. الرجاء المسارعة بالتجديد للاستمرار بظهور اسمك للزبائن في التطبيق.",
                                        targetType = "USER",
                                        targetValue = p.phone
                                    )
                                    Toast.makeText(context, "تم إرسال إشعار بوش يدوي ينبه الفني بالفترة المحددة بـ 48 ساعة", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تنبيه بـ 48 ساعة 🔔", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
