package com.example.ui.screens.assistant
import com.example.ui.MainViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceManager

import com.example.ui.ProviderCard
import com.example.utils.VisualThemePalette

/**
 * 💬 AssistantMessageItem - Individual message bubble item with provider cards suggestions and voice output option.
 */
@Composable
fun AssistantMessageItem(
    msg: AssistantMessage,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    themeColors: VisualThemePalette,
    onRequestQuickService: () -> Unit,
    onNavigateToMap: () -> Unit,
    onChatOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.isUser) themeColors.primary else themeColors.surface
                ),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (msg.isUser) 12.dp else 2.dp,
                    bottomEnd = if (msg.isUser) 2.dp else 12.dp
                ),
                border = BorderStroke(
                    1.dp,
                    if (msg.isUser) themeColors.primary else themeColors.border
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = msg.text,
                        fontSize = 12.sp,
                        color = themeColors.textPrimary,
                        lineHeight = 18.sp
                    )

                    if (!msg.isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = themeColors.border)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .border(0.8.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                                    .clickable { onRequestQuickService() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("⚡ اطلب الآن", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .border(0.8.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToMap() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("📍 الخريطة", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = { VoiceManager.onSpeak?.invoke(msg.text) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "استماع",
                                    tint = themeColors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (msg.matchedProviders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "👇 الفنيين المقترحين لطلبك (اتصال / حجز مباشر):",
                fontSize = 11.sp,
                color = themeColors.accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                msg.matchedProviders.forEach { provider ->
                    ProviderCard(
                        provider = provider,
                        themeColors = themeColors,
                        viewModel = viewModel,
                        onChatOpen = onChatOpen
                    )
                }
            }
        }
    }
}
