@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

import com.example.ui.screens.dashboard.*
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
import com.example.ui.*
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
/* MockMapViewScreen has been moved to com.example.ui.screens.map.MockMapViewScreen */
fun JoinRequestStatusScreen(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val context = LocalContext.current
    val joinPhone by viewModel.joinRequestPhone.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val chatChannels by viewModel.chatChannels.collectAsState()

    var activeProviderChatChannel by remember { mutableStateOf<com.example.data.ChatChannelEntity?>(null) }
    var replyInputText by remember { mutableStateOf("") }

    LaunchedEffect(chatChannels, activeProviderChatChannel?.id) {
        activeProviderChatChannel?.id?.let { activeId ->
            val latestCh = chatChannels.find { it.id == activeId }
            if (latestCh != null && latestCh.messages.size != activeProviderChatChannel?.messages?.size) {
                activeProviderChatChannel = latestCh
            }
        }
    }

    val matchingPending = pendingProviders.find { it.phone == joinPhone }
    val matchingApproved = providers.find { it.phone == joinPhone }
    val recoveryWaitingPhone by viewModel.passwordRecoveryWaitingPhone.collectAsState()
    val isWaitingRecovery = recoveryWaitingPhone.trim() == joinPhone.trim() && joinPhone.isNotEmpty()
    
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val ratings by viewModel.ratings.collectAsState()

    val matchingStore = remember(stores, joinPhone) {
        val cleanJoin = joinPhone.trim().replace(" ", "").replace("+", "")
        stores.find { 
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanJoin || 
             it.phone.trim().replace(" ", "").replace("+", "") == cleanJoin) && 
            cleanJoin.isNotEmpty() && !it.isDeleted 
        }
    }
    val matchingProperty = remember(properties, joinPhone) {
        val cleanJoin = joinPhone.trim().replace(" ", "").replace("+", "")
        properties.find { 
            (it.ownerId.trim().replace(" ", "").replace("+", "") == cleanJoin || 
             it.phone.trim().replace(" ", "").replace("+", "") == cleanJoin) && 
            cleanJoin.isNotEmpty() && !it.isDeleted 
        }
    }

    if (matchingStore != null && matchingStore.isActive) {
        val accType = if (matchingStore.sectionId.contains("restaurant") || matchingStore.name.contains("مطعم") || matchingStore.description.contains("أكل") || matchingStore.description.contains("وجبة")) "RESTAURANT" else if (matchingStore.sectionId.contains("medical") || matchingStore.name.contains("عيادة") || matchingStore.name.contains("طبي")) "MEDICAL" else "STORE"
        UnifiedBusinessProfileDashboard(accountType = accType, providerId = matchingStore.id, viewModel = viewModel, themeColors = themeColors)
        return
    }
    if (matchingProperty != null && matchingProperty.isActive) {
        UnifiedBusinessProfileDashboard(accountType = "REAL_ESTATE", providerId = matchingProperty.id, viewModel = viewModel, themeColors = themeColors)
        return
    }
    if (matchingApproved != null) {
        UnifiedBusinessProfileDashboard(accountType = "TECHNICIAN", providerId = matchingApproved.id, viewModel = viewModel, themeColors = themeColors)
        return
    }

    // Look for rejection notification
    val rejectionNotif = notifications.find { 
        it.targetValue == joinPhone && (it.title.contains("رفض") || it.message.contains("رفض") || it.title.contains("تنويه"))
    }

    LaunchedEffect(rejectionNotif, matchingApproved, matchingPending) {
        if (rejectionNotif != null && matchingApproved == null && matchingPending == null) {
            android.widget.Toast.makeText(context, "❌ تم تنبيهك: ${rejectionNotif.message}", android.widget.Toast.LENGTH_LONG).show()
            viewModel.cancelOrResetJoinRequest(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = when {
                    matchingApproved != null || matchingStore != null || matchingProperty != null -> Color(0xFF10B981)
                    rejectionNotif != null -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val categories by viewModel.categories.collectAsState()
                when {
                    isWaitingRecovery -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏳", fontSize = 28.sp)
                        }

                        Text(
                            text = "في انتظار إعادة تعيين كلمة المرور",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "تم إرسال طلب استعادة كلمة المرور الخاصة بحسابك إلى الإدارة والأدمن بنجاح. نحن بانتظار مراجعة طلبك وإعادة تعيين كلمة المرور أو إرسال تفاصيل التحقق والمحادثة الفورية.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.setPasswordRecoveryWaitingPhone("")
                                viewModel.cancelOrResetJoinRequest(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("إعادة محاولة الدخول 🔄", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    matchingStore != null && matchingStore.isActive -> {
                        StoreOwnerDashboardLayout(store = matchingStore, viewModel = viewModel, themeColors = themeColors, ratings = ratings)
                    }
                    matchingStore != null && !matchingStore.isActive -> {
                        // Store / Restaurant / Medical Center Pending Approval Screen
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏳", fontSize = 28.sp)
                        }

                        Text(
                            text = "⏳ طلب انضمام نشاطك التجاري قيد المراجعة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "تم استلام طلب انضمام '${matchingStore.name}' بنجاح وهو قيد المراجعة والتدقيق الإداري. فور تفعيل حسابك من قبل الإدارة ستتمكن من إدارة المنتجات واستقبال الطلبات مباشرة.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2214)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📋 تفاصيل النشاط المقدم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text("• الاسم: ${matchingStore.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("• المالك: ${matchingStore.ownerName}", fontSize = 11.sp, color = Color.White)
                                Text("• الهاتف: ${matchingStore.phone}", fontSize = 11.sp, color = Color.White)
                                Text("• المحافظة والحي: ${matchingStore.cityId} - ${matchingStore.localNeighborhood}", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.cancelOrResetJoinRequest(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("❌ إلغاء والعودة لشاشة التسجيل", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    matchingProperty != null && matchingProperty.isActive -> {
                        PropertyOwnerDashboardLayout(property = matchingProperty, viewModel = viewModel, themeColors = themeColors, ratings = ratings)
                    }
                    matchingProperty != null && !matchingProperty.isActive -> {
                        // Property / Real Estate Pending Approval Screen
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏳", fontSize = 28.sp)
                        }

                        Text(
                            text = "⏳ إعلان عقارك قيد المراجعة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "تم استلام إعلان العقار '${matchingProperty.title}' بنجاح وهو قيد التدقيق الإداري للتأكد من صحة التفاصيل. فور الاعتماد سيظهر للجميع في قسم العقارات.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2214)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📋 تفاصيل العقار المقدم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text("• العنوان: ${matchingProperty.title}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("• السعر المعلن: ${matchingProperty.price} ${matchingProperty.currency}", fontSize = 11.sp, color = Color.White)
                                Text("• الهاتف: ${matchingProperty.phone}", fontSize = 11.sp, color = Color.White)
                                Text("• المحافظة والحي: ${matchingProperty.cityId} - ${matchingProperty.localNeighborhood}", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.cancelOrResetJoinRequest(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("❌ إلغاء والعودة لشاشة التسجيل", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    matchingApproved != null -> {
                        // Accepted (Image 2 design style + Full Interactive Provider Dashboard)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✅",
                                fontSize = 28.sp
                            )
                        }
                        
                        Text(
                            text = "🎉 تم تفعيل حسابك كفني معتمد!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "مرحباً بك يا غالي! حسابك نشط الآن في دليل كل خدمات اليمن ومتاح لجميع العملاء للتواصل والحجز المباشر.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        
                        // 1. Profile Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111C15)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF10B981), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👷", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = matchingApproved.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💼", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val catName = if (matchingApproved.categoryId == "other" && matchingApproved.customCategoryName.isNotEmpty()) matchingApproved.customCategoryName else (categories.find { it.id == matchingApproved.categoryId }?.name ?: "صيانة فنية")
                                        Text(catName, fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📍", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${matchingApproved.area} - ${matchingApproved.localNeighborhood}", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }

                        // 2. Active Status Toggle Button
                        Button(
                            onClick = {
                                viewModel.updateProviderEntity(matchingApproved.copy(isAvailable = !matchingApproved.isAvailable))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (matchingApproved.isAvailable) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, if (matchingApproved.isAvailable) Color(0xFFEF4444) else Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text(
                                text = if (matchingApproved.isAvailable) "🔴 تغيير حالتك الحالية إلى: مشغول مؤقتاً" else "🟢 تغيير حالتك الحالية إلى: متاح للعمل فوراً",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (matchingApproved.isAvailable) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }

                        // 3. Private Bookings Section
                        val myBookings = bookings.filter { it.providerId == matchingApproved.id }
                        Text("📅 طلبات الحجز والعمل الموجهة لك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myBookings.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد طلبات حجز موجهة لك حالياً.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myBookings.forEach { b ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("العميل: ${b.customerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                text = when(b.status) {
                                                    "PENDING" -> "⏳ بانتظار تأكيدك"
                                                    "APPROVED", "IN_PROGRESS" -> "🟢 مقبول وجاري التنفيذ"
                                                    "REJECTED" -> "❌ مرفوض"
                                                    "COMPLETED" -> "✅ مكتمل"
                                                    else -> b.status
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when(b.status) {
                                                    "PENDING" -> Color.Yellow
                                                    "APPROVED", "IN_PROGRESS", "COMPLETED" -> Color.Green
                                                    else -> Color.Red
                                                }
                                            )
                                        }
                                        Text("📞 هاتف العميل للتواصل: ${b.customerPhone}", fontSize = 10.sp, color = themeColors.accent)
                                        Text("📍 موقع ومحافظة العميل: ${b.customerArea}", fontSize = 10.sp, color = Color.LightGray)
                                        Text("🔧 الخدمة المطلوبة: ${b.serviceType}", fontSize = 10.sp, color = Color.LightGray)
                                        Text("⏰ موعد الزيارة: ${b.dateString} - ${b.timeString}", fontSize = 10.sp, color = Color.LightGray)
                                        
                                        if (b.status == "PENDING") {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { viewModel.updateBookingStatus(b.id, "IN_PROGRESS") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("موافقة وقبول العمل ✅", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { viewModel.updateBookingStatus(b.id, "REJECTED", "اعتذر الفني لإنشغاله") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("اعتذار ورفض العمل ❌", fontSize = 9.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Conversations/Chats section
                        val myChats = chatChannels.filter { it.id.contains("chat_p_${matchingApproved.id}_") || it.id.contains("_u_${matchingApproved.id}") }
                        Text("💬 محادثات العملاء المباشرة معك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myChats.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد محادثات نشطة مع عملاء حالياً.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myChats.forEach { ch ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        activeProviderChatChannel = ch
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💬", fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ch.userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(ch.lastMessage, fontSize = 10.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("◀️", fontSize = 10.sp, color = themeColors.accent)
                                    }
                                }
                            }
                        }

                        // 5. Notifications Section
                        val myNotifs = notifications.filter { it.targetValue == matchingApproved.phone || it.targetType == "ALL" }
                        Text("🔔 الإشعارات والتعميمات الإدارية الموجهة إليك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent, modifier = Modifier.align(Alignment.Start))
                        if (myNotifs.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text("📭 لا توجد إشعارات إدارية جديدة.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            myNotifs.take(4).forEach { notif ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(notif.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.message, fontSize = 11.sp, color = Color.LightGray, lineHeight = 16.sp)
                                        
                                        if (notif.customerPhone.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Call Button
                                                Button(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${notif.customerPhone}"))
                                                        context.startActivity(intent)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                ) {
                                                    Text("📞 اتصال مباشر", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                // Chat Button
                                                Button(
                                                    onClick = {
                                                        val sorted = listOf(matchingApproved.id, notif.customerPhone).sorted()
                                                        val newTargetId = "chat_${sorted[0]}_${sorted[1]}"
                                                        val oldTargetId = "chat_p_${matchingApproved.id}_u_${notif.customerPhone}"
                                                        val existingChannel = chatChannels.find { it.id == newTargetId } ?: chatChannels.find { it.id == oldTargetId }
                                                        val finalId = existingChannel?.id ?: newTargetId
                                                        
                                                        viewModel.getOrCreateChatChannel(
                                                            matchingApproved.id,
                                                            matchingApproved.name,
                                                            notif.customerPhone,
                                                            notif.customerName
                                                        )
                                                        
                                                        activeProviderChatChannel = existingChannel ?: com.example.data.ChatChannelEntity(
                                                            id = finalId,
                                                            userName = notif.customerName.ifEmpty { "عميل جديد" },
                                                            lastMessage = "",
                                                            messages = emptyList()
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                ) {
                                                    Text("💬 مراسلة فورية", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Local Chat Dialog Overlay for Provider
                        activeProviderChatChannel?.let { ch ->
                            Dialog(onDismissRequest = { activeProviderChatChannel = null }) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, themeColors.accent),
                                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("💬 محادثة العميل: ${ch.userName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(ch.messages) { msg ->
                                                    val isMe = msg.senderId == matchingApproved.id
                                                    val alignment = if (isMe) Alignment.End else Alignment.Start
                                                    val bubbleBg = if (isMe) themeColors.primary else Color.Gray.copy(alpha = 0.3f)
                                                    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(bubbleBg)
                                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(msg.message, fontSize = 10.sp, color = Color.White)
                                                        }
                                                        val formattedDate = remember(msg.timestamp) {
                                                            try {
                                                                java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale("ar")).format(java.util.Date(msg.timestamp))
                                                            } catch (e: Exception) { "" }
                                                        }
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(msg.senderName, fontSize = 8.sp, color = themeColors.textSecondary)
                                                            Text("• $formattedDate", fontSize = 8.sp, color = Color.Gray)
                                                            if (isMe) {
                                                                val statusIcon = when (msg.status) {
                                                                    "READ" -> "✓✓ تمت القراءة"
                                                                    "DELIVERED" -> "✓✓ تم التسليم"
                                                                    else -> "✓ مرسل"
                                                                }
                                                                val statusColor = when (msg.status) {
                                                                    "READ" -> Color(0xFF10B981)
                                                                    "DELIVERED" -> themeColors.accent
                                                                    else -> Color.Gray
                                                                }
                                                                Text(statusIcon, fontSize = 8.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = replyInputText,
                                            onValueChange = { replyInputText = it },
                                            placeholder = { Text("اكتب ردك هنا...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (replyInputText.trim().isNotEmpty()) {
                                                        viewModel.replyToChatChannel(ch.id, matchingApproved.id, replyInputText.trim(), matchingApproved.name)
                                                        // Refresh chat locally
                                                        val updatedMsg = com.example.data.ChatMessageEntity(
                                                            id = UUID.randomUUID().toString(),
                                                            senderId = matchingApproved.id,
                                                            message = replyInputText.trim(),
                                                            timestamp = System.currentTimeMillis(),
                                                            senderName = matchingApproved.name
                                                        )
                                                        activeProviderChatChannel = ch.copy(
                                                            messages = ch.messages + updatedMsg,
                                                            lastMessage = replyInputText.trim(),
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                        replyInputText = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("إرسال الرد السريع ⚡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    matchingPending != null -> {
                        if (matchingPending.status == "REJECTED") {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "❌", fontSize = 28.sp)
                            }

                            Text(
                                text = "❌ تم رفض طلب انضمامك كفني",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "عذراً يا غالي! تم مراجعة طلبك من قبل الإدارة ورفضه للسبب التالي:\n\n👉 [ ${matchingPending.reason.ifEmpty { "مستندات غير واضحة أو ناقصة" }} ] 👈\n\nيمكنك الضغط على الزر أدناه لتعديل بياناتك وإعادة إرسال طلبك بكل سهولة.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.cancelOrResetJoinRequest(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("✍️ تعديل وإعادة تقديم الطلب", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            // Pending approval screen
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⏳", fontSize = 28.sp)
                            }

                            Text(
                                text = "⏳ طلب انضمامك قيد المراجعة حالياً",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "أهلاً بك يا غالي! تم إرسال طلبك بنجاح وهو قيد المراجعة والتدقيق الإداري الآن من قبل الإدارة. نسعد بانضمامك وسنبلغك فور التنشيط والموافقة لتتمكن من استقبال الحجوزات والدردشة مباشرة!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2214)),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📋 تفاصيل الطلب المقدم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                    Text("• الاسم الثلاثي: ${matchingPending.name}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("• رقم الهاتف: ${matchingPending.phone}", fontSize = 11.sp, color = Color.White)
                                    val catName = if (matchingPending.categoryId == "other" && matchingPending.customCategoryName.isNotEmpty()) matchingPending.customCategoryName else (categories.find { it.id == matchingPending.categoryId }?.name ?: "صيانة عامة")
                                    Text("• القسم والتخصص: $catName", fontSize = 11.sp, color = Color.White)
                                    Text("• منطقة الخدمة: ${matchingPending.area}", fontSize = 11.sp, color = Color.White)
                                    if (matchingPending.localNeighborhood.isNotEmpty()) {
                                        Text("• الحي/الشارع التفصيلي: ${matchingPending.localNeighborhood}", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.cancelOrResetJoinRequest(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Text("❌ إلغاء الطلب والعودة للرئيسية", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    else -> {
                        // Fallback state
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "ℹ️", fontSize = 28.sp)
                        }

                        Text(
                            text = "لم يتم العثور على طلب نشط",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "يرجى تقديم طلب انضمام جديد للتمتع بكافة الميزات والتحكم ببياناتك وحجوزاتك كفني معتمد.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.cancelOrResetJoinRequest(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("📝 العودة لشاشة التسجيل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}