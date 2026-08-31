@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import com.example.viewmodels.StoreViewModel
import com.example.viewmodels.RegistrationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RatingEntity
import com.example.data.StoreEntity

import com.example.ui.screens.dashboard.components.StoreAddProductDialog
import com.example.ui.screens.dashboard.components.StoreEditDetailsCard
import com.example.ui.screens.dashboard.components.StoreProductCatalogCard
import com.example.ui.screens.dashboard.components.StoreReviewsCard
import com.example.utils.VisualThemePalette

/**
 * 🏪 StoreOwnerDashboardLayout - لوحة تحكم وإدارة المتجر صاحب المحل التجاري
 * مفككة ومبنية وفق معمارية Clean Architecture الموحدة (< 150 سطر)
 */
@Composable
fun StoreOwnerDashboardLayout(
    store: StoreEntity,
    storeViewModel: StoreViewModel = viewModel(),
    registrationViewModel: RegistrationViewModel = viewModel(),
    themeColors: VisualThemePalette,
    ratings: List<RatingEntity>
) {
    val context = LocalContext.current
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()
    val storeProducts = remember(products, store.id) {
        products.filter { it.storeId == store.id }
    }

    val storeRatings = remember(ratings, store.id) {
        ratings.filter { it.targetId == store.id }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🏪", fontSize = 28.sp)
        }

        Text(
            text = "🎉 لوحة تحكم وإدارة متجرك: ${store.name}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Part 1: Edit Details Card
        StoreEditDetailsCard(
            store = store,
            
            themeColors = themeColors,
            context = context
        )

        // Part 2: Product Catalog management
        StoreProductCatalogCard(
            storeProducts = storeProducts,
            
            themeColors = themeColors,
            context = context,
            onAddProductClick = { showAddProductDialog = true }
        )

        // Part 3: Reviews and Replies
        StoreReviewsCard(
            storeRatings = storeRatings,
            
            themeColors = themeColors,
            context = context
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: Logout & Delete Store Account
        Button(
            onClick = {
                registrationViewModel.cancelOrResetJoinRequest(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🚪 تسجيل الخروج من لوحة التحكم", color = Color.White, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🗑️ حذف حساب المتجر نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("⚠️ تأكيد الحذف النهائي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف حساب متجرك بالكامل؟ سيتم حذف المتجر وكافة المنتجات المرفقة والتعليقات نهائياً من قاعدة البيانات ولا يمكن التراجع عن هذا الإجراء!", color = Color.LightGray, fontSize = 11.sp) },
                containerColor = themeColors.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            storeViewModel.deleteStorePermanently(store.id)
                            registrationViewModel.cancelOrResetJoinRequest(context)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("نعم، احذف نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("إلغاء", color = Color.White, fontSize = 11.sp)
                    }
                }
            )
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        StoreAddProductDialog(
            storeId = store.id,
            
            themeColors = themeColors,
            context = context,
            onDismiss = { showAddProductDialog = false }
        )
    }
}
