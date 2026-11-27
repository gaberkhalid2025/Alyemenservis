package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import com.example.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VpnKey
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
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isEn = currentLang == "en"

    val footerBg = remember(settingsState.footerBgColorHex) {
        try {
            if (settingsState.footerBgColorHex.isNotBlank()) {
                Color(android.graphics.Color.parseColor(settingsState.footerBgColorHex))
            } else {
                Color(0xFF0B1120)
            }
        } catch (e: Exception) {
            Color(0xFF0B1120)
        }
    }

    val isBrightFooter = remember(footerBg) {
        androidx.core.graphics.ColorUtils.calculateLuminance(
            android.graphics.Color.argb(
                (footerBg.alpha * 255).toInt(),
                (footerBg.red * 255).toInt(),
                (footerBg.green * 255).toInt(),
                (footerBg.blue * 255).toInt()
            )
        ) > 0.25
    }
    val footerUnselectedLabel = if (isBrightFooter) Color(0xFF0B1120) else Color(0xFFF1F5F9)
    val footerSelectedLabel = if (isBrightFooter) Color(0xFF0B1120) else Color(0xFFFBBF24)
    val footerCenterTextColor = if (isBrightFooter) Color(0xFF0B1120) else Color.White

    val orderedItems = remember(settingsState.footerItemsOrder) {
        settingsState.footerItemsOrder
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("INFO", "BOOKINGS", "TEXT", "LANG", "ADMIN") }
    }

    Surface(
        color = footerBg,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF0F5243), Color(0xFFD4AF37), Color(0xFF0F5243)))),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("app_footer_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val bookings by viewModel.bookings.collectAsState()
            val currentUserPhone by viewModel.currentUserPhone.collectAsState()
            val providers by viewModel.providers.collectAsState()

            val activeStatuses = remember { setOf("PENDING", "UNDER_REVIEW", "ACCEPTED", "APPROVED", "IN_PROGRESS", "STARTED") }
            val cleanUserPhone = remember(currentUserPhone) { com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(currentUserPhone) }

            val matchingProvider = remember(providers, cleanUserPhone) {
                providers.find { com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(it.phone) == cleanUserPhone && cleanUserPhone.isNotEmpty() }
            }

            val unreadCount = remember(bookings, cleanUserPhone, matchingProvider) {
                val custCount = bookings.count { b ->
                    com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(b.effectiveCustomerPhone) == cleanUserPhone && cleanUserPhone.isNotEmpty() && b.status.uppercase() in activeStatuses
                }
                val provCount = if (matchingProvider != null) {
                    bookings.count { b ->
                        b.providerId == matchingProvider.id && b.status.uppercase() in activeStatuses
                    }
                } else 0
                custCount + provCount
            }

            val bookingsVector = when (settingsState.bookingsIconType.uppercase()) {
                "STAR" -> Icons.Default.Star
                "FAVORITE" -> Icons.Default.Favorite
                "HOME" -> Icons.Default.Home
                else -> Icons.Default.DateRange
            }

            val adminVector = when (settingsState.adminIconType.uppercase()) {
                "SETTINGS", "GEAR" -> Icons.Default.Settings
                "KEY" -> Icons.Default.VpnKey
                "SHIELD" -> Icons.Default.Security
                else -> Icons.Default.Lock
            }
            val adminEmoji = when (settingsState.adminIconType.uppercase()) {
                "SETTINGS", "GEAR" -> "⚙️"
                "KEY" -> "🔑"
                "SHIELD" -> "🛡️"
                else -> settingsState.bottomAdminIcon.ifEmpty { "🔒" }
            }

            val (langEmoji, langVector) = when (settingsState.langIconType.uppercase()) {
                "TRANSLATE" -> "🔤" to Icons.Default.Translate
                "TEXT" -> (if (isEn) "AR" else "EN") to null
                else -> (settingsState.bottomLangIcon.ifEmpty { if (isEn) "🌐" else "EN" }) to Icons.Default.Public
            }

            orderedItems.forEach { slot ->
                when (slot) {
                    "INFO" -> if (settingsState.showInfoIcon) {
                        val isAbout = currentScreen == AppScreens.ABOUT_APP
                        Luxury3DNavIcon(
                            emojiIcon = settingsState.bottomInfoIcon.ifEmpty { "ℹ️" },
                            vectorIcon = Icons.Default.Info,
                            label = if (isEn) "About" else "عن التطبيق",
                            isSelected = isAbout,
                            iconSizeDp = settingsState.navIconSizeDp,
                            iconStyle = settingsState.topNavIconStyle,
                            unselectedLabelColor = footerUnselectedLabel,
                            selectedLabelColor = footerSelectedLabel,
                            onClick = { onInfoClick() }
                        )
                    }

                    "BOOKINGS" -> if (settingsState.showBookingsIcon && settingsState.isBookingsIconVisible) {
                        val isBookings = currentScreen == AppScreens.BOOKINGS_VIEW
                        Luxury3DNavIcon(
                            emojiIcon = settingsState.bottomBookingsIcon.ifEmpty { "📅" },
                            vectorIcon = bookingsVector,
                            label = if (isEn) "Bookings" else "الحجوزات",
                            isSelected = isBookings,
                            badgeCount = unreadCount,
                            iconSizeDp = settingsState.navIconSizeDp,
                            iconStyle = settingsState.topNavIconStyle,
                            unselectedLabelColor = footerUnselectedLabel,
                            selectedLabelColor = footerSelectedLabel,
                            onClick = { viewModel.navigateTo("BOOKINGS_VIEW") }
                        )
                    }

                    "TEXT" -> if (settingsState.showFooterText) {
                        Box(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (settingsState.footerMessage.startsWith("card_custom_") || settingsState.footerMessage.isBlank() || settingsState.footerMessage == "WAM2026") "ASH" else settingsState.footerMessage,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = footerCenterTextColor,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    "LANG" -> if (settingsState.showLangIcon) {
                        Luxury3DNavIcon(
                            emojiIcon = langEmoji,
                            vectorIcon = langVector,
                            label = if (isEn) "العربية" else "English",
                            isSelected = false,
                            iconSizeDp = settingsState.navIconSizeDp,
                            iconStyle = settingsState.topNavIconStyle,
                            unselectedLabelColor = footerUnselectedLabel,
                            selectedLabelColor = footerSelectedLabel,
                            onClick = {
                                viewModel.switchLanguage()
                                viewModel.triggerNotification(
                                    if (isEn) "تم التحويل إلى اللغة العربية 🇾🇪" else "Language switched to English 🌐"
                                )
                            }
                        )
                    }

                    "ADMIN" -> if (settingsState.showAdminIcon) {
                        val isAdmin = currentScreen == AppScreens.ADMIN_PANEL
                        Luxury3DNavIcon(
                            emojiIcon = adminEmoji,
                            vectorIcon = adminVector,
                            label = if (isEn) "Admin" else "الإدارة",
                            isSelected = isAdmin,
                            iconSizeDp = settingsState.navIconSizeDp,
                            iconStyle = settingsState.topNavIconStyle,
                            unselectedLabelColor = footerUnselectedLabel,
                            selectedLabelColor = footerSelectedLabel,
                            onClick = { viewModel.navigateTo("ADMIN_PANEL") }
                        )
                    }
                }
            }
        }
    }
}
