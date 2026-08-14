@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.utils.VisualThemePalette

@Composable
fun AdminRestaurantsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val adminRoleStr by viewModel.adminRole.collectAsState()
    if (!PermissionGuard.hasPermission(
            RoleManager.fromRoleString(adminRoleStr),
            PermissionGuard.PERMISSION_RESTAURANTS
        )
    ) {
        PermissionGuard.UnauthorizedView()
        return
    }

    val context = LocalContext.current
    val stores by viewModel.stores.collectAsState()
    val restaurants = stores.filter { it.sectionId == "restaurants" || it.categoryId.contains("restaurant") || it.name.contains("مطعم") || it.name.contains("كافيه") }

    var searchQuery by remember { mutableStateOf("") }
    val displayList = if (restaurants.isEmpty()) stores else restaurants
    val filteredList = displayList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🍔 إدارة المطاعم والكافيهات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "إدارة منشآت الأطعمة والمطاعم، توثيق الشارات، ترقية VIP، والتثبيت والحظر:",
                fontSize = 11.sp,
                color = themeColors.textSecondary
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث في المطاعم والكافيهات") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            filteredList.take(15).forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("📞 ${item.phone} • 📍 ${item.cityId}", color = themeColors.textSecondary, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.deleteStore(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
