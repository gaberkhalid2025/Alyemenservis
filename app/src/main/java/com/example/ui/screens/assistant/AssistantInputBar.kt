package com.example.ui.screens.assistant

import androidx.compose.foundation.background
import com.example.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

/**
 * ⌨️ AssistantInputBar - Bottom input field and send button in Assistant Screen
 */
@Composable
fun AssistantInputBar(
    typedText: String,
    isGenerating: Boolean,
    themeColors: VisualThemePalette,
    onTypedTextChanged: (String) -> Unit,
    onSendQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = themeColors.surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = onTypedTextChanged,
                placeholder = {
                    Text(
                        text = "اكتب سؤالك أو اطلب خدمة هنا...",
                        fontSize = 12.sp,
                        color = themeColors.textSecondary,
                        maxLines = 1
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = themeColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (typedText.isNotBlank() && !isGenerating) {
                            onSendQuery()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = themeColors.border,
                    focusedContainerColor = themeColors.background,
                    unfocusedContainerColor = themeColors.background,
                    cursorColor = themeColors.accent
                )
            )

            IconButton(
                onClick = onSendQuery,
                enabled = typedText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (typedText.isNotBlank()) themeColors.accent else themeColors.border.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = if (typedText.isNotBlank()) Color.Black else themeColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
