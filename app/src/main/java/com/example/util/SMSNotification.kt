package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SMSNotification {
    fun sendDirectSMS(context: Context, phone: String, message: String) {
        try {
            val uri = Uri.parse("smsto:$phone")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", message)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "فشل في فتح تطبيق الرسائل النصية القصيرة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
