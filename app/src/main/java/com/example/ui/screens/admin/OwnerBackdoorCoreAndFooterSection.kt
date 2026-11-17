@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
internal fun ColumnScope.OwnerBackdoorCoreAndFooterSection(
    state: OwnerBackdoorState,
    themeColors: VisualThemePalette
) {
    OutlinedTextField(
        value = state.appName,
        onValueChange = { state.appName = it },
        label = { Text("اسم التطبيق الرئيسي") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.welcomeMessage,
        onValueChange = { state.welcomeMessage = it },
        label = { Text("رسالة الترحيب في الهيدر") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.splashWelcomeMessageState,
        onValueChange = { state.splashWelcomeMessageState = it },
        label = { Text("رسالة ترحيب شاشة البداية (Splash)") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.footerMessage,
        onValueChange = { state.footerMessage = it },
        label = { Text("نص الفوتر الأوسط (افتراضي: wam777644)") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.footerBgColorHex,
        onValueChange = { state.footerBgColorHex = it },
        label = { Text("لون خلفية الفوتر (كود Hex مثل #115E59)") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.appVersion,
        onValueChange = { state.appVersion = it },
        label = { Text("رقم إصدار التطبيق بالفوتر (مثل v2.2026)") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚙️ التحكم الكامل في أيقونات ومظهر الشريط السفلي (الفوتر):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️ أيقونة 'عن التطبيق':", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = state.showInfoIcon,
                    onCheckedChange = { state.showInfoIcon = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅 أيقونة 'الحجوزات':", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = state.showBookingsIcon,
                    onCheckedChange = { state.showBookingsIcon = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🌐 أيقونة 'تبديل اللغة' (العربية / English):", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = state.showLangIcon,
                    onCheckedChange = { state.showLangIcon = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒 أيقونة 'لوحة الإدارة':", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = state.showAdminIcon,
                    onCheckedChange = { state.showAdminIcon = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💬 نص الشريط السفلي (الفوتر):", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = state.showFooterText,
                    onCheckedChange = { state.showFooterText = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
                )
            }

            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

            Text("🎨 شكل أيقونة تبديل اللغة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("GLOBE" to "كرة أرضية 🌐", "TRANSLATE" to "ترجمة 🔤", "TEXT" to "نص لغة 💬").forEach { (type, lbl) ->
                    FilterChip(
                        selected = state.langIconType == type,
                        onClick = { state.langIconType = type },
                        label = { Text(lbl, fontSize = 9.sp) }
                    )
                }
            }

            Text("🎨 شكل أيقونة لوحة الإدارة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("LOCK" to "قفل 🔒", "SETTINGS" to "ترس ⚙️", "KEY" to "مفتاح 🔑", "SHIELD" to "درع 🛡️").forEach { (type, lbl) ->
                    FilterChip(
                        selected = state.adminIconType == type,
                        onClick = { state.adminIconType = type },
                        label = { Text(lbl, fontSize = 9.sp) }
                    )
                }
            }

            Text("تنسيق وترتيب شريط التذييل (الفوتر):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.footerItemsOrder,
                onValueChange = { state.footerItemsOrder = it },
                label = { Text("ترتيب عناصر الفوتر (مفصولة بفاصلة: INFO,BOOKINGS,TEXT,LANG,ADMIN)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                val layouts = listOf(
                    "INFO,BOOKINGS,TEXT,LANG,ADMIN" to "تنسيق قياسي (يسار->يمين)",
                    "INFO,TEXT,LANG,ADMIN" to "بدون حجوزات",
                    "TEXT,LANG,ADMIN" to "نص ولغة وإدارة"
                )
                layouts.forEach { (order, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (state.footerItemsOrder == order) themeColors.accent else themeColors.surface)
                            .clickable { state.footerItemsOrder = order }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 8.sp, color = if (state.footerItemsOrder == order) Color.Black else Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = state.supportPhone,
        onValueChange = { state.supportPhone = it },
        label = { Text("رقم هاتف الدعم الفني للبرنامج") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.supportEmail,
        onValueChange = { state.supportEmail = it },
        label = { Text("البريد الإلكتروني للدعم والمظالم") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.supportWhatsapp,
        onValueChange = { state.supportWhatsapp = it },
        label = { Text("رابط أو رقم واتساب الدعم الفني") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.appDownloadUrl,
        onValueChange = { state.appDownloadUrl = it },
        label = { Text("رابط خارجي لتحميل التطبيق (نسخة APK)") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.websiteUrl,
        onValueChange = { state.websiteUrl = it },
        label = { Text("رابط الموقع الإلكتروني الرسمي") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.telegramUrl,
        onValueChange = { state.telegramUrl = it },
        label = { Text("رابط حساب/قناة التليجرام") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.facebookUrl,
        onValueChange = { state.facebookUrl = it },
        label = { Text("رابط صفحة الفيسبوك الرسمية") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    Text("🎨 سمة التطبيق الافتراضية:", fontSize = 12.sp, color = themeColors.textSecondary)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val themes = listOf("EMERALD_YEMEN", "COSMIC_SILVER", "LUXURY_GOLD", "ELITE_EMERALD", "SMOKE_BLACK", "LIGHT_PINK", "GOLDEN_WHITE", "CUSTOM_THEME")
        themes.forEach { th ->
            val label = when (th) {
                "EMERALD_YEMEN" -> "الزمرد اليمني 🟢"
                "COSMIC_SILVER" -> "كوزميك سيلفر 🪐"
                "LUXURY_GOLD" -> "الذهبي الفاخر 🌟"
                "ELITE_EMERALD" -> "الزمردي الراقي 💚"
                "SMOKE_BLACK" -> "الأسود الدخاني ⚫"
                "LIGHT_PINK" -> "الزهري الفاتح 🌸"
                "GOLDEN_WHITE" -> "الأبيض الذهبي ⚪🟡"
                "CUSTOM_THEME" -> "سمة مخصصة 🎨"
                else -> th
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.activeThemeId == th) themeColors.accent else themeColors.surface)
                    .clickable { state.activeThemeId = th }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(label, fontSize = 9.sp, color = if (state.activeThemeId == th) Color.Black else Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("وضع الصيانة المؤقت", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isMaintenanceActive, onCheckedChange = { state.isMaintenanceActive = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء زر المساعد الذكي العائم", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.assistantHidden, onCheckedChange = { state.assistantHidden = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("تفعيل البحث الصوتي", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isSpeechSearchEnabled, onCheckedChange = { state.isSpeechSearchEnabled = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إلغاء شرط تسجيل الزائرين للحجز والمحادثة", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.bypassVisitorRegistration, onCheckedChange = { state.bypassVisitorRegistration = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("🔑 إلزامية كلمة المرور لجميع حسابات المواطنين الجديدة", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isUserPasswordRequired, onCheckedChange = { state.isUserPasswordRequired = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("تمكين ميزة الرادار والخريطة للجماهير", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isMapFeatureEnabled, onCheckedChange = { state.isMapFeatureEnabled = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("محرك الخرائط المعتمد:", color = Color.White, fontSize = 13.sp)
        Row {
            FilterChip(
                selected = state.mapProviderState == "MAPLIBRE",
                onClick = { state.mapProviderState = "MAPLIBRE" },
                label = { Text("MapLibre", fontSize = 10.sp) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = state.mapProviderState == "GOOGLE",
                onClick = { state.mapProviderState = "GOOGLE" },
                label = { Text("Google", fontSize = 10.sp) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = state.mapProviderState == "MAPBOX",
                onClick = { state.mapProviderState = "MAPBOX" },
                label = { Text("Mapbox", fontSize = 10.sp) }
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("🔓 تعطيل جدار حماية المحادثات الفورية", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.disableChatFirewall, onCheckedChange = { state.disableChatFirewall = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("🔓 تعطيل جدار حماية حجز المواعيد", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.disableBookingFirewall, onCheckedChange = { state.disableBookingFirewall = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("🏪 تفعيل وتبويب قسم المحلات التجارية بالكامل", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isStoresEnabled, onCheckedChange = { state.isStoresEnabled = it })
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("🏠 تفعيل وتبويب قسم العقارات بالكامل", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.isPropertiesEnabled, onCheckedChange = { state.isPropertiesEnabled = it })
    }

    // Dedicated Card for Registration Forms Control
    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📋 التحكم بإظهار/إخفاء استمارات التسجيل للأقسام", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            Text("تحديد الاستمارات المتاحة للمستخدمين للتسجيل والانضمام (المالك والأدمن يظهر له كل شيء دائماً):", fontSize = 10.sp, color = themeColors.textSecondary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🛠️ استمارة انضمام مقدمي الخدمات (أصحاب المهن)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Switch(checked = state.enableProvidersRegistration, onCheckedChange = { state.enableProvidersRegistration = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🏪 استمارة تسجيل المحلات والمراكز التجارية", color = Color.White, fontSize = 12.sp)
                Switch(checked = state.enableStoresRegistration, onCheckedChange = { state.enableStoresRegistration = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🍔 استمارة إضافة المطاعم والكافيهات", color = Color.White, fontSize = 12.sp)
                Switch(checked = state.enableRestaurantsRegistration, onCheckedChange = { state.enableRestaurantsRegistration = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🏢 استمارة إدراج العقارات والبيوت", color = Color.White, fontSize = 12.sp)
                Switch(checked = state.enablePropertiesRegistration, onCheckedChange = { state.enablePropertiesRegistration = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🏥 استمارة تسجيل المراكز الطبية والعيادات", color = Color.White, fontSize = 12.sp)
                Switch(checked = state.enableMedicalRegistration, onCheckedChange = { state.enableMedicalRegistration = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("💼 استمارة نشر الوظائف والفرص", color = Color.White, fontSize = 12.sp)
                Switch(checked = state.enableJobsRegistration, onCheckedChange = { state.enableJobsRegistration = it })
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء البوت المساعد المساعد", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.assistantHidden, onCheckedChange = { state.assistantHidden = it })
    }

    Column {
        Text("قطر دائرة البوت المساعد: ${state.assistantSize.toInt()} dp", color = Color.White, fontSize = 11.sp)
        Slider(value = state.assistantSize, onValueChange = { state.assistantSize = it }, valueRange = 40f..80f)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء زر المحادثة العائم", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.chatHidden, onCheckedChange = { state.chatHidden = it })
    }

    Column {
        Text("قطر دائرة زر المحادثة العائم: ${state.chatSize.toInt()} dp", color = Color.White, fontSize = 11.sp)
        Slider(value = state.chatSize, onValueChange = { state.chatSize = it }, valueRange = 28f..80f)
    }

    Column {
        Text("الحد الأقصى لنطاق البحث الجغرافي: ${state.maxSearchRadiusKm.toInt()} كم", color = Color.White, fontSize = 11.sp)
        Slider(value = state.maxSearchRadiusKm, onValueChange = { state.maxSearchRadiusKm = it }, valueRange = 10f..100f)
    }
}
