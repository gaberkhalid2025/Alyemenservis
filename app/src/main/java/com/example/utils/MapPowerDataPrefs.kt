package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🔋🌐 MapPowerDataPrefs
 * إدارة ذكية لتوفير البطارية والبيانات في الخرائط والملاحة دون الإخلال بالسرعة أو الدقة:
 * - تخزين البلاطات في الذاكرة والقرص يقلل إعادة التحميل ويزيد سرعة العرض
 * - تكييف فترات GPS حسب الحالة (تصفح vs ملاحة vs توفير)
 * - تجنب تنزيل البلاطات العابرة أثناء التحريك السريع
 */
object MapPowerDataPrefs {
    private const val PREFS = "map_power_data_prefs"
    private const val KEY_ECO_MODE = "eco_mode_enabled"

    private val _ecoMode = MutableStateFlow(false)
    val ecoMode: StateFlow<Boolean> = _ecoMode.asStateFlow()

    @Volatile
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            _ecoMode.value = prefs.getBoolean(KEY_ECO_MODE, false)
            loaded = true
        }
    }

    fun isEcoMode(context: Context): Boolean {
        init(context)
        return _ecoMode.value
    }

    fun setEcoMode(context: Context, enabled: Boolean) {
        init(context)
        _ecoMode.value = enabled
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ECO_MODE, enabled).apply()
    }

    fun isMeteredOrDataSaver(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val saver = cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
            saver || cm.isActiveNetworkMetered
        } catch (_: Throwable) {
            false
        }
    }
}
