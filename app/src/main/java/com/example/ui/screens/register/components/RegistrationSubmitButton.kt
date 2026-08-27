package com.example.ui.screens.register.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * 🚀 RegistrationSubmitButton - زر إرسال موحد مع مؤشر تحميل وحالة التعطيل
 */
@Composable
fun RegistrationSubmitButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    loadingText: String = "جاري إرسال الطلب...",
    enabled: Boolean = true,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = themeColors.accent,
            disabledContainerColor = themeColors.accent.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = loadingText,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        } else {
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
        }
    }
}
