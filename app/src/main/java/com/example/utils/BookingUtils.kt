package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object BookingUtils {
    fun generateBookingNumber(prefix: String = "BK"): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val datePart = sdf.format(Date())
        val randomPart = String.format(Locale.US, "%04d", Random.nextInt(1000, 9999))
        return "$prefix-$datePart-$randomPart"
    }

    fun generateBookingPassword(length: Int = 4): String {
        val builder = StringBuilder()
        for (i in 0 until length) {
            builder.append(Random.nextInt(0, 10))
        }
        return builder.toString()
    }
}
