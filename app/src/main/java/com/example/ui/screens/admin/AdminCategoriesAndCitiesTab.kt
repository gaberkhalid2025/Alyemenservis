package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminCategoriesAndCitiesTab(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val categories = state.categories
    val citiesList = state.citiesList

    when (activeSubTab) {
        "CATEGORIES" -> {
            item {
                Text("🗂️ إدارة الأقسام والقطاعات بالمنصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("يمكنك التحكم بأقسام الصيانة أو إضافة وتفعيل قطاعات رئيسية جديدة كلياً (كقسم التوصيل، المتاجر، العقارات):", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { state.categoryManagementMode = "MAINTENANCE" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.categoryManagementMode == "MAINTENANCE") themeColors.accent else Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🛠️ أقسام الصيانة والمهن", fontSize = 10.sp, color = if (state.categoryManagementMode == "MAINTENANCE") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { state.categoryManagementMode = "PLATFORM_SECTIONS" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.categoryManagementMode == "PLATFORM_SECTIONS") themeColors.accent else Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🌐 القطاعات والأنشطة الرئيسية", fontSize = 10.sp, color = if (state.categoryManagementMode == "PLATFORM_SECTIONS") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.categoryManagementMode == "PLATFORM_SECTIONS") {
                item {
                    AdminStoresPropertiesPanel(viewModel = viewModel, themeColors = themeColors)
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إضافة قسم صيانة جديد ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = state.newCatName,
                                onValueChange = { state.newCatName = it },
                                label = { Text("اسم القسم (مثال: سباكة، كهرباء...)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = state.newCatIcon,
                                onValueChange = { state.newCatIcon = it },
                                label = { Text("أيقونة إيموجي مميزة (مثال: 🚰, ⚡)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            var isNewMainCategory by remember { mutableStateOf(true) }
                            var selectedParentCatId by remember { mutableStateOf("") }

                            Text("نوع القسم:", fontSize = 11.sp, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { isNewMainCategory = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isNewMainCategory) themeColors.accent else Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قسم رئيسي 🟢", fontSize = 10.sp, color = if (isNewMainCategory) Color.Black else Color.White)
                                }
                                Button(
                                    onClick = { isNewMainCategory = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (!isNewMainCategory) themeColors.accent else Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قسم فرعي 🟡", fontSize = 10.sp, color = if (!isNewMainCategory) Color.Black else Color.White)
                                }
                            }

                            if (!isNewMainCategory) {
                                val mainCategories = categories.filter { it.isMainCategory || it.parentId.isEmpty() }
                                Text("اختر القسم الرئيسي التابع له:", fontSize = 10.sp, color = themeColors.accent)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    mainCategories.forEach { parent ->
                                        val isSelected = selectedParentCatId == parent.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedParentCatId = parent.id },
                                            label = { Text("${parent.icon} ${parent.name}", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = themeColors.accent,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (state.newCatName.trim().isEmpty() || state.newCatIcon.trim().isEmpty()) {
                                        Toast.makeText(context, "الرجاء تعبئة اسم القسم والأيقونة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.addNewCategory(
                                            nameAr = state.newCatName.trim(),
                                            nameEn = state.newCatName.trim(),
                                            icon = state.newCatIcon.trim(),
                                            description = "",
                                            parentId = if (!isNewMainCategory) selectedParentCatId else "",
                                            isMainCategory = isNewMainCategory
                                        )
                                        state.newCatName = ""
                                        state.newCatIcon = ""
                                        selectedParentCatId = ""
                                        Toast.makeText(context, "تمت إضافة قسم الصيانة بنجاح 🗂️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة القسم وتفعيله فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(categories, key = { it.id }) { cat ->
                    val index = categories.indexOf(cat)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(cat.icon, fontSize = 20.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (cat.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "مثبت",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { viewModel.togglePinCategory(cat.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "تثبيت القسم",
                                        tint = if (cat.isPinned) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(onClick = {
                                    state.showEditCategoryObj = cat
                                    state.editCatName = cat.name
                                    state.editCatIcon = cat.icon
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل القسم",
                                        tint = themeColors.accent
                                    )
                                }
                                IconButton(onClick = {
                                    state.showMergeCategoryObj = cat
                                    state.selectedTargetCategoryIdForMerge = categories.firstOrNull { it.id != cat.id }?.id ?: ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "دمج القسم",
                                        tint = Color.Cyan
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val list = categories.toMutableList()
                                        if (index > 0) {
                                            list.removeAt(index)
                                            list.add(index - 1, cat)
                                            viewModel.reorderCategories(list)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "ترتيب للأعلى",
                                        tint = if (index > 0) themeColors.accent else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val list = categories.toMutableList()
                                        if (index < list.size - 1) {
                                            list.removeAt(index)
                                            list.add(index + 1, cat)
                                            viewModel.reorderCategories(list)
                                        }
                                    },
                                    enabled = index < categories.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "ترتيب للأسفل",
                                        tint = if (index < categories.size - 1) themeColors.accent else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = { state.showDeleteCategoryConfirmId = cat.id }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف القسم", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }

        "CITIES" -> {
            item {
                Text("🗺️ إدارة محافظات ومدن الجمهورية اليمنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إضافة المحافظات والمدن المستهدفة بالخدمة وتصفح المضاف حالياً بالمنصة:", fontSize = 11.sp, color = themeColors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافة مدينة/محافظة يمنية جديدة ➕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = state.newCityArName,
                            onValueChange = { state.newCityArName = it },
                            label = { Text("الاسم باللغة العربية (مثال: صنعاء، عدن...)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = state.newCityEnName,
                            onValueChange = { state.newCityEnName = it },
                            label = { Text("الاسم باللغة الإنجليزية (مثال: Sana'a, Aden...)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = state.newCityIcon,
                            onValueChange = { state.newCityIcon = it },
                            label = { Text("أيقونة/إيموجي رمزية (مثال: 🏰, 🏖️, 📍)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )

                        Button(
                            onClick = {
                                if (state.newCityArName.trim().isEmpty() || state.newCityEnName.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء ملء الاسم العربي والإنجليزي للمحافظة", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addNewCity(
                                        nameAr = state.newCityArName.trim(),
                                        nameEn = state.newCityEnName.trim(),
                                        icon = state.newCityIcon.trim().ifEmpty { "📍" }
                                    )
                                    state.newCityArName = ""
                                    state.newCityEnName = ""
                                    state.newCityIcon = "📍"
                                    Toast.makeText(context, "تمت إضافة المحافظة بنجاح 🗺️", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تأكيد إضافة المحافظة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(citiesList, key = { city -> city.id }) { city ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                            IconButton(onClick = { state.showEditCityObj = city }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل المحافظة", tint = themeColors.accent)
                            }
                            IconButton(onClick = {
                                viewModel.removeCity(city.id)
                                Toast.makeText(context, "تم حذف المحافظة بنجاح", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف المحافظة", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
