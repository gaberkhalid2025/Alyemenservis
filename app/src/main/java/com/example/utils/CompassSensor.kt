package com.example.utils

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 🧭 اتجاه الجهاز بالدرجات (0 = شمال حقيقي، يزداد مع عقارب الساعة).
 * [إصلاح 18] بوصلة حقيقية باستخدام حساس تدوير الجهاز (Rotation Vector) مع حساب زاوية الميل المغناطيسي
 */
@Composable
fun rememberHeadingDegrees(enabled: Boolean, lat: Double, lng: Double): Float {
    val context = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!enabled || sm == null || sensor == null) return@DisposableEffect onDispose { }
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        var smoothed = 0f
        var first = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val magnetic = Math.toDegrees(orient[0].toDouble()).toFloat()
                val declination = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
                val az = (magnetic + declination + 360f) % 360f
                if (first) {
                    smoothed = az
                    first = false
                } else {
                    var d = az - smoothed
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    smoothed = (smoothed + 0.15f * d + 360f) % 360f // تنعيم مع التفاف 359→1
                }
                heading = smoothed
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return heading
}
