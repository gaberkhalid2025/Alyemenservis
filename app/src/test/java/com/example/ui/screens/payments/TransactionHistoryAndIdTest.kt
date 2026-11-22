package com.example.ui.screens.payments

import com.example.data.NotificationEntity
import com.example.data.models.Transaction
import com.example.utils.EntityIdGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHistoryAndIdTest {

    @Test
    fun computeTotalsLong_includesRefundInflowsAndTransferPaymentOutflows_ignoringPending() {
        val txs = listOf(
            Transaction(id = "t1", amount = 10_000.0, type = "DEPOSIT", status = "COMPLETED"),
            Transaction(id = "t2", amount = 2_500.0, type = "REFUND", status = "COMPLETED"),
            Transaction(id = "t3", amount = 1_500.0, type = "WITHDRAWAL", status = "COMPLETED"),
            Transaction(id = "t4", amount = 3_000.0, type = "PAYMENT", status = "COMPLETED"),
            Transaction(id = "t5", amount = 500.0, type = "TRANSFER", status = "COMPLETED"),
            Transaction(id = "t6", amount = 99_000.0, type = "DEPOSIT", status = "PENDING"),
            Transaction(id = "t7", amount = 40_000.0, type = "WITHDRAWAL", status = "FAILED"),
            Transaction(id = "t8", amount = -100.0, type = "DEPOSIT", status = "COMPLETED")
        )

        val (inflows, outflows) = TransactionHistoryViewModel.computeTotalsLong(txs)
        assertEquals(12_500L, inflows)
        assertEquals(5_000L, outflows)
    }

    @Test
    fun entityIdGenerator_supportsAllNewPrefixes() {
        val tags = listOf("MED", "REST", "WAL", "TOPUP", "REP", "ADM", "OWN", "SUP", "TXN")
        for (tag in tags) {
            val generated = EntityIdGenerator.generate(tag)
            assertTrue("Expected valid prefixed id for $tag, got $generated", EntityIdGenerator.isValidPrefixedId(generated))
            assertEquals(tag, EntityIdGenerator.getPrefix(generated))
        }
    }

    @Test
    fun notificationEntity_expiryCheckRespectsTimestamp() {
        val now = 1_700_000_000_000L
        val nonExpiring = NotificationEntity(id = "n1", title = "A", message = "B", expiryTimestamp = 0L)
        val future = NotificationEntity(id = "n2", title = "A", message = "B", expiryTimestamp = now + 5_000L)
        val past = NotificationEntity(id = "n3", title = "A", message = "B", expiryTimestamp = now - 1L)

        assertFalse(nonExpiring.isExpired(now))
        assertFalse(future.isExpired(now))
        assertTrue(past.isExpired(now))
    }
}
