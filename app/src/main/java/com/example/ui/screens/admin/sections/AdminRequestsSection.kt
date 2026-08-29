package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingProviderEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminRequestsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val context = LocalContext.current
    var rejectingProvider by remember { mutableStateOf<PendingProviderEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "⏳ طلبات الانضمام والتسجيل الجديدة (${pendingProviders.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (pendingProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد طلبات انضمام قيد الانتظار حالياً ✨",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                items(pendingProviders, key = { it.id }) { provider ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Surface(
                                    color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "معلق",
                                        color = Color(0xFFFFB300),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "📱 الهاتف: ${provider.phone}", fontSize = 12.5.sp, color = Color.LightGray)
                            Text(text = "🔧 المهنة: ${provider.profession.ifEmpty { provider.customCategoryName }}", fontSize = 12.5.sp, color = Color.LightGray)
                            Text(text = "📍 المنطقة: ${provider.area}", fontSize = 12.5.sp, color = Color.LightGray)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.approvePendingProvider(provider)
                                        Toast.makeText(context, "تم قبول واعتماد المزود بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قبول واعتماد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { rejectingProvider = provider },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF5350))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("رفض الطلب", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejectingProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { rejectingProvider = null },
            title = { Text("سبب رفض الطلب", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    label = { Text("اكتب سبب الرفض موضحاً للمزود", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEF5350)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectPendingProvider(provider, rejectionReason.ifEmpty { "تم رفض الطلب" })
                        Toast.makeText(context, "تم رفض الطلب وإبلاغ المزود", Toast.LENGTH_SHORT).show()
                        rejectingProvider = null
                        rejectionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("إرسال ورفض", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingProvider = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
