package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.utils.VisualThemePalette

fun LazyListScope.adminSectionCustomization(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    when (activeSubTab) {
        "CUSTOM_TABS" -> adminCustomTabsPanel(viewModel, themeColors, state.customTabsListState)
        "GOLDEN_ICONS" -> adminGoldenIconsPanel(viewModel, themeColors)
        "CARD_CUSTOMIZER" -> adminCardCustomizerPanel(viewModel, themeColors)
        "NEW_SECTION_CREATOR" -> adminSectionCreatorPanel(viewModel, themeColors)
        "REG_FORMS_MANAGER" -> adminRegistrationFormsManagerPanel(viewModel, themeColors)
        "COLORS" -> {
            val settingsState = state.settingsState
            val context = state.context

            item {
                Text("🎨 القسم العاشر: التحكم المتقدم والألوان ونماذج الشروط والخط", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تخصيص لوحة الألوان اليمينة الفاخرة للهيئات بالتفصيل (Hex Color):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = state.editPrimaryHex,
                            onValueChange = { state.editPrimaryHex = it },
                            label = { Text("اللون الرئيسي للبرنامج (Primary Color)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editSecondaryHex,
                            onValueChange = { state.editSecondaryHex = it },
                            label = { Text("اللون الثانوي للبرنامج (Secondary Color)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editCardBgHex,
                            onValueChange = { state.editCardBgHex = it },
                            label = { Text("لون خلفية كروت الفنيين (Card Background Hex)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editProviderNameHex,
                            onValueChange = { state.editProviderNameHex = it },
                            label = { Text("لون اسم مقدم الخدمة (Provider Name Color)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editLocationHex,
                            onValueChange = { state.editLocationHex = it },
                            label = { Text("لون خط المكان والموقع الجغرافي للشارع") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editRatingHex,
                            onValueChange = { state.editRatingHex = it },
                            label = { Text("لون نجمة وأرقام التقاييم والنسب الفنية") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editVipBadgeHex,
                            onValueChange = { state.editVipBadgeHex = it },
                            label = { Text("لون شارة VIP الذهبية المحيطة") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editVerifiedHex,
                            onValueChange = { state.editVerifiedHex = it },
                            label = { Text("لون الشارة الزرقاء الموثقة للدعم") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = state.editRecommendedHex,
                            onValueChange = { state.editRecommendedHex = it },
                            label = { Text("لون نجمة وشريحة التوصية (Recommended Badge)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تخصيص نمط الخطوط العربية بالدليل (RTL typography):", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        val fontOptions = listOf("cairo", "amiri", "tahoma", "system")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            fontOptions.forEach { font ->
                                val isSel = state.editFontSelected == font
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) themeColors.accent else Color.Black.copy(alpha = 0.3f))
                                        .clickable { state.editFontSelected = font }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(font.uppercase(), fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تعديل أحجام وإحداثيات أيقونات الدردشة العائمة بالدعم:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Text("1. حجم أيقونة شات المساعدة المباشرة: ${state.editChatIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editChatIconSize, onValueChange = { state.editChatIconSize = it }, valueRange = 35f..90f)

                        Text("• إحداثي الإزاحة الأفقي (X-Offset): ${state.editChatIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                        Slider(value = state.editChatIconX, onValueChange = { state.editChatIconX = it }, valueRange = 10f..120f)

                        Text("• إحداثي الإزاحة الرأسي (Y-Offset): ${state.editChatIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                        Slider(value = state.editChatIconY, onValueChange = { state.editChatIconY = it }, valueRange = 30f..180f)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("2. حجم أيقونة المساعد الصوتي الذكي (البوت): ${state.editAssistantIconSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editAssistantIconSize, onValueChange = { state.editAssistantIconSize = it }, valueRange = 35f..90f)

                        Text("• إحداثي البوت الأفقي (X-Offset): ${state.editAssistantIconX.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                        Slider(value = state.editAssistantIconX, onValueChange = { state.editAssistantIconX = it }, valueRange = 10f..120f)

                        Text("• إحداثي البوت الرأسي (Y-Offset): ${state.editAssistantIconY.toInt()}", fontSize = 10.sp, color = themeColors.textSecondary)
                        Slider(value = state.editAssistantIconY, onValueChange = { state.editAssistantIconY = it }, valueRange = 30f..180f)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📋 إدارة شروط ونموذج تسجيل الفنيين بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = state.requirementItemInput,
                                onValueChange = { state.requirementItemInput = it },
                                label = { Text("اسم الشرط (مثال: فيش جنائي)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إلزامي؟", fontSize = 9.sp, color = Color.White)
                                Switch(
                                    checked = state.isNewRequirementMandatory,
                                    onCheckedChange = { state.isNewRequirementMandatory = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            Button(
                                onClick = {
                                    if (state.requirementItemInput.trim().isNotEmpty()) {
                                        val suffix = if (state.isNewRequirementMandatory) "|Mandatory" else "|Optional"
                                        state.requirementsListState = state.requirementsListState + "${state.requirementItemInput.trim()}$suffix"
                                        state.requirementItemInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("أضف", color = Color.Black)
                            }
                        }

                        state.requirementsListState.forEachIndexed { idx, reqItem ->
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
                                    Text(
                                        if (isMand) "إلزامي (مطلوب لإنشاء الحساب) 🔴" else "اختياري (غير معرقل للتسجيل) 🟢",
                                        color = if (isMand) Color.Red.copy(alpha = 0.8f) else Color.Green,
                                        fontSize = 10.sp
                                    )
                                }

                                var isEditingItem by remember { mutableStateOf(false) }
                                var editItemValue by remember(reqName) { mutableStateOf(reqName) }
                                var editItemMandatory by remember(isMand) { mutableStateOf(isMand) }

                                if (isEditingItem) {
                                    AlertDialog(
                                        onDismissRequest = { isEditingItem = false },
                                        title = { Text("📝 تعديل الشرط أو المرفق") },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = editItemValue,
                                                    onValueChange = { editItemValue = it },
                                                    label = { Text("اسم الشرط أو المستند") },
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("شرط إلزامي للجميع؟")
                                                    Switch(checked = editItemMandatory, onCheckedChange = { editItemMandatory = it })
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(onClick = {
                                                if (editItemValue.trim().isNotEmpty()) {
                                                    val updatedList = state.requirementsListState.toMutableList()
                                                    val suff = if (editItemMandatory) "|Mandatory" else "|Optional"
                                                    updatedList[idx] = "${editItemValue.trim()}$suff"
                                                    state.requirementsListState = updatedList
                                                    isEditingItem = false
                                                }
                                            }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)) {
                                                Text("حفظ التعديل")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { isEditingItem = false }) {
                                                Text("إلغاء")
                                            }
                                        }
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isEditingItem = true }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            state.requirementsListState = state.requirementsListState.filterIndexed { pIdx, _ -> pIdx != idx }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "حذف الالتزام", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📏 تخصيص مقاسات وأبعاد كروت الفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Text("• ارتفاع صورة غلاف الكرت (0 للإخفاء): ${state.editCoverHeight.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editCoverHeight, onValueChange = { state.editCoverHeight = it }, valueRange = 0f..250f)

                        Text("• حجم الصورة الشخصية (Avatar Size): ${state.editAvatarSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editAvatarSize, onValueChange = { state.editAvatarSize = it }, valueRange = 30f..100f)

                        Text("• الهامش والتباعد الداخلي للكرت (Padding): ${state.editCardPadding.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editCardPadding, onValueChange = { state.editCardPadding = it }, valueRange = 4f..24f)

                        Text("• المسافات بين عناصر الكرت (Spacing): ${state.editElementSpacing.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Slider(value = state.editElementSpacing, onValueChange = { state.editElementSpacing = it }, valueRange = 2f..16f)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛡️ إظهار وإخفاء شارات التميز والتوثيق بالفنيين:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏆 شارة VIP الذهبية والدرع المحيط بالكرت", fontSize = 11.sp, color = Color.White)
                            Switch(checked = state.editShowVipBadge, onCheckedChange = { state.editShowVipBadge = it })
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🔵 شارة التوثيق الزرقاء المعتمدة", fontSize = 11.sp, color = Color.White)
                            Switch(checked = state.editShowVerifiedBadge, onCheckedChange = { state.editShowVerifiedBadge = it })
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🟢 شارة نجمة التوصية (موصى به)", fontSize = 11.sp, color = Color.White)
                            Switch(checked = state.editShowRecommendedBadge, onCheckedChange = { state.editShowRecommendedBadge = it })
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖 إعدادات المساعد الذكي وسوابق الأعمال الفنية:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎁 تفعيل نقاط الولاء والمشاركة للمساعد الذكي", fontSize = 11.sp, color = Color.White)
                            Switch(checked = state.editShowLoyaltyBanner, onCheckedChange = { state.editShowLoyaltyBanner = it })
                        }

                        Text("📂 أقصى حد لصور سابقة الأعمال التي يرفعها مقدم الخدمة: ${state.editMaxWorkPhotos.toInt()}", fontSize = 11.sp, color = Color.White)
                        Slider(
                            value = state.editMaxWorkPhotos,
                            onValueChange = { state.editMaxWorkPhotos = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📞 التحكم الفائق بأزرار الاتصال والتواصل في الكروت:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📞 تفعيل زر الاتصال الهاتفي المباشر", fontSize = 11.sp, color = Color.White)
                                Switch(checked = state.editShowCallButton, onCheckedChange = { state.editShowCallButton = it })
                            }
                            if (state.editShowCallButton) {
                                OutlinedTextField(
                                    value = state.editCallButtonColorHex,
                                    onValueChange = { state.editCallButtonColorHex = it },
                                    label = { Text("لون زر الاتصال (Hex)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💬 تفعيل زر المحادثة السريعة واتساب", fontSize = 11.sp, color = Color.White)
                                Switch(checked = state.editShowWhatsappButton, onCheckedChange = { state.editShowWhatsappButton = it })
                            }
                            if (state.editShowWhatsappButton) {
                                OutlinedTextField(
                                    value = state.editWhatsappButtonColorHex,
                                    onValueChange = { state.editWhatsappButtonColorHex = it },
                                    label = { Text("لون زر واتساب (Hex)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🔍 تفعيل زر عرض التفاصيل والتقييمات", fontSize = 11.sp, color = Color.White)
                                Switch(checked = state.editShowDetailsButton, onCheckedChange = { state.editShowDetailsButton = it })
                            }
                            if (state.editShowDetailsButton) {
                                OutlinedTextField(
                                    value = state.editDetailsButtonColorHex,
                                    onValueChange = { state.editDetailsButtonColorHex = it },
                                    label = { Text("لون زر التفاصيل (Hex)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📅 تفعيل زر طلب الحجز المباشر", fontSize = 11.sp, color = Color.White)
                                Switch(checked = state.editShowBookButton, onCheckedChange = { state.editShowBookButton = it })
                            }
                            if (state.editShowBookButton) {
                                OutlinedTextField(
                                    value = state.editBookButtonColorHex,
                                    onValueChange = { state.editBookButtonColorHex = it },
                                    label = { Text("لون زر الحجز المباشر (Hex)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val upToDateSettings = settingsState.copy(
                            customPrimaryHex = state.editPrimaryHex,
                            customSecondaryHex = state.editSecondaryHex,
                            cardBackgroundHex = state.editCardBgHex,
                            providerNameColorHex = state.editProviderNameHex,
                            locationColorHex = state.editLocationHex,
                            ratingColorHex = state.editRatingHex,
                            vipBadgeColorHex = state.editVipBadgeHex,
                            verifiedBadgeColorHex = state.editVerifiedHex,
                            recommendedBadgeColorHex = state.editRecommendedHex,
                            activeFontFamily = state.editFontSelected,
                            chatSize = state.editChatIconSize.toInt(),
                            chatXOffset = state.editChatIconX.toInt(),
                            chatYOffset = state.editChatIconY.toInt(),
                            assistantSize = state.editAssistantIconSize.toInt(),
                            assistantXOffset = state.editAssistantIconX.toInt(),
                            assistantYOffset = state.editAssistantIconY.toInt(),
                            registrationRequirements = state.requirementsListState.joinToString(","),
                            coverHeight = state.editCoverHeight.toInt(),
                            avatarSize = state.editAvatarSize.toInt(),
                            elementSpacing = state.editElementSpacing.toInt(),
                            cardPadding = state.editCardPadding.toInt(),
                            showVipBadge = state.editShowVipBadge,
                            showVerifiedBadge = state.editShowVerifiedBadge,
                            showRecommendedBadge = state.editShowRecommendedBadge,
                            showCallButton = state.editShowCallButton,
                            showWhatsappButton = state.editShowWhatsappButton,
                            showDetailsButton = state.editShowDetailsButton,
                            showBookButton = state.editShowBookButton,
                            callButtonColorHex = state.editCallButtonColorHex,
                            whatsappButtonColorHex = state.editWhatsappButtonColorHex,
                            detailsButtonColorHex = state.editDetailsButtonColorHex,
                            bookButtonColorHex = state.editBookButtonColorHex,
                            showLoyaltyBanner = state.editShowLoyaltyBanner,
                            maxWorkPhotos = state.editMaxWorkPhotos.toInt()
                        )
                        viewModel.updateBackdoorSettings(
                            appName = upToDateSettings.appName,
                            welcomeMsg = upToDateSettings.welcomeMessage,
                            footerMsg = upToDateSettings.footerMessage,
                            themeId = upToDateSettings.activeThemeId,
                            supportPhone = upToDateSettings.supportPhone,
                            supportEmail = upToDateSettings.supportEmail,
                            supportWhatsapp = upToDateSettings.supportWhatsapp,
                            isMaintenance = upToDateSettings.isMaintenanceActive,
                            hiddenFooter = upToDateSettings.hidePromoFooter,
                            botHidden = upToDateSettings.assistantHidden,
                            botSize = upToDateSettings.assistantSize,
                            chatHidden = upToDateSettings.chatHidden,
                            chatSize = upToDateSettings.chatSize,
                            radiusKm = upToDateSettings.maxSearchRadiusKm,
                            isSpeech = upToDateSettings.isSpeechSearchEnabled,
                            isDataSaver = false,
                            imgQuality = 90
                        )
                        viewModel.saveCustomSettingsState(upToDateSettings)
                        Toast.makeText(context, "تم حفظ وضبط وتعميم مظهر الدليل والأزرار والبطاقات بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 حفظ وحقن جميع تخصيصات المظهر بالدليل الصريح والكامل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
