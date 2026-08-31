package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.ui.screens.admin.components.AdminFilterChips
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🏪 Admin Panel: Stores & Properties Management (إدارة المتاجر والعقارات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoresPropertiesPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var selectedSection by remember { mutableStateOf("المتاجر") }
    val sections = listOf("المتاجر", "العقارات")

    var searchQuery by remember { mutableStateOf("") }

    // Dialog states for Store
    var showStoreDialog by remember { mutableStateOf(false) }
    var editingStore by remember { mutableStateOf<StoreEntity?>(null) }
    var storeNameState by remember { mutableStateOf("") }
    var storePhoneState by remember { mutableStateOf("") }
    var storeCityState by remember { mutableStateOf("") }
    var storeNeighborhoodState by remember { mutableStateOf("") }
    var storeHoursState by remember { mutableStateOf("") }
    var storeDescState by remember { mutableStateOf("") }

    // Dialog states for Property
    var showPropertyDialog by remember { mutableStateOf(false) }
    var editingProperty by remember { mutableStateOf<PropertyEntity?>(null) }
    var propTitleState by remember { mutableStateOf("") }
    var propPhoneState by remember { mutableStateOf("") }
    var propCityState by remember { mutableStateOf("") }
    var propNeighborhoodState by remember { mutableStateOf("") }
    var propPriceState by remember { mutableStateOf("") }
    var propTypeState by remember { mutableStateOf("rent") } // rent, sale
    var propDescState by remember { mutableStateOf("") }

    // Filtered lists
    val filteredStores = remember(stores, searchQuery) {
        stores.filter {
            it.sectionId == "stores" && (searchQuery.isBlank() || 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery))
        }.sortedByDescending { it.createdAt }
    }

    val filteredProperties = remember(properties, searchQuery) {
        properties.filter {
            searchQuery.isBlank() || 
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
        }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedSection == "المتاجر") "🏪 إدارة المتاجر والأسواق" else "🏢 إدارة العقارات والأملاك", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedSection == "المتاجر") {
                            editingStore = null
                            storeNameState = ""
                            storePhoneState = ""
                            storeCityState = ""
                            storeNeighborhoodState = ""
                            storeHoursState = "9:00 AM - 10:00 PM"
                            storeDescState = ""
                            showStoreDialog = true
                        } else {
                            editingProperty = null
                            propTitleState = ""
                            propPhoneState = ""
                            propCityState = ""
                            propNeighborhoodState = ""
                            propPriceState = ""
                            propTypeState = "rent"
                            propDescState = ""
                            showPropertyDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة جديد", tint = themeColors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (selectedSection == "المتاجر") "بحث باسم المتجر أو رقم الهاتف..." else "بحث باسم العقار أو رقم الهاتف...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )

            AdminFilterChips(
                categories = sections,
                selectedCategory = selectedSection,
                onSelectCategory = { selectedSection = it },
                themeColors = themeColors
            )

            if (selectedSection == "المتاجر") {
                if (filteredStores.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد متاجر تطابق البحث", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredStores, key = { it.id }) { store ->
                            AdminEntityCard(
                                title = store.name,
                                subtitle = "📱 ${store.phone} • 📍 ${store.cityId} - ${store.localNeighborhood}",
                                details = "ساعات العمل: ${store.workingHours}",
                                statusText = if (store.isBlocked) "محظور" else "نشط",
                                statusColor = if (store.isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                                isBlocked = store.isBlocked,
                                themeColors = themeColors,
                                actions = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.toggleStoreBlocked(store.id, !store.isBlocked)
                                                scope.launch { snackbarHostState.showSnackbar(if (store.isBlocked) "تم إلغاء حظر المتجر" else "تم حظر المتجر") }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (store.isBlocked) "فك الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                editingStore = store
                                                storeNameState = store.name
                                                storePhoneState = store.phone
                                                storeCityState = store.cityId
                                                storeNeighborhoodState = store.localNeighborhood
                                                storeHoursState = store.workingHours
                                                storeDescState = store.description
                                                showStoreDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تعديل 📝", fontSize = 10.5.sp, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteStore(store.id)
                                                scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف المتجر") }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (filteredProperties.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد عقارات تطابق البحث", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProperties, key = { it.id }) { prop ->
                            AdminEntityCard(
                                title = prop.title,
                                subtitle = "📱 ${prop.phone} • 📍 ${prop.cityId} - ${prop.localNeighborhood}",
                                details = "السعر: ${prop.price} ${prop.currency} • النوع: ${prop.type}",
                                statusText = if (prop.isBlocked) "محظور" else "متاح",
                                statusColor = if (prop.isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                                isBlocked = prop.isBlocked,
                                themeColors = themeColors,
                                actions = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.togglePropertyBlocked(prop.id, !prop.isBlocked)
                                                scope.launch { snackbarHostState.showSnackbar(if (prop.isBlocked) "تم إلغاء حظر العقار" else "تم حظر العقار") }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (prop.isBlocked) "فك الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                editingProperty = prop
                                                propTitleState = prop.title
                                                propPhoneState = prop.phone
                                                propCityState = prop.cityId
                                                propNeighborhoodState = prop.localNeighborhood
                                                propPriceState = prop.price.toString()
                                                propTypeState = prop.type
                                                propDescState = prop.description
                                                showPropertyDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("تعديل 📝", fontSize = 10.5.sp, color = Color.White)
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteProperty(prop.id)
                                                scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف العقار") }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Store Dialog
    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text(if (editingStore == null) "🏪 إضافة متجر جديد" else "📝 تعديل بيانات المتجر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = storeNameState,
                            onValueChange = { storeNameState = it },
                            label = { Text("اسم المتجر") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = storePhoneState,
                            onValueChange = { storePhoneState = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = storeCityState,
                            onValueChange = { storeCityState = it },
                            label = { Text("المحافظة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = storeNeighborhoodState,
                            onValueChange = { storeNeighborhoodState = it },
                            label = { Text("الحي / المنطقة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = storeHoursState,
                            onValueChange = { storeHoursState = it },
                            label = { Text("ساعات العمل") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = storeDescState,
                            onValueChange = { storeDescState = it },
                            label = { Text("الوصف والتفاصيل") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = editingStore ?: StoreEntity()
                        val finalStore = base.copy(
                            name = storeNameState,
                            phone = storePhoneState,
                            cityId = storeCityState,
                            localNeighborhood = storeNeighborhoodState,
                            workingHours = storeHoursState,
                            description = storeDescState,
                            isActive = true,
                            isApproved = true
                        )
                        viewModel.saveStore(finalStore)
                        showStoreDialog = false
                        scope.launch { snackbarHostState.showSnackbar("💾 تم حفظ بيانات المتجر") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStoreDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Add / Edit Property Dialog
    if (showPropertyDialog) {
        AlertDialog(
            onDismissRequest = { showPropertyDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text(if (editingProperty == null) "🏢 إضافة عقار جديد" else "📝 تعديل بيانات العقار", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = propTitleState,
                            onValueChange = { propTitleState = it },
                            label = { Text("عنوان الإعلان / العقار") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = propPhoneState,
                            onValueChange = { propPhoneState = it },
                            label = { Text("رقم الهاتف للتواصل") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = propCityState,
                            onValueChange = { propCityState = it },
                            label = { Text("المحافظة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = propNeighborhoodState,
                            onValueChange = { propNeighborhoodState = it },
                            label = { Text("الحي / المنطقة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = propPriceState,
                            onValueChange = { propPriceState = it },
                            label = { Text("السعر") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("نوع العقار:", color = Color.White, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = propTypeState == "rent", onClick = { propTypeState = "rent" })
                                Text("إيجار", color = Color.White, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = propTypeState == "sale", onClick = { propTypeState = "sale" })
                                Text("بيع", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = propDescState,
                            onValueChange = { propDescState = it },
                            label = { Text("التفاصيل والمواصفات") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val base = editingProperty ?: PropertyEntity()
                        val priceVal = propPriceState.toDoubleOrNull() ?: 0.0
                        val finalProperty = base.copy(
                            title = propTitleState,
                            phone = propPhoneState,
                            cityId = propCityState,
                            localNeighborhood = propNeighborhoodState,
                            price = priceVal,
                            type = propTypeState,
                            description = propDescState,
                            isActive = true,
                            isApproved = true
                        )
                        viewModel.saveProperty(finalProperty)
                        showPropertyDialog = false
                        scope.launch { snackbarHostState.showSnackbar("💾 تم حفظ بيانات العقار") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPropertyDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
