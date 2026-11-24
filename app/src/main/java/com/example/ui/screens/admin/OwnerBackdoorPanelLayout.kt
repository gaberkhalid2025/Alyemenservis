@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.ErrorLogsViewerDialog
import com.example.utils.VisualThemePalette

@Composable
/* OwnerBackdoorPanelLayout coordinator delegating to specialized sub-sections */
fun OwnerBackdoorPanelLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()
    val state = rememberOwnerBackdoorState(settingsState)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔓 بوابة المالك والتحكم الخلفي الديناميكي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Button(
                onClick = { viewModel.logout(state.context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.testTag("owner_close_panel_btn")
            ) {
                Text("إغلاق اللوحة", color = Color.White, fontSize = 10.sp)
            }
        }

        // 🛡️ زر استعراض سجل تشخيص أخطاء Firestore و API للمالك
        Button(
            onClick = { state.showLogsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_logs_btn")
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🔍 سجل تشخيص أخطاء السحابة والـ API (Logs)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OwnerBackdoorCoreAndFooterSection(state = state, themeColors = themeColors)

        OwnerBackdoorLayoutAndAboutSection(state = state, themeColors = themeColors)

        OwnerBackdoorBannerAndLogoSection(state = state, themeColors = themeColors)

        Button(
            onClick = { state.saveAllSettings(viewModel) },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_save_settings_btn")
        ) {
            Text("💾 حفظ إعدادات البوابة والتخزين والتطبيق", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.showWipeConfirmDialog) {
            OwnerBackdoorWipeMockDialog(state = state, viewModel = viewModel)
        }

        if (com.example.BuildConfig.DEBUG) {
            Button(
                onClick = { state.showRegistrationTestScreen = true },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_registration_test_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل اختبار التسجيل",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🧪 تشغيل اختبار التسجيل الشامل للأقسام 8", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { state.showAssistantTestScreen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_assistant_test_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "اختبار المساعد الذكي",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🤖 اختبار المساعد الذكي", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { state.showWipeConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_wipe_mock_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "حذف البيانات الوهمية",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("🧹 حذف الفنيين والرسائل والإشعارات الوهمية", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (state.showLogsDialog) {
            ErrorLogsViewerDialog(
                themeColors = themeColors,
                onDismiss = { state.showLogsDialog = false }
            )
        }

        if (state.showRegistrationTestScreen) {
            RegistrationTestScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onDismiss = { state.showRegistrationTestScreen = false }
            )
        }

        if (state.showAssistantTestScreen) {
            AssistantTestScreen(
                viewModel = viewModel,
                themeColors = themeColors,
                onDismiss = { state.showAssistantTestScreen = false }
            )
        }
    }
}

@Composable
private fun OwnerBackdoorWipeMockDialog(
    state: OwnerBackdoorState,
    viewModel: MainViewModel
) {
    AlertDialog(
        onDismissRequest = {
            state.showWipeConfirmDialog = false
            state.wipePasswordInput = ""
            state.wipeDoubleConfirmed = false
            state.wipeErrorMsg = ""
        },
        title = { Text("🔒 إدخال كلمة المرور لتأكيد الحذف والمسح", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠️ تنبيه أمني: يتطلب حذف أو تطهير البيانات إدخال كلمة مرور الأدمن وتأكيد الإجراء:", fontSize = 11.sp, color = Color.White)
                OutlinedTextField(
                    value = state.wipePasswordInput,
                    onValueChange = {
                        state.wipePasswordInput = it
                        state.wipeErrorMsg = ""
                    },
                    label = { Text("كلمة مرور الأدمن") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_wipe_password_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.wipeDoubleConfirmed,
                        onCheckedChange = {
                            state.wipeDoubleConfirmed = it
                            state.wipeErrorMsg = ""
                        },
                        modifier = Modifier.testTag("owner_wipe_confirm_checkbox")
                    )
                    Text("أؤكد مسؤوليتي الكاملة عن حذف البيانات التجريبية", fontSize = 10.5.sp, color = Color.LightGray)
                }
                if (state.wipeErrorMsg.isNotEmpty()) {
                    Text(state.wipeErrorMsg, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!state.wipeDoubleConfirmed) {
                        state.wipeErrorMsg = "⚠️ يرجى تأكيد خانة الموافقة أولاً."
                    } else if (viewModel.verifyAdminOrOwnerPassword(state.wipePasswordInput)) {
                        viewModel.wipeAllMockAndTemporaryData()
                        state.showWipeConfirmDialog = false
                        state.wipePasswordInput = ""
                        state.wipeDoubleConfirmed = false
                        state.wipeErrorMsg = ""
                    } else {
                        state.wipeErrorMsg = "❌ كلمة المرور غير صحيحة! تم منع التطهير والحذف."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.testTag("owner_wipe_confirm_submit_btn")
            ) {
                Text("تأكيد الحذف النهائي 🗑️", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                state.showWipeConfirmDialog = false
                state.wipePasswordInput = ""
                state.wipeDoubleConfirmed = false
                state.wipeErrorMsg = ""
            }) {
                Text("إلغاء", color = Color.LightGray)
            }
        }
    )
}
