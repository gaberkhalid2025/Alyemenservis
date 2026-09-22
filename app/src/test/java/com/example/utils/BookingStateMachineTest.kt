package com.example.utils

import com.example.data.BookingEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🧪 اختبارات آلة حالات الحجز (BookingStateMachine)
 * تختبر قواعد الانتقال المسموحة، الحالات النهائية، وقاعدة الـ 8 ساعات للإلغاء.
 */
class BookingStateMachineTest {

    @Test
    fun `test allowed transitions from PENDING`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "ACCEPTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "UNDER_REVIEW"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "REJECTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "CANCELLED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "PENDING")) // Same state allowed

        assertFalse(BookingStateMachine.canTransition("PENDING", "COMPLETED"))
        assertFalse(BookingStateMachine.canTransition("PENDING", "PAID"))
        assertFalse(BookingStateMachine.canTransition("PENDING", "CLOSED"))
    }

    @Test
    fun `test allowed transitions from ACCEPTED`() {
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "IN_PROGRESS"))
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "CANCELLED"))

        assertFalse(BookingStateMachine.canTransition("ACCEPTED", "PENDING"))
        assertFalse(BookingStateMachine.canTransition("ACCEPTED", "COMPLETED"))
    }

    @Test
    fun `test allowed transitions from IN_PROGRESS`() {
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "COMPLETED"))
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "CANCELLED"))

        assertFalse(BookingStateMachine.canTransition("IN_PROGRESS", "PENDING"))
        assertFalse(BookingStateMachine.canTransition("IN_PROGRESS", "ACCEPTED"))
    }

    @Test
    fun `test terminal states cannot transition to anything`() {
        val terminalStates = listOf("CLOSED", "CANCELLED", "REJECTED")
        for (state in terminalStates) {
            assertTrue(BookingStateMachine.isTerminalStatus(state))
            assertFalse(BookingStateMachine.canTransition(state, "PENDING"))
            assertFalse(BookingStateMachine.canTransition(state, "ACCEPTED"))
            assertFalse(BookingStateMachine.canTransition(state, "IN_PROGRESS"))
            assertEquals(emptyList<String>(), BookingStateMachine.getAvailableTransitions(state))
        }
    }

    @Test
    fun `test non terminal states return correct isTerminalStatus`() {
        assertFalse(BookingStateMachine.isTerminalStatus("PENDING"))
        assertFalse(BookingStateMachine.isTerminalStatus("ACCEPTED"))
        assertFalse(BookingStateMachine.isTerminalStatus("IN_PROGRESS"))
        assertFalse(BookingStateMachine.isTerminalStatus("UNDER_REVIEW"))
    }

    @Test
    fun `test canCancel returns false for locked booking`() {
        val lockedBooking = BookingEntity(
            id = "b_locked",
            status = "PENDING",
            isLocked = true
        )
        assertFalse(BookingStateMachine.canCancel(lockedBooking))
    }

    @Test
    fun `test canCancel returns false for in-progress or completed booking`() {
        val inProgressBooking = BookingEntity(id = "b_prog", status = "IN_PROGRESS")
        assertFalse(BookingStateMachine.canCancel(inProgressBooking))

        val completedBooking = BookingEntity(id = "b_comp", status = "COMPLETED")
        assertFalse(BookingStateMachine.canCancel(completedBooking))
    }

    @Test
    fun `test canCancel enforces 8-hour cancellation rule`() {
        // موعد بعد ساعتين فقط من الآن (أقل من 8 ساعات) -> لا يمكن الإلغاء
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val twoHoursLater = System.currentTimeMillis() + (2 * 60 * 60 * 1000L)
        val dateTwoHours = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(twoHoursLater))
        val timeTwoHours = SimpleDateFormat("HH:mm", Locale.US).format(Date(twoHoursLater))

        val urgentBooking = BookingEntity(
            id = "b_urgent",
            status = "PENDING",
            date = dateTwoHours,
            time = timeTwoHours
        )
        assertFalse("Cannot cancel when less than 8 hours remain", BookingStateMachine.canCancel(urgentBooking))

        // موعد بعد يومين من الآن (أكثر من 8 ساعات) -> مسموح الإلغاء
        val twoDaysLater = System.currentTimeMillis() + (48 * 60 * 60 * 1000L)
        val dateTwoDays = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(twoDaysLater))
        val timeTwoDays = SimpleDateFormat("HH:mm", Locale.US).format(Date(twoDaysLater))

        val farBooking = BookingEntity(
            id = "b_far",
            status = "PENDING",
            date = dateTwoDays,
            time = timeTwoDays
        )
        assertTrue("Can cancel when more than 8 hours remain", BookingStateMachine.canCancel(farBooking))
    }

    @Test
    fun `test getStatusLabel returns Arabic labels`() {
        assertEquals("قيد الانتظار", BookingStateMachine.getStatusLabel("PENDING"))
        assertEquals("مقبول", BookingStateMachine.getStatusLabel("ACCEPTED"))
        assertEquals("مرفوض", BookingStateMachine.getStatusLabel("REJECTED"))
        assertEquals("ملغي", BookingStateMachine.getStatusLabel("CANCELLED"))
        assertEquals("مكتمل", BookingStateMachine.getStatusLabel("COMPLETED"))
    }

    @Test
    fun `test getStatusColor returns valid hex format`() {
        val pendingColor = BookingStateMachine.getStatusColor("PENDING")
        assertTrue(pendingColor.startsWith("#"))

        val acceptedColor = BookingStateMachine.getStatusColor("ACCEPTED")
        assertEquals("#10B981", acceptedColor)
    }
}
