package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun RestoreAccountDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var restorePhoneInput by remember { mutableStateOf("") }
    var restorePasswordInput by remember { mutableStateOf("") }
    var restoreStep by remember { mutableStateOf(1) }
    var isSearchingAccount by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<MainViewModel.RestoreAccountMatch?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🔑 استعادة حسابك التالف أو المفقود", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (restoreStep == 1) {
                    Text("الرجاء إدخال رقم الهاتف المسجل به حسابك للبحث المباشر في قاعدة البيانات:", color = Color.LightGray, fontSize = 11.sp)
                    OutlinedTextField(
                        value = restorePhoneInput,
                        onValueChange = { restorePhoneInput = it },
                        label = { Text("رقم الهاتف المسجل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val cleanPhone = restorePhoneInput.trim().replace(" ", "").replace("+967", "").replace("00967", "")
                                if (cleanPhone.length >= 7) {
                                    isSearchingAccount = true
                                    viewModel.searchAccountForRestore(cleanPhone) { match ->
                                        isSearchingAccount = false
                                        if (match != null) {
                                            matchResult = match
                                            restoreStep = 2
                                        } else {
                                            Toast.makeText(context, "❌ لا يوجد حساب مسجل بهذا الرقم!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "❌ يرجى إدخال رقم هاتف صحيح!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            enabled = !isSearchingAccount
                        ) {
                            Text(if (isSearchingAccount) "جاري البحث..." else "التالي ➡️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.weight(1f)) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                } else {
                    val match = matchResult
                    val provName = match?.name ?: "مستخدم"
                    Text("👤 تم العثور على حساب (${match?.type}) لـ: $provName", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("الرجاء إدخال كلمة المرور للتحقق واسترجاع البيانات:", color = Color.LightGray, fontSize = 10.sp)

                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        placeholder = { Text("أدخل كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val cleanPhone = restorePhoneInput.trim().replace(" ", "").replace("+967", "").replace("00967", "")
                                if (restorePasswordInput.isNotBlank()) {
                                    val savedPass = match?.savedPassword?.trim() ?: ""
                                    val isPasswordCorrect = if (savedPass.isNotEmpty()) {
                                        restorePasswordInput.trim() == savedPass ||
                                        com.example.utils.PasswordHasher.verifyPassword(restorePasswordInput.trim(), savedPass) ||
                                        com.example.utils.SecurityCryptoUtils.verifyAdminPassword(restorePasswordInput.trim(), savedPass)
                                    } else {
                                        true
                                    }

                                    if (!isPasswordCorrect) {
                                        Toast.makeText(context, "❌ كلمة المرور غير صحيحة! تأكد منها أو اضغط طلب الاستعادة.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val provArea = match?.provider?.area ?: match?.store?.cityId ?: "اليمن"
                                    viewModel.setUserSessionDetails(context, provName, cleanPhone, provArea)
                                    
                                    val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
                                    sp.edit()
                                        .putBoolean("is_account_logged_in", true)
                                        .putString("user_account_type", match?.type ?: "CLIENT")
                                        .putString("logged_account_id", match?.provider?.id ?: match?.store?.id ?: match?.property?.id ?: "")
                                        .apply()

                                    viewModel.setJoinRequestPhone(context, cleanPhone)

                                    if (match?.provider != null) {
                                        if (match.provider.isDeleted) viewModel.restoreProvider(match.provider.id)
                                        viewModel.selectedProvider = match.provider
                                        viewModel.selectedStore = null
                                        viewModel.selectedProperty = null
                                        viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.DYNAMIC_PROFILE)
                                    } else if (match?.store != null) {
                                        if (match.store.isDeleted) viewModel.restoreStore(match.store.id)
                                        viewModel.selectedStore = match.store
                                        viewModel.selectedProvider = null
                                        viewModel.selectedProperty = null
                                        viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.DYNAMIC_PROFILE)
                                    } else if (match?.property != null) {
                                        if (match.property.isDeleted) viewModel.restoreProperty(match.property.id)
                                        viewModel.selectedProperty = match.property
                                        viewModel.selectedProvider = null
                                        viewModel.selectedStore = null
                                        viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.DYNAMIC_PROFILE)
                                    } else {
                                        viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.USER_BROWSE)
                                    }

                                    Toast.makeText(context, "🔓 تم تسجيل الدخول بنجاح! مرحباً بك $provName", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "❌ يرجى إدخال كلمة المرور!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد ودخول 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(onClick = { restoreStep = 1 }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.weight(1f)) {
                            Text("رجوع", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = restorePhoneInput.trim().replace(" ", "").replace("+967", "").replace("00967", "")
                            viewModel.requestPasswordReset(context, cleanPhone, provName, match?.type ?: "USER") { success ->
                                if (success) {
                                    Toast.makeText(context, "⏳ تم إرسال طلب استعادة كلمة المرور للإدارة بنجاح!", Toast.LENGTH_LONG).show()
                                    viewModel.navigateToScreen(com.example.ui.navigation.AppScreens.PASSWORD_RESET_WAITING)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "❌ حدث خطأ، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("💬 نسيت كلمة المرور؟ طلب الاستعادة من الأدمن", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
