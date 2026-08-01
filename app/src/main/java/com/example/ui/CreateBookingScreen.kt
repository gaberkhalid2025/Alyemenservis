package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BookingValidation {
    fun validateBookingFields(
        name: String,
        phone: String,
        address: String,
        password: String
    ): List<String> {
        val missing = mutableListOf<String>()
        val cleanName = name.trim()
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val cleanAddr = address.trim()
        val cleanPass = password.trim()

        if (cleanName.isEmpty()) missing.add("الاسم الثلاثي بالكامل")
        val isValidYemeniPhone = cleanPhone.length == 9 && (
            cleanPhone.startsWith("77") ||
            cleanPhone.startsWith("73") ||
            cleanPhone.startsWith("71") ||
            cleanPhone.startsWith("70") ||
            cleanPhone.startsWith("78")
        )
        if (cleanPhone.isEmpty()) {
            missing.add("رقم الهاتف اليمني")
        } else if (!isValidYemeniPhone) {
            missing.add("رقم الهاتف اليمني غير صحيح (يجب أن يتكون من 9 أرقام ويبدأ بـ 77، 73، 71، 70، 78)")
        }
        if (cleanAddr.isEmpty()) missing.add("العنوان أو مكان السكن والحي")
        if (cleanPass.isEmpty()) missing.add("كلمة المرور لحفظ الحجز والتعرف على الحساب")

        return missing
    }
}
