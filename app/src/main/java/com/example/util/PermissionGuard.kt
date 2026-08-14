package com.example.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PermissionGuard {
    const val PERMISSION_BOOKINGS = "bookings"
    const val PERMISSION_RESTAURANTS = "restaurants"

    fun hasPermission(role: AdminRole, permission: String): Boolean {
        if (role == AdminRole.GUEST) return false
        // Let's allow SUPER_ADMIN and ADMIN, and optionally SUPERVISOR depending on permission
        return when (permission) {
            PERMISSION_BOOKINGS -> role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN || role == AdminRole.SUPERVISOR
            PERMISSION_RESTAURANTS -> role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN || role == AdminRole.SUPERVISOR
            else -> false
        }
    }

    @Composable
    fun UnauthorizedView() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "عذراً، ليس لديك الصلاحية الكافية لعرض هذه الصفحة.",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
