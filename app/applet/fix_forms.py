import os

print("Updating fix_forms.py to add import com.example.ui.*...")

forms_dir = "app/src/main/java/com/example/ui/screens/register/forms"
os.makedirs(forms_dir, exist_ok=True)

common_imports = """package com.example.ui.screens.register.forms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.ui.components.FlexibleCatalogUploader
import com.example.utils.*
import java.util.UUID
import kotlinx.coroutines.launch
"""

# 1. TechnicianRegistrationForm.kt
tech_code = common_imports + """
@Composable
fun TechnicianRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var customProfession by remember { mutableStateOf("") }
    var selfiePhotoBase64 by remember { mutableStateOf("") }
    var idPhotoBase64 by remember { mutableStateOf("") }
    var workPhotosList by remember { mutableStateOf(listOf<String>()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🛠️ استمارة تسجيل فني / مقدم خدمة", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الثلاثي") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف (اليمن)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("المنطقة / المديرية") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = neighborhood,
                onValueChange = { neighborhood = it },
                label = { Text("الحي / الشارع") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Button(
                onClick = {
                    val raw = phone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (name.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال الاسم ورقم هاتف صحيح", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    viewModel.submitJoinForm(context, name, cleanPhone, selectedCatId, area, neighborhood, selfiePhotoBase64, idPhotoBase64, "", workPhotosList, customProfession)
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب انضمام فني 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/TechnicianRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(tech_code)

# 2. StoreRegistrationForm.kt
store_code = common_imports + """
@Composable
fun StoreRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var storeOwnerName by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var storePhone by remember { mutableStateOf("") }
    var storeDesc by remember { mutableStateOf("") }
    var storeCity by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var storePassword by remember { mutableStateOf("") }
    var storePhotosList by remember { mutableStateOf(listOf<String>()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏪 استمارة تسجيل محل / معرض تجاري", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("اسم المتجر / المعرض") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = storePhone,
                onValueChange = { storePhone = it },
                label = { Text("رقم هاتف المتجر") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = storePhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (storeName.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال اسم المتجر ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newStore = StoreEntity(
                        id = UUID.randomUUID().toString(),
                        sectionId = "stores",
                        name = storeName,
                        phone = cleanPhone,
                        localNeighborhood = storeAddress,
                        cityId = storeCity,
                        description = storeDesc,
                        ownerName = storeOwnerName,
                        password = storePassword,
                        images = storePhotosList,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveStore(newStore)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب المتجر للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب تسجيل المتجر 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/StoreRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(store_code)

# 3. RestaurantRegistrationForm.kt
rest_code = common_imports + """
@Composable
fun RestaurantRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var restName by remember { mutableStateOf("") }
    var restPhone by remember { mutableStateOf("") }
    var restDesc by remember { mutableStateOf("") }
    var restCity by remember { mutableStateOf("") }
    var restAddress by remember { mutableStateOf("") }
    var restPassword by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🍔 استمارة تسجيل مطعم / كافيه", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = restName,
                onValueChange = { restName = it },
                label = { Text("اسم المطعم / الكافيه") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = restPhone,
                onValueChange = { restPhone = it },
                label = { Text("رقم الهاتف") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = restPhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (restName.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال اسم المطعم ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newRest = StoreEntity(
                        id = UUID.randomUUID().toString(),
                        sectionId = "restaurants",
                        name = restName,
                        phone = cleanPhone,
                        localNeighborhood = restAddress,
                        cityId = restCity,
                        description = restDesc,
                        password = restPassword,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveStore(newRest)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب المطعم للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب تسجيل المطعم 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/RestaurantRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(rest_code)

# 4. PropertyRegistrationForm.kt
prop_code = common_imports + """
@Composable
fun PropertyRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var propTitle by remember { mutableStateOf("") }
    var propPhone by remember { mutableStateOf("") }
    var propPrice by remember { mutableStateOf("") }
    var propCity by remember { mutableStateOf("") }
    var propArea by remember { mutableStateOf("") }
    var propPassword by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏢 استمارة إدراج عقار / أرض", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = propTitle,
                onValueChange = { propTitle = it },
                label = { Text("عنوان العقار") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = propPhone,
                onValueChange = { propPhone = it },
                label = { Text("رقم هاتف المسؤول") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = propPhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (propTitle.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال عنوان العقار ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newProperty = PropertyEntity(
                        id = UUID.randomUUID().toString(),
                        title = propTitle,
                        phone = cleanPhone,
                        cityId = propCity,
                        localNeighborhood = propArea,
                        price = propPrice.toDoubleOrNull() ?: 0.0,
                        type = "rent",
                        propertyType = "apartment",
                        password = propPassword,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveProperty(newProperty)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب العقار للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب إدراج العقار 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/PropertyRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(prop_code)

# 5. MedicalRegistrationForm.kt
med_code = common_imports + """
@Composable
fun MedicalRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var medName by remember { mutableStateOf("") }
    var medPhone by remember { mutableStateOf("") }
    var medDesc by remember { mutableStateOf("") }
    var medCity by remember { mutableStateOf("") }
    var medAddress by remember { mutableStateOf("") }
    var medPassword by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏥 استمارة تسجيل مركز طبي / عيادة", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = medName,
                onValueChange = { medName = it },
                label = { Text("اسم المركز الطبي / العيادة") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = medPhone,
                onValueChange = { medPhone = it },
                label = { Text("رقم الهاتف") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = medPhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (medName.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال اسم المركز ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newMed = StoreEntity(
                        id = UUID.randomUUID().toString(),
                        sectionId = "medical",
                        name = medName,
                        phone = cleanPhone,
                        localNeighborhood = medAddress,
                        cityId = medCity,
                        description = medDesc,
                        password = medPassword,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveStore(newMed)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب المركز الطبي للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب تسجيل المركز الطبي 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/MedicalRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(med_code)

# 6. JobRegistrationForm.kt
job_code = common_imports + """
@Composable
fun JobRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var jobTitleInput by remember { mutableStateOf("") }
    var jobCompanyNameInput by remember { mutableStateOf("") }
    var jobPhoneInput by remember { mutableStateOf("") }
    var jobSalaryInput by remember { mutableStateOf("") }
    var jobDescInput by remember { mutableStateOf("") }
    var jobCityInput by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💼 استمارة نشر وظيفة / شاغر وظيفي", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = jobTitleInput,
                onValueChange = { jobTitleInput = it },
                label = { Text("المسمى الوظيفي") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = jobCompanyNameInput,
                onValueChange = { jobCompanyNameInput = it },
                label = { Text("اسم الشركة / الجهة") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = jobPhoneInput,
                onValueChange = { jobPhoneInput = it },
                label = { Text("رقم التواصل") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = jobPhoneInput.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (jobTitleInput.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال المسمى الوظيفي ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newJob = JobEntity(
                        id = UUID.randomUUID().toString(),
                        sectionId = "jobs",
                        title = jobTitleInput.trim(),
                        companyName = jobCompanyNameInput.trim(),
                        phone = cleanPhone,
                        cityId = jobCityInput.trim(),
                        salary = jobSalaryInput.trim(),
                        description = jobDescInput.trim(),
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveJob(newJob)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال إعلان الوظيفة للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال إعلان الوظيفة 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
"""
with open(f"{forms_dir}/JobRegistrationForm.kt", "w", encoding="utf-8") as f:
    f.write(job_code)

print("Updated fix_forms.py execution completed.")
