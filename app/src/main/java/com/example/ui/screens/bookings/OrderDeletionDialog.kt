package com.example.ui.screens.bookings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.OrderEntity
import com.example.utils.VisualThemePalette
import kotlin.math.abs

@Composable
fun OrderDeletionDialog(
    order: OrderEntity,
    deletionCodeInput: String,
    onDeletionCodeChange: (String) -> Unit,
    themeColors: VisualThemePalette,
    onConfirmDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val correctCode = (abs(order.id.hashCode()) % 9000 + 1000).toString()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, themeColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔒 تأكيد رمز الحذف الآمن",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent
                )
                Text(
                    text = "لحذف هذا الطلب، يرجى كتابة كود الحذف المكون من 4 أرقام الظاهر على بطاقة الطلب:",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = deletionCodeInput,
                    onValueChange = onDeletionCodeChange,
                    placeholder = { Text("اكتب الرمز المكون من 4 أرقام") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColors.accent
                    ),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (deletionCodeInput.trim() == correctCode) {
                                onConfirmDelete(order.id)
                                Toast.makeText(context, "✅ تم حذف وإلغاء الطلب بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ رمز الحذف غير صحيح! يرجى المحاولة مرة أخرى.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد الحذف 🗑️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("تراجع", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
