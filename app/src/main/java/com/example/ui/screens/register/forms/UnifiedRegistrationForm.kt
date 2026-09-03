package com.example.ui.screens.register.forms

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedRegistrationForm(
    role: String,
    themeColors: VisualThemePalette,
    onRegistrationSuccess: (Map<String, String>) -> Unit
) {
    val viewModel: SimplifiedRegistrationViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(passwordVisible) {
        if (passwordVisible) {
            kotlinx.coroutines.delay(3000L)
            passwordVisible = false
        }
    }

    LaunchedEffect(confirmPasswordVisible) {
        if (confirmPasswordVisible) {
            kotlinx.coroutines.delay(3000L)
            confirmPasswordVisible = false
        }
    }
    
    // Detailed terms dialog states
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(role) {
        viewModel.loadDraft(role)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 📸 Profile / Entity Photo Picker
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "الصورة المختارة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColors.accent.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.accent)
                            }
                        }
                    }

                    Column {
                        Text(
                            text = if (selectedImageUri != null) "تم إرفاق الصورة بنجاح ✅" else "صورة الحساب / الشعار / الهوية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedImageUri != null) Color(0xFF10B981) else Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (selectedImageUri != null) "انقر لتغيير الصورة" else "اختياري لتوثيق الحساب",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = if (selectedImageUri != null) "تغيير الصورة 🔄" else "اختيار 📷",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }
        // 1. Entity Name
        val nameLabel = when (role) {
            "CLIENT" -> "الاسم الثلاثي"
            "STORE" -> "اسم المتجر"
            "RESTAURANT" -> "اسم المطعم"
            "MEDICAL" -> "اسم المركز الطبي/العيادة"
            "PROPERTY" -> "اسم المكتب العقاري (أو المالك)"
            else -> "الاسم الكامل"
        }
        
        OutlinedTextField(
            value = state.entityName,
            onValueChange = { if (it.length <= 500) viewModel.onEvent(RegistrationEvent.EntityNameChanged(it)) },
            label = { Text(nameLabel, fontSize = 12.sp) },
            isError = state.entityNameError != null,
            supportingText = { state.entityNameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 2. Manager Name (if applicable)
        if (role in listOf("STORE", "RESTAURANT", "MEDICAL", "PROPERTY")) {
            OutlinedTextField(
                value = state.managerName,
                onValueChange = { if (it.length <= 500) viewModel.onEvent(RegistrationEvent.ManagerNameChanged(it)) },
                label = { Text("اسم المدير / المسؤول", fontSize = 12.sp) },
                isError = state.managerNameError != null,
                supportingText = { state.managerNameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // 3. Phone Number
        OutlinedTextField(
            value = state.phone,
            onValueChange = { if (it.length <= 500) viewModel.onEvent(RegistrationEvent.PhoneChanged(it)) },
            label = { Text("رقم الهاتف (9 أرقام)", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.phoneError != null,
            supportingText = { state.phoneError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 4. Password
        OutlinedTextField(
            value = state.password,
            onValueChange = { if (it.length <= 500) viewModel.onEvent(RegistrationEvent.PasswordChanged(it)) },
            label = { Text("كلمة المرور", fontSize = 12.sp) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.passwordError != null,
            supportingText = { state.passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "إخفاء" else "إظهار", fontSize = 10.sp)
                }
            }
        )

        // 5. Confirm Password
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { if (it.length <= 500) viewModel.onEvent(RegistrationEvent.ConfirmPasswordChanged(it)) },
            label = { Text("تأكيد كلمة المرور", fontSize = 12.sp) },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.confirmPasswordError != null,
            supportingText = { state.confirmPasswordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Text(if (confirmPasswordVisible) "إخفاء" else "إظهار", fontSize = 10.sp)
                }
            }
        )

        // 6. City (Dropdown simplification for demo)
        val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت")
        var expandedCity by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedCity,
            onExpandedChange = { expandedCity = !expandedCity }
        ) {
            OutlinedTextField(
                value = state.city,
                onValueChange = {},
                readOnly = true,
                label = { Text("المدينة/المحافظة", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedCity,
                onDismissRequest = { expandedCity = false }
            ) {
                cities.forEach { city ->
                    DropdownMenuItem(
                        text = { Text(city) },
                        onClick = {
                            viewModel.onEvent(RegistrationEvent.CityChanged(city))
                            expandedCity = false
                        }
                    )
                }
            }
        }

        // 7. Specialization (Only for Technician)
        if (role == "TECHNICIAN" || role == "PROVIDER") {
            val specs = listOf("سباكة", "كهرباء", "نجارة", "ألمنيوم", "إلكترونيات", "سيارات")
            var expandedSpec by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSpec,
                onExpandedChange = { expandedSpec = !expandedSpec }
            ) {
                OutlinedTextField(
                    value = state.specialization,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("التخصص الرئيسي", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedSpec,
                    onDismissRequest = { expandedSpec = false }
                ) {
                    specs.forEach { spec ->
                        DropdownMenuItem(
                            text = { Text(spec) },
                            onClick = {
                                viewModel.onEvent(RegistrationEvent.SpecializationChanged(spec))
                                expandedSpec = false
                            }
                        )
                    }
                }
            }
        }

        // 8. Terms and Policies
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.agreedToTerms,
                onCheckedChange = { viewModel.onEvent(RegistrationEvent.AgreedToTermsChanged(it)) },
                colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
            )
            Column {
                Row {
                    Text("أوافق على ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "الشروط والأحكام",
                        fontSize = 12.sp,
                        color = themeColors.accent,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                }
                Row {
                    Text("و ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "سياسة الخصوصية",
                        fontSize = 12.sp,
                        color = themeColors.accent,
                        modifier = Modifier.clickable { showPrivacyDialog = true }
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = { viewModel.submit(onRegistrationSuccess) },
            enabled = state.isFormValid && !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("تسجيل وحفظ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        if (state.successMessage != null) {
            Text(state.successMessage!!, color = Color(0xFF10B981), fontSize = 14.sp)
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("الشروط والأحكام ($role)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("هذه الشروط مفصلة ومخصصة لدورك في التطبيق. تتضمن قواعد المصداقية، الالتزام بالمواعيد، قاعدة 8 ساعات للإلغاء، الجودة، الشفافية بالأسعار، والالتزام بالقوانين.", fontSize = 12.sp) },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text("موافق") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("سياسة الخصوصية", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("نلتزم بحماية بياناتك الشخصية واستخدامها لتحسين تجربة الخدمة فقط. لا يتم مشاركتها مع أطراف خارجية، وستخضع للحذف عند الطلب بموجب سياسة الإلغاء.", fontSize = 12.sp) },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("موافق") } }
        )
    }
}
