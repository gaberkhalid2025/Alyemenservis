package com.example.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GroupMember(
    val label: String,
    val token: String,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val updatedAt: Long = 0L,
    val expired: Boolean = false
)

/**
 * 👥 متجر أعضاء المجموعة (حتى 5 أعضاء فقط، تحديث يدوي فقط — بلا أي مستمع لحظي لتوفير البيانات وحصة القراءة).
 */
class GroupStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<GroupMember> {
        val raw = prefs.getString(KEY_MEMBERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val token = o.optString("token")
                if (!TrackingShareManager.TOKEN_REGEX.matches(token)) return@mapNotNull null
                val hasCoords = o.has("lat") && o.has("lng") && !o.isNull("lat") && !o.isNull("lng")
                GroupMember(
                    label = o.optString("label").ifBlank { "عضو" },
                    token = token,
                    lastLat = if (hasCoords) o.optDouble("lat") else null,
                    lastLng = if (hasCoords) o.optDouble("lng") else null,
                    updatedAt = o.optLong("updatedAt", 0L),
                    expired = o.optBoolean("expired", false)
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @Synchronized
    fun add(label: String, rawTokenOrLink: String): Result<List<GroupMember>> {
        val token = extractToken(rawTokenOrLink)
        if (!TrackingShareManager.TOKEN_REGEX.matches(token)) {
            return Result.failure(IllegalArgumentException("الرمز غير صالح (يجب أن يكون 24 حرفاً)"))
        }
        val list = all().toMutableList()
        if (list.any { it.token == token }) {
            return Result.failure(IllegalStateException("هذا العضو مضاف مسبقاً"))
        }
        if (list.size >= MAX_MEMBERS) {
            return Result.failure(IllegalStateException("الحد الأقصى للمجموعة هو $MAX_MEMBERS أعضاء"))
        }
        list.add(
            GroupMember(
                label = label.trim().take(30).ifBlank { "عضو ${list.size + 1}" },
                token = token
            )
        )
        save(list)
        return Result.success(list)
    }

    @Synchronized
    fun remove(token: String): List<GroupMember> {
        val list = all().filterNot { it.token == token }
        save(list)
        return list
    }

    @Synchronized
    fun updateFromFetch(token: String, snap: TrackingSnapshot?): List<GroupMember> {
        val list = all().map { m ->
            if (m.token != token) m
            else if (snap == null) m.copy(expired = true)
            else m.copy(
                label = if (snap.label.isNotBlank()) snap.label else m.label,
                lastLat = snap.lat,
                lastLng = snap.lng,
                updatedAt = snap.updatedAt,
                expired = false
            )
        }
        save(list)
        return list
    }

    private fun save(list: List<GroupMember>) {
        val arr = JSONArray()
        list.take(MAX_MEMBERS).forEach { m ->
            arr.put(
                JSONObject()
                    .put("label", m.label)
                    .put("token", m.token)
                    .apply {
                        if (m.lastLat != null && m.lastLng != null) {
                            put("lat", m.lastLat)
                            put("lng", m.lastLng)
                        }
                    }
                    .put("updatedAt", m.updatedAt)
                    .put("expired", m.expired)
            )
        }
        prefs.edit().putString(KEY_MEMBERS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "group_store"
        private const val KEY_MEMBERS = "members_json"
        const val MAX_MEMBERS = 5

        fun extractToken(input: String): String {
            val trimmed = input.trim()
            return if (trimmed.contains("alyemen://track/")) {
                trimmed.substringAfter("alyemen://track/").substringBefore("?").substringBefore("/").trim()
            } else {
                trimmed
            }
        }
    }
}
