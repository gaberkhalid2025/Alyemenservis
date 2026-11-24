package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.NavRouteResult
import org.json.JSONArray
import org.json.JSONObject

data class RouteStreetItem(
    val index: Int,
    val name: String,
    val instruction: String,
    val distanceM: Double,
    val lat: Double,
    val lng: Double
)

object RouteStreetsHelper {

    fun extractRouteStreets(result: NavRouteResult?): List<RouteStreetItem> {
        if (result == null || result.coordinates.isEmpty()) return emptyList()
        val allSteps = result.getAllSteps()
        if (allSteps.isEmpty()) return emptyList()

        val items = mutableListOf<RouteStreetItem>()
        var streetCount = 0

        for (step in allSteps) {
            val rawName = step.streetName.trim()
            if (rawName.isBlank()) continue
            val coord = result.coordinates.getOrNull(step.polylineIndex)
                ?: result.coordinates.firstOrNull() ?: continue

            val lastItem = items.lastOrNull()
            if (lastItem != null && lastItem.name == rawName) {
                // تجميع المسافة المتتالية لنفس الشارع
                items[items.size - 1] = lastItem.copy(
                    distanceM = lastItem.distanceM + step.distanceMeters
                )
            } else {
                streetCount++
                items.add(
                    RouteStreetItem(
                        index = streetCount,
                        name = rawName,
                        instruction = step.instruction,
                        distanceM = step.distanceMeters,
                        lat = coord.first,
                        lng = coord.second
                    )
                )
            }
        }
        return items
    }

    fun routeStreetsToJson(streets: List<RouteStreetItem>): String {
        if (streets.isEmpty()) return ""
        val arr = JSONArray()
        streets.take(30).forEach { st ->
            arr.put(JSONObject().apply {
                put("name", st.name)
                put("lat", st.lat)
                put("lng", st.lng)
            })
        }
        return arr.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteStreetsBottomSheet(
    streets: List<RouteStreetItem>,
    currentStreetIndex: Int,
    onSelectStreet: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier.testTag("route_streets_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "شوارع خط السير (${streets.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "انقر على أي شارع لتفقده على الخريطة دون قطع الملاحة",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            if (streets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "جاري استخراج أسماء الشوارع من خط السير...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(streets) { idx, street ->
                        val isCurrent = idx == currentStreetIndex
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) Color(0xFF10B981).copy(alpha = 0.20f) else Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) Color(0xFF10B981) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectStreet(street.lat, street.lng, street.name)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrent) Color(0xFF10B981) else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isCurrent) Color(0xFF10B981) else Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${street.index}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = street.name,
                                        color = if (isCurrent) Color(0xFF10B981) else Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (street.instruction.isNotBlank()) {
                                        Text(
                                            text = street.instruction,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MapDistanceCalculator.formatDistance(street.distanceM),
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrent) {
                                        Text(
                                            text = "المسار الحالي 📍",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
