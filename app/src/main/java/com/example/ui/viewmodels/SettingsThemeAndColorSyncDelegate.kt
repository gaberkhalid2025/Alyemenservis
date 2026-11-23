package com.example.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.data.ColorPaletteEntity
import com.example.data.ColorSchemeEntity
import com.example.data.ColorSyncStatus
import com.example.data.PersonalColors
import com.example.data.SyncLogEntity
import com.example.data.UserColorsEntity
import com.example.utils.ColorSyncManager
import com.example.utils.DateFormatter
import com.example.utils.SecureHasher
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

internal class SettingsThemeAndColorSyncDelegate(private val vm: SettingsViewModel) {

    private var colorSchemeListener: ListenerRegistration? = null
    private var userColorsListener: ListenerRegistration? = null
    private var cardSettingsListener: ListenerRegistration? = null

    fun updateAppLanguage(lang: String) {
        val updated = vm._settings.value.copy(appLanguage = lang)
        vm._settings.value = updated
        try {
            vm.db.collection("settings").document("main_settings").update("appLanguage", lang)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error: ", e)
        }
    }

    fun updateCardSettings(settings: SettingsViewModel.CardSettings) {
        vm._cardSettings.value = settings
        try {
            vm.db.collection("settings").document("card_settings").set(settings)
        } catch (_: Exception) {}
    }

    fun loadCardSettings() {
        try {
            cardSettingsListener?.remove()
            cardSettingsListener = vm.db.collection("settings").document("card_settings")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val settings = snapshot.toObject(SettingsViewModel.CardSettings::class.java)
                        if (settings != null) {
                            vm._cardSettings.value = settings
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error loading card settings", e)
        }
    }

    fun updateTheme(themeId: String) {
        val updated = vm._settings.value.copy(activeThemeId = themeId)
        vm._settings.value = updated
        vm.db.collection("settings").document("main_settings").get().addOnSuccessListener { snapshot ->
            val s = snapshot.toObject(AdminSettingsEntity::class.java) ?: AdminSettingsEntity()
            vm.db.collection("settings").document("main_settings").set(s.copy(activeThemeId = themeId), SetOptions.merge())
        }.addOnFailureListener {
            vm.db.collection("settings").document("main_settings").set(updated, SetOptions.merge())
        }
        vm.mainViewModel.triggerNotification("🎨 تم تغيير مظهر التطبيق ومزامنته سحابياً إلى $themeId")
    }

    fun saveCustomSettingsState(newSettings: AdminSettingsEntity) {
        vm.runSafeFirestoreCallback(
            operation = { onSuccess, onFailure ->
                vm.db.collection("settings").document("main_settings").set(newSettings, SetOptions.merge())
                    .addOnSuccessListener {
                        vm._settings.value = newSettings
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        vm._settings.value = newSettings
                        onFailure(e)
                    }
            },
            onSuccess = { vm.mainViewModel.triggerNotification("✅ تم حفظ ومزامنة كافة إعدادات التطبيق والدفع سحابياً بنجاح!") },
            onError = { vm.mainViewModel.triggerNotification("⚠️ تم حفظ الإعدادات محلياً، وسيتم المزامنة سحابياً فور توفر الاتصال") },
            errorMessage = "فشل حفظ الإعدادات"
        )
    }

    fun updatePaymentEnabled(enabled: Boolean) {
        val current = vm._settings.value
        val updated = current.copy(isPaymentEnabled = enabled)
        saveCustomSettingsState(updated)
    }

    fun updateBackdoorSettings(
        appName: String,
        welcomeMsg: String,
        footerMsg: String,
        themeId: String,
        supportPhone: String,
        supportEmail: String,
        supportWhatsapp: String,
        isMaintenance: Boolean,
        hiddenFooter: Boolean,
        botHidden: Boolean,
        botSize: Int,
        chatHidden: Boolean,
        chatSize: Int,
        radiusKm: Int,
        isSpeech: Boolean,
        isDataSaver: Boolean,
        imgQuality: Int,
        bookingTerms: String,
        bookingLabelName: String,
        bookingLabelPhone: String,
        bookingLabelArea: String,
        bookingLabelService: String,
        adminUsername: String,
        adminPassword: String,
        customPrimaryHex: String,
        customSecondaryHex: String,
        customBackgroundHex: String,
        customSurfaceHex: String
    ) {
        val updated = vm._settings.value.copy(
            appName = appName,
            welcomeMessage = welcomeMsg,
            footerMessage = footerMsg,
            activeThemeId = themeId,
            isMaintenanceActive = isMaintenance,
            hidePromoFooter = hiddenFooter,
            assistantHidden = botHidden,
            assistantSize = botSize,
            chatHidden = chatHidden,
            chatSize = chatSize,
            maxSearchRadiusKm = radiusKm,
            isSpeechSearchEnabled = isSpeech,
            supportPhone = supportPhone,
            supportEmail = supportEmail,
            supportWhatsapp = supportWhatsapp,
            bookingTerms = bookingTerms,
            bookingLabelName = bookingLabelName,
            bookingLabelPhone = bookingLabelPhone,
            bookingLabelArea = bookingLabelArea,
            bookingLabelService = bookingLabelService,
            adminUsername = adminUsername,
            ownerPassword = "",
            customPrimaryHex = customPrimaryHex,
            customSecondaryHex = customSecondaryHex,
            customBackgroundHex = customBackgroundHex,
            customSurfaceHex = customSurfaceHex
        )
        vm._settings.value = updated
        if (adminPassword.isNotBlank()) {
            val hashedAdmin = if (adminPassword.contains(":")) adminPassword else SecureHasher.hashPassword(adminPassword.trim())
            val credUpdates = mapOf(
                "adminUsername" to adminUsername.trim(),
                "adminPasswordHash" to hashedAdmin
            )
            vm.db.collection("settings").document("main_settings").set(credUpdates, SetOptions.merge())
            vm.db.collection("admin_secrets").document("credentials").set(credUpdates, SetOptions.merge())
        }
        vm.db.collection("settings").document("main_settings").set(updated, SetOptions.merge())
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("💾 تم حفظ ومزامنة إعدادات الإدارة العليا سحابياً بنجاح!")
            }
            .addOnFailureListener { e ->
                Log.e("SettingsViewModel", "Failed to sync backdoor settings", e)
                vm.mainViewModel.triggerNotification("⚠️ تم حفظ الإعدادات محلياً، وسيتم المزامنة سحابياً فور توفر الاتصال")
            }
    }

    fun updateAdminSettings(newSettings: AdminSettingsEntity) {
        val sanitized = newSettings.copy(ownerPassword = "")
        vm._settings.value = sanitized
        vm.db.collection("settings").document("main_settings").set(sanitized, SetOptions.merge())
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("👑 تم تحديث ومزامنة إعدادات المنصة سحابياً بنجاح!")
            }
            .addOnFailureListener { e ->
                Log.e("SettingsViewModel", "Failed to sync admin settings", e)
                vm.mainViewModel.triggerNotification("⚠️ تم حفظ الإعدادات محلياً، تعذر المزامنة السحابية مؤقتاً")
            }
    }

    fun initColorSync(context: Context) {
        val localScheme = ColorSyncManager.getLocalColorScheme(context)
        vm._colorScheme.value = localScheme

        val localPersonal = ColorSyncManager.getLocalPersonalColors(context)
        vm._personalColors.value = localPersonal

        vm._colorSyncLogs.value = ColorSyncManager.getSyncLogs(context)

        vm._colorSyncStatus.value = ColorSyncStatus.SYNCING
        colorSchemeListener?.remove()
        colorSchemeListener = vm.db.collection("app_settings").document("color_scheme")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val cloudScheme = snapshot.toObject(ColorSchemeEntity::class.java)
                        if (cloudScheme != null) {
                            val currentLocal = vm._colorScheme.value
                            if (cloudScheme.version > currentLocal.version) {
                                val cloudSerialized = ColorSyncManager.serializeColorScheme(cloudScheme)
                                val localSerialized = ColorSyncManager.serializeColorScheme(currentLocal)

                                if (cloudSerialized != localSerialized) {
                                    vm._pendingConflictScheme.value = cloudScheme
                                    vm._colorSyncStatus.value = ColorSyncStatus.CONFLICT
                                    addNewSyncLog(
                                        context,
                                        "colors",
                                        "conflict",
                                        listOf("تعارض الإصدارات: محلي v${currentLocal.version} وسحابي v${cloudScheme.version}"),
                                        currentLocal.version,
                                        cloudScheme.version
                                    )
                                } else {
                                    ColorSyncManager.saveLocalColorScheme(context, cloudScheme)
                                    vm._colorScheme.value = cloudScheme
                                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                                }
                            } else if (cloudScheme.version < currentLocal.version) {
                                vm.db.collection("app_settings").document("color_scheme").set(currentLocal)
                                    .addOnSuccessListener {
                                        vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                                        addNewSyncLog(
                                            context,
                                            "colors",
                                            "success",
                                            listOf("مزامنة تصاعدية: تحديث السحابة للإصدار v${currentLocal.version}"),
                                            cloudScheme.version,
                                            currentLocal.version
                                        )
                                    }
                            } else {
                                val cloudSerialized = ColorSyncManager.serializeColorScheme(cloudScheme)
                                val localSerialized = ColorSyncManager.serializeColorScheme(currentLocal)
                                if (cloudSerialized != localSerialized) {
                                    ColorSyncManager.saveLocalColorScheme(context, cloudScheme)
                                    vm._colorScheme.value = cloudScheme
                                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                                    addNewSyncLog(
                                        context,
                                        "colors",
                                        "success",
                                        listOf("مزامنة تلقائية: تم توحيد محتوى الألوان المتطابقة الإصدار"),
                                        currentLocal.version,
                                        cloudScheme.version
                                    )
                                } else {
                                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                    }
                } else {
                    val defaultScheme = ColorSchemeEntity(version = 1, lastUpdated = getCurrentTimestampString())
                    vm.db.collection("app_settings").document("color_scheme").set(defaultScheme)
                        .addOnSuccessListener {
                            vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                            vm._colorScheme.value = defaultScheme
                            ColorSyncManager.saveLocalColorScheme(context, defaultScheme)
                            addNewSyncLog(context, "colors", "success", listOf("تهيئة أولية لنظام الألوان السحابي"), 0, 1)
                        }
                }
            }
        colorSchemeListener?.let { vm.firestoreListeners.add(it) }

        vm.viewModelScope.launch {
            vm.mainViewModel.authViewModel._currentUserId.collect { userId ->
                if (userId != "guest" && userId.isNotEmpty()) {
                    userColorsListener?.remove()
                    userColorsListener = vm.db.collection("users").document(userId)
                        .addSnapshotListener { snapshot, error ->
                            if (error == null && snapshot != null && snapshot.exists()) {
                                try {
                                    val cloudPersonal = snapshot.toObject(UserColorsEntity::class.java)
                                    if (cloudPersonal != null) {
                                        val localPersonalColors = vm._personalColors.value
                                        if (cloudPersonal.colorsLastSynced != localPersonalColors.colorsLastSynced) {
                                            ColorSyncManager.saveLocalPersonalColors(context, cloudPersonal)
                                            vm._personalColors.value = cloudPersonal
                                            addNewSyncLog(
                                                context,
                                                "personal",
                                                "success",
                                                listOf("تحديث الألوان الشخصية للمستخدم من السحابة"),
                                                0,
                                                0
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    userColorsListener?.let { vm.firestoreListeners.add(it) }
                }
            }
        }
    }

    fun updateCloudColorScheme(context: Context, newScheme: ColorSchemeEntity) {
        vm._colorSyncStatus.value = ColorSyncStatus.SYNCING
        val oldVer = vm._colorScheme.value.version
        vm.db.collection("app_settings").document("color_scheme").set(newScheme)
            .addOnSuccessListener {
                ColorSyncManager.saveLocalColorScheme(context, newScheme)
                vm._colorScheme.value = newScheme
                vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                addNewSyncLog(
                    context,
                    "colors",
                    "success",
                    listOf("تحديث مظهر الألوان العام بنجاح وزيادة الإصدار إلى v${newScheme.version}"),
                    oldVer,
                    newScheme.version
                )
                vm.mainViewModel.triggerNotification("🎨 تم تحديث ومزامنة ألوان المظهر العام بنجاح!")
            }
            .addOnFailureListener { err ->
                vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                addNewSyncLog(
                    context,
                    "colors",
                    "failed",
                    listOf("فشل تحديث الألوان السحابية: ${err.localizedMessage}"),
                    oldVer,
                    newScheme.version
                )
                vm.mainViewModel.triggerNotification("❌ فشل تحديث المظهر السحابي")
            }
    }

    fun updatePersonalColors(context: Context, personal: PersonalColors) {
        val userId = vm.mainViewModel.authViewModel._currentUserId.value
        val nowStr = getCurrentTimestampString()
        val newPersonalEntity = UserColorsEntity(personalColors = personal, colorsLastSynced = nowStr)

        ColorSyncManager.saveLocalPersonalColors(context, newPersonalEntity)
        vm._personalColors.value = newPersonalEntity

        addNewSyncLog(
            context,
            "personal",
            "success",
            listOf("تخصيص ألوان المستخدم الشخصية محلياً: ${personal.favorite}"),
            0,
            0
        )

        if (userId != "guest" && userId.isNotEmpty()) {
            vm._colorSyncStatus.value = ColorSyncStatus.SYNCING
            vm.db.collection("users").document(userId).set(newPersonalEntity)
                .addOnSuccessListener {
                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                    addNewSyncLog(
                        context,
                        "personal",
                        "success",
                        listOf("تم مزامنة ألوان المستخدم الشخصية بنجاح مع حسابه السحابي"),
                        0,
                        0
                    )
                }
                .addOnFailureListener { err ->
                    vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                    addNewSyncLog(
                        context,
                        "personal",
                        "failed",
                        listOf("فشل مزامنة ألوان المستخدم الشخصية: ${err.localizedMessage}"),
                        0,
                        0
                    )
                }
        }
    }

    fun triggerManualSync(context: Context) {
        vm._colorSyncStatus.value = ColorSyncStatus.SYNCING
        addNewSyncLog(context, "colors", "syncing", listOf("بدء المزامنة اليدوية الإجبارية للألوان"), vm._colorScheme.value.version, vm._colorScheme.value.version)

        vm.db.collection("app_settings").document("color_scheme").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val cloud = doc.toObject(ColorSchemeEntity::class.java)
                    if (cloud != null) {
                        val local = vm._colorScheme.value
                        if (cloud.version > local.version) {
                            vm._pendingConflictScheme.value = cloud
                            vm._colorSyncStatus.value = ColorSyncStatus.CONFLICT
                            addNewSyncLog(context, "colors", "conflict", listOf("توقف المزامنة بسبب وجود تعارض في الألوان في السحابة v${cloud.version}"), local.version, cloud.version)
                        } else if (cloud.version < local.version) {
                            vm.db.collection("app_settings").document("color_scheme").set(local)
                                .addOnSuccessListener {
                                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                                    addNewSyncLog(context, "colors", "success", listOf("نجاح مزامنة الألوان التصاعدية يدوياً v${local.version}"), cloud.version, local.version)
                                    vm.mainViewModel.triggerNotification("✅ تم رفع وتحديث ألوان الدليل بنجاح!")
                                }
                        } else {
                            ColorSyncManager.saveLocalColorScheme(context, cloud)
                            vm._colorScheme.value = cloud
                            vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                            addNewSyncLog(context, "colors", "success", listOf("ألوان الدليل متزامنة تماماً ومتطابقة مع السحابة"), local.version, cloud.version)
                            vm.mainViewModel.triggerNotification("✅ ألوان التطبيق متزامنة بالكامل!")
                        }
                    }
                } else {
                    val defaultScheme = ColorSchemeEntity(version = 1, lastUpdated = getCurrentTimestampString())
                    vm.db.collection("app_settings").document("color_scheme").set(defaultScheme)
                        .addOnSuccessListener {
                            vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                            vm._colorScheme.value = defaultScheme
                            ColorSyncManager.saveLocalColorScheme(context, defaultScheme)
                            addNewSyncLog(context, "colors", "success", listOf("تهيئة أولية ناجحة أثناء المزامنة اليدوية"), 0, 1)
                            vm.mainViewModel.triggerNotification("✅ تم تهيئة ألوان السحابة بنجاح!")
                        }
                }
            }
            .addOnFailureListener { err ->
                vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                addNewSyncLog(context, "colors", "failed", listOf("فشل المزامنة اليدوية: ${err.localizedMessage}"), vm._colorScheme.value.version, vm._colorScheme.value.version)
                vm.mainViewModel.triggerNotification("❌ فشل الاتصال بالخادم لمزامنة الألوان")
            }
    }

    fun resolveConflict(context: Context, useCloud: Boolean) {
        val pending = vm._pendingConflictScheme.value ?: return
        val local = vm._colorScheme.value

        if (useCloud) {
            ColorSyncManager.saveLocalColorScheme(context, pending)
            vm._colorScheme.value = pending
            vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
            addNewSyncLog(
                context,
                "colors",
                "success",
                listOf("تم حل التعارض: تم اختيار واعتماد النسخة السحابية v${pending.version}"),
                local.version,
                pending.version
            )
            vm._pendingConflictScheme.value = null
            vm.mainViewModel.triggerNotification("✅ تم اعتماد وتطبيق ألوان السحابة بنجاح!")
        } else {
            val updatedLocal = local.copy(version = pending.version + 1, lastUpdated = getCurrentTimestampString())
            vm._colorSyncStatus.value = ColorSyncStatus.SYNCING

            vm.db.collection("app_settings").document("color_scheme").set(updatedLocal)
                .addOnSuccessListener {
                    ColorSyncManager.saveLocalColorScheme(context, updatedLocal)
                    vm._colorScheme.value = updatedLocal
                    vm._colorSyncStatus.value = ColorSyncStatus.SYNCED
                    addNewSyncLog(
                        context,
                        "colors",
                        "success",
                        listOf("تم حل التعارض: تم فرض النسخة المحلية وتحديث السحابة للإصدار v${updatedLocal.version}"),
                        local.version,
                        updatedLocal.version
                    )
                    vm._pendingConflictScheme.value = null
                    vm.mainViewModel.triggerNotification("✅ تم فرض ألوانك المحلية وتحديث السحابة بنجاح!")
                }
                .addOnFailureListener { err ->
                    vm._colorSyncStatus.value = ColorSyncStatus.NOT_SYNCED
                    addNewSyncLog(
                        context,
                        "colors",
                        "failed",
                        listOf("فشل فرض النسخة المحلية في حل التعارض: ${err.localizedMessage}"),
                        local.version,
                        updatedLocal.version
                    )
                    vm.mainViewModel.triggerNotification("❌ تعذر رفع نسختك المحلية لحل التعارض")
                }
        }
    }

    fun getCurrentTimestampString(): String {
        return try {
            DateFormatter.formatIso(System.currentTimeMillis()) + "Z"
        } catch (_: Exception) {
            "2026-08-06T15:00:00Z"
        }
    }

    fun addNewSyncLog(
        context: Context,
        type: String,
        status: String,
        changes: List<String>,
        versionFrom: Int,
        versionTo: Int
    ) {
        val newLog = SyncLogEntity(
            syncId = "sync_${UUID.randomUUID().toString().take(6)}",
            timestamp = getCurrentTimestampString(),
            type = type,
            status = status,
            changes = changes,
            versionFrom = versionFrom,
            versionTo = versionTo
        )
        ColorSyncManager.saveSyncLog(context, newLog)
        vm._colorSyncLogs.value = ColorSyncManager.getSyncLogs(context)
    }

    fun saveCustomPermissionsMatrixToFirestore(permissions: List<String>, role: String = "ADMIN") {
        val cleanRole = role.trim().uppercase().ifBlank { "ADMIN" }
        try {
            val appCtx = com.example.MyApplication.instance
            val prefs = appCtx?.getSharedPreferences("admin_rbac_cache", Context.MODE_PRIVATE)
            prefs?.edit()
                ?.putStringSet("role_perms_$cleanRole", permissions.toSet())
                ?.putBoolean("role_configured_$cleanRole", true)
                ?.apply()
            if (vm.mainViewModel.adminViewModel.adminRole.value.equals(cleanRole, ignoreCase = true)) {
                vm.mainViewModel.adminViewModel._supervisorPermissions.value = permissions
            }
        } catch (_: Exception) {}
        val payload = mapOf(
            "roles" to mapOf(
                cleanRole to mapOf(
                    "activePermissions" to permissions,
                    "totalCount" to permissions.size,
                    "updatedAt" to System.currentTimeMillis()
                )
            ),
            "lastUpdatedRole" to cleanRole,
            "activePermissions" to permissions,
            "totalCount" to permissions.size,
            "updatedAt" to System.currentTimeMillis()
        )
        vm.db.collection("settings").document("admin_permissions_matrix")
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                vm.mainViewModel.triggerNotification("💾 تم حفظ وتحديث مصفوفة الصلاحيات للدور $cleanRole (${permissions.size} / 538) في قاعدة بيانات Firestore بنجاح!")
            }
            .addOnFailureListener {
                vm.mainViewModel.triggerNotification("❌ تعذر حفظ مصفوفة الصلاحيات في Firestore: ${it.localizedMessage}")
            }
    }

    fun addColorPalette(name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") {
        val nextId = "palette_" + UUID.randomUUID().toString().take(6)
        val newPal = ColorPaletteEntity(nextId, name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
        vm.db.collection("color_themes").document(nextId).set(newPal)
        vm.mainViewModel.triggerNotification("🎨 تم إضافة اللون $name بنجاح")
    }

    fun updateColorPalette(id: String, name: String, primaryHex: String, secondaryHex: String, backgroundHex: String = "#0A0F0D", surfaceHex: String = "#121D18") {
        val updatedPal = ColorPaletteEntity(id, name, primaryHex, secondaryHex, backgroundHex, surfaceHex)
        vm.db.collection("color_themes").document(id).set(updatedPal)
        vm.mainViewModel.triggerNotification("✏️ تم تعديل اللون $name بنجاح")
    }

    fun deleteColorPalette(id: String) {
        vm.db.collection("color_themes").document(id).delete()
        vm.mainViewModel.triggerNotification("🗑️ تم حذف اللون بنجاح")
    }

    fun cleanup() {
        cardSettingsListener?.remove()
        colorSchemeListener?.remove()
        userColorsListener?.remove()
    }
}
