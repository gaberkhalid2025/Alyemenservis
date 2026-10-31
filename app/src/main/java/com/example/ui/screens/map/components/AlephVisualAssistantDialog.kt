package com.example.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import java.util.Locale

private fun ratingText(r: Number) = if (r.toDouble() > 0) String.format(Locale.US, "%.1f ⭐", r.toDouble()) else "لا تقييمات بعد"

private fun distText(hasUser: Boolean, uLat: Double, uLng: Double, lat: Double, lng: Double): String =
    if (!hasUser || lat == 0.0 || lng == 0.0) "المسافة غير متاحة"
    else MapDistanceCalculator.formatDistance(MapDistanceCalculator.calculateDistanceMeters(uLat, uLng, lat, lng)) + " (مباشرة)"

private fun isMedicalStore(s: StoreEntity): Boolean {
    val t = "${s.name} ${s.categoryId} ${s.sectionId}"
    return t.contains("صيدلية") || t.contains("مستشفى") || t.contains("عيادة") || t.contains("medical", true) || t.contains("pharmacy", true)
}

private fun describeEntity(e: Any?, hasUser: Boolean, uLat: Double, uLng: Double): String? = when (e) {
    is ProviderEntity -> buildString {
        append("📍 ${e.name} — ${e.profession}\n")
        append("المنطقة: ${e.area.ifBlank { "غير محددة" }}\n")
        append("التقييم: ${ratingText(e.rating)}\n")
        append("الحالة: ${if (e.isAvailable) "متاح" else "غير متاح حالياً"}\n")
        append(distText(hasUser, uLat, uLng, e.latitude, e.longitude))
    }
    is StoreEntity -> buildString {
        append("📍 ${e.name}\n")
        if (e.description.isNotBlank()) append("${e.description}\n")
        append("الهاتف: ${e.phone.ifBlank { "غير مسجل" }}\n")
        append(distText(hasUser, uLat, uLng, e.latitude, e.longitude))
    }
    else -> null
}

/**
 * ℹ️ AlephVisualAssistantDialog: ملخص المنطقة
 * ملخص آلي صادق مبني على بيانات التطبيق الحقيقية وموقع المستخدم بدون ادعاءات ذكاء اصطناعي.
 */
@Composable
fun AlephVisualAssistantDialog(
    userLat: Double,
    userLng: Double,
    hasUserLocation: Boolean = true,
    selectedCity: String = "الكل",
    nearbyProviders: List<ProviderEntity> = emptyList(),
    nearbyStores: List<StoreEntity> = emptyList(),
    targetEntity: Any? = null,
    onDismiss: () -> Unit,
    onNavigateTo: ((Double, Double, String) -> Unit)? = null
) {
    var queryText by remember { mutableStateOf("") }
    var currentAnswer by remember {
        mutableStateOf(
            describeEntity(targetEntity, hasUserLocation, userLat, userLng)
                ?: buildString {
                    append("ملخص آلي من بيانات التطبيق في $selectedCity:\n")
                    append("• ${nearbyProviders.size} فني و${nearbyStores.size} متجر ضمن النتائج الحالية.\n")
                    if (!hasUserLocation) append("• موقعك الحالي غير محدد، فالمسافات غير متاحة.")
                }
        )
    }

    fun askQuestion(q: String) {
        queryText = q
        fun byDistance(lat: Double, lng: Double) =
            if (hasUserLocation && lat != 0.0 && lng != 0.0)
                MapDistanceCalculator.calculateDistanceMeters(userLat, userLng, lat, lng) else Double.MAX_VALUE

        currentAnswer = when {
            q.contains("أقرب الخدمات") -> {
                val p = nearbyProviders.sortedBy { byDistance(it.latitude, it.longitude) }.take(3)
                val s = nearbyStores.sortedBy { byDistance(it.latitude, it.longitude) }.take(3)
                if (p.isEmpty() && s.isEmpty()) "لا توجد خدمات ضمن النتائج الحالية."
                else buildString {
                    if (!hasUserLocation) append("⚠️ موقعك غير محدد؛ الترتيب غير مبني على القرب.\n")
                    if (p.isNotEmpty()) append("• فنيون: " + p.joinToString("، ") { "${it.name} (${distText(hasUserLocation, userLat, userLng, it.latitude, it.longitude)})" } + "\n")
                    if (s.isNotEmpty()) append("• متاجر: " + s.joinToString("، ") { "${it.name} (${distText(hasUserLocation, userLat, userLng, it.latitude, it.longitude)})" })
                }
            }
            q.contains("فنيون") || q.contains("صيانة") -> {
                val list = nearbyProviders.filter { it.isAvailable }.sortedBy { byDistance(it.latitude, it.longitude) }.take(4)
                if (list.isEmpty()) "لا يوجد فنيون متاحون حالياً ضمن النتائج."
                else "🛠️ ${list.size} فنيين متاحين:\n" + list.joinToString("\n") { "• ${it.name} (${it.profession}) — ${ratingText(it.rating)}" }
            }
            q.contains("صيدلية") || q.contains("متجر") -> {
                val list = nearbyStores.filter { isMedicalStore(it) }.sortedBy { byDistance(it.latitude, it.longitude) }.take(4)
                if (list.isEmpty()) "لا توجد صيدليات أو مرافق طبية مسجلة ضمن النتائج الحالية."
                else "🏪 الأقرب:\n" + list.joinToString("\n") { "• ${it.name} — هاتف: ${it.phone.ifBlank { "غير مسجل" }}" }
            }
            else -> {
                val noCoords = nearbyProviders.count { it.latitude == 0.0 || it.longitude == 0.0 } + nearbyStores.count { it.latitude == 0.0 || it.longitude == 0.0 }
                "ملخص: ${nearbyProviders.size} فني و${nearbyStores.size} متجر. " +
                    (if (noCoords > 0) "$noCoords منها بلا إحداثيات دقيقة (تظهر موقعاً تقريبياً على الخريطة)." else "كلها بإحداثيات مسجلة.")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ملخص المنطقة",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "ملخص آلي من بيانات التطبيق",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Answer Display Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentAnswer,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "استفسارات سريعة:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                // Preset Buttons
                val buttons = listOf(
                    "📍 أقرب الخدمات في المنطقة",
                    "🛠️ الفنيون المتاحون للصيانة",
                    "🏪 الصيدليات والمتاجر القريبة",
                    "📋 اكتمال بيانات الموقع"
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(buttons.size) { idx ->
                        val label = buttons[idx]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { askQuestion(label) }
                        ) {
                            Text(
                                text = label,
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
