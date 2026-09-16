package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class BookingStateMachineTest {

    // ============================================
    // 1. اختبارات الانتقالات الصحيحة (Valid Transitions)
    // ============================================
    
    @Test
    fun `PENDING to ACCEPTED is valid`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "ACCEPTED"))
    }
    
    @Test
    fun `PENDING to REJECTED is valid`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "REJECTED"))
    }
    
    @Test
    fun `PENDING to CANCELLED is valid`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "CANCELLED"))
    }
    
    @Test
    fun `ACCEPTED to IN_PROGRESS is valid`() {
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "IN_PROGRESS"))
    }
    
    @Test
    fun `IN_PROGRESS to COMPLETED is valid`() {
        assertTrue(BookingStateMachine.canTransition("IN_PROGRESS", "COMPLETED"))
    }
    
    @Test
    fun `COMPLETED to PAID is valid`() {
        assertTrue(BookingStateMachine.canTransition("COMPLETED", "PAID"))
    }
    
    @Test
    fun `PAID to CLOSED is valid`() {
        assertTrue(BookingStateMachine.canTransition("PAID", "CLOSED"))
    }
    
    // ============================================
    // 2. اختبارات الانتقالات غير الصحيحة (Invalid)
    // ============================================
    
    @Test
    fun `PENDING to COMPLETED is invalid`() {
        assertFalse(BookingStateMachine.canTransition("PENDING", "COMPLETED"))
    }
    
    @Test
    fun `PENDING to IN_PROGRESS is invalid`() {
        assertFalse(BookingStateMachine.canTransition("PENDING", "IN_PROGRESS"))
    }
    
    @Test
    fun `ACCEPTED to COMPLETED is invalid`() {
        assertFalse(BookingStateMachine.canTransition("ACCEPTED", "COMPLETED"))
    }
    
    @Test
    fun `CANCELLED cannot transition anywhere`() {
        assertFalse(BookingStateMachine.canTransition("CANCELLED", "ACCEPTED"))
        assertFalse(BookingStateMachine.canTransition("CANCELLED", "PENDING"))
        assertFalse(BookingStateMachine.canTransition("CANCELLED", "IN_PROGRESS"))
    }
    
    @Test
    fun `COMPLETED cannot go back to PENDING`() {
        assertFalse(BookingStateMachine.canTransition("COMPLETED", "PENDING"))
    }
    
    // ============================================
    // 3. اختبارات نفس الحالة (Self Transition)
    // ============================================
    
    @Test
    fun `Same status always returns true`() {
        assertTrue(BookingStateMachine.canTransition("PENDING", "PENDING"))
        assertTrue(BookingStateMachine.canTransition("ACCEPTED", "ACCEPTED"))
        assertTrue(BookingStateMachine.canTransition("COMPLETED", "COMPLETED"))
    }
    
    // ============================================
    // 4. اختبارات getAvailableTransitions
    // ============================================
    
    @Test
    fun `getAvailableTransitions for PENDING returns 4 options`() {
        val transitions = BookingStateMachine.getAvailableTransitions("PENDING")
        assertEquals(4, transitions.size)
        assertTrue(transitions.contains("ACCEPTED"))
        assertTrue(transitions.contains("REJECTED"))
        assertTrue(transitions.contains("CANCELLED"))
        assertTrue(transitions.contains("UNDER_REVIEW"))
    }
    
    @Test
    fun `getAvailableTransitions for CANCELLED returns empty`() {
        val transitions = BookingStateMachine.getAvailableTransitions("CANCELLED")
        assertTrue(transitions.isEmpty())
    }
    
    @Test
    fun `getAvailableTransitions for CLOSED returns empty`() {
        val transitions = BookingStateMachine.getAvailableTransitions("CLOSED")
        assertTrue(transitions.isEmpty())
    }
    
    // ============================================
    // 5. اختبارات Terminal Status
    // ============================================
    
    @Test
    fun `CLOSED is terminal`() {
        assertTrue(BookingStateMachine.isTerminalStatus("CLOSED"))
    }
    
    @Test
    fun `CANCELLED is terminal`() {
        assertTrue(BookingStateMachine.isTerminalStatus("CANCELLED"))
    }
    
    @Test
    fun `REJECTED is terminal`() {
        assertTrue(BookingStateMachine.isTerminalStatus("REJECTED"))
    }
    
    @Test
    fun `PENDING is not terminal`() {
        assertFalse(BookingStateMachine.isTerminalStatus("PENDING"))
    }
    
    @Test
    fun `IN_PROGRESS is not terminal`() {
        assertFalse(BookingStateMachine.isTerminalStatus("IN_PROGRESS"))
    }
    
    // ============================================
    // 6. اختبارات Labels و Colors
    // ============================================
    
    @Test
    fun `getStatusLabel for PENDING returns correct Arabic`() {
        assertEquals("قيد الانتظار", BookingStateMachine.getStatusLabel("PENDING"))
    }
    
    @Test
    fun `getStatusLabel for ACCEPTED returns correct Arabic`() {
        assertEquals("مقبول", BookingStateMachine.getStatusLabel("ACCEPTED"))
    }
    
    @Test
    fun `getStatusColor for PENDING returns correct hex`() {
        assertEquals("#F59E0B", BookingStateMachine.getStatusColor("PENDING"))
    }
    
    @Test
    fun `getStatusColor for COMPLETED returns correct hex`() {
        assertEquals("#059669", BookingStateMachine.getStatusColor("COMPLETED"))
    }
    
    // ============================================
    // 7. اختبارات fromCode
    // ============================================
    
    @Test
    fun `fromCode returns correct enum`() {
        assertEquals(BookingStatus.PENDING, BookingStatus.fromCode("PENDING"))
        assertEquals(BookingStatus.ACCEPTED, BookingStatus.fromCode("ACCEPTED"))
        assertEquals(BookingStatus.COMPLETED, BookingStatus.fromCode("COMPLETED"))
    }
    
    @Test
    fun `fromCode with invalid code returns PENDING`() {
        assertEquals(BookingStatus.PENDING, BookingStatus.fromCode("INVALID"))
    }
    
    @Test
    fun `fromCode is case insensitive`() {
        assertEquals(BookingStatus.PENDING, BookingStatus.fromCode("pending"))
        assertEquals(BookingStatus.ACCEPTED, BookingStatus.fromCode("accepted"))
    }
}
