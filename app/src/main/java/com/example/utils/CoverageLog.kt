package com.example.utils

import android.content.Context
import com.example.ui.screens.map.MapZone
import org.json.JSONObject
import kotlin.math.floor

/**
 * 📡 سجل تغطية الشبكة المتعلم ذاتياً محلياً (SharedPreferences، ≤ 200 خلية).
 * يرصد مناطق انقطاع الإنترنت أثناء تنقّل المستخدم لعرضها لاحقاً بلا أي خوادم.
 */
object CoverageLog {
    private const val PREFS = "coverage_log"
    private const val KEY = "cells"
    private const val CELL = 0.005 // ≈ 550 متر
    private const val MAX_CELLS = 200

    @Synchronized
    fun sample(context: Context, lat: Double, lng: Double, online: Boolean) {
        if (!LandmarkAddress.isValid(lat, lng)) return
        try {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val root = runCatching { JSONObject(sp.getString(KEY, "{}") ?: "{}") }.getOrElse { JSONObject() }
            val cellKey = "${floor(lat / CELL).toLong()}_${floor(lng / CELL).toLong()}"
            val entry = root.optJSONObject(cellKey) ?: JSONObject()
            val ok = entry.optInt("ok", 0)
            val off = entry.optInt("off", 0)
            if (online) entry.put("ok", ok + 1) else entry.put("off", off + 1)
            entry.put("ts", System.currentTimeMillis())
            root.put(cellKey, entry)

            if (root.length() > MAX_CELLS) {
                trimOldest(root, MAX_CELLS)
            }
            sp.edit().putString(KEY, root.toString()).apply()
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun deadZones(context: Context): List<MapZone> {
        return try {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val root = JSONObject(sp.getString(KEY, "{}") ?: "{}")
            val out = mutableListOf<MapZone>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val parts = k.split("_")
                if (parts.size != 2) continue
                val cLat = parts[0].toLongOrNull() ?: continue
                val cLng = parts[1].toLongOrNull() ?: continue
                val obj = root.optJSONObject(k) ?: continue
                val ok = obj.optInt("ok", 0)
                val off = obj.optInt("off", 0)
                val total = ok + off
                if (total >= 3 && off.toDouble() / total >= 0.7) {
                    val lat = (cLat + 0.5) * CELL
                    val lng = (cLng + 0.5) * CELL
                    out.add(MapZone(lat = lat, lng = lng, radiusM = 300, colorHex = "#7C3AED"))
                }
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @Synchronized
    fun cellCount(context: Context): Int = try {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        JSONObject(sp.getString(KEY, "{}") ?: "{}").length()
    } catch (_: Throwable) { 0 }

    @Synchronized
    fun clear(context: Context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        } catch (_: Throwable) {}
    }

    private fun trimOldest(root: JSONObject, limit: Int) {
        val list = mutableListOf<Pair<String, Long>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val ts = root.optJSONObject(k)?.optLong("ts", 0L) ?: 0L
            list.add(k to ts)
        }
        list.sortBy { it.second }
        val toRemove = list.size - limit
        for (i in 0 until toRemove) {
            root.remove(list[i].first)
        }
    }
}
