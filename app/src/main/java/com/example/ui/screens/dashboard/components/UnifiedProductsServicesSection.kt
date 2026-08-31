package com.example.ui.screens.dashboard.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount

import com.example.utils.VisualThemePalette

/**
 * 🛒 Unified Products / Services Section Component
 */
@Composable
fun UnifiedProductsServicesSection(
    account: UnifiedBusinessAccount,
    viewModel: AuthViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 قائمة المنتجات / الخدمات / الأطباق", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
            ) {
                Text("إضافة جديدة ➕", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد منتجات مسجلة حالياً بهذا الحساب.", fontSize = 11.sp, color = Color.LightGray)
                Text("اضغط على زر (إضافة جديدة) لإدراج قائمة منتجاتك أو خدماتك بالأسعار والصور.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة عنصر جديد 🛒", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("اسم المنتج / الخدمة / الطبق", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("السعر (بالريال اليمني YER)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("التفاصيل والوصف", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prodName.isNotBlank()) {
                            Toast.makeText(context, "✅ تم إضافة العنصر بنجاح!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                            prodName = ""
                            prodPrice = ""
                            prodDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("حفظ وإضافة 💾", fontSize = 11.sp, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        )
    }
}
