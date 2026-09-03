package com.example.chat.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.domain.MessageType

/**
 * 🔍 ChatSearchBar
 * Advanced real-time search and filter composable for chat history.
 * Supports query input, match counter (X of Y), match stepping (Next/Prev),
 * and media filter chips.
 */
@Composable
fun ChatSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilterType: MessageType?,
    onFilterTypeSelected: (MessageType?) -> Unit,
    matchCount: Int = 0,
    currentMatchIndex: Int = 0,
    onNextMatch: () -> Unit = {},
    onPrevMatch: () -> Unit = {},
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Search Input Field Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "إغلاق البحث",
                    tint = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "بحث في الرسائل...",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Match Navigator Buttons (shown if results exist)
            if (query.isNotBlank() && matchCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentMatchIndex + 1}/$matchCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    IconButton(
                        onClick = onPrevMatch,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "السابق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextMatch,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "التالي",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Media Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SearchFilterChip(
                label = "الكل",
                icon = "💬",
                isSelected = selectedFilterType == null,
                onClick = { onFilterTypeSelected(null) }
            )
            SearchFilterChip(
                label = "الصور",
                icon = "📷",
                isSelected = selectedFilterType == MessageType.IMAGE,
                onClick = { onFilterTypeSelected(MessageType.IMAGE) }
            )
            SearchFilterChip(
                label = "الصوتيات",
                icon = "🎙️",
                isSelected = selectedFilterType == MessageType.AUDIO,
                onClick = { onFilterTypeSelected(MessageType.AUDIO) }
            )
            SearchFilterChip(
                label = "المستندات",
                icon = "📄",
                isSelected = selectedFilterType == MessageType.DOCUMENT || selectedFilterType == MessageType.PDF,
                onClick = { onFilterTypeSelected(MessageType.PDF) }
            )
            SearchFilterChip(
                label = "جهات اتصال",
                icon = "👤",
                isSelected = selectedFilterType == MessageType.CONTACT,
                onClick = { onFilterTypeSelected(MessageType.CONTACT) }
            )
            SearchFilterChip(
                label = "المواقع",
                icon = "📍",
                isSelected = selectedFilterType == MessageType.LOCATION,
                onClick = { onFilterTypeSelected(MessageType.LOCATION) }
            )
        }
    }
}

@Composable
private fun SearchFilterChip(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)
        ),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 11.sp)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8)
            )
        }
    }
}
