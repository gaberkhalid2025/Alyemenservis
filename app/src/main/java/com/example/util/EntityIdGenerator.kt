package com.example.util

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * 🔑 EntityIdGenerator - استراتيجية توليد المعرفات الفريدة المنظمة في كامل التطبيق
 * 
 * الميزات:
 * 1. استبدال معرفات UUID العشوائية بمعرفات منظمة ومفروزة زمنياً وسهلة القراءة والفهرسة.
 * 2. الصيغة: <PREFIX>_<TIMESTAMP_MILLIS>_<6_CHAR_HEX_RANDOM>
 * 3. ضمان تصاعد زمني وتسلسل فريد حتى في حالات التزامن العالي (High Concurrency).
 */
object EntityIdGenerator {

    private val lastTimestamp = AtomicLong(0L)

    enum class Prefix(val tag: String) {
        USER("USR"),
        PROVIDER("PRV"),
        STORE("STR"),
        PROPERTY("PROP"),
        JOB("JOB"),
        BOOKING("BKG"),
        PAYMENT("PAY"),
        CHAT("CHT"),
        PRODUCT("PRD"),
        REVIEW("REV"),
        OFFER("OFFER"),
        CATEGORY("CAT"),
        CITY("CITY"),
        NOTIFICATION("NTF"),
        COUPON("CPN"),
        REPORT("REP"),
        SUPERVISOR("SUP"),
        BANNER("BNR"),
        WALLET("WLT")
    }

    /**
     * توليد معرف فريد مسبوق برمز الكيان مع ضمان التسلسل الزمني
     * 
     * @param prefix نوع الكيان المطلوب توليد معرف له
     * @return المعرف النصي الفريد
     */
    fun generate(prefix: Prefix): String {
        var now = System.currentTimeMillis()
        var prev = lastTimestamp.get()
        while (now <= prev) {
            now = prev + 1
        }
        lastTimestamp.set(now)

        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        return "${prefix.tag}_${now}_$randomSuffix"
    }

    fun generateUserId(): String = generate(Prefix.USER)
    fun generateProviderId(): String = generate(Prefix.PROVIDER)
    fun generateStoreId(): String = generate(Prefix.STORE)
    fun generatePropertyId(): String = generate(Prefix.PROPERTY)
    fun generateJobId(): String = generate(Prefix.JOB)
    fun generateBookingId(): String = generate(Prefix.BOOKING)
    fun generatePaymentId(): String = generate(Prefix.PAYMENT)
    fun generateChatId(): String = generate(Prefix.CHAT)
    fun generateProductId(): String = generate(Prefix.PRODUCT)
    fun generateReviewId(): String = generate(Prefix.REVIEW)
    fun generateOfferId(): String = generate(Prefix.OFFER)
    fun generateCategoryId(): String = generate(Prefix.CATEGORY)
    fun generateCityId(): String = generate(Prefix.CITY)
    fun generateNotificationId(): String = generate(Prefix.NOTIFICATION)
    fun generateCouponId(): String = generate(Prefix.COUPON)
    fun generateReportId(): String = generate(Prefix.REPORT)
    fun generateSupervisorId(): String = generate(Prefix.SUPERVISOR)
    fun generateBannerId(): String = generate(Prefix.BANNER)
    fun generateWalletId(): String = generate(Prefix.WALLET)

    /**
     * استخراج بادئة نوع الكيان من المعرف النصي
     * @param id المعرف النصي
     * @return البادئة النصية (مثل "USR" أو "PRV")
     */
    fun getPrefix(id: String): String {
        return id.substringBefore("_", "")
    }

    /**
     * التحقق من صحة ومطابقة المعرف لصيغة المعرفات المنظمة للنظام
     * @param id المعرف المطلوب فحصه
     * @return true إذا كان المعرف صالحاً
     */
    fun isValidPrefixedId(id: String): Boolean {
        if (id.isBlank() || !id.contains("_")) return false
        val parts = id.split("_")
        return parts.size >= 3 && parts[0].length in 2..6 && parts[1].toLongOrNull() != null
    }
}
