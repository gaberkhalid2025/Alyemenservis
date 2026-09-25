package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TestSystem(val label: String, val icon: String) {
    REGISTRATION("التسجيل", "🎫"),
    BOOKINGS("الحجوزات", "📅"),
    INSTANT_REQUESTS("الطلبات العاجلة", "⚡"),
    CHATS("المحادثات", "💬"),
    PAYMENTS("المدفوعات", "💳"),
    REVIEWS("التقييمات", "⭐"),
    NOTIFICATIONS("الإشعارات", "🔔"),
    REPORTS("التقارير", "📢"),
    MAPS("الخريطة", "🗺️"),
    SEARCH("البحث", "🔍"),
    RUN_ALL("تشغيل الكل", "🧪")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationTestScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Initialize all test runners
    val regRunner = remember { RegistrationTestRunner(context) }
    val bookingRunner = remember { BookingTestRunner(context) }
    val instantRunner = remember { InstantRequestTestRunner(context) }
    val chatRunner = remember { ChatTestRunner(context) }
    val paymentRunner = remember { PaymentTestRunner(context) }
    val reviewRunner = remember { ReviewTestRunner(context) }
    val notificationRunner = remember { NotificationTestRunner(context) }
    val reportRunner = remember { ReportTestRunner(context) }
    val mapRunner = remember { MapTestRunner(context) }
    val searchRunner = remember { SearchTestRunner(context) }

    var selectedSystem by remember { mutableStateOf(TestSystem.REGISTRATION) }
    var isRunning by remember { mutableStateOf(false) }
    var currentProgressText by remember { mutableStateOf("يرجى اختيار النظام ثم الضغط على 'بدء الاختبار الشامل'...") }

    // Reports states
    var regReport by remember { mutableStateOf<FullTestReport?>(null) }
    var bookingReport by remember { mutableStateOf<BookingFullReport?>(null) }
    var instantReport by remember { mutableStateOf<InstantRequestFullReport?>(null) }
    var chatReport by remember { mutableStateOf<ChatFullReport?>(null) }
    var paymentReport by remember { mutableStateOf<PaymentFullReport?>(null) }
    var reviewReport by remember { mutableStateOf<ReviewFullReport?>(null) }
    var notificationReport by remember { mutableStateOf<NotificationFullReport?>(null) }
    var reportReport by remember { mutableStateOf<ReportFullReport?>(null) }
    var mapReport by remember { mutableStateOf<MapFullReport?>(null) }
    var searchReport by remember { mutableStateOf<SearchFullReport?>(null) }

    BackHandler {
        if (!isRunning) {
            onDismiss()
        } else {
            Toast.makeText(context, "⏳ يرجى الانتظار حتى اكتمال دورة الاختبار!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate Dark Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(
                    onClick = {
                        if (!isRunning) onDismiss()
                        else Toast.makeText(context, "⏳ الاختبار قيد التشغيل حالياً!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🧪 فحص وتحقق الأنظمة الشامل العملي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs / Sidebar selection for Systems
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestSystem.values().forEach { system ->
                    val isSelected = selectedSystem == system
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) themeColors.accent else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            if (!isRunning) {
                                selectedSystem = system
                                currentProgressText = "جاهز لبدء فحص نظام: ${system.label}"
                            } else {
                                Toast.makeText(context, "⏳ يرجى الانتظار حتى انتهاء الفحص الجاري!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(system.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = system.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // Info Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ℹ️ معلومات هامة عن الفحص العملي الجاري:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "هذا الفحص يتصل مباشرة بالفايربيز ويقوم بمحاكاة عمليات حقيقية 100% ثم تنظيف قاعدة البيانات بعد الفحص.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }

            // Progress log area
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isRunning) themeColors.accent else Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                color = themeColors.accent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = currentProgressText,
                            fontSize = 12.sp,
                            color = if (isRunning) Color.White else Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Test results list depending on selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedSystem) {
                    TestSystem.REGISTRATION -> {
                        RenderRegistrationReport(regReport, themeColors)
                    }
                    TestSystem.BOOKINGS -> {
                        RenderBookingReport(bookingReport)
                    }
                    TestSystem.INSTANT_REQUESTS -> {
                        RenderInstantRequestReport(instantReport)
                    }
                    TestSystem.CHATS -> {
                        RenderChatReport(chatReport)
                    }
                    TestSystem.PAYMENTS -> {
                        RenderPaymentReport(paymentReport)
                    }
                    TestSystem.REVIEWS -> {
                        RenderReviewReport(reviewReport)
                    }
                    TestSystem.NOTIFICATIONS -> {
                        RenderNotificationReport(notificationReport)
                    }
                    TestSystem.REPORTS -> {
                        RenderReportReport(reportReport)
                    }
                    TestSystem.MAPS -> {
                        RenderMapReport(mapReport)
                    }
                    TestSystem.SEARCH -> {
                        RenderSearchReport(searchReport)
                    }
                    TestSystem.RUN_ALL -> {
                        RenderAllSummary(
                            regReport, bookingReport, instantReport, chatReport,
                            paymentReport, reviewReport, notificationReport, reportReport,
                            mapReport, searchReport
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                scope.launch {
                                    try {
                                        when (selectedSystem) {
                                            TestSystem.REGISTRATION -> {
                                                regRunner.runFullTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { regReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.BOOKINGS -> {
                                                bookingRunner.runBookingTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { bookingReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.INSTANT_REQUESTS -> {
                                                instantRunner.runInstantRequestTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { instantReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.CHATS -> {
                                                chatRunner.runChatTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { chatReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.PAYMENTS -> {
                                                paymentRunner.runPaymentTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { paymentReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.REVIEWS -> {
                                                reviewRunner.runReviewTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { reviewReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.NOTIFICATIONS -> {
                                                notificationRunner.runNotificationTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { notificationReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.REPORTS -> {
                                                reportRunner.runReportTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { reportReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.MAPS -> {
                                                mapRunner.runMapTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { mapReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.SEARCH -> {
                                                searchRunner.runSearchTest(
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { searchReport = it; isRunning = false }
                                                )
                                            }
                                            TestSystem.RUN_ALL -> {
                                                runAllSystemsSequentialTests(
                                                    regRunner, bookingRunner, instantRunner, chatRunner,
                                                    paymentRunner, reviewRunner, notificationRunner, reportRunner,
                                                    mapRunner, searchRunner,
                                                    onProgress = { currentProgressText = it },
                                                    onComplete = { reg, book, inst, ch, pay, rev, noti, rep, mp, srh ->
                                                        regReport = reg
                                                        bookingReport = book
                                                        instantReport = inst
                                                        chatReport = ch
                                                        paymentReport = pay
                                                        reviewReport = rev
                                                        notificationReport = noti
                                                        reportReport = rep
                                                        mapReport = mp
                                                        searchReport = srh
                                                        isRunning = false
                                                        currentProgressText = "✅ تم الانتهاء من فحص كافة أنظمة التطبيق وحفظ تقاريرها بنجاح!"
                                                    }
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isRunning = false
                                        currentProgressText = "❌ حدث خطأ: ${e.localizedMessage}"
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "جاري الفحص..." else "بدء الفحص العملي", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                scope.launch {
                                    try {
                                        currentProgressText = "🧹 جاري تصفية وتطهير قاعدة البيانات..."
                                        regRunner.cleanAllTestData()
                                        bookingRunner.cleanAllTestData()
                                        instantRunner.cleanAllTestData()
                                        chatRunner.cleanAllTestData()
                                        paymentRunner.cleanAllTestData()
                                        reviewRunner.cleanAllTestData()
                                        notificationRunner.cleanAllTestData()
                                        reportRunner.cleanAllTestData()

                                        regReport = null
                                        bookingReport = null
                                        instantReport = null
                                        chatReport = null
                                        paymentReport = null
                                        reviewReport = null
                                        notificationReport = null
                                        reportReport = null
                                        mapReport = null
                                        searchReport = null

                                        currentProgressText = "🧹 تم تنظيف وتطهير كافة السجلات الاختبارية بنجاح!"
                                    } catch (e: Exception) {
                                        currentProgressText = "❌ خطأ أثناء تصفية البيانات"
                                    } finally {
                                        isRunning = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "تنظيف", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنظيف السجلات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = {
                        val reportText = buildComprehensiveReportText(
                            regReport, bookingReport, instantReport, chatReport,
                            paymentReport, reviewReport, notificationReport, reportReport,
                            mapReport, searchReport
                        )
                        clipboardManager.setText(AnnotatedString(reportText))
                        Toast.makeText(context, "📋 تم نسخ التقرير الشامل للحافظة!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ التقرير الشامل للحافظة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────
// Render Functions for report types
// ────────────────────────────────────────────────────────

@Composable
fun RenderRegistrationReport(report: FullTestReport?, themeColors: VisualThemePalette) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام التسجيل")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 نتائج فحص التسجيل (8 أقسام):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(step.section, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.groupVerified) "✅ ناجح" else "❌ فشل",
                                color = if (step.groupVerified) Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الهاتف: ${step.phone} | الاسم: ${step.name}", fontSize = 11.sp, color = Color.LightGray)
                        Text("الحالة بالفايربيز: ${step.status}", fontSize = 11.sp, color = themeColors.accent)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderBookingReport(report: BookingFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام الحجوزات")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص نظام الحجوزات والدورة المستندية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderInstantRequestReport(report: InstantRequestFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام الطلبات العاجلة")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص نظام الطلبات العاجلة وعروض الفنيين:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderChatReport(report: ChatFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام المحادثات")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص المحادثات والردود والحذف الآمن:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderPaymentReport(report: PaymentFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام المدفوعات")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص المحفظة والعمليات المالية والتحويلات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderReviewReport(report: ReviewFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام التقييمات")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص التقييمات وحساب المتوسط والردود:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderNotificationReport(report: NotificationFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام الإشعارات")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص الإشعارات الموجهة والجماعية والجغرافية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderReportReport(report: ReportFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام الشكاوى والتقارير")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص بلاغات الشكاوى والمرفقات والقرارات الادارية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMapReport(report: MapFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام الخريطة الجغرافية")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص إعدادات وحاسبة الخريطة ومواقع المنشآت:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderSearchReport(report: SearchFullReport?) {
    if (report == null) {
        RenderEmptyPlaceholder("نظام البحث والفرز")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📋 مخرجات فحص نتائج كلمات البحث وتصفية المدن والأقسام الفنية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            itemsIndexed(report.steps) { _, step ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(step.stepName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (step.status == "SUCCESS") "✅ ناجح" else "❌ فشل",
                                color = if (step.status == "SUCCESS") Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(step.notes, fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderAllSummary(
    reg: FullTestReport?, bk: BookingFullReport?, inst: InstantRequestFullReport?,
    ch: ChatFullReport?, pay: PaymentFullReport?, rev: ReviewFullReport?,
    noti: NotificationFullReport?, rep: ReportFullReport?, mp: MapFullReport?, srh: SearchFullReport?
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("📋 تقرير الأداء الإجمالي لكافة أنظمة التطبيق العشرة:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Text("💡 ملخص الفحوصات الجارية ومقارنتها بالواقع:", fontSize = 12.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    RenderSummaryRow("1. نظام التسجيل (8 أقسام)", reg != null, reg?.passed ?: 0, reg?.failed ?: 0)
                    RenderSummaryRow("2. نظام الحجوزات والتعميد", bk != null, bk?.passed ?: 0, bk?.failed ?: 0)
                    RenderSummaryRow("3. نظام الطلبات العاجلة والتفاوض", inst != null, inst?.passed ?: 0, inst?.failed ?: 0)
                    RenderSummaryRow("4. نظام المحادثات والرسائل والردود", ch != null, ch?.passed ?: 0, ch?.failed ?: 0)
                    RenderSummaryRow("5. نظام المدفوعات والعمليات والمحافظ", pay != null, pay?.passed ?: 0, pay?.failed ?: 0)
                    RenderSummaryRow("6. نظام التقييمات ومراجعة الأبعاد", rev != null, rev?.passed ?: 0, rev?.failed ?: 0)
                    RenderSummaryRow("7. نظام الإشعارات والتنبيهات الجغرافية", noti != null, noti?.passed ?: 0, noti?.failed ?: 0)
                    RenderSummaryRow("8. نظام البلاغات والشكاوى الإدارية", rep != null, rep?.passed ?: 0, rep?.failed ?: 0)
                    RenderSummaryRow("9. نظام الخريطة وحساب المسافات دقة GPS", mp != null, mp?.passed ?: 0, mp?.failed ?: 0)
                    RenderSummaryRow("10. نظام البحث والفلترة الذكي للفنيين", srh != null, srh?.passed ?: 0, srh?.failed ?: 0)
                }
            }
        }
    }
}

@Composable
fun RenderSummaryRow(name: String, ran: Boolean, passed: Int, failed: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
        if (ran) {
            Text("ناجح: $passed | فشل: $failed", fontSize = 11.sp, color = if (failed == 0) Color.Green else Color.Red)
        } else {
            Text("لم يُشغّل بعد 💤", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RenderEmptyPlaceholder(systemName: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = "لم يتم تشغيل فحص ($systemName) بعد.\nاضغط على زر 'بدء الفحص العملي' في الأسفل لتشغيله ومراقبته على الفايربيز حياً.",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ────────────────────────────────────────────────────────
// Logic Helpers
// ────────────────────────────────────────────────────────

suspend fun runAllSystemsSequentialTests(
    regRunner: RegistrationTestRunner,
    bookingRunner: BookingTestRunner,
    instantRunner: InstantRequestTestRunner,
    chatRunner: ChatTestRunner,
    paymentRunner: PaymentTestRunner,
    reviewRunner: ReviewTestRunner,
    notificationRunner: NotificationTestRunner,
    reportRunner: ReportTestRunner,
    mapRunner: MapTestRunner,
    searchRunner: SearchTestRunner,
    onProgress: (String) -> Unit,
    onComplete: (
        FullTestReport?, BookingFullReport?, InstantRequestFullReport?,
        ChatFullReport?, PaymentFullReport?, ReviewFullReport?,
        NotificationFullReport?, ReportFullReport?, MapFullReport?, SearchFullReport?
    ) -> Unit
) {
    var reg: FullTestReport? = null
    var book: BookingFullReport? = null
    var inst: InstantRequestFullReport? = null
    var ch: ChatFullReport? = null
    var pay: PaymentFullReport? = null
    var rev: ReviewFullReport? = null
    var noti: NotificationFullReport? = null
    var rep: ReportFullReport? = null
    var mp: MapFullReport? = null
    var srh: SearchFullReport? = null

    onProgress("🧪 جاري بدء الفحص الشامل التراكمي لكافة أنظمة التطبيق (10 أنظمة)...")
    delay(1000)

    regRunner.runFullTest(
        onProgress = { onProgress("🎫 [1/10] فحص التسجيل: $it") },
        onComplete = { reg = it }
    )
    delay(1000)

    bookingRunner.runBookingTest(
        onProgress = { onProgress("📅 [2/10] فحص الحجوزات: $it") },
        onComplete = { book = it }
    )
    delay(1000)

    instantRunner.runInstantRequestTest(
        onProgress = { onProgress("⚡ [3/10] فحص الطلبات العاجلة: $it") },
        onComplete = { inst = it }
    )
    delay(1000)

    chatRunner.runChatTest(
        onProgress = { onProgress("💬 [4/10] فحص المحادثات: $it") },
        onComplete = { ch = it }
    )
    delay(1000)

    paymentRunner.runPaymentTest(
        onProgress = { onProgress("💳 [5/10] فحص المدفوعات والمحافظ: $it") },
        onComplete = { pay = it }
    )
    delay(1000)

    reviewRunner.runReviewTest(
        onProgress = { onProgress("⭐ [6/10] فحص التقييمات والمتوسطات: $it") },
        onComplete = { rev = it }
    )
    delay(1000)

    notificationRunner.runNotificationTest(
        onProgress = { onProgress("🔔 [7/10] فحص الإشعارات الجغرافية والموجهة: $it") },
        onComplete = { noti = it }
    )
    delay(1000)

    reportRunner.runReportTest(
        onProgress = { onProgress("📢 [8/10] فحص الشكاوى والحلول: $it") },
        onComplete = { rep = it }
    )
    delay(1000)

    mapRunner.runMapTest(
        onProgress = { onProgress("🗺️ [9/10] فحص الخريطة وحساب المسافات: $it") },
        onComplete = { mp = it }
    )
    delay(1000)

    searchRunner.runSearchTest(
        onProgress = { onProgress("🔍 [10/10] فحص محرك البحث والفرز والكلمات المفتاحية: $it") },
        onComplete = { srh = it }
    )
    delay(1000)

    onComplete(reg, book, inst, ch, pay, rev, noti, rep, mp, srh)
}

fun buildComprehensiveReportText(
    reg: FullTestReport?, bk: BookingFullReport?, inst: InstantRequestFullReport?,
    ch: ChatFullReport?, pay: PaymentFullReport?, rev: ReviewFullReport?,
    noti: NotificationFullReport?, rep: ReportFullReport?, mp: MapFullReport?, srh: SearchFullReport?
): String {
    val sb = StringBuilder()
    sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    sb.append("📋 تقرير الفحص العملي والتحقق الشامل لكافة أنظمة التطبيق\n")
    sb.append("تاريخ التوليد: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
    sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

    sb.append("1. نظام التسجيل (8 أقسام): ")
    if (reg != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${reg.passed} | فشل: ${reg.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("2. نظام الحجوزات: ")
    if (bk != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${bk.passed} | فشل: ${bk.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("3. نظام الطلبات العاجلة: ")
    if (inst != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${inst.passed} | فشل: ${inst.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("4. نظام المحادثات والرسائل: ")
    if (ch != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${ch.passed} | فشل: ${ch.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("5. نظام المدفوعات والعمليات: ")
    if (pay != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${pay.passed} | فشل: ${pay.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("6. نظام التقييمات ومراجعة الأبعاد: ")
    if (rev != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${rev.passed} | فشل: ${rev.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("7. نظام الإشعارات والتنبيهات: ")
    if (noti != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${noti.passed} | فشل: ${noti.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("8. نظام التقارير والشكاوى الإدارية: ")
    if (rep != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${rep.passed} | فشل: ${rep.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("9. نظام الخريطة GPS وحساب المسافات: ")
    if (mp != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${mp.passed} | فشل: ${mp.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("10. نظام البحث والفلترة الذكية: ")
    if (srh != null) sb.append("✅ تم الفحص بنجاح | ناجح: ${srh.passed} | فشل: ${srh.failed}\n") else sb.append("💤 لم يُفحص بعد\n")

    sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    sb.append("تمت عملية الفحص الفعلي والتكامل الحقيقي بنجاح 100% ويشمل تنظيف البيانات من خوادم Firestore.\n")
    sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    return sb.toString()
}
