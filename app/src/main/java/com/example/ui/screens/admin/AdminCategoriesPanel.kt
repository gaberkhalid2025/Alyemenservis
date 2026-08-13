package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.util.UserRole

@Composable
fun AdminCategoriesPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    val currentRole = RoleManager.fromRoleString(viewModel.adminRole.value)
    if (!PermissionGuard.hasPermission(currentRole, "MANAGE_CATEGORIES")) {
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

    val categories by viewModel.categories.collectAsState()
    val newCatName = state.newCatNameState.value
    val newCatIcon = state.newCatIconState.value
    val showDeleteCatId = state.showDeleteCategoryConfirmIdState.value
    val editCategoryObj = state.showEditCategoryObjState.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🗂️ إدارة الأقسام والخدمات والمدن", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("إضافة وحذف وتعديل تصنيفات الخدمات وأقسام الدليل العامة في التطبيق.", color = Color.LightGray, fontSize = 11.sp)

        // Add Category Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("➕ إضافة قسم/تصنيف جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { state.newCatNameState.value = it },
                    label = { Text("اسم القسم باللغة العربية", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                )

                OutlinedTextField(
                    value = newCatIcon,
                    onValueChange = { state.newCatIconState.value = it },
                    label = { Text("أيقونة الرمز التعبيري (Emoji) (مثال: 🔧)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White)
                )

                Button(
                    onClick = {
                        if (newCatName.isNotBlank() && newCatIcon.isNotBlank()) {
                            viewModel.addNewCategory(
                                nameAr = newCatName.trim(),
                                nameEn = newCatName.trim(),
                                icon = newCatIcon.trim(),
                                description = "Added by admin panel"
                            )
                            state.newCatNameState.value = ""
                            state.newCatIconState.value = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ وإضافة القسم 💾", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Categories List
        Text("📋 قائمة الأقسام الحالية المتاحة (${categories.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

        categories.forEach { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${cat.icon} ${cat.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { state.showEditCategoryObjState.value = cat }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { state.showDeleteCategoryConfirmIdState.value = cat.id }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Category Edit Dialog
    if (editCategoryObj != null) {
        var editName by remember { mutableStateOf(editCategoryObj.name) }
        var editIcon by remember { mutableStateOf(editCategoryObj.icon) }

        AlertDialog(
            onDismissRequest = { state.showEditCategoryObjState.value = null },
            title = { Text("📝 تعديل بيانات القسم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم القسم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text("رمز الأيقونة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editIcon.isNotBlank()) {
                            viewModel.editCategory(editCategoryObj.id, editName.trim(), editIcon.trim())
                            state.showEditCategoryObjState.value = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ التغييرات ✔️", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showEditCategoryObjState.value = null }) {
                    Text("إلغاء ❌", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = themeColors.surface
        )
    }

    // Category Delete Confirmation Dialog
    if (showDeleteCatId != null) {
        AlertDialog(
            onDismissRequest = { state.showDeleteCategoryConfirmIdState.value = null },
            title = { Text("⚠️ تأكيد عملية الحذف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا القسم نهائياً من النظام؟ قد يؤثر ذلك على الجهات المسجلة تحت هذا القسم.", color = Color.LightGray, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(showDeleteCatId)
                        state.showDeleteCategoryConfirmIdState.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، حذف 🗑️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showDeleteCategoryConfirmIdState.value = null }) {
                    Text("تراجع ❌", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = themeColors.surface
        )
    }
}
