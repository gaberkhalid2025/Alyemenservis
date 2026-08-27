package com.example.ui.screens.register.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette
import java.util.Locale

/**
 * 📝 RegistrationField - حقل إدخال موحد لشاشات التسجيل مع دعم الإدخال الصوتي والتحقق
 */
@Composable
fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    enableVoiceInput: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onValueChange(if (value.isBlank()) spokenText else "$value $spokenText")
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 11.5.sp) },
            placeholder = { Text(placeholder, fontSize = 11.sp, color = Color.Gray) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = themeColors.accent) }
            },
            trailingIcon = {
                Row {
                    if (isPassword) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "عرض كلمة المرور",
                                tint = if (passwordVisible) themeColors.accent else Color.Gray
                            )
                        }
                    }
                    if (enableVoiceInput && !isPassword) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ar", "YE"))
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لإدخال $label...")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Speech recognition unavailable
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "إدخال صوتي",
                                tint = themeColors.accent
                            )
                        }
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = !errorMessage.isNullOrBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = themeColors.accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedLabelColor = themeColors.accent,
                unfocusedLabelColor = Color.Gray,
                errorBorderColor = Color(0xFFEF5350),
                errorLabelColor = Color(0xFFEF5350)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            errorMessage?.let { err ->
                Text(
                    text = "⚠️ $err",
                    color = Color(0xFFEF5350),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                )
            }
        }
    }
}
