package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity

/**
 * 🛡️ FloatingActionButtonsHandler & Scroll-Guard:
 * Manages dynamic floating action buttons ("المساعد الذكي" & "اطلب خدمتك الآن").
 * Enforces automatic hide on Auth screens, Forms, & Detail Edit screens to prevent UI overlap.
 */

class FabScrollConnection(
    private val onScrollDown: () -> Unit,
    private val onScrollUp: () -> Unit
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < -12f) {
            onScrollDown()
        } else if (available.y > 12f) {
            onScrollUp()
        }
        return Offset.Zero
    }
}

@Composable
fun FloatingActionButtonsHandler(
    currentRoute: String,
    settings: AdminSettingsEntity,
    onOpenAssistant: () -> Unit,
    onOpenUrgentRequest: () -> Unit,
    isScrollVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 1. Mandatory Route Guard List: Auth, Forms, Detail Edit, Call, and Chat screens
    val guardedRoutes = remember {
        listOf(
            "login",
            "register",
            "edit_profile",
            "edit_store",
            "edit_property",
            "add_store",
            "add_property",
            "add_provider",
            "call_screen",
            "chat_screen",
            "booking_form",
            "payment_screen"
        )
    }

    val isGuarded = guardedRoutes.any { currentRoute.contains(it, ignoreCase = true) }

    // If global setting is OFF or route is guarded, HIDE FAB completely
    if (isGuarded || settings.assistantHidden || !settings.isAssistantIconVisible) {
        return
    }

    val fabSize: Dp = settings.assistantSize.dp.coerceAtLeast(44.dp)
    val opacity = if (isScrollVisible) 0.95f else 0.0f

    val shape = when (settings.avatarShape.uppercase()) {
        "SQUARE" -> RoundedCornerShape(8.dp)
        "ROUNDED" -> RoundedCornerShape(16.dp)
        else -> CircleShape
    }

    AnimatedVisibility(
        visible = isScrollVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(bottom = 80.dp, end = 16.dp)
                .alpha(opacity)
        ) {
            // Floating Button 1: "المساعد الذكي"
            Surface(
                onClick = onOpenAssistant,
                shape = shape,
                color = Color(0xFF0284C7),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(fabSize)
                    .border(1.5.dp, Color(0xFF38BDF8), shape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "🤖",
                        fontSize = (settings.chatFontSizeSp + 4).sp
                    )
                }
            }

            // Floating Button 2: "اطلب خدمتك الآن ⚡"
            Surface(
                onClick = onOpenUrgentRequest,
                shape = shape,
                color = Color(0xFFD97706),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(fabSize)
                    .border(1.5.dp, Color(0xFFFBBF24), shape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "⚡",
                        fontSize = (settings.chatFontSizeSp + 4).sp
                    )
                }
            }
        }
    }
}
