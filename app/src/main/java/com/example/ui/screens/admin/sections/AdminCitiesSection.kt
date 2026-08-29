package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminCitiesSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val cities by viewModel.cities.collectAsState()
    val context = LocalContext.current
    var newCityName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🗺️ إضافة مدينة / محافظة جديدة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = newCityName,
                    onValueChange = { newCityName = it },
                    label = { Text("اسم المدينة / المحافظة", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    )
                )

                Button(
                    onClick = {
                        if (newCityName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المدينة", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addNewCity(newCityName, newCityName)
                            Toast.makeText(context, "تمت إضافة المدينة بنجاح", Toast.LENGTH_SHORT).show()
                            newCityName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة المدينة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "📍 المدن والمحافظات المتاحة (${cities.size})",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            items(cities, key = { it.id }) { city ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📍 ${city.nameAr}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        IconButton(onClick = {
                            viewModel.removeCity(city.id)
                            Toast.makeText(context, "تم حذف المدينة", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }
}
