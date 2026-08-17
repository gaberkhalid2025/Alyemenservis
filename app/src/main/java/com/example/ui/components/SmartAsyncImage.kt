package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * 🌟 Universal Smart Image Renderer
 * Seamlessly handles:
 * 1. Firebase Storage / Web URLs via Coil with disk cache & memory cache
 * 2. Legacy Base64 image strings with graceful decoding
 * 3. Contextual, attractive 3D icon fallbacks instead of empty grey boxes
 */
@Composable
fun SmartAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackPlaceholderColor: Color = Color(0xFF1E293B),
    placeholderIconTint: Color = Color.Gray,
    fallbackEmoji: String = "✨"
) {
    val modelStr = (model as? String)?.trim() ?: ""

    val isBase64 = remember(modelStr) {
        modelStr.isNotBlank() &&
                !modelStr.startsWith("http://") &&
                !modelStr.startsWith("https://") &&
                !modelStr.startsWith("content://") &&
                !modelStr.startsWith("file://")
    }

    if (isBase64) {
        val base64Bitmap: ImageBitmap? = remember(modelStr) {
            try {
                val cleanBase64 = if (modelStr.contains(",")) modelStr.substringAfter(",") else modelStr
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bmp?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        if (base64Bitmap != null) {
            Image(
                bitmap = base64Bitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            DefaultImageFallback(
                modifier = modifier,
                fallbackColor = fallbackPlaceholderColor,
                emoji = fallbackEmoji
            )
        }
    } else if (modelStr.isNotBlank() && (modelStr.startsWith("http://") || modelStr.startsWith("https://") || modelStr.startsWith("content://") || modelStr.startsWith("file://"))) {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(modelStr)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackPlaceholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFF59E0B),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                DefaultImageFallback(
                    modifier = Modifier.fillMaxSize(),
                    fallbackColor = fallbackPlaceholderColor,
                    emoji = fallbackEmoji
                )
            }
        )
    } else {
        DefaultImageFallback(
            modifier = modifier,
            fallbackColor = fallbackPlaceholderColor,
            emoji = fallbackEmoji
        )
    }
}

@Composable
private fun DefaultImageFallback(
    modifier: Modifier,
    fallbackColor: Color,
    emoji: String = "✨"
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        fallbackColor,
                        fallbackColor.copy(alpha = 0.7f),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji.ifBlank { "🌟" },
            fontSize = 24.sp
        )
    }
}

