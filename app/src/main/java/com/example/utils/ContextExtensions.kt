package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Extension functions for Context providing crash-proof intent launches.
 */
fun Context.safeStartActivity(
    intent: Intent,
    fallbackMessage: String = "تعذر فتح التطبيق المطلوب على هذا الجهاز"
): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
        false
    } catch (e: SecurityException) {
        Toast.makeText(this, "لا تتوفر صلاحية لفتح هذا التطبيق", Toast.LENGTH_SHORT).show()
        false
    } catch (e: Exception) {
        Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
        false
    }
}
