package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 📍 LocationCapture
 * التقاط موقع حي حديث ومؤكد للأجهزة أثناء التسجيل بدون اختلاق إحداثيات (Document 74)
 */
object LocationCapture {
    /** موقع حيّ حديث (≤ maxAccuracyM م)، أو null عند الفشل/المهلة. يتطلب إذن الموقع الممنوح مسبقاً. */
    @SuppressLint("MissingPermission")
    suspend fun fresh(context: Context, timeoutMs: Long = 15_000L, maxAccuracyM: Float = 60f): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val cts = CancellationTokenSource()
        val loc = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }
        cts.cancel()
        return loc?.takeIf {
            LandmarkAddress.isValid(it.latitude, it.longitude) &&
                (!it.hasAccuracy() || it.accuracy <= maxAccuracyM)
        }
    }
}
