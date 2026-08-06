package com.example.data.local

import android.content.Context
import com.example.data.LocalAppCacheManager
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MapDao(private val context: Context) {
    private val cacheManager = LocalAppCacheManager(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    fun saveProviders(providers: List<ProviderEntity>) {
        try {
            val type = Types.newParameterizedType(List::class.java, ProviderEntity::class.java)
            val adapter = moshi.adapter<List<ProviderEntity>>(type)
            cacheManager.saveProvidersCache(adapter.toJson(providers))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProviders(): List<ProviderEntity> {
        return try {
            val raw = cacheManager.getProvidersCacheRaw()
            if (raw.isEmpty() || raw == "[]") return emptyList()
            val type = Types.newParameterizedType(List::class.java, ProviderEntity::class.java)
            val adapter = moshi.adapter<List<ProviderEntity>>(type)
            adapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveStores(stores: List<StoreEntity>) {
        try {
            val type = Types.newParameterizedType(List::class.java, StoreEntity::class.java)
            val adapter = moshi.adapter<List<StoreEntity>>(type)
            cacheManager.saveStoresCache(adapter.toJson(stores))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getStores(): List<StoreEntity> {
        return try {
            val raw = cacheManager.getStoresCacheRaw()
            if (raw.isEmpty() || raw == "[]") return emptyList()
            val type = Types.newParameterizedType(List::class.java, StoreEntity::class.java)
            val adapter = moshi.adapter<List<StoreEntity>>(type)
            adapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProperties(properties: List<PropertyEntity>) {
        try {
            val type = Types.newParameterizedType(List::class.java, PropertyEntity::class.java)
            val adapter = moshi.adapter<List<PropertyEntity>>(type)
            val raw = adapter.toJson(properties)
            context.getSharedPreferences("YS_Properties_Cache", Context.MODE_PRIVATE)
                .edit().putString("KEY_PROPERTIES_CACHE", raw).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProperties(): List<PropertyEntity> {
        return try {
            val raw = context.getSharedPreferences("YS_Properties_Cache", Context.MODE_PRIVATE)
                .getString("KEY_PROPERTIES_CACHE", "[]") ?: "[]"
            if (raw.isEmpty() || raw == "[]") return emptyList()
            val type = Types.newParameterizedType(List::class.java, PropertyEntity::class.java)
            val adapter = moshi.adapter<List<PropertyEntity>>(type)
            adapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class MapDatabase private constructor(context: Context) {
    val mapDao: MapDao = MapDao(context)

    companion object {
        @Volatile
        private var INSTANCE: MapDatabase? = null

        fun getInstance(context: Context): MapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = MapDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
