package com.example.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.ManeuverType

/**
 * 🚘 وضع عارض الزجاج الأمامي HUD داخل شاشة الملاحة الحية.
 * يدعم قلب الصورة أفقياً (scaleX = -1f) لقراءتها كانعكاس على الزجاج الأمامي ليلاً.
 */
@Composable
fun HudMirrorOverlay(
    speedKmh: Int,
    distToManeuverText: String,
    instruction: String,
    maneuverType: ManeuverType?,
    mirrored: Boolean,
    onToggleMirror: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowIcon = when (maneuverType) {
        ManeuverType.LEFT, ManeuverType.SLIGHT_LEFT, ManeuverType.SHARP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        ManeuverType.RIGHT, ManeuverType.SLIGHT_RIGHT, ManeuverType.SHARP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        ManeuverType.UTURN, ManeuverType.ROUNDABOUT -> Icons.Default.Refresh
        ManeuverType.DESTINATION -> Icons.Default.Place
        else -> Icons.Default.Navigation
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .graphicsLayer(scaleX = if (mirrored) -1f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = distToManeuverText,
                color = Color(0xFF00E5FF),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$speedKmh",
                color = Color(0xFF10B981),
                fontSize = 86.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "كم / ساعة",
                color = Color(0xFF94A3B8),
                fontSize = 18.sp
            )
            Text(
                text = instruction,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleMirror,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp).testTag("hud_toggle_mirror_btn")
            ) {
                Text(
                    if (mirrored) "عادي" else "مرآة زجاجية 🪞",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp).testTag("hud_close_btn")
            ) {
                Text("إغلاق HUD", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
