package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    themeColors: VisualThemePalette,
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            "مرحباً بك في دليل اليمن للخدمات 🇾🇪",
            "بوابتك الإلكترونية المتكاملة للبحث عن أمهر الفنيين، المحلات التجارية، المراكز الطبية والمطاعم في مختلف المحافظات.",
            "🏪"
        ),
        OnboardingPage(
            "البحث الذكي المتقاطع والجغرافي 📍",
            "ابحث بسهولة عبر الفلاتر المتقدمة والخرائط لتحديد أقرب مقدم خدمة متاح بجودة ممتازة وأسعار مناسبة.",
            "🗺️"
        ),
        OnboardingPage(
            "محادثات وحجوزات آمنة فورية 💬",
            "تواصل مباشرة بالدردشة والصوت مع أصحاب الأعمال، وأنجز معاملاتك وحجوزاتك بكل ثقة وموثوقية تامة.",
            "🎉"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { position ->
                val page = pages[position]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = page.icon,
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = page.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = page.description,
                        fontSize = 12.sp,
                        color = themeColors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Indicator Dots & Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button
                TextButton(onClick = onFinish) {
                    Text("تخطي ➡️", color = themeColors.textSecondary, fontSize = 12.sp)
                }

                // Page Indicator Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.indices.forEach { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) themeColors.accent else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                // Next / Start Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isLastPage) "ابدأ الآن 🚀" else "التالي ⬅️",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
