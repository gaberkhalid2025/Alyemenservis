package com.example.ui.screens.entities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onCategoryClick: (String) -> Unit
) {
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(retryCount) {
        isLoading = true
        errorMessage = null
        val registration = viewModel.db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = "فشل تحميل الفئات: ${error.localizedMessage ?: "خطأ غير معروف"}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        try {
                            val obj = doc.toObject(CategoryEntity::class.java)
                            if (obj != null) {
                                if (obj.id.isEmpty()) obj.copy(id = doc.id) else obj
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }.distinctBy { it.id }.sortedWith(
                        compareByDescending<CategoryEntity> { it.isPinned }
                            .thenBy { it.order }
                    )
                    categories = fetched
                    errorMessage = null
                }
            }
        onDispose {
            registration.remove()
        }
    }

    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            val result = snackbarHostState.showSnackbar(
                message = errorMessage ?: "حدث خطأ غير متوقع",
                actionLabel = "إعادة المحاولة",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                retryCount++
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗂️ فئات دليل الخدمات اليمني المتاحة:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.primary,
                            modifier = Modifier.testTag("categories_loading_indicator")
                        )
                        Text(
                            text = "جاري تحميل الفئات...",
                            color = themeColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (errorMessage != null && categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "حدث خطأ أثناء تحميل الفئات",
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Button(
                            onClick = { retryCount++ },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("إعادة المحاولة 🔄", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else if (categories.isEmpty() && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد فئات متاحة حالياً.",
                        color = themeColors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("categories_grid")
                ) {
                    items(categories, key = { it.id }) { cat ->
                        DynamicCategoryCard(
                            category = cat,
                            themeColors = themeColors,
                            onClick = { onCategoryClick(cat.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicCategoryCard(
    category: CategoryEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = themeColors.accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = category.icon.ifBlank { "✨" },
                    fontSize = 28.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = category.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
