package com.example.ui.screens.admin

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminReviewsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_REVIEWS")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⭐ إدارة التقييمات والتعليقات والتحققات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { adminReviewSubTabState.value = "PENDING" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (adminReviewSubTabState.value == "PENDING") themeColors.primary else themeColors.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("قيد المراجعة ⏳", fontSize = 11.sp, color = Color.White)
                }
                Button(
                    onClick = { adminReviewSubTabState.value = "APPROVED" },
                    colors = ButtonDefaults.buttonColors(containerColor = if (adminReviewSubTabState.value == "APPROVED") themeColors.primary else themeColors.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("المعتمدة ✅", fontSize = 11.sp, color = Color.White)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("لا توجد تقييمات جديدة في هذه الفئة حالياً.", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        }
    }
}
