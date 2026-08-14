@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager

@Composable
fun AdminCardCustomizerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_CARD_CUSTOMIZER")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    var selectedCardSection by remember { mutableStateOf("SERVICES") }
    var cardShape by remember { mutableStateOf("ROUNDED") }
    var cardSize by remember { mutableStateOf("NORMAL") }
    var cardBgHex by remember { mutableStateOf("#1E293B") }
    var btnWhatsAppEnabled by remember { mutableStateOf(true) }
    var btnCallEnabled by remember { mutableStateOf(true) }
    var btnMapEnabled by remember { mutableStateOf(true) }
    var btnBookingEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🎛️ تخصيص أزرار وأشكال وألوان البطائق (فوري)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("التحكم الكامل بأشكال، أحجام، ألوان، وأزرار بطائق (الخدمات، العقارات، المتاجر، المطاعم، الطب، الوظائف):", fontSize = 11.sp, color = themeColors.textSecondary)

                Text("اختر القسم المستهدف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("SERVICES" to "🔧 الخدمات", "STORES" to "🏪 المتاجر", "RESTAURANTS" to "🍔 المطاعم", "MEDICAL" to "🏥 الطب", "PROPERTIES" to "🏠 العقارات", "JOBS" to "💼 الوظائف")) { (sec, label) ->
                        FilterChip(
                            selected = selectedCardSection == sec,
                            onClick = { selectedCardSection = sec },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("شكل وحواف البطاقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ROUNDED" to "زوايا دائرية 🔲", "PILL" to "كبسولة 💊", "SQUARE" to "مربع حاد ⬛").forEach { (shape, label) ->
                        FilterChip(
                            selected = cardShape == shape,
                            onClick = { cardShape = shape },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("حجم البطاقة وكثافة العناصر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("COMPACT" to "مضغوط ⚡", "NORMAL" to "عادي 📐", "LARGE" to "كبير بارز 🌟").forEach { (sz, label) ->
                        FilterChip(
                            selected = cardSize == sz,
                            onClick = { cardSize = sz },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("أزرار التفاعل الظاهرة بالبطاقة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnWhatsAppEnabled, onCheckedChange = { btnWhatsAppEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر مراسلة واتساب 🟢", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnCallEnabled, onCheckedChange = { btnCallEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الاتصال المباشر 📞", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnMapEnabled, onCheckedChange = { btnMapEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الخريطة والموقع 🗺️", color = Color.White, fontSize = 11.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = btnBookingEnabled, onCheckedChange = { btnBookingEnabled = it }); Spacer(modifier = Modifier.width(8.dp)); Text("زر الحجز أو الطلب الفوري 📅", color = Color.White, fontSize = 11.sp) }
                }

                Button(
                    onClick = {
                        val updated = settingsState.copy(
                            footerMessage = "card_custom_${selectedCardSection}_${cardShape}_${cardSize}"
                        )
                        viewModel.updateAdminSettings(updated)
                        Toast.makeText(context, "✨ تم حفظ ومزامنة إعدادات وشكل أزرار البطائق فورياً لكل الأجهزة!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("حفظ ومزامنة تخصيص البطائق فورياً ⚡", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
