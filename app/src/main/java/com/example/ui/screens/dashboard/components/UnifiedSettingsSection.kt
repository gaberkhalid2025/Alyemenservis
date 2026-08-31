package com.example.ui.screens.dashboard.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BusinessType
import com.example.data.UnifiedBusinessAccount

import com.example.utils.VisualThemePalette

/**
 * ⚙️ Unified Settings & Permissions Section Component
 */
@Composable
fun UnifiedSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: AuthViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf(account.password) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙️ الإعدادات والصلاحيات والأمن", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور المشفرة للوصول لوحة التحكم", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    Button(
                        onClick = {
                            if (newPassword.isNotBlank()) {
                                viewModel.resetAccountPassword(
                                    if (account.businessType == BusinessType.TECHNICIAN) "PROVIDER" else "STORE",
                                    account.phone,
                                    newPassword
                                )
                                Toast.makeText(context, "🔒 تم مشفر وتحديث كلمة المرور بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تشفير وتحديث كلمة المرور 🔑", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
