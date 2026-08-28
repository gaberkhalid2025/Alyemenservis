@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.register.forms.RegistrationFormFactory
import com.example.ui.screens.register.forms.RegistrationFormViewModel
import com.example.utils.VisualThemePalette

/**
 * 📝 ProviderRegisterFormLayout - شاشة نماذج التسجيل والانضمام الموحدة
 * تتبع معمارية Factory Pattern و MVVM النظيفة (<200 سطر)
 *
 * @param viewModel نموذج العرض الرئيسي للتطبيق
 * @param themeColors لوحة ألوان الثيم المختارة
 * @param regType نوع التسجيل المطلوب (client, provider, store, ...)
 * @param sectionId القسم الفرعي المختار اختيارياً
 * @param onRegTypeChange استدعاء عند تغيير التبويب
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRegisterFormLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    regType: String,
    sectionId: String = "",
    onRegTypeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val formViewModel: RegistrationFormViewModel = remember(viewModel) {
        RegistrationFormViewModel(viewModel)
    }

    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserResidence by viewModel.currentUserResidence.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var activeType by remember(regType) { mutableStateOf(RegistrationType.fromId(regType)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "نموذج تسجيل: ${activeType.title}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "دليل خدمات اليمن - استكمال البيانات المعيارية",
                            fontSize = 10.5.sp,
                            color = themeColors.accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelOrResetJoinRequest(context) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category selector tabs
            ScrollableTabRow(
                selectedTabIndex = RegistrationType.values().indexOf(activeType),
                containerColor = Color(0xFF1E293B),
                contentColor = themeColors.accent,
                edgePadding = 8.dp
            ) {
                RegistrationType.values().forEach { item ->
                    Tab(
                        selected = activeType == item,
                        onClick = {
                            activeType = item
                            onRegTypeChange(item.id)
                        },
                        text = {
                            Text(
                                text = "${item.icon} ${item.title}",
                                fontSize = 11.5.sp,
                                fontWeight = if (activeType == item) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeType == item) themeColors.accent else Color.Gray
                            )
                        }
                    )
                }
            }

            // Dynamic Form from Factory
            RegistrationFormFactory.CreateForm(
                type = activeType,
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                categories = categories,
                snackbarHostState = snackbarHostState,
                initialName = currentUserName,
                initialPhone = currentUserPhone,
                initialResidence = currentUserResidence
            )
        }
    }
}
