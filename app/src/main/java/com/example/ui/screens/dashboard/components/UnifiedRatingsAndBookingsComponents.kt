package com.example.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * ⭐ Unified Ratings Section Component
 */
@Composable
fun UnifiedRatingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⭐ التقييمات وآراء العملاء", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("متوسط التقييم العام: ", fontSize = 12.sp, color = Color.White)
                    Text("⭐ ${account.rating} / 5.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(${account.numReviews} تقييم)", fontSize = 11.sp, color = Color.LightGray)
                }
                Text("يتيح هذا التبويب استعراض تعليقات وتقييمات العملاء وإمكانية الرد المباشر عليها.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }
}

/**
 * 📅 Unified Bookings Section Component
 */
@Composable
fun UnifiedBookingsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📅 إدارة الحجوزات والمواعيد والطلبات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(
                Pair("ALL", "الكل"),
                Pair("PENDING", "قيد الانتظار ⏳"),
                Pair("APPROVED", "مقبولة ✅"),
                Pair("COMPLETED", "مكتملة 🎉"),
                Pair("REJECTED", "ملغاة ❌")
            )
            items(filters) { item ->
                val isSel = selectedFilter == item.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) themeColors.accent else Color.DarkGray)
                        .clickable { selectedFilter = item.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(item.second, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد طلبيات أو حجوزات مسجلة ضمن الفلتر المختار حالياً.", fontSize = 11.sp, color = Color.LightGray)
                Text("تتم المزامنة الفورية عند قيام أي عميل بالحجز أو طلب الخدمة من التطبيق.", fontSize = 10.sp, color = themeColors.textSecondary)
            }
        }
    }
}

/**
 * 📎 Unified Attachments Section Component
 */
@Composable
fun UnifiedAttachmentsSection(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📎 إدارة المرفقات والصور والملفات (PDF / Excel)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رفع صور واجهة الغلاف والمنيو أو ملفات PDF:", fontSize = 11.sp, color = Color.White)
                Button(
                    onClick = {
                        android.widget.Toast.makeText(context, "📎 جاري فتح منتقي الصور والملفات بالهاتف...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("رفع صور جديدة / ملف PDF 📤", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
