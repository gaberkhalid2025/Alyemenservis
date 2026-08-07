package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.BannerEntity
import com.example.ui.theme.VisualThemePalette
import kotlinx.coroutines.delay

@Composable
fun BannerSliderView(banners: List<BannerEntity>, themeColors: VisualThemePalette, onBannerClick: (String) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    
    val activeBanner = if (banners.isNotEmpty()) banners.getOrNull(currentIndex) else null
    
    LaunchedEffect(currentIndex, banners) {
        if (banners.isNotEmpty()) {
            val active = banners.getOrNull(currentIndex)
            val durationSec = if (active != null && active.duration > 0) active.duration else 5
            delay(durationSec * 1000L)
            currentIndex = (currentIndex + 1) % banners.size
        }
    }

    if (activeBanner != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onBannerClick(activeBanner.redirectCategory) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.secondary)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeBanner.type.uppercase()) {
                    "IMAGE" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            if (activeBanner.url.startsWith("data:image") || activeBanner.url.length > 200) {
                                val bitmap = remember(activeBanner.url) {
                                    try {
                                        val base64Data = if (activeBanner.url.contains(",")) activeBanner.url.substringAfter(",") else activeBanner.url
                                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        painter = BitmapPainter(bitmap.asImageBitmap()),
                                        contentDescription = activeBanner.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = activeBanner.url,
                                        contentDescription = activeBanner.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = activeBanner.url,
                                    contentDescription = activeBanner.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Overlay gradient for text legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            // Title & Label at bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (activeBanner.redirectCategory.isNotEmpty()) {
                                    Text(
                                        text = "اضغط للانتقال إلى قسم: ${activeBanner.redirectCategory}",
                                        fontSize = 9.sp,
                                        color = themeColors.accent
                                    )
                                }
                            }
                        } else {
                            // Text Fallback if URL is empty
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    "VIDEO" -> {
                        if (activeBanner.url.isNotEmpty()) {
                            val context = LocalContext.current
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoURI(android.net.Uri.parse(activeBanner.url))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(0f, 0f) // Silent looping banner
                                            start()
                                        }
                                        setOnErrorListener { _, _, _ -> true } // silent failure
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎬 فيديو مميز", fontSize = 9.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = activeBanner.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                        }
                    }
                    else -> { // TEXT banner
                        BannerTextFallback(activeBanner = activeBanner, themeColors = themeColors)
                    }
                }
            }
        }
    }
}

@Composable
fun BannerTextFallback(activeBanner: BannerEntity, themeColors: VisualThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        themeColors.primary,
                        themeColors.secondary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "إعلان ممتاز", tint = themeColors.accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إعلان رسمي دليل خدمات اليمن",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeBanner.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (activeBanner.redirectCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔗 الانتقال لقسم: ${activeBanner.redirectCategory}",
                    fontSize = 9.sp,
                    color = themeColors.accent.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
