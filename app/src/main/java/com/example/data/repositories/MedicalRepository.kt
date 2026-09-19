package com.example.data.repositories

import com.example.data.LocalAppCacheManager
import com.example.domain.entities.DoctorItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class MedicalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheManager: LocalAppCacheManager
) {
    fun getDoctors(ownerId: String): Flow<List<DoctorItem>> = callbackFlow {
        val listener = firestore.collection("doctors")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    DoctorItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        specialty = doc.getString("specialty") ?: "",
                        workingHours = doc.getString("workingHours") ?: ""
                    )
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addDoctor(ownerId: String, doctor: DoctorItem): Result<String> {
        return try {
            val data = mapOf(
                "id" to doctor.id,
                "ownerId" to ownerId,
                "name" to doctor.name,
                "specialty" to doctor.specialty,
                "workingHours" to doctor.workingHours,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("doctors").document(doctor.id).set(data).await()
            Result.success(doctor.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDoctor(id: String): Result<Unit> {
        return try {
            firestore.collection("doctors").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
