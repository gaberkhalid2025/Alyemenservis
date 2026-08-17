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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    modifier: Modifier = Modifier,
    onSaveRequested: (() -> Unit)? = null
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تفويض الصلاحيات (538 صلاحية كاملة)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColors.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedPermissions.size} / 538 مفعّلة",
                        color = themeColors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Quick Presets
            Text("قوالب سريعة للتعيين الفوري:", color = themeColors.textSecondary, fontSize = 10.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PresetChip(title = "👑 مدير كامل (538)", isSelected = selectedPermissions.size >= 538, onClick = {
                    onPermissionsChanged(AdminPermissionsRegistry.allPermissions.map { it.key })
                })
                PresetChip(title = "🔍 مدقق ومراقب أمني", isSelected = false, onClick = {
                    val keys = AdminPermissionsRegistry.allPermissions
                        .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM || it.category == PermissionCategory.COMPLAINTS || it.category == PermissionCategory.BLOCKED }
                        .map { it.key }
                    onPermissionsChanged(keys)
                })
                PresetChip(title = "📞 دعم فني ومحادثات", isSelected = false, onClick = {
                    val keys = AdminPermissionsRegistry.allPermissions
                        .filter { it.category == PermissionCategory.CHATS || it.category == PermissionCategory.NOTIFICATIONS || it.category == PermissionCategory.BOOKINGS || it.category == PermissionCategory.REVIEWS }
                        .map { it.key }
                    onPermissionsChanged(keys)
                })
                PresetChip(title = "🏬 مراكز ومحلات وتجارة", isSelected = false, onClick = {
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
                label = { Text("بحث في الصلاحيات (الاسم، الوصف، الرمز)...", fontSize = 11.sp) },
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(category.iconEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${category.arabicTitle} (${activeInCat}/${category.expectedCount})",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
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
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isAllSelectedInCat) "إلغاء القسم" else "تحديد القسم",
                                                color = if (isAllSelectedInCat) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }

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
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Text(
                                                            text = perm.level.arabicTitle,
                                                            color = when(perm.level) {
                                                                PermissionLevel.BASIC -> Color(0xFF10B981)
                                                                PermissionLevel.MEDIUM -> Color(0xFF3B82F6)
                                                                PermissionLevel.ADVANCED -> Color(0xFFF59E0B)
                                                                PermissionLevel.SENSITIVE -> Color(0xFFEF4444)
                                                            },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = perm.description,
                                                        color = Color.LightGray,
                                                        fontSize = 9.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "الرمز: ${perm.key}",
                                                        color = Color.Gray,
                                                        fontSize = 8.sp,
                                                        maxLines = 1
                                                    )
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

            // Save button if provided
            if (onSaveRequested != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onSaveRequested,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💾 حفظ ومزامنة الصلاحيات (${selectedPermissions.size} مفعّلة)",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.LightGray,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
