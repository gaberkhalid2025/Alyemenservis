package com.example.data

import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Keep
data class Review(
    val id: String = java.util.UUID.randomUUID().toString(),
    val shopId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object DataManager {
    private val firestore = FirebaseFirestore.getInstance()

    fun submitReview(
        shopId: String, 
        review: Review, 
        onSuccess: () -> Unit = {}, 
        onFailure: (Exception) -> Unit = {}
    ) {
        val reviewDocument = review.copy(shopId = shopId)
        firestore.collection("reviews")
            .document(reviewDocument.id)
            .set(reviewDocument)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getReviews(shopId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection("reviews")
            .whereEqualTo("shopId", shopId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fail gracefully
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reviewsList = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(reviewsList)
                }
            }
        awaitClose { listener.remove() }
    }
}
