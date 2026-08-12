package com.example.ui.screens.payments

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun InvoiceGenerator(
    themeColors: VisualThemePalette,
    invoiceNumber: String = "INV-2026-08129",
    clientName: String = "صالح العولقي",
    serviceName: String = "صيانة مكيفات هواء - منزلية",
    providerName: String = "المقاول فؤاد للكهربائيات",
    cost: Double = 15000.0,
    tax: Double = 500.0
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄 تفاصيل فاتورة الدفع والخدمة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )

                Text(
                    text = invoiceNumber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textSecondary
                )
            }

            Divider(color = themeColors.accent.copy(alpha = 0.2f))

            InvoiceRow("اسم العميل:", clientName, themeColors)
            InvoiceRow("مقدم الخدمة:", providerName, themeColors)
            InvoiceRow("الخدمة المنجزة:", serviceName, themeColors)

            Divider(color = Color.Gray.copy(alpha = 0.15f))

            InvoiceRow("قيمة الخدمة الأساسية:", "$cost ر.ي", themeColors)
            InvoiceRow("رسوم دعم وتأمين الخدمة المضافة:", "$tax / مجانية", themeColors)

            Divider(color = themeColors.accent.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المجموع النهائي الصافي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                Text(
                    text = "${cost + tax} ريال يمني",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "جاري معالجة وتصدير الفاتورة كـ PDF وتخزينها في التنزيلات...", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تحميل PDF 📥", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "تم توليد رابط مشاركة سريع للفاتورة وإرساله للحافظة", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color.Gray),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة الرابط", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, themeColors: VisualThemePalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = themeColors.textSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
    }
}
