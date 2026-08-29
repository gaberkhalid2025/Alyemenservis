package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminManualAddSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "➕ الإضافة اليدوية المباشرة للفنيين والمنشآت",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل / اسم المنشأة", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف التواصل", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = serviceType,
                onValueChange = { serviceType = it },
                label = { Text("نوع الخدمة / القطاع", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("المدينة / المحافظة", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "يرجى تعبئة الاسم ورقم الهاتف على الأقل", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addNewProvider(name, phone, serviceType.ifEmpty { "general" }, city, 100.0, false)
                        Toast.makeText(context, "تمت الإضافة المباشرة والتفعيل بنجاح", Toast.LENGTH_SHORT).show()
                        name = ""
                        phone = ""
                        serviceType = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تفعيل وإضافة مباشرة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
