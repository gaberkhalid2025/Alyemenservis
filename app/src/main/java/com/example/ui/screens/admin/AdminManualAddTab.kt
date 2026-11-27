package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JobEntity
import com.example.data.PropertyEntity
import com.example.data.StoreEntity
import com.example.ui.*
import com.example.utils.FirebaseStorageUploader
import com.example.utils.VisualThemePalette
import java.util.UUID
import kotlinx.coroutines.launch

internal fun LazyListScope.adminManualAddTab(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = state.context
    val categories = state.categories
    val citiesList = state.citiesList

    item {
        Text("✨ الإضافة المباشرة الفورية من قِبل إدارة المنصة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
        Text("اختر القسم المطلوب لإضافة وتفعيل البيانات مباشرة على الدليل دون انتظار:", fontSize = 11.sp, color = themeColors.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        val addSubTabs = listOf(
            Pair("SERVICES", "إضافة فني / مهني 🔧"),
            Pair("PROPERTIES", "إضافة عقار 🏠"),
            Pair("STORES", "مركز تجاري 🏪24"),
            Pair("MEDICAL", "مركز طبي / عيادة 🏥"),
            Pair("RESTAURANTS", "مطعم / كافيه 🍔"),
            Pair("JOBS", "إعلان وظيفي 💼")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(addSubTabs.size) { index ->
                val st = addSubTabs[index]
                val isSel = state.adminAddSubTab == st.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { state.adminAddSubTab = st.first }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .border(
                            if (isSel) 2.dp else 1.dp,
                            if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(30.dp)
                        )
                ) {
                    Text(st.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (state.adminAddSubTab == "SERVICES") {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.manualName,
                        onValueChange = { state.manualName = it },
                        label = { Text("الاسم الكامل للحرفي/المهندِس") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = state.manualPhone,
                        onValueChange = { state.manualPhone = it },
                        label = { Text("رقم الهاتف (مثال: 777644)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = state.manualStreet,
                        onValueChange = { state.manualStreet = it },
                        label = { Text("الشارع أو الحي التفصيلي") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Text("اختر قسم الصيانة المستهدف:", fontSize = 11.sp, color = themeColors.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            val isSel = state.manualCategoryId == cat.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { state.manualCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Text(cat.name, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    Text("اختر المدينة اليمنية المحتوية للحي:", fontSize = 11.sp, color = themeColors.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(citiesList, key = { it.id }) { city ->
                            val isSel = state.manualCityId == city.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) themeColors.accent else themeColors.surface)
                                    .clickable { state.manualCityId = city.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Text(city.nameAr, fontSize = 10.sp, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    val galleryScope = rememberCoroutineScope()
                    val galleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { selectedUri ->
                            galleryScope.launch {
                                val path = FirebaseStorageUploader.getProviderProfilePath(UUID.randomUUID().toString())
                                val uploadedUrl = FirebaseStorageUploader.uploadImageUri(context, selectedUri, path)
                                    .getOrElse { selectedUri.toString() }
                                if (uploadedUrl.isNotEmpty()) {
                                    state.manualPhotoUrl = uploadedUrl
                                    Toast.makeText(context, "✅ تم اختيار الصورة ورفعها بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.manualPhotoUrl,
                        onValueChange = { state.manualPhotoUrl = it },
                        label = { Text("رابط صورة الفني الشخصية أو كود Base64") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📸 اختيار صورة من استوديو الهاتف", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.manualIsVipGolden,
                            onCheckedChange = { state.manualIsVipGolden = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                        )
                        Text("تفعيل كرت VIP الذهبي المميز على الدليل فورا", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (state.manualName.trim().isEmpty() || state.manualPhone.trim().isEmpty()) {
                                Toast.makeText(context, "الرجاء تعبئة الاسم ورقم الهاتف على الأقل للتفعيل", Toast.LENGTH_SHORT).show()
                            } else {
                                val finalCat = if (state.manualCategoryId.isEmpty()) (categories.firstOrNull()?.id ?: "1") else state.manualCategoryId
                                val finalCity = if (state.manualCityId.isEmpty()) (citiesList.firstOrNull()?.id ?: "ye_san") else state.manualCityId
                                val priceVal = state.manualPriceValue.toDoubleOrNull() ?: 1500.0
                                val finalStreet = if (state.manualStreet.trim().isEmpty()) "غير محدد" else state.manualStreet.trim()

                                viewModel.addNewProviderCustom(
                                    name = state.manualName.trim(),
                                    phone = state.manualPhone.trim(),
                                    catId = finalCat,
                                    street = finalStreet,
                                    cityId = finalCity,
                                    profileImage = state.manualPhotoUrl.trim(),
                                    idCardImage = state.manualIdCardUrl.trim(),
                                    forensicImage = state.manualForensicUrl.trim(),
                                    price = priceVal,
                                    isVip = state.manualIsVipGolden
                                )

                                state.manualName = ""
                                state.manualPhone = ""
                                state.manualStreet = ""
                                state.manualPhotoUrl = ""
                                Toast.makeText(context, "🚀 تم إضافة وتفعيل مقدم الخدمة بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ إضافة الفني وتفعيله بالكامل فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (state.adminAddSubTab == "PROPERTIES") {
        item {
            var pTitle by remember { mutableStateOf("") }
            var pType by remember { mutableStateOf("شقة") }
            var pPrice by remember { mutableStateOf("") }
            var pPhone by remember { mutableStateOf("") }
            var pCity by remember { mutableStateOf("صنعاء") }
            var pDistrict by remember { mutableStateOf("") }
            var pDesc by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏠 إضافة عقار جديد معتمد فوراً", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(value = pTitle, onValueChange = { pTitle = it }, label = { Text("عنوان العقار (مثال: شقة مفروشة فاخرة للبيع)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pType, onValueChange = { pType = it }, label = { Text("نوع العقار (شقة / أرض / بيت / محل)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("السعر أو الإيجار (مثال: 50,000 ريال)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pPhone, onValueChange = { pPhone = it }, label = { Text("رقم هاتف المالك/المكتب") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pCity, onValueChange = { pCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pDistrict, onValueChange = { pDistrict = it }, label = { Text("المنطقة / الحي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("تفاصيل ومواصفات العقار") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                    Button(
                        onClick = {
                            if (pTitle.trim().isEmpty() || pPhone.trim().isEmpty()) {
                                Toast.makeText(context, "يرجى كتابة عنوان العقار ورقم الهاتف", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveProperty(
                                    PropertyEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = pTitle.trim(),
                                        propertyType = pType.trim(),
                                        price = pPrice.trim().toDoubleOrNull() ?: 0.0,
                                        phone = pPhone.trim(),
                                        cityId = pCity.trim(),
                                        localNeighborhood = pDistrict.trim(),
                                        description = pDesc.trim(),
                                        isApproved = true,
                                        isActive = true,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                pTitle = ""; pPrice = ""; pPhone = ""; pDesc = ""
                                Toast.makeText(context, "✅ تم إضافة ونشر العقار بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ نشر ونشر العقار فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (state.adminAddSubTab == "STORES" || state.adminAddSubTab == "MEDICAL" || state.adminAddSubTab == "RESTAURANTS") {
        item {
            var sName by remember { mutableStateOf("") }
            var sPhone by remember { mutableStateOf("") }
            var sCity by remember { mutableStateOf("صنعاء") }
            var sAddress by remember { mutableStateOf("") }
            var sDesc by remember { mutableStateOf("") }

            val categoryTitle = when (state.adminAddSubTab) {
                "MEDICAL" -> "🏥 إضافة مركز طبي أو عيادة تخصصية"
                "RESTAURANTS" -> "🍔 إضافة مطعم أو كافيه مميز"
                else -> "🏪 إضافة مركز تجاري أو محل خدمي"
            }

            val defaultCategory = when (state.adminAddSubTab) {
                "MEDICAL" -> "مراكز طبية وعيادات"
                "RESTAURANTS" -> "مطاعم وكافيهات"
                else -> "مراكز تجارية ومحلات"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, themeColors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(categoryTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم المنشأة/المحل/المركز") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text("رقم الهاتف أو الواتساب") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sCity, onValueChange = { sCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sAddress, onValueChange = { sAddress = it }, label = { Text("العنوان والشارع التفصيلي") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sDesc, onValueChange = { sDesc = it }, label = { Text("وصف الخدمات والتخصصات وقائمة العمل") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                    Button(
                        onClick = {
                            if (sName.trim().isEmpty() || sPhone.trim().isEmpty()) {
                                Toast.makeText(context, "يرجى إدخال الاسم ورقم الهاتف", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveStore(
                                    StoreEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = sName.trim(),
                                        phone = sPhone.trim(),
                                        cityId = sCity.trim(),
                                        localNeighborhood = sAddress.trim(),
                                        description = sDesc.trim(),
                                        categoryId = defaultCategory,
                                        isApproved = true,
                                        isActive = true,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                sName = ""; sPhone = ""; sAddress = ""; sDesc = ""
                                Toast.makeText(context, "✅ تم إضافة وتفعيل $defaultCategory بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ إضافة وتفعيل المنشأة فوراً", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (state.adminAddSubTab == "JOBS") {
        item {
            var jTitle by remember { mutableStateOf("") }
            var jCompany by remember { mutableStateOf("") }
            var jPhone by remember { mutableStateOf("") }
            var jSalary by remember { mutableStateOf("") }
            var jCity by remember { mutableStateOf("صنعاء") }
            var jDesc by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💼 إضافة إعلان وظيفي جديد معتمد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(value = jTitle, onValueChange = { jTitle = it }, label = { Text("المسمى الوظيفي (مثال: محاسب مالي خبرة)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jCompany, onValueChange = { jCompany = it }, label = { Text("اسم الشركة أو جهة العمل") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jPhone, onValueChange = { jPhone = it }, label = { Text("رقم التواصل للتقديم") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jSalary, onValueChange = { jSalary = it }, label = { Text("الراتب المتوقع أو يحدد بعد المقابلة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jCity, onValueChange = { jCity = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = jDesc, onValueChange = { jDesc = it }, label = { Text("الشروط والمتطلبات وساعات العمل") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                    Button(
                        onClick = {
                            if (jTitle.trim().isEmpty() || jPhone.trim().isEmpty()) {
                                Toast.makeText(context, "يرجى كتابة المسمى الوظيفي ورقم التواصل", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveJob(
                                    JobEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = jTitle.trim(),
                                        companyName = jCompany.trim(),
                                        phone = jPhone.trim(),
                                        salary = jSalary.trim(),
                                        cityId = jCity.trim(),
                                        description = jDesc.trim(),
                                        isApproved = true,
                                        isActive = true,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                jTitle = ""; jCompany = ""; jPhone = ""; jDesc = ""
                                Toast.makeText(context, "✅ تم إعلان ونشر الوظيفة بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ نشر ونشر الإعلان الوظيفي فوراً", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
