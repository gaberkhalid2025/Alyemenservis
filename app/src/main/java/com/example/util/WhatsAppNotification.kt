package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppNotification {
    fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            var cleanPhone = phone.trim()
            if (!cleanPhone.startsWith("+")) {
                if (cleanPhone.startsWith("0")) {
                    cleanPhone = "967" + cleanPhone.substring(1)
                } else if (!cleanPhone.startsWith("967")) {
                    cleanPhone = "967$cleanPhone"
                }
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(message))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "لم يتم العثور على تطبيق واتساب في هذا الهاتف!", Toast.LENGTH_SHORT).show()
        }
    }
}
