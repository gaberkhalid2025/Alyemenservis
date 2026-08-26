@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * ℹ️ AboutAppDialogView
 * نافذة سفلية منبثقة (ModalBottomSheet) تعرض تفاصيل التطبيق، النسخة، وروابط التواصل
 * متوافقة 100% مع نظام التصميم الموحد والألوان الديناميكية.
 */
@Composable
fun AboutAppDialogView(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themeColors.background,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = themeColors.accent.copy(alpha = 0.6f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.primary)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "معلومات عن التطبيق",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }
}

/**
 * محتوى شاشة نبذة عن التطبيق
 */
@Composable
fun AboutAppScreenContent(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (adminRole != "GUEST") {
            var isEditingAboutPanel by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "👑 تنسيق وترتيب عناصر شاشة (عن التطبيق)",
                            color = themeColors.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { isEditingAboutPanel = !isEditingAboutPanel }, modifier = Modifier.size(28.dp)) {
                            Text(if (isEditingAboutPanel) "🔽" else "⚙️", fontSize = 14.sp)
                        }
                    }

                    if (isEditingAboutPanel) {
                        Divider(color = Color.White.copy(alpha = 0.15f))

                        Text("1. ترتيب ظهور العناصر:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val keyLabels = mapOf(
                            "COVER" to "🖼️ غلاف التطبيق",
                            "LOGO" to "🔴 شعار التطبيق",
                            "TITLE" to "🏷️ اسم التطبيق",
                            "ANNOUNCEMENT" to "📢 إعلان المنصة",
                            "ABOUT_CARD" to "ℹ️ كارت نبذة عن التطبيق",
                            "DOWNLOAD_BTN" to "📥 زر تحميل وتحديث التطبيق",
                            "CONTACTS" to "📞 أرقام وتثبيت الدعم",
                            "SOCIALS" to "🌐 شبكات التواصل الاجتماعي"
                        )

                        val currentList = remember(settingsState.aboutLayoutOrder) {
                            settingsState.aboutLayoutOrder.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableList()
                        }

                        currentList.forEachIndexed { index, k ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeColors.background, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(keyLabels[k] ?: k, color = Color.White, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (index > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(themeColors.primary)
                                                .clickable {
                                                    val newList = currentList.toMutableList()
                                                    val tmp = newList[index]
                                                    newList[index] = newList[index - 1]
                                                    newList[index - 1] = tmp
                                                    viewModel.saveCustomSettingsState(settingsState.copy(aboutLayoutOrder = newList.joinToString(",")))
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("⬆️ تقديم", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (index < currentList.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(themeColors.surface)
                                                .clickable {
                                                    val newList = currentList.toMutableList()
                                                    val tmp = newList[index]
                                                    newList[index] = newList[index + 1]
                                                    newList[index + 1] = tmp
                                                    viewModel.saveCustomSettingsState(settingsState.copy(aboutLayoutOrder = newList.joinToString(",")))
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("⬇️ تأخير", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. النص المخصص في كارت حول التطبيق:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        var customTextTemp by remember(settingsState.aboutCustomInfo) { mutableStateOf(settingsState.aboutCustomInfo) }
                        OutlinedTextField(
                            value = customTextTemp,
                            onValueChange = { customTextTemp = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                        )
                        Button(
                            onClick = {
                                viewModel.saveCustomSettingsState(settingsState.copy(aboutCustomInfo = customTextTemp))
                                viewModel.triggerNotification("💾 تم تحديث وحفظ نص شاشة عن التطبيق!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("💾 حفظ النص المخصص", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Parse layout order from settings state
        val orderKeys = settingsState.aboutLayoutOrder
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }

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
                "LOGO" -> {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(6.dp, CircleShape)
                            .background(themeColors.primary, CircleShape)
                            .border(3.dp, themeColors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇾🇪",
                            fontSize = 44.sp
                        )
                    }
                }
                "TITLE" -> {
                    Text(
                        text = settingsState.appName.ifBlank { "دليل خدمات اليمن" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                "ANNOUNCEMENT" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
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
                                text = settingsState.bannerContent.ifEmpty { "منصة شاملة تربط مقدمي الخدمات بالعملاء في جميع محافظات اليمن" },
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
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
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
                                    text = "حول تطبيق دليل ${settingsState.appName.ifBlank { "خدمات اليمن" }}",
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
                                    text = settingsState.appVersion.ifBlank { "v2.5.0" },
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
                                    text = settingsState.encryptionType.ifBlank { "End-to-End Encrypted" },
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settingsState.appDownloadUrl.ifBlank { "https://example.com/download" }))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تحميل وتحديث التطبيق",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📥 تحميل وتحديث التطبيق المباشر",
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
                                    val phone = settingsState.supportWhatsapp.ifBlank { "967770000000" }
                                    val url = "https://wa.me/$phone"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try { context.startActivity(intent) } catch(e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("💬 واتساب الدعم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val phone = settingsState.supportPhone.ifBlank { "770000000" }
                                    val uri = Uri.parse("tel:$phone")
                                    val intent = Intent(Intent.ACTION_DIAL, uri)
                                    try { context.startActivity(intent) } catch(e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📞 اتصال الدعم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                color = themeColors.accent,
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
                                            modifier = Modifier.weight(1f).height(42.dp),
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
            }
        }

        if (settingsState.supportEmail.isNotBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(settingsState.supportEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "استفسار بخصوص ${settingsState.appName}")
                    }
                    try { context.startActivity(intent) } catch(e: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("✉️ مراسلة الإدارة: ${settingsState.supportEmail}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}
