package com.example.utils

import kotlin.math.cos

/**
 * 🧭 مرشح كالمان ثنائي المحور (شرق/شمال بالمتر) لتنعيم قراءات GPS المتذبذبة
 * دون تأخير ملموس ودون أي تبعيات خارجية.
 */
class LatLngKalman(private val processNoiseMps: Double = 3.0) {
    private var refLat = 0.0
    private var refLng = 0.0
    private var cosLat = 1.0

    private var xE = 0.0
    private var pE = 0.0
    private var xN = 0.0
    private var pN = 0.0
    private var lastTimeMs = 0L

    fun reset() {
        lastTimeMs = 0L
        xE = 0.0
        xN = 0.0
        pE = 0.0
        pN = 0.0
    }

    fun update(
        lat: Double,
        lng: Double,
        accuracyM: Float,
        timeMs: Long = System.currentTimeMillis()
    ): Pair<Double, Double> {
        if (lat.isNaN() || lng.isNaN()) return lat to lng
        val acc = accuracyM.coerceAtLeast(3f).toDouble()
        val r = acc * acc

        if (lastTimeMs == 0L || timeMs - lastTimeMs > 10_000L || timeMs < lastTimeMs) {
            refLat = lat
            refLng = lng
            cosLat = cos(Math.toRadians(lat)).coerceAtLeast(0.1)
            xE = 0.0
            xN = 0.0
            pE = r
            pN = r
            lastTimeMs = timeMs
            return lat to lng
        }

        val dtSec = ((timeMs - lastTimeMs).coerceAtLeast(1L)) / 1000.0
        lastTimeMs = timeMs

        val q = (processNoiseMps * dtSec) * (processNoiseMps * dtSec)
        val measN = (lat - refLat) * METERS_PER_DEG
        val measE = (lng - refLng) * METERS_PER_DEG * cosLat

        pE += q
        val kE = pE / (pE + r)
        xE += kE * (measE - xE)
        pE *= (1.0 - kE)

        pN += q
        val kN = pN / (pN + r)
        xN += kN * (measN - xN)
        pN *= (1.0 - kN)

        val outLat = refLat + xN / METERS_PER_DEG
        val outLng = refLng + xE / (METERS_PER_DEG * cosLat)
        return outLat to outLng
    }

    companion object {
        private const val METERS_PER_DEG = 111_320.0
    }
}
