package com.example.ui.helpers

import android.content.Context
import com.example.ui.*
import com.example.data.NotificationEntity
import com.example.data.PendingProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.data.JobEntity
import com.example.data.models.JoinRequestEntity
import com.example.utils.FirebaseStorageUploader
import com.example.utils.SecurityCryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Helper class for handling provider/store/property/job join requests and registrations.
 */
class RegistrationHelper(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preferenceHelper: AppPreferenceHelper
) {

    suspend fun uploadImageStringOrUri(
        context: Context,
        input: String,
        storagePath: String,
        maxSizeBytes: Long
    ): String {
        if (input.isBlank()) return ""
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        return try {
            if (input.startsWith("content://") || input.startsWith("file://")) {
                val uri = android.net.Uri.parse(input)
                val res = FirebaseStorageUploader.uploadImageUri(
                    context, uri, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                )
                res.getOrDefault(input)
            } else {
                val cleanBase64 = if (input.contains(",")) input.substringAfter(",") else input
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val res = FirebaseStorageUploader.uploadBitmap(
                        bitmap, storagePath, maxDimension = 800, maxSizeBytes = maxSizeBytes
                    )
                    res.getOrDefault(input)
                } else input
            }
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    fun submitJoinForm(
        context: Context,
        scope: CoroutineScope,
        name: String,
        phone: String,
        catId: String,
        area: String,
        neighborhood: String,
        photoPath: String,
        idCardPath: String,
        gpsCoords: String,
        workPhotos: List<String> = emptyList(),
        customCategoryName: String = "",
        password: String = "",
        productAttachmentsJson: String = "",
        checkDuplicate: (String) -> String?,
        logAdminActivity: (String) -> Unit,
        triggerNotification: (String) -> Unit,
        addApplicantNotification: (String, String, String, String) -> Unit,
        onPendingAdded: (PendingProviderEntity) -> Unit,
        onStoreAdded: (StoreEntity) -> Unit,
        onPropertyAdded: (PropertyEntity) -> Unit,
        onJobAdded: (JobEntity) -> Unit,
        onClientAdded: (Map<String, Any>) -> Unit,
        onJoinRequestPhoneUpdated: (String) -> Unit,
        onNavigateToScreen: (String) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val duplicateType = checkDuplicate(cleanPhone)
        if (duplicateType != null) {
            triggerNotification("❌ عذراً! رقم الهاتف ($phone) مسجل بالفعل كـ ($duplicateType). لا يُسمح بتكرار الحسابات.")
            logAdminActivity("محاولة تسجيل فني مكرر محجوبة لرقم: $cleanPhone - نوع التكرار: $duplicateType")
            return
        }

        scope.launch {
            try {
                // Async duplicate check in join_requests
                db.collection("join_requests").whereEqualTo("phone", cleanPhone).get().addOnSuccessListener { qs ->
                    if (!qs.isEmpty) {
                        triggerNotification("❌ يوجد طلب انضمام مسجل بالفعل قيد المراجعة لرقم الهاتف هذا")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                triggerNotification("⏳ جاري ضغط الصور وحفظ الملفات في سحابة التخزين...")
            val finalSelfie = uploadImageStringOrUri(
                context, photoPath,
                FirebaseStorageUploader.getProviderProfilePath(cleanPhone),
                maxSizeBytes = 150 * 1024L
            )
            val finalIdCard = uploadImageStringOrUri(
                context, idCardPath,
                FirebaseStorageUploader.getProviderIdCardPath(cleanPhone),
                maxSizeBytes = 150 * 1024L
            )
            val finalWorkPhotos = workPhotos.mapIndexed { idx, p ->
                uploadImageStringOrUri(
                    context, p,
                    FirebaseStorageUploader.getProviderWorkPhotoPath(cleanPhone, idx),
                    maxSizeBytes = 300 * 1024L
                )
            }
            val encSelfie = if (finalSelfie.isNotEmpty()) SecurityCryptoUtils.encrypt(finalSelfie) else ""
            val encIdCard = if (finalIdCard.isNotEmpty()) SecurityCryptoUtils.encrypt(finalIdCard) else ""
            if (password.isNotEmpty()) {
                val valResult = SecurityCryptoUtils.validatePasswordPolicy(password)
                if (valResult.first) {
                    val authEmail = "$cleanPhone@yemen-services.app"
                    auth.createUserWithEmailAndPassword(authEmail, password.trim())
                        .addOnFailureListener { /* Account might already exist */ }
                }
            }
            val requestType = when (catId.uppercase()) {
                "STORE" -> "STORE"
                "RESTAURANT" -> "RESTAURANT"
                "MEDICAL" -> "MEDICAL"
                "PROPERTY" -> "PROPERTY"
                "JOB" -> "JOB"
                "CLIENT" -> "CLIENT"
                else -> "PROVIDER"
            }
            val requestProfession = when (requestType) {
                "STORE", "RESTAURANT", "MEDICAL" -> "STORE_OWNER"
                "PROPERTY" -> "PROPERTY_OWNER"
                "JOB" -> "JOB_POSTER"
                "CLIENT" -> "CLIENT"
                else -> "PROVIDER"
            }
            val requestDocId = cleanPhone
            val securedPasswordHash = if (password.isNotBlank()) com.example.utils.PasswordHasher.hash(password.trim()) else ""
            val newRequest = PendingProviderEntity(
                id = requestDocId,
                name = name,
                phone = phone,
                categoryId = catId,
                area = area,
                localNeighborhood = neighborhood,
                status = "PENDING",
                selfiePhotoBase64 = encSelfie,
                idPhotoBase64 = encIdCard,
                workPhotosBase64 = finalWorkPhotos,
                customCategoryName = customCategoryName,
                password = securedPasswordHash,
                productAttachmentsJson = productAttachmentsJson,
                profession = requestProfession,
                providerType = requestProfession
            )
            // Push to Cloud
            db.collection("pending_providers").document(requestDocId).set(newRequest)
            val unifiedJoinRequest = JoinRequestEntity(
                id = requestDocId,
                type = requestType,
                status = "PENDING",
                fullName = name,
                phone = cleanPhone,
                passwordHash = securedPasswordHash,
                city = area,
                area = neighborhood,
                neighborhood = neighborhood,
                categoryId = catId,
                categoryName = customCategoryName.ifBlank { catId },
                businessName = if (requestType == "STORE" || requestType == "RESTAURANT" || requestType == "MEDICAL") name else "",
                ownerName = name,
                propertyTitle = if (requestType == "PROPERTY") (if (customCategoryName.isNotBlank()) "$customCategoryName ($name)" else "مكتب/عقار ($name)") else "",
                jobTitle = if (requestType == "JOB") customCategoryName.ifBlank { name } else "",
                companyName = if (requestType == "JOB") name else "",
                profileImage = finalSelfie,
                idCardImage = finalIdCard,
                workImages = finalWorkPhotos,
                approvalStatus = "PENDING",
                submittedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.collection("join_requests").document(requestDocId).set(unifiedJoinRequest)
                .addOnSuccessListener {
                    try {
                        when (requestType.uppercase()) {
                            "STORE", "RESTAURANT", "MEDICAL" -> {
                                val secId = if (requestType == "RESTAURANT") "restaurants" else if (requestType == "MEDICAL") "medical" else "stores"
                                val catName = when (requestType) {
                                    "RESTAURANT" -> "مطاعم وكافيهات"
                                    "MEDICAL" -> "مراكز طبية وعيادات"
                                    else -> "محلات ومراكز تجارية"
                                }
                                val newStore = StoreEntity(
                                    id = requestDocId,
                                    name = name,
                                    phone = cleanPhone,
                                    ownerId = cleanPhone,
                                    ownerName = name,
                                    cityId = area,
                                    localNeighborhood = neighborhood,
                                    sectionId = secId,
                                    categoryId = catName,
                                    isActive = false,
                                    isApproved = false,
                                    password = password
                                )
                                db.collection("stores").document(requestDocId).set(newStore)
                                onStoreAdded(newStore)
                            }
                            "PROPERTY" -> {
                                val newProp = PropertyEntity(
                                    id = requestDocId,
                                    title = if (customCategoryName.isNotBlank()) "$customCategoryName ($name)" else "مكتب عقاري - $name",
                                    phone = cleanPhone,
                                    ownerId = cleanPhone,
                                    cityId = area,
                                    localNeighborhood = neighborhood,
                                    isActive = false,
                                    isApproved = false,
                                    password = password
                                )
                                db.collection("properties").document(requestDocId).set(newProp)
                                onPropertyAdded(newProp)
                            }
                            "JOB" -> {
                                val newJob = JobEntity(
                                    id = requestDocId,
                                    title = if (customCategoryName.isNotBlank()) customCategoryName else "وظيفة - $name",
                                    companyName = name,
                                    phone = cleanPhone,
                                    cityId = area,
                                    isActive = false,
                                    isApproved = false
                                )
                                db.collection("jobs").document(requestDocId).set(newJob)
                                onJobAdded(newJob)
                            }
                            "CLIENT" -> {
                                val userMap = mapOf(
                                    "id" to requestDocId,
                                    "name" to name,
                                    "phone" to cleanPhone,
                                    "residence" to area,
                                    "isApproved" to false,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.collection("users").document(requestDocId).set(userMap)
                                onClientAdded(userMap)
                            }
                        }
                    } catch (e: Exception) {}

                    val adminNotif = NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        title = "👷 طلب انضمام جديد للدليل",
                        message = "قدم ${name} طلب انضمام جديد في قسم ${if (customCategoryName.isNullOrBlank()) catId else customCategoryName} بمنطقة ${area}.",
                        targetType = "SUPERVISOR",
                        targetValue = "ALL",
                        timestamp = System.currentTimeMillis()
                    )
                    try {
                        db.collection("notifications").document(adminNotif.id).set(adminNotif)
                    } catch (e: Exception) {}
                    
                    triggerNotification("📨 تم تقديم طلبك ورفع المستندات بنجاح، جاري المراجعة من الإدارة")
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "تأكد من صغر حجم الصور واتصالك بالإنترنت"
                    triggerNotification("❌ فشل تقديم الطلب: $errorMsg")
                }

            onPendingAdded(newRequest)
            preferenceHelper.setJoinRequestPhone(context, phone)
            onJoinRequestPhoneUpdated(phone)

            addApplicantNotification(
                "📨 تم استلام طلب انضمامك بنجاح",
                "مرحباً يا غالي، تم استلام طلبك وجاري مراجعته والتحقق من التخصص والخبرة من قبل إدارة الدليل. نسعد بانضمامك وسنبلغك فور التنشيط!",
                "USER",
                phone
            )

            triggerNotification("📨 تم تقديم طلبك بنجاح، سيتم مراجعته من قبل الإدارة")
            onNavigateToScreen("JOIN_REQUEST_STATUS")
            } catch (e: Exception) {
                e.printStackTrace()
                com.example.utils.AppErrorLogManager.logFirestoreError("RegistrationHelper", "Error in submitJoinForm", e)
                triggerNotification("❌ حدث خطأ أثناء إرسال الطلب: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
            }
        }
    }

    fun registerClientUser(
        name: String,
        phone: String,
        residence: String,
        password: String = "",
        onClientAdded: (Map<String, Any>) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val userMap = mapOf(
            "id" to cleanPhone,
            "name" to name,
            "phone" to cleanPhone,
            "residence" to residence,
            "password" to password,
            "isApproved" to false,
            "createdAt" to System.currentTimeMillis()
        )
        try {
            db.collection("registered_users").document(cleanPhone).set(userMap)
            db.collection("users").document(cleanPhone).set(userMap)
        } catch (e: Exception) {}
        onClientAdded(userMap)
    }

    fun cancelOrResetJoinRequest(
        context: Context,
        phone: String,
        pendingProviders: List<PendingProviderEntity>,
        onPendingRemoved: (String) -> Unit,
        onPhoneCleared: () -> Unit,
        onGoBack: () -> Boolean
    ) {
        if (phone.isNotEmpty()) {
            val matching = pendingProviders.find { it.phone == phone }
            matching?.let {
                onPendingRemoved(it.id)
                try {
                    db.collection("pending_providers").document(it.id).delete()
                } catch (e: Exception) {}
            }
            try {
                db.collection("join_requests").document(phone).delete()
            } catch (e: Exception) {}
        }
        preferenceHelper.clearJoinRequestPhone(context)
        onPhoneCleared()
        onGoBack()
    }

    fun setJoinRequestPhone(
        context: Context,
        phone: String,
        onPhoneUpdated: (String) -> Unit
    ) {
        preferenceHelper.setJoinRequestPhone(context, phone)
        onPhoneUpdated(phone)
    }
}
