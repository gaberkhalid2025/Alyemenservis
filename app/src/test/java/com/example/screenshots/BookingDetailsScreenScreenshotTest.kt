package com.example.screenshots

import androidx.compose.ui.unit.LayoutDirection
import com.example.data.BookingEntity
import com.example.ui.screens.bookings.BookingDetailsScreen
import org.junit.Test

/**
 * 📋 BookingDetailsScreenScreenshotTest
 * Visual screenshot testing for the Booking Details Screen with all mandatory fields:
 * - Customer Name (اسم العميل)
 * - Phone Number (رقم الهاتف)
 * - Service Type (نوع الخدمة)
 * - Date (التاريخ)
 * - Time (الوقت)
 * - Secret PIN Code (رمز PIN)
 * - Booking Status (حالة الحجز)
 * - Total Amount (المبلغ الإجمالي)
 * - Down Payment / Deposit (الدفعة المقدمة)
 * Tested across Light, Dark, RTL, and LTR.
 */
class BookingDetailsScreenScreenshotTest : RoborazziTestBase() {

    private val detailedBooking = BookingEntity(
        id = "bk_fixture_99182",
        bookingNumber = "BK-2026-991",
        customerName = "علي محمد الطالب",
        customerPhone = "771234567",
        customerArea = "صنعاء - حدة - شارع بيروت",
        clientAddress = "صنعاء - حدة - شارع بيروت",
        serviceType = "صيانة كهرباء وتمديدات منزلية",
        serviceName = "صيانة كهرباء وتمديدات منزلية",
        serviceDetails = "فحص لوحة القواطع الرئيسية وتغيير قاطع التماس",
        date = "2026-05-15",
        time = "10:00 AM",
        bookingPassword = "8421",
        status = "ACCEPTED",
        totalAmount = 25000.0,
        advancePayment = 5000.0,
        currency = "YER"
    )

    @Test
    fun bookingDetails_light() {
        captureScreen(
            screenshotName = "booking_details_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            BookingDetailsScreen(
                booking = detailedBooking,
                userRole = "CLIENT",
                onBack = {}
            )
        }
    }

    @Test
    fun bookingDetails_dark() {
        captureScreen(
            screenshotName = "booking_details_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            BookingDetailsScreen(
                booking = detailedBooking,
                userRole = "CLIENT",
                onBack = {}
            )
        }
    }

    @Test
    fun bookingDetails_rtl() {
        captureScreen(
            screenshotName = "booking_details_rtl",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            BookingDetailsScreen(
                booking = detailedBooking,
                userRole = "CLIENT",
                onBack = {}
            )
        }
    }

    @Test
    fun bookingDetails_ltr() {
        captureScreen(
            screenshotName = "booking_details_ltr",
            darkTheme = false,
            layoutDirection = LayoutDirection.Ltr
        ) {
            BookingDetailsScreen(
                booking = detailedBooking,
                userRole = "CLIENT",
                onBack = {}
            )
        }
    }
}
