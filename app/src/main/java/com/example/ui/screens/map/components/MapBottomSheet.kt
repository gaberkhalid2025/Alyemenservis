package com.example.ui.screens.map.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.ui.screens.map.utils.MapDistanceCalculator
import com.example.utils.VisualThemePalette

/**
 * 📋 MapBottomSheet
 * Modern Material 3 Bottom Sheet Card displaying full details of the clicked entity:
 * - Real-time ETA and exact distance calculation
 * - Direct Call, WhatsApp, Navigation, and Booking actions
 * - Distinct category badge colors (#00E5FF, #10B981, #F59E0B, #EC4899)
 */
@Composable
fun MapBottomSheet(
    entity: Any,
    userLat: Double,
    userLng: Double,
    onDismiss: () -> Unit,
    onRequestBooking: (ProviderEntity) -> Unit,
    onOpenDetails: (Any) -> Unit,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Extract standardized information
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var categoryLabel by remember { mutableStateOf("") }
    var badgeColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var rating by remember { mutableDoubleStateOf(5.0) }
    var phone by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(true) }
    var lat by remember { mutableDoubleStateOf(0.0) }
    var lng by remember { mutableDoubleStateOf(0.0) }

    when (entity) {
        is ProviderEntity -> {
            title = entity.name
            subtitle = entity.customCategoryName.ifEmpty { entity.specialization.ifEmpty { entity.profession } }
            categoryLabel = "👷 فني صيانة معتمد"
            badgeColor = Color(0xFF00E5FF)
            rating = entity.rating.toDouble()
            phone = entity.phone
            imageUrl = entity.profileImage.ifEmpty { entity.coverImage }
            isAvailable = entity.isAvailable
            lat = entity.latitude
            lng = entity.longitude
        }
        is StoreEntity -> {
            title = entity.name
            val isMedical = entity.sectionId.contains("medical") || entity.categoryId.contains("medical") || entity.name.contains("طبي") || entity.name.contains("صيدلية")
            val isRestaurant = !isMedical && (entity.sectionId.contains("restaurant") || entity.categoryId.contains("restaurant") || entity.name.contains("مطعم") || entity.name.contains("كافيه"))
            
            categoryLabel = if (isMedical) "🏥 مركز طبي / صيدلية" else if (isRestaurant) "🍔 مطعم / كافيه" else "🏪 متجر معتمد"
            badgeColor = if (isMedical) Color(0xFFEC4899) else if (isRestaurant) Color(0xFFF59E0B) else Color(0xFF10B981)
            subtitle = entity.description.take(50).ifEmpty { if (isMedical) "خدمات طبية وصيدلانية" else "محل تجاري معتمد" }
            rating = entity.rating.toDouble()
            phone = entity.phone
            imageUrl = entity.logoImage.ifEmpty { entity.coverImage }
            isAvailable = entity.isActive
            lat = entity.latitude
            lng = entity.longitude
        }
        is PropertyEntity -> {
            title = entity.title
            subtitle = "${entity.price} ${entity.currency} - ${entity.description.take(40)}"
            categoryLabel = "🏠 عقار معروض"
            badgeColor = Color(0xFF8B5CF6)
            rating = 5.0
            phone = entity.phone
            imageUrl = entity.images.firstOrNull() ?: ""
            isAvailable = entity.isActive
            lat = entity.latitude
            lng = entity.longitude
        }
    }

    val distanceMeters = MapDistanceCalculator.calculateDistanceMeters(userLat, userLng, lat, lng)
    val distanceText = MapDistanceCalculator.formatDistance(distanceMeters)
    val etaText = MapDistanceCalculator.computeEta(distanceMeters)

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row: Avatar, Info, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image / Icon Avatar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(2.dp, badgeColor.copy(alpha = 0.6f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (entity is ProviderEntity) "👷" else if (entity is StoreEntity) "🏪" else "🏠",
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                // Title & Category
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_bottom_sheet_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Meta Info: Distance + ETA + Rating + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance & ETA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Text(
                        text = "$distanceText ($etaText)",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Rating & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⭐ $rating",
                        color = Color(0xFFFFD700),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAvailable) "متاح 🟢" else "غير متاح 🔴",
                        color = if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: View Details
                Button(
                    onClick = { onOpenDetails(entity) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = badgeColor,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("view_details_action_btn")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("عرض التفاصيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Call Action
                if (phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(42.dp)
                            .testTag("call_action_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Direct Booking Action (Providers only)
                if (entity is ProviderEntity) {
                    Button(
                        onClick = { onRequestBooking(entity) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("book_provider_btn")
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حجز فوري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
