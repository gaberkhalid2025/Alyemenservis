package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminRestaurantsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_RESTAURANTS")) {
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

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🍔 إدارة المطاعم والكافيهات وقوائم الطعام والخصومات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            
            val storesList by viewModel.stores.collectAsState()
            val filteredRestaurants = storesList.filter { it.sectionId == "restaurants" || it.sectionId == "restaurant" }

            var selectedFilterType by remember { mutableStateOf("الكل") }

            OutlinedTextField(
                value = restaurantsSearchQueryState.value,
                onValueChange = { restaurantsSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم المطعم أو الكافيه...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            val finalFiltered = filteredRestaurants.filter { rest ->
                val matchesSearch = rest.name.contains(restaurantsSearchQueryState.value, ignoreCase = true) || rest.phone.contains(restaurantsSearchQueryState.value)
                matchesSearch
            }

            if (finalFiltered.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد مطاعم مسجلة حالياً 🍔", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            } else {
                finalFiltered.forEach { rest ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(rest.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (rest.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                    if (rest.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                }
                            }
                            Text("📞 ${rest.phone} | 📍 ${rest.cityId}", color = Color.LightGray, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { viewModel.toggleStoreVip(rest.id) }, colors = ButtonDefaults.buttonColors(containerColor = if (rest.isVip) Color.Yellow else Color.DarkGray)) {
                                    Text(if (rest.isVip) "إلغاء VIP" else "تفعيل VIP", fontSize = 10.sp, color = if (rest.isVip) Color.Black else Color.White)
                                }
                                Button(onClick = { viewModel.deleteStore(rest.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                    Text("حذف 🗑️", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
