package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun ChatRoutingPanel(
    themeColors: VisualThemePalette,
    currentRouter: String = "ADMIN", // ADMIN, BRANCH, ADMIN_AND_BRANCH
    onRouterChanged: (String) -> Unit
) {
    var selectedRoute by remember { mutableStateOf(currentRouter) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("🔀 توجيه المحادثات الذكي (تحكم إداري):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("ADMIN" to "للإدارة فقط", "BRANCH" to "للمركز الفني", "BOTH" to "إدارة + مركز").forEach { (route, label) ->
                val isSel = selectedRoute == route
                Button(
                    onClick = {
                        selectedRoute = route
                        onRouterChanged(route)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) themeColors.accent else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(label, fontSize = 9.sp, color = if (isSel) androidx.compose.ui.graphics.Color.Black else themeColors.textPrimary)
                }
            }
        }
    }
}
