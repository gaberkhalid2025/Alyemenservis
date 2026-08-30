package com.example.ui.screens.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatTypingIndicator(
    isTyping: Boolean,
    modifier: Modifier = Modifier
) {
    if (isTyping) {
        Row(
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "الطرف الآخر يكتب الآن...",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
