package com.example.ui.screens.map

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.ManeuverType

// [نظام خرائط وملاحة عالمي - ملكنا]
@Composable
fun NavTopCard(
    maneuver: ManeuverType,
    distanceMeters: Double,
    instruction: String,
    thenText: String?,
    statusText: String?,
    statusColor: Color = MapUi.Warn,
    speedKmh: Int = 0,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (num, unit) = splitDistance(distanceMeters)
    val haptic = LocalHapticFeedback.current
    var lastHapticKey by remember { mutableStateOf("") }
    LaunchedEffect(maneuver, instruction, distanceMeters <= 30.0) {
        val key = "${maneuver.name}_${instruction.take(18)}"
        if (distanceMeters in 1.0..30.0 && lastHapticKey != key) {
            lastHapticKey = key
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    GlassPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            statusText?.let { StatusPill(it, statusColor) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val maneuverShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(10.dp, maneuverShape, clip = false, ambientColor = MapUi.RoyalMid, spotColor = MapUi.RoyalShadow)
                        .clip(maneuverShape)
                        .background(MapUi.RoyalGemBrush)
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
                                topLeft = Offset(this.size.width * 0.10f, 3f),
                                size = androidx.compose.ui.geometry.Size(this.size.width * 0.80f, this.size.height * 0.38f),
                                cornerRadius = CornerRadius(18f, 18f)
                            )
                        }
                        .border(1.2.dp, Color.White.copy(alpha = 0.42f), maneuverShape),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = maneuver,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180))) togetherWith
                                fadeOut(tween(140))
                        },
                        label = "nav_maneuver_icon"
                    ) { m ->
                        Icon(
                            imageVector = maneuverVector(m),
                            contentDescription = instruction,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LtrText(num, MapUi.TextPrimary, 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text(unit, color = MapUi.TextMuted, fontSize = 15.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    AnimatedContent(
                        targetState = instruction,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "nav_instruction_text"
                    ) { text ->
                        Text(
                            text = text,
                            color = MapUi.TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(48.dp).testTag("nav_expand_steps_btn")
                ) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "تفاصيل الخطوات", tint = MapUi.TextSecondary)
                }
            }
            if (thenText != null && speedKmh <= 15) {
                HorizontalDivider(color = MapUi.Border)
                Text("ثم: $thenText", color = MapUi.TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LtrText(value, valueColor, 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MapUi.TextMuted, fontSize = 11.sp)
    }
}

@Composable
fun NavBottomPanel(
    arrivalClock: String,
    durationText: String,
    distanceText: String,
    destinationName: String,
    progress: Float,
    isApproximate: Boolean,
    onEnd: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = MapUi.RadiusL, topEnd = MapUi.RadiusL, bottomStart = MapUi.RadiusM, bottomEnd = MapUi.RadiusM)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric(arrivalClock, "الوصول", MapUi.Ok, Modifier.weight(1f))
                Metric(durationText, "المدة", MapUi.TextPrimary, Modifier.weight(1f))
                Metric(distanceText, "المسافة", MapUi.TextPrimary, Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MapUi.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(MapUi.RoyalMid, MapUi.Accent)))
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(destinationName, color = MapUi.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isApproximate) Text("مسار تقريبي", color = MapUi.Warn, fontSize = 11.sp)
                }
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.size(48.dp).testTag("nav_more_menu_btn")
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "المزيد", tint = MapUi.TextSecondary)
                }
                RedGemButton(
                    text = "إنهاء",
                    icon = Icons.Default.Close,
                    onClick = onEnd,
                    modifier = Modifier.testTag("nav_end_btn")
                )
            }
        }
    }
}
