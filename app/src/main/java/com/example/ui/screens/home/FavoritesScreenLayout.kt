package com.example.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.utils.VisualThemePalette

import com.example.data.repositories.*
import com.example.ui.screens.dashboard.FavoritesViewModel
import com.example.ui.screens.dashboard.DashboardEvent

/**
 * ❤️ شاشة المفضلة الشاملة
 * تتيح للمستخدم حفظ وإدارة الخدمات والمتاجر والعقارات المفضلة للرجوع إليها سريعاً
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreenLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    onOpenProviderDetails: (ProviderEntity) -> Unit = {},
    onOpenStoreDetails: (StoreEntity) -> Unit = {},
    onOpenPropertyDetails: (PropertyEntity) -> Unit = {},
    onOpenChat: (channelId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserId = currentUserPhone.ifBlank { "guest_user" }

    val favoritesViewModel = remember(currentUserId) {
        FavoritesViewModel(
            userId = currentUserId,
            favoritesRepository = FavoritesRepositoryImpl(context)
        )
    }

    val favoritesUiState by favoritesViewModel.uiState.collectAsState()

    LaunchedEffect(favoritesViewModel) {
        favoritesViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                is DashboardEvent.NavigateToDetail -> { }
            }
        }
    }

    val providers by viewModel.providers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isProvidersLoading by viewModel.isProvidersLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: الكل, 1: فنيين وخدمات, 2: متاجر ومطاعم, 3: عقارات

    val favoriteProviders = remember(providers, favoriteIds) {
        providers.filter { favoriteIds.contains(it.id) || it.isVip }
    }
    val favoriteStores = remember(stores, favoriteIds) {
        stores.filter { favoriteIds.contains(it.id) }
    }
    val favoriteProperties = remember(properties, favoriteIds) {
        properties.filter { favoriteIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Text("قائمة المفضلة ❤️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.surface)
            )
        },
        containerColor = themeColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = themeColors.surface,
                contentColor = themeColors.accent,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                listOf("الكل", "فنيين 🔧", "متاجر 🛍️", "عقارات 🏢").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) themeColors.accent else Color.LightGray
                            )
                        }
                    )
                }
            }

            if (isProvidersLoading && favoriteIds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = themeColors.accent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري تحميل قائمتك المفضلة... ❤️", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Providers
                    if (selectedTab == 0 || selectedTab == 1) {
                        if (favoriteProviders.isNotEmpty()) {
                            item {
                                Text("الفنيون ومقدمو الخدمات المميزون", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                            }
                            items(favoriteProviders) { provider ->
                                FavoriteProviderCard(
                                    provider = provider,
                                    themeColors = themeColors,
                                    onClick = { onOpenProviderDetails(provider) },
                                    onRemoveFavorite = { viewModel.toggleFavorite(provider.id) },
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }

                    // Stores
                    if (selectedTab == 0 || selectedTab == 2) {
                        if (favoriteStores.isNotEmpty()) {
                            item {
                                Text("المتاجر والمطاعم المفضلة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                            items(favoriteStores) { store ->
                                FavoriteStoreCard(
                                    store = store,
                                    themeColors = themeColors,
                                    onClick = { onOpenStoreDetails(store) },
                                    onRemoveFavorite = { viewModel.toggleFavorite(store.id) },
                                    onCall = {
                                        if (store.phone.isNotEmpty()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}"))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Properties
                    if (selectedTab == 0 || selectedTab == 3) {
                        if (favoriteProperties.isNotEmpty()) {
                            item {
                                Text("العقارات والاستثمارات المحفوظة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                            }
                            items(favoriteProperties) { property ->
                                FavoritePropertyCard(
                                    property = property,
                                    themeColors = themeColors,
                                    onClick = { onOpenPropertyDetails(property) },
                                    onRemoveFavorite = { viewModel.toggleFavorite(property.id) },
                                    onCall = {
                                        if (property.phone.isNotEmpty()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${property.phone}"))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (favoriteProviders.isEmpty() && favoriteStores.isEmpty() && favoriteProperties.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Text("قائمة المفضلة فارغة حالياً.", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("يمكنك الضغط على رمز القلب ❤️ في أي صفحة لإضافتها هنا والوصول إليها بسرعة.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteProviderCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmartAsyncImage(
                model = provider.profileImage.ifEmpty { provider.coverImage },
                contentDescription = provider.name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(provider.profession.ifEmpty { provider.area }, color = Color.LightGray, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(String.format("%.1f", provider.rating), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📍 ${provider.localNeighborhood.ifEmpty { provider.area }}", color = Color.Gray, fontSize = 10.sp)
                }
            }

            IconButton(onClick = onCall) {
                Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onRemoveFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "إزالة من المفضلة", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FavoriteStoreCard(
    store: StoreEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmartAsyncImage(
                model = store.logoImage.ifEmpty { store.coverImage },
                contentDescription = store.name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(store.description, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(String.format("%.1f", store.rating), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📍 ${store.localNeighborhood.ifEmpty { store.cityId }}", color = Color.Gray, fontSize = 10.sp)
                }
            }

            if (store.phone.isNotEmpty()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                }
            }

            IconButton(onClick = onRemoveFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "إزالة من المفضلة", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FavoritePropertyCard(
    property: PropertyEntity,
    themeColors: VisualThemePalette,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmartAsyncImage(
                model = property.images.firstOrNull() ?: "",
                contentDescription = property.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text("${property.price.toInt()} ${property.currency} • ${property.propertyType}", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(String.format("%.1f", property.rating), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📍 ${property.localNeighborhood.ifEmpty { property.cityId }}", color = Color.Gray, fontSize = 10.sp)
                }
            }

            if (property.phone.isNotEmpty()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                }
            }

            IconButton(onClick = onRemoveFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "إزالة من المفضلة", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
    }
}
