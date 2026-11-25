package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// [نظام خرائط وملاحة عالمي - ملكنا] نظام التصميم الزجاجي بالأزرق الملكي والأيقونات ثلاثية الأبعاد
object MapUi {
    val Bg = Color(0xFF0B1220)
    val Surface = Color(0xEB0F172A)
    val SurfaceTop = Color(0xF0172542)
    val SurfaceSolid = Color(0xFF111B2E)
    val Border = Color.White.copy(alpha = 0.14f)
    val BorderGlow = Color(0xFF93C5FD).copy(alpha = 0.28f)
    val Accent = Color(0xFF00E5FF)
    val RoyalTop = Color(0xFF5B86FF)
    val RoyalMid = Color(0xFF3A6BFF)
    val RoyalDeep = Color(0xFF2851D8)
    val RoyalShadow = Color(0xFF1E3A8A)
    val RedTop = Color(0xFFF87171)
    val RedMid = Color(0xFFEF4444)
    val RedDeep = Color(0xFFB91C1C)
    val Ok = Color(0xFF10B981)
    val Warn = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)
    val RadiusL = 20.dp
    val RadiusM = 16.dp
    val RadiusS = 12.dp
    val Gap = 8.dp
    val Pad = 12.dp

    val RoyalGemBrush: Brush
        get() = Brush.verticalGradient(listOf(RoyalTop, RoyalMid, RoyalDeep))

    val RedGemBrush: Brush
        get() = Brush.verticalGradient(listOf(RedTop, RedMid, RedDeep))

    val GlassPanelBrush: Brush
        get() = Brush.verticalGradient(listOf(SurfaceTop, Surface))

    val GlassRimBrush: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color(0xFF93C5FD).copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.05f)
            )
        )
}

/** أرقام وإحداثيات تُرسم دائماً LTR داخل نص RTL (يمنع انكسار «54 م» وانعكاس الأرقام). */
@Composable
fun LtrText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(text, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = maxLines, softWrap = false, modifier = modifier)
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MapUi.RadiusM),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(10.dp, shape, clip = false, ambientColor = MapUi.RoyalShadow.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.65f))
            .clip(shape)
            .background(MapUi.GlassPanelBrush)
            .border(BorderStroke(1.dp, MapUi.GlassRimBrush), shape),
        content = content
    )
}

/** كرية زجاجية ثلاثية الأبعاد (Glass Orb) مع لمعة علوية وإطار مضيء. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color? = null,
    size: Dp = 48.dp
) {
    val fg = tint ?: if (active) MapUi.Accent else MapUi.TextPrimary
    val orbBrush = if (active) {
        Brush.verticalGradient(
            listOf(
                MapUi.RoyalTop.copy(alpha = 0.45f),
                MapUi.RoyalMid.copy(alpha = 0.28f),
                MapUi.SurfaceSolid
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF223354).copy(alpha = 0.94f),
                MapUi.Surface
            )
        )
    }
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (active) 0.48f else 0.34f),
            if (active) MapUi.Accent.copy(alpha = 0.36f) else Color(0xFF93C5FD).copy(alpha = 0.14f),
            MapUi.RoyalShadow.copy(alpha = 0.45f)
        )
    )
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape, clip = false, ambientColor = MapUi.RoyalShadow, spotColor = Color.Black)
            .clip(CircleShape)
            .background(orbBrush)
            .drawBehind {
                // لمعة زجاجية علوية (specular highlight)
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.26f), Color.Transparent)
                    ),
                    topLeft = Offset(this.size.width * 0.16f, this.size.height * 0.06f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.68f, this.size.height * 0.34f)
                )
            }
            .border(1.1.dp, rimBrush, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
    }
}

/** جوهرة زرقاء ملكية ثلاثية الأبعاد للأزرار الأساسية والملاحة. */
@Composable
fun RoyalBlueGemButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(14.dp, shape, clip = false, ambientColor = MapUi.RoyalMid, spotColor = MapUi.RoyalShadow)
            .clip(shape)
            .background(MapUi.RoyalGemBrush)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.38f), Color.Transparent)
                    ),
                    topLeft = Offset(this.size.width * 0.14f, this.size.height * 0.05f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.72f, this.size.height * 0.38f),
                    cornerRadius = CornerRadius(this.size.width * 0.35f, this.size.height * 0.25f)
                )
            }
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f))),
                shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** زر ثلاثي الأبعاد أحمر ياقوتي لإنهاء الملاحة والطوارئ. */
@Composable
fun RedGemButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(MapUi.RadiusS)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .shadow(10.dp, shape, clip = false, ambientColor = MapUi.RedDeep, spotColor = MapUi.RedDeep)
            .clip(shape)
            .background(MapUi.RedGemBrush)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent)),
                    topLeft = Offset(this.size.width * 0.08f, 2f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.84f, this.size.height * 0.42f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f))), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** كبسولة حالة صغيرة (لافتة اتصال، GPS ضعيف...). لون واحد وظيفي. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .shadow(6.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MapUi.SurfaceTop,
                        color.copy(alpha = 0.22f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.65f), color.copy(alpha = 0.28f))),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(14.dp)) }
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
