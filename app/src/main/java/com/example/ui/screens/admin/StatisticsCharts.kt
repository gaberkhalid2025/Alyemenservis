package com.example.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
fun StatisticsCharts(
    themeColors: VisualThemePalette
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📈 تحليل نمو الطلبات والحجوزات شهرياً", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                val points = listOf(20f, 45f, 30f, 65f, 80f, 95f, 110f)
                val lineColor = themeColors.accent

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (points.size - 1)
                    val maxVal = 120f

                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f)
                    )

                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("يناير", "مارس", "مايو", "يوليو", "سبتمبر", "نوفمبر").forEach { month ->
                    Text(text = month, color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}
