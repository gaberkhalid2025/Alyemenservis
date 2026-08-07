@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.about




import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.ui.screens.chat.*
import com.example.ui.screens.notifications.*
import com.example.ui.screens.dashboard.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// ------ About App Info Dialog overlay ------
@Composable
fun AboutAppDialogView(viewModel: MainViewModel, themeColors: VisualThemePalette, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = themeColors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top header bar for the full-screen about page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.primary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "معلومات عن التطبيق",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    AboutAppScreenContent(viewModel = viewModel, themeColors = themeColors)
                }
            }
        }
    }
}



// ------ Unused screen layouts defined as secondary ------
@Composable
fun AboutAppScreenContent(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val settingsState by viewModel.settings.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val context = LocalContext.current
    val isOnline = com.example.NetworkUtils.isNetworkAvailable(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        if (adminRole != "GUEST") {
            var isEditingAboutPanel by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑 تنسيق وترتيب عناصر شاشة (عن التطبيق)", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { isEditingAboutPanel = !isEditingAboutPanel }, modifier = Modifier.size(28.dp)) {
                            Text(if (isEditingAboutPanel) "🔽" else "⚙️", fontSize = 14.sp)
                        }
                    }

                    if (isEditingAboutPanel) {
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        
                        Text("1. ترتيب ظهور العناصر (اضغط على الأسهم للتقديم أو التأخير):", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val keyLabels = mapOf(
                            "COVER" to "🖼️ غلاف التطبيق",
                            "LOGO" to "🔴 شعار WAM",
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
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
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
                                                .background(Color(0xFF334155))
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
                        Text("2. النص المخصص في كارت حول التطبيق:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
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
                    // Cover container (Dynamic)
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
                                coil.compose.AsyncImage(
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
                    // Beautiful red WAM circle logo with border and subtle elevation shadow
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
                    // 📢 Platform Announcement Card
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
                    // ℹ️ About Platform Card
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
                    // 📥 💾 Download / Update Button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settingsState.appDownloadUrl.ifBlank { "https://example.com/download_app" }))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
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
                            // Whatsapp Support Button
                            Button(
                                onClick = {
                                    val url = "https://wa.me/${settingsState.supportWhatsapp}"
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

                            // Call Support Button
                            Button(
                                onClick = {
                                    val uri = Uri.parse("tel:${settingsState.supportPhone}")
                                    val intent = Intent(Intent.ACTION_DIAL, uri)
                                    try { context.startActivity(intent) } catch(e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
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
                                color = Color(0xFFFFD700),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Render socials in chunks of 2 for grid layout
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
                                    // Add spacer to align if single item in last row
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

        // Email Support Button
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B5563)),
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
