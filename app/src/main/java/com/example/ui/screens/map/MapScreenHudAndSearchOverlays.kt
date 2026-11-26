package com.example.ui.screens.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.components.MapFilterBar
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.ui.screens.map.utils.OfflineMapManager
import com.example.utils.HaversinePrecisionNavigator
import com.example.utils.RealValhallaNavigator
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun BoxScope.MapHudOverlay(
    isDrivingMode: Boolean,
    isHudMirrored: Boolean,
    onToggleMirror: () -> Unit,
    onExitHud: () -> Unit,
    currentSpeedKmh: Int = 0,
    nextDistanceText: String = "",
    nextInstruction: String = "واصل السير على المسار الملاحي"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .graphicsLayer(
                scaleX = if (isHudMirrored) -1f else 1f,
                scaleY = if (isHudMirrored) -1f else 1f
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Straight,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(96.dp)
            )
            if (nextDistanceText.isNotBlank()) {
                Text(
                    text = nextDistanceText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF)
                )
            }
            Text(
                text = currentSpeedKmh.coerceAtLeast(0).toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF10B981)
            )
            Text(
                text = if (isDrivingMode) "كم / ساعة" else "كم / س (مشياً)",
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = nextInstruction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onToggleMirror,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isHudMirrored) "عكس عادي" else "انعكاس الزجاج 🪞", color = Color(0xFF10B981))
            }
            Button(
                onClick = onExitHud,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("خروج من وضع HUD", color = Color.White)
            }
        }
    }
}

@Composable
internal fun BoxScope.MapScreenTopSearchAndResults(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    keyboardController: SoftwareKeyboardController?,
    state: MapScreenState,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit,
    onVoiceSearchClick: () -> Unit,
    isTilesDownloaded: Boolean,
    onOpenDownloadDialog: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    searchPanelOpen: Boolean,
    onOpenSearchPanel: () -> Unit,
    onCloseSearchPanel: () -> Unit,
    onRetrySearch: () -> Unit,
    isSearching: Boolean,
    searchResults: List<RealValhallaNavigator.SearchAddressResult>,
    searchMessage: String?,
    onSelectSearchResult: (RealValhallaNavigator.SearchAddressResult) -> Unit,
    focusTick: Int = 0,
    safeUserLat: Double = 15.3694,
    safeUserLng: Double = 44.1910,
    hasRealLocation: Boolean = false
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        MapFilterBar(
            selectedCategory = state.selectedCategory,
            onCategorySelected = { state.selectedCategory = it },
            selectedCity = state.selectedCity,
            onCitySelected = { city -> state.selectedCity = city },
            searchQuery = state.searchQuery,
            onSearchQueryChange = { state.searchQuery = it },
            themeColors = themeColors,
            onBackClick = onBackClick,
            onVoiceSearchClick = onVoiceSearchClick,
            focusTick = focusTick,
            isTilesDownloaded = isTilesDownloaded,
            onDownloadMapsClick = onOpenDownloadDialog,
            onDiagnosticsClick = onOpenDiagnostics,
            onSearchExecute = { query ->
                val q = query.trim()
                if (q.length >= 2) {
                    onOpenSearchPanel()
                    onRetrySearch()
                }
                OfflineMapManager.findCity(q)?.let { city -> state.selectedCity = city.nameAr }
                keyboardController?.hide()
            }
        )

        // 📋 لوحة نتائج البحث
        if (searchPanelOpen && (isSearching || searchResults.isNotEmpty() || searchMessage != null)) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0B0F19).copy(alpha = 0.97f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Column {
                    if (isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF10B981)
                        )
                    }
                    searchMessage?.let {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                            Text(
                                text = it,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    val q = state.searchQuery.trim()
                                    if (q.length >= 2) onRetrySearch()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("إعادة المحاولة", fontSize = 12.sp, color = Color(0xFF00E5FF))
                            }
                        }
                    }
                    LazyColumn {
                        items(searchResults) { res ->
                            val distM = if (hasRealLocation) {
                                HaversinePrecisionNavigator.calculateDistance(safeUserLat, safeUserLng, res.latitude, res.longitude)
                            } else null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSearchResult(res)
                                        onCloseSearchPanel()
                                        state.searchQuery = ""
                                        keyboardController?.hide()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(res.icon, fontSize = 18.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = res.shortName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = res.displayName,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (distM != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = MapDistanceCalculator.formatDistance(distM),
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
        }
    }
}
