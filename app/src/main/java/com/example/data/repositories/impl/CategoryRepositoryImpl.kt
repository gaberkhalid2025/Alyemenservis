package com.example.data.repositories.impl

import com.example.data.CategoryEntity
import com.example.data.repositories.contracts.ICategoryRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CategoryRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ICategoryRepository {

    override fun observeCategories(): Flow<AppResult<List<CategoryEntity>>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("categories")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(AppError.DatabaseError(error.localizedMessage ?: "فشل مراقبة الفئات")))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val fetched = snapshot.documents.mapNotNull { doc ->
                            try {
                                val obj = doc.toObject(CategoryEntity::class.java)
                                if (obj != null) {
                                    if (obj.id.isEmpty()) obj.copy(id = doc.id) else obj
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }.distinctBy { it.id }.sortedWith(
                            compareByDescending<CategoryEntity> { it.isPinned }
                                .thenBy { it.order }
                        )
                        trySend(Result.success(fetched))
                    }
                }
        } catch (e: Exception) {
            trySend(Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل مراقبة الفئات")))
        }

        awaitClose {
            registration?.remove()
        }
    }
}
