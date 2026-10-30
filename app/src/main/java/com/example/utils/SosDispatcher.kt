package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object SosDispatcher {

    /** رابط خريطة يعمل بدون حساب جوجل. */
    fun mapLink(lat: Double, lng: Double): String {
        val la = String.format(Locale.US, "%.6f", lat)
        val lo = String.format(Locale.US, "%.6f", lng)
        return "https://www.openstreetmap.org/?mlat=$la&mlon=$lo#map=18/$la/$lo"
    }

    fun buildMessage(lat: Double, lng: Double, accuracyM: Float?): String {
        val acc = accuracyM?.let { " (دقة ≈ ${it.toInt()} م)" } ?: ""
        return "🚨 نداء فزعة: أحتاج مساعدة عاجلة.\nموقعي$acc:\n${mapLink(lat, lng)}"
    }

    private fun clean(phone: String) = phone.filter { it.isDigit() || it == '+' }

    fun smsIntent(phone: String, body: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", clean(phone), null)).putExtra("sms_body", body)

    fun dialIntent(phone: String): Intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", clean(phone), null))

    /** مشاركة عامة (واتساب...) إن لم يتوفر رقم. */
    fun shareIntent(body: String): Intent =
        Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }, "مشاركة موقعي")

    fun safeStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
