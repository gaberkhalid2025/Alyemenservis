package com.example.ui.screens.bookings

import android.widget.Toast
import com.example.ui.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettingsEntity
import com.example.data.BookingEntity
import com.example.ui.dialogs.BookingCancellationDialog
import com.example.utils.VisualThemePalette
import com.example.utils.resolveThemePalette

/**
 * 📋 BookingListScreen
 * شاشة "حجوزاتي" لإدارة وعرض الحجوزات العادية بتباين عالٍ بدون شريط علوي مكرر.
 */
@Composable
fun BookingListScreen(
    bookings: List<BookingEntity>,
    themeColors: VisualThemePalette = resolveThemePalette(AdminSettingsEntity()),
    currentUserId: String = "",
    isAdmin: Boolean = false,
    isProvider: Boolean = false,
    onBackClick: () -> Unit,
    onCreateNewBookingClick: () -> Unit,
    onUpdateBooking: (BookingEntity) -> Unit,
    onStatusChange: (BookingEntity, String) -> Unit = { _, _ -> },
    onCancelBooking: (BookingEntity, String, String) -> Unit, // booking, reason, password
    onDeleteBooking: (BookingEntity) -> Unit,
    onOpenChatClick: (BookingEntity) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    isPaymentFeatureEnabled: Boolean = false,
    onTrackStatusClick: ((BookingEntity) -> Unit)? = null,
    onSubmitPaymentClick: ((BookingEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    var selectedTabFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, COMPLETED, CANCELLED
    var searchQuery by remember { mutableStateOf("") }

    var bookingToEdit by remember { mutableStateOf<BookingEntity?>(null) }
    var bookingToCancel by remember { mutableStateOf<BookingEntity?>(null) }
    var bookingToDelete by remember { mutableStateOf<BookingEntity?>(null) }

    val activeStatusSet = remember {
        setOf("PENDING", "UNDER_REVIEW", "ACCEPTED", "APPROVED", "IN_PROGRESS", "STARTED")
    }
    val completedStatusSet = remember {
        setOf("COMPLETED", "PAID", "CLOSED")
    }
    val cancelledStatusSet = remember {
        setOf("CANCELLED", "REJECTED")
    }

    val isAccentLight = remember(themeColors.accent) {
        androidx.core.graphics.ColorUtils.calculateLuminance(
            android.graphics.Color.argb(
                (themeColors.accent.alpha * 255).toInt(),
                (themeColors.accent.red * 255).toInt(),
                (themeColors.accent.green * 255).toInt(),
                (themeColors.accent.blue * 255).toInt()
            )
        ) > 0.45
    }
    val onAccentColor = if (isAccentLight) Color(0xFF0B1120) else Color.White

    // Pre-calculate tab counts efficiently with remember
    val totalCount = remember(bookings) { bookings.size }
    val activeCount = remember(bookings) { bookings.count { it.status.uppercase() in activeStatusSet } }
    val completedCount = remember(bookings) { bookings.count { it.status.uppercase() in completedStatusSet } }
    val cancelledCount = remember(bookings) { bookings.count { it.status.uppercase() in cancelledStatusSet } }

    // Filter bookings with derivedStateOf
    val filteredBookings by remember(bookings, selectedTabFilter, searchQuery) {
        derivedStateOf {
            val query = searchQuery.trim().lowercase()
            bookings.filter { bk ->
                val upperStatus = bk.status.uppercase()
                val matchesTab = when (selectedTabFilter) {
                    "ACTIVE" -> upperStatus in activeStatusSet
                    "COMPLETED" -> upperStatus in completedStatusSet
                    "CANCELLED" -> upperStatus in cancelledStatusSet
                    else -> true
                }

                val matchesQuery = query.isEmpty() ||
                    bk.bookingCode.lowercase().contains(query) ||
                    bk.bookingNumber.lowercase().contains(query) ||
                    bk.fullName.lowercase().contains(query) ||
                    bk.customerName.lowercase().contains(query) ||
                    bk.clientPhone.lowercase().contains(query) ||
                    bk.customerPhone.lowercase().contains(query) ||
                    bk.providerName.lowercase().contains(query)

                matchesTab && matchesQuery
            }
        }
    }

    var showExportMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // Compact Inline Header Row (No duplicate Scaffold/TopAppBar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColors.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "سجل حجوزاتي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColors.textPrimary
                    )
                    Text(
                        text = "الإجمالي: $totalCount حجز  •  الحالية: $activeCount",
                        fontSize = 11.sp,
                        color = themeColors.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { showExportMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.accent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("bookings_export_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير التقرير", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصدير", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        containerColor = themeColors.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("📄 تصدير مستند PDF", color = themeColors.textPrimary) },
                            onClick = {
                                showExportMenu = false
                                val result = com.example.utils.BookingExportHelper.exportBookingsToPdf(context, filteredBookings)
                                result.onSuccess { file ->
                                    com.example.utils.BookingExportHelper.shareExportedFile(context, file, "تقرير PDF للحجوزات")
                                    Toast.makeText(context, "تم تجهيز تقرير PDF بنجاح", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل تصدير PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📊 تصدير ملف Excel (XLS)", color = themeColors.textPrimary) },
                            onClick = {
                                showExportMenu = false
                                val result = com.example.utils.BookingExportHelper.exportBookingsToExcel(context, filteredBookings)
                                result.onSuccess { file ->
                                    com.example.utils.BookingExportHelper.shareExportedFile(context, file, "تقرير Excel للحجوزات")
                                    Toast.makeText(context, "تم تجهيز ملف Excel بنجاح", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل تصدير Excel", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📝 تصدير ملف CSV", color = themeColors.textPrimary) },
                            onClick = {
                                showExportMenu = false
                                val result = com.example.utils.BookingExportHelper.exportBookingsToCsv(context, filteredBookings)
                                result.onSuccess { file ->
                                    com.example.utils.BookingExportHelper.shareExportedFile(context, file, "تقرير CSV للحجوزات")
                                    Toast.makeText(context, "تم تجهيز ملف CSV بنجاح", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل تصدير CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Button(
                    onClick = onCreateNewBookingClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.accent,
                        contentColor = onAccentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("bookings_new_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حجز جديد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "بحث برقم الحجز، الاسم، أو الهاتف...",
                    color = themeColors.textSecondary.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = themeColors.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = themeColors.textPrimary,
                unfocusedTextColor = themeColors.textPrimary,
                focusedContainerColor = themeColors.surface,
                unfocusedContainerColor = themeColors.surface,
                focusedBorderColor = themeColors.accent,
                unfocusedBorderColor = themeColors.border,
                cursorColor = themeColors.accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("bookings_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // High-Contrast Filter Tabs
        val selectedTabIndex = when (selectedTabFilter) {
            "ACTIVE" -> 1
            "COMPLETED" -> 2
            "CANCELLED" -> 3
            else -> 0
        }
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = themeColors.surface,
            contentColor = themeColors.accent,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = themeColors.accent
                    )
                }
            },
            divider = {
                HorizontalDivider(color = themeColors.border, thickness = 1.dp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("bookings_filter_tabs")
        ) {
            Tab(
                selected = selectedTabFilter == "ALL",
                onClick = { selectedTabFilter = "ALL" },
                selectedContentColor = themeColors.accent,
                unselectedContentColor = themeColors.textSecondary,
                text = { Text("الكل ($totalCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTabFilter == "ACTIVE",
                onClick = { selectedTabFilter = "ACTIVE" },
                selectedContentColor = themeColors.accent,
                unselectedContentColor = themeColors.textSecondary,
                text = { Text("الحالية ($activeCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTabFilter == "COMPLETED",
                onClick = { selectedTabFilter = "COMPLETED" },
                selectedContentColor = themeColors.accent,
                unselectedContentColor = themeColors.textSecondary,
                text = { Text("المكتملة ($completedCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTabFilter == "CANCELLED",
                onClick = { selectedTabFilter = "CANCELLED" },
                selectedContentColor = themeColors.accent,
                unselectedContentColor = themeColors.textSecondary,
                text = { Text("الملغاة ($cancelledCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Booking List Content
        if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(themeColors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد حجوزات مسجلة في هذا التبويب",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary
                    )
                    Text(
                        text = "يمكنك حجز موعد جديد مع الفنيين والمحلات والمراكز بسهولة",
                        fontSize = 12.sp,
                        color = themeColors.textSecondary
                    )
                    Button(
                        onClick = onCreateNewBookingClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accent,
                            contentColor = onAccentColor
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("empty_create_booking_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء طلب حجز جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            var pageLimit by remember { mutableIntStateOf(20) }
            val paginatedList = remember(filteredBookings, pageLimit) {
                filteredBookings.take(pageLimit)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
            ) {
                items(
                    items = paginatedList,
                    key = { it.id.ifBlank { it.bookingCode } }
                ) { bk ->
                    BookingCardItem(
                        booking = bk,
                        themeColors = themeColors,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        isProvider = isProvider,
                        onStatusChange = onStatusChange,
                        onEditClick = { bookingToEdit = it },
                        onCancelClick = { bookingToCancel = it },
                        onDeleteClick = { bookingToDelete = it },
                        onOpenChatClick = { onOpenChatClick(it) },
                        isPaymentFeatureEnabled = isPaymentFeatureEnabled,
                        onTrackStatusClick = onTrackStatusClick,
                        onSubmitPaymentClick = onSubmitPaymentClick
                    )
                }

                if (filteredBookings.size > paginatedList.size) {
                    item {
                        Button(
                            onClick = { pageLimit += 20 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.surface,
                                contentColor = themeColors.textPrimary
                            )
                        ) {
                            Text(
                                "تحميل المزيد من الحجوزات (${paginatedList.size} من ${filteredBookings.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    val currentBookingToEdit = bookingToEdit
    if (currentBookingToEdit != null) {
        BookingEditDialog(
            booking = currentBookingToEdit,
            isAdmin = isAdmin,
            onDismiss = { bookingToEdit = null },
            onConfirmEdit = { updatedBooking, pass ->
                onUpdateBooking(updatedBooking)
                bookingToEdit = null
                Toast.makeText(context, "تم تحديث بيانات الحجز بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Cancellation Dialog
    val bkCancel = bookingToCancel
    if (bkCancel != null) {
        val bk = bkCancel
        BookingCancellationDialog(
            booking = bk,
            onDismiss = { bookingToCancel = null },
            onConfirmCancel = { pass, reason ->
                onCancelBooking(bk, reason, pass)
                bookingToCancel = null
                Toast.makeText(context, "تم إرسال طلب إلغاء الحجز", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    val bkDelete = bookingToDelete
    if (bkDelete != null) {
        val bk = bkDelete
        AlertDialog(
            onDismissRequest = { bookingToDelete = null },
            containerColor = themeColors.surface,
            title = {
                Text(
                    "تأكيد حذف الحجز النهائي",
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف الحجز رقم (${bk.bookingCode}) بشكل نهائي؟",
                    color = themeColors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBooking(bk)
                        bookingToDelete = null
                        Toast.makeText(context, "تم حذف الحجز بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("حذف نهائي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { bookingToDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.textSecondary)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
