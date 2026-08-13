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
fun AdminStoresPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_STORES")) {
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
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🏪 إدارة المحلات والأنشطة التجارية الكبرى", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            
            val storesList by viewModel.stores.collectAsState()
            val filteredStores = storesList.filter { it.sectionId == "" || it.sectionId == "stores" || it.sectionId == "store" }

            var selectedFilterType by remember { mutableStateOf("الكل") }
            var storeToManageProducts by remember { mutableStateOf<StoreEntity?>(null) }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المحلات التجارية", color = Color.LightGray, fontSize = 10.sp)
                        Text("${filteredStores.size}", color = themeColors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("أعضاء مميزين VIP", color = Color.LightGray, fontSize = 10.sp)
                        Text("${filteredStores.count { it.isVip }}", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = storesSearchQueryState.value,
                onValueChange = { storesSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم المحل أو رقم الهاتف...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(listOf("الكل", "مميز VIP", "موثق 🛡️", "موصى به ⭐", "محظور 🚫")) { filter ->
                    val isSel = selectedFilterType == filter
                    Button(
                        onClick = { selectedFilterType = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) themeColors.accent else themeColors.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(filter, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                    }
                }
            }

            val finalFiltered = filteredStores.filter { store ->
                val matchesSearch = store.name.contains(storesSearchQueryState.value, ignoreCase = true) || store.phone.contains(storesSearchQueryState.value)
                val matchesFilter = when (selectedFilterType) {
                    "الكل" -> true
                    "مميز VIP" -> store.isVip
                    "موثق 🛡️" -> store.isVerified
                    "موصى به ⭐" -> store.isRecommended
                    "محظور 🚫" -> store.isBlocked
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            if (finalFiltered.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد محلات تجارية مطابقة للبحث حالياً 🏪", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            } else {
                finalFiltered.forEach { store ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(store.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("📍 ${store.cityId} - ${store.localNeighborhood.ifBlank { "غير محدد" }}", color = Color.Gray, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (store.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                    if (store.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                    if (store.isRecommended) Badge(containerColor = themeColors.accent) { Text("موصى به", color = Color.Black, fontSize = 9.sp) }
                                    if (store.isBlocked) Badge(containerColor = Color.Red) { Text("محظور", color = Color.White, fontSize = 9.sp) }
                                }
                            }

                            Text("📝 الوصف: ${store.description.ifBlank { "لا يوجد وصف متوفر" }}", color = Color.LightGray, fontSize = 11.sp)
                            Text("📞 رقم التواصل: ${store.phone}", color = Color.LightGray, fontSize = 11.sp)
                            Text("🕒 ساعات العمل: ${store.workingHours.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 11.sp)

                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleStoreVip(store.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (store.isVip) Color.Yellow else Color.DarkGray),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(if (store.isVip) "🌟 إلغاء VIP" else "🏆 تفعيل VIP", fontSize = 10.sp, color = if (store.isVip) Color.Black else Color.White)
                                }
                                Button(
                                    onClick = { viewModel.toggleStoreVerified(store.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (store.isVerified) Color.Green else Color.DarkGray),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(if (store.isVerified) "🛡️ إلغاء توثيق" else "🛡️ توثيق", fontSize = 10.sp, color = if (store.isVerified) Color.Black else Color.White)
                                }
                                Button(
                                    onClick = { viewModel.toggleStoreRecommended(store.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (store.isRecommended) themeColors.accent else Color.DarkGray),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(if (store.isRecommended) "⭐ إلغاء توصية" else "⭐️ إبراز وتوصية", fontSize = 10.sp, color = if (store.isRecommended) Color.Black else Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { storeToManageProducts = store },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📦 إدارة المنتجات", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { viewModel.toggleStoreBlock(store.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (store.isBlocked) Color.Gray else Color.Red),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(if (store.isBlocked) "🔓 إلغاء الحظر" else "🚫 حظر المحل", fontSize = 10.sp, color = Color.White)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteStore(store.id) },
                                    modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            storeToManageProducts?.let { currentStore ->
                var prodName by remember { mutableStateOf("") }
                var prodDesc by remember { mutableStateOf("") }
                var prodPrice by remember { mutableStateOf("") }
                
                val productsList = remember(currentStore.productAttachmentsJson) {
                    val list = mutableListOf<Triple<String, String, String>>()
                    try {
                        if (currentStore.productAttachmentsJson.isNotBlank()) {
                            val arr = org.json.JSONArray(currentStore.productAttachmentsJson)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                list.add(Triple(
                                    obj.optString("name", ""),
                                    obj.optString("desc", ""),
                                    obj.optString("price", "")
                                ))
                            }
                        }
                    } catch (e: Exception) {}
                    list
                }

                AlertDialog(
                    onDismissRequest = { storeToManageProducts = null },
                    title = { Text("📦 إدارة منتجات المحل: ${currentStore.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("أضف منتج أو سلعة جديدة:", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = prodName,
                                onValueChange = { prodName = it },
                                placeholder = { Text("اسم المنتج", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = prodDesc,
                                onValueChange = { prodDesc = it },
                                placeholder = { Text("وصف المنتج ومميزاته", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = prodPrice,
                                onValueChange = { prodPrice = it },
                                placeholder = { Text("السعر", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            Button(
                                onClick = {
                                    if (prodName.isNotBlank()) {
                                        val updatedList = productsList + Triple(prodName, prodDesc, prodPrice)
                                        val arr = org.json.JSONArray()
                                        updatedList.forEach { (n, d, p) ->
                                            val obj = org.json.JSONObject()
                                            obj.put("name", n)
                                            obj.put("desc", d)
                                            obj.put("price", p)
                                            arr.put(obj)
                                        }
                                        val updatedJson = arr.toString()
                                        val updatedStore = currentStore.copy(productAttachmentsJson = updatedJson)
                                        try {
                                            FirebaseFirestore.getInstance().collection("stores").document(currentStore.id).set(updatedStore)
                                            viewModel._stores.value = viewModel._stores.value.map { if (it.id == currentStore.id) updatedStore else it }
                                            viewModel.triggerNotification("📦 تم إضافة المنتج بنجاح!")
                                            storeToManageProducts = updatedStore
                                        } catch(e: Exception) {}

                                        prodName = ""
                                        prodDesc = ""
                                        prodPrice = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة السلعة للمخزون ➕", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { storeToManageProducts = null }) {
                            Text("تم وإغلاق", color = Color.White)
                        }
                    },
                    containerColor = themeColors.surface
                )
            }
        }
    }
}
