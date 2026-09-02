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
                                    viewModel.setUserSessionDetails(context, provName, cleanPhone, "اليمن")
                                    match?.provider?.let { if (it.isDeleted) viewModel.restoreProvider(it.id) }
                                    match?.store?.let { if (it.isDeleted) viewModel.restoreStore(it.id) }
                                    match?.property?.let { if (it.isDeleted) viewModel.restoreProperty(it.id) }
                                    viewModel.setJoinRequestPhone(context, cleanPhone)
                                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                                    Toast.makeText(context, "🔓 تم استعادة حسابك بنجاح! مرحباً بك $provName", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "❌ كلمة المرور غير صحيحة!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد واسترجاع 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(onClick = { restoreStep = 1 }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.weight(1f)) {
                            Text("رجوع", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = restorePhoneInput.trim().replace(" ", "").replace("+967", "").replace("00967", "")
                            viewModel.setPasswordRecoveryWaitingPhone(cleanPhone)
                            Toast.makeText(context, "💬 تم إرسال طلب إعادة التعيين للإدارة بنجاح!", Toast.LENGTH_LONG).show()
                            onDismiss()
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
