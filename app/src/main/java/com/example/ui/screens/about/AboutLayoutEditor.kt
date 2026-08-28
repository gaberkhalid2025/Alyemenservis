package com.example.ui.screens.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

/**
 * Renders the customizer UI for Admins on the About screen.
 * Allows shifting element ordering and editing custom about text.
 */
@Composable
fun AboutLayoutEditor(
    settingsState: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    viewModel: AboutViewModel,
    modifier: Modifier = Modifier
) {
    var isEditingAboutPanel by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👑 تنسيق وترتيب عناصر شاشة (عن التطبيق)",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                IconButton(
                    onClick = {
                        isEditingAboutPanel = !isEditingAboutPanel
                        viewModel.setEditingMode(isEditingAboutPanel)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(if (isEditingAboutPanel) "🔽" else "⚙️", fontSize = 14.sp)
                }
            }

            if (isEditingAboutPanel) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                Text(
                    text = "1. ترتيب ظهور العناصر (اضغط على الأسهم للتقديم أو التأخير):",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                val keyLabels = mapOf(
                    "COVER" to "🖼️ غلاف التطبيق",
                    "LOGO" to "🔴 شعار WAM",
                    "TITLE" to "🏷️ اسم التطبيق",
                    "ANNOUNCEMENT" to "📢 إعلان المنصة",
                    "ABOUT_CARD" to "ℹ️ كارت نبذة عن التطبيق",
                    "DOWNLOAD_BTN" to "📥 زر تحميل وتحديث التطبيق",
                    "CONTACTS" to "📞 أرقام وتثبيت الدعم",
                    "SOCIALS" to "🌐 شبكات التواصل الاجتماعي"
                )

                val currentList = remember(settingsState.aboutLayoutOrder) {
                    settingsState.aboutLayoutOrder
                        .split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                }

                currentList.forEachIndexed { index, k ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(keyLabels[k] ?: k, color = Color.White, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeColors.primary)
                                        .clickable {
                                            viewModel.moveItem(index, moveUp = true)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⬆️ تقديم", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < currentList.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF334155))
                                        .clickable {
                                            viewModel.moveItem(index, moveUp = false)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⬇️ تأخير", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("2. النص المخصص في كارت حول التطبيق:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                var customTextTemp by remember(settingsState.aboutCustomInfo) { mutableStateOf(settingsState.aboutCustomInfo) }
                OutlinedTextField(
                    value = customTextTemp,
                    onValueChange = { customTextTemp = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                )
                Button(
                    onClick = {
                        viewModel.updateCustomInfo(customTextTemp)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("💾 حفظ النص المخصص", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
