package com.example.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 🧪 WalletManagerTest
 * اختبارات وحدة لإدارة المحافظ الإلكترونية المتعددة العملات والتحقق من القيود المالية وتجميد المحافظ
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WalletManagerTest {

    private lateinit var walletManager: WalletManager

    @Before
    fun setup() {
        walletManager = WalletManager(context = null)
    }

    @Test
    fun `createWallet initializes active multi-currency wallet with zero balances`() {
        val result = walletManager.createWallet(userId = "777123456", type = "PROVIDER")
        assertTrue(result.isSuccess)
        val wallet = result.getOrNull()
        assertNotNull(wallet)
        assertEquals("wallet_777123456", wallet?.id)
        assertEquals("PROVIDER", wallet?.type)
        assertEquals("ACTIVE", wallet?.status)
        assertEquals(0.0, walletManager.getBalance("777123456", "YER"), 0.001)
        assertEquals(0.0, walletManager.getBalance("777123456", "USD"), 0.001)
        assertEquals(0.0, walletManager.getBalance("777123456", "SAR"), 0.001)
    }

    @Test
    fun `deposit rejects non-positive amounts and fractional YER`() = runTest {
        val zeroDeposit = walletManager.deposit("wallet_u1", 0.0, "YER")
        assertTrue(zeroDeposit.isFailure)

        val negativeDeposit = walletManager.deposit("wallet_u1", -500.0, "YER")
        assertTrue(negativeDeposit.isFailure)

        val fractionalYerDeposit = walletManager.deposit("wallet_u1", 100.5, "YER")
        assertTrue(fractionalYerDeposit.isFailure)
    }

    @Test
    fun `withdraw rejects non-positive amounts and fractional YER`() = runTest {
        val zeroWithdraw = walletManager.withdraw("wallet_u1", 0.0, "YER")
        assertTrue(zeroWithdraw.isFailure)

        val fractionalYerWithdraw = walletManager.withdraw("wallet_u1", 250.75, "YER")
        assertTrue(fractionalYerWithdraw.isFailure)
    }

    @Test
    fun `transfer rejects invalid non-positive amount`() = runTest {
        val invalidTransfer = walletManager.transfer("wallet_a", "wallet_b", -10.0, "USD")
        assertTrue(invalidTransfer.isFailure)
    }

    @Test
    fun `freezeWallet and unfreezeWallet update wallet status`() {
        walletManager.createWallet("771112223", "USER")
        val freezeRes = walletManager.freezeWallet("wallet_771112223", "مراجعة أمنية")
        assertTrue(freezeRes.isSuccess)

        val unfreezeRes = walletManager.unfreezeWallet("wallet_771112223")
        assertTrue(unfreezeRes.isSuccess)
    }
}
