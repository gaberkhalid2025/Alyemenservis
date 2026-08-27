package com.example.ui.screens.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.LoadingState
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.SnackbarType
import com.example.ui.components.showCustomSnackbar
import com.example.util.Transaction
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 💳 TransactionHistoryScreen
 * شاشة عرض سجل المعاملات المالية والمحفظة الإلكترونية متصلة بـ Firebase Firestore
 * مع دعم التصفية، البحث، الشحن الفوري، ورسائل الـ Snackbar التفاعلية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    currentUserId: String = "user_default",
    userRole: String = "USER",
    onBack: () -> Unit = {},
    paymentsViewModel: PaymentsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val numberFormat = remember { DecimalFormat("#,###.##") }

    val transactionsState by paymentsViewModel.transactionsState.collectAsState()
    val allTransactions by paymentsViewModel.transactions.collectAsState()
    val balance by paymentsViewModel.balance.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var showDepositDialog by remember { mutableStateOf(false) }
    var depositAmountText by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        paymentsViewModel.listenToUserWallet(currentUserId)
    }

    val totalDeposits = remember(allTransactions) {
        allTransactions.filter { it.type == "DEPOSIT" && it.status == "COMPLETED" }.sumOf { it.amount }
    }

    val totalWithdrawals = remember(allTransactions) {
        allTransactions.filter { (it.type == "WITHDRAWAL" || it.type == "PAYMENT") && it.status == "COMPLETED" }.sumOf { it.amount }
    }

    val filteredTransactions = remember(allTransactions, searchQuery, selectedTypeFilter, selectedStatusFilter) {
        allTransactions.filter { tx ->
            val matchesType = when (selectedTypeFilter) {
                "DEPOSIT" -> tx.type == "DEPOSIT"
                "WITHDRAWAL" -> tx.type == "WITHDRAWAL"
                "PAYMENT" -> tx.type == "PAYMENT"
                "TRANSFER" -> tx.type == "TRANSFER"
                "REFUND" -> tx.type == "REFUND"
                else -> true
            }
            val matchesStatus = when (selectedStatusFilter) {
                "COMPLETED" -> tx.status == "COMPLETED"
                "PENDING" -> tx.status == "PENDING"
                "FAILED" -> tx.status == "FAILED"
                "CANCELLED" -> tx.status == "CANCELLED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.id.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true)

            matchesType && matchesStatus && matchesSearch
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("سجل المعاملات والمحفظة", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showCustomSnackbar(
                                message = "تم تصدير ملخص المعاملات المالية بنجاح",
                                type = SnackbarType.INFO
                            )
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير التقرير", tint = Color(0xFF00668B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // بطاقة الرصيد والإحصائيات الرئيسية
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00668B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الرصيد الحالي بالمحفظة", color = Color(0xFFE0F2FE), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = numberFormat.format(balance),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ريال يمني", fontSize = 14.sp, color = Color(0xFFBAE6FD), modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }

                        Button(
                            onClick = { showDepositDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00668B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("شحن رصيد", color = Color(0xFF00668B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("إجمالي الإيداعات", fontSize = 11.sp, color = Color(0xFFE0F2FE))
                                Text("${numberFormat.format(totalDeposits)} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFE53935).copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("إجمالي السحوبات", fontSize = 11.sp, color = Color(0xFFE0F2FE))
                                Text("${numberFormat.format(totalWithdrawals)} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("بحث برقم المعاملة أو الوصف...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00668B),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // فلاتر النوع
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val typeFilters = listOf(
                    "ALL" to "الكل",
                    "DEPOSIT" to "إيداع 🟢",
                    "PAYMENT" to "دفع 🔴",
                    "WITHDRAWAL" to "سحب 💸",
                    "TRANSFER" to "تحويل 🔁",
                    "REFUND" to "استرداد ↩️"
                )
                items(typeFilters) { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00668B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // حالة التحميل والعرض
            when (val state = transactionsState) {
                is LoadingState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00668B))
                    }
                }
                is LoadingState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                            Text(state.message, color = Color(0xFF64748B), fontSize = 13.sp)
                            Button(
                                onClick = { paymentsViewModel.listenToUserWallet(currentUserId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                            ) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
                else -> {
                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    "لا توجد معاملات مالية مسجلة حالياً",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { tx ->
                                val isPositive = tx.type == "DEPOSIT" || tx.type == "REFUND"
                                val formattedDate = remember(tx.timestamp) {
                                    SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date(tx.timestamp))
                                }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (isPositive) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.note.ifEmpty { "معاملة مالية" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formattedDate,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "رقم المرجع: ${tx.id}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${if (isPositive) "+" else "-"}${numberFormat.format(tx.amount)} ريال",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when (tx.status.uppercase()) {
                                                    "COMPLETED" -> Color(0xFFE8F5E9)
                                                    "PENDING" -> Color(0xFFFFF3E0)
                                                    "CANCELLED" -> Color(0xFFECEFF1)
                                                    else -> Color(0xFFFFEBEE)
                                                }
                                            ) {
                                                Text(
                                                    text = when (tx.status.uppercase()) {
                                                        "COMPLETED" -> "مكتمل"
                                                        "PENDING" -> "قيد المعالجة"
                                                        "CANCELLED" -> "ملغي"
                                                        else -> "فاشل"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = when (tx.status.uppercase()) {
                                                        "COMPLETED" -> Color(0xFF2E7D32)
                                                        "PENDING" -> Color(0xFFEF6C00)
                                                        "CANCELLED" -> Color(0xFF546E7A)
                                                        else -> Color(0xFFC62828)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة إيداع رصيد فوري
    if (showDepositDialog) {
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("شحن المحفظة الإلكترونية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل المبلغ بالريال اليمني للشحن الفوري:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { depositAmountText = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("مثال: 25000") },
                        trailingIcon = { Text("YER", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = depositAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            paymentsViewModel.deposit(
                                userId = currentUserId,
                                amount = amount,
                                note = "شحن رصيد محفظة فوري",
                                onSuccess = {
                                    showDepositDialog = false
                                    depositAmountText = ""
                                    coroutineScope.launch {
                                        snackbarHostState.showCustomSnackbar(
                                            message = "تم إيداع ${numberFormat.format(amount)} ريال بنجاح!",
                                            type = SnackbarType.SUCCESS
                                        )
                                    }
                                },
                                onError = { err ->
                                    coroutineScope.launch {
                                        snackbarHostState.showCustomSnackbar(
                                            message = err,
                                            type = SnackbarType.ERROR
                                        )
                                    }
                                }
                            )
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showCustomSnackbar(
                                    message = "يرجى إدخال مبلغ صحيح أكبر من صفر",
                                    type = SnackbarType.WARNING
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                ) {
                    Text("تأكيد الشحن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
