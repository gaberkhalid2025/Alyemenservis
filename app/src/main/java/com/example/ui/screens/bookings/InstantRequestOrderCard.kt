@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.InstantRequestEntity
import com.example.ui.*
import com.example.utils.VisualThemePalette

@Composable
fun InstantRequestOrderCard(
    req: InstantRequestEntity,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Request code & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "كود الطلب: ${req.requestCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                val statusLabel = when (req.status) {
                    "WAITING_FOR_OFFERS" -> "⏳ بانتظار العروض"
                    "REVIEWING_OFFERS" -> "👀 جاري مراجعة العروض"
                    "IN_PROGRESS" -> "⚙️ جاري التنفيذ"
                    "COMPLETED" -> "✅ مكتمل"
                    "CANCELLED" -> "❌ ملغي"
                    else -> "⏳ بانتظار العروض"
                }
                val statusColor = when (req.status) {
                    "COMPLETED" -> Color(0xFF10B981)
                    "IN_PROGRESS" -> Color(0xFFF59E0B)
                    "CANCELLED" -> Color.Gray
                    else -> Color(0xFF00E5FF)
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = req.serviceTitle.ifBlank { "طلب خدمة عاجلة" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📍 ${req.userCity} ${if (req.userNeighborhood.isNotBlank()) "(${req.userNeighborhood})" else ""}", fontSize = 11.sp, color = Color.LightGray)
                Text("⏱️ ${req.urgencyTime}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
            }

            if (req.rawPin.isNotBlank()) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🔑 الرمز السري (PIN): ${req.rawPin}",
                        fontSize = 10.5.sp,
                        color = Color(0xFFFDE047),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (req.description.isNotBlank()) {
                Text(
                    text = req.description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.navigateToScreen(AppScreens.INSTANT_REQUESTS_VIEW) },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text("تصفح العروض والأسعار 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (req.status != "COMPLETED" && req.status != "CANCELLED") {
                    OutlinedButton(
                        onClick = {
                            viewModel.cancelInstantRequest(req.id, req.rawPin)
                            Toast.makeText(context, "🗑️ تم إلغاء الطلب بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("إلغاء ❌", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
