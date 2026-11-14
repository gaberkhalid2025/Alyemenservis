package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PropertyEntity
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.utils.VisualThemePalette

/**
 * 🏪 Admin Panel: Stores & Properties Management (إدارة المتاجر والعقارات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoresPropertiesPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier,
    initialSector: String = "STORES"
) {
    val context = LocalContext.current

    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    var selectedSection by remember(initialSector) { mutableStateOf(if (initialSector == "PROPERTIES") "العقارات" else "المتاجر") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showAddPropertyDialog by remember { mutableStateOf(false) }

    val filteredStores = remember(stores, searchQuery, selectedFilter) {
        stores.filter { store ->
            val matchFilter = when (selectedFilter) {
                "مميز VIP" -> store.isVip
                "محظور" -> store.isBlocked
                "نشط" -> !store.isBlocked && store.isActive
                else -> true
            }
            val matchSearch = searchQuery.isBlank() ||
                    store.name.contains(searchQuery, ignoreCase = true) ||
                    store.phone.contains(searchQuery) ||
                    store.cityId.contains(searchQuery, ignoreCase = true) ||
                    store.description.contains(searchQuery, ignoreCase = true)
            matchFilter && matchSearch
        }
    }

    val filteredProperties = remember(properties, searchQuery, selectedFilter) {
        properties.filter { prop ->
            val matchFilter = when (selectedFilter) {
                "مميز VIP" -> prop.isVip
                "محظور" -> prop.isBlocked
                "للإيجار" -> prop.type.contains("إيجار") || prop.type.contains("ايجار")
                "للبيع" -> prop.type.contains("بيع")
                else -> true
            }
            val matchSearch = searchQuery.isBlank() ||
                    prop.title.contains(searchQuery, ignoreCase = true) ||
                    prop.phone.contains(searchQuery) ||
                    prop.cityId.contains(searchQuery, ignoreCase = true) ||
                    prop.type.contains(searchQuery, ignoreCase = true)
            matchFilter && matchSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedSection == "المتاجر") "🏪 إدارة المحلات والمراكز التجارية (${stores.size})" else "🏠 إدارة العقارات والأراضي (${properties.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = themeColors.textPrimary
                    )

                    Button(
                        onClick = {
                            if (selectedSection == "المتاجر") showAddStoreDialog = true
                            else showAddPropertyDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedSection == "المتاجر") "إضافة محل ➕" else "إضافة عقار ➕",
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Section Selector Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedSection = "المتاجر"
                            selectedFilter = "الكل"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == "المتاجر") themeColors.accent else themeColors.textPrimary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(
                            "🏪 المحلات التجارية (${stores.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSection == "المتاجر") Color.Black else themeColors.textPrimary
                        )
                    }

                    Button(
                        onClick = {
                            selectedSection = "العقارات"
                            selectedFilter = "الكل"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == "العقارات") themeColors.accent else themeColors.textPrimary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(
                            "🏠 العقارات والأراضي (${properties.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSection == "العقارات") Color.Black else themeColors.textPrimary
                        )
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (selectedSection == "المتاجر") "بحث باسم المتجر، المدينة، الهاتف، النشاط..." else "بحث بنوع العقار، المدينة، الهاتف، السعر...", fontSize = 11.5.sp, color = themeColors.textSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = themeColors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = themeColors.textSecondary.copy(alpha = 0.25f)
                    )
                )

                // Filter Chips
                val currentFilters = if (selectedSection == "المتاجر") listOf("الكل", "مميز VIP", "نشط", "محظور") else listOf("الكل", "مميز VIP", "للإيجار", "للبيع", "محظور")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currentFilters, key = { it }) { f ->
                        val isSelected = selectedFilter == f
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = f },
                            label = { Text(f, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black,
                                containerColor = themeColors.textPrimary.copy(alpha = 0.05f),
                                labelColor = themeColors.textPrimary
                            )
                        )
                    }
                }
            }
        }

        // Stores Section
        if (selectedSection == "المتاجر") {
            if (filteredStores.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد محلات تجارية مطابقة 🏪", color = themeColors.textSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStores, key = { it.id }) { store ->
                        AdminEntityCard(
                            title = store.name.ifBlank { "متجر تجاري" },
                            subtitle = "📱 ${store.phone} • 📍 ${store.cityId} - ${store.localNeighborhood.ifBlank { "المركز" }}",
                            details = "🏢 النشاط: ${store.description.ifBlank { "تجارة وخدمات" }} • ساعات العمل: ${store.workingHours.ifBlank { "يومياً" }}",
                            imageUrl = store.logoImage.ifBlank { store.coverImage }.ifBlank { null },
                            statusText = if (store.isBlocked) "🚫 محظور" else if (store.isVip) "⭐ VIP" else "نشط ✅",
                            statusColor = if (store.isBlocked) Color(0xFFEF4444) else if (store.isVip) Color(0xFFF59E0B) else Color(0xFF10B981),
                            isVip = store.isVip,
                            isBlocked = store.isBlocked,
                            themeColors = themeColors,
                            actions = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.adminViewModel.toggleStoreBlocked(store.id, !store.isBlocked)
                                            Toast.makeText(context, if (store.isBlocked) "تم إلغاء حظر المتجر ✅" else "تم حظر المتجر 🚫", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            if (store.isBlocked) "فك الحظر ✅" else "حظر 🚫",
                                            fontSize = 10.5.sp,
                                            color = if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)
                                        )
                                    }

                                    if (store.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.adminViewModel.deleteStore(store.id)
                                            Toast.makeText(context, "🗑️ تم حذف المتجر", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
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
            // Properties Section
            if (filteredProperties.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد عقارات أو أراضي مطابقة 🏠", color = themeColors.textSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProperties, key = { it.id }) { prop ->
                        AdminEntityCard(
                            title = prop.title.ifBlank { "عقار معروض" },
                            subtitle = "📱 ${prop.phone} • 📍 ${prop.cityId} - ${prop.localNeighborhood.ifBlank { "المنطقة" }}",
                            details = "💰 السعر: ${prop.price} ${prop.currency} • 🏷️ النوع: ${prop.type}",
                            imageUrl = prop.images.firstOrNull()?.takeIf { it.isNotBlank() },
                            statusText = if (prop.isBlocked) "🚫 محظور" else if (prop.isVip) "⭐ VIP" else "متاح ✅",
                            statusColor = if (prop.isBlocked) Color(0xFFEF4444) else if (prop.isVip) Color(0xFFF59E0B) else Color(0xFF10B981),
                            isVip = prop.isVip,
                            isBlocked = prop.isBlocked,
                            themeColors = themeColors,
                            actions = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.adminViewModel.togglePropertyBlocked(prop.id, !prop.isBlocked)
                                            Toast.makeText(context, if (prop.isBlocked) "تم فك الحظر عن العقار ✅" else "تم حظر العقار 🚫", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            if (prop.isBlocked) "فك الحظر ✅" else "حظر 🚫",
                                            fontSize = 10.5.sp,
                                            color = if (prop.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)
                                        )
                                    }

                                    if (prop.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prop.phone}"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.adminViewModel.deleteProperty(prop.id)
                                            Toast.makeText(context, "🗑️ تم حذف العقار", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
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

    // Add Store Dialog
    if (showAddStoreDialog) {
        var sName by remember { mutableStateOf("") }
        var sPhone by remember { mutableStateOf("") }
        var sCity by remember { mutableStateOf("صنعاء") }
        var sCategory by remember { mutableStateOf("تجاري ومحلات") }
        var sAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("➕ إضافة متجر أو مركز تجاري", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم المتجر / المركز") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = sPhone,
                        onValueChange = { sPhone = it },
                        label = { Text("رقم الهاتف والتواصل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = sCity, onValueChange = { sCity = it }, label = { Text("المدينة / المحافظة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sAddress, onValueChange = { sAddress = it }, label = { Text("الحي والشارع") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sCategory, onValueChange = { sCategory = it }, label = { Text("النشاط والتصنيف") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المتجر", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanPhone = sPhone.filter { it.isDigit() }
                        if (sPhone.isNotBlank() && cleanPhone.length < 9) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتف صحيح (9 أرقام على الأقل)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newStore = StoreEntity(
                            id = "store_${System.currentTimeMillis()}",
                            name = sName.trim(),
                            phone = sPhone.trim(),
                            cityId = sCity.trim(),
                            localNeighborhood = sAddress.trim(),
                            sectionId = "stores",
                            categoryId = "stores",
                            description = sCategory.trim(),
                            isApproved = true,
                            isActive = true
                        )
                        viewModel.adminViewModel.saveStore(newStore)
                        showAddStoreDialog = false
                        Toast.makeText(context, "تمت إضافة المتجر بنجاح 🏪", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة وحفظ ➕", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) { Text("إلغاء", color = themeColors.textSecondary) }
            },
            containerColor = themeColors.surface
        )
    }

    // Add Property Dialog
    if (showAddPropertyDialog) {
        var pTitle by remember { mutableStateOf("") }
        var pPhone by remember { mutableStateOf("") }
        var pCity by remember { mutableStateOf("صنعاء") }
        var pPrice by remember { mutableStateOf("") }
        var pType by remember { mutableStateOf("شقة للإيجار") }
        var pDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPropertyDialog = false },
            title = { Text("➕ إضافة عقار أو أرض جديدة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themeColors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pTitle, onValueChange = { pTitle = it }, label = { Text("عنوان العقار (مثال: شقة مفروشة راقية)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = pPhone,
                        onValueChange = { pPhone = it },
                        label = { Text("رقم التواصل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = pCity, onValueChange = { pCity = it }, label = { Text("المدينة / المحافظة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pType, onValueChange = { pType = it }, label = { Text("النوع (شقة، فيلا، أرض، محل، مكتب)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = pPrice,
                        onValueChange = { pPrice = it },
                        label = { Text("السعر أو الإيجار") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("تفاصيل ومواصفات العقار") }, modifier = Modifier.fillMaxWidth().height(70.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pTitle.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة عنوان العقار", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val cleanPhone = pPhone.filter { it.isDigit() }
                        if (pPhone.isNotBlank() && cleanPhone.length < 9) {
                            Toast.makeText(context, "يرجى إدخال رقم هاتف صحيح (9 أرقام على الأقل)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val parsedPrice = if (pPrice.isBlank()) 0.0 else (pPrice.trim().toDoubleOrNull() ?: -1.0)
                        if (pPrice.isNotBlank() && parsedPrice <= 0.0) {
                            Toast.makeText(context, "يرجى إدخال سعر رقمي صالح أكبر من صفر", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newProp = PropertyEntity(
                            id = "prop_${System.currentTimeMillis()}",
                            title = pTitle.trim(),
                            phone = pPhone.trim(),
                            cityId = pCity.trim(),
                            type = pType.trim(),
                            price = parsedPrice,
                            description = pDesc.trim(),
                            isApproved = true,
                            isActive = true
                        )
                        viewModel.adminViewModel.saveProperty(newProp)
                        showAddPropertyDialog = false
                        Toast.makeText(context, "تمت إضافة العقار بنجاح 🏠", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة وحفظ ➕", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPropertyDialog = false }) { Text("إلغاء", color = themeColors.textSecondary) }
            },
            containerColor = themeColors.surface
        )
    }
}
