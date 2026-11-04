package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray

class LandmarkAddressStore(context: Context) {

    private val prefs: SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            context, "landmark_addresses",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Encrypted prefs unavailable: ${e.message}")
        null                                   // لا نخزن بيانات حساسة نصاً صريحاً عند فشل التشفير
    }

    @Synchronized fun getAll(): List<LandmarkAddress> {
        val raw = prefs?.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { LandmarkAddress.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    /** true عند نجاح الحفظ (والعنوان صالح). */
    @Synchronized fun save(address: LandmarkAddress): Boolean {
        val p = prefs ?: return false
        if (address.validate() != null) return false
        val list = (listOf(address) + getAll().filterNot { it.id == address.id }).take(MAX_ADDRESSES)
        val arr = JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }
        return p.edit().putString(KEY, arr.toString()).commit()
    }

    @Synchronized fun delete(id: String): Boolean {
        val p = prefs ?: return false
        val arr = JSONArray().also { a -> getAll().filterNot { it.id == id }.forEach { a.put(it.toJson()) } }
        return p.edit().putString(KEY, arr.toString()).commit()
    }

    companion object {
        private const val TAG = "LandmarkStore"
        private const val KEY = "addresses_json"
        const val MAX_ADDRESSES = 10
    }
}
