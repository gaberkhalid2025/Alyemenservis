package com.example.ui.helpers

import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

internal fun RealtimeSyncHelper.registerCoreRealtimeListeners(
    db: FirebaseFirestore,
    appState: AppState
) {
    // 1. Settings (Document main_settings)
    registerDocListener(db.collection("settings").document("main_settings")) { snapshot, error ->
        if (error != null) {
            error.printStackTrace()
            return@registerDocListener
        }
        if (snapshot != null && snapshot.exists()) {
            try {
                // Security migration: Cleanup legacy adminPassword from Firestore ONLY for signed-in staff/admin
                val fbUser = FirebaseAuth.getInstance().currentUser
                if (fbUser != null && !fbUser.isAnonymous && snapshot.contains("adminPassword")) {
                    snapshot.reference.update("adminPassword", FieldValue.delete())
                        .addOnFailureListener { e ->
                            android.util.Log.w("RealtimeSyncHelper", "Skipped adminPassword cleanup: ${e.message}")
                        }
                }
                snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                    appState._settings.value = it.copy(ownerPassword = "")
                    appState._maxKmRadius.value = it.maxSearchRadiusKm
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            appState._settings.value = AdminSettingsEntity()
        }
    }

    // 1.1 Navigation Settings (Sync download URL without modifying AdminSettingsEntity)
    registerDocListener(db.collection("settings").document("navigation_settings")) { snapshot, error ->
        if (error != null) {
            return@registerDocListener
        }
        if (snapshot != null && snapshot.exists()) {
            try {
                val url = snapshot.getString("valhallaTilesDownloadUrl")
                if (!url.isNullOrBlank()) {
                    val ctx = com.example.MyApplication.instance
                    if (ctx != null) {
                        AppPreferenceHelper().setValhallaTilesUrl(ctx, url)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("RealtimeSyncHelper", "navigation_settings sync warning: ${e.message}")
            }
        }
    }

    // 2. Categories (Listener 2)
    registerQueryListener(db.collection("categories")) { snapshot, error ->
        if (error != null) {
            error.printStackTrace()
            return@registerQueryListener
        }
        if (snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    val obj = doc.toObject(CategoryEntity::class.java)
                    if (obj != null && obj.id.isEmpty()) {
                        obj.copy(id = doc.id)
                    } else {
                        obj
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.distinctBy { it.id }.sortedWith(compareByDescending<CategoryEntity> { it.isPinned }.thenBy { it.order })
            appState._categories.value = fetched
        }
    }

    // 3. Cities (Listener 3)
    registerQueryListener(db.collection("cities")) { snapshot, error ->
        if (error != null) {
            error.printStackTrace()
            return@registerQueryListener
        }
        if (snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(CityEntity::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            appState._cities.value = fetched
        }
    }

    // 4. Providers (Realtime limit with pagination support) (Listener 4)
    registerQueryListener(
        db.collection("providers").limit(RealtimeSyncHelper.REALTIME_QUERY_LIMIT)
    ) { snapshot, error ->
        appState._isProvidersLoading.value = false
        if (error != null) {
            error.printStackTrace()
            return@registerQueryListener
        }
        if (snapshot != null) {
            val allList = snapshot.documents.mapNotNull { doc ->
                try {
                    val parsed = doc.toObject(ProviderEntity::class.java)
                    parsed?.copy(id = doc.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        ProviderEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            phone = doc.getString("phone") ?: "",
                            categoryId = doc.getString("categoryId") ?: "",
                            area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                            isVip = doc.getBoolean("isVip") ?: doc.getBoolean("vip") ?: false,
                            subscriptionStatus = doc.getString("subscriptionStatus") ?: "APPROVED",
                            isAvailable = doc.getBoolean("isAvailable") ?: doc.getBoolean("available") ?: true,
                            cityId = doc.getString("cityId") ?: "",
                            localNeighborhood = doc.getString("localNeighborhood") ?: "",
                            rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                            points = (doc.getLong("points") ?: 0L).toInt(),
                            isVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: true,
                            isRecommended = doc.getBoolean("isRecommended") ?: doc.getBoolean("recommended") ?: true,
                            numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                            coverImage = doc.getString("coverImage") ?: "",
                            profileImage = doc.getString("profileImage") ?: "",
                            previewPrice = doc.getDouble("previewPrice") ?: 1500.0,
                            latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                            longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                            customCategoryName = doc.getString("customCategoryName") ?: "",
                            profession = doc.getString("profession") ?: "",
                            specialization = doc.getString("specialization") ?: "",
                            isBlocked = doc.getBoolean("isBlocked") ?: doc.getBoolean("blocked") ?: false,
                            isChatDisabled = doc.getBoolean("isChatDisabled") ?: doc.getBoolean("chatDisabled") ?: false,
                            isDeleted = doc.getBoolean("isDeleted") ?: doc.getBoolean("deleted") ?: false,
                            providerType = doc.getString("providerType") ?: ""
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        null
                    }
                }
            }.filter { !it.name.contains("ماهر") && it.id != "p_maher" }

            val activeList = allList.filter {
                !it.isDeleted &&
                    !it.isBlocked &&
                    (it.subscriptionStatus == "APPROVED" || it.subscriptionStatus == "ACTIVE" || it.subscriptionStatus.isBlank())
            }
            val deletedList = allList.filter { it.isDeleted }

            appState._providers.value = activeList
            appState._deletedProviders.value = deletedList
        }
    }
}
