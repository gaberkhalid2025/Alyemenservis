@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.register

import com.example.ui.screens.dashboard.*
import com.example.ui.*
import com.example.utils.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.screens.register.forms.*
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ProviderRegisterFormLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    var showRegistrationFormsAnyway by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf(0) }

    val providers by viewModel.providers.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()

    val matchingApproved = providers.find { it.phone == currentUserPhone }
    val matchingPending = pendingProviders.find { it.phone == currentUserPhone }
    val matchingStore = stores.find { it.phone == currentUserPhone }
    val matchingProperty = properties.find { it.phone == currentUserPhone }

    val hasSubmittedAccount = matchingApproved != null || matchingPending != null || matchingStore != null || matchingProperty != null

    Box(modifier = Modifier.fillMaxSize().background(themeColors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Text(
                        text = "بوابة تسجيل المزودين والشركاء",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            if (hasSubmittedAccount && !showRegistrationFormsAnyway) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, themeColors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ لديك طلب أو حساب مسجل مسبقاً بهذا الرقم ($currentUserPhone)",
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { showRegistrationFormsAnyway = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                            ) {
                                Text("إظهار استمارات التسجيل على أي حال", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    val adminRoleState by viewModel.adminRole.collectAsState()
                    val isAdmin = adminRoleState != "GUEST"
                    val allTabs = listOf(
                        Triple("🛠️ خدمات ومهن", 0, settingsState.enableProvidersRegistration),
                        Triple("🏪 محل/معرض", 1, settingsState.enableStoresRegistration),
                        Triple("🍔 مطعم/كافيه", 2, settingsState.enableRestaurantsRegistration),
                        Triple("🏢 إدراج عقار", 3, settingsState.enablePropertiesRegistration),
                        Triple("🏥 مركز طبي", 4, settingsState.enableMedicalRegistration),
                        Triple("💼 نشر وظيفة", 5, settingsState.enableJobsRegistration)
                    )
                    val availableTabs = allTabs.filter { it.third || isAdmin }
                    if (availableTabs.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🔒 جميع استمارات التسجيل مغلقة حالياً من قبل إدارة المنصة.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Yellow, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        if (availableTabs.none { it.second == selectedCategoryTab }) {
                            selectedCategoryTab = availableTabs.first().second
                        }
                        ScrollableTabRow(
                            selectedTabIndex = availableTabs.indexOfFirst { it.second == selectedCategoryTab }.coerceAtLeast(0),
                            containerColor = Color.Transparent,
                            edgePadding = 0.dp
                        ) {
                            availableTabs.forEach { tab ->
                                Tab(
                                    selected = selectedCategoryTab == tab.second,
                                    onClick = { selectedCategoryTab = tab.second },
                                    text = { Text(tab.first, color = if (selectedCategoryTab == tab.second) themeColors.accent else Color.White, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }

                item {
                    when (selectedCategoryTab) {
                        0 -> TechnicianRegistrationForm(viewModel, themeColors, settingsState)
                        1 -> StoreRegistrationForm(viewModel, themeColors, settingsState)
                        2 -> RestaurantRegistrationForm(viewModel, themeColors, settingsState)
                        3 -> PropertyRegistrationForm(viewModel, themeColors, settingsState)
                        4 -> MedicalRegistrationForm(viewModel, themeColors, settingsState)
                        5 -> JobRegistrationForm(viewModel, themeColors, settingsState)
                    }
                }
            }
        }
    }
}
