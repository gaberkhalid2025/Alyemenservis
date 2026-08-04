package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdminSettingsEntity
import com.example.data.CategoryEntity
import com.example.data.CityEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.util.AIAssistantEngine
import com.example.util.AssistantResponse
import kotlin.math.roundToInt

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "BOT"
    val text: String,
    val title: String? = null,
    val actionLabel: String? = null,
    val actionRoute: String? = null,
    val suggestionChips: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AIAssistantDialog(
    settings: AdminSettingsEntity,
    providers: List<ProviderEntity>,
    cities: List<CityEntity>,
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val assistantEngine = remember { AIAssistantEngine(context) }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    sender = "BOT",
                    text = assistantEngine.getWelcomeMessage(),
                    suggestionChips = assistantEngine.getDefaultSuggestions()
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }

    fun handleSend(textToSend: String) {
        if (textToSend.isBlank()) return

        val userMsg = ChatMessage(sender = "USER", text = textToSend)
        messages = messages + userMsg

        val botResponse: AssistantResponse = assistantEngine.processQuery(
            query = textToSend,
            providers = providers,
            cities = cities,
            stores = stores,
            categories = categories,
            isOnline = isOnline
        )

        val botMsg = ChatMessage(
            sender = "BOT",
            title = botResponse.title.ifBlank { null },
            text = botResponse.text,
            actionLabel = botResponse.actionLabel,
            actionRoute = botResponse.actionRoute,
            suggestionChips = botResponse.suggestionChips
        )

        messages = messages + botMsg
        inputText = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🤖", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "المساعد الذكي 🇾🇪",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF059669) else Color(0xFFD97706))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOnline) "متصل (أونلاين)" else "يعمل بدون إنترنت (أوفلاين)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Chat Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onActionClick = { route ->
                                if (route.isNotBlank()) {
                                    onDismiss()
                                    onNavigateRoute(route)
                                }
                            },
                            onChipClick = { chipText ->
                                handleSend(chipText)
                            }
                        )
                    }
                }

                // Input Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("اكتب استفسارك هنا...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 100.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        IconButton(
                            onClick = { handleSend(inputText) },
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onActionClick: (String) -> Unit,
    onChipClick: (String) -> Unit
) {
    val isUser = message.sender == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 4.dp else 16.dp,
                bottomEnd = if (isUser) 16.dp else 4.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.title.isNullOrEmpty()) {
                    Text(
                        text = message.title!!,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (!message.actionLabel.isNullOrEmpty() && !message.actionRoute.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onActionClick(message.actionRoute!!) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = message.actionLabel!!,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Suggestion Chips
        if (message.suggestionChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(message.suggestionChips) { chip ->
                    SuggestionChip(
                        onClick = { onChipClick(chip) },
                        label = { Text(chip, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

private fun String?.isNullToEmpty(): Boolean = this == null || this.isBlank()

@Composable
fun getShapeFromSetting(shapeType: String): Shape {
    return when (shapeType.uppercase()) {
        "ROUNDED" -> RoundedCornerShape(12.dp)
        "SQUARE" -> RoundedCornerShape(4.dp)
        "PILL" -> RoundedCornerShape(24.dp)
        else -> CircleShape
    }
}

@Composable
fun DraggableAssistantIcon(
    settings: AdminSettingsEntity,
    onClick: () -> Unit
) {
    if (!settings.isAssistantEnabled || !settings.isAssistantIconVisible || settings.assistantHidden) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var offsetX by remember { mutableStateOf(settings.assistantPositionX) }
    var offsetY by remember { mutableStateOf(settings.assistantPositionY) }

    val iconSize = settings.assistantSize.dp
    val shape = getShapeFromSetting(settings.assistantIconShape)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (offsetX * screenWidth.toPx()).roundToInt(),
                        (offsetY * screenHeight.toPx()).roundToInt()
                    )
                }
                .size(iconSize)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(
                    when (settings.assistantIconStyle.uppercase()) {
                        "GOLDEN_3D" -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)))
                        "NEON" -> Brush.horizontalGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)))
                        "MINIMAL" -> Brush.verticalGradient(listOf(Color(0xFF374151), Color(0xFF111827)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF059669), Color(0xFF047857)))
                    }
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x / screenWidth.toPx()).coerceIn(0.02f, 0.88f)
                        offsetY = (offsetY + dragAmount.y / screenHeight.toPx()).coerceIn(0.05f, 0.88f)
                    }
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = (settings.assistantSize * 0.45f).sp)
        }
    }
}

@Composable
fun DraggableUrgentRequestIcon(
    settings: AdminSettingsEntity,
    onClick: () -> Unit
) {
    if (!settings.isUrgentRequestEnabled || !settings.isUrgentRequestIconVisible) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var offsetX by remember { mutableStateOf(settings.urgentRequestPositionX) }
    var offsetY by remember { mutableStateOf(settings.urgentRequestPositionY) }

    val iconSize = settings.urgentRequestSize.dp
    val shape = getShapeFromSetting(settings.urgentRequestIconShape)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (offsetX * screenWidth.toPx()).roundToInt(),
                        (offsetY * screenHeight.toPx()).roundToInt()
                    )
                }
                .size(iconSize)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(
                    when (settings.urgentRequestIconStyle.uppercase()) {
                        "GOLDEN_3D" -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFD97706)))
                        "NEON" -> Brush.horizontalGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
                        "MINIMAL" -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        else -> Brush.verticalGradient(listOf(Color(0xFF059669), Color(0xFF047857)))
                    }
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x / screenWidth.toPx()).coerceIn(0.02f, 0.88f)
                        offsetY = (offsetY + dragAmount.y / screenHeight.toPx()).coerceIn(0.05f, 0.88f)
                    }
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", fontSize = (settings.urgentRequestSize * 0.45f).sp)
        }
    }
}
