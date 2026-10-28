package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.MapMode

/**
 * 🗺️ MapLayersDialog (نظام الطبقات الخمس)
 * 1. أقمار صناعية (عالي الدقة) - tiltAngle = 0f
 * 2. Vela 3D (مباني ثلاثية الأبعاد) - tiltAngle = 42f
 * 3. خريطة الشوارع الحية (HOT) - tiltAngle = 0f
 * 4. التضاريس والارتفاعات (Topo) - tiltAngle = 0f
 * 5. خريطة الشوارع القياسية (OSM) - tiltAngle = 0f
 */
@Composable
fun MapLayersDialog(
    currentMapMode: MapMode,
    onSelectLayer: (MapMode, Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24) // Golden #FBBF24
                )
                Text("🗺️ نظام الطبقات الخمس للخرائط", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val layers = listOf(
                    LayerInfo(MapMode.SATELLITE, "🛰️ أقمار صناعية (عالي الدقة)", "عرض الخريطة مسطح تماماً بدقة فضائية فائقة وتسميات عربية (Tilt: 0°)", 0f),
                    LayerInfo(MapMode.VELA_3D, "🏙️ Vela 3D (مباني ثلاثية الأبعاد)", "تفعيل المنظور ثلاثي الأبعاد وإمالة الكاميرا بزاوية 42 درجة مع مجسمات المباني (Tilt: 42°)", 42f),
                    LayerInfo(MapMode.HOT, "🗺️ خريطة الشوارع الحية (HOT)", "متجهات الشوارع الإنسانية المحدثة مع إمالة مسطحة (Tilt: 0°)", 0f),
                    LayerInfo(MapMode.TOPO, "⛰️ التضاريس والارتفاعات (Topo)", "خريطة OpenTopoMap الجغرافية بخطوط الكنتور وظلال الجبال (Tilt: 0°)", 0f),
                    LayerInfo(MapMode.OSM, "🌐 خريطة الشوارع القياسية (OSM)", "خريطة OpenStreetMap القياسية مع تسميات عربية (Tilt: 0°)", 0f)
                )

                layers.forEach { layer ->
                    val isSelected = currentMapMode == layer.mode
                    Surface(
                        onClick = {
                            onSelectLayer(layer.mode, layer.tilt)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = layer.title,
                                color = if (isSelected) Color(0xFF10B981) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = layer.desc,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)), // Sky blue #38BDF8
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إغلاق", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class LayerInfo(
    val mode: MapMode,
    val title: String,
    val desc: String,
    val tilt: Float
)
