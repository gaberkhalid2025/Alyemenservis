package com.example.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.models.MediaType
import com.example.ui.components.InteractiveMapLocationPickerDialog
import com.example.utils.OfflineYemenGazetteer
import java.io.File

@Composable
internal fun ChatAttachmentDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    surfaceColor: Color,
    textPrimary: Color,
    accentColor: Color,
    context: Context,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onLaunchCamera: (File, Uri) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onSendCurrentLocation: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    onSendLiveLocation: (Location) -> Unit,
    onOpenMapPicker: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(surfaceColor)
    ) {
        DropdownMenuItem(
            text = { Text("🖼️ اختيار صورة من المعرض", color = textPrimary, fontSize = 13.sp) },
            onClick = {
                onDismiss()
                onPickImage()
            },
            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = accentColor) }
        )
        DropdownMenuItem(
            text = { Text("🎥 اختيار فيديو من المعرض", color = textPrimary, fontSize = 13.sp) },
            onClick = {
                onDismiss()
                onPickVideo()
            },
            leadingIcon = { Text("🎬", fontSize = 16.sp) }
        )
        DropdownMenuItem(
            text = { Text("📷 التقاط صورة بالكاميرا", color = textPrimary, fontSize = 13.sp) },
            onClick = {
                onDismiss()
                val hasCameraPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPerm) {
                    val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(sharedDir, "camera_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    onLaunchCamera(file, uri)
                } else {
                    onRequestCameraPermission()
                }
            },
            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = accentColor) }
        )
        DropdownMenuItem(
            text = { Text("📍 إرسال موقعي الحالي (خريطة التطبيق)", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
            onClick = {
                onDismiss()
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    onSendCurrentLocation()
                } else {
                    onRequestLocationPermissions()
                }
            },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444)) }
        )
        DropdownMenuItem(
            text = { Text("🟢 بث موقعي الحي المباشر (خريطة التطبيق)", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
            onClick = {
                onDismiss()
                val currentLoc = getBestLastKnownLocation(context)
                if (currentLoc != null) {
                    onSendLiveLocation(currentLoc)
                } else {
                    onOpenMapPicker()
                }
            },
            leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF34D399)) }
        )
        DropdownMenuItem(
            text = { Text("🗺️ تحديد موقع من خريطة التطبيق", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
            onClick = {
                onDismiss()
                onOpenMapPicker()
            },
            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF10B981)) }
        )
    }
}

@Composable
internal fun ChatImagePreviewDialog(
    activePreviewUri: Uri,
    imageCaption: String,
    onCaptionChange: (String) -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    onSend: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "معاينة الصورة قبل الإرسال 🖼️",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = activePreviewUri,
                    contentDescription = "معاينة الصورة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentScale = ContentScale.Fit
                )

                OutlinedTextField(
                    value = imageCaption,
                    onValueChange = { if (it.length <= 200) onCaptionChange(it) },
                    placeholder = { Text("إضافة تعليق على الصورة...", color = textSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = borderColor
                    ),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(activePreviewUri, imageCaption.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(
                    text = "إرسال الصورة 🚀",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = textSecondary)
            }
        },
        containerColor = surfaceColor
    )
}

@Composable
internal fun ChatVideoPreviewDialog(
    activePreviewVideoUri: Uri,
    videoCaption: String,
    onCaptionChange: (String) -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    onSend: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "معاينة الفيديو قبل الإرسال 🎥",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "فيديو محدد",
                            tint = Color.White,
                            modifier = Modifier
                                .size(54.dp)
                                .background(primaryColor, CircleShape)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("فيديو محدد للإرسال", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = videoCaption,
                    onValueChange = { if (it.length <= 200) onCaptionChange(it) },
                    placeholder = { Text("إضافة تعليق على الفيديو...", color = textSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = borderColor
                    ),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(activePreviewVideoUri, videoCaption.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(
                    text = "إرسال الفيديو 🚀",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = textSecondary)
            }
        },
        containerColor = surfaceColor
    )
}

@Composable
internal fun ChatMapLocationPickerHost(
    context: Context,
    onDismiss: () -> Unit,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit
) {
    val currentLoc = getBestLastKnownLocation(context)
    InteractiveMapLocationPickerDialog(
        initialLat = currentLoc?.latitude ?: 15.3694,
        initialLng = currentLoc?.longitude ?: 44.1910,
        hasGps = currentLoc != null,
        gpsLat = currentLoc?.latitude ?: 0.0,
        gpsLng = currentLoc?.longitude ?: 0.0,
        title = "🗺️ تحديد وإرسال موقع على خريطة التطبيق",
        onDismiss = onDismiss,
        onLocationPicked = { picked ->
            val summary = picked.landmarkName.ifBlank { picked.label.ifBlank { picked.formattedAddress.ifBlank { "موقع محدد على الخريطة" } } }
            val appUri = OfflineYemenGazetteer.buildAppLocationUri(picked.latitude, picked.longitude, summary)
            val displayText = buildString {
                append("📍 موقع جغرافي: ")
                append(summary)
                if (picked.note.isNotBlank()) {
                    append(" — ")
                    append(picked.note)
                }
            }
            onSendMessage(displayText, MediaType.LOCATION, appUri)
            Toast.makeText(context, "✅ تم إرسال الموقع عبر خريطة التطبيق", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    )
}

internal fun getBestLastKnownLocation(context: Context): Location? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null

        var best: Location? = null
        for (provider in locationManager.getProviders(true)) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (best == null || l.accuracy < best.accuracy) {
                best = l
            }
        }
        best
    } catch (_: Exception) {
        null
    }
}

internal fun sendCurrentLocation(
    context: Context,
    onSendMessage: (text: String, mediaType: MediaType, mediaUrl: String) -> Unit,
    onFallbackToPicker: () -> Unit
) {
    try {
        val location = getBestLastKnownLocation(context)
        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val summary = OfflineYemenGazetteer.nearestReferenceSummary(context, lat, lng).ifBlank { "موقعي الجغرافي المباشر" }
            val appUri = OfflineYemenGazetteer.buildAppLocationUri(lat, lng, summary)
            onSendMessage("📍 موقعي: $summary", MediaType.LOCATION, appUri)
            Toast.makeText(context, "✅ تم إرسال موقعك عبر خريطة التطبيق", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "📍 تعذر التقاط إشارة GPS حالياً، حدد موقعك يدوياً على خريطة التطبيق", Toast.LENGTH_LONG).show()
            onFallbackToPicker()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "تعذر تحديد الموقع تلقائياً، اختره من الخريطة", Toast.LENGTH_SHORT).show()
        onFallbackToPicker()
    }
}
