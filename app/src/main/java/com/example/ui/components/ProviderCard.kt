package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.dialogs.InAppVoiceCallDialog
import com.example.ui.theme.VisualThemePalette

@Composable
fun DetailedProviderPlaceholderCard(themeColors: VisualThemePalette) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("provider_detail_placeholder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        border = BorderStroke(2.dp, themeColors.accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with VIP badge and Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(themeColors.accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "👑 نموذجي معتمد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "صيانة منزلية",
                        fontSize = 10.sp,
                        color = themeColors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Rating Star Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = themeColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "4.9 (نموذج)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Name & Call Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "امين الغرباني (صيانة عامة)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "صنعاء، منطقة الدائري جوار مدرسة اسماء للبنات",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                // Call Button
                IconButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:777703195"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier
                        .background(Color.Green.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                        .testTag("provider_placeholder_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال بالفني",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = themeColors.accent.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Service Description
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "وصف الخدمة النموذجية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مختص صيانة وتمديد كهربائي، صيانة المكيفات والأجهزة المنزلية بدقة وأمان تام. تتوفر لدينا أحدث أجهزة الفحص وبأسعار مناسبة ومعتمدة مع ضمان الخدمة.",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}
