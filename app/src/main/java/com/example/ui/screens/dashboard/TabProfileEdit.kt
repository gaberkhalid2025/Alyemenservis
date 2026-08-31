package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 📝 Modular Tab: Profile Editing (تعديل الملف الشخصي والبيانات العامة)
 */
@Composable
fun TabProfileEdit(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(account.name) }
    var description by remember { mutableStateOf(account.description) }
    var phone by remember { mutableStateOf(account.phone) }
    var ownerName by remember { mutableStateOf(account.ownerName) }
    var workingHours by remember { mutableStateOf(account.workingHours) }
    var neighborhood by remember { mutableStateOf(account.neighborhood) }
    var logoImage by remember { mutableStateOf(account.rawStore?.logoImage ?: "") }
    var coverImage by remember { mutableStateOf(account.rawStore?.coverImage ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            UnifiedImagePicker(
                label = "🖼️ شعار المنشأة / لوجو الحساب",
                imageUrl = logoImage,
                onImageSelected = { uri ->
                    logoImage = uri.toString()
                    if (account.rawStore != null) {
                        viewModel.saveStore(account.rawStore.copy(logoImage = uri.toString()))
                    }
                },
                themeColors = themeColors
            )
        }

        item {
            UnifiedImagePicker(
                label = "🌄 صورة غلاف المنشأة / الخلفية الرئيسية",
                imageUrl = coverImage,
                onImageSelected = { uri ->
                    coverImage = uri.toString()
                    if (account.rawStore != null) {
                        viewModel.saveStore(account.rawStore.copy(coverImage = uri.toString()))
                    }
                },
                themeColors = themeColors
            )
        }

        item {
            val fields = listOf<Triple<String, String, (String) -> Unit>>(
                Triple("الاسم التجاري للمنشأة", name) { name = it },
                Triple("اسم المدير / مالك الحساب", ownerName) { ownerName = it },
                Triple("رقم الهاتف والتواصل الفوري", phone) { phone = it },
                Triple("تفاصيل العنوان والحي المعتمد", neighborhood) { neighborhood = it },
                Triple("نبذة وتفاصيل وصفية كاملة", description) { description = it },
                Triple("ساعات العمل والدوام اليومي", workingHours) { workingHours = it }
            )

            UnifiedEditDetailsCard(
                title = "📝 تعديل وتحديث بيانات الملف الشخصي سحابياً",
                fields = fields,
                onSaveClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        if (account.rawStore != null) {
                            val updated = account.rawStore.copy(
                                name = name,
                                description = description,
                                phone = phone,
                                ownerName = ownerName,
                                workingHours = workingHours,
                                localNeighborhood = neighborhood
                            )
                            viewModel.saveStore(updated)
                        } else if (account.rawProvider != null) {
                            val updated = account.rawProvider.copy(
                                name = name,
                                phone = phone,
                                localNeighborhood = neighborhood,
                                profession = description
                            )
                            viewModel.updateProviderEntity(updated)
                        } else if (account.rawProperty != null) {
                            val updated = account.rawProperty.copy(
                                title = name,
                                description = description,
                                phone = phone,
                                ownerName = ownerName,
                                localNeighborhood = neighborhood
                            )
                            viewModel.saveProperty(updated)
                        }
                        Toast.makeText(context, "✅ تم حفظ وتحديث الملف الشخصي بنجاح!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⚠️ يرجى تعبئة الحقول الأساسية المطلوبة", Toast.LENGTH_SHORT).show()
                    }
                },
                themeColors = themeColors
            )
        }
    }
}
