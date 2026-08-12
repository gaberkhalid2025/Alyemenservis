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
fun MedicalRegistrationForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    settingsState: AdminSettingsEntity
) {
    val context = LocalContext.current
    var medName by remember { mutableStateOf("") }
    var medPhone by remember { mutableStateOf("") }
    var medDesc by remember { mutableStateOf("") }
    var medCity by remember { mutableStateOf("") }
    var medAddress by remember { mutableStateOf("") }
    var medPassword by remember { mutableStateOf("") }

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
            Text("🏥 استمارة تسجيل مركز طبي / عيادة", color = themeColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            OutlinedTextField(
                value = medName,
                onValueChange = { medName = it },
                label = { Text("اسم المركز الطبي / العيادة") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            OutlinedTextField(
                value = medPhone,
                onValueChange = { medPhone = it },
                label = { Text("رقم الهاتف") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(
                onClick = {
                    val raw = medPhone.trim().replace(" ", "")
                    val cleanPhone = when {
                        raw.startsWith("+967") -> raw.substring(4)
                        raw.startsWith("00967") -> raw.substring(5)
                        raw.startsWith("0") -> raw.substring(1)
                        else -> raw
                    }
                    if (medName.isBlank() || cleanPhone.length < 9) {
                        Toast.makeText(context, "الرجاء إدخال اسم المركز ورقم الهاتف", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val newMed = StoreEntity(
                        id = UUID.randomUUID().toString(),
                        sectionId = "medical",
                        name = medName,
                        phone = cleanPhone,
                        localNeighborhood = medAddress,
                        cityId = medCity,
                        description = medDesc,
                        password = medPassword,
                        isApproved = false,
                        isActive = false
                    )
                    viewModel.saveStore(newMed)
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                    Toast.makeText(context, "تم إرسال طلب المركز الطبي للمراجعة بنجاح!", Toast.LENGTH_LONG).show()
                    viewModel.navigateTo("JOIN_REQUEST_STATUS")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إرسال طلب تسجيل المركز الطبي 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
