package com.example.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat.domain.ChatMessage
import com.example.utils.VisualThemePalette

@Composable
fun AdvancedChatScreen(
    currentUserId: String,
    targetUserId: String,
    targetUserName: String,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    
    // Fake messages for demonstration of the new system until Firebase is hooked up
    val messages = remember {
        listOf(
            ChatMessage(id = "1", senderId = targetUserId, content = "مرحباً، هل الخدمة متاحة؟"),
            ChatMessage(id = "2", senderId = currentUserId, content = "أهلاً بك، نعم متوفرة تفضل.")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(targetUserName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                ChatTypingIndicator(isTyping = isTyping)
            }
        }

        if (searchQuery.isNotEmpty()) {
            ChatSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedFilterType = null,
                onFilterTypeSelected = {},
                onCloseSearch = { searchQuery = "" }
            )
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { msg ->
                ChatMessageItem(
                    message = msg,
                    isMe = msg.senderId == currentUserId
                )
            }
        }
        
        // Chat Input Bar Placeholder
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "مكان حقل إدخال الدردشة وتسجيل الصوت...",
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
