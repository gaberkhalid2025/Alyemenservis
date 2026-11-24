package com.example.data.repositories

import androidx.annotation.Keep
import com.example.data.MallEntity
import com.example.data.StoreEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

@Keep
data class MallTenantItem(
    val id: String = "",
    val floorNumber: Int = 1,
    val floorLabel: String = "الدور الأول",
    val shopName: String = "",
    val category: String = "",
    val unitCode: String = "",
    val phone: String = ""
)

/**
 * 🏬 MallRepository
 * مستودع مخصص لإدارة المراكز التجارية والمولات (طوابق المول ودليل المحلات المستأجرة)
 * مع جلب أحادي موفر لحصة Firebase Spark وكاش محلي.
 */
object MallRepository {

    private const val MALLS_COLLECTION = "malls"
    private const val STORES_COLLECTION = "stores"
    private const val CACHE_TTL_MS = 10L * 60L * 1000L

    private data class CachedMall(val fetchedAt: Long, val mall: MallEntity)
    private val cache = ConcurrentHashMap<String, CachedMall>()

    suspend fun getOrResolveMall(store: StoreEntity): MallEntity {
        val now = System.currentTimeMillis()
        cache[store.id]?.let {
            if ((now - it.fetchedAt) < CACHE_TTL_MS) return it.mall
        }
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection(MALLS_COLLECTION)
                .document(store.id)
                .get()
                .await()
            val resolved = doc.toObject(MallEntity::class.java)?.copy(id = doc.id)
                ?: defaultMallFromStore(store)
            cache[store.id] = CachedMall(now, resolved)
            resolved
        } catch (_: Exception) {
            defaultMallFromStore(store)
        }
    }

    fun defaultMallFromStore(store: StoreEntity): MallEntity {
        return MallEntity(
            id = store.id,
            name = store.name,
            description = store.description,
            ownerId = store.ownerId,
            ownerName = store.ownerName,
            phone = store.phone,
            cityId = store.cityId,
            localNeighborhood = store.localNeighborhood,
            coverImage = store.coverImage,
            logoImage = store.logoImage,
            rating = store.rating,
            numReviews = store.numReviews,
            isActive = store.isActive,
            workingHours = store.workingHours.ifBlank { "9:00 AM - 11:00 PM" },
            latitude = store.latitude,
            longitude = store.longitude,
            floorsCount = 3,
            storesCount = 0,
            images = store.images,
            floorsDirectoryJson = "",
            createdAt = store.createdAt
        )
    }

    suspend fun saveMallDirectory(
        mall: MallEntity,
        tenants: List<MallTenantItem>,
        floorsCount: Int
    ): Result<MallEntity> {
        return try {
            val encodedJson = encodeTenantsJson(tenants)
            val updated = mall.copy(
                floorsCount = floorsCount.coerceIn(1, 20),
                storesCount = tenants.size,
                floorsDirectoryJson = encodedJson
            )
            val db = FirebaseFirestore.getInstance()
            db.collection(MALLS_COLLECTION).document(updated.id).set(updated, SetOptions.merge()).await()
            db.collection(STORES_COLLECTION).document(updated.id).set(
                mapOf(
                    "sectionId" to "malls",
                    "floorsCount" to updated.floorsCount,
                    "storesCount" to updated.storesCount,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            cache[updated.id] = CachedMall(System.currentTimeMillis(), updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseTenantsJson(json: String): List<MallTenantItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val floor = obj.optInt("floorNumber", 1).coerceAtLeast(1)
                    add(
                        MallTenantItem(
                            id = obj.optString("id", "t_$i"),
                            floorNumber = floor,
                            floorLabel = obj.optString("floorLabel", defaultFloorLabel(floor)),
                            shopName = obj.optString("shopName", ""),
                            category = obj.optString("category", ""),
                            unitCode = obj.optString("unitCode", ""),
                            phone = obj.optString("phone", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeTenantsJson(items: List<MallTenantItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("floorNumber", item.floorNumber)
                put("floorLabel", item.floorLabel.ifBlank { defaultFloorLabel(item.floorNumber) })
                put("shopName", item.shopName)
                put("category", item.category)
                put("unitCode", item.unitCode)
                put("phone", item.phone)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun defaultFloorLabel(floorNumber: Int): String = when (floorNumber) {
        0 -> "القبو / مواقف"
        1 -> "الدور الأرضي"
        2 -> "الدور الأول"
        3 -> "الدور الثاني"
        4 -> "الدور الثالث"
        else -> "الدور $floorNumber"
    }
}
