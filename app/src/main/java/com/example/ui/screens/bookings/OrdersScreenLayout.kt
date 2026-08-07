@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings




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
fun OrdersScreenLayout(viewModel: MainViewModel, themeColors: VisualThemePalette) {
    val bookings by viewModel.bookings.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val urgentRequestsList by viewModel.bookings.collectAsState() // filtered for urgent requests or orders
    val context = LocalContext.current

    val myOrders = remember(bookings, currentUserPhone) {
        bookings.filter { 
            it.serviceType.contains("عاجل") || 
            it.customerPhone == currentUserPhone || 
            currentUserPhone.isEmpty() 
        }.sortedByDescending { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 شاشة طلباتي والخدمات العاجلة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "الإجمالي: ${myOrders.size}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (myOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد طلبات خدمات نشطة حالياً", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("يمكنك إرسال طلب خدمتك الآن عبر الأيقونة السريعة", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myOrders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "طلب #${order.id.takeLast(6)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = themeColors.primary
                                )
                                Surface(
                                    color = when (order.status) {
                                        "APPROVED" -> Color(0xFF10B981)
                                        "REJECTED" -> Color(0xFFEF4444)
                                        else -> Color(0xFFF59E0B)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (order.status) {
                                            "APPROVED" -> "مقبول"
                                            "REJECTED" -> "ملغي"
                                            else -> "قيد الانتظار ⚡"
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "نوع الخدمة: ${order.serviceType}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "ملاحظات الطلب: ${order.serviceDetails.ifBlank { "لا توجد" }}", fontSize = 12.sp, color = Color.Gray)
                            Text(text = "تاريخ الطلب: ${order.dateString} - ${order.timeString}", fontSize = 11.sp, color = Color.Gray)
                            
                            if (order.providerPhone.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "اسم الفني/المحل: ${order.providerName} (${order.providerPhone})", fontSize = 12.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
