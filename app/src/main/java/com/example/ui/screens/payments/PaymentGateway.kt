package com.example.ui.screens.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun PaymentGateway(
    themeColors: VisualThemePalette,
    amount: Double,
    onPaymentSuccess: (String) -> Unit,
    onPaymentCancel: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CASH") } // CASH, JEEB, JAWALY, KREEMY, BANK

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💳 بوابة الدفع الإلكتروني اليمنية الموحدة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المبلغ الإجمالي المستحق للخدمة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Text(
                    text = "$amount ريال يمني",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }

            Text("اختر وسيلة الدفع المناسبة لك:", fontSize = 11.sp, color = themeColors.textSecondary)

            val paymentMethods = listOf(
                "CASH" to "💵 دفع نقدي عند الاستلام",
                "JEEB" to "📱 محفظة جيب الإلكترونية (Jeeb)",
                "JAWALY" to "📱 محفظة جوالي الإلكترونية",
                "KREEMY" to "🏦 حساب الكريمي أونلاين (M-Karimi)",
                "BANK" to "🏦 تحويل بنكي مباشر / سداد صراف"
            )

            paymentMethods.forEach { (id, label) ->
                val isSelected = selectedMethod == id
                Surface(
                    onClick = { selectedMethod = id },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) themeColors.accent.copy(alpha = 0.15f) else themeColors.background,
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { selectedMethod = id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 12.sp, color = themeColors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPaymentSuccess(selectedMethod) },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إتمام الدفع الآمن 🚀", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onPaymentCancel,
                    border = BorderStroke(1.dp, Color.Gray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء العملية", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
