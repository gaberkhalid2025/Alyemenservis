package com.example.ui.screens.entities

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RatingEntity
import com.example.data.SpecialOfferEntity
import com.example.data.StoreEntity
import com.example.domain.entities.DoctorItem
import com.example.ui.AppScreens
import com.example.ui.MainViewModel
import com.example.ui.components.SmartAsyncImage
import com.example.ui.navigateToScreen
import com.example.ui.toggleFavorite
import com.example.utils.ChatTypeResolver
import com.example.utils.VisualThemePalette
import java.util.Locale

@Composable
internal fun StoreHeroBanner(
    store: StoreEntity,
    isMedical: Boolean,
    isRestaurant: Boolean,
    isFav: Boolean,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onReportClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(Color(0xFF0F172A))
    ) {
        val bannerUrl = store.coverImage.ifBlank { store.logoImage }
        if (bannerUrl.isNotBlank()) {
            SmartAsyncImage(
                model = bannerUrl,
                contentDescription = store.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            when {
                                isMedical -> listOf(Color(0xFF082F49), Color(0xFF0F172A), Color(0xFF065F46))
                                isRestaurant -> listOf(Color(0xFF451A03), Color(0xFF1E293B), Color(0xFF9A3412))
                                else -> listOf(Color(0xFF0F2B48), Color(0xFF1E3A5F), Color(0xFF2563EB))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = store.name,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { viewModel.toggleFavorite(store.id) },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFav) Color(0xFFEF4444) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        val shareText = "تفضل بزيارة ${store.name} على منصة دليل خدمات اليمن 🇾🇪\nالعنوان: ${store.localNeighborhood}\nالهاتف: ${store.phone}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "مشاركة"))
                        }.onFailure {
                            Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onReportClick != null) {
                    Surface(
                        onClick = onReportClick,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "إبلاغ عن مخالفة",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StoreInfoAndActionsCard(
    store: StoreEntity,
    isMedical: Boolean,
    isRestaurant: Boolean,
    isMall: Boolean,
    storeReviews: List<RatingEntity>,
    currentUserId: String,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onOpenRatingDialog: () -> Unit,
    onRequireGuestRegistration: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, themeColors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = store.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (store.isVerified || store.isActive) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "موثق رسميًا",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isMall) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, Color(0xFF8B5CF6))
                            ) {
                                Text(
                                    text = "🏬 مول / مركز تجاري",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = store.description.ifBlank {
                            when {
                                isMedical -> "مركز طبي / عيادة معتمدة"
                                isRestaurant -> "مطعم وكافيه معتمد"
                                else -> "منشأة معتمدة لتقديم أرقى المنتجات والخدمات"
                            }
                        },
                        fontSize = 11.5.sp,
                        color = themeColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.6.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.clickable { onOpenRatingDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                        Text(String.format(Locale.US, "%.1f", store.rating), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                        Text("(${maxOf(store.numReviews, storeReviews.size)})", fontSize = 9.5.sp, color = Color(0xFFB45309))
                    }
                }
            }

            HorizontalDivider(color = themeColors.border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val addressText = store.localNeighborhood.ifBlank { store.cityId.ifBlank { "اليمن" } }
                Text(
                    text = "📍 $addressText",
                    fontSize = 11.5.sp,
                    color = themeColors.textSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    onClick = {
                        viewModel.selectedStore = store
                        viewModel.navigateToScreen(AppScreens.MAP_VIEW)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1D4ED8).copy(alpha = 0.15f),
                    border = BorderStroke(0.6.dp, Color(0xFF3B82F6))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(13.dp))
                        Text("الخريطة 🧭", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    }
                }
            }

            // Action Buttons Row (Chat + Call + Book Appointment for Medical)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (currentUserId.isBlank() || currentUserId == "guest") {
                            onRequireGuestRegistration()
                        } else {
                            viewModel.openDirectChat(
                                targetUserId = store.id,
                                targetUserName = store.name,
                                targetUserPhoto = store.logoImage,
                                relatedEntityId = store.id,
                                relatedEntityType = ChatTypeResolver.forStore(
                                    store.sectionId,
                                    store.categoryId,
                                    store.name
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("محادثة 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (store.phone.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${store.phone}")))
                            }.onFailure {
                                Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("اتصال 📞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (isMedical) {
                    Button(
                        onClick = {
                            viewModel.selectedStore = store
                            viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f).height(40.dp)
                    ) {
                        Text("حجز موعد 🩺", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun StoreOffersSection(activeOffers: List<SpecialOfferEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (activeOffers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFCD34D))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Text("🔥 عروض وخصومات حصرية متوفرة الآن!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                }
            }
        } else {
            activeOffers.forEach { offer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "🏷️ ${offer.title.ifBlank { "عرض خاص" }}${if (offer.discountPercent > 0) " - خصم ${offer.discountPercent}%" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        if (offer.description.isNotBlank()) {
                            Text(offer.description, fontSize = 10.5.sp, color = Color(0xFFB45309))
                        }
                        if (offer.couponCode.isNotBlank()) {
                            Text("الكود: ${offer.couponCode}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StoreDoctorsListSection(
    store: StoreEntity,
    doctors: List<DoctorItem>,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("👨‍⚕️ أطباء المركز والعيادات (${doctors.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        doctors.forEach { doc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColors.border)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("د. ${doc.name}", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                        if (doc.specialty.isNotBlank()) {
                            Text("التخصص: ${doc.specialty}", fontSize = 11.5.sp, color = Color(0xFF06B6D4))
                        }
                        if (doc.workingHours.isNotBlank()) {
                            Text("الدوام: ${doc.workingHours}", fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.selectedStore = store
                            viewModel.navigateToScreen(AppScreens.CREATE_BOOKING)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("احجز 📅", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
