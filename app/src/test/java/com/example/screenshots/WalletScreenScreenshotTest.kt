package com.example.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Test

/**
 * 💳 WalletScreenScreenshotTest
 * Visual screenshot testing for the Multi-Currency Wallet Screen:
 * - Balances in 3 currencies: YER (الريال اليمني), USD (الدولار الأمريكي), SAR (الريال السعودي)
 * - Quick action buttons (إيداع، سحب، تحويل، مشاركة)
 * - Recent transaction history items
 * Across Light, Dark, RTL, and LTR.
 */
class WalletScreenScreenshotTest : RoborazziTestBase() {

    @Test
    fun walletScreen_light() {
        captureScreen(
            screenshotName = "wallet_screen_light",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            WalletScreenTestContent()
        }
    }

    @Test
    fun walletScreen_dark() {
        captureScreen(
            screenshotName = "wallet_screen_dark",
            darkTheme = true,
            layoutDirection = LayoutDirection.Rtl
        ) {
            WalletScreenTestContent()
        }
    }

    @Test
    fun walletScreen_rtl() {
        captureScreen(
            screenshotName = "wallet_screen_rtl",
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl
        ) {
            WalletScreenTestContent()
        }
    }

    @Test
    fun walletScreen_ltr() {
        captureScreen(
            screenshotName = "wallet_screen_ltr",
            darkTheme = false,
            layoutDirection = LayoutDirection.Ltr
        ) {
            WalletScreenTestContent()
        }
    }
}

private data class WalletTransactionItem(
    val id: String,
    val title: String,
    val date: String,
    val amount: String,
    val isIncome: Boolean,
    val currency: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreenTestContent() {
    val transactions = listOf(
        WalletTransactionItem(
            id = "tx_1",
            title = "دفعة حجز صيانة كهرباء منزلية",
            date = "اليوم - 11:30 AM",
            amount = "+15,000",
            isIncome = true,
            currency = "YER"
        ),
        WalletTransactionItem(
            id = "tx_2",
            title = "شحن رصيد المحفظة عبر بنك الكريمي",
            date = "أمس - 04:15 PM",
            amount = "+100,000",
            isIncome = true,
            currency = "YER"
        ),
        WalletTransactionItem(
            id = "tx_3",
            title = "سداد اشتراك سنوي لمقدم خدمة",
            date = "20 مايو 2026",
            amount = "-$50.00",
            isIncome = false,
            currency = "USD"
        ),
        WalletTransactionItem(
            id = "tx_4",
            title = "تحويل دفعة لفني صيانة معتمد",
            date = "18 مايو 2026",
            amount = "-500",
            isIncome = false,
            currency = "SAR"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "محفظتي الرقمية",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Multi-Currency Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "إجمالي الأرصدة المتاحة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Yemeni Rial
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الريال اليمني (YER)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "350,000 ر.ي",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )

                        // US Dollar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الدولار الأمريكي (USD)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$1,250.00",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )

                        // Saudi Riyal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الريال السعودي (SAR)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "4,800 ر.س",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickWalletAction(icon = Icons.Default.Add, label = "إيداع شحن")
                    QuickWalletAction(icon = Icons.Default.ArrowUpward, label = "سحب نقدي")
                    QuickWalletAction(icon = Icons.Default.Send, label = "تحويل سريع")
                    QuickWalletAction(icon = Icons.Default.Share, label = "مشاركة QR")
                }
            }

            // Recent Transactions Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات والتحويلات الأخيرة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "كشف حساب",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Transactions List
            items(transactions, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconBg = if (tx.isIncome) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        val iconColor = if (tx.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                        val icon = if (tx.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tx.date,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${tx.amount} ${if (tx.currency != "USD") tx.currency else ""}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickWalletAction(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
