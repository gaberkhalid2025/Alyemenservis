package com.example.ui.helpers

import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal suspend fun searchProvidersHelper(
    db: FirebaseFirestore,
    query: String,
    limit: Long
): List<ProviderEntity> = try {
    val q = query.trim()
    if (q.isEmpty()) emptyList()
    else {
        val results = db.collection("providers")
            .whereGreaterThanOrEqualTo("name", q)
            .whereLessThanOrEqualTo("name", q + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ProviderEntity::class.java)?.copy(id = it.id) }
            .filter { !it.isDeleted && !it.isBlocked }

        if (results.isEmpty()) {
            db.collection("providers")
                .whereEqualTo("phone", q)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ProviderEntity::class.java)?.copy(id = it.id) }
                .filter { !it.isDeleted && !it.isBlocked }
        } else results
    }
} catch (_: Exception) { emptyList() }

internal suspend fun searchStoresHelper(
    db: FirebaseFirestore,
    query: String,
    limit: Long
): List<StoreEntity> = try {
    val q = query.trim()
    if (q.isEmpty()) emptyList()
    else {
        db.collection("stores")
            .whereGreaterThanOrEqualTo("name", q)
            .whereLessThanOrEqualTo("name", q + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(StoreEntity::class.java)?.copy(id = it.id) }
            .filter { !it.isDeleted && it.isApproved && it.isActive && !it.isBlocked }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun searchPropertiesHelper(
    db: FirebaseFirestore,
    query: String,
    limit: Long
): List<PropertyEntity> = try {
    val q = query.trim()
    if (q.isEmpty()) emptyList()
    else {
        db.collection("properties")
            .whereGreaterThanOrEqualTo("title", q)
            .whereLessThanOrEqualTo("title", q + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PropertyEntity::class.java)?.copy(id = it.id) }
            .filter { !it.isDeleted && it.isApproved && it.isActive && !it.isBlocked }
    }
} catch (_: Exception) { emptyList() }

internal suspend fun searchJobsHelper(
    db: FirebaseFirestore,
    query: String,
    limit: Long
): List<JobEntity> = try {
    val q = query.trim()
    if (q.isEmpty()) emptyList()
    else {
        db.collection("jobs")
            .whereGreaterThanOrEqualTo("title", q)
            .whereLessThanOrEqualTo("title", q + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(JobEntity::class.java)?.copy(id = it.id) }
            .filter { !it.isDeleted && (it.isApproved || it.isActive) }
    }
} catch (_: Exception) { emptyList() }
