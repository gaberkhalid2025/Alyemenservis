package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.dialogs.BookingDialog
import com.example.ui.screens.register.GuestRegistrationDialog
import com.example.utils.getStarsString
import com.example.utils.VisualThemePalette

@Composable
fun ProviderCard(
    provider: ProviderEntity,
    themeColors: VisualThemePalette,
    viewModel: MainViewModel,
    onChatOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val currentUserIdState by viewModel.currentUserId.collectAsState()

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showReviewsListDialog by remember { mutableStateOf(false) }
    var showGuestRegisterDialogForBooking by remember { mutableStateOf(false) }
    var showBookingDialog by remember { mutableStateOf(false) }

    var isPressed by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (settingsState.enableScaleAnimation && isPressed) settingsState.clickScaleRatio else 1.0f,
        label = "click_scale"
    )

    val isVerified = provider.isVerified || provider.subscriptionStatus == "APPROVED" || provider.isAvailable
    val coverImg = provider.coverImage.ifBlank { "" }
    val avatarImg = provider.profileImage.ifBlank { "" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .scale(scaleFactor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { showDetailsDialog = true }
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, if (isVerified) themeColors.accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Cover Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF1E293B))
            ) {
                if (coverImg.isNotBlank()) {
                    SmartAsyncImage(
                        model = coverImg,
                        contentDescription = provider.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛠️", fontSize = 34.sp)
                    }
                }

                // Subtle dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                startY = 30f
                            )
                        )
                )

                // Badges in Cover Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVerified || provider.isVip) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, themeColors.accent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (provider.isVip) "👑 VIP" else "موثق ✓",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.accent
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Rating Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { showReviewsListDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(String.format("%.1f", provider.rating), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // 2. Overlapping Avatar & Content Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                // Header with Overlapping Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, themeColors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarImg.isNotBlank()) {
                            SmartAsyncImage(
                                model = avatarImg,
                                contentDescription = provider.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("👤", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("✔️", fontSize = 11.sp, color = themeColors.accent)
                            }
                        }
                        val profText = if (provider.profession.isBlank()) "صيانة فنية وخدمات" else provider.profession
                        Text(
                            text = profText,
                            fontSize = 10.sp,
                            color = themeColors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Location & Availability Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-10).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        val locText = if (provider.localNeighborhood.isNotBlank()) "${provider.area} - ${provider.localNeighborhood}" else provider.area.ifBlank { "اليمن" }
                        Text(
                            text = locText,
                            fontSize = 9.5.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (provider.isAvailable) "🟢 متاح الآن" else "🔴 مشغول",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (provider.isAvailable) Color(0xFF10B981) else Color(0xFFEF5350)
                        )
                    }
                }

                // 3. Action Buttons Row: [التفاصيل] [التقييمات] [حجز / اتصال]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-4).dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showDetailsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("التفاصيل 📋", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showReviewsListDialog = true },
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("التقييمات ⭐", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (currentUserIdState.isEmpty()) {
                                showGuestRegisterDialogForBooking = true
                            } else {
                                showBookingDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Text("حجز 📅", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        ProviderDetailsDialog(
            provider = provider,
            themeColors = themeColors,
            viewModel = viewModel,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showReviewsListDialog) {
        ProviderReviewsListDialog(
            provider = provider,
            themeColors = themeColors,
            onDismiss = { showReviewsListDialog = false }
        )
    }

    if (showGuestRegisterDialogForBooking) {
        GuestRegistrationDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showGuestRegisterDialogForBooking = false },
            onRegisterCompleted = { name, phone, residence, password ->
                viewModel.registerGuestUser(context, name, phone, residence, password)
                showGuestRegisterDialogForBooking = false
                showBookingDialog = true
            }
        )
    }

    if (showBookingDialog) {
        BookingDialog(
            provider = provider,
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showBookingDialog = false }
        )
    }
}
