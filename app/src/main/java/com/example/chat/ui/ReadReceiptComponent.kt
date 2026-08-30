package com.example.chat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chat.domain.MessageStatus

/**
 * 👁️‍🗨️ ReadReceiptComponent
 * Highly responsive visualizer for chat message delivery and read receipts.
 * - SENDING: Animated mini spinner
 * - SENT: Single gray checkmark
 * - DELIVERED: Double gray checkmarks
 * - READ: Double cyan/blue checkmarks
 * - FAILED: Red warning exclamation with retry trigger
 */
@Composable
fun ReadReceiptComponent(
    status: MessageStatus,
    iconSize: Dp = 13.dp,
    readColor: Color = Color(0xFF00E5FF),
    sentColor: Color = Color(0xFF94A3B8),
    failedColor: Color = Color(0xFFEF4444),
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            MessageStatus.SENDING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize - 2.dp),
                    color = sentColor,
                    strokeWidth = 1.5.dp
                )
            }
            MessageStatus.SENT -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "تم الإرسال",
                    tint = sentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            MessageStatus.DELIVERED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم التسليم",
                        tint = sentColor,
                        modifier = Modifier.size(iconSize)
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = sentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
            MessageStatus.READ -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تمت القراءة",
                        tint = readColor,
                        modifier = Modifier.size(iconSize)
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = readColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
            MessageStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "فشل الإرسال - اضغط لإعادة المحاولة",
                    tint = failedColor,
                    modifier = Modifier
                        .size(iconSize + 2.dp)
                        .then(
                            if (onRetryClick != null) Modifier.clickable { onRetryClick() } else Modifier
                        )
                )
            }
        }
    }
}
