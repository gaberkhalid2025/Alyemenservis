package com.example.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.RatingEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.ui.screens.dashboard.components.*
import com.example.utils.VisualThemePalette

/**
 * 🏛️ UnifiedDashboardComponents facade.
 * Delegates to modularized components in com.example.ui.screens.dashboard.components.*
 */

@Composable
fun UnifiedProfileSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedProfileSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedSettingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedSettingsSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedProductsServicesSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedProductsServicesSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedOffersSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedOffersSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedRatingsSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedBookingsSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedAttachmentsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedAttachmentsSection(account, viewModel, themeColors)
}

@Composable
fun UnifiedEditDetailsCard(
    title: String,
    fields: List<Triple<String, String, (String) -> Unit>>,
    onSaveClick: () -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.UnifiedEditDetailsCard(title, fields, onSaveClick, themeColors, modifier)
}

@Composable
fun UnifiedReviewsSection(
    rating: Double,
    numReviews: Int,
    reviews: List<RatingEntity>,
    onReplySubmit: (String, String) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.UnifiedReviewsSection(rating, numReviews, reviews, onReplySubmit, themeColors, modifier)
}

@Composable
fun UnifiedDeleteConfirmation(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    themeColors: VisualThemePalette
) {
    com.example.ui.screens.dashboard.components.UnifiedDeleteConfirmation(title, message, onConfirm, onDismiss, themeColors)
}

@Composable
fun UnifiedImagePicker(
    label: String,
    imageUrl: String,
    onImageSelected: (android.net.Uri) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.UnifiedImagePicker(label, imageUrl, onImageSelected, themeColors, modifier)
}

@Composable
fun UnifiedLoadingIndicator(
    text: String = "جاري تحميل البيانات والمزامنة الفورية...",
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.UnifiedLoadingIndicator(text, themeColors, modifier)
}

@Composable
fun UnifiedEmptyState(
    icon: String,
    title: String,
    description: String,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.UnifiedEmptyState(icon, title, description, themeColors, modifier)
}

@Composable
fun ProfessionalDashboardHeader(
    account: UnifiedBusinessAccount,
    subtitle: String,
    isVerified: Boolean,
    isServiceActive: Boolean,
    onToggleServiceActive: (Boolean) -> Unit,
    onEditProfileClick: () -> Unit,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit,
    themeColors: VisualThemePalette,
    coverUrl: String = "",
    avatarUrl: String = ""
) {
    com.example.ui.screens.dashboard.components.ProfessionalDashboardHeader(
        account = account,
        subtitle = subtitle,
        isVerified = isVerified,
        isServiceActive = isServiceActive,
        onToggleServiceActive = onToggleServiceActive,
        onEditProfileClick = onEditProfileClick,
        onShareClick = onShareClick,
        onBackClick = onBackClick,
        themeColors = themeColors,
        coverUrl = coverUrl,
        avatarUrl = avatarUrl
    )
}

@Composable
fun ProfessionalQuickStatsGrid(
    todayOrdersCount: Int,
    overallRating: Number,
    activeOffersCount: Int,
    approxRevenue: String,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    com.example.ui.screens.dashboard.components.ProfessionalQuickStatsGrid(
        todayOrdersCount = todayOrdersCount,
        overallRating = overallRating,
        activeOffersCount = activeOffersCount,
        approxRevenue = approxRevenue,
        themeColors = themeColors,
        modifier = modifier
    )
}

