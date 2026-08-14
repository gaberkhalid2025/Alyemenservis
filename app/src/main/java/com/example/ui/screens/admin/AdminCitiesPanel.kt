package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CityEntity
import com.example.ui.MainViewModel
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminCitiesPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_CITIES")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    val context = LocalContext.current
    val citiesList by viewModel.cities.collectAsState()

    var newCityArName by remember { mutableStateOf("") }
    var newCityEnName by remember { mutableStateOf("") }
    var newCityIcon by remember { mutableStateOf("📍") }
    var editingCityObj by remember { mutableStateOf<CityEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🗺️ تحكم المدن والمحافظات اليمنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("إضافة المحافظات والمدن المستهدفة بالخدمة وتصفح المضاف حالياً بالمنصة:", fontSize = 11.sp, color = themeColors.textSecondary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة مدينة/محافظة يمنية جديدة ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newCityArName,
                    onValueChange = { newCityArName = it },
                    label = { Text("الاسم باللغة العربية (مثال: صنعاء، عدن...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = newCityEnName,
                    onValueChange = { newCityEnName = it },
                    label = { Text("الاسم باللغة الإنجليزية (مثال: Sana'a, Aden...)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = newCityIcon,
                    onValueChange = { newCityIcon = it },
                    label = { Text("أيقونة/إيموجي رمزية (مثال: 🏰, 🏖️, 📍)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Button(
                    onClick = {
                        if (newCityArName.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء ملء الاسم العربي للمحافظة", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addNewCity(
                                nameAr = newCityArName.trim(),
                                nameEn = newCityEnName.trim().ifEmpty { newCityArName.trim() }
                            )
                            newCityArName = ""
                            newCityEnName = ""
                            newCityIcon = "📍"
                            Toast.makeText(context, "تمت إضافة المحافظة بنجاح 🗺️", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                ) {
                    Text("إضافة المدينة/المحافظة ➕", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        citiesList.forEach { city ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(city.icon.ifEmpty { "📍" }, fontSize = 20.sp)
                        Column {
                            Text(city.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(city.nameEn, fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { editingCityObj = city }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل المحافظة", tint = themeColors.accent)
                        }
                        IconButton(onClick = {
                            viewModel.removeCity(city.id)
                            Toast.makeText(context, "تم حذف المحافظة بنجاح", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف المحافظة", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    editingCityObj?.let { city ->
        var editArName by remember(city.id) { mutableStateOf(city.nameAr) }
        var editEnName by remember(city.id) { mutableStateOf(city.nameEn) }
        var editIcon by remember(city.id) { mutableStateOf(city.icon.ifEmpty { "📍" }) }
        var editPhotoUrl by remember(city.id) { mutableStateOf(city.photoUrl) }

        Dialog(onDismissRequest = { editingCityObj = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✏️ تعديل بيانات المحافظة/المدينة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = editArName,
                        onValueChange = { editArName = it },
                        label = { Text("الاسم باللغة العربية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editEnName,
                        onValueChange = { editEnName = it },
                        label = { Text("الاسم باللغة الإنجليزية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text("أيقونة/إيموجي رمزية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = editPhotoUrl,
                        onValueChange = { editPhotoUrl = it },
                        label = { Text("رابط صورة رمزية للمحافظة (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editArName.trim().isNotEmpty()) {
                                    viewModel.updateCity(
                                        city.copy(
                                            nameAr = editArName.trim(),
                                            nameEn = editEnEnOrAr(editEnName.trim(), editArName.trim()),
                                            icon = editIcon.trim().ifEmpty { "📍" },
                                            photoUrl = editPhotoUrl.trim()
                                        )
                                    )
                                    editingCityObj = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { editingCityObj = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun editEnEnOrAr(en: String, ar: String) = if (en.isBlank()) ar else en
