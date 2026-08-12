package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun ChatMediaUploader(
    themeColors: VisualThemePalette,
    onMediaSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var uploadProgress by remember { mutableFloatStateOf(0.0f) }
    var isUploading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColors.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🖼️ إرفاق وسائط وصور في المحادثة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)

        if (isUploading) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    color = themeColors.accent,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("جاري الضغط والرفع التلقائي للوسائط: ${(uploadProgress * 100).toInt()}%", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isUploading = true
                        uploadProgress = 0.3f
                        // Simulate delayed complete
                        onMediaSelected("https://firebasestorage.googleapis.com/v0/b/mock/o/chat_media.jpg")
                        isUploading = false
                        Toast.makeText(context, "تم إرفاق الصورة وضغطها بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("رفع صورة 📸", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "تم إرفاق ملف الموقع الجغرافي بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("موقع خريطة 📍", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
