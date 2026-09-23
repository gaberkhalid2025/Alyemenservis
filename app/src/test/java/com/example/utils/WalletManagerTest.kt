package com.example.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 🧪 اختبارات مدير المحفظة المالية (WalletManager)
 * تغطي إدارة الأرصدة المتعددة والتحقق الصارم من العمليات وقواعد العملة اليمنية.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WalletManagerTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var walletManager: WalletManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        walletManager = WalletManager(context = context)
    }

    @Test
    fun `test createWallet initializes active wallet with zero balances`() {
        val result = walletManager.createWallet("usr_test_1", "USER")
        assertTrue("Wallet creation should succeed", result.isSuccess)

        val wallet = result.getOrNull()
        assertNotNull(wallet)
        assertEquals("usr_test_1", wallet?.userId)
        assertEquals("ACTIVE", wallet?.status)
        assertEquals(0.0, wallet?.balanceYer ?: -1.0, 0.001)
        assertEquals(0.0, wallet?.balanceUsd ?: -1.0, 0.001)
        assertEquals(0.0, wallet?.balanceSar ?: -1.0, 0.001)
    }

    @Test
    fun `test getBalance returns correct currency amounts`() {
        val createRes = walletManager.createWallet("usr_test_2", "PROVIDER")
        val wallet = createRes.getOrNull()
        assertNotNull(wallet)

        val yer = walletManager.getBalance("usr_test_2", "YER")
        val usd = walletManager.getBalance("usr_test_2", "USD")
        val sar = walletManager.getBalance("usr_test_2", "SAR")

        assertEquals(0.0, yer, 0.001)
        assertEquals(0.0, usd, 0.001)
        assertEquals(0.0, sar, 0.001)
    }

    @Test
    fun `test deposit rejects zero and negative amounts`() = runTest {
        val walletId = "wallet_usr_test_3"
        walletManager.createWallet("usr_test_3")

        val zeroRes = walletManager.deposit(walletId, 0.0, "YER")
        assertTrue(zeroRes.isFailure)
        assertTrue(zeroRes.exceptionOrNull() is IllegalArgumentException)

        val negRes = walletManager.deposit(walletId, -150.0, "YER")
        assertTrue(negRes.isFailure)
        assertTrue(negRes.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test deposit rejects decimals in Yemeni Rial (YER)`() = runTest {
        val walletId = "wallet_usr_test_4"
        walletManager.createWallet("usr_test_4")

        val decimalRes = walletManager.deposit(walletId, 1500.75, "YER")
        assertTrue("YER deposit with fractions must fail", decimalRes.isFailure)
        val msg = decimalRes.exceptionOrNull()?.message.orEmpty()
        assertTrue("Must specify YER fractions are not allowed", msg.contains("الريال اليمني لا يدعم الكسور"))
    }

    @Test
    fun `test withdraw rejects zero or negative amounts`() = runTest {
        val walletId = "wallet_usr_test_5"
        walletManager.createWallet("usr_test_5")

        val res = walletManager.withdraw(walletId, 0.0, "YER")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is IllegalArgumentException)

        val negRes = walletManager.withdraw(walletId, -500.0, "YER")
        assertTrue(negRes.isFailure)
        assertTrue(negRes.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test withdraw rejects decimals in Yemeni Rial (YER)`() = runTest {
        val walletId = "wallet_usr_test_6"
        walletManager.createWallet("usr_test_6")

        val decimalRes = walletManager.withdraw(walletId, 250.50, "YER")
        assertTrue("YER withdrawal with fractions must fail", decimalRes.isFailure)
        val msg = decimalRes.exceptionOrNull()?.message.orEmpty()
        assertTrue("Must specify YER fractions are not allowed", msg.contains("الريال اليمني لا يدعم الكسور"))
    }

    @Test
    fun `test transfer rejects zero or negative amounts`() = runTest {
        val res = walletManager.transfer("w1", "w2", -100.0, "YER")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `test freeze and unfreeze wallet status`() {
        val res = walletManager.createWallet("usr_test_freeze")
        val walletId = res.getOrNull()!!.id

        val freezeRes = walletManager.freezeWallet(walletId, "نشاط مريب")
        assertTrue(freezeRes.isSuccess)

        val unfreezeRes = walletManager.unfreezeWallet(walletId)
        assertTrue(unfreezeRes.isSuccess)
    }
}
