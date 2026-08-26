package com.example.data.repositories

import com.example.data.RatingEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * ⭐️ RatingRepository - مستودع التقييمات والمراجعات
 * 
 * الميزات:
 * 1. تطبيق نموذج Repository Pattern لفصل منطق قواعد البيانات والشبكة عن الواجهات.
 * 2. دعم التحديث المباشر المباشر Real-time باستخدام Flow و SnapshotListener.
 * 3. دعم التخزين المؤقت المحلي (In-Memory Cache) للعمل بدون إنترنت وإتاحة سرعة فائقة.
 */
class RatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val ratingsCache = ConcurrentHashMap<String, List<RatingEntity>>()

    /**
     * جلب تدفق التقييمات بناءً على معرّف الفني/المحل/الخدمة (targetId)
     */
    fun getRatingsFlow(targetId: String): Flow<List<RatingEntity>> = callbackFlow {
        if (targetId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // إرجاع الكاش المحلي فوراً إن وجد
        ratingsCache[targetId]?.let { trySend(it) }

        val listener = firestore.collection("ratings")
            .whereEqualTo("targetId", targetId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ratingsCache[targetId] ?: emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RatingEntity::class.java)
                } ?: emptyList()

                ratingsCache[targetId] = list
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * تقديم تقييم جديد في Firestore
     */
    suspend fun submitRating(rating: RatingEntity): Result<Unit> {
        return try {
            val docId = rating.id.ifBlank { firestore.collection("ratings").document().id }
            val finalRating = rating.copy(id = docId, timestamp = System.currentTimeMillis())
            firestore.collection("ratings").document(docId).set(finalRating).await()

            // تحديث الكاش المحلي
            val currentList = ratingsCache[rating.targetId]?.toMutableList() ?: mutableListOf()
            currentList.removeAll { it.id == docId }
            currentList.add(0, finalRating)
            ratingsCache[rating.targetId] = currentList

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * جلب متوسط التقييم كـ Double
     */
    suspend fun getAverageRating(targetId: String): Double {
        return try {
            val cached = ratingsCache[targetId]
            if (cached != null && cached.isNotEmpty()) {
                return cached.map { it.rating.toDouble() }.average()
            }

            val snapshot = firestore.collection("ratings")
                .whereEqualTo("targetId", targetId)
                .get()
                .await()

            val ratings = snapshot.documents.mapNotNull { it.getDouble("rating") }
            if (ratings.isEmpty()) 0.0 else ratings.average()
        } catch (e: Exception) {
            0.0
        }
    }
}
