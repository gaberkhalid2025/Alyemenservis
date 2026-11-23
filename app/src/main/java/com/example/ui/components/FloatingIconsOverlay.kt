package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.utils.VisualThemePalette

/**
 * 🌟 High-Craft Circular Floating Action Buttons (FABs)
 * بمحاذاة الشريط السفلي تماماً (بدون تداخل أو ارتفاع زائد):
 * 1. "اطلب خدمتك الآن" (جهة البداية): أيقونة دائرية أنيقة باسمها بداخلها.
 * 2. "المساعد الذكي" (جهة النهاية): أيقونة دائرية أنيقة باسمها بداخلها.
 */
@Composable
fun BoxScope.FloatingIconsOverlay(
    settings: AdminSettingsEntity,
    themeColors: VisualThemePalette,
    isClientUser: Boolean = true,
    onAssistantClick: () -> Unit,
    onRequestServiceClick: () -> Unit
) {
    // 1. زر "اطلب خدمتك الآن" الدائري بمحاذاة الشريط السفلي في إحدى الجهتين
    if (!settings.footerMessage.contains("hide_urgent_fab")) {
        Surface(
            onClick = onRequestServiceClick,
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 6.dp)
                .size(66.dp)
                .shadow(10.dp, CircleShape)
                .semantics { contentDescription = "اطلب خدمتك الآن" }
                .testTag("fab_quick_service_request")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
                        ),
                        shape = CircleShape
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "اطلب خدمتك\nالآن",
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }

    // 2. زر "المساعد الذكي" الدائري بمحاذاة الشريط السفلي في الجهة المقابلة
    if (!settings.assistantHidden) {
        val assistantFabSize = settings.assistantSize.coerceIn(48, 84).dp
        Surface(
            onClick = onAssistantClick,
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 6.dp)
                .size(assistantFabSize)
                .shadow(10.dp, CircleShape)
                .semantics { contentDescription = "المساعد الذكي" }
                .testTag("fab_smart_assistant")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8))
                        ),
                        shape = CircleShape
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "المساعد\nالذكي",
                        fontSize = 9.5.sp,
                        lineHeight = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
