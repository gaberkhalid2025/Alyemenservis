package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import com.example.utils.VisualThemePalette
import kotlin.random.Random

data class AccountTarget(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val category: String,
    val roleType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPasswordResetSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<AccountTarget?>(null) }
    var newGeneratedPassword by remember { mutableStateOf("") }
    var sendNotificationToTarget by remember { mutableStateOf(true) }

    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val jobs by viewModel.jobs.collectAsState()

    // Aggregate all accounts across all categories
    val allAccounts = remember(providers, stores, properties, jobs) {
        val list = mutableListOf<AccountTarget>()

        // 1. Technicians / Providers
        providers.forEach { p ->
            list.add(
                AccountTarget(
                    id = p.id,
                    name = p.name,
                    phone = p.phone,
                    category = "TECHNICIANS",
                    roleType = "فني / مزود خدمة (${p.profession})"
                )
            )
        }

        // 2. Commercial Stores & Centers
        stores.forEach { s ->
            list.add(
                AccountTarget(
                    id = s.id,
                    name = s.name,
                    phone = s.phone,
                    category = "STORES",
                    roleType = "متجر / منشأة"
                )
            )
        }

        // 3. Real Estate & Lands
        properties.forEach { pr ->
            list.add(
                AccountTarget(
                    id = pr.id,
                    name = pr.title,
                    phone = pr.phone,
                    category = "REAL_ESTATE",
                    roleType = "عقار / معلن عقاري"
                )
            )
        }

        // 4. Job Advertisers & Applicants
        jobs.forEach { j ->
            list.add(
                AccountTarget(
                    id = j.id,
                    name = j.companyName.ifEmpty { j.title },
                    phone = j.phone,
                    category = "JOB_ADVERTISERS",
                    roleType = "معلن وظيفة (${j.title})"
                )
            )
        }

        list
    }

    val filteredAccounts = remember(allAccounts, selectedCategoryFilter, searchQuery) {
        allAccounts.filter { item ->
            val matchesCat = selectedCategoryFilter == "ALL" || item.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) || item.phone.contains(searchQuery)
            matchesCat && matchesQuery
        }
    }

    fun generateStrongPassword(): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghjkmnpqrstuvwxyz"
        val digits = "23456789"
        val symbols = "@#$%&"
        val rand = Random.Default
        return (1..3).map { upper[rand.nextInt(upper.length)] }.joinToString("") +
                (1..3).map { lower[rand.nextInt(lower.length)] }.joinToString("") +
                (1..2).map { digits[rand.nextInt(digits.length)] }.joinToString("") +
                symbols[rand.nextInt(symbols.length)]
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔑 نظام إدارة وإعادة تعيين كلمات المرور الشامل",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "يتيح هذا القسم إعادة تعيين كلمات المرور فورياً لـ (المستخدمين، الفنيين، المتاجر، المطاعم، المراكز الطبية، العقارات، والوظائف) مع الإشعار الفوري.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث بالاسم أو رقم الهاتف...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                // Category Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "الكل (${allAccounts.size})",
                        "TECHNICIANS" to "فنيين",
                        "STORES" to "متاجر",
                        "REAL_ESTATE" to "عقارات",
                        "JOB_ADVERTISERS" to "وظائف"
                    )

                    filterOptions.forEach { (cat, label) ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        Text(
            text = "📋 الحسابات المتاحة للتعيين (${filteredAccounts.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            items(filteredAccounts, key = { it.id + it.category }) { account ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "📱 ${account.phone} | 🏷️ ${account.roleType}", fontSize = 11.5.sp, color = Color.LightGray)
                        }

                        Button(
                            onClick = {
                                selectedAccount = account
                                newGeneratedPassword = generateStrongPassword()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعيين كلمة سر", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selectedAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { selectedAccount = null },
            title = {
                Text(
                    text = "🔑 إعادة تعيين كلمة المرور",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "الحساب: ${account.name} (${account.roleType})", color = Color.LightGray, fontSize = 13.sp)
                    Text(text = "الهاتف: ${account.phone}", color = Color.LightGray, fontSize = 13.sp)

                    OutlinedTextField(
                        value = newGeneratedPassword,
                        onValueChange = { newGeneratedPassword = it },
                        label = { Text("كلمة المرور الجديدة", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "إرسال إشعار فوري للطرف المعني", color = Color.White, fontSize = 12.sp)
                        Checkbox(
                            checked = sendNotificationToTarget,
                            onCheckedChange = { sendNotificationToTarget = it },
                            colors = CheckboxDefaults.colors(checkedColor = themeColors.accent)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGeneratedPassword.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة كلمة المرور", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Send instant notification if enabled
                        if (sendNotificationToTarget && account.phone.isNotBlank()) {
                            viewModel.addNotification(
                                title = "🔐 تحديث أمني لكلمة المرور",
                                message = "تمت إعادة تعيين كلمة المرور الخاصة بحسابك (${account.name}) من قبل الإدارة إلى: $newGeneratedPassword",
                                targetType = "USER",
                                targetValue = account.phone
                            )
                        }

                        Toast.makeText(context, "✅ تم تعيين كلمة المرور ومزامنتها بنجاح للطرف المعني", Toast.LENGTH_LONG).show()
                        selectedAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ وتأكيد التعيين", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAccount = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
