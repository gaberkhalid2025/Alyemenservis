package com.example

import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Keep
data class Category(
    val id: Int = 0,
    val name: String = "",
    val icon: String = "",
    val order: Int = 0
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Category {
            return Category(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                name = map["name"] as? String ?: "",
                icon = map["icon"] as? String ?: "",
                order = (map["order"] as? Number)?.toInt() ?: 0
            )
        }

        /**
         * جلب الأقسام من Firebase في الوقت الفعلي عبر Flow
         */
        fun observeCategoriesRealtime(): Flow<List<Category>> = callbackFlow {
            val firestore = FirebaseFirestore.getInstance()
            val registration: ListenerRegistration = firestore.collection("categories")
                .orderBy("order")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(defaultCategories)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
                        trySend(list)
                    } else {
                        trySend(defaultCategories)
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "icon" to icon,
            "order" to order
        )
    }
}

val defaultCategories = listOf(
    Category(1, "صيانة منزلية", "🔧", 1),
    Category(2, "صحة ورعاية", "🏥", 2),
    Category(3, "تعليم وتدريب", "📚", 3),
    Category(4, "سيارات ونقل", "🚗", 4),
    Category(5, "تقنية وبرمجة", "💻", 5),
    Category(6, "جمال ولياقة", "💇", 6)
)
