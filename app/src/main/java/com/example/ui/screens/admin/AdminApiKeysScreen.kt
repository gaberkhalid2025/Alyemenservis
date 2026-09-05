package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.models.*
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.ui.screens.admin.components.*

@Composable
fun AdminApiKeysScreenContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var geminiApiKey by remember { mutableStateOf("") }
    var openaiApiKey by remember { mutableStateOf("") }
    var selectedAiModel by remember { mutableStateOf("gemini-1.5-flash") }
    var showGeminiKey by remember { mutableStateOf(false) }

    var googleMapsKey by remember { mutableStateOf("") }
    var mapboxKey by remember { mutableStateOf("") }
    var selectedMapEngine by remember { mutableStateOf("OPEN_STREET_MAP") }
    var showMapsKey by remember { mutableStateOf(false) }

    var kuraimiToken by remember { mutableStateOf("") }
    var jawwalPayKey by remember { mutableStateOf("") }
    var floosakKey by remember { mutableStateOf("") }
    var oneCashKey by remember { mutableStateOf("") }

    var webhookUrl by remember { mutableStateOf("") }
    var whatsappToken by remember { mutableStateOf("") }
    var smsGatewayKey by remember { mutableStateOf("") }

    val customKeys = remember { mutableStateListOf<Triple<String, String, String>>() }
    var showAddCustomKeyDialog by remember { mutableStateOf(false) }
    var newKeyName by remember { mutableStateOf("") }
    var newKeyValue by remember { mutableStateOf("") }
    var newKeyEndpoint by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.db.collection("settings").document("api_keys").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    geminiApiKey = doc.getString("geminiApiKey") ?: ""
                    openaiApiKey = doc.getString("openaiApiKey") ?: ""
                    selectedAiModel = doc.getString("selectedAiModel") ?: "gemini-1.5-flash"
                    googleMapsKey = doc.getString("googleMapsKey") ?: ""
                    mapboxKey = doc.getString("mapboxKey") ?: ""
                    selectedMapEngine = doc.getString("selectedMapEngine") ?: "OPEN_STREET_MAP"
                    kuraimiToken = doc.getString("kuraimiToken") ?: ""
                    jawwalPayKey = doc.getString("jawwalPayKey") ?: ""
                    floosakKey = doc.getString("floosakKey") ?: ""
                    oneCashKey = doc.getString("oneCashKey") ?: ""
                    webhookUrl = doc.getString("webhookUrl") ?: ""
                    whatsappToken = doc.getString("whatsappToken") ?: ""
                    smsGatewayKey = doc.getString("smsGatewayKey") ?: ""
                }
            }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🔌 إدارة مفاتيح الربط والخدمات السحابية (API Keys)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("صلاحية كاملة لإضافة، تعديل، أو حذف مفاتيح API للمساعد الذكي، الخرائط، ومحافظ الدفع والارتباط السحابي:", fontSize = 11.sp, color = themeColors.textSecondary)

        // 1. مفاتيح المساعد الذكي
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = Color(0xFF60A5FA))
                    Text("🤖 مفاتيح المساعد الذكي (AI Assistant)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    label = { Text("Google Gemini API Key") },
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(if (showGeminiKey) Icons.Default.Close else Icons.Default.Done, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = openaiApiKey,
                    onValueChange = { openaiApiKey = it },
                    label = { Text("OpenAI / DeepSeek API Key (اختياري)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 2. مفاتيح الخرائط
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF34D399))
                    Text("🗺️ مفاتيح الخرائط وتحديد المواقع", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = googleMapsKey,
                    onValueChange = { googleMapsKey = it },
                    label = { Text("Google Maps SDK API Key") },
                    visualTransformation = if (showMapsKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showMapsKey = !showMapsKey }) {
                            Icon(if (showMapsKey) Icons.Default.Close else Icons.Default.Done, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = mapboxKey,
                    onValueChange = { mapboxKey = it },
                    label = { Text("Mapbox Access Token (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 3. محافظ وبوابات الدفع الإلكتروني اليمنية
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFFBBF24))
                    Text("💳 محافظ وبوابات الدفع الإلكتروني", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = kuraimiToken,
                    onValueChange = { kuraimiToken = it },
                    label = { Text("مفتاح بنك الكريمي إكسبرس (Kuraimi API Token)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = jawwalPayKey,
                    onValueChange = { jawwalPayKey = it },
                    label = { Text("مفتاح محفظة جوال بي (Jawwal Pay Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = floosakKey,
                    onValueChange = { floosakKey = it },
                    label = { Text("مفتاح محفظة فلوسك (Floosak Secret Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = oneCashKey,
                    onValueChange = { oneCashKey = it },
                    label = { Text("مفتاح ون كاش / جيب (OneCash API Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 4. الارتباط السحابي والويب هوك
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFA78BFA))
                    Text("☁️ الارتباط السحابي والويب هوك (Webhooks & SMS)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("رابط الخادم السحابي (Cloud Webhook URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = whatsappToken,
                    onValueChange = { whatsappToken = it },
                    label = { Text("مفتاح واتساب للأعمال (WhatsApp Cloud API Token)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = smsGatewayKey,
                    onValueChange = { smsGatewayKey = it },
                    label = { Text("مفتاح بوابة الرسائل القصيرة (SMS Gateway Key)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        // 5. المفاتيح المخصصة
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕ المفاتيح السحابية المخصصة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(
                        onClick = { showAddCustomKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+ إضافة مفتاح جديد", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (customKeys.isEmpty()) {
                    Text("لا توجد مفاتيح مخصصة مضافة حالياً.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    customKeys.forEachIndexed { index, (name, key, endpoint) ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    Text("Endpoint: $endpoint", fontSize = 10.sp, color = Color.LightGray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(key))
                                            Toast.makeText(context, "تم نسخ المفتاح للحافظة", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "نسخ", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { customKeys.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // زر الحفظ والمزامنة الفورية السحابية
        Button(
            onClick = {
                val data = mapOf(
                    "geminiApiKey" to geminiApiKey,
                    "openaiApiKey" to openaiApiKey,
                    "selectedAiModel" to selectedAiModel,
                    "googleMapsKey" to googleMapsKey,
                    "mapboxKey" to mapboxKey,
                    "selectedMapEngine" to selectedMapEngine,
                    "kuraimiToken" to kuraimiToken,
                    "jawwalPayKey" to jawwalPayKey,
                    "floosakKey" to floosakKey,
                    "oneCashKey" to oneCashKey,
                    "webhookUrl" to webhookUrl,
                    "whatsappToken" to whatsappToken,
                    "smsGatewayKey" to smsGatewayKey,
                    "updatedAt" to System.currentTimeMillis()
                )
                viewModel.db.collection("settings").document("api_keys").set(data)
                Toast.makeText(context, "✅ تم حفظ ومزامنة كافة المفاتيح سحابياً فوراً وأمان تام!", Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("💾 حفظ ومزامنة كافة المفاتيح سحابياً الآن", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddCustomKeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomKeyDialog = false },
            title = { Text("➕ إضافة مفتاح ربط سحابي مخصص", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKeyName,
                        onValueChange = { newKeyName = it },
                        label = { Text("اسم الخدمة أو البوابة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newKeyValue,
                        onValueChange = { newKeyValue = it },
                        label = { Text("مفتاح API السري") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newKeyEndpoint,
                        onValueChange = { newKeyEndpoint = it },
                        label = { Text("رابط الربط / Endpoint (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKeyName.isNotBlank() && newKeyValue.isNotBlank()) {
                            customKeys.add(Triple(newKeyName, newKeyValue, newKeyEndpoint))
                            newKeyName = ""
                            newKeyValue = ""
                            newKeyEndpoint = ""
                            showAddCustomKeyDialog = false
                            Toast.makeText(context, "تمت إضافة المفتاح بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomKeyDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}


@Composable
fun AdminApiKeysScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) = AdminApiKeysScreenContent(viewModel, themeColors, modifier)
