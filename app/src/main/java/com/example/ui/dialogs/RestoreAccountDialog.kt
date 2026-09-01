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
import com.example.data.*
import com.example.ui.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RestoreAccountDialog(
    viewModel: MainViewModel,
    themeColors: com.example.utils.VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var restorePhoneInput by remember { mutableStateOf("") }
    var restorePasswordInput by remember { mutableStateOf("") }
    var restoreStep by remember { mutableStateOf(1) }
    var isSearchingAccount by remember { mutableStateOf(false) }

    var matchedProvider by remember { mutableStateOf<ProviderEntity?>(null) }
    var matchedPending by remember { mutableStateOf<PendingProviderEntity?>(null) }
    var matchedStore by remember { mutableStateOf<StoreEntity?>(null) }
    var matchedProperty by remember { mutableStateOf<PropertyEntity?>(null) }
    var matchedUserDoc by remember { mutableStateOf<Map<String, Any>?>(null) }

    var failedRecoveryAttempts by remember { mutableStateOf(0) }
    var recoveryLockoutUntil by remember { mutableStateOf(0L) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔑 استعادة حسابك التالف أو المفقود",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (restoreStep == 1) {
                    Text(
                        text = "الرجاء إدخال رقم الهاتف المسجل به حسابك للبحث المباشر في قاعدة البيانات:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = restorePhoneInput,
                        onValueChange = { restorePhoneInput = it },
                        label = { Text("رقم الهاتف المسجل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val raw = restorePhoneInput.trim().replace(" ", "")
                                val cleanPhone = when {
                                    raw.startsWith("+967") -> raw.substring(4)
                                    raw.startsWith("00967") -> raw.substring(5)
                                    raw.startsWith("0") -> raw.substring(1)
                                    else -> raw
                                }
                                if (cleanPhone.length >= 7) {
                                    isSearchingAccount = true
                                    val foundProv = viewModel.providers.value.firstOrNull { it.phone.endsWith(cleanPhone) }
                                    val foundStor = viewModel.stores.value.firstOrNull { it.phone.endsWith(cleanPhone) }
                                    val foundProp = viewModel.properties.value.firstOrNull { it.phone.endsWith(cleanPhone) }

                                    if (foundProv != null) {
                                        matchedProvider = foundProv
                                        isSearchingAccount = false
                                        restoreStep = 2
                                    } else if (foundStor != null) {
                                        matchedStore = foundStor
                                        isSearchingAccount = false
                                        restoreStep = 2
                                    } else if (foundProp != null) {
                                        matchedProperty = foundProp
                                        isSearchingAccount = false
                                        restoreStep = 2
                                    } else {
                                        FirebaseFirestore.getInstance().collection("users")
                                            .whereEqualTo("phone", cleanPhone)
                                            .get()
                                            .addOnSuccessListener { qs ->
                                                isSearchingAccount = false
                                                if (qs != null && !qs.isEmpty) {
                                                    matchedUserDoc = qs.documents.first().data
                                                    restoreStep = 2
                                                } else {
                                                    Toast.makeText(context, "❌ لا يوجد حساب مسجل بهذا الرقم!", Toast.LENGTH_LONG).show()
                                                }
                                            }.addOnFailureListener {
                                                isSearchingAccount = false
                                                Toast.makeText(context, "❌ تعذر العثور على حساب مسجل بهذا الرقم!", Toast.LENGTH_SHORT).show()
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
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White, fontSize = 11.sp)
                        }
                    }
                } else {
                    val provName = matchedProvider?.name ?: matchedPending?.name ?: matchedStore?.name ?: matchedProperty?.title ?: matchedUserDoc?.get("name")?.toString() ?: "مستخدم"
                    val accountType = when {
                        matchedProvider != null -> "فني معتمد"
                        matchedPending != null -> "طلب فني معلق"
                        matchedStore != null -> "محل / مركز / مطعم"
                        matchedProperty != null -> "عقار / بيت"
                        matchedUserDoc != null -> "حساب عميل"
                        else -> "حساب مسجل"
                    }
                    Text("👤 تم العثور على حساب ($accountType) لـ: $provName", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("الرجاء إدخال كلمة المرور للتحقق واسترجاع البيانات:", color = Color.LightGray, fontSize = 10.sp)

                    var passVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        placeholder = { Text("أدخل كلمة المرور") },
                        visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Text(if (passVisible) "👁️" else "🙈", fontSize = 16.sp)
                            }
                        }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime < recoveryLockoutUntil) {
                                    val remSec = (recoveryLockoutUntil - currentTime) / 1000
                                    Toast.makeText(context, "⚠️ الحساب مقفل مؤقتاً (${remSec / 60} دقيقة).", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                val raw = restorePhoneInput.trim().replace(" ", "")
                                val cleanPhone = when {
                                    raw.startsWith("+967") -> raw.substring(4)
                                    raw.startsWith("00967") -> raw.substring(5)
                                    raw.startsWith("0") -> raw.substring(1)
                                    else -> raw
                                }
                                val isPassValid = restorePasswordInput.isNotBlank()

                                if (isPassValid) {
                                    viewModel.setUserSessionDetails(context, provName, cleanPhone, "اليمن")
                                    val prov = matchedProvider
                                    val pend = matchedPending
                                    val stor = matchedStore
                                    val prop = matchedProperty
                                    if (prov != null) {
                                        if (prov.isDeleted) viewModel.restoreProvider(prov.id)
                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        viewModel.navigateTo("REGISTER_FORM")
                                    } else if (pend != null) {
                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        viewModel.navigateTo("JOIN_REQUEST_STATUS")
                                    } else if (stor != null) {
                                        if (stor.isDeleted) viewModel.restoreStore(stor.id)
                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        viewModel.navigateTo("REGISTER_FORM")
                                    } else if (prop != null) {
                                        if (prop.isDeleted) viewModel.restoreProperty(prop.id)
                                        viewModel.setJoinRequestPhone(context, cleanPhone)
                                        viewModel.navigateTo("REGISTER_FORM")
                                    } else {
                                        viewModel.navigateTo("REGISTER_FORM")
                                    }
                                    Toast.makeText(context, "🔓 تم استعادة حسابك بنجاح! مرحباً بك $provName", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    failedRecoveryAttempts++
                                    if (failedRecoveryAttempts >= 3) {
                                        recoveryLockoutUntil = System.currentTimeMillis() + 5 * 60 * 1000L
                                        Toast.makeText(context, "🚫 تم تجاوز المحاولات (3). قفل لم 5 دقائق.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "❌ كلمة المرور غير صحيحة!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد واسترجاع 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { restoreStep = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val raw = restorePhoneInput.trim().replace(" ", "")
                            val cleanPhone = when {
                                raw.startsWith("+967") -> raw.substring(4)
                                raw.startsWith("00967") -> raw.substring(5)
                                raw.startsWith("0") -> raw.substring(1)
                                else -> raw
                            }
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
