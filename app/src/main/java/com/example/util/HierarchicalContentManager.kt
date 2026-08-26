package com.example.util

import com.google.firebase.firestore.FirebaseFirestore

/**
 * 🗂️ HierarchicalContentManager - إدارة التصنيفات الشجرية والفرعية ومراجعة واعتماد المحتوى
 * 
 * الميزات:
 * 1. هيكل شجري متكامل للأقسام الرئيسية والفرعية والكلمات الدلالية الشائعة في اليمن.
 * 2. تدفق إرسال طلبات إضافة المزودين الجدد للمراجعة والاعتماد من قبل الإدارة (Moderation Workflow).
 * 3. إطلاق الإشعارات الإدارية للمشرفين عند تقديم محتوى جديد.
 * 4. فحص المحتوى القديم أو غير المحدث (Stale Content Checker) لأكثر من 90 يوماً لتحديث البيانات.
 */
object HierarchicalContentManager {

    private val db = FirebaseFirestore.getInstance()

    // 1. نماذج التصنيفات الشجرية
    data class Subcategory(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val iconName: String = "category"
    )

    data class HierarchicalCategory(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val iconName: String = "grid_view",
        val subcategories: List<Subcategory> = emptyList(),
        val tagsKeywords: List<String> = emptyList()
    )

    // دليل التصنيفات الافتراضي لخدمات اليمن
    val DefaultYemenCategories = listOf(
        HierarchicalCategory(
            id = "CAT_TECH",
            nameAr = "الصيانة والتكنولوجيا 💻",
            nameEn = "Tech & Maintenance",
            iconName = "build",
            subcategories = listOf(
                Subcategory("SUB_SOLAR", "صيانة المنظومات الشمسية والبطاريات ☀️", "Solar Maintenance"),
                Subcategory("SUB_MOBILE", "صيانة الهواتف والبرمجيات 📱", "Mobile Repair"),
                Subcategory("SUB_LAPTOP", "صيانة الكمبيوتر والشبكات 💻", "PC Repair"),
                Subcategory("SUB_ELEC", "التمديدات الكهربائية والمولدات ⚡", "Electrical Wiring")
            ),
            tagsKeywords = listOf("طاقة شمسية", "بطاريات", "إنفرتر", "برمجة", "تلفونات", "مولدات", "شاشات")
        ),
        HierarchicalCategory(
            id = "CAT_HEALTH",
            nameAr = "الرعاية الصحية والمختبرات 🩺",
            nameEn = "Health & Medical",
            iconName = "medical_services",
            subcategories = listOf(
                Subcategory("SUB_CLINIC", "العيادات والمراكز التخصصية 🏥", "Specialized Clinics"),
                Subcategory("SUB_LAB", "المختبرات والتحاليل الطبية 🧪", "Medical Labs"),
                Subcategory("SUB_PHARMA", "الصيدليات والمستلزمات الطبية 💊", "Pharmacies"),
                Subcategory("SUB_NURSE", "التمريض والتمريض المنزلي 🩺", "Home Nursing")
            ),
            tagsKeywords = listOf("دكتور", "فحوصات", "دم", "أدوية", "عيادة أسنان", "عيادة عيون", "أشعة")
        ),
        HierarchicalCategory(
            id = "CAT_HOME",
            nameAr = "الخدمات المنزلية والإنشاءات 🏠",
            nameEn = "Home & Construction",
            iconName = "home",
            subcategories = listOf(
                Subcategory("SUB_PLUMB", "السباكة وخزانات المياه 🚰", "Plumbing"),
                Subcategory("SUB_CLEAN", "التنظيف ومكافحة الحشرات 🧹", "Cleaning & Pest Control"),
                Subcategory("SUB_DECOR", "الدهانات والديكورات والجبس 🎨", "Paints & Decor"),
                Subcategory("SUB_CONSTR", "المقاولات والبناء والخرسانة 🏗️", "Construction")
            ),
            tagsKeywords = listOf("سباك", "تنظيف فلل", "رش حشرات", "نقاش", "ديكورات", "خزان مياه", "مقاولات")
        )
    )

    /**
     * تقديم مزود خدمة جديد للمراجعة والاعتماد من قبل الإدارة
     * 
     * @param providerPayload بيانات مزود الخدمة
     * @param onSuccess الإجراء المتبع عند نجاح الإرسال
     * @param onError الإجراء المتبع عند حدوث خطأ
     */
    fun submitNewProviderForReview(
        providerPayload: HashMap<String, Any?>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val pendingId = providerPayload["id"] as? String ?: EntityIdGenerator.generate(EntityIdGenerator.Prefix.PROVIDER)
        providerPayload["isApprovedByAdmin"] = false
        providerPayload["approvalStatus"] = "PENDING_REVIEW"
        providerPayload["submittedAt"] = System.currentTimeMillis()

        db.collection("pending_providers")
            .document(pendingId)
            .set(providerPayload)
            .addOnSuccessListener {
                notifyAdminNewContentSubmitted(pendingId, providerPayload["name"] as? String ?: "مزود جديد")
                onSuccess()
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "فشل تقديم الطلب للمراجعة") }
    }

    /**
     * إرسال إشعار للإدارة بوصول طلب تسجيل جديد
     */
    private fun notifyAdminNewContentSubmitted(entityId: String, entityTitle: String) {
        val notifId = EntityIdGenerator.generate(EntityIdGenerator.Prefix.NOTIFICATION)
        val notif = hashMapOf<String, Any?>(
            "id" to notifId,
            "title" to "طلب إضافة جديد قيد المراجعة 📑",
            "body" to "تم تقديم طلب جديد لإضافة: $entityTitle (معرّف: $entityId) يتطلب موافقتك للمعاينة والاعتماد.",
            "type" to "ADMIN_MODERATION",
            "targetId" to entityId,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        db.collection("admin_notifications").document(notifId).set(notif)
    }

    /**
     * فحص الإعلانات والخدمات التي لم يتم تحديثها منذ أكثر من 90 يوماً
     */
    fun checkStaleListings(
        onResult: (staleCount: Int) -> Unit
    ) {
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        db.collection("providers")
            .whereLessThan("updatedAt", ninetyDaysAgo)
            .get()
            .addOnSuccessListener { snapshots ->
                onResult(snapshots.size())
            }
            .addOnFailureListener {
                onResult(0)
            }
    }
}
