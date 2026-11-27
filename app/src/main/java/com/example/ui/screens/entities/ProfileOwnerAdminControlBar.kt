package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
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
import com.example.data.*
import com.example.ui.*
import com.example.utils.VisualThemePalette

@Composable
fun ProfileOwnerAdminControlBar(
    entityId: String,
    entityType: ProfileEntityType,
    provider: ProviderEntity?,
    store: StoreEntity?,
    property: PropertyEntity?,
    job: JobEntity?,
    isOwner: Boolean,
    isAdmin: Boolean,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    if (!isOwner && !isAdmin) return

    val context = LocalContext.current
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var showChangePhotosDialog by remember { mutableStateOf(false) }
    var showPriceOfferDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val collectionName = when {
        provider != null -> "providers"
        store != null -> "stores"
        property != null -> "properties"
        job != null -> "jobs"
        else -> "providers"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(
            1.5.dp,
            if (isAdmin) Color(0xFFEAB308).copy(alpha = 0.6f) else themeColors.accent.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Settings else Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = if (isAdmin) Color(0xFFEAB308) else themeColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAdmin) "👑 لوحة التحكم والإشراف الشامل للأدمن" else "🛠️ لوحة تحكم حسابك (${entityType.labelAr})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color(0xFFEAB308) else themeColors.textPrimary
                    )
                }
                if (isAdmin) {
                    Text(
                        text = "صلاحيات كاملة",
                        fontSize = 10.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            when (entityType) {
                ProfileEntityType.TECHNICIAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.BOOKINGS_VIEW) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الحجوزات 📅", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.CHAT_LIST) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المحادثات 💬", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showPriceOfferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الأسعار 🏷️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ProfileEntityType.STORE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة سلعة 📦", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showPriceOfferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("العروض 🏷️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.ORDERS_VIEW) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الطلبيات 🛒", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ProfileEntityType.MEDICAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.BOOKINGS_VIEW) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مواعيد المرضى 🩺", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showPriceOfferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("كشفية وخدمات 🏷️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.CHAT_LIST) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استشارات 💬", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ProfileEntityType.RESTAURANT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة وجبة 🍔", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showPriceOfferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عروض اليوم 🎁", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.ORDERS_VIEW) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الطلبات والطاولات 🛵", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ProfileEntityType.REAL_ESTATE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showPriceOfferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الأسعار والإيجار 🏠", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.navigateToScreen(AppScreens.CHAT_LIST) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استفسارات العملاء 💬", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {}
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { showChangePhotosDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, themeColors.accent)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColors.accent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الصورة والغلاف 🖼️", fontSize = 10.sp, color = themeColors.textPrimary, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showEditDetailsDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل البيانات 📝", fontSize = 10.sp, color = themeColors.textPrimary, fontWeight = FontWeight.Bold)
                }

                if (isAdmin) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("حذف النشاط 🗑️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showChangePhotosDialog) {
        ProfileEditPhotosDialog(
            entityId = entityId,
            collectionName = collectionName,
            provider = provider,
            store = store,
            property = property,
            context = context,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showChangePhotosDialog = false }
        )
    }

    if (showEditDetailsDialog) {
        ProfileEditDetailsDialog(
            provider = provider,
            store = store,
            property = property,
            context = context,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showEditDetailsDialog = false }
        )
    }

    if (showPriceOfferDialog) {
        ProfilePriceOfferDialog(
            entityId = entityId,
            provider = provider,
            store = store,
            property = property,
            context = context,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showPriceOfferDialog = false }
        )
    }

    if (showAddProductDialog) {
        ProfileAddProductDialog(
            entityId = entityId,
            context = context,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showAddProductDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        ProfileDeleteEntityConfirmDialog(
            collectionName = collectionName,
            entityId = entityId,
            context = context,
            viewModel = viewModel,
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}
