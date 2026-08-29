package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

data class ThemePreset(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val description: String
)

@Composable
fun AdminThemeColorsSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()

    val presets = remember {
        listOf(
            ThemePreset("THEME_DEFAULT_GOLD", "الذهبي الملكي (افتراضي)", Color(0xFF1E293B), Color(0xFFFFD700), Color(0xFF0F172A), "طابع فاخر راقي"),
            ThemePreset("THEME_CYAN_OCEAN", "المحيط السماوي (Cyan)", Color(0xFF0F2027), Color(0xFF00E5FF), Color(0xFF203A43), "طابع حيوي وتقني"),
            ThemePreset("THEME_EMERALD_GREEN", "الزمرد الأخضر (Emerald)", Color(0xFF064E3B), Color(0xFF10B981), Color(0xFF022C22), "طابع يوحي بالنمو والبركة"),
            ThemePreset("THEME_ROYAL_PURPLE", "البنفسجي الملكي (Royal)", Color(0xFF3B0764), Color(0xFFA855F7), Color(0xFF1E1B4B), "طابع الحداثة والابتكار"),
            ThemePreset("THEME_SUNSET_ORANGE", "غروب الشمس (Orange/Amber)", Color(0xFF451A03), Color(0xFFF59E0B), Color(0xFF1C1917), "طابع دافئ وجذاب"),
            ThemePreset("THEME_RUBY_RED", "الياقوت الأحمر (Ruby)", Color(0xFF4C0519), Color(0xFFF43F5E), Color(0xFF1F1F1F), "طابع قوي وملفت")
        )
    }

    var selectedThemeId by remember(settingsState.activeThemeId) {
        mutableStateOf(settingsState.activeThemeId.ifEmpty { "THEME_DEFAULT_GOLD" })
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التحكم الشامل بالألوان والمظهر والثيمات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "اختر السمة والباليت اللونية للتطبيق. يتم تطبيق التغييرات فورياً وحفظها ومزامنتها في قاعدة البيانات السحابية عبر جميع أجهزة المستخدمين.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        Text(
            text = "القوالب اللونية المتاحة (Presets)",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                val isSelected = selectedThemeId == preset.id

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF334155) else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) preset.accentColor else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedThemeId = preset.id
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = preset.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = preset.accentColor, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(text = preset.description, fontSize = 11.sp, color = Color.Gray)

                        // Color preview dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.primaryColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.accentColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(preset.surfaceColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val st = settingsState
                viewModel.updateBackdoorSettings(
                    st.appName, st.welcomeMessage, st.footerMessage, selectedThemeId,
                    st.supportPhone, st.supportEmail, st.supportWhatsapp,
                    st.isMaintenanceActive, st.hidePromoFooter, st.assistantHidden, st.assistantSize,
                    st.chatHidden, st.chatSize, st.maxSearchRadiusKm, st.isSpeechSearchEnabled,
                    false, 90
                )
                Toast.makeText(context, "✅ تم تطبيق الثيم وتحديث مظهر التطبيق لجميع المستخدمين فورياً!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("حفظ وتطبيق الثيم فورياً لجميع الأجهزة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
