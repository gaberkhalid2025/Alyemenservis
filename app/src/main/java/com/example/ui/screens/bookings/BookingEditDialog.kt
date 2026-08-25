package com.example.ui.screens.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.utils.BookingUtils
import java.util.Calendar
import java.util.Locale

/**
 * ✏️ BookingEditDialog
 * نافذة تعديل بيانات الحجز مع التحقق الصارم من كلمة المرور وقاعدة الـ 8 ساعات قبل الموعد.
 */
@Composable
fun BookingEditDialog(
    booking: BookingEntity,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmEdit: (updatedBooking: BookingEntity, passwordInput: String) -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(booking.fullName.ifBlank { booking.customerName }) }
    var clientPhoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf(booking.fullAddress.ifBlank { booking.customerArea }) }
    var selectedDate by remember { mutableStateOf(booking.date.ifBlank { booking.dateString }) }
    var selectedTime by remember { mutableStateOf(booking.time.ifBlank { booking.timeString }) }
    var serviceDetails by remember { mutableStateOf(booking.serviceDetails) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canModify = BookingUtils.canModifyOrCancelBooking(
        scheduledAtTimestamp = booking.scheduledAt,
        dateString = selectedDate,
        timeString = selectedTime
    )

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val period = if (hourOfDay < 12) "صباحاً" else "مساءً"
            val hourFormatted = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            selectedTime = String.format(Locale.US, "%02d:%02d %s", hourFormatted, minute, period)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تعديل تفاصيل الحجز", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!canModify && !isAdmin) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "تنبيه: لا يمكن التعديل؛ تبقى أقل من 8 ساعات على الموعد المحدد.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Phone Verification (if not admin)
                if (!isAdmin) {
                    OutlinedTextField(
                        value = clientPhoneInput,
                        onValueChange = { clientPhoneInput = it; errorMessage = null },
                        label = { Text("رقم الهاتف للتحقق *") },
                        placeholder = { Text("أدخل رقم الهاتف الذي استخدمته في الحجز") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMessage = null },
                        label = { Text("كلمة سر الحجز *") },
                        placeholder = { Text("أدخل كلمة المرور الخاصة بالحجز") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الثلاثي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it },
                    label = { Text("العنوان والحي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time pickers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedCard(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedTime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = serviceDetails,
                    onValueChange = { serviceDetails = it },
                    label = { Text("تفاصيل وملاحظات الحجز") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                    maxLines = 3
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!canModify && !isAdmin) {
                        errorMessage = "لا يمكن التعديل: تبقى أقل من 8 ساعات على الموعد."
                        return@Button
                    }

                    if (!isAdmin) {
                        val cleanExpectedPhone = booking.clientPhone.ifBlank { booking.customerPhone }.trim()
                        val cleanInputPhone = clientPhoneInput.trim()
                        val cleanExpectedPass = booking.bookingPassword.ifBlank { booking.pinCode }.trim()
                        val cleanInputPass = passwordInput.trim()

                        if (cleanInputPhone.isBlank() || cleanInputPass.isBlank()) {
                            errorMessage = "يرجى أدخال رقم الهاتف وكلمة سر الحجز للتحقق"
                            return@Button
                        }

                        if (cleanInputPhone != cleanExpectedPhone) {
                            errorMessage = "رقم الهاتف غير مطابق لرقم الحجز المسجل"
                            return@Button
                        }

                        if (cleanInputPass != cleanExpectedPass) {
                            errorMessage = "كلمة المرور غير صحيحة"
                            return@Button
                        }
                    }

                    val updatedScheduledTs = BookingUtils.parseScheduledTimestamp(selectedDate, selectedTime)
                    val updatedBooking = booking.copy(
                        fullName = fullName.trim(),
                        customerName = fullName.trim(),
                        fullAddress = fullAddress.trim(),
                        customerArea = fullAddress.trim(),
                        date = selectedDate,
                        dateString = selectedDate,
                        time = selectedTime,
                        timeString = selectedTime,
                        scheduledAt = updatedScheduledTs,
                        serviceDetails = serviceDetails.trim(),
                        updatedAt = System.currentTimeMillis()
                    )

                    onConfirmEdit(updatedBooking, passwordInput)
                },
                enabled = canModify || isAdmin
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
