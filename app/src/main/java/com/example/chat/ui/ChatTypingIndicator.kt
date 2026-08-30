package com.example.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ⌨️ ChatTypingIndicator
 * UI representation of debounced typing status (minimizes server writes).
 */
@Composable
fun ChatTypingIndicator(isTyping: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = isTyping, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "يكتب الآن...",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary
            )
            // Animated Lottie dots could be placed here for 10/10 polish
        }
    }
}
