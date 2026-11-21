package com.example.ui.dialogs

import android.widget.Toast
import com.example.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
    var restorePhoneInput by rememberSaveable { mutableStateOf("") }
    var restorePasswordInput by rememberSaveable { mutableStateOf("") }
    var restoreStep by rememberSaveable { mutableStateOf(1) }
    var isSearchingAccount by remember { mutableStateOf(false) }
    var isVerifyingPassword by remember { mutableStateOf(false) }
    var isRequestingReset by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<MainViewModel.RestoreAccountMatch?>(null) }
    var foundAccounts by remember { mutableStateOf<List<MainViewModel.RestoreAccountMatch>>(emptyList()) }
    var showSuccessState by rememberSaveable { mutableStateOf(false) }
    var successUserName by rememberSaveable { mutableStateOf("") }
    var targetSuccessScreen by rememberSaveable { mutableStateOf(AppScreens.USER_BROWSE) }

    val sp = remember(context) {
        context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
    }
    val secureSp = remember(context) {
        runCatching {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                "yemen_service_secure_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    // في حال تدوير الشاشة أثناء الخطوة 2 وفقدان matchResult غير القابل للحفظ في Bundle، نعيد جلبه تلقائياً
    LaunchedEffect(restoreStep) {
        if (restoreStep == 2 && matchResult == null) {
            val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput).filter { it.isDigit() }
            if (cleanPhone.length >= 7) {
                isSearchingAccount = true
                viewModel.searchAccountForRestore(cleanPhone) { match ->
                    isSearchingAccount = false
                    if (match != null) {
                        matchResult = match
                    } else {
                        restoreStep = 1
                    }
                }
            } else {
                restoreStep = 1
            }
        }
    }

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
                                val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput).filter { it.isDigit() }
                                if (cleanPhone.length >= 7) {
                                    isSearchingAccount = true
                                    viewModel.searchAllAccountsForRestore(cleanPhone) { matches ->
                                        isSearchingAccount = false
                                        if (matches.isEmpty()) {
                                            Toast.makeText(context, "❌ لا يوجد حساب مسجل بهذا الرقم!", Toast.LENGTH_LONG).show()
                                        } else if (matches.size == 1) {
                                            matchResult = matches.first()
                                            restoreStep = 2
                                        } else {
                                            foundAccounts = matches
                                            restoreStep = 3
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
                } else if (restoreStep == 3) {
                    Text(
                        "📌 تم العثور على (${foundAccounts.size}) حسابات مسجلة بهذا الرقم. اختر الحساب المطلوب:",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        foundAccounts.forEach { acc ->
                            val (icon, typeLabel) = when (acc.type.uppercase()) {
                                "PROVIDER", "TECHNICIAN" -> "🔧" to "حساب فني / مهني"
                                "STORE" -> "🏪" to "حساب متجر / محل"
                                "RESTAURANT" -> "🍔" to "حساب مطعم / كافيه"
                                "MEDICAL" -> "🏥" to "حساب مركز طبي / عيادة"
                                "PROPERTY" -> "🏠" to "حساب مكتب عقاري"
                                "JOB" -> "💼" to "حساب معلن وظائف"
                                else -> "👤" to "حساب مواطن / عميل"
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = themeColors.accent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        matchResult = acc
                                        restoreStep = 2
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(icon, fontSize = 22.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(typeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Text(acc.name, fontSize = 11.sp, color = Color.White)
                                    }
                                    Text("اختيار ➡️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { restoreStep = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("رجوع ⬅️", color = Color.White, fontSize = 11.sp)
                    }
                } else {
                    val match = matchResult
                    val provName = match?.name?.takeIf { it.isNotBlank() } ?: "مستخدم"
                    Text("👤 تم العثور على حساب (${match?.type ?: "CLIENT"}) لـ: $provName", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                val currentMatch = matchResult
                                val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput).filter { it.isDigit() }
                                val cleanPassword = restorePasswordInput.trim()
                                if (currentMatch == null || cleanPhone.length < 7) {
                                    Toast.makeText(context, "❌ بيانات الحساب غير صالحة، يرجى إعادة البحث برقم الهاتف.", Toast.LENGTH_LONG).show()
                                    restoreStep = 1
                                    return@Button
                                }
                                if (cleanPassword.isBlank()) {
                                    Toast.makeText(context, "❌ يرجى إدخال كلمة المرور!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                isVerifyingPassword = true
                                viewModel.verifyRestorePassword(
                                    cleanPhone = cleanPhone,
                                    accountType = currentMatch.type.ifBlank { "CLIENT" },
                                    passwordInput = cleanPassword
                                ) { isPasswordCorrect ->
                                    isVerifyingPassword = false
                                    if (!isPasswordCorrect) {
                                        Toast.makeText(context, "❌ كلمة المرور غير صحيحة! تأكد منها أو اضغط طلب الاستعادة.", Toast.LENGTH_LONG).show()
                                    } else {
                                        val provArea = currentMatch.provider?.area?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.store?.cityId?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.property?.cityId?.takeIf { it.isNotBlank() }
                                            ?: "اليمن"
                                        val unmaskedName = currentMatch.provider?.name?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.store?.name?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.property?.title?.takeIf { it.isNotBlank() }
                                            ?: provName
                                        viewModel.setUserSessionDetails(context, unmaskedName, cleanPhone, provArea)

                                        val resolvedAccountType = currentMatch.type.ifBlank { "CLIENT" }
                                        val resolvedAccountId = currentMatch.provider?.id
                                            ?: currentMatch.store?.id
                                            ?: currentMatch.property?.id
                                            ?: ""

                                        sp.edit()
                                            .putBoolean("is_account_logged_in", true)
                                            .putString("user_account_type", resolvedAccountType)
                                            .putString("logged_account_id", resolvedAccountId)
                                            .apply()

                                        secureSp?.edit()
                                            ?.putBoolean("is_account_logged_in", true)
                                            ?.putString("user_account_type", resolvedAccountType)
                                            ?.putString("logged_account_id", resolvedAccountId)
                                            ?.apply()

                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        if (viewModel.adminRole.value !in listOf("OWNER", "ADMIN", "SUPERVISOR")) {
                                            viewModel.authViewModel._adminRole.value = when (resolvedAccountType.uppercase()) {
                                                "PROVIDER", "TECHNICIAN" -> "PROVIDER"
                                                "STORE", "RESTAURANT", "MEDICAL" -> "STORE_OWNER"
                                                "PROPERTY" -> "PROPERTY_OWNER"
                                                "JOB", "JOB_POSTER" -> "JOB_POSTER"
                                                "CLIENT" -> "CLIENT"
                                                else -> "GUEST"
                                            }
                                        }

                                        if (currentMatch.provider != null) {
                                            if (currentMatch.provider.isDeleted) viewModel.restoreProvider(currentMatch.provider.id)
                                            viewModel.selectedProvider = currentMatch.provider
                                            viewModel.selectedStore = null
                                            viewModel.selectedProperty = null
                                            viewModel.selectedJob = null
                                            targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                        } else if (currentMatch.store != null) {
                                            if (currentMatch.store.isDeleted) viewModel.restoreStore(currentMatch.store.id)
                                            viewModel.selectedStore = currentMatch.store
                                            viewModel.selectedProvider = null
                                            viewModel.selectedProperty = null
                                            viewModel.selectedJob = null
                                            targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                        } else if (currentMatch.property != null) {
                                            if (currentMatch.property.isDeleted) viewModel.restoreProperty(currentMatch.property.id)
                                            viewModel.selectedProperty = currentMatch.property
                                            viewModel.selectedProvider = null
                                            viewModel.selectedStore = null
                                            viewModel.selectedJob = null
                                            targetSuccessScreen = AppScreens.DYNAMIC_PROFILE
                                        } else {
                                            viewModel.selectedProvider = null
                                            viewModel.selectedStore = null
                                            viewModel.selectedProperty = null
                                            viewModel.selectedJob = null
                                            targetSuccessScreen = AppScreens.USER_BROWSE
                                        }

                                        restorePasswordInput = ""
                                        successUserName = currentMatch.provider?.name?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.store?.name?.takeIf { it.isNotBlank() }
                                            ?: currentMatch.property?.title?.takeIf { it.isNotBlank() }
                                            ?: provName
                                        showSuccessState = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f),
                            enabled = !isVerifyingPassword && !isRequestingReset
                        ) {
                            Text(if (isVerifyingPassword) "جاري التحقق..." else "تأكيد ودخول 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                restorePasswordInput = ""
                                matchResult = null
                                restoreStep = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f),
                            enabled = !isVerifyingPassword && !isRequestingReset
                        ) {
                            Text("رجوع", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = com.example.domain.usecases.ValidatePhoneUseCase.normalizePhone(restorePhoneInput).filter { it.isDigit() }
                            if (cleanPhone.length < 7) {
                                Toast.makeText(context, "❌ رقم الهاتف غير صالح لإرسال طلب الاستعادة!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isRequestingReset = true
                            val reqRawName = match?.provider?.name?.takeIf { it.isNotBlank() }
                                ?: match?.store?.name?.takeIf { it.isNotBlank() }
                                ?: match?.property?.title?.takeIf { it.isNotBlank() }
                                ?: provName
                            viewModel.requestPasswordReset(context, cleanPhone, reqRawName, match?.type ?: "USER") { success ->
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
