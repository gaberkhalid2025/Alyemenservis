@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminGoldenIconsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_THEMES")) {
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

    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    var styleInput by remember(settingsState.topNavIconStyle) { mutableStateOf(settingsState.topNavIconStyle) }
    var iconSizeInput by remember(settingsState.navIconSizeDp) { mutableStateOf(settingsState.navIconSizeDp.toFloat()) }
    var fontScaleInput by remember(settingsState.globalFontScale) { mutableStateOf(settingsState.globalFontScale) }
    var homeIconInput by remember(settingsState.topHomeIcon) { mutableStateOf(settingsState.topHomeIcon.ifEmpty { "🏠" }) }
    var mapsIconInput by remember(settingsState.topMapsIcon) { mutableStateOf(settingsState.topMapsIcon.ifEmpty { "🗺️" }) }
    var joinIconInput by remember(settingsState.topJoinIcon) { mutableStateOf(settingsState.topJoinIcon.ifEmpty { "👤" }) }
    var notifIconInput by remember(settingsState.topNotifIcon) { mutableStateOf(settingsState.topNotifIcon.ifEmpty { "🔔" }) }
    var chatsIconInput by remember(settingsState.topChatsIcon) { mutableStateOf(settingsState.topChatsIcon.ifEmpty { "✉️" }) }
    var infoIconInput by remember(settingsState.bottomInfoIcon) { mutableStateOf(settingsState.bottomInfoIcon.ifEmpty { "ℹ️" }) }
    var bookingsIconInput by remember(settingsState.bottomBookingsIcon) { mutableStateOf(settingsState.bottomBookingsIcon.ifEmpty { "📅" }) }
    var langIconInput by remember(settingsState.bottomLangIcon) { mutableStateOf(settingsState.bottomLangIcon.ifEmpty { "🌐" }) }
    var adminIconInput by remember(settingsState.bottomAdminIcon) { mutableStateOf(settingsState.bottomAdminIcon.ifEmpty { "🔒" }) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("👑 التحكم بالأيقونات الذهبية ثلاثية الأبعاد وحجم خط التطبيق", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("تخصيص كامل للأيقونات العلوية والسفلية، وحجم الخط وحجم الأيقونات مع مزامنتها لكل مستخدمي التطبيق فورياً:", fontSize = 11.sp, color = themeColors.textSecondary)

                // 1. Icon Style Selection
                Text("أسلوب ونمط الأيقونات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GOLDEN_3D" to "ذهبي ثلاثي الأبعاد ✨", "METALLIC" to "معدني فاخر 🪙", "MINIMAL" to "خطي بسيطة 🎨").forEach { (style, label) ->
                        FilterChip(
                            selected = styleInput == style,
                            onClick = { styleInput = style },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                // 2. Icon Size Slider
                Text("حجم أيقونات شريط التنقل العلوي والسفلي: ${iconSizeInput.toInt()}dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Slider(
                    value = iconSizeInput,
                    onValueChange = { iconSizeInput = it },
                    valueRange = 18f..40f,
                    steps = 22,
                    colors = SliderDefaults.colors(thumbColor = themeColors.accent, activeTrackColor = themeColors.accent)
                )

                // 3. Global Font Size Scale Slider
                Text("مقياس حجم الخط بالتطبيق كامل: ${(fontScaleInput * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.85f to "85%", 1.0f to "100%", 1.15f to "115%", 1.3f to "130%").forEach { (scale, label) ->
                        FilterChip(
                            selected = Math.abs(fontScaleInput - scale) < 0.05f,
                            onClick = { fontScaleInput = scale },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                Text("تخصيص رموز وأيقونات الشريط العلوي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = homeIconInput, onValueChange = { homeIconInput = it }, label = { Text("الرئيسية") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = mapsIconInput, onValueChange = { mapsIconInput = it }, label = { Text("الخرائط") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = joinIconInput, onValueChange = { joinIconInput = it }, label = { Text("الانضمام") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = notifIconInput, onValueChange = { notifIconInput = it }, label = { Text("الإشعارات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = chatsIconInput, onValueChange = { chatsIconInput = it }, label = { Text("المحادثات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Text("تخصيص رموز وأيقونات الشريط السفلي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = infoIconInput, onValueChange = { infoIconInput = it }, label = { Text("عن التطبيق") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = bookingsIconInput, onValueChange = { bookingsIconInput = it }, label = { Text("الحجوزات") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = langIconInput, onValueChange = { langIconInput = it }, label = { Text("اللغة") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = adminIconInput, onValueChange = { adminIconInput = it }, label = { Text("الإدارة") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Button(
                    onClick = {
                        val updated = settingsState.copy(
                            topNavIconStyle = styleInput,
                            navIconSizeDp = iconSizeInput.toInt(),
                            globalFontScale = fontScaleInput,
                            topHomeIcon = homeIconInput.trim(),
                            topMapsIcon = mapsIconInput.trim(),
                            topJoinIcon = joinIconInput.trim(),
                            topNotifIcon = notifIconInput.trim(),
                            topChatsIcon = chatsIconInput.trim(),
                            bottomInfoIcon = infoIconInput.trim(),
                            bottomBookingsIcon = bookingsIconInput.trim(),
                            bottomLangIcon = langIconInput.trim(),
                            bottomAdminIcon = adminIconInput.trim()
                        )
                        viewModel.updateAdminSettings(updated)
                        Toast.makeText(context, "تم حفظ ومزامنة إعدادات الأيقونات والخط فورياً 👑", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ ومزامنة الأيقونات وحجم الخط 👑", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
