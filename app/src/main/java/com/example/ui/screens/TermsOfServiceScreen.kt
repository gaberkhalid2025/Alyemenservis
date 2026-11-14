@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens
import com.example.ui.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun TermsOfServiceScreen(
    onBack: () -> Unit = {},
    themeColors: VisualThemePalette
) {
    val cardBg = themeColors.surface
    val textColor = themeColors.textPrimary
    val primaryColor = themeColors.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شروط وأحكام الاستخدام") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background,
                    titleContentColor = textColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "شروط وأحكام الاستخدام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                "القواعد المنظمة لاستخدام منصة دليل خدمات اليمن",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        "استخدامك لتطبيق دليل خدمات اليمن يعني موافقتك التامة على الالتزام بالقوانين المعمول بها، واحترام مقدمي الخدمات، ودفع التكاليف المقرة عبر وسائل الدفع المعتمدة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        lineHeight = 22.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TermItem(
                        icon = Icons.Default.CheckCircle,
                        title = "1. التزامات العميل ومقدم الخدمة",
                        desc = "الالتزام بالمواعيد المحددة والتعامل بأمانة وشفافية في تحديد المشاكل وتقديم الأسعار."
                    )
                    TermItem(
                        icon = Icons.Default.Info,
                        title = "2. سياسة الإلغاء والضمان",
                        desc = "تخضع الخدمات لضمان الفني أو المحل المباشر، وتحدد إدارة التطبيق شروط استرداد العربون وفقاً للحالة."
                    )
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            val termsPrefs = androidx.compose.runtime.remember {
                context.getSharedPreferences("yemen_services_terms", android.content.Context.MODE_PRIVATE)
            }
            var hasAgreedTerms by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(
                    termsPrefs.getBoolean("has_agreed_terms", false) &&
                        termsPrefs.getString("agreed_terms_version", "") == "2.0"
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الإصدار: 2.0 • محدث لعام 2026", fontSize = 11.sp, color = textColor.copy(alpha = 0.65f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasAgreedTerms, onCheckedChange = { hasAgreedTerms = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("أوافق على شروط وأحكام الاستخدام", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Button(
                        onClick = {
                            termsPrefs.edit()
                                .putBoolean("has_agreed_terms", true)
                                .putString("agreed_terms_version", "2.0")
                                .putLong("agreed_terms_at", System.currentTimeMillis())
                                .apply()
                            onBack()
                        },
                        enabled = hasAgreedTerms,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ الموافقة والمتابعة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TermItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
