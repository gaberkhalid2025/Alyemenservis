package com.example.ui.screens.about

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

/**
 * Renders individual elements of the About screen dynamically based on [AdminSettingsEntity.aboutLayoutOrder].
 */
@Composable
fun AboutContentRenderer(
    settingsState: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse layout order from settings state
    val orderKeys = remember(settingsState.aboutLayoutOrder) {
        settingsState.aboutLayoutOrder
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        orderKeys.forEach { key ->
            when (key) {
                "COVER" -> {
                    val coverType = settingsState.aboutCoverType
                    val coverContent = settingsState.aboutCoverContent
                    val coverBase64 = settingsState.aboutCoverBase64

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
                                        val length = java.lang.reflect.Array.getLength(bytes)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, length)
                                    } catch (e: Exception) {
                                        null
                                    }
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
                "LOGO" -> {
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
                "TITLE" -> {
                    Text(
                        text = settingsState.appName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                "ANNOUNCEMENT" -> {
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
                                text = settingsState.bannerContent.ifEmpty { "منصة لكل الخدمات" },
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                "ABOUT_CARD" -> {
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
                                    text = "حول تطبيق دليل ${settingsState.appName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }
                            Text(
                                text = settingsState.aboutCustomInfo.ifEmpty { "المنصة الأولى لربط العملاء بالمهنيين والحرفيين المعتمدين في كافة المجالات والمحافظات اليمنية مباشرة وبكل سهولة وأمان." },
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Start,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

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
                                    text = settingsState.appVersion,
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
                                    text = settingsState.encryptionType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }
                        }
                    }
                }
                "DOWNLOAD_BTN" -> {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(settingsState.appDownloadUrl.ifBlank { "https://example.com/download_app" })
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
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
                "CONTACTS" -> {
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
                                    val url = "https://wa.me/${settingsState.supportWhatsapp}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "💬 واتساب الدعم",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val uri = Uri.parse("tel:${settingsState.supportPhone}")
                                    val intent = Intent(Intent.ACTION_DIAL, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "📞 اتصال الدعم",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                "SOCIALS" -> {
                    val socialsList = mutableListOf<Triple<String, String, Color>>()
                    if (!settingsState.hideWebsite && settingsState.websiteUrl.isNotBlank()) {
                        socialsList.add(Triple("الموقع", settingsState.websiteUrl, Color(0xFF3B82F6)))
                    }
                    if (!settingsState.hideTelegram && settingsState.telegramUrl.isNotBlank()) {
                        socialsList.add(Triple("تليجرام", settingsState.telegramUrl, Color(0xFF0088CC)))
                    }
                    if (!settingsState.hideFacebook && settingsState.facebookUrl.isNotBlank()) {
                        socialsList.add(Triple("فيسبوك", settingsState.facebookUrl, Color(0xFF3B5998)))
                    }
                    if (!settingsState.hideTwitter && settingsState.twitterUrl.isNotBlank()) {
                        socialsList.add(Triple("تويتر (X)", settingsState.twitterUrl, Color(0xFF1DA1F2)))
                    }
                    if (!settingsState.hideInstagram && settingsState.instagramUrl.isNotBlank()) {
                        socialsList.add(Triple("إنستغرام", settingsState.instagramUrl, Color(0xFFE1306C)))
                    }
                    if (!settingsState.hideYoutube && settingsState.youtubeUrl.isNotBlank()) {
                        socialsList.add(Triple("يوتيوب", settingsState.youtubeUrl, Color(0xFFFF0000)))
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
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // Ignore
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = social.third),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                social.first,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
            }
        }

        // Email Support Button (Rendered Outside the order sequence)
        if (settingsState.supportEmail.isNotBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(settingsState.supportEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "استفسار بخصوص ${settingsState.appName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "✉️ مراسلة الإدارة: ${settingsState.supportEmail}",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}
