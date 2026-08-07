package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AppFooterBar(viewModel: MainViewModel, themeColors: VisualThemePalette, onInfoClick: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isEn = currentLang == "en"

    val footerBg = remember(settingsState.footerBgColorHex, themeColors.secondary) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.footerBgColorHex))
        } catch (e: Exception) {
            Color(0xFF0D332D) // Dark teal metallic container like image 2
        }
    }

    Surface(
        color = footerBg,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF0F5243), Color(0xFF1B8A72), Color(0xFF0F5243)))),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("app_footer_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val bookings by viewModel.bookings.collectAsState()
            val currentUserPhone by viewModel.currentUserPhone.collectAsState()
            val providers by viewModel.providers.collectAsState()

            val matchingProvider = remember(providers, currentUserPhone) {
                providers.find { it.phone.trim() == currentUserPhone.trim() && currentUserPhone.isNotEmpty() }
            }

            val unreadCount = remember(bookings, currentUserPhone, matchingProvider) {
                val custCount = bookings.count { b ->
                    b.customerPhone.trim() == currentUserPhone.trim() && currentUserPhone.isNotEmpty() && (b.status == "PENDING" || b.status == "APPROVED" || b.status == "STARTED")
                }
                val provCount = if (matchingProvider != null) {
                    bookings.count { b ->
                        b.providerId == matchingProvider.id && (b.status == "PENDING" || b.status == "APPROVED" || b.status == "STARTED")
                    }
                } else 0
                custCount + provCount
            }

            // 1. Info Icon ("عن التطبيق")
            if (settingsState.showInfoIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomInfoIcon.ifEmpty { "ℹ️" },
                    vectorIcon = Icons.Default.Info,
                    label = if (isEn) "About" else "عن التطبيق",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { onInfoClick() }
                )
            }

            // 2. Bookings Icon ("الحجوزات")
            if (settingsState.showBookingsIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomBookingsIcon.ifEmpty { "📅" },
                    vectorIcon = Icons.Default.DateRange,
                    label = if (isEn) "Bookings" else "الحجوزات",
                    isSelected = false,
                    badgeCount = unreadCount,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("BOOKINGS_VIEW") }
                )
            }

            // 3. Center Brand Text ("WAM2026")
            if (settingsState.showFooterText) {
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = settingsState.footerMessage.ifBlank { "WAM2026" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE2E8F0),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Single Clean Language Switcher Icon ("EN" / "🌐")
            if (settingsState.showLangIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomLangIcon.ifEmpty { if (isEn) "🌐" else "EN" },
                    vectorIcon = null,
                    label = if (isEn) "العربية" else "English",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = {
                        viewModel.switchLanguage()
                        viewModel.triggerNotification(
                            if (isEn) "تم التحويل إلى اللغة العربية 🇾🇪" else "Language switched to English 🌐"
                        )
                    }
                )
            }

            // 5. Admin Lock Icon ("الإدارة")
            if (settingsState.showAdminIcon) {
                Luxury3DNavIcon(
                    emojiIcon = settingsState.bottomAdminIcon.ifEmpty { "🔒" },
                    vectorIcon = Icons.Default.Lock,
                    label = if (isEn) "Admin" else "الإدارة",
                    isSelected = false,
                    iconSizeDp = settingsState.navIconSizeDp,
                    iconStyle = settingsState.topNavIconStyle,
                    onClick = { viewModel.navigateTo("ADMIN_PANEL") }
                )
            }
        }
    }
}
