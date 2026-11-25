package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.BookingEntity
import com.example.data.ChatChannelEntity
import com.example.ui.*
import com.example.utils.VisualThemePalette

/**
 * 📅 BookingsScreenLayout
 * الشاشة الرئيسية لنظام الحجوزات، نظيفة ومنفصلة تماماً عن نظام الطلبات العاجلة.
 */
@Composable
fun BookingsScreenLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current

    val bookings by viewModel.bookings.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val adminRole by viewModel.adminRole.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val joinRequestPhone by viewModel.joinRequestPhone.collectAsState()

    var isCreatingNewBooking by remember { mutableStateOf(false) }

    val isAdmin = adminRole != "GUEST" && adminRole != "SUPERVISOR"

    // Filter relevant bookings for the user or admin
    val relevantBookings = remember(bookings, currentUserPhone, currentUserId, isAdmin, joinRequestPhone) {
        if (isAdmin) {
            bookings.sortedByDescending { it.createdAt }
        } else {
            val phoneClean = currentUserPhone.filter { it.isDigit() }.takeLast(9)
            val joinPhoneClean = joinRequestPhone.filter { it.isDigit() }.takeLast(9)
            val uid = currentUserId.trim()
            bookings.filter { bk ->
                val bClientP = bk.clientPhone.filter { it.isDigit() }.takeLast(9)
                val bCustP = bk.customerPhone.filter { it.isDigit() }.takeLast(9)
                val bProvP = bk.providerPhone.filter { it.isDigit() }.takeLast(9)

                (phoneClean.isNotEmpty() && (bClientP == phoneClean || bCustP == phoneClean || bProvP == phoneClean)) ||
                (joinPhoneClean.isNotEmpty() && (bClientP == joinPhoneClean || bCustP == joinPhoneClean || bProvP == joinPhoneClean)) ||
                (uid.isNotEmpty() && (bk.clientId.trim() == uid || bk.providerId.trim() == uid))
            }.sortedByDescending { it.createdAt }
        }
    }

    AnimatedContent(
        targetState = isCreatingNewBooking,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "BookingScreenAnimation"
    ) { creating ->
        if (creating) {
            BookingFormScreen(
                onBack = { isCreatingNewBooking = false },
                onBookingCreated = { newBooking ->
                    viewModel.createBooking(newBooking) { success ->
                        if (success) {
                            Toast.makeText(context, "تم حفظ الحجز وتأكيده بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    isCreatingNewBooking = false
                }
            )
        } else {
            BookingListScreen(
                bookings = relevantBookings,
                currentUserId = currentUserId.ifBlank { currentUserPhone },
                isAdmin = isAdmin,
                isProvider = viewModel.isProviderUser,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData(showNotification = true) },
                onBackClick = { viewModel.navigateTo("USER_BROWSE") },
                onCreateNewBookingClick = { isCreatingNewBooking = true },
                onUpdateBooking = { updatedBooking ->
                    viewModel.updateBookingImpl(updatedBooking)
                },
                onStatusChange = { booking, newStatus ->
                    viewModel.updateBookingStatus(booking.id, newStatus)
                },
                onCancelBooking = { booking, reason, password ->
                    val cancelledByStr = if (isAdmin) "ADMIN" else if (viewModel.isProviderUser) "PROVIDER" else "USER"
                    viewModel.attemptCancelBookingImpl(
                        bookingId = booking.id,
                        input = password,
                        reason = reason,
                        cancelledByParam = cancelledByStr
                    ) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                onDeleteBooking = { booking ->
                    viewModel.deleteBookingImpl(booking.id)
                },
                onOpenChatClick = { booking ->
                    // توجيه المحادثة من بطاقة الحجز بشكل دقيق مع تمرير مرجع الحجز والأطراف الصحيحة
                    val cleanUserP = com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(currentUserPhone)
                    val cleanBookingCustP = com.example.ui.helpers.AppPreferenceHelper.normalizePhoneNumber(booking.effectiveCustomerPhone)
                    val isCurrentClient = (currentUserId.isNotBlank() && (booking.customerId == currentUserId || booking.clientId == currentUserId)) ||
                        (cleanUserP.isNotBlank() && cleanUserP == cleanBookingCustP)
                    val targetUserId = if (isCurrentClient) booking.providerId.ifBlank { booking.providerPhone } else booking.clientId.ifBlank { booking.customerId.ifBlank { booking.customerPhone } }
                    val targetUserName = if (isCurrentClient) booking.providerName.ifBlank { "مقدم الخدمة" } else booking.customerName.ifBlank { booking.clientName.ifBlank { "العميل" } }

                    viewModel.openDirectChat(
                        targetUserId = targetUserId.ifBlank { "support" },
                        targetUserName = targetUserName,
                        relatedEntityId = booking.id,
                        relatedEntityType = "BOOKING"
                    )
                }
            )
        }
    }
}
