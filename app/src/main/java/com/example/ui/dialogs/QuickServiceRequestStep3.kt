package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
internal fun Step3ContactAndSubmit(
    phone: String,
    onPhoneChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    selectedSection: String,
    selectedSpecialty: String,
    selectedCity: String,
    neighborhood: String,
    hasExactCoords: Boolean,
    hasAttachment: Boolean,
    deliveryMethod: String,
    serviceTitle: String,
    urgencyTime: String,
    themeColors: VisualThemePalette
) {
    val sectionArabic = when (selectedSection) {
        "SERVICES" -> "الخدمات والفنيين"
        "STORES" -> "المتاجر والسلع والمحلات"
        "RESTAURANTS" -> "المطاعم والكافيهات"
        "MEDICAL" -> "المراكز الطبية والصيدليات"
        "PROPERTIES" -> "العقارات والشقق والأراضي"
        else -> "عام"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "3️⃣ بيانات التواصل ومراجعة الطلب:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Phone Input (Auto-filled)
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف للتواصل واستلام العروض (9 أرقام) *") },
            placeholder = { Text("مثال: 771234567") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = {
                Text("🇾🇪 +967", fontSize = 11.sp, color = themeColors.accent, modifier = Modifier.padding(start = 8.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // Name input
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("الاسم أو اللقب (اختياري)") },
            placeholder = { Text("مثال: أبو محمد") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        // PIN (Optional)
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("رمز PIN السري لمتابعة الطلب (اختياري - 4 أرقام)") },
            placeholder = { Text("إذا تركته فارغاً سيتم توليد رمز سري تلقائياً") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Summary Review Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ملخص طلبك قبل الإطلاق:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Text("🏷️ القسم والتخصص: $sectionArabic — $selectedSpecialty", fontSize = 11.sp, color = Color.White)
                Text("📍 الموقع: $selectedCity ${if (neighborhood.isNotBlank()) "($neighborhood)" else ""}", fontSize = 11.sp, color = Color.White)
                if (hasExactCoords) {
                    Text("🛰️ الإحداثيات الدقيقة: مرفقة لتحديد أقرب مقدمي الخدمة", fontSize = 10.5.sp, color = Color(0xFF34D399))
                }
                Text("📝 عنوان الطلب: ${serviceTitle.ifBlank { "طلب خدمة" }}", fontSize = 11.sp, color = Color.White)
                if (selectedSection != "SERVICES" && deliveryMethod.isNotBlank()) {
                    Text("🛵 طريقة الاستلام/المعاينة: $deliveryMethod", fontSize = 11.sp, color = Color.LightGray)
                }
                if (hasAttachment) {
                    Text("📷 المرفقات: صورة توضيحية جاهزة للرفع", fontSize = 10.5.sp, color = Color(0xFF38BDF8))
                }
                Text("⏱️ وقت التنفيذ: $urgencyTime", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun QuickServiceSuccessDialog(
    selectedCity: String,
    requestCode: String,
    secretPin: String?,
    themeColors: VisualThemePalette,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDone,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🚀", fontSize = 24.sp)
                Text("تم إطلاق طلبك بنجاح!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "تم توجيه طلبك فوراً للمزودين والمتخصصين في $selectedCity، وستصلك العروض والمزايدات مباشرة على حسابك.",
                    fontSize = 11.5.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("كود متابعة الطلب:", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = requestCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        if (secretPin != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("الرمز السري (PIN):", fontSize = 10.sp, color = Color.Gray)
                                Text(secretPin, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Request Code", "$requestCode (PIN: $secretPin)")
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "📋 تم نسخ كود الطلب للحافظة!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكود للحافظة", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("يمكنك متابعة العروض المقدمة من قسم 'طلباتي' في أي وقت.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("الانتقال إلى طلباتي 📋", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}
