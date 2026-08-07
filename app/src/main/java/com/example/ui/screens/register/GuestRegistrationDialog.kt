@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register




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
fun GuestRegistrationDialog(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onDismiss: () -> Unit,
    onRegisterCompleted: (String, String, String, String) -> Unit
) {
    val currentName = viewModel.currentUserName.collectAsState().value
    val currentPhone = viewModel.currentUserPhone.collectAsState().value
    val currentResidence = viewModel.currentUserResidence.collectAsState().value
    val settingsState by viewModel.settings.collectAsState()

    var isRestoreMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(currentName) }
    var phonePrefix by remember { mutableStateOf("+967") }
    var phoneBody by remember { mutableStateOf(if (currentPhone.startsWith("+967")) currentPhone.removePrefix("+967") else currentPhone) }
    var residence by remember { mutableStateOf(currentResidence) }
    var password by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(2.dp, themeColors.accent, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRestoreMode) "🔓 استرجاع الحساب والبيانات" else "🔐 جدار الحماية - التحقق من الهوية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red)
                    }
                }

                if (isRestoreMode) {
                    Text(
                        text = "يرجى إدخال رقم هاتفك وكلمة المرور لاسترجاع حسابك وحجوزاتك ومحادثاتك السابقة بالكامل:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Text(phonePrefix, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = phoneBody,
                            onValueChange = { phoneBody = it },
                            placeholder = { Text("رقم الهاتف (مثلاً 777644)", fontSize = 11.sp, color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themeColors.accent,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", fontSize = 11.sp, color = themeColors.accent) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cleanPhone = phoneBody.trim()
                            val cleanPassword = password.trim()
                            val isValidPhone = (cleanPhone.length == 9 && (
                                cleanPhone.startsWith("77") || 
                                cleanPhone.startsWith("73") || 
                                cleanPhone.startsWith("71") || 
                                cleanPhone.startsWith("70") || 
                                cleanPhone.startsWith("78")
                            )) || (cleanPhone.length == 7 && !cleanPhone.startsWith("0"))

                            if (cleanPhone.isEmpty() || cleanPassword.isEmpty()) {
                                android.widget.Toast.makeText(context, "⚠️ الرجاء إدخال رقم الهاتف وكلمة المرور!", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (!isValidPhone) {
                                android.widget.Toast.makeText(context, "⚠️ رقم الهاتف غير صالح!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val fullPhone = if (cleanPhone.length == 9) cleanPhone else "77$cleanPhone"
                                viewModel.restoreGuestUser(context, fullPhone, cleanPassword) { success, msg ->
                                    if (success) {
                                        onDismiss()
                                    } else {
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("استرجاع الحساب الآن 🔓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = { isRestoreMode = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إنشاء حساب جديد؟ اضغط هنا للتسجيل", color = themeColors.accent, fontSize = 11.sp)
                    }

                } else {
                    Text(
                        text = "لتفادي الحسابات والاتصالات والمحادثات الوهمية وتقليل استهلاك الموارد تماشياً مع سياسة الخصوصية بالبوابة، يرجى ملء هوية مستخدم يمني حقيقي مفعّل بالجمهورية:",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الثلاثي بالكامل (إجباري) *", fontSize = 11.sp, color = themeColors.accent) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Text(phonePrefix, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = phoneBody,
                            onValueChange = { phoneBody = it },
                            placeholder = { Text("رقم الهاتف (إجباري) *", fontSize = 11.sp, color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themeColors.accent,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = residence,
                        onValueChange = { residence = it },
                        label = { Text("السكن داخل اليمن (إجباري) *", fontSize = 11.sp, color = themeColors.accent) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val isPasswordRequired = settingsState.isUserPasswordRequired
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("إنشاء كلمة مرور للحساب" + (if (isPasswordRequired) " (إجباري) *" else " (اختياري)"), fontSize = 11.sp, color = themeColors.accent) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val cleanPhone = phoneBody.trim()
                            val cleanResidence = residence.trim()
                            val cleanPassword = password.trim()

                            val isValidPhone = (cleanPhone.length == 9 && (
                                cleanPhone.startsWith("77") || 
                                cleanPhone.startsWith("73") || 
                                cleanPhone.startsWith("71") || 
                                cleanPhone.startsWith("70") || 
                                cleanPhone.startsWith("78")
                            )) || (cleanPhone.length == 7 && !cleanPhone.startsWith("0"))

                            if (cleanName.isEmpty() || cleanPhone.isEmpty() || cleanResidence.isEmpty()) {
                                android.widget.Toast.makeText(context, "⚠️ جميع الحقول ذات النجمة إجبارية!", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (isPasswordRequired && cleanPassword.isEmpty()) {
                                android.widget.Toast.makeText(context, "⚠️ كلمة المرور إجبارية بقرار الإدارة!", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (!isValidPhone) {
                                android.widget.Toast.makeText(context, "⚠️ رقم الهاتف غير صالح!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val fullPhone = if (cleanPhone.length == 9) cleanPhone else "77$cleanPhone"
                                onRegisterCompleted(cleanName, fullPhone, cleanResidence, cleanPassword)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إتمام التحقق العادل والانطلاق 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = { isRestoreMode = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("لديك حساب بالفعل؟ استرجاع الحساب الآن 🔓", color = themeColors.accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
