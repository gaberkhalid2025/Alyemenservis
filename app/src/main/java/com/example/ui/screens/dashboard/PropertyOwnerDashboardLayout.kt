@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.dashboard

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
fun PropertyOwnerDashboardLayout(
    property: com.example.data.PropertyEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    ratings: List<com.example.data.RatingEntity>
) {
    val context = LocalContext.current
    var editTitle by remember(property) { mutableStateOf(property.title) }
    var editDesc by remember(property) { mutableStateOf(property.description) }
    var editPrice by remember(property) { mutableStateOf(property.price.toString()) }
    var editArea by remember(property) { mutableStateOf(property.localNeighborhood) }
    var editPhone by remember(property) { mutableStateOf(property.phone) }

    val propRatings = remember(ratings, property.id) {
        ratings.filter { it.targetId == property.id }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🏠", fontSize = 28.sp)
        }

        Text(
            text = "🎉 لوحة إدارة العقار الخاص بك: ${property.title}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        // Part 1: Edit Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📝 تعديل مواصفات وبيانات العقار:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("عنوان الإعلان الرئيسي للعقار") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = editDesc,
                    onValueChange = { editDesc = it },
                    label = { Text("المواصفات (عدد الغرف - الحمامات - الدور - الخدمات)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    label = { Text("سعر البيع أو الإيجار (ريال يمني)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = editArea,
                    onValueChange = { editArea = it },
                    label = { Text("المنطقة / المحافظة في اليمن") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("رقم الهاتف أو الواتساب للتواصل") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )

                Button(
                    onClick = {
                        val dPrice = editPrice.toDoubleOrNull() ?: 0.0
                        if (editTitle.trim().isEmpty() || editPhone.trim().isEmpty() || dPrice <= 0.0) {
                            viewModel.triggerNotification("⚠️ يرجى تعبئة الحقول والأسعار بطريقة صحيحة!")
                        } else {
                            viewModel.saveProperty(
                                property.copy(
                                    title = editTitle.trim(),
                                    description = editDesc.trim(),
                                    price = dPrice,
                                    localNeighborhood = editArea.trim(),
                                    phone = editPhone.trim()
                                )
                            )
                            android.widget.Toast.makeText(context, "✅ تم حفظ تغييرات عقارك بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حفظ التحديثات 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Part 2: Reviews and Replies
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ تقييمات العملاء والرد المباشر عليها:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                if (propRatings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📭 لا توجد تقييمات أو تعليقات من العملاء حالياً.", fontSize = 10.sp, color = Color.Gray)
                    }
                } else {
                    propRatings.forEach { r ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.userName.ifEmpty { "عميل مجهول" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("⭐".repeat(r.rating.toInt().coerceIn(1, 5)), fontSize = 10.sp, color = Color.Yellow)
                            }

                            Text(r.comment, fontSize = 11.sp, color = Color.LightGray)

                            if (r.reply.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .background(themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Text("💬 ردّك الحالي: ${r.reply}", fontSize = 10.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                var replyText by remember { mutableStateOf("") }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("اكتب ردك للعميل هنا...", fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    Button(
                                        onClick = {
                                            if (replyText.trim().isNotEmpty()) {
                                                viewModel.addRatingReply(r.id, replyText.trim())
                                                replyText = ""
                                                android.widget.Toast.makeText(context, "✅ تم إرسال ردّك بنجاح للعميل والنشره فوراً", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("رد ⚡", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        var showDeleteConfirm by remember { mutableStateOf(false) }

        Button(
            onClick = {
                viewModel.cancelOrResetJoinRequest(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🚪 تسجيل الخروج من لوحة التحكم", color = Color.White, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🗑️ حذف حساب العقار نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("⚠️ تأكيد الحذف النهائي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف حساب عقارك بالكامل؟ سيتم حذف الإعلان والمعلومات والتعليقات نهائياً من قاعدة البيانات ولا يمكن التراجع عن هذا الإجراء!", color = Color.LightGray, fontSize = 11.sp) },
                containerColor = themeColors.surface,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePropertyPermanently(property.id)
                            viewModel.cancelOrResetJoinRequest(context)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("نعم، احذف نهائياً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("إلغاء", color = Color.White, fontSize = 11.sp)
                    }
                }
            )
        }
    }
}