package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminColorsPanel(
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

    var editPrimaryHex by remember(settingsState) { mutableStateOf(settingsState.customPrimaryHex) }
    var editSecondaryHex by remember(settingsState) { mutableStateOf(settingsState.customSecondaryHex) }
    var editCardBgHex by remember(settingsState) { mutableStateOf(settingsState.cardBackgroundHex) }
    var editProviderNameHex by remember(settingsState) { mutableStateOf(settingsState.providerNameColorHex) }
    var editLocationHex by remember(settingsState) { mutableStateOf(settingsState.locationColorHex) }
    var editRatingHex by remember(settingsState) { mutableStateOf(settingsState.ratingColorHex) }
    var editVipBadgeHex by remember(settingsState) { mutableStateOf(settingsState.vipBadgeColorHex) }
    var editVerifiedHex by remember(settingsState) { mutableStateOf(settingsState.verifiedBadgeColorHex) }
    var editRecommendedHex by remember(settingsState) { mutableStateOf(settingsState.recommendedBadgeColorHex) }

    var editFontSelected by remember(settingsState) { mutableStateOf(settingsState.activeFontFamily) }

    var editChatIconSize by remember(settingsState.chatSize) { mutableStateOf(settingsState.chatSize.toFloat()) }
    var editChatIconX by remember(settingsState.chatXOffset) { mutableStateOf(settingsState.chatXOffset.toFloat()) }
    var editChatIconY by remember(settingsState.chatYOffset) { mutableStateOf(settingsState.chatYOffset.toFloat()) }

    var editAssistantIconSize by remember(settingsState.assistantSize) { mutableStateOf(settingsState.assistantSize.toFloat()) }
    var editAssistantIconX by remember(settingsState.assistantXOffset) { mutableStateOf(settingsState.assistantXOffset.toFloat()) }
    var editAssistantIconY by remember(settingsState.assistantYOffset) { mutableStateOf(settingsState.assistantYOffset.toFloat()) }

    var requirementItemInput by remember { mutableStateOf("") }
    var isNewRequirementMandatory by remember { mutableStateOf(true) }
    var requirementsListState by remember(settingsState) { mutableStateOf(settingsState.registrationRequirements.split(",").filter { it.isNotBlank() }) }

    var editCoverHeight by remember(settingsState.coverHeight) { mutableStateOf(settingsState.coverHeight.toFloat()) }
    var editAvatarSize by remember(settingsState.avatarSize) { mutableStateOf(settingsState.avatarSize.toFloat()) }
    var editElementSpacing by remember(settingsState.elementSpacing) { mutableStateOf(settingsState.elementSpacing.toFloat()) }
    var editCardPadding by remember(settingsState.cardPadding) { mutableStateOf(settingsState.cardPadding.toFloat()) }

    var editShowVipBadge by remember(settingsState.showVipBadge) { mutableStateOf(settingsState.showVipBadge) }
    var editShowVerifiedBadge by remember(settingsState.showVerifiedBadge) { mutableStateOf(settingsState.showVerifiedBadge) }
    var editShowRecommendedBadge by remember(settingsState.showRecommendedBadge) { mutableStateOf(settingsState.showRecommendedBadge) }

    var editShowCallButton by remember(settingsState.showCallButton) { mutableStateOf(settingsState.showCallButton) }
    var editShowWhatsappButton by remember(settingsState.showWhatsappButton) { mutableStateOf(settingsState.showWhatsappButton) }
    var editShowDetailsButton by remember(settingsState.showDetailsButton) { mutableStateOf(settingsState.showDetailsButton) }
    var editShowBookButton by remember(settingsState.showBookButton) { mutableStateOf(settingsState.showBookButton) }

    var editCallButtonColorHex by remember(settingsState.callButtonColorHex) { mutableStateOf(settingsState.callButtonColorHex) }
    var editWhatsappButtonColorHex by remember(settingsState.whatsappButtonColorHex) { mutableStateOf(settingsState.whatsappButtonColorHex) }
    var editDetailsButtonColorHex by remember(settingsState.detailsButtonColorHex) { mutableStateOf(settingsState.detailsButtonColorHex) }
    var editBookButtonColorHex by remember(settingsState.bookButtonColorHex) { mutableStateOf(settingsState.bookButtonColorHex) }

    var editShowLoyaltyBanner by remember(settingsState.showLoyaltyBanner) { mutableStateOf(settingsState.showLoyaltyBanner) }
    var editMaxWorkPhotos by remember(settingsState.maxWorkPhotos) { mutableStateOf(settingsState.maxWorkPhotos.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🎨 التحكم المتقدم بالألوان ونماذج الشروط والخط والمظهر", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        // Color Pickers
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تخصيص لوحة الألوان الفاخرة للهيئات بالتفصيل (Hex Color):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = editPrimaryHex, onValueChange = { editPrimaryHex = it }, label = { Text("اللون الرئيسي للبرنامج (Primary Color)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editSecondaryHex, onValueChange = { editSecondaryHex = it }, label = { Text("اللون الثانوي للبرنامج (Secondary Color)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editCardBgHex, onValueChange = { editCardBgHex = it }, label = { Text("لون خلفية كروت الفنيين (Card Background Hex)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editProviderNameHex, onValueChange = { editProviderNameHex = it }, label = { Text("لون اسم مقدم الخدمة (Provider Name Color)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editLocationHex, onValueChange = { editLocationHex = it }, label = { Text("لون خط المكان والموقع الجغرافي للشارع") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editRatingHex, onValueChange = { editRatingHex = it }, label = { Text("لون نجمة وأرقام التقاييم والنسب الفنية") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editVipBadgeHex, onValueChange = { editVipBadgeHex = it }, label = { Text("لون شارة VIP الذهبية المحيطة") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editVerifiedHex, onValueChange = { editVerifiedHex = it }, label = { Text("لون الشارة الزرقاء الموثقة للدعم") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = editRecommendedHex, onValueChange = { editRecommendedHex = it }, label = { Text("لون نجمة وشريحة التوصية (Recommended Badge)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            }
        }

        // Fonts
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تخصيص نمط الخطوط العربية بالدليل (RTL typography):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                val fontOptions = listOf("cairo", "amiri", "tahoma", "system")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    fontOptions.forEach { font ->
                        val isSel = editFontSelected == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                .clickable { editFontSelected = font }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(font.uppercase(), fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Floating Bubbles Coordinates
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تعديل أحجام وإحداثيات أيقونات الدردشة العائمة بالدعم:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Text("1. حجم أيقونة شات المساعدة المباشرة: ${editChatIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editChatIconSize, onValueChange = { editChatIconSize = it }, valueRange = 35f..90f)

                Text("• إحداثي الإزاحة الأفقي (X-Offset): ${editChatIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                Slider(value = editChatIconX, onValueChange = { editChatIconX = it }, valueRange = 10f..120f)

                Text("• إحداثي الإزاحة الرأسي (Y-Offset): ${editChatIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                Slider(value = editChatIconY, onValueChange = { editChatIconY = it }, valueRange = 30f..180f)

                Spacer(modifier = Modifier.height(6.dp))

                Text("2. حجم أيقونة المساعد الصوتي الذكي (البوت): ${editAssistantIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editAssistantIconSize, onValueChange = { editAssistantIconSize = it }, valueRange = 35f..90f)

                Text("• إحداثي البوت الأفقي (X-Offset): ${editAssistantIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                Slider(value = editAssistantIconX, onValueChange = { editAssistantIconX = it }, valueRange = 10f..120f)

                Text("• إحداثي البوت الرأسي (Y-Offset): ${editAssistantIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                Slider(value = editAssistantIconY, onValueChange = { editAssistantIconY = it }, valueRange = 30f..180f)
            }
        }

        // Requirements Form Manager
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📋 إدارة شروط ونموذج تسجيل الفنيين بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = requirementItemInput,
                        onValueChange = { requirementItemInput = it },
                        label = { Text("اسم الشرط (مثال: فيش جنائي)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إلزامي؟", fontSize = 9.sp, color = Color.White)
                        Switch(checked = isNewRequirementMandatory, onCheckedChange = { isNewRequirementMandatory = it }, modifier = Modifier.scale(0.8f))
                    }
                    Button(
                        onClick = {
                            if (requirementItemInput.trim().isNotEmpty()) {
                                val suffix = if (isNewRequirementMandatory) "|Mandatory" else "|Optional"
                                requirementsListState = requirementsListState + "${requirementItemInput.trim()}$suffix"
                                requirementItemInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("أضف", color = Color.Black)
                    }
                }

                requirementsListState.forEachIndexed { idx, reqItem ->
                    val parts = reqItem.split("|")
                    val reqName = parts.getOrNull(0) ?: reqItem
                    val isMand = parts.getOrNull(1)?.lowercase() != "optional"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${idx + 1}. $reqName", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (isMand) "إلزامي (مطلوب لإنشاء الحساب) 🔴" else "اختياري (غير معرقل للتسجيل) 🟢", color = if (isMand) Color.Red.copy(alpha = 0.8f) else Color.Green, fontSize = 10.sp)
                        }

                        IconButton(onClick = { requirementsListState = requirementsListState.filterIndexed { pIdx, _ -> pIdx != idx } }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Dimensions and Badges
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📏 تخصيص مقاسات وأبعاد كروت الفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Text("• ارتفاع صورة غلاف الكرت (0 للإخفاء): ${editCoverHeight.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editCoverHeight, onValueChange = { editCoverHeight = it }, valueRange = 0f..250f)

                Text("• حجم الصورة الشخصية (Avatar Size): ${editAvatarSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editAvatarSize, onValueChange = { editAvatarSize = it }, valueRange = 30f..100f)

                Text("• الهامش والتباعد الداخلي للكرت (Padding): ${editCardPadding.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editCardPadding, onValueChange = { editCardPadding = it }, valueRange = 4f..24f)

                Text("• المسافات بين عناصر الكرت (Spacing): ${editElementSpacing.toInt()}dp", fontSize = 11.sp, color = Color.White)
                Slider(value = editElementSpacing, onValueChange = { editElementSpacing = it }, valueRange = 2f..16f)

                Divider(color = Color.White.copy(alpha = 0.1f))

                Text("🛡️ إظهار وإخفاء شارات التميز والتوثيق بالفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🏆 شارة VIP الذهبية والدرع المحيط بالكرت", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowVipBadge, onCheckedChange = { editShowVipBadge = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔵 شارة التوثيق الزرقاء المعتمدة", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowVerifiedBadge, onCheckedChange = { editShowVerifiedBadge = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🟢 شارة نجمة التوصية (موصى به)", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowRecommendedBadge, onCheckedChange = { editShowRecommendedBadge = it })
                }
            }
        }

        // Interactive Buttons
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📞 التحكم بأزرار الاتصال والتواصل في الكروت:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📞 تفعيل زر الاتصال الهاتفي المباشر", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowCallButton, onCheckedChange = { editShowCallButton = it })
                }
                if (editShowCallButton) {
                    OutlinedTextField(value = editCallButtonColorHex, onValueChange = { editCallButtonColorHex = it }, label = { Text("لون زر الاتصال (Hex)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("💬 تفعيل زر المحادثة السريعة واتساب", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowWhatsappButton, onCheckedChange = { editShowWhatsappButton = it })
                }
                if (editShowWhatsappButton) {
                    OutlinedTextField(value = editWhatsappButtonColorHex, onValueChange = { editWhatsappButtonColorHex = it }, label = { Text("لون زر واتساب (Hex)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔍 تفعيل زر عرض التفاصيل والتقييمات", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowDetailsButton, onCheckedChange = { editShowDetailsButton = it })
                }
                if (editShowDetailsButton) {
                    OutlinedTextField(value = editDetailsButtonColorHex, onValueChange = { editDetailsButtonColorHex = it }, label = { Text("لون زر التفاصيل (Hex)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📅 تفعيل زر طلب الحجز المباشر", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowBookButton, onCheckedChange = { editShowBookButton = it })
                }
                if (editShowBookButton) {
                    OutlinedTextField(value = editBookButtonColorHex, onValueChange = { editBookButtonColorHex = it }, label = { Text("لون زر الحجز المباشر (Hex)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎁 تفعيل نقاط الولاء والمشاركة للمساعد الذكي", fontSize = 11.sp, color = Color.White)
                    Switch(checked = editShowLoyaltyBanner, onCheckedChange = { editShowLoyaltyBanner = it })
                }

                Text("📂 أقصى حد لصور سابقة الأعمال التي يرفعها مقدم الخدمة: ${editMaxWorkPhotos.toInt()}", fontSize = 11.sp, color = Color.White)
                Slider(value = editMaxWorkPhotos, onValueChange = { editMaxWorkPhotos = it }, valueRange = 1f..5f, steps = 3)
            }
        }

        // Save Button
        Button(
            onClick = {
                val upToDateSettings = settingsState.copy(
                    customPrimaryHex = editPrimaryHex,
                    customSecondaryHex = editSecondaryHex,
                    cardBackgroundHex = editCardBgHex,
                    providerNameColorHex = editProviderNameHex,
                    locationColorHex = editLocationHex,
                    ratingColorHex = editRatingHex,
                    vipBadgeColorHex = editVipBadgeHex,
                    verifiedBadgeColorHex = editVerifiedHex,
                    recommendedBadgeColorHex = editRecommendedHex,
                    activeFontFamily = editFontSelected,
                    chatSize = editChatIconSize.toInt(),
                    chatXOffset = editChatIconX.toInt(),
                    chatYOffset = editChatIconY.toInt(),
                    assistantSize = editAssistantIconSize.toInt(),
                    assistantXOffset = editAssistantIconX.toInt(),
                    assistantYOffset = editAssistantIconY.toInt(),
                    registrationRequirements = requirementsListState.joinToString(","),
                    coverHeight = editCoverHeight.toInt(),
                    avatarSize = editAvatarSize.toInt(),
                    elementSpacing = editElementSpacing.toInt(),
                    cardPadding = editCardPadding.toInt(),
                    showVipBadge = editShowVipBadge,
                    showVerifiedBadge = editShowVerifiedBadge,
                    showRecommendedBadge = editShowRecommendedBadge,
                    showCallButton = editShowCallButton,
                    showWhatsappButton = editShowWhatsappButton,
                    showDetailsButton = editShowDetailsButton,
                    showBookButton = editShowBookButton,
                    callButtonColorHex = editCallButtonColorHex,
                    whatsappButtonColorHex = editWhatsappButtonColorHex,
                    detailsButtonColorHex = editDetailsButtonColorHex,
                    bookButtonColorHex = editBookButtonColorHex,
                    showLoyaltyBanner = editShowLoyaltyBanner,
                    maxWorkPhotos = editMaxWorkPhotos.toInt()
                )
                viewModel.saveCustomSettingsState(upToDateSettings)
                Toast.makeText(context, "تم حفظ وضبط مظهر الدليل والأزرار والبطاقات بنجاح! 🎉", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("💾 حفظ وحقن جميع تخصيصات المظهر بالدليل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
