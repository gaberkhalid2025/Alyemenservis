package com.example.ui.screens.admin

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.data.CouponManager

@Composable
fun AdminCouponsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_COUPONS")) {
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

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🎫 إدارة الكوبونات وخصومات العروض", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            var couponCodeInput by remember { mutableStateOf("") }
            var discountInput by remember { mutableStateOf("") }
            var descInput by remember { mutableStateOf("") }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إضافة كوبون خصم جديد:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = couponCodeInput,
                        onValueChange = { couponCodeInput = it },
                        label = { Text("رمز الكوبون (مثال: SAVE20)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("نسبة الخصم (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("وصف الكوبون") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val disc = discountInput.toIntOrNull() ?: 10
                            if (couponCodeInput.isNotBlank()) {
                                CouponManager.createCoupon(couponCodeInput, disc, descInput)
                                couponCodeInput = ""
                                discountInput = ""
                                descInput = ""
                                viewModel.triggerNotification("✅ تم إضافة الكوبون بنجاح")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text("حفظ الكوبون 💾", color = Color.White)
                    }
                }
            }

            Text("📋 الكوبونات المتاحة حالياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val allCoupons = remember(couponCodeInput) { CouponManager.getAllCoupons() }
            allCoupons.forEach { coupon ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("كود: ${coupon.code} (${coupon.discountPercent}% خصم)", fontWeight = FontWeight.Bold, color = themeColors.accent, fontSize = 12.sp)
                            Text(coupon.description, color = Color.LightGray, fontSize = 10.sp)
                        }
                        Text(if (coupon.isUsed) "مستخدم ❌" else "نشط ✅", color = if (coupon.isUsed) Color.Red else Color.Green, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
