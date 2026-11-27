package com.example.ui.helpers

import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

internal fun RealtimeSyncHelper.registerCatalogAndOpsRealtimeListeners(
    db: FirebaseFirestore,
    appState: AppState
) {
    // 5. Stores (Realtime limit with pagination support) (Listener 5)
    registerQueryListener(
        db.collection("stores").limit(RealtimeSyncHelper.REALTIME_QUERY_LIMIT)
    ) { snapshot, error ->
        if (error == null && snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    val obj = doc.toObject(StoreEntity::class.java)
                    if (obj != null) {
                        val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                        val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                        val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: false
                        val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                        val vip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true
                        val rec = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true
                        val ver = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true
                        val blk = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                        val chatDis = doc.getBoolean("isChatDisabled") == true || doc.getBoolean("chatDisabled") == true
                        obj.copy(
                            id = doc.id,
                            isDeleted = isDel,
                            isActive = act,
                            isApproved = appr,
                            isPinned = pin,
                            isVip = vip,
                            isRecommended = rec,
                            isVerified = ver,
                            isBlocked = blk,
                            isChatDisabled = chatDis
                        )
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        StoreEntity(
                            id = doc.id,
                            sectionId = doc.getString("sectionId") ?: "stores",
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            phone = doc.getString("phone") ?: "",
                            categoryId = doc.getString("categoryId") ?: "",
                            cityId = doc.getString("cityId") ?: "",
                            localNeighborhood = doc.getString("localNeighborhood") ?: "",
                            coverImage = doc.getString("coverImage") ?: "",
                            logoImage = doc.getString("logoImage") ?: "",
                            rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                            numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                            isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
                            isPinned = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true,
                            latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                            longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                            isDeleted = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true,
                            isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: false,
                            isVip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true,
                            isVerified = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true,
                            isRecommended = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true,
                            isBlocked = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true,
                            medicalLicenseNo = doc.getString("medicalLicenseNo") ?: "",
                            commercialRegisterNo = doc.getString("commercialRegisterNo") ?: "",
                            providerType = doc.getString("providerType") ?: ""
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        null
                    }
                }
            }
            appState._stores.value = fetched.filter { !it.isDeleted && it.isApproved && it.isActive && !it.isBlocked }
        }
    }

    // 6. Properties (Realtime limit with pagination support) (Listener 6)
    registerQueryListener(
        db.collection("properties").limit(RealtimeSyncHelper.REALTIME_QUERY_LIMIT)
    ) { snapshot, error ->
        if (error == null && snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    val obj = doc.toObject(PropertyEntity::class.java)
                    if (obj != null) {
                        val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                        val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                        val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: false
                        val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                        val vip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true
                        val rec = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true
                        val ver = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true
                        val blk = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                        obj.copy(
                            id = doc.id,
                            isDeleted = isDel,
                            isActive = act,
                            isApproved = appr,
                            isPinned = pin,
                            isVip = vip,
                            isRecommended = rec,
                            isVerified = ver,
                            isBlocked = blk
                        )
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        PropertyEntity(
                            id = doc.id,
                            sectionId = doc.getString("sectionId") ?: "properties",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            price = doc.getDouble("price") ?: doc.getLong("price")?.toDouble() ?: 0.0,
                            currency = doc.getString("currency") ?: "YER",
                            type = doc.getString("type") ?: "rent",
                            propertyType = doc.getString("propertyType") ?: "apartment",
                            phone = doc.getString("phone") ?: "",
                            cityId = doc.getString("cityId") ?: "",
                            localNeighborhood = doc.getString("localNeighborhood") ?: "",
                            rating = (doc.getDouble("rating") ?: doc.getLong("rating")?.toDouble() ?: 5.0).toFloat(),
                            numReviews = (doc.getLong("numReviews") ?: 0L).toInt(),
                            isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
                            isPinned = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true,
                            latitude = doc.getDouble("latitude") ?: doc.getString("latitude")?.toDoubleOrNull() ?: 15.3694,
                            longitude = doc.getDouble("longitude") ?: doc.getString("longitude")?.toDoubleOrNull() ?: 44.1910,
                            isDeleted = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true,
                            isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: false,
                            isVip = doc.getBoolean("isVip") == true || doc.getBoolean("vip") == true,
                            isVerified = doc.getBoolean("isVerified") == true || doc.getBoolean("verified") == true,
                            isRecommended = doc.getBoolean("isRecommended") == true || doc.getBoolean("recommended") == true,
                            isBlocked = doc.getBoolean("isBlocked") == true || doc.getBoolean("blocked") == true
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        null
                    }
                }
            }
            appState._properties.value = fetched.filter { !it.isDeleted && it.isApproved && it.isActive && !it.isBlocked }
        }
    }

    // 7. Notifications (Realtime limit with pagination support) (Listener 7)
    registerQueryListener(
        db.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(RealtimeSyncHelper.REALTIME_QUERY_LIMIT)
    ) { snapshot, error ->
        if (error != null) {
            error.printStackTrace()
            return@registerQueryListener
        }
        if (snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    val obj = doc.toObject(NotificationEntity::class.java)
                    val finalObj = if (obj != null && obj.id.isEmpty()) {
                        obj.copy(id = doc.id)
                    } else {
                        obj
                    }
                    if (finalObj != null && finalObj.isValid()) finalObj else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}" } }.sortedByDescending { it.timestamp }
            appState._notifications.value = fetched
        }
    }

    // 8. Chat Channels (Realtime limit with pagination support) (Listener 8)
    registerQueryListener(
        db.collection("chat_channels").orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(RealtimeSyncHelper.REALTIME_QUERY_LIMIT)
    ) { snapshot, error ->
        appState._isChatChannelsLoading.value = false
        if (error != null) {
            error.printStackTrace()
            return@registerQueryListener
        }
        if (snapshot != null) {
            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ChatChannelEntity::class.java)?.copy(id = doc.id)?.toCanonicalChannel()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.sortedByDescending { it.lastMessageTime.coerceAtLeast(it.timestamp) }
            appState._chatChannels.value = fetched
        }
    }

    // 9. Pending Providers (Realtime Listener 9 - Staff gets active review queue, user gets own requests)
    val fbUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    val isStaffAuth = fbUser != null && !fbUser.isAnonymous
    val userPhone = fbUser?.phoneNumber?.takeIf { it.isNotBlank() }?.let {
        AppPreferenceHelper.normalizePhoneNumber(it)
    }.orEmpty()

    val pendingQuery = when {
        isStaffAuth -> db.collection("pending_providers").limit(40)
        userPhone.isNotBlank() -> db.collection("pending_providers").whereEqualTo("phone", userPhone).limit(5)
        else -> null
    }
    if (pendingQuery != null) {
        registerQueryListener(pendingQuery) { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val parsed = doc.toObject(PendingProviderEntity::class.java)
                        parsed?.copy(id = doc.id)
                    } catch (e: Exception) {
                        try {
                            PendingProviderEntity(
                                id = doc.id,
                                name = doc.getString("name") ?: doc.getString("ownerName") ?: "",
                                phone = doc.getString("phone") ?: "",
                                categoryId = doc.getString("categoryId") ?: "",
                                area = doc.getString("area") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                profession = doc.getString("profession") ?: "",
                                customCategoryName = doc.getString("customCategoryName") ?: "",
                                passwordHash = doc.getString("passwordHash") ?: doc.getString("password") ?: ""
                            )
                        } catch (ex: Exception) {
                            null
                        }
                    }
                }
                appState._pendingProviders.value = fetched
            }
        }
    }

    // 10. Registered Users (Realtime Listener 10 - Staff only to prevent PERMISSION_DENIED on guest startup)
    if (isStaffAuth) {
        registerQueryListener(db.collection("registered_users").limit(40)) { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                appState._registeredUsersList.value = fetched
                appState._registeredUsersCount.value = fetched.size
            }
        }
    }

    // 11. Fallback Sync from join_requests collection
    val joinReqQuery = when {
        isStaffAuth -> db.collection("join_requests").whereEqualTo("status", "PENDING").limit(40)
        userPhone.isNotBlank() -> db.collection("join_requests").whereEqualTo("phone", userPhone).limit(5)
        else -> null
    }
    if (joinReqQuery != null) {
        registerQueryListener(joinReqQuery) { snapshot, error ->
            if (error == null && snapshot != null) {
                val currentPending = appState._pendingProviders.value.associateBy { it.id }.toMutableMap()
                var updated = false
                snapshot.documents.forEach { doc ->
                    val docId = doc.id
                    if (!currentPending.containsKey(docId)) {
                        val type = doc.getString("type") ?: "CLIENT"
                        val phone = doc.getString("phone") ?: ""
                        val name = doc.getString("fullName") ?: doc.getString("name") ?: doc.getString("businessName") ?: "طلب انضمام جديد"
                        val city = doc.getString("city") ?: doc.getString("area") ?: ""
                        val entity = PendingProviderEntity(
                            id = docId,
                            name = name,
                            phone = phone,
                            categoryId = type,
                            area = city,
                            status = "PENDING",
                            profession = type,
                            providerType = type
                        )
                        currentPending[docId] = entity
                        updated = true
                    }
                }
                if (updated) {
                    appState._pendingProviders.value = currentPending.values.toList()
                }
            }
        }
    }
}
