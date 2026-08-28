package com.example.ui.screens.about.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

/**
 * AboutContentRenderer is responsible for displaying the list of about components
 * in the requested order configured by administrators.
 *
 * @param settings The AdminSettingsEntity contains all customization variables.
 * @param themeColors The system theme color palette.
 * @param context The Android context used to launch intent requests.
 */
@Composable
fun AboutContentRenderer(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    context: Context
) {
    val orderKeys = settings.aboutLayoutOrder
        .split(",")
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        orderKeys.forEach { key ->
            when (key) {
                "COVER" -> {
                    RenderCover(settings, themeColors)
                }
                "LOGO" -> {
                    RenderLogo(themeColors)
                }
                "TITLE" -> {
                    RenderTitle(settings)
                }
                "ANNOUNCEMENT" -> {
                    RenderAnnouncement(settings, themeColors)
                }
                "ABOUT_CARD" -> {
                    RenderAboutCard(settings, themeColors)
                }
                "DOWNLOAD_BTN" -> {
                    RenderDownloadButton(settings, context)
                }
                "CONTACTS" -> {
                    RenderContacts(settings, context)
                }
                "SOCIALS" -> {
                    RenderSocials(settings, context)
                }
            }
        }

        // Email Support Button (Common bottom element)
        if (settings.supportEmail.isNotBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(settings.supportEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "استفسار بخصوص ${settings.appName}")
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("✉️ مراسلة الإدارة: ${settings.supportEmail}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RenderCover(settings: AdminSettingsEntity, themeColors: VisualThemePalette) {
    val coverType = settings.aboutCoverType
    val coverContent = settings.aboutCoverContent
    val coverBase64 = settings.aboutCoverBase64

    if (coverType == "IMAGE" && (coverBase64.isNotEmpty() || coverContent.isNotEmpty())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (coverBase64.isNotEmpty()) {
                val bitmap = remember(coverBase64) {
                    try {
                        val bytes = android.util.Base64.decode(coverBase64, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "صورة الغلاف",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (coverContent.isNotEmpty()) {
                AsyncImage(
                    model = coverContent,
                    contentDescription = "صورة الغلاف",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun RenderLogo(themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .shadow(6.dp, CircleShape)
            .background(Color(0xFFD91A1A), CircleShape)
            .border(3.dp, themeColors.accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "WAM",
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp
        )
    }
}

@Composable
private fun RenderTitle(settings: AdminSettingsEntity) {
    Text(
        text = settings.appName,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RenderAnnouncement(settings: AdminSettingsEntity, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إعلان المنصة الرسمي",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = settings.bannerContent.ifEmpty { "منصة لكل الخدمات" },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RenderAboutCard(settings: AdminSettingsEntity, themeColors: VisualThemePalette) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حول تطبيق دليل ${settings.appName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
            Text(
                text = settings.aboutCustomInfo.ifEmpty { "المنصة الأولى لربط العملاء بالمهنيين والحرفيين المعتمدين في كافة المجالات والمحافظات اليمنية مباشرة وبكل سهولة وأمان." },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Start,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "النسخة الحالية",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = settings.appVersion,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مستوى التشفير والأمان",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = settings.encryptionType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
        }
    }
}

@Composable
private fun RenderDownloadButton(settings: AdminSettingsEntity, context: Context) {
    Button(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settings.appDownloadUrl.ifBlank { "https://example.com/download_app" }))
                context.startActivity(intent)
            } catch (e: Exception) {}
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "تحميل وتحديث التطبيق",
            tint = Color.Black,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "📥 💾 تحميل وتحديث التطبيق المباشر",
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RenderContacts(settings: AdminSettingsEntity, context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📞 اتصل وتواصل معنا مباشرة:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val url = "https://wa.me/${settings.supportWhatsapp}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try { context.startActivity(intent) } catch(e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("💬 واتساب الدعم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = {
                    val uri = Uri.parse("tel:${settings.supportPhone}")
                    val intent = Intent(Intent.ACTION_DIAL, uri)
                    try { context.startActivity(intent) } catch(e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("📞 اتصال الدعم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RenderSocials(settings: AdminSettingsEntity, context: Context) {
    val socialsList = mutableListOf<Triple<String, String, Color>>()
    if (!settings.hideWebsite && settings.websiteUrl.isNotBlank()) {
        socialsList.add(Triple("الموقع", settings.websiteUrl, Color(0xFF3B82F6)))
    }
    if (!settings.hideTelegram && settings.telegramUrl.isNotBlank()) {
        socialsList.add(Triple("تليجرام", settings.telegramUrl, Color(0xFF0088CC)))
    }
    if (!settings.hideFacebook && settings.facebookUrl.isNotBlank()) {
        socialsList.add(Triple("فيسبوك", settings.facebookUrl, Color(0xFF3B5998)))
    }
    if (!settings.hideTwitter && settings.twitterUrl.isNotBlank()) {
        socialsList.add(Triple("تويتر (X)", settings.twitterUrl, Color(0xFF1DA1F2)))
    }
    if (!settings.hideInstagram && settings.instagramUrl.isNotBlank()) {
        socialsList.add(Triple("إنستغرام", settings.instagramUrl, Color(0xFFE1306C)))
    }
    if (!settings.hideYoutube && settings.youtubeUrl.isNotBlank()) {
        socialsList.add(Triple("يوتيوب", settings.youtubeUrl, Color(0xFFFF0000)))
    }

    if (socialsList.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌐 حساباتنا الرسمية والروابط الخارجية:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            socialsList.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { social ->
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(social.second))
                                try { context.startActivity(intent) } catch(e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = social.third),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(social.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
