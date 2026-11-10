package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class GnssQuality { NONE, POOR, FAIR, GOOD }

data class GnssSnapshot(
    val visible: Int = 0,
    val used: Int = 0,
    val avgCn0: Float = 0f,          // dB-Hz (متوسط الأقمار ذات الإشارة)
    val quality: GnssQuality = GnssQuality.NONE
)

class GnssMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _state = MutableStateFlow(GnssSnapshot())
    val state: StateFlow<GnssSnapshot> = _state
    private var callback: GnssStatus.Callback? = null
    private var lastEmit = 0L

    val isGpsProviderEnabled: Boolean
        get() = try { lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true } catch (e: Exception) { false }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val manager = lm ?: return false
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
        if (callback != null) return true
        val cb = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastEmit < 1000L) return                       // تحديث واحد كل ثانية
                lastEmit = now
                var used = 0; var sum = 0f; var n = 0
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) used++
                    val c = status.getCn0DbHz(i)
                    if (c > 0f) { sum += c; n++ }
                }
                val avg = if (n > 0) sum / n else 0f
                _state.value = GnssSnapshot(status.satelliteCount, used, avg, classify(used, avg))
            }
            override fun onStopped() { _state.value = GnssSnapshot() }
        }
        return try {
            manager.registerGnssStatusCallback(cb, Handler(Looper.getMainLooper()))
            callback = cb; true
        } catch (e: Exception) { false }
    }

    fun stop() {
        callback?.let { try { lm?.unregisterGnssStatusCallback(it) } catch (_: Exception) {} }
        callback = null
        _state.value = GnssSnapshot()
    }

    companion object {
        fun classify(used: Int, avgCn0: Float): GnssQuality = when {
            used >= 8 && avgCn0 >= 30f -> GnssQuality.GOOD
            used >= 4 && avgCn0 >= 20f -> GnssQuality.FAIR
            used > 0 || avgCn0 > 0f -> GnssQuality.POOR
            else -> GnssQuality.NONE
        }
    }
}
