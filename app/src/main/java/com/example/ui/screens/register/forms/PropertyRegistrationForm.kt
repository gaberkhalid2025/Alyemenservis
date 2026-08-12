package com.example.ui.screens.register.forms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.*
import com.example.utils.VisualThemePalette
import com.example.ui.components.FlexibleCatalogUploader
import com.example.utils.*
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun PropertyRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var propTitle by remember { mutableStateOf("") }
    var propPhone by remember { mutableStateOf("") }
    var propPrice by remember { mutableStateOf("") }
    var propCity by remember { mutableStateOf("") }
    var propArea by remember { mutableStateOf("") }
    var propPassword by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🏢 استمارة إدراج عقار / أرض", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = propTitle,
                onValueChange = { propTitle = it },
                label = { Text("عنوان العقار") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = propPhone,
                onValueChange = { propPhone = it },
                label = { Text("رقم هاتف المسؤول") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = propPhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (propTitle.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال عنوان العقار ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newProperty = PropertyEntity(
                        id = UUID.randomUUID().toString(),
                        title = propTitle,
                        phone = cleanPhone,
                        cityId = propCity,
                        localNeighborhood = propArea,
                        price = propPrice.toDoubleOrNull() ?: 0.0,
                        type = "rent",
                        propertyType = "apartment",
                        password = propPassword,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveProperty(newProperty)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب العقار للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب إدراج العقار 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
