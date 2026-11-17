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
internal fun ColumnScope.OwnerBackdoorBannerAndLogoSection(
    state: OwnerBackdoorState,
    themeColors: VisualThemePalette
) {
    val context = state.context

    // 📢 Dynamic App Banner Configuration UI
    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)
    Text("📢 إعداد وتخصيص البنر الإعلاني بالأعلى (الرئيسي):", fontSize = 13.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("تفعيل عرض البنر بالرئيسية:", fontSize = 12.sp, color = Color.White)
        Switch(
            checked = state.bannerEnabled,
            onCheckedChange = { state.bannerEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = themeColors.accent)
        )
    }

    if (state.bannerEnabled) {
        Text("نوع محتوى البنر:", fontSize = 11.sp, color = themeColors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val bannerTypes = listOf("TEXT" to "نص فقط 📝", "IMAGE" to "صورة 🖼️", "VIDEO" to "فيديو قصير 📹")
            bannerTypes.forEach { (typeKey, typeLabel) ->
                val isSel = state.bannerType == typeKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.bannerType = typeKey }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(typeLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("مكان ظهور البنر بالرئيسية:", fontSize = 11.sp, color = themeColors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val locations = listOf("TOP" to "أعلى الشاشة ⬆️", "BOTTOM" to "أسفل الشاشة ⬇️")
            locations.forEach { (locKey, locLabel) ->
                val isSel = state.bannerLocation == locKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.bannerLocation = locKey }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(locLabel, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (state.bannerType == "IMAGE" || state.bannerType == "VIDEO") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (state.bannerType == "VIDEO") {
                            state.galleryLauncherForBanner.launch("video/*")
                        } else {
                            state.galleryLauncherForBanner.launch("image/*")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(if (state.bannerType == "VIDEO") "فيديو قصير 📹" else "صورة البنر 📁", fontSize = 10.sp, color = Color.White)
                }

                OutlinedTextField(
                    value = state.bannerContent,
                    onValueChange = { state.bannerContent = it },
                    label = { Text("أو رابط الإنترنت") },
                    modifier = Modifier.weight(2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        } else {
            OutlinedTextField(
                value = state.bannerContent,
                onValueChange = { state.bannerContent = it },
                label = { Text("محتوى الإعلان النصي") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = state.bannerDurationSeconds,
            onValueChange = { state.bannerDurationSeconds = it },
            label = { Text("مدة ظهور البنر بالثواني (0 للبقاء المستمر)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("طريقة حركة أو ظهور البنر الإعلاني:", fontSize = 11.sp, color = themeColors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val styles = listOf("SLIDE" to "انزلاق ➡️", "FADE" to "تلاشي 🌫️", "BLINK" to "وميض 💡", "SCROLL" to "تمرير 📜")
            styles.forEach { (styleKey, styleLabel) ->
                val isSel = state.bannerDisplayStyle == styleKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.bannerDisplayStyle = styleKey }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(styleLabel, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Divider(color = themeColors.accent.copy(alpha = 0.3f), thickness = 1.dp)

    Spacer(modifier = Modifier.height(4.dp))
    Text("🖼️ شعار التطبيق المستخدم بالمنصة:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val logos = listOf("شعار احترافي 🔧", "بسيط 🌟", "رسمي 🤝", "أيقونة الدليل 🎯")
        logos.forEach { lg ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.selectedSimulatedLogo == lg) themeColors.accent else themeColors.surface)
                    .clickable {
                        state.selectedSimulatedLogo = lg
                        Toast.makeText(context, "🖼️ تم تحديد الشعار ($lg) بنجاح للتطبيق!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(lg, fontSize = 9.sp, color = if (state.selectedSimulatedLogo == lg) Color.Black else Color.White)
            }
        }
    }
}
