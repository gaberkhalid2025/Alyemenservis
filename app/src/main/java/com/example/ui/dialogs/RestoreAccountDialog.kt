package com.example.ui.dialogs

import android.widget.Toast
import com.example.ui.*
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
    var isVerifyingPassword by remember { mutableStateOf(false) }
    var isRequestingReset by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<MainViewModel.RestoreAccountMatch?>(null) }
    var showSuccessState by remember { mutableStateOf(false) }
    var successUserName by remember { mutableStateOf("") }
    var targetSuccessScreen by remember { mutableStateOf(AppScreens.USER_BROWSE) }

    // ⏳ حماية زمنية (30 ثانية) لمنع تعليق أزرار التحميل والإرسال (القاعدة 7)
    LaunchedEffect(isSearchingAccount) {
        if (isSearchingAccount) {
            kotlinx.coroutines.delay(30_000L)
            isSearchingAccount = false
        }
    }
    LaunchedEffect(isVerifyingPassword) {
        if (isVerifyingPassword) {
            kotlinx.coroutines.delay(30_000L)
            isVerifyingPassword = false
        }
    }
    LaunchedEffect(isRequestingReset) {
        if (isRequestingReset) {
            kotlinx.coroutines.delay(30_000L)
            isRequestingReset = false
        }
    }

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

                if (showSuccessState) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text("🎉 تم استعادة الحساب بنجاح!", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("أهلاً وسهلاً بك مجدداً: $successUserName", color = Color.White, fontSize = 14.sp)
                        Text("تم توثيق جهازك وتسجيل الدخول بنجاح إلى حسابك.", color = Color.LightGray, fontSize = 12.sp)

                        Button(
                            onClick = {
                                viewModel.navigateToScreen(targetSuccessScreen)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("موافق (الانتقال للحساب) 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else if (restoreStep == 1) {
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
                                val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput)
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
                                val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput)
                                if (restorePasswordInput.isNotBlank()) {
                                    isVerifyingPassword = true
                                    viewModel.verifyRestorePassword(cleanPhone, match?.type ?: "CLIENT", restorePasswordInput.trim()) { isPasswordCorrect ->
                                        isVerifyingPassword = false
                                        if (!isPasswordCorrect) {
                                            Toast.makeText(context, "❌ كلمة المرور غير صحيحة! تأكد منها أو اضغط طلب الاستعادة.", Toast.LENGTH_LONG).show()
                                        } else {
                                            val provArea = match?.provider?.area ?: match?.store?.cityId ?: match?.property?.cityId ?: "اليمن"
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
                                                targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                            } else if (match?.store != null) {
                                                if (match.store.isDeleted) viewModel.restoreStore(match.store.id)
                                                viewModel.selectedStore = match.store
                                                viewModel.selectedProvider = null
                                                viewModel.selectedProperty = null
                                                targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                            } else if (match?.property != null) {
                                                if (match.property.isDeleted) viewModel.restoreProperty(match.property.id)
                                                viewModel.selectedProperty = match.property
                                                viewModel.selectedProvider = null
                                                viewModel.selectedStore = null
                                                targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                            } else {
                                                viewModel.selectedProvider = null
                                                viewModel.selectedStore = null
                                                viewModel.selectedProperty = null
                                                viewModel.selectedJob = null
                                                targetSuccessScreen = AppScreens.USER_BROWSE
                                            }

                                            successUserName = provName
                                            showSuccessState = true
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "❌ يرجى إدخال كلمة المرور!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            enabled = !isVerifyingPassword && !isRequestingReset
                        ) {
                            Text(if (isVerifyingPassword) "جاري التحقق..." else "تأكيد ودخول 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { restoreStep = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f),
                            enabled = !isVerifyingPassword && !isRequestingReset
                        ) {
                            Text("رجوع", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput)
                            isRequestingReset = true
                            viewModel.requestPasswordReset(context, cleanPhone, provName, match?.type ?: "USER") { success ->
                                isRequestingReset = false
                                if (success) {
                                    Toast.makeText(context, "⏳ تم إرسال طلب استعادة كلمة المرور للإدارة بنجاح!", Toast.LENGTH_LONG).show()
                                    viewModel.navigateToScreen(AppScreens.PASSWORD_RESET_WAITING)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "❌ حدث خطأ، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.secondary),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        enabled = !isRequestingReset && !isVerifyingPassword
                    ) {
                        Text(
                            if (isRequestingReset) "جاري إرسال الطلب..." else "💬 نسيت كلمة المرور؟ طلب الاستعادة من الأدمن",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
