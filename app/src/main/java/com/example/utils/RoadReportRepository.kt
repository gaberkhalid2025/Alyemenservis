package com.example.utils

import android.content.Context
import androidx.annotation.Keep
import com.example.ui.screens.map.MapZone
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

enum class RoadReportType(
    val code: String,
    val labelAr: String,
    val emoji: String,
    val colorHex: String,
    val defaultTtlHours: Int
) {
    HAZARD("HAZARD", "طريق وعر / حفرة", "⚠️", "#F59E0B", 12),
    FLOOD("FLOOD", "تجمع سيول / أمطار", "🌊", "#0EA5E9", 8),
    LANDSLIDE("LANDSLIDE", "انهيار صخري / نقيل مغلق", "🪨", "#EF4444", 12),
    TRAFFIC("TRAFFIC", "ازدحام شديد", "🚦", "#F97316", 3),
    ACCIDENT("ACCIDENT", "حادث مروري", "🚨", "#EF4444", 4),
    ROADBLOCK("ROADBLOCK", "إغلاق / تحويلة طريق", "🚧", "#DC2626", 10);

    companion object {
        fun fromCode(code: String?): RoadReportType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HAZARD
    }
}

@Keep
data class RoadReportEntity(
    val id: String = "",
    val cellId: String = "",
    val type: String = RoadReportType.HAZARD.code,
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val reporterId: String = "",
    val confirmCount: Int = 1,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
) {
    fun toMapZone(): MapZone {
        val reportType = RoadReportType.fromCode(type)
        return MapZone(
            lat = latitude,
            lng = longitude,
            radiusM = 280,
            colorHex = reportType.colorHex
        )
    }
}

/**
 * 🚧 RoadReportRepository
 * السحابة التجميعية لبلاغات الطريق بأسلوب Waze مع حماية صارمة لحصة Firebase Spark المجانية:
 * - قراءة لمرة واحدة (.get) فقط بدون أي SnapshotListener مفتوح.
 * - تقسيم جغرافي بالخلايا (~1.1 كم) وبحد أقصى 4 خلايا و10 بلاغات لكل جلب.
 * - ذاكرة مؤقتة لمدة 15 دقيقة وفترة تهدئة 2 دقيقة للتحديث اليدوي.
 * - حد كتابة محلي: بحد أقصى 5 بلاغات يومياً لكل جهاز وبفاصل 10 دقائق.
 */
object RoadReportRepository {

    private const val COLLECTION = "road_reports"
    private const val PREFS_NAME = "road_reports_quota_prefs"
    private const val CACHE_TTL_MS = 15L * 60L * 1000L
    private const val MANUAL_REFRESH_COOLDOWN_MS = 2L * 60L * 1000L
    private const val WRITE_COOLDOWN_MS = 10L * 60L * 1000L
    private const val MAX_WRITES_PER_DAY = 5

    private data class CachedCellBatch(
        val key: String,
        val fetchedAt: Long,
        val reports: List<RoadReportEntity>
    )

    private val memoryCache = ConcurrentHashMap<String, CachedCellBatch>()
    @Volatile
    private var lastManualFetchAt: Long = 0L

    fun computeCellId(lat: Double, lng: Double): String {
        val cLat = (lat * 100.0).roundToInt() / 100.0
        val cLng = (lng * 100.0).roundToInt() / 100.0
        return String.format(Locale.US, "%.2f_%.2f", cLat, cLng)
    }

    private fun computeAdjacentCells(lat: Double, lng: Double): List<String> {
        val center = computeCellId(lat, lng)
        val cLat = (lat * 100.0).roundToInt() / 100.0
        val cLng = (lng * 100.0).roundToInt() / 100.0
        val neighbors = listOf(
            String.format(Locale.US, "%.2f_%.2f", cLat + 0.01, cLng),
            String.format(Locale.US, "%.2f_%.2f", cLat - 0.01, cLng),
            String.format(Locale.US, "%.2f_%.2f", cLat, cLng + 0.01)
        )
        return (listOf(center) + neighbors).distinct().take(4)
    }

    suspend fun fetchNearbyReports(
        lat: Double,
        lng: Double,
        forceRefresh: Boolean = false
    ): Result<List<RoadReportEntity>> {
        if (!LandmarkAddress.isValid(lat, lng)) {
            return Result.success(emptyList())
        }
        val now = System.currentTimeMillis()
        val cells = computeAdjacentCells(lat, lng)
        val cacheKey = cells.first()
        val cached = memoryCache[cacheKey]

        if (!forceRefresh && cached != null && (now - cached.fetchedAt) < CACHE_TTL_MS) {
            return Result.success(cached.reports.filter { it.expiresAt > now })
        }
        if (forceRefresh && cached != null && (now - lastManualFetchAt) < MANUAL_REFRESH_COOLDOWN_MS) {
            return Result.success(cached.reports.filter { it.expiresAt > now })
        }

        return try {
            lastManualFetchAt = now
            val snap = FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .whereIn("cellId", cells)
                .limit(10L)
                .get()
                .await()

            val activeReports = snap.documents.mapNotNull { doc ->
                doc.toObject(RoadReportEntity::class.java)?.copy(id = doc.id)
            }.filter { it.expiresAt > now && LandmarkAddress.isValid(it.latitude, it.longitude) }

            memoryCache[cacheKey] = CachedCellBatch(cacheKey, now, activeReports)
            Result.success(activeReports)
        } catch (e: Exception) {
            if (cached != null) {
                Result.success(cached.reports.filter { it.expiresAt > now })
            } else {
                Result.failure(e)
            }
        }
    }

    fun getCachedNearby(lat: Double, lng: Double): List<RoadReportEntity> {
        if (!LandmarkAddress.isValid(lat, lng)) return emptyList()
        val now = System.currentTimeMillis()
        val key = computeCellId(lat, lng)
        return memoryCache[key]?.reports?.filter { it.expiresAt > now } ?: emptyList()
    }

    suspend fun submitReport(
        context: Context,
        type: RoadReportType,
        lat: Double,
        lng: Double,
        description: String
    ): Result<RoadReportEntity> {
        if (!LandmarkAddress.isValid(lat, lng)) {
            return Result.failure(IllegalArgumentException("يرجى تحديد موقع صحيح على الخريطة قبل إرسال البلاغ"))
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayKey = "count_" + DateFormatter.formatDateKeyUs(now)
        val todayCount = prefs.getInt(todayKey, 0)
        val lastWriteAt = prefs.getLong("last_write_at", 0L)

        if (todayCount >= MAX_WRITES_PER_DAY) {
            return Result.failure(IllegalStateException("وصلت للحد الأقصى اليومي للبلاغات ($MAX_WRITES_PER_DAY بلاغات يومياً) حفاظاً على جودة البيانات"))
        }
        val remainingCooldownSec = ((WRITE_COOLDOWN_MS - (now - lastWriteAt)) / 1000L).coerceAtLeast(0L)
        if (lastWriteAt > 0L && remainingCooldownSec > 0L) {
            val mins = (remainingCooldownSec / 60L) + 1L
            return Result.failure(IllegalStateException("يرجى الانتظار $mins دقيقة قبل إرسال بلاغ جديد"))
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("يجب تفعيل الجلسة أولاً لإرسال البلاغ"))
        }

        val cleanDesc = description.trim().take(180)
        val cellId = computeCellId(lat, lng)
        val reportId = "rr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val expiresAt = now + (type.defaultTtlHours * 3600_000L)

        val entity = RoadReportEntity(
            id = reportId,
            cellId = cellId,
            type = type.code,
            title = "${type.emoji} ${type.labelAr}",
            description = cleanDesc,
            latitude = lat,
            longitude = lng,
            reporterId = uid,
            confirmCount = 1,
            createdAt = now,
            expiresAt = expiresAt
        )

        return try {
            FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .document(reportId)
                .set(entity)
                .await()

            prefs.edit()
                .putInt(todayKey, todayCount + 1)
                .putLong("last_write_at", now)
                .apply()

            val existing = memoryCache[cellId]?.reports.orEmpty()
            memoryCache[cellId] = CachedCellBatch(cellId, now, (listOf(entity) + existing).distinctBy { it.id }.take(10))
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmReport(context: Context, report: RoadReportEntity): Result<Unit> {
        if (report.id.isBlank()) return Result.failure(IllegalArgumentException("معرف البلاغ غير صالح"))
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val confirmedKey = "confirmed_${report.id}"
        if (prefs.getBoolean(confirmedKey, false)) {
            return Result.failure(IllegalStateException("تم تأكيد هذا البلاغ من جهازك مسبقاً"))
        }
        return try {
            FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .document(report.id)
                .update("confirmCount", FieldValue.increment(1L))
                .await()
            prefs.edit().putBoolean(confirmedKey, true).apply()
            val cellKey = report.cellId.ifBlank { computeCellId(report.latitude, report.longitude) }
            memoryCache[cellKey]?.let { batch ->
                val updated = batch.reports.map {
                    if (it.id == report.id) it.copy(confirmCount = it.confirmCount + 1) else it
                }
                memoryCache[cellKey] = batch.copy(reports = updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
