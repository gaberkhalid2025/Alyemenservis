package com.example.ui.screens.map

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

// [نظام خرائط وملاحة عالمي - ملكنا]
/** عمود الأزرار العائمة بنظام الكريّات الزجاجية والجوهرة الزرقاء الملكية. */
@Composable
fun MapFabCluster(
    isNavigating: Boolean,
    locateActive: Boolean,
    onNavigate: () -> Unit,
    onLayers: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassIconButton(
            icon = Icons.Default.Layers,
            description = "الطبقات",
            onClick = onLayers,
            modifier = Modifier.testTag("map_layers_orb_btn")
        )
        GlassIconButton(
            icon = Icons.Default.Tune,
            description = "أدوات",
            onClick = onMore,
            modifier = Modifier.testTag("map_tools_orb_btn")
        )
        GlassPanel(shape = RoundedCornerShape(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(48.dp).testTag("map_zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تكبير", tint = MapUi.TextPrimary)
                }
                HorizontalDivider(Modifier.width(28.dp), color = MapUi.BorderGlow)
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(48.dp).testTag("map_zoom_out_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "تصغير", tint = MapUi.TextPrimary)
                }
            }
        }
        GlassIconButton(
            icon = Icons.Default.MyLocation,
            description = "موقعي",
            onClick = onLocate,
            active = locateActive,
            modifier = Modifier.testTag("map_locate_orb_btn")
        )
        RoyalBlueGemButton(
            onClick = onNavigate,
            description = if (isNavigating) "متابعة الملاحة" else "ابدأ الملاحة",
            size = 56.dp,
            modifier = Modifier.testTag("map_navigate_gem_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** لافتة سياقية واحدة تحت البحث. */
@Composable
fun MapContextBanner(
    text: String?,
    color: Color,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        StatusPill(
            text = text.orEmpty(),
            color = color,
            icon = icon,
            modifier = Modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        )
    }
}
