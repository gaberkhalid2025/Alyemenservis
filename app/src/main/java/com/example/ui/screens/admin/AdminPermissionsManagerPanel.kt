package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminPermissionsRegistry
import com.example.data.models.PermissionCategory
import com.example.data.models.PermissionLevel
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPermissionsSelectorView(
    themeColors: VisualThemePalette,
    selectedPermissions: List<String>,
    onPermissionsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf<PermissionLevel?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<PermissionCategory?>(null) }
    var expandedCategories by remember { mutableStateOf<Set<PermissionCategory>>(emptySet()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تفويض الصلاحيات (320 صلاحية كاملة)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColors.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedPermissions.size} / 320 مفعّلة",
                        color = themeColors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Presets
            Text("قوالب سريعة للتعيين:", color = themeColors.textSecondary, fontSize = 10.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PresetChip(title = "👑 مدير كامل (320)", isSelected = selectedPermissions.size == 320, onClick = {
                    onPermissionsChanged(AdminPermissionsRegistry.allPermissions.map { it.key })
                })
                PresetChip(title = "🔍 مدقق ومراقب (أساسي+متوسط)", isSelected = false, onClick = {
                    val keys = AdminPermissionsRegistry.allPermissions
                        .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM }
                        .map { it.key }
                    onPermissionsChanged(keys)
                })
                PresetChip(title = "📞 دعم فني (شات+إشعارات+حجوزات)", isSelected = false, onClick = {
                    val keys = AdminPermissionsRegistry.allPermissions
                        .filter { it.category == PermissionCategory.CHAT || it.category == PermissionCategory.NOTIFICATIONS || it.category == PermissionCategory.BOOKING_FORMS || it.category == PermissionCategory.QUICK_SERVICE }
                        .map { it.key }
                    onPermissionsChanged(keys)
                })
                PresetChip(title = "🏬 مراكز ومحلات ومطاعم", isSelected = false, onClick = {
                    val keys = AdminPermissionsRegistry.allPermissions
                        .filter { it.category == PermissionCategory.STORES || it.category == PermissionCategory.RESTAURANTS || it.category == PermissionCategory.MEDICAL || it.category == PermissionCategory.PROPERTIES || it.category == PermissionCategory.JOBS }
                        .map { it.key }
                    onPermissionsChanged(keys)
                })
                PresetChip(title = "❌ إلغاء تحديد الكل", isSelected = selectedPermissions.isEmpty(), onClick = {
                    onPermissionsChanged(emptyList())
                })
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث في الصلاحيات (الاسم، الوصف، الفئة)...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )

            // Level filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LevelFilterBadge("الكل", isSelected = selectedLevelFilter == null, onClick = { selectedLevelFilter = null })
                LevelFilterBadge("أساسي 🟢", isSelected = selectedLevelFilter == PermissionLevel.BASIC, onClick = { selectedLevelFilter = if (selectedLevelFilter == PermissionLevel.BASIC) null else PermissionLevel.BASIC })
                LevelFilterBadge("متوسط 🔵", isSelected = selectedLevelFilter == PermissionLevel.MEDIUM, onClick = { selectedLevelFilter = if (selectedLevelFilter == PermissionLevel.MEDIUM) null else PermissionLevel.MEDIUM })
                LevelFilterBadge("متقدم 🟡", isSelected = selectedLevelFilter == PermissionLevel.ADVANCED, onClick = { selectedLevelFilter = if (selectedLevelFilter == PermissionLevel.ADVANCED) null else PermissionLevel.ADVANCED })
                LevelFilterBadge("حساس 🔴", isSelected = selectedLevelFilter == PermissionLevel.SENSITIVE, onClick = { selectedLevelFilter = if (selectedLevelFilter == PermissionLevel.SENSITIVE) null else PermissionLevel.SENSITIVE })
            }

            // Categories list / items
            val displayedCategories = PermissionCategory.values().filter { cat ->
                selectedCategoryFilter == null || selectedCategoryFilter == cat
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                displayedCategories.forEach { category ->
                    var categoryPerms = AdminPermissionsRegistry.getByCategory(category)
                    
                    if (selectedLevelFilter != null) {
                        categoryPerms = categoryPerms.filter { it.level == selectedLevelFilter }
                    }
                    if (searchQuery.isNotBlank()) {
                        val clean = searchQuery.trim().lowercase()
                        categoryPerms = categoryPerms.filter {
                            it.name.lowercase().contains(clean) ||
                            it.key.lowercase().contains(clean) ||
                            it.description.lowercase().contains(clean) ||
                            it.targetGroup.lowercase().contains(clean)
                        }
                    }

                    if (categoryPerms.isNotEmpty()) {
                        val isExpanded = expandedCategories.contains(category) || searchQuery.isNotBlank()
                        val catKeys = categoryPerms.map { it.key }
                        val activeInCat = catKeys.count { selectedPermissions.contains(it) }
                        val isAllSelectedInCat = activeInCat == catKeys.size

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, if (activeInCat > 0) themeColors.accent.copy(alpha = 0.5f) else Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Category Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategories = if (expandedCategories.contains(category)) {
                                                expandedCategories - category
                                            } else {
                                                expandedCategories + category
                                            }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.iconEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${category.arabicTitle} (${activeInCat}/${category.count})",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Toggle all in category
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isAllSelectedInCat) themeColors.accent else Color.DarkGray)
                                                .clickable {
                                                    val newSet = selectedPermissions.toMutableSet()
                                                    if (isAllSelectedInCat) {
                                                        newSet.removeAll(catKeys.toSet())
                                                    } else {
                                                        newSet.addAll(catKeys)
                                                    }
                                                    onPermissionsChanged(newSet.toList())
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isAllSelectedInCat) "إلغاء القسم" else "تحديد القسم",
                                                color = if (isAllSelectedInCat) Color.Black else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Expanded items
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        categoryPerms.forEach { perm ->
                                            val isPermSelected = selectedPermissions.contains(perm.key)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isPermSelected) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .clickable {
                                                        val newSet = selectedPermissions.toMutableSet()
                                                        if (isPermSelected) {
                                                            newSet.remove(perm.key)
                                                        } else {
                                                            newSet.add(perm.key)
                                                        }
                                                        onPermissionsChanged(newSet.toList())
                                                    }
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isPermSelected,
                                                    onCheckedChange = { checked ->
                                                        val newSet = selectedPermissions.toMutableSet()
                                                        if (checked) newSet.add(perm.key) else newSet.remove(perm.key)
                                                        onPermissionsChanged(newSet.toList())
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = themeColors.accent,
                                                        uncheckedColor = Color.Gray,
                                                        checkmarkColor = Color.Black
                                                    ),
                                                    modifier = Modifier.size(24.dp)
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = perm.name,
                                                            color = if (isPermSelected) Color.White else Color.LightGray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )

                                                        // Level tag
                                                        val (lvlText, lvlColor) = when (perm.level) {
                                                            PermissionLevel.BASIC -> "أساسي" to Color(0xFF10B981)
                                                            PermissionLevel.MEDIUM -> "متوسط" to Color(0xFF3B82F6)
                                                            PermissionLevel.ADVANCED -> "متقدم" to Color(0xFFF59E0B)
                                                            PermissionLevel.SENSITIVE -> "حساس ⚠️" to Color(0xFFEF4444)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(lvlColor.copy(alpha = 0.2f))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(lvlText, color = lvlColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    Text(
                                                        text = perm.description,
                                                        color = themeColors.textSecondary,
                                                        fontSize = 9.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Row(
                                                        modifier = Modifier.padding(top = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text("🎯 الفئة: ${perm.targetGroup}", color = Color.Gray, fontSize = 8.sp)
                                                        Text("🌐 النطاق: ${perm.scope}", color = Color.Gray, fontSize = 8.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LevelFilterBadge(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF334155) else Color(0xFF0F172A))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
