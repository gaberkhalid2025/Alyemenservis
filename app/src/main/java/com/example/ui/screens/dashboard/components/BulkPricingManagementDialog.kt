package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun BulkPricingManagementDialog(
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    var percentage by remember { mutableStateOf("10") }
    var isIncrease by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("إدارة الأسعار الجماعية 📈📉", fontSize = 16.sp, color = themeColors.primary)
            Text("تطبيق تعديل نسبوي على جميع الخدمات والمنتجات دفعة واحدة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isIncrease = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isIncrease) themeColors.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("زيادة الأسعار (+)", fontSize = 12.sp)
                }
                Button(
                    onClick = { isIncrease = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isIncrease) themeColors.primary else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("خفض الأسعار (-)", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = percentage,
                onValueChange = { percentage = it },
                label = { Text("النسبة المئوية %") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("إلغاء") }
                Button(
                    onClick = {
                        // Apply bulk pricing logic
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("تنفيذ التعديل الجماعي")
                }
            }
        }
    }
}
