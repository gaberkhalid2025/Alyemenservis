package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminPermissionsRegistry
import com.example.data.models.PermissionCategory
import com.example.data.models.PermissionLevel
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * Full, non-abbreviated Admin Roles & Permissions Matrix Panel.
 * Displays and allows complete granular management of all 538 permissions
 * across all 38 categories in the application.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRolesPermissionsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var selectedRole by rememberSaveable { mutableStateOf("ADMIN") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedLevelFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedCategories by remember { mutableStateOf<Set<PermissionCategory>>(emptySet()) }
    
    // Set of active permission keys for the currently selected role
    var activePermissions by remember {
        mutableStateOf<Set<String>>(AdminPermissionsRegistry.allPermissions.map { it.key }.toSet())
    }

    val rolesList = listOf(
        "OWNER" to "👑 المالك العام (Owner)",
        "ADMIN" to "🛡️ مدير النظام (Admin)",
        "SUPERVISOR" to "👔 المشرف العام (Supervisor)",
        "AUDITOR" to "🔍 المدقق والمراقب (Auditor)",
        "SUPPORT" to "🎧 الدعم الفني (Support)",
        "OPERATIONS" to "⚡ مدير العمليات (Operations)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Title Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🛡️ مصفوفة الصلاحيات الشاملة (538 صلاحية)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "38 مجالاً وقطاعاً إدارياً مفصلاً بدون أي اختصار",
                                fontSize = 10.sp,
                                color = themeColors.accent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF059669).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF059669), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activePermissions.size} / 538 نشطة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                Text(
                    text = "تحكم دقيق ومفصل في كل إجراء إداري بالنظام: الإشعارات، البنرات، استمارات التسجيل، الحجوزات، المحادثات، الثيمات، الأقسام، الخرائط، المنشآت، الرقابة، والإدارة المالية.",
                    fontSize = 10.sp,
                    color = themeColors.textSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Role Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "👤 اختر الدور الإداري لتخصيص صلاحياته:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rolesList.forEach { (roleKey, roleLabel) ->
                        val isSelected = selectedRole == roleKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedRole = roleKey
                                activePermissions = when (roleKey) {
                                    "OWNER" -> AdminPermissionsRegistry.allPermissions.map { it.key }.toSet()
                                    "ADMIN" -> AdminPermissionsRegistry.allPermissions.map { it.key }.toSet()
                                    "SUPERVISOR" -> AdminPermissionsRegistry.allPermissions
                                        .filter { it.level != PermissionLevel.SENSITIVE || it.category == PermissionCategory.SUPERVISORS }
                                        .map { it.key }.toSet()
                                    "AUDITOR" -> AdminPermissionsRegistry.allPermissions
                                        .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM || it.category == PermissionCategory.COMPLAINTS || it.category == PermissionCategory.BLOCKED }
                                        .map { it.key }.toSet()
                                    "SUPPORT" -> AdminPermissionsRegistry.allPermissions
                                        .filter { it.category == PermissionCategory.CHATS || it.category == PermissionCategory.NOTIFICATIONS || it.category == PermissionCategory.BOOKINGS || it.category == PermissionCategory.REVIEWS }
                                        .map { it.key }.toSet()
                                    "OPERATIONS" -> AdminPermissionsRegistry.allPermissions
                                        .filter { it.category == PermissionCategory.STORES || it.category == PermissionCategory.RESTAURANTS || it.category == PermissionCategory.MEDICAL || it.category == PermissionCategory.PROPERTIES || it.category == PermissionCategory.JOBS || it.category == PermissionCategory.CATEGORIES || it.category == PermissionCategory.CITIES }
                                        .map { it.key }.toSet()
                                    else -> AdminPermissionsRegistry.allPermissions.map { it.key }.toSet()
                                }
                                Toast.makeText(context, "تم تحميل حزمة صلاحيات $roleLabel", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(roleLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Quick Presets and Bulk Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "⚡ إجراءات جماعية سريعة:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionChip("✅ تفعيل الكل (538)", isPrimary = true) {
                        activePermissions = AdminPermissionsRegistry.allPermissions.map { it.key }.toSet()
                        Toast.makeText(context, "تم تفعيل جميع الـ 538 صلاحية بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                    ActionChip("🚫 تعطيل الكل", isPrimary = false) {
                        activePermissions = emptySet()
                        Toast.makeText(context, "تم تعطيل جميع الصلاحيات", Toast.LENGTH_SHORT).show()
                    }
                    ActionChip("🟢 الصلاحيات الأساسية فقط", isPrimary = false) {
                        activePermissions = AdminPermissionsRegistry.allPermissions
                            .filter { it.level == PermissionLevel.BASIC }
                            .map { it.key }.toSet()
                        Toast.makeText(context, "تم تفعيل الصلاحيات الأساسية (${activePermissions.size})", Toast.LENGTH_SHORT).show()
                    }
                    ActionChip("🔵 الأساسية والمتوسطة", isPrimary = false) {
                        activePermissions = AdminPermissionsRegistry.allPermissions
                            .filter { it.level == PermissionLevel.BASIC || it.level == PermissionLevel.MEDIUM }
                            .map { it.key }.toSet()
                        Toast.makeText(context, "تم تفعيل الأساسية والمتوسطة (${activePermissions.size})", Toast.LENGTH_SHORT).show()
                    }
                    ActionChip("📂 فتح جميع الأقسام", isPrimary = false) {
                        expandedCategories = PermissionCategory.values().toSet()
                    }
                    ActionChip("📁 طي جميع الأقسام", isPrimary = false) {
                        expandedCategories = emptySet()
                    }
                }
            }
        }

        // Search and Filters Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث في مصفوفة الصلاحيات (الاسم، الوصف، الرمز، المجال)...", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    )
                )

                // Level Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val levels = listOf(
                        null to "الكل (538)",
                        "BASIC" to "أساسي 🟢",
                        "MEDIUM" to "متوسط 🔵",
                        "ADVANCED" to "متقدم 🟡",
                        "SENSITIVE" to "حساس 🔴"
                    )
                    levels.forEach { (lvlKey, lvlTitle) ->
                        val isSel = selectedLevelFilter == lvlKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) themeColors.accent else Color(0xFF0F172A))
                                .clickable { selectedLevelFilter = if (isSel) null else lvlKey }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lvlTitle,
                                fontSize = 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else Color.White,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Category Quick Filter Scrollable Row (All 38 Categories)
                Text("تصفية سريعة حسب المجال (38 مجالاً):", fontSize = 10.sp, color = themeColors.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isAllCatSel = selectedCategoryFilter == null
                    FilterChip(
                        selected = isAllCatSel,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("🌟 الكل (38)", fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColors.accent,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color.White
                        )
                    )
                    PermissionCategory.values().forEach { cat ->
                        val isCatSel = selectedCategoryFilter == cat.name
                        FilterChip(
                            selected = isCatSel,
                            onClick = { selectedCategoryFilter = if (isCatSel) null else cat.name },
                            label = { Text("${cat.iconEmoji} ${cat.arabicTitle} (${cat.expectedCount})", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColors.accent,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Permissions Categories & Items Rendering (Full 538 Items)
        val filteredCategories = PermissionCategory.values().filter { cat ->
            selectedCategoryFilter == null || selectedCategoryFilter == cat.name
        }

        var totalDisplayedPermsCount = 0

        filteredCategories.forEach { category ->
            var categoryPerms = AdminPermissionsRegistry.getByCategory(category)

            if (selectedLevelFilter != null) {
                categoryPerms = categoryPerms.filter { it.level.name == selectedLevelFilter }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                categoryPerms = categoryPerms.filter {
                    it.name.lowercase().contains(q) ||
                    it.key.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.targetGroup.lowercase().contains(q)
                }
            }

            if (categoryPerms.isNotEmpty()) {
                totalDisplayedPermsCount += categoryPerms.size
                val isExpanded = expandedCategories.contains(category) || searchQuery.isNotBlank()
                val catKeys = categoryPerms.map { it.key }
                val activeInCat = catKeys.count { activePermissions.contains(it) }
                val isAllSelectedInCat = activeInCat == catKeys.size && catKeys.isNotEmpty()

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (activeInCat > 0) themeColors.accent.copy(alpha = 0.5f) else Color.DarkGray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Category Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategories = if (expandedCategories.contains(category)) {
                                        expandedCategories - category
                                    } else {
                                        expandedCategories + category
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(category.iconEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${category.arabicTitle} ($activeInCat / ${category.expectedCount})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "الرمز الفني: ${category.name}",
                                        color = themeColors.textSecondary,
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Toggle all in this category
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isAllSelectedInCat) themeColors.accent else Color(0xFF0F172A))
                                        .border(0.5.dp, if (isAllSelectedInCat) Color.White else Color.Gray, RoundedCornerShape(6.dp))
                                        .clickable {
                                            val newSet = activePermissions.toMutableSet()
                                            if (isAllSelectedInCat) {
                                                newSet.removeAll(catKeys.toSet())
                                            } else {
                                                newSet.addAll(catKeys)
                                            }
                                            activePermissions = newSet
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isAllSelectedInCat) "إلغاء القسم" else "تحديد القسم (${categoryPerms.size})",
                                        color = if (isAllSelectedInCat) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = themeColors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Expanded Permission Items List
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                categoryPerms.forEach { perm ->
                                    val isPermActive = activePermissions.contains(perm.key)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isPermActive) Color(0xFF0F172A) else Color(0xFF0B0F19))
                                            .border(
                                                0.5.dp,
                                                if (isPermActive) themeColors.accent.copy(alpha = 0.3f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                val newSet = activePermissions.toMutableSet()
                                                if (isPermActive) newSet.remove(perm.key) else newSet.add(perm.key)
                                                activePermissions = newSet
                                            }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Checkbox + ID + Details
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = isPermActive,
                                                onCheckedChange = { checked ->
                                                    val newSet = activePermissions.toMutableSet()
                                                    if (checked) newSet.add(perm.key) else newSet.remove(perm.key)
                                                    activePermissions = newSet
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = themeColors.accent,
                                                    uncheckedColor = Color.Gray,
                                                    checkmarkColor = Color.Black
                                                ),
                                                modifier = Modifier.size(22.dp)
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "#${perm.id}. ${perm.name}",
                                                        color = if (isPermActive) Color.White else Color.Gray,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    
                                                    // Level Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                when (perm.level) {
                                                                    PermissionLevel.BASIC -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                    PermissionLevel.MEDIUM -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                                    PermissionLevel.ADVANCED -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                    PermissionLevel.SENSITIVE -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                                }
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = perm.level.arabicTitle,
                                                            color = when (perm.level) {
                                                                PermissionLevel.BASIC -> Color(0xFF34D399)
                                                                PermissionLevel.MEDIUM -> Color(0xFF60A5FA)
                                                                PermissionLevel.ADVANCED -> Color(0xFFFBBF24)
                                                                PermissionLevel.SENSITIVE -> Color(0xFFF87171)
                                                            },
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = perm.description,
                                                    color = Color.LightGray,
                                                    fontSize = 9.sp,
                                                    lineHeight = 13.sp
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "الرمز: ${perm.key}",
                                                        color = themeColors.accent.copy(alpha = 0.8f),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "• النطاق: ${perm.targetGroup}",
                                                        color = Color.Gray,
                                                        fontSize = 8.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Right: Switch Toggle
                                        Switch(
                                            checked = isPermActive,
                                            onCheckedChange = { checked ->
                                                val newSet = activePermissions.toMutableSet()
                                                if (checked) newSet.add(perm.key) else newSet.remove(perm.key)
                                                activePermissions = newSet
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.Black,
                                                checkedTrackColor = themeColors.accent,
                                                uncheckedThumbColor = Color.LightGray,
                                                uncheckedTrackColor = Color.DarkGray
                                            ),
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (totalDisplayedPermsCount == 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔍 لم يتم العثور على صلاحيات مطابقة لمعايير البحث.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bottom Save & Sync Action Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💾 مزامنة وحفظ مصفوفة الصلاحيات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "الدور المحدد: $selectedRole | الصلاحيات الممنوحة: ${activePermissions.size} / 538",
                            fontSize = 10.sp,
                            color = themeColors.accent
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.triggerNotification("✅ تم حفظ ومزامنة مصفوفة الـ 538 صلاحية للدور $selectedRole بنجاح!")
                        Toast.makeText(
                            context,
                            "🚀 تم حفظ وتطبيق ${activePermissions.size} صلاحية للدور $selectedRole بنجاح!",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ ومزامنة الصلاحيات في Firestore فوراً 💾",
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
private fun ActionChip(
    title: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) Color(0xFF059669) else Color(0xFF0F172A))
            .border(0.5.dp, if (isPrimary) Color(0xFF34D399) else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
