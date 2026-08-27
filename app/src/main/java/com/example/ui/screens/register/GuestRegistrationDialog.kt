@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.RegistrationField
import com.example.ui.screens.register.components.RegistrationSubmitButton
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🔐 GuestRegistrationDialog - نافذة تسجيل الزوار واسترجاع الحسابات السابقة
 */
@Composable
fun GuestRegistrationDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onRegisterCompleted: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentName = viewModel.currentUserName.collectAsState().value
    val currentPhone = viewModel.currentUserPhone.collectAsState().value
    val currentResidence = viewModel.currentUserResidence.collectAsState().value
    val settingsState by viewModel.settings.collectAsState()

    var isRestoreMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(currentName) }
    var phonePrefix by remember { mutableStateOf("+967") }
    var phoneBody by remember {
        mutableStateOf(if (currentPhone.startsWith("+967")) currentPhone.removePrefix("+967") else currentPhone)
    }
    var residence by remember { mutableStateOf(currentResidence) }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRestoreMode) "🔓 استرجاع الحساب والبيانات" else "🔐 التحقق من الهوية وتسجيل زائر",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Red)
                    }
                }

                // Snackbar Host
                SnackbarHost(hostState = snackbarHostState)

                // Status banner when restoring/saving
                AnimatedVisibility(visible = statusMessage.isNotEmpty() || isLoading) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.accent.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = themeColors.accent,
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = if (isLoading && statusMessage.isEmpty()) "جاري معالجة طلبك..." else statusMessage,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isRestoreMode) {
                    Text(
                        text = "يرجى إدخال رقم هاتفك وكلمة المرور لاسترجاع حسابك وحجوزاتك ومحادثاتك السابقة بالكامل:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    // Phone Field
                    RegistrationField(
                        value = phoneBody,
                        onValueChange = {
                            phoneBody = it
                            phoneError = null
                        },
                        label = "رقم الهاتف المسجل (مثلاً 771234567)",
                        placeholder = "771234567",
                        leadingIcon = Icons.Default.Phone,
                        errorMessage = phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        themeColors = themeColors
                    )

                    // Password Field
                    RegistrationField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = "كلمة المرور",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        themeColors = themeColors
                    )

                    RegistrationSubmitButton(
                        text = "استرجاع الحساب الآن 🔓",
                        onClick = {
                            val cleanPhone = phoneBody.trim()
                            val cleanPassword = password.trim()

                            val phoneVal = Validators.validateYemenPhone(cleanPhone)
                            if (!phoneVal.isValid) {
                                phoneError = phoneVal.errorMessage
                                return@RegistrationSubmitButton
                            }
                            if (cleanPassword.isEmpty()) {
                                passwordError = "يرجى إدخال كلمة المرور"
                                return@RegistrationSubmitButton
                            }

                            val fullPhone = if (cleanPhone.length == 9) cleanPhone else "77$cleanPhone"
                            isLoading = true
                            statusMessage = "🔍 جاري البحث عن حسابك برقم الهاتف $fullPhone..."

                            viewModel.restoreGuestUser(context, fullPhone, cleanPassword) { success, msg ->
                                isLoading = false
                                if (success) {
                                    statusMessage = "✅ تم العثور على حسابك! جاري استرجاع البيانات..."
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🔓 تم استرجاع الحساب بنجاح!")
                                    }
                                    onDismiss()
                                } else {
                                    statusMessage = "❌ لم يتم العثور على حساب بهذا الرقم أو كلمة المرور غير صحيحة"
                                    scope.launch {
                                        snackbarHostState.showSnackbar("❌ $msg")
                                    }
                                }
                            }
                        },
                        isLoading = isLoading,
                        loadingText = "جاري البحث واسترجاع الحساب...",
                        themeColors = themeColors
                    )

                    TextButton(
                        onClick = {
                            isRestoreMode = false
                            statusMessage = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إنشاء حساب جديد؟ اضغط هنا للتسجيل", color = themeColors.accent, fontSize = 11.sp)
                    }

                } else {
                    Text(
                        text = "لتفادي الحسابات والاتصالات الوهمية، يرجى إدخال اسمك ورقم هاتفك المعتمد بجمهورية اليمن:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    // Name Field
                    RegistrationField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = "الاسم الثلاثي بالكامل (إجباري) *",
                        leadingIcon = Icons.Default.Person,
                        errorMessage = nameError,
                        themeColors = themeColors
                    )

                    // Phone Field
                    RegistrationField(
                        value = phoneBody,
                        onValueChange = {
                            phoneBody = it
                            phoneError = null
                        },
                        label = "رقم الهاتف اليمني (إجباري) *",
                        placeholder = "771234567",
                        leadingIcon = Icons.Default.Phone,
                        errorMessage = phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        themeColors = themeColors
                    )

                    // Residence Field
                    RegistrationField(
                        value = residence,
                        onValueChange = { residence = it },
                        label = "المحافظة/المنطقة داخل اليمن (إجباري) *",
                        leadingIcon = Icons.Default.Place,
                        themeColors = themeColors
                    )

                    val isPasswordRequired = settingsState.isUserPasswordRequired
                    RegistrationField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = "إنشاء كلمة مرور للحساب" + (if (isPasswordRequired) " (إجباري) *" else " (اختياري)"),
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        errorMessage = passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        themeColors = themeColors
                    )

                    RegistrationSubmitButton(
                        text = "إتمام التحقق والانطلاق 🚀",
                        onClick = {
                            val cleanName = name.trim()
                            val cleanPhone = phoneBody.trim()
                            val cleanResidence = residence.trim()
                            val cleanPassword = password.trim()

                            val nameVal = Validators.validateName(cleanName, "الاسم")
                            if (!nameVal.isValid) {
                                nameError = nameVal.errorMessage
                                return@RegistrationSubmitButton
                            }

                            val phoneVal = Validators.validateYemenPhone(cleanPhone)
                            if (!phoneVal.isValid) {
                                phoneError = phoneVal.errorMessage
                                return@RegistrationSubmitButton
                            }

                            if (isPasswordRequired && cleanPassword.isEmpty()) {
                                passwordError = "كلمة المرور إجبارية بقرار الإدارة"
                                return@RegistrationSubmitButton
                            }

                            val fullPhone = if (cleanPhone.length == 9) cleanPhone else "77$cleanPhone"
                            onRegisterCompleted(cleanName, fullPhone, cleanResidence, cleanPassword)
                        },
                        isLoading = isLoading,
                        themeColors = themeColors
                    )

                    TextButton(
                        onClick = {
                            isRestoreMode = true
                            statusMessage = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("لديك حساب بالفعل؟ استرجاع الحساب الآن 🔓", color = themeColors.accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
