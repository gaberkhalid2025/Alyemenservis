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
import com.example.utils.FullTestReport
import com.example.utils.RegistrationTestRunner
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

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

    val testRunner = remember { RegistrationTestRunner(context) }

    var isRunning by remember { mutableStateOf(false) }
    var currentProgressText by remember { mutableStateOf("اضغط على الزر أدناه لبدء اختبار التسجيل الشامل للأقسام الثمانية...") }
    var testReport by remember { mutableStateOf<FullTestReport?>(null) }

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
                    .padding(bottom = 16.dp)
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
                    text = "🧪 الفحص والتحقق الفعلي لطلبات التسجيل",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // Info Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ℹ️ معلومات هامة عن الفحص العملي:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "هذا الفحص يتصل مباشرة بقاعدة بيانات Firebase حقيقية ويقوم بحقن وفحص 8 حسابات تمثل كافة أقسام الانضمام، ثم يعيد محاكاة الموافقة والرفض ويتحقق من الانتقال والعرَض في الكولكشنات المناسبة.",
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
                    .padding(bottom = 16.dp)
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

            // Test results list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (testReport == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "لم يتم تشغيل الفحص بعد.\nانقر على زر 'بدء الاختبار الشامل' أدناه للتشغيل والمراقبة لحظياً.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Title and Stats Summary
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📊 نتائج الفحص العملي المكتمل:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("إجمالي الأقسام: ${testReport!!.totalSections}", fontSize = 12.sp, color = Color.LightGray)
                                        Text("الناجح: ${testReport!!.passed} ✅", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                        Text("الفشل المققصود: ${testReport!!.failed} 🛑", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Each step
                        itemsIndexed(testReport!!.steps) { _, step ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = step.section,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (step.groupVerified) "✅ ناجح" else "❌ فشل",
                                            color = if (step.groupVerified) Color.Green else Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("رقم الهاتف المستخدم: ${step.phone}", fontSize = 11.sp, color = Color.LightGray)
                                    Text("الاسم المستهدف: ${step.name}", fontSize = 11.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Checks checklist row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "التسجيل: " + if (step.registered) "✅" else "❌",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Firestore: " + if (step.firestoreReceived) "✅" else "❌",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "النتيجة: " + if (step.status == "APPROVED") "🟢 قبول" else "🛑 رفض",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "المزامنة: " + if (step.groupVerified) "✅" else "❌",
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }

                                    if (step.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📝 ملاحظة: ${step.notes}",
                                            fontSize = 11.sp,
                                            color = themeColors.accent,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (testReport != null) {
                    Button(
                        onClick = {
                            val sb = StringBuilder()
                            sb.append("📋 تقرير فحص التسجيل الشامل التلقائي:\n")
                            sb.append("تاريخ الفحص: ${testReport!!.timestamp}\n")
                            testReport!!.steps.forEach { s ->
                                val statusAr = if (s.approved) "موافقة وقبول" else "رفض وتطهير"
                                val finalStatus = if (s.groupVerified) "✅ ناجح" else "❌ فشل"
                                sb.append("- ${s.section} (${s.phone}): $statusAr | النتيجة: $finalStatus\n")
                            }
                            clipboardManager.setText(AnnotatedString(sb.toString()))
                            Toast.makeText(context, "📋 تم نسخ نص التقرير الإجمالي بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ التقرير النهائي للحافظة", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

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
                                        testRunner.runFullTest(
                                            onProgress = { currentProgressText = it },
                                            onComplete = { report ->
                                                testReport = report
                                                isRunning = false
                                                currentProgressText = "✅ تم الانتهاء من دورة الفحص الكاملة وحفظ التقرير بنجاح!"
                                            }
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        isRunning = false
                                        currentProgressText = "❌ حدث خطأ أثناء تشغيل الفحص: ${e.localizedMessage}"
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRunning) "جاري الفحص..." else "بدء الاختبار الشامل", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!isRunning) {
                                isRunning = true
                                scope.launch {
                                    try {
                                        testRunner.cleanAllTestData { currentProgressText = it }
                                        testReport = null
                                        currentProgressText = "🧹 تم تطهير وحذف جميع السجلات الاختبارية من قاعدة البيانات بنجاح!"
                                    } catch (e: Exception) {
                                        e.printStackTrace()
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
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "تنظيف",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تنظيف البيانات", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
