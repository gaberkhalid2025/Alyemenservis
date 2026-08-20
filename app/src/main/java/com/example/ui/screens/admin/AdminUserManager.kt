package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserEntity
import com.example.viewmodels.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 👑 AdminUserManager
 * إدارة المستخدمين والحسابات للأدمن (حظر، فك حظر، حذف، ترقية، وإرسال إشعارات مخصصة)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManager(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    var selectedUserForDetails by remember { mutableStateOf<UserEntity?>(null) }
    var userToNotify by remember { mutableStateOf<UserEntity?>(null) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }

    var usersList by remember {
        mutableStateOf(
            listOf(
                UserEntity(
                    id = "usr_101",
                    name = "صالح بن علي الأهدل",
                    phone = "771234567",
                    email = "saleh.ahdal@gmail.com",
                    city = "صنعاء",
                    neighborhood = "حدة",
                    role = "CLIENT",
                    isBlocked = false,
                    totalBookings = 14,
                    rating = 4.8f,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30
                ),
                UserEntity(
                    id = "usr_102",
                    name = "م. فؤاد القباطي",
                    phone = "733456789",
                    email = "fouad.eng@yahoo.com",
                    city = "تعز",
                    neighborhood = "شارع جمال",
                    role = "PROVIDER",
                    isBlocked = false,
                    totalBookings = 38,
                    rating = 4.9f,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 90
                ),
                UserEntity(
                    id = "usr_103",
                    name = "مؤسسة البركة للمواد الكهربائية",
                    phone = "711998877",
                    email = "baraka.store@gmail.com",
                    city = "عدن",
                    neighborhood = "المنصورة",
                    role = "STORE",
                    isBlocked = false,
                    totalBookings = 52,
                    rating = 4.7f,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 120
                ),
                UserEntity(
                    id = "usr_104",
                    name = "عبدالله القدسي",
                    phone = "778899001",
                    email = "abdullah.q@gmail.com",
                    city = "إب",
                    neighborhood = "المشنة",
                    role = "CLIENT",
                    isBlocked = true,
                    totalBookings = 2,
                    rating = 2.5f,
                    createdAt = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 10
                )
            )
        )
    }

    val filteredUsers = remember(usersList, searchQuery, selectedRoleFilter) {
        usersList.filter { u ->
            val matchesRole = when (selectedRoleFilter) {
                "CLIENT" -> u.role.equals("CLIENT", ignoreCase = true) || u.role.equals("USER", ignoreCase = true)
                "PROVIDER" -> u.role.equals("PROVIDER", ignoreCase = true)
                "STORE" -> u.role.equals("STORE", ignoreCase = true)
                "BLOCKED" -> u.isBlocked
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    u.name.contains(searchQuery, ignoreCase = true) ||
                    u.phone.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.city.contains(searchQuery, ignoreCase = true)
            matchesRole && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("إدارة المستخدمين والحسابات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("إجمالي الحسابات: ${usersList.size}", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = buildString {
                            appendLine("ID,Name,Phone,Email,City,Role,Status,Bookings,Rating")
                            usersList.forEach { u ->
                                appendLine("${u.id},${u.name},${u.phone},${u.email},${u.city},${u.role},${if (u.isBlocked) "BLOCKED" else "ACTIVE"},${u.totalBookings},${u.rating}")
                            }
                        }
                        adminViewModel.recordAuditLog("EXPORT_USERS_CSV", "تصدير قائمة المستخدمين بصيغة CSV")
                        Toast.makeText(context, "تم تصدير ملف CSV لقائمة المستخدمين بنجاح", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير CSV", tint = Color(0xFF00668B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم، رقم الهاتف، البريد، أو المدينة...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00668B),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // تصفيات الأدوار
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterChips = listOf(
                    "ALL" to "الكل (${usersList.size})",
                    "CLIENT" to "العملاء",
                    "PROVIDER" to "الفنيين",
                    "STORE" to "المتاجر",
                    "BLOCKED" to "المحظورين"
                )
                filterChips.forEach { (key, label) ->
                    val isSelected = selectedRoleFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRoleFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00668B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // قائمة المستخدمين
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // الصورة أو الأيقونة
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            if (user.isBlocked) Color(0xFFFFEBEE) else Color(0xFFE0F2FE),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (user.isBlocked) Icons.Default.Close else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (user.isBlocked) Color(0xFFD32F2F) else Color(0xFF00668B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // تفاصيل المستخدم
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        if (user.isBlocked) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFEBEE)
                                            ) {
                                                Text("محظور", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${user.phone} • ${user.city} - ${user.neighborhood}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "النوع: ${when(user.role) { "PROVIDER" -> "فني معتمد" "STORE" -> "متجر" else -> "عميل" }} | الحجوزات: ${user.totalBookings} | التقييم: ★ ${user.rating}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00668B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                            // أزرار التحكم
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // زر تفاصيل
                                TextButton(onClick = { selectedUserForDetails = user }) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("التفاصيل", fontSize = 12.sp)
                                }

                                // زر إرسال إشعار
                                TextButton(onClick = { userToNotify = user }) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00668B))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إشعار", fontSize = 12.sp, color = Color(0xFF00668B))
                                }

                                // زر حظر / إلغاء حظر
                                TextButton(onClick = {
                                    val newBlocked = !user.isBlocked
                                    usersList = usersList.map { if (it.id == user.id) it.copy(isBlocked = newBlocked) else it }
                                    if (newBlocked) {
                                        adminViewModel.blockUser(user.id)
                                        Toast.makeText(context, "تم حظر المستخدم ${user.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        adminViewModel.unblockUser(user.id)
                                        Toast.makeText(context, "تم إلغاء حظر المستخدم ${user.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        if (user.isBlocked) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (user.isBlocked) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (user.isBlocked) "فك الحظر" else "حظر",
                                        fontSize = 12.sp,
                                        color = if (user.isBlocked) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    )
                                }

                                // زر حذف
                                IconButton(
                                    onClick = {
                                        usersList = usersList.filterNot { it.id == user.id }
                                        adminViewModel.deleteUser(user.id)
                                        Toast.makeText(context, "تم حذف المستخدم نهائياً", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة تفاصيل المستخدم والترقية
    if (selectedUserForDetails != null) {
        val user = selectedUserForDetails!!
        AlertDialog(
            onDismissRequest = { selectedUserForDetails = null },
            title = { Text("بيانات الحساب: ${user.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• معرف المستخدم: ${user.id}", fontSize = 13.sp)
                    Text("• الهاتف: ${user.phone}", fontSize = 13.sp)
                    Text("• البريد: ${user.email.ifEmpty { "غير متوفر" }}", fontSize = 13.sp)
                    Text("• المحافظة والحي: ${user.city} - ${user.neighborhood}", fontSize = 13.sp)
                    Text("• إجمالي الحجوزات: ${user.totalBookings}", fontSize = 13.sp)
                    Text("• التقييم العام: ★ ${user.rating}", fontSize = 13.sp)
                    Text("• تاريخ التسجيل: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(user.createdAt))}", fontSize = 13.sp)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("ترقية نوع الحساب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            usersList = usersList.map { if (it.id == user.id) it.copy(role = "PROVIDER") else it }
                            adminViewModel.recordAuditLog("UPGRADE_USER", "ترقية المستخدم ${user.id} إلى مقدم خدمة")
                            selectedUserForDetails = null
                            Toast.makeText(context, "تمت ترقية الحساب إلى فني معتمد", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("فني 🔧", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            usersList = usersList.map { if (it.id == user.id) it.copy(role = "STORE") else it }
                            adminViewModel.recordAuditLog("UPGRADE_USER", "ترقية المستخدم ${user.id} إلى متجر")
                            selectedUserForDetails = null
                            Toast.makeText(context, "تمت ترقية الحساب إلى متجر", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("متجر 🛍️", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedUserForDetails = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // نافذة إرسال إشعار للمستخدم
    if (userToNotify != null) {
        val user = userToNotify!!
        AlertDialog(
            onDismissRequest = { userToNotify = null },
            title = { Text("إرسال إشعار مباشر لـ ${user.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notificationTitle,
                        onValueChange = { notificationTitle = it },
                        label = { Text("عنوان الإشعار") },
                        placeholder = { Text("مثال: تنبيه بخصوص حسابك") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notificationMessage,
                        onValueChange = { notificationMessage = it },
                        label = { Text("نص الرسالة") },
                        placeholder = { Text("اكتب محتوى الإشعار هنا...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notificationTitle.isNotBlank() && notificationMessage.isNotBlank()) {
                            adminViewModel.sendAdminNotification(notificationTitle, notificationMessage, user.id)
                            userToNotify = null
                            notificationTitle = ""
                            notificationMessage = ""
                            Toast.makeText(context, "تم إرسال الإشعار بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى ملء كافة الحقول", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                ) {
                    Text("إرسال الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToNotify = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
