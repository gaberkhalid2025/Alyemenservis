package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🔐 BackdoorLoginDialog - نافذة تسجيل الدخول للبوابة الخلفية للتحكم الشامل بالدليل
 * تفتح بالضغط على زر الهوم 3 مرات متتالية
 */
@Composable
fun BackdoorLoginDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val supervisors by viewModel.supervisors.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var isAuthenticating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, themeColors.accent.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = themeColors.accent.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔐", fontSize = 26.sp)
                    }
                }

                Text(
                    text = "تسجيل دخول البوابة الخلفية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "منطقة صلاحيات المالك والإدارة العليا لمنصة دليل خدمات اليمن",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Email field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("البريد الإلكتروني للإدارة", fontSize = 12.sp) },
                    placeholder = { Text("mah73646@gmail.com", fontSize = 12.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = themeColors.accent)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                // Password field
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("كلمة المرور السرية", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.accent)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Text(
                                text = if (isPasswordVisible) "👁️" else "🔒",
                                fontSize = 16.sp
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedLabelColor = themeColors.accent,
                        unfocusedLabelColor = Color.LightGray
                    )
                )

                // Remember me option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberMe = !rememberMe },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = themeColors.accent,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تذكرني وحفظ تسجيل الدخول 🔐",
                        color = Color.White,
                        fontSize = 11.5.sp
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val trimmedUser = emailInput.trim()
                            val trimmedPass = passwordInput.trim()

                            if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
                                viewModel.triggerNotification("❌ يرجى إدخال البريد الإلكتروني وكلمة المرور!")
                                return@Button
                            }

                            isAuthenticating = true
                            try {
                                // Owner check
                                val isOwner = (trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                                        trimmedUser.equals(settingsState.ownerEmail, ignoreCase = true) ||
                                        trimmedUser == "WAM2026") &&
                                        (trimmedPass == "Maher@@--@@736462##" ||
                                                trimmedPass == settingsState.ownerPassword ||
                                                com.example.utils.PasswordHasher.verifyPassword(trimmedPass, settingsState.ownerPassword) ||
                                                com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settingsState.ownerPassword))

                                // Admin check
                                val isAdmin = (trimmedUser.equals("mah73646@gmail.com", ignoreCase = true) ||
                                        trimmedUser.equals("meh777644@gmail.com", ignoreCase = true) ||
                                        trimmedUser.equals(settingsState.adminUsername, ignoreCase = true)) &&
                                        (trimmedPass == "Maher@@--@@736462##" ||
                                                trimmedPass == settingsState.adminPassword ||
                                                com.example.utils.PasswordHasher.verifyPassword(trimmedPass, settingsState.adminPassword) ||
                                                com.example.utils.SecurityCryptoUtils.verifyAdminPassword(trimmedPass, settingsState.adminPassword))

                                if (isOwner) {
                                    onDismiss()
                                    viewModel.authenticateAdmin(context, "OWNER", rememberMe)
                                    viewModel.triggerNotification("🔓 مرحباً بك في البوابة الخلفية بصلاحية المالك!")
                                } else if (isAdmin) {
                                    onDismiss()
                                    viewModel.authenticateAdmin(context, "ADMIN", rememberMe)
                                    viewModel.triggerNotification("🔓 مرحباً بك بصلاحية مدير النظام!")
                                } else {
                                    // Supervisor check
                                    val matchingSup = supervisors.find {
                                        (it.name.trim().equals(trimmedUser, ignoreCase = true) || it.id.equals(trimmedUser, ignoreCase = true)) &&
                                                (it.passcode.isNotBlank() && (it.passcode.trim() == trimmedPass || com.example.utils.PasswordHasher.verifyPassword(trimmedPass, it.passcode)))
                                    }
                                    if (matchingSup != null) {
                                        viewModel.setSupervisorSession(matchingSup)
                                        if (rememberMe) {
                                            val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                                            sp.edit().putString("saved_admin_role", "SUPERVISOR").apply()
                                        }
                                        onDismiss()
                                        viewModel.authenticateAdmin(context, "SUPERVISOR", rememberMe)
                                        viewModel.triggerNotification("🔓 مرحباً بك المشرف: ${matchingSup.name}")
                                    } else {
                                        viewModel.triggerNotification("❌ البريد الإلكتروني أو كلمة المرور غير صحيحة!")
                                    }
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                viewModel.triggerNotification("❌ حدث خطأ أثناء التحقق: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
                            } finally {
                                isAuthenticating = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.weight(1.5f),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        } else {
                            Text("دخول للنظام 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
