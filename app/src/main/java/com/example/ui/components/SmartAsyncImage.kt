package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * 🌟 Universal Smart Image Renderer
 * Seamlessly handles:
 * 1. Firebase Storage / Web URLs via Coil with disk cache & memory cache
 * 2. Legacy Base64 image strings with graceful decoding
 * 3. Loading indicators and fallbacks
 */
@Composable
fun SmartAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackPlaceholderColor: Color = Color(0xFF1E293B),
    placeholderIconTint: Color = Color.Gray
) {
    val modelStr = model as? String ?: ""

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
                iconTint = placeholderIconTint
            )
        }
    } else if (modelStr.isNotBlank() || model != null) {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
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
                    iconTint = placeholderIconTint
                )
            }
        )
    } else {
        DefaultImageFallback(
            modifier = modifier,
            fallbackColor = fallbackPlaceholderColor,
            iconTint = placeholderIconTint
        )
    }
}

@Composable
private fun DefaultImageFallback(
    modifier: Modifier,
    fallbackColor: Color,
    iconTint: Color
) {
    Box(
        modifier = modifier.background(fallbackColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
