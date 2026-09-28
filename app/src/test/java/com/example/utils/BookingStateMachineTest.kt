package com.example.utils

import com.example.data.BookingEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 BookingStateMachineTest
 * اختبارات وحدة شاملة لمحرك حالات الحجز وقواعد الانتقال والإلغاء (قاعدة الـ 8 ساعات وقفل المحاولات)
 */
class BookingStateMachineTest {

    @Test
    fun `canTransition allows valid state transitions`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "ACCEPTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "REJECTED"))
        assertTrue(BookingStateMachine.canTransition("PENDING", "CANCELLED"))
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "IN_PROGRESS"))
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "COMPLETED"))
        assertTrue(BookingStateMachine.canTransition("COMPLETED", "PAID"))
        assertTrue(BookingStateMachine.canTransition("PAID", "CLOSED"))
    }

    @Test
    fun `canTransition rejects invalid backward or terminal transitions`() {
        assertFalse(BookingStateMachine.canTransition("COMPLETED", "PENDING"))
        assertFalse(BookingStateMachine.canTransition("CANCELLED", "ACCEPTED"))
        assertFalse(BookingStateMachine.canTransition("REJECTED", "IN_PROGRESS"))
        assertFalse(BookingStateMachine.canTransition("CLOSED", "PENDING"))
    }

    @Test
    fun `isSlotOccupiedStatus returns true for active booking statuses including ACCEPTED and legacy APPROVED`() {
        assertTrue(BookingStateMachine.isSlotOccupiedStatus("PENDING"))
        assertTrue(BookingStateMachine.isSlotOccupiedStatus("ACCEPTED"))
        assertTrue(BookingStateMachine.isSlotOccupiedStatus("APPROVED"))
        assertTrue(BookingStateMachine.isSlotOccupiedStatus("IN_PROGRESS"))
        assertFalse(BookingStateMachine.isSlotOccupiedStatus("CANCELLED"))
        assertFalse(BookingStateMachine.isSlotOccupiedStatus("REJECTED"))
    }

    @Test
    fun `isTerminalStatus identifies terminal states accurately`() {
        assertTrue(BookingStateMachine.isTerminalStatus("CANCELLED"))
        assertTrue(BookingStateMachine.isTerminalStatus("REJECTED"))
        assertTrue(BookingStateMachine.isTerminalStatus("CLOSED"))
        assertFalse(BookingStateMachine.isTerminalStatus("PENDING"))
        assertFalse(BookingStateMachine.isTerminalStatus("ACCEPTED"))
    }

    @Test
    fun `canCancel blocks locked or completed bookings`() {
        val lockedBooking = BookingEntity(id = "b_locked", status = "PENDING", isLocked = true)
        assertFalse(BookingStateMachine.canCancel(lockedBooking))

        val completedBooking = BookingEntity(id = "b_done", status = "COMPLETED", isLocked = false)
        assertFalse(BookingStateMachine.canCancel(completedBooking))

        val inProgressBooking = BookingEntity(id = "b_prog", status = "IN_PROGRESS", isLocked = false)
        assertFalse(BookingStateMachine.canCancel(inProgressBooking))
    }

    @Test
    fun `canCancel allows pending booking without near-term deadline`() {
        val futureBooking = BookingEntity(
            id = "b_future",
            status = "PENDING",
            isLocked = false,
            date = "2099-12-31",
            time = "14:00"
        )
        assertTrue(BookingStateMachine.canCancel(futureBooking))
    }
}
