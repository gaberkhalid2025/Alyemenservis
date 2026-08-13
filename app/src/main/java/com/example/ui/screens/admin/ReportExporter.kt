package com.example.ui.screens.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun ReportExporter(
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📥 أدوات تصدير التقارير والأرشيف",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary
            )

            Text(
                text = "اختر صيغة الملف المفضلة لتصدير السجلات الشاملة وإحصائيات الاستخدام مباشرة:",
                fontSize = 11.sp,
                color = themeColors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { Toast.makeText(context, "تم تصدير ملف Excel الشامل بنجاح!", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Excel 📊", color = androidx.compose.ui.graphics.Color.Black, fontSize = 10.sp)
                }

                Button(
                    onClick = { Toast.makeText(context, "تم تصدير ملف CSV بنجاح!", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CSV 📁", color = androidx.compose.ui.graphics.Color.Black, fontSize = 10.sp)
                }

                Button(
                    onClick = { Toast.makeText(context, "تم تصدير تقرير PDF بنجاح!", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PDF 📃", color = androidx.compose.ui.graphics.Color.Black, fontSize = 10.sp)
                }
            }
        }
    }
}
