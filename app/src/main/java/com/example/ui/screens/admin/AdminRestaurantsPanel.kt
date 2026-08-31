@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.utils.VisualThemePalette
import com.example.data.StoreEntity
import kotlinx.coroutines.launch

@Composable
fun AdminRestaurantsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val adminRoleStr by viewModel.adminRole.collectAsState()
    val supervisorPermissions by viewModel.currentSupervisorPermissions.collectAsState()
    if (!PermissionGuard.hasPermission(
            role = RoleManager.fromRoleString(adminRoleStr),
            permission = PermissionGuard.PERMISSION_RESTAURANTS,
            supervisorGrantedPermissions = supervisorPermissions
         )
    ) {
        PermissionGuard.UnauthorizedView()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stores by viewModel.stores.collectAsState()
    val restaurants = stores.filter { 
        it.sectionId == "restaurants" || it.categoryId.contains("rest", true) || it.categoryId.contains("مطعم", true) || it.name.contains("مطعم", true) || it.name.contains("كافيه", true) || it.name.contains("وجب", true) 
    }

    var searchQuery by remember { mutableStateOf("") }
    val displayList = restaurants
    val filteredList = displayList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }.sortedByDescending { it.createdAt }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRestaurant by remember { mutableStateOf<StoreEntity?>(null) }
    var restNameState by remember { mutableStateOf("") }
    var restPhoneState by remember { mutableStateOf("") }
    var restCityState by remember { mutableStateOf("") }
    var restNeighborhoodState by remember { mutableStateOf("") }
    var restHoursState by remember { mutableStateOf("") }
    var restDescState by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🍔 إدارة المطاعم والكافيهات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = {
                    editingRestaurant = null
                    restNameState = ""
                    restPhoneState = ""
                    restCityState = ""
                    restNeighborhoodState = ""
                    restHoursState = "9:00 AM - 11:00 PM"
                    restDescState = ""
                    showAddEditDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مطعم", tint = themeColors.accent)
                }
            }

            Text(
                text = "إدارة منشآت الأطعمة والمطاعم، توثيق الشارات، ترقية VIP، والتثبيت والتوصية والحظر:",
                fontSize = 11.sp,
                color = themeColors.textSecondary
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث في المطاعم والكافيهات") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            if (filteredList.isEmpty()) {
                Text("لا توجد مطاعم مسجلة حالياً", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                filteredList.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("📞 ${item.phone} • 📍 ${item.cityId} - ${item.localNeighborhood}", color = themeColors.textSecondary, fontSize = 11.sp)
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingRestaurant = item
                                        restNameState = item.name
                                        restPhoneState = item.phone
                                        restCityState = item.cityId
                                        restNeighborhoodState = item.localNeighborhood
                                        restHoursState = item.workingHours
                                        restDescState = item.description
                                        showAddEditDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.setStoreActive(item.id, !item.isActive) }) {
                                        Icon(
                                            imageVector = if (item.isActive) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = "Active",
                                            tint = if (item.isActive) Color.Green else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteStore(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isVip, onCheckedChange = { viewModel.setStoreVip(item.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)), modifier = Modifier.size(28.dp))
                                    Text("VIP", fontSize = 10.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isVerified, onCheckedChange = { viewModel.setStoreVerified(item.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)), modifier = Modifier.size(28.dp))
                                    Text("موثق", fontSize = 10.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isRecommended, onCheckedChange = { viewModel.setStoreRecommended(item.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)), modifier = Modifier.size(28.dp))
                                    Text("موصى به", fontSize = 10.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isPinned, onCheckedChange = { viewModel.setStorePinned(item.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)), modifier = Modifier.size(28.dp))
                                    Text("تثبيت", fontSize = 10.sp, color = Color.White)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isChatDisabled, onCheckedChange = { viewModel.setStoreChatDisabled(item.id, it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)), modifier = Modifier.size(28.dp))
                                    Text("قفل الدردشة", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = item.isBlocked, onCheckedChange = { viewModel.setStoreBlocked(item.id, it, "حظر إداري") }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFB91C1C)), modifier = Modifier.size(28.dp))
                                    Text("حظر المطعم", fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Restaurant Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text(if (editingRestaurant == null) "🍔 إضافة مطعم جديد" else "📝 تعديل بيانات المطعم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = restNameState,
                        onValueChange = { restNameState = it },
                        label = { Text("اسم المطعم / الكافيه") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restPhoneState,
                        onValueChange = { restPhoneState = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restCityState,
                        onValueChange = { restCityState = it },
                        label = { Text("المحافظة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restNeighborhoodState,
                        onValueChange = { restNeighborhoodState = it },
                        label = { Text("الحي / المنطقة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restHoursState,
                        onValueChange = { restHoursState = it },
                        label = { Text("ساعات العمل") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = restDescState,
                        onValueChange = { restDescState = it },
                        label = { Text("الوصف والتفاصيل") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = editingRestaurant ?: StoreEntity()
                        val finalRest = base.copy(
                            name = restNameState,
                            phone = restPhoneState,
                            cityId = restCityState,
                            localNeighborhood = restNeighborhoodState,
                            workingHours = restHoursState,
                            description = restDescState,
                            sectionId = "restaurants",
                            categoryId = "restaurants",
                            isActive = true,
                            isApproved = true
                        )
                        viewModel.saveStore(finalRest)
                        showAddEditDialog = false
                        Toast.makeText(context, "💾 تم حفظ بيانات المطعم بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
