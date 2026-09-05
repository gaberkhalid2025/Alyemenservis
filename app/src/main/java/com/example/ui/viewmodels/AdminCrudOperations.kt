package com.example.ui.viewmodels

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminCrudOperations(private val db: FirebaseFirestore) {

    suspend fun <T : Any> saveEntity(
        collection: String,
        id: String,
        data: T,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            db.collection(collection).document(id).set(data).await()
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun deleteEntity(
        collection: String,
        id: String,
        softDelete: Boolean = true,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            if (softDelete) {
                db.collection(collection).document(id)
                    .update("isDeleted", true, "deletedAt", System.currentTimeMillis())
                    .await()
            } else {
                db.collection(collection).document(id).delete().await()
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun toggleEntityStatus(
        collection: String,
        id: String,
        field: String,
        value: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            db.collection(collection).document(id).update(field, value).await()
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun updateFields(
        collection: String,
        id: String,
        fields: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            db.collection(collection).document(id).update(fields).await()
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }
}
