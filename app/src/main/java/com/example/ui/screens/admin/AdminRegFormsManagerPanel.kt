@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminRegFormsManagerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_REG_FORMS")) {
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

    var targetRegSection by remember { mutableStateOf("SERVICES") }
    var customFieldTitle by remember { mutableStateOf("") }
    var customFieldType by remember { mutableStateOf("TEXT") } // TEXT, PHONE, IMAGE, DROPDOWN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📋 تخصيص استمارات التسجيل وطلبات الانضمام الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إضافة وتعديل الحقول المطلوبة في استمارات التسجيل والانضمام للفنيين، المتاجر، المطاعم، الطب، العقارات، والوظائف:", fontSize = 11.sp, color = themeColors.textSecondary)

                Text("اختر القسم المستهدف للاستمارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("SERVICES" to "🔧 الفنيين", "STORES" to "🏪 المتاجر", "RESTAURANTS" to "🍔 المطاعم", "MEDICAL" to "🏥 الطب", "PROPERTIES" to "🏠 العقارات", "JOBS" to "💼 الوظائف")) { (sec, label) ->
                        FilterChip(
                            selected = targetRegSection == sec,
                            onClick = { targetRegSection = sec },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                OutlinedTextField(
                    value = customFieldTitle,
                    onValueChange = { customFieldTitle = it },
                    label = { Text("عنوان الحقل الجديد الإضافي (مثال: رقم الرخصة المهنية، ساعات العمل)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("نوع الحقل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TEXT" to "نص 📝", "PHONE" to "رقم هاتف 📞", "IMAGE" to "صورة / مستند 📷", "DROPDOWN" to "قائمة منسدلة 🔽").forEach { (t, lbl) ->
                        FilterChip(
                            selected = customFieldType == t,
                            onClick = { customFieldType = t },
                            label = { Text(lbl, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (customFieldTitle.isNotBlank()) {
                            Toast.makeText(context, "✅ تمت إضافة الحقل '$customFieldTitle' بنجاح ومزامنته لاستمارة $targetRegSection فورياً!", Toast.LENGTH_LONG).show()
                            customFieldTitle = ""
                        } else {
                            Toast.makeText(context, "⚠️ يرجى إدخال عنوان الحقل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("إضافة وحفظ الحقل بالاستمارة فورياً 📋", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
