package com.example

import com.example.utils.*

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    /**
     * يتحقق من وجود وسيط اتصال بالإنترنت (واي فاي، شبكة خلوية، إيثرنت).
     * أكثر مرونة لمنع الإبلاغ الخاطئ بعدم وجود اتصال عند بطء فحص VALIDATED.
     */
    fun hasInternetTransport(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * يتحقق من التحقق التام للإنترنت عبر مسار DNS/ping النظام.
     */
    fun isValidatedNetwork(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            false
        }
    }

    fun isNetworkAvailable(context: Context): Boolean = hasInternetTransport(context)
}
