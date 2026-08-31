package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 💬 Modular Tab: Ratings & Customer Feedback (إدارة التقييمات والردود الحية)
 */
@Composable
fun TabReviewsFeedback(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val allRatings by viewModel.ratings.collectAsState()
    
    val myReviews = remember(allRatings, account.id, account.phone) {
        allRatings.filter { r ->
            r.targetId == account.id || r.targetId == account.phone
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            UnifiedReviewsSection(
                rating = account.rating.toDouble(),
                numReviews = myReviews.size,
                reviews = myReviews,
                onReplySubmit = { ratingId, replyText ->
                    viewModel.addRatingReply(ratingId, replyText)
                    Toast.makeText(context, "✅ تم نشر ردك على تقييم العميل بنجاح!", Toast.LENGTH_SHORT).show()
                },
                themeColors = themeColors
            )
        }
    }
}
