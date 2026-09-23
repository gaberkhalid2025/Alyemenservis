package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 📦 ServiceItemModel - Simple representation of service entity in UI
 */
data class ServiceItemUiModel(
    val id: String,
    val title: String,
    val category: String,
    val priceText: String,
    val rating: Float = 4.8f
)

/**
 * 🎨 ServiceListViewState - Sealed UI state for services list
 */
sealed class ServiceListViewState {
    object Loading : ServiceListViewState()
    object Empty : ServiceListViewState()
    data class Success(val services: List<ServiceItemUiModel>) : ServiceListViewState()
    data class Error(val message: String) : ServiceListViewState()
}

/**
 * 📱 ServicesListTestView - Composable rendering different states for UI testing
 */
@Composable
fun ServicesListTestView(
    state: ServiceListViewState,
    onServiceClick: (ServiceItemUiModel) -> Unit = {}
) {
    MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is ServiceListViewState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("services_loading_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ServiceListViewState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .testTag("services_empty_state_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد خدمات متاحة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("empty_state_message")
                        )
                    }
                }
                is ServiceListViewState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .testTag("services_list_container"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.services, key = { it.id }) { service ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("service_card_${service.id}"),
                                onClick = { onServiceClick(service) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = service.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.testTag("service_title_${service.id}")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = service.category)
                                        Text(text = service.priceText)
                                    }
                                }
                            }
                        }
                    }
                }
                is ServiceListViewState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("services_error_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

/**
 * 🧪 ServicesListScreenUiTest
 * UI test verifying that the services screen accurately renders loading, empty, and populated data
 * states across both Portrait and Landscape screen orientations using Compose Test Rule and Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ServicesListScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==========================================
    // 1. LOADING STATE TEST
    // ==========================================

    @Test
    fun `test loading state displays circular progress indicator`() {
        composeTestRule.setContent {
            ServicesListTestView(state = ServiceListViewState.Loading)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_loading_indicator").assertIsDisplayed()
    }

    // ==========================================
    // 2. EMPTY STATE TEST
    // ==========================================

    @Test
    fun `test empty state displays clear empty message`() {
        composeTestRule.setContent {
            ServicesListTestView(state = ServiceListViewState.Empty)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_empty_state_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_message").assertIsDisplayed()
    }

    // ==========================================
    // 3. POPULATED DATA LIST TEST
    // ==========================================

    @Test
    fun `test populated services list displays items correctly`() {
        val sampleServices = listOf(
            ServiceItemUiModel(
                id = "srv_1",
                title = "خدمة تركيب الطاقة الشمسية",
                category = "طاقة وكهرباء",
                priceText = "25,000 ر.ي"
            ),
            ServiceItemUiModel(
                id = "srv_2",
                title = "صيانة شبكات وتمديدات مياه",
                category = "سباكة",
                priceText = "15,000 ر.ي"
            )
        )

        composeTestRule.setContent {
            ServicesListTestView(state = ServiceListViewState.Success(sampleServices))
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_list_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_card_srv_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("خدمة تركيب الطاقة الشمسية").assertExists()
        composeTestRule.onNodeWithTag("service_card_srv_2").assertIsDisplayed()
        composeTestRule.onNodeWithText("صيانة شبكات وتمديدات مياه").assertExists()
    }

    // ==========================================
    // 4. SCREEN ORIENTATIONS (PORTRAIT & LANDSCAPE)
    // ==========================================

    @Test
    @Config(qualifiers = "port")
    fun `test services list renders properly in portrait orientation`() {
        val sampleServices = listOf(
            ServiceItemUiModel(
                id = "srv_port",
                title = "فحص وصيانة أجهزة تكييف",
                category = "تبريد وتكييف",
                priceText = "18,000 ر.ي"
            )
        )

        composeTestRule.setContent {
            ServicesListTestView(state = ServiceListViewState.Success(sampleServices))
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_list_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_card_srv_port").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "land")
    fun `test services list renders properly in landscape orientation`() {
        val sampleServices = listOf(
            ServiceItemUiModel(
                id = "srv_land",
                title = "فحص وصيانة مصاعد كهربائية",
                category = "كهرباء ومصاعد",
                priceText = "40,000 ر.ي"
            )
        )

        composeTestRule.setContent {
            ServicesListTestView(state = ServiceListViewState.Success(sampleServices))
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_list_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_card_srv_land").assertIsDisplayed()
    }
}
