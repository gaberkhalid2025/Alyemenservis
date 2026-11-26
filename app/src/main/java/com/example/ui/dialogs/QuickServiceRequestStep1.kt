@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

@Composable
internal fun Step1CategorySection(
    selectedSection: String,
    onSectionSelect: (String) -> Unit,
    selectedSpecialty: String,
    onSpecialtySelect: (String) -> Unit,
    specialtiesList: List<String>,
    themeColors: VisualThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "1️⃣ اختر القسم الرئيسي المناسب لطلبك:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // 5 Big distinct visual cards covering ALL app sectors
        val categoriesData = listOf(
            Triple("SERVICES", "🔧 الخدمات والفنيين والصيانة", "صيانة منازل، سباكة، كهرباء، طاقة شمسية، تكييف وورش"),
            Triple("STORES", "🏬 المتاجر والسلع والمحلات", "أجهزة، سوبرماركت، مواد بناء، أثاث، ملابس، قطع غيار"),
            Triple("RESTAURANTS", "🍽️ المطاعم والكافيهات والوجبات", "وجبات سريعة، مشويات ومندي، أطباق شعبية وكافيهات"),
            Triple("MEDICAL", "🏥 المراكز الطبية والصيدليات", "أدوية وصيدليات، عيادات، تحاليل ورعاية منزلية"),
            Triple("PROPERTIES", "🏢 العقارات والشقق والأراضي", "شقق إيجار أو تمليك، فلل، أراضي، محلات ومكاتب، استراحات")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categoriesData.forEach { (id, title, subtitle) ->
                val isSelected = selectedSection == id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSectionSelect(id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D2E)
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color(0xFFEF4444) else Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title.take(2), fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.drop(2).trim(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFEF4444) else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 10.5.sp,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSectionSelect(id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFEF4444),
                                unselectedColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub-specialties pills
        Text(
            text = "👇 اختر التخصص الدقيق:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.accent
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            specialtiesList.forEach { specialty ->
                val isSelected = selectedSpecialty == specialty
                FilterChip(
                    selected = isSelected,
                    onClick = { onSpecialtySelect(specialty) },
                    label = {
                        Text(
                            text = specialty,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color(0xFFEF4444) else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
