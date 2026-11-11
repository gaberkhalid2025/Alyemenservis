package com.example.utils

import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

data class TrackingSnapshot(
    val lat: Double, val lng: Double, val updatedAt: Long,
    val status: String, val label: String, val expiresAt: Long
) {
    fun isExpired(now: Long = System.currentTimeMillis()) = now >= expiresAt || status != "ACTIVE"
}

object TrackingShareManager {
    private const val COL = "public_tracking"
    const val MIN_WRITE_INTERVAL_MS = 30_000L
    val TOKEN_REGEX = Regex("^[A-Za-z0-9_-]{24}$")
    private val rng = SecureRandom()
    @Volatile private var lastWriteAt = 0L

    fun newToken(): String {
        val b = ByteArray(18); rng.nextBytes(b)                         // 18 بايت = 24 حرفاً
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** يعيد الرمز أو null عند الفشل. */
    suspend fun create(db: FirebaseFirestore, ownerUid: String, bookingId: String, lat: Double, lng: Double,
                       label: String, ttlMs: Long = 2 * 3_600_000L): String? {
        if (ownerUid.isBlank() || !LandmarkAddress.isValid(lat, lng)) return null
        val token = newToken(); val now = System.currentTimeMillis()
        val data = hashMapOf<String, Any>(
            "ownerUid" to ownerUid, "bookingId" to bookingId, "lat" to lat, "lng" to lng,
            "status" to "ACTIVE", "label" to label.take(40), "updatedAt" to now, "expiresAt" to now + ttlMs
        )
        return try { db.collection(COL).document(token).set(data).await(); lastWriteAt = now; token } catch (e: Exception) { null }
    }

    /** تحديث يدوي للموقع (كتابة واحدة). يعيد false إن كان قبل المهلة أو فشل. */
    suspend fun update(db: FirebaseFirestore, token: String, lat: Double, lng: Double): Boolean {
        val now = System.currentTimeMillis()
        if (!TOKEN_REGEX.matches(token) || !LandmarkAddress.isValid(lat, lng) || now - lastWriteAt < MIN_WRITE_INTERVAL_MS) return false
        return try {
            db.collection(COL).document(token).update(mapOf("lat" to lat, "lng" to lng, "updatedAt" to now)).await()
            lastWriteAt = now; true
        } catch (e: Exception) { false }
    }

    suspend fun revoke(db: FirebaseFirestore, token: String): Boolean {
        if (!TOKEN_REGEX.matches(token)) return false
        return try { db.collection(COL).document(token).delete().await(); true } catch (e: Exception) { false }
    }

    /** قراءة واحدة. null = غير موجود/منتهٍ/خطأ. */
    suspend fun fetch(db: FirebaseFirestore, token: String): TrackingSnapshot? {
        if (!TOKEN_REGEX.matches(token)) return null
        return try {
            val s = db.collection(COL).document(token).get().await()
            if (!s.exists()) return null
            val snap = TrackingSnapshot(
                lat = s.getDouble("lat") ?: return null, lng = s.getDouble("lng") ?: return null,
                updatedAt = s.getLong("updatedAt") ?: 0L, status = s.getString("status") ?: "ENDED",
                label = s.getString("label").orEmpty(), expiresAt = s.getLong("expiresAt") ?: 0L
            )
            if (snap.isExpired()) null else snap
        } catch (e: Exception) { null }
    }

    /** نص للمشاركة: رابط التطبيق + رابط OSM للموقع الحالي + الرمز للإدخال اليدوي. */
    fun shareText(token: String, lat: Double, lng: Double): String =
        "تتبّع موقعي لمدة محدودة:\n• التطبيق: alyemen://track/$token\n• الرمز: $token\n• آخر موقع معروف: ${SosDispatcher.mapLink(lat, lng)}"
}
