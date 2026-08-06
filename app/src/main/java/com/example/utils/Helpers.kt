package com.example.utils

import com.example.utils.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.example.data.AdminSettingsEntity
import com.example.data.ProviderEntity
import java.io.ByteArrayOutputStream

data class VisualThemePalette(
    val activeId: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val gradientBrush: Brush,
    val scheme: ColorScheme
)

data class PresetPalette(
    val name: String,
    val primaryHex: String,
    val secondaryHex: String,
    val bgHex: String,
    val surfaceHex: String
)

val colorsPresetsList = listOf(
    PresetPalette("🌌 الكوني الفضي", "#9CA3AF", "#374151", "#111827", "#1F2937"),
    PresetPalette("✨ ذهبي فاخر", "#D4AF37", "#FFD700", "#1A1A1A", "#2D2D2D"),
    PresetPalette("🟢 زمردي راقي", "#004B49", "#50C878", "#0C1814", "#152A20"),
    PresetPalette("⚫ الأسود الدخاني", "#121212", "#333333", "#080808", "#101010")
)

fun getBookingTimestamp(): Long = System.currentTimeMillis()

fun resolveThemePalette(settings: AdminSettingsEntity): VisualThemePalette {
    return when (settings.activeThemeId) {
        "COSMIC_SILVER" -> {
            val primary = Color(0xFF9CA3AF)
            val secondary = Color(0xFF374151)
            val background = Color(0xFF111827)
            val surface = Color(0xFF1F2937)
            val textPrimary = Color(0xFFF9FAFB)
            val textSecondary = Color(0xFFD1D5DB)
            val accent = Color(0xFFE5E7EB)
            VisualThemePalette(
                activeId = "COSMIC_SILVER",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        "LUXURY_GOLD" -> {
            val primary = Color(0xFFD97706)
            val secondary = Color(0xFF451A03)
            val background = Color(0xFF0F0F10)
            val surface = Color(0xFF1C1917)
            val textPrimary = Color(0xFFFFFAFA)
            val textSecondary = Color(0xFFE7E5E4)
            val accent = Color(0xFFFBBF24)
            VisualThemePalette(
                activeId = "LUXURY_GOLD",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF292524), Color(0xFF0F0F10))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        "ELITE_EMERALD" -> {
            val primary = Color(0xFF059669)
            val secondary = Color(0xFF047857)
            val background = Color(0xFF022C22)
            val surface = Color(0xFF064E3B)
            val textPrimary = Color(0xFFF0FDF4)
            val textSecondary = Color(0xFFA7F3D0)
            val accent = Color(0xFF34D399)
            VisualThemePalette(
                activeId = "ELITE_EMERALD",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF022C22))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
        else -> {
            val primary = Color(0xFF2563EB)
            val secondary = Color(0xFF1D4ED8)
            val background = Color(0xFF0F172A)
            val surface = Color(0xFF1E293B)
            val textPrimary = Color(0xFFF8FAFC)
            val textSecondary = Color(0xFF94A3B8)
            val accent = Color(0xFF38BDF8)
            VisualThemePalette(
                activeId = "ROYAL_BLUE",
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                gradientBrush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                scheme = darkColorScheme(primary = primary, secondary = secondary, background = background, surface = surface)
            )
        }
    }
}

fun getCityCenterCoords(cityId: String): Pair<Double, Double> {
    return when (cityId) {
        "ye_san" -> Pair(15.3694, 44.1910)
        "ye_ade" -> Pair(12.7855, 45.0186)
        "ye_tai" -> Pair(13.5794, 44.0205)
        "ye_hod" -> Pair(14.7979, 42.9530)
        else -> Pair(15.3694, 44.1910)
    }
}

fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    return try {
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        results[0]
    } catch (e: Exception) {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        (6371000 * c).toFloat()
    }
}

fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
        "${meters.toInt()} م"
    } else {
        val km = meters / 1000f
        String.format(java.util.Locale.US, "%.1f كم", km)
    }
}

fun getAreaCoords(areaName: String): Pair<Double, Double> {
    return when {
        areaName.contains("تعز") -> Pair(13.5794, 44.0135)
        areaName.contains("عدن") -> Pair(12.7855, 45.0186)
        areaName.contains("الحديدة") -> Pair(14.7979, 42.9530)
        areaName.contains("حضرموت") || areaName.contains("المكلا") -> Pair(14.5424, 49.1242)
        else -> Pair(15.3694, 44.1910)
    }
}

fun getProviderCoords(provider: ProviderEntity): Pair<Double, Double> {
    if (provider.latitude != 0.0 && provider.longitude != 0.0) {
        return Pair(provider.latitude, provider.longitude)
    }
    val baseStr = if (provider.area.isNotEmpty()) provider.area else provider.cityId
    val base = getAreaCoords(baseStr)
    val hash = provider.id.hashCode().toDouble()
    val offsetLat = (hash % 100) / 1000.0
    val offsetLng = ((hash / 100) % 100) / 1000.0
    return Pair(base.first + offsetLat, base.second + offsetLng)
}

fun getProviderCoords(providerId: String): Pair<Double, Double> {
    return when (providerId) {
        "p_amin" -> Pair(15.3694, 44.1910)
        else -> {
            val hash = providerId.hashCode().toDouble()
            val offsetLat = (hash % 100) / 1000.0
            val offsetLng = ((hash / 100) % 100) / 1000.0
            Pair(15.3694 + offsetLat, 44.1910 + offsetLng)
        }
    }
}

fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return 6371.0 * c
}

fun convertUriToBase64(context: Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val reqWidth = 220
        val reqHeight = 220
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val nextInputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val decodedBitmap = BitmapFactory.decodeStream(nextInputStream, null, finalOptions)
        nextInputStream.close()

        if (decodedBitmap != null) {
            val scaledBitmap = if (decodedBitmap.width > reqWidth || decodedBitmap.height > reqHeight) {
                val ratio = Math.min(reqWidth.toFloat() / decodedBitmap.width, reqHeight.toFloat() / decodedBitmap.height)
                Bitmap.createScaledBitmap(
                    decodedBitmap,
                    (decodedBitmap.width * ratio).toInt(),
                    (decodedBitmap.height * ratio).toInt(),
                    true
                )
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else ""
    } catch (e: Exception) {
        ""
    }
}

fun convertGenericUriToBase64(context: Context, uri: Uri): String = convertUriToBase64(context, uri)

fun convertBitmapToBase64(bitmap: Bitmap): String {
    return try {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

fun compressAndResizeImageUri(context: Context, uri: Uri): String = convertUriToBase64(context, uri)

fun isMoreThan8HoursBefore(timestamp: Long): Boolean {
    val diff = timestamp - System.currentTimeMillis()
    return diff > (8 * 60 * 60 * 1000)
}
