package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🎨 Notification / Feedback Message Types
 */
enum class SnackbarType(val containerColor: Color, val contentColor: Color, val icon: ImageVector) {
    SUCCESS(Color(0xFF10B981), Color.White, Icons.Default.CheckCircle),
    ERROR(Color(0xFFEF4444), Color.White, Icons.Default.Close),
    WARNING(Color(0xFFF59E0B), Color.Black, Icons.Default.Warning),
    INFO(Color(0xFF3B82F6), Color.White, Icons.Default.Info)
}

/**
 * Custom Visuals supporting typed colors
 */
class CustomSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.INFO
) : SnackbarVisuals

/**
 * Helper to show typed snackbar on SnackbarHostState
 */
suspend fun SnackbarHostState.showCustomSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    return showSnackbar(
        CustomSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = false,
            duration = duration,
            type = type
        )
    )
}

/**
 * Custom styled SnackbarHost composable
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val customVisuals = data.visuals as? CustomSnackbarVisuals
        val type = customVisuals?.type ?: SnackbarType.INFO

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = type.containerColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = type.contentColor,
                    modifier = Modifier.size(22.dp)
                )

                Text(
                    text = data.visuals.message,
                    color = type.contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (data.visuals.actionLabel != null) {
                    TextButton(
                        onClick = { data.performAction() },
                        colors = ButtonDefaults.textButtonColors(contentColor = type.contentColor)
                    ) {
                        Text(
                            text = data.visuals.actionLabel!!,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
