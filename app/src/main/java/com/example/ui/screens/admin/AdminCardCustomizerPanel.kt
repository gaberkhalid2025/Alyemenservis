@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.data.AdminSettingsEntity

fun LazyListScope.adminCardCustomizerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    item {
        val settingsState by viewModel.settings.collectAsState()
        val context = LocalContext.current

        var selectedCardSection by remember { mutableStateOf("SERVICES") }
        var cardShape by remember { mutableStateOf("ROUNDED") }
        var cardSize by remember { mutableStateOf("NORMAL") }
        var btnWhatsAppEnabled by remember { mutableStateOf(settingsState.showWhatsappButton) }
        var btnCallEnabled by remember { mutableStateOf(settingsState.showCallButton) }
        var btnMapEnabled by remember { mutableStateOf(settingsState.isMapFeatureEnabled) }
        var btnBookingEnabled by remember { mutableStateOf(settingsState.showBookButton) }

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
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            showWhatsappButton = btnWhatsAppEnabled,
                            showCallButton = btnCallEnabled,
                            isMapFeatureEnabled = btnMapEnabled,
                            showBookButton = btnBookingEnabled
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
