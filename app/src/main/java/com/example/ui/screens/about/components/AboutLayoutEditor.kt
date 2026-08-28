package com.example.ui.screens.about.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.about.AboutUiState
import com.example.ui.screens.about.AboutViewModel
import com.example.utils.VisualThemePalette

/**
 * AboutLayoutEditor provides administrative controls to customize the layout order of
 * elements on the About screen and modify the custom info text.
 *
 * @param viewModel The AboutViewModel managing the screen state.
 * @param themeColors The palette theme colors.
 */
@Composable
fun AboutLayoutEditor(
    viewModel: AboutViewModel,
    themeColors: VisualThemePalette
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val settings = when (val state = uiState) {
        is AboutUiState.Success -> state.settings
        is AboutUiState.Editing -> state.settings
        else -> null
    } ?: return

    val isEditing = uiState is AboutUiState.Editing
    val customText = (uiState as? AboutUiState.Editing)?.tempCustomText ?: settings.aboutCustomInfo

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    onClick = { viewModel.toggleEditing() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(if (isEditing) "🔽" else "⚙️", fontSize = 14.sp)
                }
            }

            AnimatedVisibility(
                visible = isEditing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.15f))

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

                    val currentList = settings.aboutLayoutOrder
                        .split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }

                    currentList.forEachIndexed { index, key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(keyLabels[key] ?: key, color = Color.White, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(themeColors.primary)
                                            .clickable { viewModel.moveItemUp(index) }
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
                                            .clickable { viewModel.moveItemDown(index) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("⬇️ تأخير", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. النص المخصص في كارت حول التطبيق:",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customText,
                        onValueChange = { viewModel.updateCustomText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.saveCustomTextChanges()
                            Toast.makeText(context, "💾 تم حفظ النص المخصص بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text("💾 حفظ النص المخصص", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
