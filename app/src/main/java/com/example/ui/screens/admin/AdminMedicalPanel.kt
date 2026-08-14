package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import com.example.util.PermissionGuard
import com.example.util.RoleManager
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminMedicalPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    state: AdminPanelState
) {
    if (!PermissionGuard.hasPermission(RoleManager.fromRoleString(viewModel.adminRole.value), "MANAGE_MEDICAL")) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("🔒 ليس لديك صلاحية للوصول إلى هذه اللوحة", color = Color.White, fontSize = 14.sp)
                Text("يرجى التواصل مع المالك أو المدير الرئيسي", color = Color.Gray, fontSize = 12.sp)
            }
        }
        return
    }

    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🏥 إدارة المراكز الطبية والمستشفيات والعيادات التخصصية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            
            val storesList by viewModel.stores.collectAsState()
            val filteredMedical = storesList.filter { it.sectionId == "medical" || it.sectionId == "clinic" || it.sectionId == "hospital" }

            var selectedFilterType by remember { mutableStateOf("الكل") }
            var medicalToManageDoctors by remember { mutableStateOf<StoreEntity?>(null) }

            OutlinedTextField(
                value = medicalSearchQueryState.value,
                onValueChange = { medicalSearchQueryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم المركز أو التخصص الطبي...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            val filterTypes = listOf("الكل", "VIP مميز", "موثق 🛡️", "موصى به ⭐", "محظور 🚫")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filterTypes) { filter ->
                    val isSel = selectedFilterType == filter
                    Button(
                        onClick = { selectedFilterType = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) themeColors.accent else themeColors.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(filter, fontSize = 11.sp, color = if (isSel) Color.Black else Color.White)
                    }
                }
            }

            val finalFiltered = filteredMedical.filter { med ->
                val matchesSearch = med.name.contains(medicalSearchQueryState.value, ignoreCase = true) || med.categoryId.contains(medicalSearchQueryState.value)
                val matchesFilter = when (selectedFilterType) {
                    "الكل" -> true
                    "VIP مميز" -> med.isVip
                    "موثق 🛡️" -> med.isVerified
                    "موصى به ⭐" -> med.isRecommended
                    "محظور 🚫" -> med.isBlocked
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            if (finalFiltered.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد مراكز طبية أو عيادات حالياً 🏥", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                finalFiltered.forEach { med ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(med.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("🏥 التخصص: ${med.categoryId}", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (med.isVip) Badge(containerColor = Color.Yellow) { Text("VIP", color = Color.Black, fontSize = 9.sp) }
                                    if (med.isVerified) Badge(containerColor = Color.Green) { Text("موثق", color = Color.Black, fontSize = 9.sp) }
                                }
                            }

                            Text("📞 الطوارئ والاستعلامات: ${med.phone}", color = Color.LightGray, fontSize = 11.sp)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { medicalToManageDoctors = med },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👨‍⚕️ إدارة الأطباء", fontSize = 10.sp, color = Color.White)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteStore(med.id) },
                                    modifier = Modifier.size(36.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            medicalToManageDoctors?.let { med ->
                var docName by remember { mutableStateOf("") }
                var docSpec by remember { mutableStateOf("") }
                var docFee by remember { mutableStateOf("") }

                val doctorsList = remember(med.productAttachmentsJson) {
                    val list = mutableListOf<Triple<String, String, String>>()
                    try {
                        if (med.productAttachmentsJson.isNotBlank()) {
                            val arr = org.json.JSONArray(med.productAttachmentsJson)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                list.add(Triple(obj.optString("name", ""), obj.optString("spec", ""), obj.optString("fee", "")))
                            }
                        }
                    } catch(e: Exception) {}
                    list
                }

                AlertDialog(
                    onDismissRequest = { medicalToManageDoctors = null },
                    title = { Text("👨‍⚕️ الكادر الطبي بـ: ${med.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(value = docName, onValueChange = { docName = it }, placeholder = { Text("اسم الطبيب", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = docSpec, onValueChange = { docSpec = it }, placeholder = { Text("التخصص", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = docFee, onValueChange = { docFee = it }, placeholder = { Text("سعر الكشفية", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())

                            Button(
                                onClick = {
                                    if (docName.isNotBlank()) {
                                        val updatedList = doctorsList + Triple(docName, docSpec, docFee)
                                        val arr = org.json.JSONArray()
                                        updatedList.forEach { (n, s, f) ->
                                            val obj = org.json.JSONObject()
                                            obj.put("name", n)
                                            obj.put("spec", s)
                                            obj.put("fee", f)
                                            arr.put(obj)
                                        }
                                        val updatedMed = med.copy(productAttachmentsJson = arr.toString())
                                        try {
                                            FirebaseFirestore.getInstance().collection("stores").document(med.id).set(updatedMed)
                                            viewModel._stores.value = viewModel._stores.value.map { if (it.id == med.id) updatedMed else it }
                                            viewModel.triggerNotification("👨‍⚕️ تم إضافة الطبيب بنجاح!")
                                            medicalToManageDoctors = updatedMed
                                        } catch(e: Exception) {}
                                        docName = ""
                                        docSpec = ""
                                        docFee = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إضافة الطبيب ➕", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { medicalToManageDoctors = null }) {
                            Text("إغلاق", color = Color.White)
                        }
                    },
                    containerColor = themeColors.surface
                )
            }
        }
    }
}
