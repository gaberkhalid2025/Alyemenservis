package com.example.ui.helpers

import com.example.data.*
import com.example.ui.*
import com.example.data.models.*
import com.example.ui.MainViewModel
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingFormFields
import com.google.firebase.firestore.*

/**
 * Helper class responsible for real-time Firestore listeners and synchronization.
 * Guarantees safe listener registration and teardown on ViewModel cleared.
 */
class RealtimeSyncHelper(private val db: FirebaseFirestore) {

    val firestoreListeners = java.util.concurrent.CopyOnWriteArrayList<ListenerRegistration>()

    private fun Query.addSnapshotListenerReg(listener: EventListener<QuerySnapshot>): ListenerRegistration? {
        return try {
            val reg = this.addSnapshotListener(listener)
            firestoreListeners.add(reg)
            reg
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Query listener", e)
            null
        }
    }

    private fun DocumentReference.addSnapshotListenerReg(listener: EventListener<DocumentSnapshot>): ListenerRegistration? {
        return try {
            val reg = this.addSnapshotListener(listener)
            firestoreListeners.add(reg)
            reg
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppErrorLogManager.logFirestoreError("RealtimeSyncHelper", "Failed to add Document listener", e)
            null
        }
    }

    fun clearListeners() {
        try {
            val iterator = firestoreListeners.iterator()
            while (iterator.hasNext()) {
                try {
                    iterator.next().remove()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            firestoreListeners.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupRealtimeFirestoreListeners(appState: com.example.ui.helpers.AppState) {
        // 1. Settings (Document main_settings)
        db.collection("settings").document("main_settings").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                appState._isInitialized.value = true
                return@addSnapshotListenerReg
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    snapshot.toObject(AdminSettingsEntity::class.java)?.let {
                        appState._settings.value = it
                        appState._maxKmRadius.value = it.maxSearchRadiusKm
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                appState._settings.value = AdminSettingsEntity()
            }
            appState._isInitialized.value = true
        }

        // 1b. Booking Form Fields Listener
        db.collection("settings").document("booking_fields").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                try {
                    snapshot.toObject(BookingFormFields::class.java)?.let {
                        appState._bookingFormFields.value = it
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // 1c. Booking Distribution Mode Listener
        db.collection("settings").document("distribution_mode").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                val modeStr = snapshot.getString("mode")
                if (!modeStr.isNullOrEmpty()) {
                    try {
                        appState._distributionMode.value = BookingDistributionMode.valueOf(modeStr)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        // 2. Categories
        db.collection("categories").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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

        // Custom Profile Tabs
        db.collection("custom_profile_tabs").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.toObjects(CustomProfileTabEntity::class.java)
                appState._customProfileTabs.value = fetched.sortedBy { it.displayOrder }
            }
        }

        // 3. Cities
        db.collection("cities").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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

        // 3b. Registered Users count listener
        db.collection("registered_users").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                appState._registeredUsersCount.value = snapshot.size()
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                appState._registeredUsersList.value = list
            }
        }

        // 3c. Internal Wallets Listener
        db.collection("internal_wallets").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                appState._internalWallets.value = snapshot.documents.mapNotNull { it.toObject(InternalWalletEntity::class.java) }
            }
        }

        // 3d. Wallet Transactions Listener
        db.collection("wallet_transactions").addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                appState._walletTransactions.value = snapshot.documents.mapNotNull { it.toObject(WalletTransactionEntity::class.java) }.sortedByDescending { it.timestamp }
            }
        }

        // 4. Banners
        db.collection("banners").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(BannerEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._banners.value = fetched.sortedBy { it.order }
            } else {
                appState._banners.value = emptyList()
            }
        }

        // 5. Providers (Full limit & safe parsing for complete Map & listing coverage)
        db.collection("providers").limit(250).addSnapshotListenerReg { snapshot, error ->
            appState._isProvidersLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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

                val activeList = allList.filter { !it.isDeleted }
                val deletedList = allList.filter { it.isDeleted }

                appState._providers.value = activeList
                appState._deletedProviders.value = deletedList
                // appState._providers updated
            }
        }

        // 6. Pending Providers (Full limit & safe parsing)
        db.collection("pending_providers").limit(200).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val parsed = doc.toObject(PendingProviderEntity::class.java)
                        parsed?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            PendingProviderEntity(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                phone = doc.getString("phone") ?: "",
                                categoryId = doc.getString("categoryId") ?: "",
                                area = doc.getString("area") ?: doc.getString("localArea") ?: "",
                                localNeighborhood = doc.getString("localNeighborhood") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                reason = doc.getString("reason") ?: "",
                                idPhotoBase64 = doc.getString("idPhotoBase64") ?: "",
                                selfiePhotoBase64 = doc.getString("selfiePhotoBase64") ?: "",
                                workPhotosBase64 = (doc.get("workPhotosBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            )
                        } catch (e2: java.lang.Exception) {
                            e2.printStackTrace()
                            null
                        }
                    }
                }
                appState._pendingProviders.value = fetched
            }
        }

        // 7. Bookings (Paginated / limited to 20)
        db.collection("bookings").orderBy("createdAt", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(BookingEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._bookings.value = fetched
            }
        }

        // 8. Notifications (Paginated / limited to 20 with strict validation & deduplication)
        db.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
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

        // 9. Chat Channels (Paginated / limited to 20)
        db.collection("chat_channels").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            appState._isChatChannelsLoading.value = false
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatChannelEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.timestamp }
                appState._chatChannels.value = fetched
            }
        }

        // 11. Reports (Paginated / limited to 20)
        db.collection("reports").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ReportEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._reports.value = fetched
            }
        }

        // 12. Supervisors (Instantly synced)
        db.collection("supervisors").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(SupervisorEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._supervisors.value = fetched
            }
        }

        // 13. Color Palettes (Instantly synced)
        db.collection("color_themes").addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ColorPaletteEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._colorPalettes.value = fetched
            }
        }

        // 14. Calls Log (Paginated / limited to 20)
        db.collection("calls").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CallEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.timestamp }
                appState._callsLog.value = fetched
            }
        }

        // 15. Coupons
        db.collection("coupons").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CouponEntity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._coupons.value = fetched
            }
        }

        // 16. Payment Wallets
        db.collection("payment_wallets").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentWalletEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedBy { it.displayOrder }
                appState._paymentWallets.value = fetched
            }
        }

        // 17. Payments (Paginated / limited to 20)
        db.collection("payments").orderBy("createdAt", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListenerReg
            }
            if (snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it.createdAt }
                appState._payments.value = fetched
            }
        }

        // 18. Stores (Full limit & safe parsing for Maps & directory coverage)
        db.collection("stores").limit(250).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(StoreEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: act
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
                                isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: true,
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
                appState._stores.value = fetched.filter { !it.isDeleted }
            }
        }

        // 19. Products (Full limit & safe parsing)
        db.collection("products").limit(250).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(ProductEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            obj.copy(id = doc.id, isDeleted = isDel)
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._products.value = fetched.filter { !it.isDeleted }
            }
        }

        // 20. Properties (Full limit & safe parsing for Maps & real estate coverage)
        db.collection("properties").limit(250).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(PropertyEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val appr = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: act
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
                                isApproved = doc.getBoolean("isApproved") ?: doc.getBoolean("approved") ?: true,
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
                appState._properties.value = fetched.filter { !it.isDeleted }
            }
        }

        // 20.1 Jobs (Paginated / limited to 20)
        db.collection("jobs").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(JobEntity::class.java)
                        if (obj != null) {
                            val isDel = doc.getBoolean("isDeleted") == true || doc.getBoolean("deleted") == true
                            val act = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true
                            val pin = doc.getBoolean("isPinned") == true || doc.getBoolean("pinned") == true
                            obj.copy(id = doc.id, isDeleted = isDel, isActive = act, isPinned = pin)
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._jobs.value = fetched
            }
        }

        // 20.2 Job Applications (Paginated / limited to 20)
        db.collection("job_applications").limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(JobApplicationEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._jobApplications.value = fetched
            }
        }

        // 21. Ratings (Paginated / limited to 20)
        db.collection("ratings").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RatingEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._ratings.value = fetched
            }
        }

        // 22. Orders (Paginated / limited to 20)
        db.collection("orders").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(OrderEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._orders.value = fetched
            }
        }

        // 22.1 Offers & Instant Pricing (Real-time synchronization)
        db.collection("offers").limit(50).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        val obj = doc.toObject(Offer::class.java)
                        obj?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                appState._offers.value = fetched
            }
        }

        // 23. Activity Logs (Paginated / limited to 20)
        db.collection("activity_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ActivityLogEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }
                appState._activityLogs.value = fetched
            }
        }

        // 24. Instant Requests (Paginated / limited to 20)
        db.collection("instant_requests").orderBy("createdAt", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(InstantRequestEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                // Auto-expire requests past expiresAt
                val now = System.currentTimeMillis()
                val processed = fetched.map { req ->
                    if ((req.status == "WAITING_FOR_OFFERS" || req.status == "REVIEWING_OFFERS") && now > req.expiresAt) {
                        req.copy(status = "EXPIRED")
                    } else req
                }
                appState._instantRequests.value = processed
            }
        }

        // 25. Request Offers (Paginated / limited to 20)
        db.collection("request_offers").orderBy("createdAt", Query.Direction.DESCENDING).limit(20).addSnapshotListenerReg { snapshot, error ->
            if (error == null && snapshot != null) {
                val fetched = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RequestOfferEntity::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                appState._requestOffers.value = fetched
            }
        }
    }
}
