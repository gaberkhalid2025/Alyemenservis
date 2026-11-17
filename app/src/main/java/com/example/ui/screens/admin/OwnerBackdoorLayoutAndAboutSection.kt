@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
internal fun ColumnScope.OwnerBackdoorLayoutAndAboutSection(
    state: OwnerBackdoorState,
    themeColors: VisualThemePalette
) {
    val context = state.context

    // --- Custom Top Bar Icons Controls ---
    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("🎨 تخصيص وترتيب أيقونات شريط الرأس العلوي (Header):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("إظهار أيقونة المزامنة/التحديث (Refresh)", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.showRefreshIcon, onCheckedChange = { state.showRefreshIcon = it })
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("إظهار أيقونة الإعدادات/اللغة (Settings)", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.showSettingsIcon, onCheckedChange = { state.showSettingsIcon = it })
    }

    OutlinedTextField(
        value = state.headerIconsOrder,
        onValueChange = { state.headerIconsOrder = it },
        label = { Text("ترتيب الأيقونات (مفصولة بفاصلة)") },
        placeholder = { Text("مثال: MENU,NOTIF,CHAT,REFRESH,SETTINGS") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Text(
        text = "الأيقونات المتاحة: MENU (القائمة), NOTIF (الإشعارات), CHAT (المحادثة), REFRESH (المزامنة), SETTINGS (الإعدادات). اكتبها بالترتيب المطلوب من اليمين لليسار.",
        color = Color.LightGray,
        fontSize = 9.sp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.surface)
                .clickable {
                    state.headerIconsOrder = "MENU,NOTIF,CHAT"
                    Toast.makeText(context, "🎯 تم ضبط الترتيب الافتراضي للأيقونات", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("الترتيب الافتراضي", fontSize = 9.sp, color = Color.White)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.surface)
                .clickable {
                    state.headerIconsOrder = "CHAT,NOTIF,MENU"
                    Toast.makeText(context, "🎯 تم عكس ترتيب الأيقونات", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("عكس الترتيب", fontSize = 9.sp, color = Color.White)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.surface)
                .clickable {
                    state.headerIconsOrder = "MENU,CHAT,NOTIF,REFRESH,SETTINGS"
                    state.showRefreshIcon = true
                    state.showSettingsIcon = true
                    Toast.makeText(context, "🎯 تم تفعيل كافة الأيقونات بالترتيب الكامل", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("تفعيل وترتيب الكل", fontSize = 9.sp, color = Color.White)
        }
    }

    // --- Custom Categories Layout Controls ---
    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("📁 طريقة عرض وحركة الأقسام (التصنيفات):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val layouts = listOf(
            "GRID_HORIZONTAL" to "شبكة أفقية (تمرير جانبي) ↔️",
            "ROW_HORIZONTAL" to "صف أفقي (تمرير جانبي) ↔️",
            "GRID_VERTICAL" to "شبكة عمودية (تمرير طولي) ↕️",
            "LIST_VERTICAL" to "قائمة عمودية (تمرير طولي) ↕️"
        )
        layouts.forEach { (layVal, layLabel) ->
            val isSel = state.categoriesLayoutTypeState == layVal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) themeColors.accent else themeColors.surface)
                    .clickable { state.categoriesLayoutTypeState = layVal }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(layLabel, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                if (isSel) {
                    Text("✔️", fontSize = 10.sp, color = Color.Black)
                }
            }
        }
    }

    // --- Custom Booking dynamic layouts & rules controls ---
    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("📅 تخصيص شروط واستمارة الحجز اليمني (كاملة بالتفصيل):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    OutlinedTextField(
        value = state.bookingTermsInput,
        onValueChange = { state.bookingTermsInput = it },
        label = { Text("نص شروط وقواعد الحجز بالدليل") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.bookingLabelNameInput,
        onValueChange = { state.bookingLabelNameInput = it },
        label = { Text("تسمية حقل الاسم بالاستمارة") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.bookingLabelPhoneInput,
        onValueChange = { state.bookingLabelPhoneInput = it },
        label = { Text("تسمية حقل الهاتف بالاستمارة") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.bookingLabelAreaInput,
        onValueChange = { state.bookingLabelAreaInput = it },
        label = { Text("تسمية حقل منطقة السكن بالاستمارة") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.bookingLabelServiceInput,
        onValueChange = { state.bookingLabelServiceInput = it },
        label = { Text("تسمية حقل نوع الخدمة بالاستمارة") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("🔐 بيانات المدير (Admin) والمصادقة الآمنة:", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    OutlinedTextField(
        value = state.adminUsernameInput,
        onValueChange = { state.adminUsernameInput = it },
        label = { Text("معرّف/بريد المدير (للقراءة فقط)") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Text("🔒 تتم إدارة المصادقة وكلمات المرور للمدير بشكل مشفر عبر Firebase Auth للحماية القصوى.", fontSize = 10.5.sp, color = Color.Gray)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("تذكرني (حفظ الدخول بصورة دائمة كمالك)", color = Color.White, fontSize = 13.sp)
        Switch(checked = state.rememberLoginInput, onCheckedChange = { state.rememberLoginInput = it })
    }

    Spacer(modifier = Modifier.height(4.dp))
    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("ℹ️ تخصيص صفحة معلومات عن التطبيق (المحتوى والغلاف):", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    Text("نوع غلاف الصفحة المعروض:", fontSize = 11.sp, color = Color.White)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val types = listOf("IMAGE" to "صورة 🖼️", "VIDEO" to "فيديو 🎥", "TEXT" to "نص فقط 📝")
        types.forEach { (typeVal, typeLabel) ->
            val isSel = state.aboutCoverType == typeVal
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) themeColors.accent else themeColors.surface)
                    .clickable { state.aboutCoverType = typeVal }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(typeLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
            }
        }
    }

    if (state.aboutCoverType == "IMAGE") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { state.galleryLauncherForCover.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.weight(1f)
            ) {
                Text("صورة الهاتف 📁", fontSize = 10.sp, color = Color.White)
            }

            OutlinedTextField(
                value = state.aboutCoverContent,
                onValueChange = { state.aboutCoverContent = it },
                label = { Text("أو رابط صورة الإنترنت") },
                modifier = Modifier.weight(1.5f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    } else if (state.aboutCoverType == "VIDEO") {
        OutlinedTextField(
            value = state.aboutCoverContent,
            onValueChange = { state.aboutCoverContent = it },
            label = { Text("رابط الفيديو (يوتيوب أو ملف مباشر)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
    } else {
        OutlinedTextField(
            value = state.aboutCoverContent,
            onValueChange = { state.aboutCoverContent = it },
            label = { Text("نص الغلاف البديل") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
    }

    OutlinedTextField(
        value = state.aboutCustomInfo,
        onValueChange = { state.aboutCustomInfo = it },
        label = { Text("المعلومات التفصيلية المكتوبة عن التطبيق") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    OutlinedTextField(
        value = state.encryptionTypeState,
        onValueChange = { state.encryptionTypeState = it },
        label = { Text("مستوى التشفير للتطبيق") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text("🔗 روابط وتعديل شبكات التواصل الاجتماعي للتطبيق:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    OutlinedTextField(
        value = state.twitterUrl,
        onValueChange = { state.twitterUrl = it },
        label = { Text("رابط تويتر (X)") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء تويتر (X):", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideTwitter, onCheckedChange = { state.hideTwitter = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    OutlinedTextField(
        value = state.instagramUrl,
        onValueChange = { state.instagramUrl = it },
        label = { Text("رابط إنستغرام") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء إنستغرام:", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideInstagram, onCheckedChange = { state.hideInstagram = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    OutlinedTextField(
        value = state.youtubeUrl,
        onValueChange = { state.youtubeUrl = it },
        label = { Text("رابط يوتيوب") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء يوتيوب:", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideYoutube, onCheckedChange = { state.hideYoutube = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    OutlinedTextField(
        value = state.websiteUrl,
        onValueChange = { state.websiteUrl = it },
        label = { Text("رابط الموقع الإلكتروني") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء الموقع الإلكتروني:", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideWebsite, onCheckedChange = { state.hideWebsite = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    OutlinedTextField(
        value = state.facebookUrl,
        onValueChange = { state.facebookUrl = it },
        label = { Text("رابط فيسبوك") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء فيسبوك:", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideFacebook, onCheckedChange = { state.hideFacebook = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    OutlinedTextField(
        value = state.telegramUrl,
        onValueChange = { state.telegramUrl = it },
        label = { Text("رابط تليجرام") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("إخفاء تليجرام:", fontSize = 11.sp, color = Color.White)
        Switch(checked = state.hideTelegram, onCheckedChange = { state.hideTelegram = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent))
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text("📐 ترتيب وهيكلة عناصر صفحة معلومات عن التطبيق:", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = state.aboutLayoutOrder,
        onValueChange = { state.aboutLayoutOrder = it },
        label = { Text("ترتيب العناصر (مفصولة بفاصلة ,)") },
        placeholder = { Text("COVER,LOGO,TITLE,ANNOUNCEMENT,ABOUT_CARD,DOWNLOAD_BTN,CONTACTS,SOCIALS") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
    Text("مفاتيح الترتيب المدعومة: COVER, LOGO, TITLE, ANNOUNCEMENT, ABOUT_CARD, DOWNLOAD_BTN, CONTACTS, SOCIALS", fontSize = 9.sp, color = Color.LightGray)
}
