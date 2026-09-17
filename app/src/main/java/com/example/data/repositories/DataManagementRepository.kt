package com.example.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

interface IDataManagementRepository {
    suspend fun countDocuments(collectionName: String, sectionFilter: String? = null): Int
    suspend fun wipeCollectionInBatches(
        collectionName: String,
        sectionFilter: String? = null,
        performedBy: String = "ADMIN",
        onProgress: (deleted: Int, total: Int) -> Unit
    ): Result<Int>
}

class DataManagementRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IDataManagementRepository {

    override suspend fun countDocuments(collectionName: String, sectionFilter: String?): Int {
        return try {
            val targetCol = if (collectionName == "restaurants" || collectionName == "medical") "stores" else collectionName
            val snapshot = db.collection(targetCol).get().await()
            filterDocuments(snapshot, collectionName, sectionFilter).size
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun wipeCollectionInBatches(
        collectionName: String,
        sectionFilter: String?,
        performedBy: String,
        onProgress: (deleted: Int, total: Int) -> Unit
    ): Result<Int> {
        return try {
            val targetCol = if (collectionName == "restaurants" || collectionName == "medical") "stores" else collectionName
            val snapshot = db.collection(targetCol).get().await()
            val docsToDelete = filterDocuments(snapshot, collectionName, sectionFilter)
            val total = docsToDelete.size
            var deletedCount = 0

            if (total == 0) {
                return Result.success(0)
            }

            // Firestore batch limit is 500 documents per batch
            val batches = docsToDelete.chunked(450)
            for (chunk in batches) {
                val batch = db.batch()
                for (doc in chunk) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                deletedCount += chunk.size
                onProgress(deletedCount, total)
            }

            // Write Audit Log
            try {
                val auditLog = mapOf(
                    "logId" to "audit_wipe_${System.currentTimeMillis()}",
                    "collection" to collectionName,
                    "targetFirestoreCollection" to targetCol,
                    "sectionFilter" to (sectionFilter ?: ""),
                    "deletedCount" to deletedCount,
                    "performedBy" to performedBy,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "SUCCESS"
                )
                db.collection("audit_logs").document("log_${System.currentTimeMillis()}").set(auditLog).await()
            } catch (_: Exception) {
                // Ignore audit log failure if main deletion succeeded
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun filterDocuments(snapshot: QuerySnapshot, collectionName: String, sectionFilter: String?): List<com.google.firebase.firestore.DocumentSnapshot> {
        return when {
            collectionName == "restaurants" -> snapshot.documents.filter { (it.getString("sectionId") ?: "") == "restaurants" }
            collectionName == "medical" -> snapshot.documents.filter { (it.getString("sectionId") ?: "") == "medical" }
            sectionFilter != null && sectionFilter.isNotBlank() -> snapshot.documents.filter { (it.getString("sectionId") ?: "") == sectionFilter }
            else -> snapshot.documents
        }
    }
}
