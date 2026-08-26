package com.example.ui.screens.register.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 📜 RegistrationTermsCheckbox - مربع الموافقة على الشروط والأحكام وسياسة الخصوصية
 */
@Composable
fun RegistrationTermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var showTermsDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = themeColors.accent,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            )
        )
        Row(
            modifier = Modifier.clickable { showTermsDialog = true }
        ) {
            Text(
                text = "أوافق على ",
                fontSize = 11.sp,
                color = Color.LightGray
            )
            Text(
                text = "شروط الاستخدام وسياسة الخصوصية",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Text(
                text = " بالمنصة",
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("📜 شروط الاستخدام وسياسة الخصوصية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. التعهد بصحة البيانات ورقم الهاتف المدخل المعتمد بجمهورية اليمن.", fontSize = 11.sp, color = Color.White)
                    Text("2. منع الإعلانات الوهمية أو الانتحال أو نشر خدمات مخلفة للآداب والأنظمة.", fontSize = 11.sp, color = Color.White)
                    Text("3. للإدارة حق مراجعة الوثائق المرفقة قبل التفعيل الفعلي للحسابات.", fontSize = 11.sp, color = Color.White)
                    Text("4. يتم تشفير وسائط المحادثات وحماية الخصوصية طبقاً لأعلى معايير الأمان.", fontSize = 11.sp, color = Color.White)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCheckedChange(true)
                        showTermsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("موافقة وقبول", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("إغلاق", color = Color.Gray, fontSize = 11.sp)
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}
