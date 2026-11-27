package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

internal fun LazyListScope.adminProvidersManagementTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val categories = state.categories
    val activatedProviders = state.activatedProviders

    item {
        Text("🏅 ترقية الفنيين وأعضاء دليل الدليل الشامل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("البحث والتحكم في شارات الفنيين والأوسمة والتحكم المستقل والحذف:", fontSize = 11.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))
    }

    item {
        OutlinedTextField(
            value = state.activeProvidersSearchQuery,
            onValueChange = { state.activeProvidersSearchQuery = it },
            label = { Text("البحث في دليل الفنيين المعتمدين...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeColors.accent) }
        )
    }

    val filteredProviders = activatedProviders.filter {
        it.name.contains(state.activeProvidersSearchQuery, ignoreCase = true) ||
            it.phone.contains(state.activeProvidersSearchQuery, ignoreCase = true) ||
            it.area.contains(state.activeProvidersSearchQuery, ignoreCase = true)
    }

    items(filteredProviders, key = { it.id }) { p ->
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val catName = if (p.categoryId == "other" && p.customCategoryName.isNotEmpty()) p.customCategoryName else (categories.find { it.id == p.categoryId }?.name ?: "خدمات عامة")
                        Text("المهنة: $catName | المنطقة: ${p.area}", fontSize = 11.sp, color = themeColors.textSecondary)
                    }
                    IconButton(
                        onClick = { viewModel.removeProvider(p.id) }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف الفني نهائياً", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isVip,
                                onCheckedChange = { viewModel.pinProvider(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("VIP ذهبي", fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isVerified,
                                onCheckedChange = { viewModel.verifyProviderBadge(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("موثق حساب", fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isRecommended,
                                onCheckedChange = { viewModel.recommendProvider(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("موصى به", fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isChatDisabled,
                                onCheckedChange = { viewModel.setProviderChatDisabled(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إيقاف الدردشة 🔇", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isNotificationsDisabled,
                                onCheckedChange = { viewModel.setProviderNotificationsDisabled(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إيقاف الإشعارات 🔕", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = p.isPaymentRequired,
                                onCheckedChange = { viewModel.setProviderPaymentRequired(p.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("ربط بالدفع 💳", fontSize = 9.sp, color = Color.LightGray, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
