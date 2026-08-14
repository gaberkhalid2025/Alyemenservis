@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.dialogs




import android.content.Intent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import okhttp3.MediaType.Companion.toMediaType

import com.example.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.home.*
import com.example.ui.screens.map.*
import com.example.ui.screens.bookings.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.assistant.*
import com.example.ui.screens.register.*
import com.example.ui.screens.status.*
import com.example.ui.screens.about.*
import com.example.ui.screens.chat.*
import com.example.ui.screens.notifications.*
import com.example.ui.screens.dashboard.*
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun QuickServiceRequestDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onRequestCreated: () -> Unit
) {
    val context = LocalContext.current
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()

    var nameInput by remember(currentUserName, currentUserPhone) { mutableStateOf(currentUserName.ifEmpty { if (currentUserPhone.isNotBlank()) "عميل ($currentUserPhone)" else "" }) }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var selectedCategoryTab by remember { mutableStateOf("SERVICES") } // SERVICES, STORES, RESTAURANTS, MEDICAL, PROPERTIES, JOBS
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var selectedServiceType by remember { mutableStateOf("سباكة وتمديدات") }
    var problemDescription by remember { mutableStateOf("") }
    var pinCodeInput by remember { mutableStateOf("") }

    val yemeniCities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "ذمار", "عمران", "صعدة", "مأرب", "شبوة", "البيضاء", "لحج", "أبين", "المهرة")
    
    val categoryTabs = listOf(
        Triple("SERVICES", "🔧 الخدمات والفنيين", listOf("سباكة وتمديدات", "كهرباء وصيانة", "تكييف وتبريد", "صيانة سيارات", "نقل عفش", "تنظيف ومكافحة", "برمجة وهواتف", "أخرى")),
        Triple("STORES", "🏪 المراكز والمتاجر", listOf("مواد غذائية", "إلكترونيات وهواتف", "أجهزة منزلية", "ملابس وأزياء", "عطور ومستحضرات", "أثاث منزلي", "أخرى")),
        Triple("RESTAURANTS", "🍔 المطاعم والكافيهات", listOf("وجبات سريعة", "مشويات ومندي", "حلويات وعصائر", "مأكولات شعبية", "قهوة ومشروبات", "أخرى")),
        Triple("MEDICAL", "🏥 المراكز الطبية", listOf("عيادة عامة وطوارئ", "أسنان وتجميل", "صيدلية وتوصيل أدوية", "مختبر وتحاليل", "أشعة", "أخرى")),
        Triple("PROPERTIES", "🏠 العقارات والأراضي", listOf("شقق إيجار", "بيوت ومنازل للبيع", "أراضي وعقارات تجارية", "استئجار مفروش", "أخرى")),
        Triple("JOBS", "💼 الوظائف والخدمات", listOf("وظيفة شاغرة", "طلب عمل/مهنة", "خدمات حرة", "أخرى"))
    )

    val currentSubTypes = categoryTabs.find { it.first == selectedCategoryTab }?.third ?: listOf("عام")

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = themeColors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚡", fontSize = 24.sp)
                        Column {
                            Text("اطلب خدمتك الآن (المزاد العكسي)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                            Text("أرسل طلبك فوراً لجميع الفنيين المعتمدين في مدينتك", fontSize = 10.sp, color = themeColors.textSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Red)
                    }
                }

                Divider(color = themeColors.accent.copy(alpha = 0.2f))

                // Customer Name Input
                Column {
                    Text("👤 اسم العميل / صاحب الطلب:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Phone Input
                Column {
                    Text("📱 رقم الهاتف اليمني للتواصل (9 أرقام):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        placeholder = { Text("77XXXXXXX / 73XXXXXXX", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // City Choice
                Column {
                    Text("🏙️ اختر المدينة / المحافظة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(yemeniCities.size) { idx ->
                            val c = yemeniCities[idx]
                            val isSel = selectedCity == c
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { selectedCity = c }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            ) {
                                Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                // Category Section Choice
                Column {
                    Text("📂 قسم المنشأة / الطلب:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(categoryTabs.size) { idx ->
                            val cat = categoryTabs[idx]
                            val isSel = selectedCategoryTab == cat.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable {
                                        selectedCategoryTab = cat.first
                                        selectedServiceType = cat.third.firstOrNull() ?: ""
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            ) {
                                Text(cat.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                // Specialty Choice
                Column {
                    Text("🔧 التخصص أو نوع السلعة/الخدمة المطلوبة:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(currentSubTypes.size) { idx ->
                            val s = currentSubTypes[idx]
                            val isSel = selectedServiceType == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { selectedServiceType = s }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            ) {
                                Text(s, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                // Problem Description
                Column {
                    Text("📝 وصف المشكلة أو الخدمة بالتفصيل:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = problemDescription,
                        onValueChange = { problemDescription = it },
                        placeholder = { Text("اكتب تفاصيل ما تحتاجه، مثل: عطل في الخزان، تسريب مياه، صيانة مكيف...", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // PIN Code Protection Field
                Column {
                    Text("🔒 رمز PIN لحماية وتعديل الطلب (رمز سري):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = pinCodeInput,
                        onValueChange = { if (it.length <= 6) pinCodeInput = it },
                        placeholder = { Text("رمز سري لحماية إلغاء/تعديل الطلب (مثال: 1234)", color = Color.Gray, fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        val cleanP = phoneInput.trim()
                        if (cleanP.length != 9 || !(cleanP.startsWith("77") || cleanP.startsWith("73") || cleanP.startsWith("71") || cleanP.startsWith("70") || cleanP.startsWith("78"))) {
                            android.widget.Toast.makeText(context, "⚠️ يرجى إدخال رقم هاتف يمني صحيح مكون من 9 أرقام (77/73/71/70/78)", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val sectionLabel = categoryTabs.find { it.first == selectedCategoryTab }?.second ?: "المنشآت"
                        val desc = if (problemDescription.isBlank()) "طلب عاجل [$sectionLabel]: $selectedServiceType في $selectedCity" else "[ $sectionLabel - $selectedServiceType ]: $problemDescription"

                        viewModel.addBooking(
                            name = nameInput.ifEmpty { "عميل" },
                            phone = cleanP,
                            area = selectedCity,
                            serviceType = desc,
                            providerId = "ALL_${selectedCategoryTab}",
                            providerName = "جميع مزودي $sectionLabel",
                            dateString = "طلب عاجل الآن ⚡",
                            timeString = "المزاد العكسي الشامل",
                            customPassword = pinCodeInput.trim()
                        )

                        viewModel.triggerNotification("🚀 تم نشر طلبك في قسم ($sectionLabel) بنجاح! تم تنبيه جميع الجهات والمزودين في $selectedCity")
                        android.widget.Toast.makeText(context, "✅ تم إرسال طلبك بنجاح لجميع المزودين والمراكز في قسم $sectionLabel! ترقب عروضهم وأسعارهم مباشرة في طلباتي.", android.widget.Toast.LENGTH_LONG).show()

                        onRequestCreated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الطلب لجميع المزودين والمتاجر فوراً 🚀", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
