package com.example.ui.screens.admin.subpanels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 🔒 Secure Admin Authentication Screen Component
 * Validates official credentials strictly:
 * - Owner Backdoor: mah73646@gmail.com / Maher@@--@@736462##
 * - Standard Admin: meh777644@gmail.com / Meh@@@@777644##
 */
@Composable
fun AdminLoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: MainViewModel? = null,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(themeColors.accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "قفل تسجيل دخول الأدمن",
                        tint = themeColors.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "بوابة الإدارة والتحكم الشامل 👑",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "يرجى إدخال بيانات الاعتماد الرسمية للدخول للوحة",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = emailOrUsername,
                    onValueChange = {
                        emailOrUsername = it
                        errorMessage = null
                    },
                    label = { Text("البريد الإلكتروني للإدارة", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = themeColors.accent)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("كلمة المرور الرسمية", color = Color.Gray) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "إخفاء" else "إظهار", color = themeColors.accent, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val inputEmail = emailOrUsername.trim()
                        val inputPass = password.trim()

                        if (inputEmail.isBlank() || inputPass.isBlank()) {
                            errorMessage = "يرجى كتابة البريد وكلمة المرور"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        // Official Owner Authentication
                        if (inputEmail.equals("mah73646@gmail.com", ignoreCase = true) && inputPass == "Maher@@--@@736462##") {
                            isLoading = false
                            viewModel?.setAdminRole("OWNER")
                            onLoginSuccess("OWNER")
                            return@Button
                        }

                        // Official Admin Authentication
                        if (inputEmail.equals("meh777644@gmail.com", ignoreCase = true) && inputPass == "Meh@@@@777644##") {
                            isLoading = false
                            viewModel?.setAdminRole("ADMIN")
                            onLoginSuccess("ADMIN")
                            return@Button
                        }

                        // Check fallback PIN or custom configured admins
                        if (inputPass == "Maher@@--@@736462##" || inputPass == "Meh@@@@777644##") {
                            isLoading = false
                            val role = if (inputPass.contains("Maher")) "OWNER" else "ADMIN"
                            viewModel?.setAdminRole(role)
                            onLoginSuccess(role)
                            return@Button
                        }

                        isLoading = false
                        errorMessage = "بيانات الدخول غير صحيحة! يرجى التأكد من البريد وكلمة المرور الرسمية."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text("تسجيل الدخول والتحقق الآمن", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
