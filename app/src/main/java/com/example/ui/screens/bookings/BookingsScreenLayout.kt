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

    var isCreatingNewBooking by remember { mutableStateOf(false) }

    val isAdmin = adminRole != "GUEST" && adminRole != "SUPERVISOR"

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Filter relevant bookings for the user or admin
    val relevantBookings = remember(bookings, currentUserPhone, currentUserId, isAdmin) {
        if (isAdmin) {
            bookings.sortedByDescending { it.createdAt }
        } else {
            val phoneClean = currentUserPhone.filter { it.isDigit() }.takeLast(9)
            val joinPhoneClean = viewModel.joinRequestPhone.value.filter { it.isDigit() }.takeLast(9)
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
                onRefresh = { viewModel.refreshData() },
                onBackClick = { viewModel.navigateTo("USER_BROWSE") },
                onCreateNewBookingClick = { isCreatingNewBooking = true },
                onUpdateBooking = { updatedBooking ->
                    viewModel.updateBookingImpl(updatedBooking)
                },
                onStatusChange = { booking, newStatus ->
                    val updated = booking.copy(status = newStatus)
                    viewModel.updateBookingImpl(updated)
                    if (newStatus == "APPROVED") {
                        // Automatically open chat when accepted
                        val otherId = booking.clientId.ifEmpty { booking.clientPhone.ifEmpty { "CUSTOMER" } }
                        val otherName = booking.customerName.ifEmpty { booking.clientName.ifEmpty { "العميل" } }
                        val otherPhone = booking.clientPhone.ifEmpty { booking.customerPhone }
                        viewModel.openOrCreateChatChannel(
                            targetId = otherId,
                            targetType = "BOOKING",
                            targetName = otherName,
                            targetPhone = otherPhone,
                            targetCategory = booking.category,
                            relatedEntityId = booking.id,
                            relatedEntityType = "BOOKING"
                        ) { createdCh ->
                            if (createdCh != null && createdCh.id.isNotEmpty()) {
                                viewModel.db.collection("bookings").document(booking.id).update("relatedChatChannelId", createdCh.id)
                            }
                        }
                    }
                    Toast.makeText(context, "تم تحديث حالة الحجز إلى: $newStatus", Toast.LENGTH_SHORT).show()
                },
                onCancelBooking = { booking, reason, password ->
                    viewModel.attemptCancelBookingImpl(
                        bookingId = booking.id,
                        input = password,
                        reason = reason
                    ) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                onDeleteBooking = { booking ->
                    viewModel.deleteBookingImpl(booking.id)
                },
                onOpenChatClick = { booking ->
                    val otherId = booking.providerId.ifEmpty { booking.providerPhone.ifEmpty { "ADMIN" } }
                    val otherName = booking.providerName.ifEmpty { "مقدم الخدمة" }
                    val otherPhone = booking.providerPhone

                    viewModel.openOrCreateChatChannel(
                        targetId = otherId,
                        targetType = "BOOKING",
                        targetName = otherName,
                        targetPhone = otherPhone,
                        targetCategory = booking.category,
                        relatedEntityId = booking.id,
                        relatedEntityType = "BOOKING"
                    ) { createdCh ->
                        if (createdCh != null && createdCh.id.isNotEmpty()) {
                            viewModel.db.collection("bookings").document(booking.id).update("relatedChatChannelId", createdCh.id)
                        }
                        viewModel.openChatChannel(createdCh)
                        viewModel.navigateTo("CHAT_DIRECT")
                    }
                }
            )
        }
    }
}
