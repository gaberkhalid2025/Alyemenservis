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
import com.example.ui.utils.getStarsString
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 2.dp)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, if (provider.isVerified) themeColors.accent else themeColors.accent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, themeColors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val imgIcon = if (provider.profileImage.isBlank()) "👤" else provider.profileImage
                        CategorySectionIconView(iconStr = imgIcon, size = 32.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (provider.isVerified) {
                                Text(" ✔️", fontSize = 11.sp, color = themeColors.accent)
                            }
                        }
                        val profText = if (provider.profession.isBlank()) "صيانة فنية" else provider.profession
                        Text(
                            text = "$profText | ${provider.area}",
                            fontSize = 10.sp,
                            color = themeColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (provider.isAvailable) "🟢 متاح" else "🔴 مشغول",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (provider.isAvailable) Color.Green else Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showReviewsListDialog = true }
                ) {
                    Text(getStarsString(provider.rating), color = Color.Yellow, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(${provider.numReviews})", fontSize = 10.sp, color = themeColors.textSecondary)
                }

                if (provider.previewPrice > 0) {
                    Text(
                        text = "معاينة: ${provider.previewPrice.toInt()} ر.ي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("اتصال 📞", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val url = "https://api.whatsapp.com/send?phone=${provider.phone}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("واتساب 💬", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (currentUserIdState.isEmpty()) {
                            showGuestRegisterDialogForBooking = true
                        } else {
                            showBookingDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("حجز 📅", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
