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
import com.example.data.UnifiedBusinessAccount

import com.example.utils.VisualThemePalette

/**
 * 📋 Unified Profile Section Component
 */
@Composable
fun UnifiedProfileSection(
    account: UnifiedBusinessAccount,
    viewModel: AuthViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(account.name) }
    var description by remember { mutableStateOf(account.description) }
    var phone by remember { mutableStateOf(account.phone) }
    var ownerName by remember { mutableStateOf(account.ownerName) }
    var workingHours by remember { mutableStateOf(account.workingHours) }
    var neighborhood by remember { mutableStateOf(account.neighborhood) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📝 البيانات الشخصية والمعلومات العامة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم التجاري / اسم المنشأة", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم صاحب العمل / المدير المسجل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف والواتساب للتواصل", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("نبذة وتفاصيل وصفية كاملة عن الخدمة/المكان", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        label = { Text("أوقات وساعات العمل والدوام (مثال: 8:00 AM - 10:00 PM)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("العنوان والحي والمنطقة التفصيلية", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.accent)
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                if (account.rawStore != null) {
                                    val updated = account.rawStore.copy(
                                        name = name,
                                        description = description,
                                        phone = phone,
                                        ownerName = ownerName,
                                        workingHours = workingHours,
                                        localNeighborhood = neighborhood
                                    )
                                    viewModel.saveStore(updated)
                                } else if (account.rawProvider != null) {
                                    val updated = account.rawProvider.copy(
                                        name = name,
                                        phone = phone,
                                        localNeighborhood = neighborhood,
                                        profession = description
                                    )
                                    viewModel.updateProviderEntity(updated)
                                }
                                Toast.makeText(context, "✅ تم حفظ التعديلات سحابياً بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "⚠️ يرجى تعبئة الحقول الأساسية", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتحديث البيانات الشخصية 💾", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
