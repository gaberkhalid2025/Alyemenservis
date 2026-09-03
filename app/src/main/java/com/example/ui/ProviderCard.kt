package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProviderCard(
    provider: Any? = null,
    themeColors: Any? = null,
    viewModel: Any? = null,
    onChatOpen: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "مزود الخدمة / فني", style = MaterialTheme.typography.titleMedium)
        }
    }
}
