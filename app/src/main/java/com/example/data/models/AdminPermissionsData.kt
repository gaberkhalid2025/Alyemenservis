package com.example.data.models

import androidx.annotation.Keep

@Keep
enum class PermissionLevel(val arabicTitle: String, val colorHex: String) {
    BASIC("أساسي", "#10B981"),
    MEDIUM("متوسط", "#3B82F6"),
    ADVANCED("متقدم", "#F59E0B"),
    SENSITIVE("حساس", "#EF4444")
}

@Keep
enum class PermissionCategory(
    val mainKey: String,
    val arabicTitle: String,
    val iconEmoji: String,
    val count: Int
) {
    NOTIFICATIONS("MANAGE_NOTIFICATIONS", "صلاحيات الإشعارات والتنبيهات", "🔔", 40),
    BANNERS("MANAGE_BANNERS", "صلاحيات البنرات الإعلانية", "🖼️", 35),
    REGISTRATION_FORMS("MANAGE_REG_FORMS", "صلاحيات استمارات التسجيل والانضمام", "📝", 30),
    BOOKING_FORMS("MANAGE_BOOKINGS", "صلاحيات استمارات الحجز والمواعيد", "📅", 30),
    QUICK_SERVICE("MANAGE_QUICK_SERVICE", "صلاحيات استمارة اطلب خدمتك الفورية", "⚡", 25),
    CHAT("MANAGE_ADVANCED_CHAT", "صلاحيات المحادثات والدردشة الفورية", "💬", 45),
    THEMES_ICONS("MANAGE_THEMES", "صلاحيات الأيقونات الذهبية والثيمات", "🎨", 25),
    NEW_SECTIONS("MANAGE_NEW_SECTIONS", "صلاحيات الأقسام والتصنيفات", "📂", 35),
    MAPS("MANAGE_MAP", "صلاحيات الخرائط والمواقع الجغرافية", "🗺️", 35),
    STORES("MANAGE_STORES", "صلاحيات إدارة المحلات والمراكز", "🏬", 35),
    RESTAURANTS("MANAGE_RESTAURANTS", "صلاحيات إدارة المطاعم والكافيهات", "🍽️", 35),
    MEDICAL("MANAGE_MEDICAL", "صلاحيات إدارة المراكز الطبية والعيادات", "🏥", 35),
    PROPERTIES("MANAGE_PROPERTIES", "صلاحيات إدارة العقارات والأملاك", "🏢", 35),
    JOBS("MANAGE_JOBS", "صلاحيات إدارة الوظائف والشركات", "💼", 35),
    CUSTOM_TABS("MANAGE_CUSTOM_TABS", "صلاحيات التبويبات المخصصة والتنقل", "📑", 30),
    SECURITY_AUDIT("MANAGE_SECURITY", "صلاحيات الرقابة الأمنية والحظر", "🛡️", 30),
    FINANCIAL("MANAGE_FINANCIAL", "صلاحيات الإدارة المالية والاشتراكات", "💰", 25),
    SYSTEM_BACKUP("MANAGE_SYSTEM", "صلاحيات النظام وقواعد البيانات السحابية", "⚙️", 13)
}

@Keep
data class AdminPermissionItem(
    val id: String,
    val key: String,
    val name: String,
    val description: String,
    val targetGroup: String,
    val scope: String,
    val level: PermissionLevel,
    val category: PermissionCategory
)

object AdminPermissionsRegistry {

    val allPermissions: List<AdminPermissionItem> by lazy {
        val list = mutableListOf<AdminPermissionItem>()
        
        // 1. الإشعارات والتنبيهات (40 صلاحية)
        val notifItems = listOf(
            "NOTIF_SEND_ALL" to "صلاحية إرسال إشعار للجميع",
            "NOTIF_SEND_USERS" to "صلاحية إرسال إشعار للمستخدمين فقط",
            "NOTIF_SEND_PROVIDERS" to "صلاحية إرسال إشعار للفنيين فقط",
            "NOTIF_SEND_STORES" to "صلاحية إرسال إشعار للمحلات فقط",
            "NOTIF_SEND_RESTAURANTS" to "صلاحية إرسال إشعار للمطاعم فقط",
            "NOTIF_SEND_MEDICAL" to "صلاحية إرسال إشعار للمراكز الطبية فقط",
            "NOTIF_SEND_PROPERTIES" to "صلاحية إرسال إشعار للعقارات فقط",
            "NOTIF_SEND_JOBS" to "صلاحية إرسال إشعار لمعلني الوظائف فقط",
            "NOTIF_SEND_AREA" to "صلاحية إرسال إشعار لمنطقة أو مدينة محددة",
            "NOTIF_SEND_CATEGORY" to "صلاحية إرسال إشعار لتصنيف أو نشاط محدد",
            "NOTIF_SEND_INDIVIDUAL" to "صلاحية إرسال إشعار لشخص أو حساب محدد",
            "NOTIF_SEND_TOPIC" to "صلاحية إرسال إشعار حسب موضوع أو قناة معينة",
            "NOTIF_SEND_SCHEDULED" to "صلاحية جدولة إرسال الإشعارات مسبقاً",
            "NOTIF_SEND_AUTOMATED" to "صلاحية تفعيل الإشعارات التلقائية للنظام",
            "NOTIF_SEND_URGENT" to "صلاحية إرسال إشعارات طارئة وعالية الأهمية",
            "NOTIF_SEND_SOUND" to "صلاحية تخصيص نغمة وصوت الإشعار",
            "NOTIF_SEND_SILENT" to "صلاحية إرسال إشعارات صامتة بدون صوت",
            "NOTIF_SEND_WITH_IMAGE" to "صلاحية إرفاق صورة أو بانر مع الإشعار",
            "NOTIF_SEND_WITH_ACTION" to "صلاحية إضافة أزرار إجراء وتوجيه سريع مع الإشعار",
            "NOTIF_SEND_WITH_LINK" to "صلاحية إرفاق رابط خارجي أو داخلي مع الإشعار",
            "NOTIF_DELETE" to "صلاحية حذف الإشعارات المرسلة من السجل",
            "NOTIF_CANCEL" to "صلاحية إلغاء إشعار مجدول قبل موعد إرساله",
            "NOTIF_REPORT" to "صلاحية الاطلاع على تقارير تسليم ونسبة قراءة الإشعارات",
            "NOTIF_LOG_CLEAR" to "صلاحية مسح سجل الإشعارات المؤقتة",
            "NOTIF_TEMPLATE_ADD" to "صلاحية إنشاء وحفظ قوالب الإشعارات الجاهزة",
            "NOTIF_TEMPLATE_EDIT" to "صلاحية تعديل قوالب الإشعارات المخزنة",
            "NOTIF_TEMPLATE_DELETE" to "صلاحية حذف قوالب الإشعارات",
            "NOTIF_WHATSAPP_SYNC" to "صلاحية إرسال الإشعار عبر واتساب للمستخدم",
            "NOTIF_SMS_GATEWAY" to "صلاحية إرسال الإشعار عبر رسائل SMS القصيرة",
            "NOTIF_EMAIL_SYNC" to "صلاحية إرسال نسخة من الإشعار عبر البريد الإلكتروني",
            "NOTIF_IN_APP_POPUP" to "صلاحية إظهار الإشعار كنافذة منبثقة ملء الشاشة",
            "NOTIF_PIN_TOP" to "صلاحية تثبيت الإشعار في مركز الإشعارات بالهاتف",
            "NOTIF_GEO_FENCE" to "صلاحية إرسال الإشعار الجغرافي للمتواجدين في نطاق معين",
            "NOTIF_DEVICE_TOKEN" to "صلاحية فحص وتدقيق توكنات الأجهزة المستلمة",
            "NOTIF_RATE_LIMIT" to "صلاحية تحديد وتعديل معدل تكرار الإشعارات",
            "NOTIF_ANALYTICS" to "صلاحية تحليل معدل التفاعل والنقر على الإشعارات",
            "NOTIF_CUSTOM_ICON" to "صلاحية تغيير أيقونة الإشعار المخصصة",
            "NOTIF_LANGUAGE_FILTER" to "صلاحية إرسال الإشعار بلغة محددة",
            "NOTIF_EXPORT_LOGS" to "صلاحية تصدير سجلات وتواريخ الإشعارات كملف",
            "NOTIF_RETRY_FAILED" to "صلاحية إعادة إرسال الإشعارات التي فشل وصولها"
        )
        notifItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(0, 14, 20, 23, 27, 28)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 2, 3, 4, 5, 6, 7, 8, 12, 13)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("1.${idx+1}", k, n, "تمكين $n داخل النظام بشكل كامل", "إشعارات النظام", "نظام الإشعارات الشامل", lvl, PermissionCategory.NOTIFICATIONS))
        }

        // 2. البنرات الإعلانية (35 صلاحية)
        val bannerItems = listOf(
            "BANNER_VIEW" to "صلاحية عرض جميع البنرات الإعلانية",
            "BANNER_ADD" to "صلاحية إضافة بنر إعلاني جديد",
            "BANNER_EDIT" to "صلاحية تعديل بيانات البنر وتصميمه",
            "BANNER_DELETE" to "صلاحية حذف البنر الإعلاني نهائياً",
            "BANNER_ACTIVATE" to "صلاحية تفعيل ظهور البنر فورياً",
            "BANNER_DEACTIVATE" to "صلاحية إيقاف البنر مؤقتاً",
            "BANNER_REORDER" to "صلاحية تغيير ترتيب تسلسل البنرات",
            "BANNER_SCHEDULE" to "صلاحية جدولة البنر بمواعيد بداية ونهاية",
            "BANNER_SET_LOCATION" to "صلاحية تحديد مكان وموضع ظهور البنر",
            "BANNER_LINK_EXTERNAL" to "صلاحية ربط البنر برابط موقع خارجي",
            "BANNER_LINK_SECTION" to "صلاحية ربط البنر بقسم أو صفحة داخلية",
            "BANNER_LINK_STORE" to "صلاحية ربط البنر بمتجر أو مزود خدمة محدد",
            "BANNER_TARGET_CITY" to "صلاحية استهداف مدينة محددة بالبنر",
            "BANNER_TARGET_USER_TYPE" to "صلاحية استهداف نوع مستخدمين محدد",
            "BANNER_ANALYTICS" to "صلاحية مشاهدة إحصائيات النقرات والمشاهدات",
            "BANNER_CLICK_TRACKING" to "صلاحية تتبع روابط وإحالات البنر",
            "BANNER_AUTO_SLIDE_SPEED" to "صلاحية ضبط سرعة انتقال البنرات التلقائي",
            "BANNER_RESIZE" to "صلاحية تعديل مقاسات وأبعاد البنر بالواجهة",
            "BANNER_CATEGORY_HEADER" to "صلاحية وضع بنرات مخصصة لرؤوس الأقسام",
            "BANNER_POPUP_PROMO" to "صلاحية إنشاء بنر نافذة إعلانية منبثقة",
            "BANNER_FLOATING" to "صلاحية تفعيل بنر عائم سريع التفاعل",
            "BANNER_VIDEO_SUPPORT" to "صلاحية تفعيل بنرات الفيديو المتحركة",
            "BANNER_BADGE_OVERLAY" to "صلاحية إضافة شارات ترويجية فوق البنر",
            "BANNER_PRICE_TAG" to "صلاحية إضافة شريط العروض والأسعار للبنر",
            "BANNER_CUSTOM_ANIMATION" to "صلاحية تخصيص تأثيرات حركة وانتقال البنرات",
            "BANNER_EXPORT_METRICS" to "صلاحية تصدير تقارير حملات البنرات الإعلانية",
            "BANNER_EXPIRY_ALERT" to "صلاحية ضبط تنبيه انتهاء مدة الإعلان للمعلن",
            "BANNER_MAX_DISPLAY_COUNT" to "صلاحية تحديد الحد الأقصى للمشاهدات",
            "BANNER_CALL_TO_ACTION" to "صلاحية تخصيص نص وزر اتخاذ الإجراء",
            "BANNER_SPONSOR_TAG" to "صلاحية إضافة علامة الإعلان الممول للبنر",
            "BANNER_DARK_MODE_STYLE" to "صلاحية ضبط مظهر البنر في الوضع الليلي",
            "BANNER_DIRECT_PHONE_CALL" to "صلاحية ربط زر البنر بالاتصال المباشر",
            "BANNER_DIRECT_WHATSAPP" to "صلاحية ربط زر البنر بمحادثة واتساب فورية",
            "BANNER_GEO_RADIUS" to "صلاحية تحديد النطاق الجغرافي بالكيلومتر",
            "BANNER_PRIORITY_BOOST" to "صلاحية رفع أولوية ظهور البنر في الصدارة"
        )
        bannerItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(3, 7, 19, 34)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 2, 4, 5, 8, 12, 13)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("2.${idx+1}", k, n, "تمكين $n بكامل الصلاحيات", "البنرات الإعلانية", "الحملات الإعلانية", lvl, PermissionCategory.BANNERS))
        }

        // 3. استمارات التسجيل والانضمام (30 صلاحية)
        val regItems = listOf(
            "REG_VIEW_ALL_FIELDS" to "صلاحية استعراض جميع حقول استمارات التسجيل",
            "REG_ADD_CUSTOM_FIELD" to "صلاحية إضافة حقل مخصص جديد للاستمارة",
            "REG_EDIT_FIELD" to "صلاحية تعديل اسم وخيارات الحقل الإضافي",
            "REG_DELETE_FIELD" to "صلاحية حذف حقل من استمارة التسجيل",
            "REG_REORDER_FIELDS" to "صلاحية إعادة ترتيب تسلسل حقول الاستمارة",
            "REG_SET_MANDATORY" to "صلاحية تعيين الحقل كإجباري أو اختياري",
            "REG_TOGGLE_DOCS_UPLOAD" to "صلاحية طلب إرفاق المستندات والوثائق",
            "REG_TOGGLE_PHOTO_UPLOAD" to "صلاحية طلب صورة شخصية / هوية من المتقدم",
            "REG_TOGGLE_COMMERCIAL_REG" to "صلاحية طلب رقم السجل التجاري أو الترخيص",
            "REG_TOGGLE_LOCATION_SELECT" to "صلاحية إلزامية تحديد الموقع بدقة على الخريطة",
            "REG_PROVIDER_FORM_CONFIG" to "صلاحية تخصيص استمارة انضمام الفنيين والخدمات",
            "REG_STORE_FORM_CONFIG" to "صلاحية تخصيص استمارة انضمام المحلات والمراكز",
            "REG_RESTAURANT_FORM_CONFIG" to "صلاحية تخصيص استمارة انضمام المطاعم والكافيهات",
            "REG_MEDICAL_FORM_CONFIG" to "صلاحية تخصيص استمارة انضمام المراكز الطبية",
            "REG_PROPERTY_FORM_CONFIG" to "صلاحية تخصيص استمارة نشر العقارات",
            "REG_JOB_FORM_CONFIG" to "صلاحية تخصيص استمارة الإعلان عن وظيفة",
            "REG_TERMS_EDIT" to "صلاحية تعديل شروط وسياسات الانضمام المعروضة",
            "REG_AUTO_APPROVAL" to "صلاحية تفعيل القبول التلقائي للطلبات المستوفية",
            "REG_SMS_VERIFICATION" to "صلاحية تفعيل التحقق من الهاتف برمز SMS",
            "REG_WHATSAPP_CONFIRM" to "صلاحية إرسال إشعار واتساب تأكيدي عند إرسال الطلب",
            "REG_WELCOME_NOTE" to "صلاحية تخصيص رسالة الترحيب بعد اكتمال التسجيل",
            "REG_FIELD_MAX_LENGTH" to "صلاحية ضبط الحد الأقصى لطول النصوص والمدخلات",
            "REG_FIELD_REGEX_RULE" to "صلاحية ضبط قواعد التحقق والأنماط (Regex)",
            "REG_DOCUMENT_FORMATS" to "صلاحية تحديد صيغ الملفات المسموح رفعها",
            "REG_MAX_FILE_SIZE" to "صلاحية تحديد الحجم الأقصى لملفات الوثائق",
            "REG_EXPORT_APPLICATIONS" to "صلاحية تصدير بيانات طلبات الانضمام إلى ملف",
            "REG_ARCHIVE_REQUESTS" to "صلاحية أرشفة طلبات الانضمام القديمة",
            "REG_CUSTOM_DROPDOWN_OPTIONS" to "صلاحية تخصيص خيارات القوائم المنسدلة",
            "REG_NOTIFICATION_RECIPIENTS" to "صلاحية تحديد المشرفين المستلمين لإشعار الطلب الجديد",
            "REG_REJECTION_REASONS_LIST" to "صلاحية إدارة قائمة أسباب الرفض الجاهزة"
        )
        regItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(3, 17, 18)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 2, 5, 10, 11, 12, 13, 14, 15)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("3.${idx+1}", k, n, "تمكين $n باستمارات التسجيل", "استمارات التسجيل", "نماذج الانضمام", lvl, PermissionCategory.REGISTRATION_FORMS))
        }

        // 4. استمارات الحجز والمواعيد (30 صلاحية)
        val bookingItems = listOf(
            "BOOKING_VIEW_ALL" to "صلاحية استعراض ومتابعة جميع الحجوزات النشطة",
            "BOOKING_ACCEPT" to "صلاحية قبول وتأكيد الحجز نيابة عن المزود",
            "BOOKING_REJECT" to "صلاحية رفض وإلغاء الحجز مع توضيح السبب",
            "BOOKING_RESCHEDULE" to "صلاحية تعديل موعد الحجز ووقت الخدمة",
            "BOOKING_ADD_CUSTOM_FIELD" to "صلاحية إضافة حقول مخصصة لاستمارة الحجز",
            "BOOKING_EDIT_FIELDS" to "صلاحية تعديل حقول استمارة الحجز",
            "BOOKING_DELETE_FIELD" to "صلاحية إزالة حقول من نموذج الحجز",
            "BOOKING_SET_TIME_SLOTS" to "صلاحية إدارة وتحديد فترات الأوقات المتاحة",
            "BOOKING_TOGGLE_DEPOSIT" to "صلاحية طلب عربون مسبق للحجز",
            "BOOKING_PRICE_CALCULATION" to "صلاحية ضبط آلية تسعير وحساب رسوم الحجز",
            "BOOKING_AUTO_CONFIRM" to "صلاحية تفعيل التأكيد الآلي للحجوزات",
            "BOOKING_CANCEL_WINDOW" to "صلاحية تحديد مهلة الإلغاء المجاني المسموحة",
            "BOOKING_EXPORT_CALENDAR" to "صلاحية مزامنة وتصدير الحجوزات للتقويم",
            "BOOKING_SMS_REMINDER" to "صلاحية تفعيل تذكير الرسائل النصية قبل الموعد",
            "BOOKING_WHATSAPP_REMINDER" to "صلاحية تفعيل تذكير واتساب التلقائي قبل الموعد",
            "BOOKING_RATING_REQUEST" to "صلاحية إرسال طلب التقييم التلقائي بعد انتهاء الموعد",
            "BOOKING_DISPUTE_RESOLVE" to "صلاحية التدخل وحل النزاعات على الحجوزات",
            "BOOKING_REFUND_PROCESS" to "صلاحية معالجة وإرجاع مبالغ الحجوزات الملغاة",
            "BOOKING_CAPACITY_LIMIT" to "صلاحية تحديد الحد الأقصى للحجوزات اليومية",
            "BOOKING_HOLIDAYS_CONFIG" to "صلاحية إدارة أيام العطلات والإجازات الرسمية",
            "BOOKING_EMERGENCY_SLOTS" to "صلاحية فتح مواعيد طوارئ استثنائية",
            "BOOKING_LOG_AUDIT" to "صلاحية الاطلاع على سجل تدقيق التعديلات على الحجز",
            "BOOKING_ASSIGN_SUPERVISOR" to "صلاحية تعيين مشرف لمتابعة حجز معين",
            "BOOKING_STATUS_OVERRIDE" to "صلاحية التغيير اليدوي لحالة الحجز",
            "BOOKING_CLIENT_BLACKLIST" to "صلاحية حظر العملاء المسيئين من نظام الحجز",
            "BOOKING_INVOICE_GENERATE" to "صلاحية إنشاء وإصدار فاتورة الحجز الإلكترونية",
            "BOOKING_REPEAT_SCHEDULE" to "صلاحية تفعيل جدولة الحجوزات الدورية المتكررة",
            "BOOKING_LOCATION_MAP_PIN" to "صلاحية إلزامية تثبيت موقع الخدمة المنزلية",
            "BOOKING_SPECIAL_REQUESTS" to "صلاحية تفعيل خانة الطلبات والملاحظات الخاصة",
            "BOOKING_STATISTICS_DASHBOARD" to "صلاحية مشاهدة إحصائيات ونسب إنجاز الحجوزات"
        )
        bookingItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(2, 16, 17, 23, 24)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 3, 4, 8, 9, 10)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("4.${idx+1}", k, n, "تمكين $n في منظومة الحجز", "استمارات الحجز", "المواعيد والحجوزات", lvl, PermissionCategory.BOOKING_FORMS))
        }

        // 5. استمارة اطلب خدمتك الفورية (25 صلاحية)
        val quickItems = listOf(
            "QUICK_DISPATCH_ALL" to "صلاحية توجيه الطلبات الفورية لكافة الفنيين",
            "QUICK_DISPATCH_AREA" to "صلاحية توجيه الطلب لأقرب فني في المنطقة جغرافياً",
            "QUICK_CANCEL_REQUEST" to "صلاحية إلغاء الطلب الفوري وحذفه من النظام",
            "QUICK_CHANGE_PRICE" to "صلاحية تسعير وتعديل التكلفة التقديرية للخدمة",
            "QUICK_ASSIGN_PROVIDER" to "صلاحية إسناد الطلب لفني أو مركز معين يدوياً",
            "QUICK_EMERGENCY_RADAR" to "صلاحية تفعيل رادار الطوارئ السريع للإشعارات",
            "QUICK_CUSTOMIZE_FIELDS" to "صلاحية تخصيص وتعديل حقول استمارة الطلب الفوري",
            "QUICK_SET_DEADLINE" to "صلاحية ضبط مهلة قبول الفني للطلب (بالثواني)",
            "QUICK_AUDIO_NOTE" to "صلاحية تفعيل إرفاق تسجيل صوتي مع وصف المشكلة",
            "QUICK_PHOTO_ATTACH" to "صلاحية تفعيل إرفاق صور للمشكلة أو العطل",
            "QUICK_AUTO_ESCALATE" to "صلاحية التصعيد التلقائي في حال عدم استجابة الفني",
            "QUICK_LIVE_TRACKING" to "صلاحية التتبع المباشر لوصول الفني إلى العميل",
            "QUICK_FEE_COMMISSION" to "صلاحية ضبط نسبة عمولة التطبيق من الطلب الفوري",
            "QUICK_CLIENT_CALL" to "صلاحية الاتصال السريع بين العميل ومقدم الخدمة",
            "QUICK_WHATSAPP_BROADCAST" to "صلاحية إرسال الطلب لقروبات واتساب المعتمدة",
            "QUICK_NIGHT_SHIFT_MODE" to "صلاحية تفعيل تسعيرة ونظام الورديات الليلية",
            "QUICK_MINIMUM_CHARGE" to "صلاحية تحديد الحد الأدنى لأجرة الفحص والزيارة",
            "QUICK_DISPUTE_MANAGEMENT" to "صلاحية فض الخلافات الفورية بين الطرفين",
            "QUICK_CANCEL_PENALTY" to "صلاحية فرض غرامة على الإلغاء بعد انطلاق الفني",
            "QUICK_STATUS_REPORT" to "صلاحية استخراج تقارير سرعة الاستجابة ومتوسط الوصول",
            "QUICK_TAG_VIP_ORDERS" to "صلاحية وسم الطلبات المستعجلة كأولوية قصوى VIP",
            "QUICK_SERVICE_HOURS" to "صلاحية تحديد ساعات توفر الخدمة الفورية",
            "QUICK_SURGE_PRICING" to "صلاحية تفعيل التسعير الديناميكي وقت الذروة",
            "QUICK_BLOCK_ABUSE" to "صلاحية حظر الأرقام التي تنشئ طلبات وهمية",
            "QUICK_EXPORT_LOGS" to "صلاحية تصدير كافة سجلات الطلبات الفورية التاريخية"
        )
        quickItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(2, 3, 12, 17, 23)) PermissionLevel.SENSITIVE else if (idx in listOf(0, 1, 4, 5, 6, 10)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("5.${idx+1}", k, n, "تمكين $n بالخدمة الفورية", "الخدمة الفورية", "اطلب خدمتك الآن", lvl, PermissionCategory.QUICK_SERVICE))
        }

        // 6. المحادثات والدردشة الفورية والرقابة (45 صلاحية)
        val chatItems = listOf(
            "CHAT_MONITOR_ALL" to "صلاحية الرقابة والإشراف العام على جميع المحادثات",
            "CHAT_READ_USER_PROVIDER" to "صلاحية قراءة محادثات العملاء والفنيين للتحقق الأمني",
            "CHAT_READ_STORE_CHAT" to "صلاحية قراءة محادثات العملاء والمتاجر للتأكد من الجودة",
            "CHAT_SEND_ADMIN_MSG" to "صلاحية إرسال رسائل رسمية باسم إدارة التطبيق",
            "CHAT_DELETE_MESSAGE" to "صلاحية حذف أي رسالة مسيئة أو مخالفة من الطرفين",
            "CHAT_DELETE_CONVERSATION" to "صلاحية حذف المحادثة بالكامل وسجلها نهائياً",
            "CHAT_BLOCK_USER" to "صلاحية حظر مستخدم من استخدام ميزة الشات والمراسلة",
            "CHAT_BLOCK_WORDS" to "صلاحية إدارة قائمة الكلمات والأرقام المحظورة تلقائياً",
            "CHAT_ATTACH_IMAGE" to "صلاحية السماح برفع وإرسال الصور في المحادثات",
            "CHAT_ATTACH_VOICE" to "صلاحية السماح بإرسال التسجيلات الصوتية في الشات",
            "CHAT_ATTACH_FILE" to "صلاحية السماح برفع المستندات وملفات PDF في الشات",
            "CHAT_ATTACH_LOCATION" to "صلاحية إرسال ومشاركة الموقع الجغرافي المباشر",
            "CHAT_DISABLE_CHAT_STORE" to "صلاحية إيقاف الدردشة عن متجر أو فني محدد",
            "CHAT_ENABLE_CHAT_STORE" to "صلاحية إعادة تفعيل الدردشة لمتجر أو فني",
            "CHAT_AI_ASSISTANT_TOGGLE" to "صلاحية تفعيل وتعطيل المساعد الذكي في الشات",
            "CHAT_AI_MODEL_SETTINGS" to "صلاحية ضبط خيارات ونموذج الذكاء الاصطناعي",
            "CHAT_EXPORT_TRANSCRIPT" to "صلاحية تصدير نص المحادثة كملف للتحقيق والمراجعة",
            "CHAT_REPORT_HANDLING" to "صلاحية معالجة بلاغات المستخدمين ضد الرسائل المسيئة",
            "CHAT_AUTO_REPLY_ADD" to "صلاحية إضافة وتعديل الردود التلقائية والترحيبية",
            "CHAT_AUTO_REPLY_DELETE" to "صلاحية حذف الردود التلقائية",
            "CHAT_WORKING_HOURS" to "صلاحية تحديد أوقات عمل الشات وإظهار رسالة غير متصل",
            "CHAT_MESSAGE_EDIT_ALLOW" to "صلاحية تفعيل خيار تعديل الرسائل للمستخدمين",
            "CHAT_MESSAGE_UNSEND_ALLOW" to "صلاحية تفعيل خيار التراجع عن إرسال الرسائل",
            "CHAT_UNREAD_BADGE_SYNC" to "صلاحية مزامنة شارات الرسائل غير المقروءة",
            "CHAT_TYPING_INDICATOR" to "صلاحية تفعيل مؤشر جاري الكتابة الآن...",
            "CHAT_READ_RECEIPTS" to "صلاحية تفعيل علامات قراءة الرسالة (صحين زرقاء)",
            "CHAT_SPAM_PROTECTION" to "صلاحية ضبط فلاتر الحماية من الرسائل المكررة (Spam)",
            "CHAT_BROADCAST_CHANNEL" to "صلاحية إنشاء قنوات وقوائم رسائل جماعية",
            "CHAT_PIN_CONVERSATION" to "صلاحية تثبيت محادثات مهمة في أعلى قائمة الشات",
            "CHAT_SEARCH_ARCHIVE" to "صلاحية البحث في أرشيف كافة المحادثات السابقة",
            "CHAT_MEDIA_PREVIEW_LIMIT" to "صلاحية ضبط حجم الوسائط المسموح إرسالها",
            "CHAT_FORWARD_MESSAGE" to "صلاحية تفعيل ميزة إعادة توجيه الرسائل",
            "CHAT_STICKERS_EMOJI" to "صلاحية إدارة باقات الملصقات والرموز التعبيرية",
            "CHAT_SUPPORT_TRANSFER" to "صلاحية تحويل تذكرة الدعم الفني بين المشرفين",
            "CHAT_CALL_IN_APP" to "صلاحية تفعيل المكالمات الصوتية المباشرة داخل التطبيق",
            "CHAT_VIDEO_CALL_IN_APP" to "صلاحية تفعيل مكالمات الفيديو داخل الشات",
            "CHAT_END_TO_END_ENCRYPT" to "صلاحية تفعيل التشفير التام للمحادثات الحساسة",
            "CHAT_WATERMARK_PHOTOS" to "صلاحية إضافة علامة مائية تلقائية على صور الشات",
            "CHAT_CUSTOMER_SATISFACTION" to "صلاحية إظهار تقييم الرضا عن خدمة الشات",
            "CHAT_PRIORITY_ROUTING" to "صلاحية توجيه العملاء المميزين VIP لمشرف مباشر",
            "CHAT_DISAPPEARING_MSGS" to "صلاحية تفعيل الرسائل ذاتية الاختفاء بعد مدة",
            "CHAT_OFFLINE_STORAGE" to "صلاحية تفعيل حفظ الرسائل محلياً بدون إنترنت",
            "CHAT_TRANSLATE_TEXT" to "صلاحية تفعيل الترجمة الفورية للنصوص داخل الشات",
            "CHAT_SYSTEM_ALERTS_FEED" to "صلاحية إرسال تنبيهات النظام داخل نافذة الشات",
            "CHAT_QUICK_SNIPPETS" to "صلاحية إنشاء واختصار الردود السريعة للمشرفين"
        )
        chatItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(0, 1, 2, 4, 5, 6, 7, 12)) PermissionLevel.SENSITIVE else if (idx in listOf(3, 14, 15, 16, 17, 33, 34, 35)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("6.${idx+1}", k, n, "تمكين $n بنظام المحادثات", "المحادثات الفورية", "الشات والدردشة", lvl, PermissionCategory.CHAT))
        }

        // 7. الأيقونات الذهبية والثيمات والواجهات (25 صلاحية)
        val themeItems = listOf(
            "THEME_CHANGE_ACTIVE" to "صلاحية تغيير الثيم النشط للتطبيق كاملاً",
            "THEME_CUSTOM_COLORS" to "صلاحية تخصيص أكواد الألوان الرئيسية والفرعية",
            "THEME_DARK_MODE_TOGGLE" to "صلاحية فرض الوضع الليلي الداكن أو الفاتح",
            "THEME_GOLDEN_ICONS" to "صلاحية تفعيل حزمة الأيقونات الذهبية الفاخرة",
            "THEME_ICON_PACK_SELECT" to "صلاحية التبديل بين حزم ومكتبات الأيقونات",
            "THEME_FONT_FAMILY" to "صلاحية تغيير الخط العربي الافتراضي للتطبيق",
            "THEME_CARD_RADIUS" to "صلاحية ضبط درجة انحناء واستدارة زوايا الكروت",
            "THEME_HEADER_STYLE" to "صلاحية تخصيص شكل وترتيب الهيدر والشريط العلوي",
            "THEME_BOTTOM_NAV_STYLE" to "صلاحية تخصيص شريط التنقل السفلي وأيقوناته",
            "THEME_SPLASH_SCREEN" to "صلاحية تعديل شاشة البداية واللوجو والشعار",
            "THEME_BACKGROUND_ART" to "صلاحية إضافة وتغيير الخلفيات الفنية للواجهات",
            "THEME_SEASONAL_EFFECTS" to "صلاحية تفعيل تأثيرات المناسبات (رمضان، الأعياد)",
            "THEME_ACCENT_GLOW" to "صلاحية تفعيل تأثير التوهج اللوني على الأزرار",
            "THEME_TRANSPARENCY_BLUR" to "صلاحية تفعيل تأثيرات الزجاج والشفافية (Blur)",
            "THEME_STATUS_BAR_COLOR" to "صلاحية ضبط لون شريط حالة النظام وإشعارات الهاتف",
            "THEME_DIALOG_SHAPES" to "صلاحية تخصيص شكل النوافذ المنبثقة والتنبيهات",
            "THEME_EXPORT_CONFIG" to "صلاحية تصدير إعدادات الثيم ومشاركتها كملف",
            "THEME_IMPORT_CONFIG" to "صلاحية استيراد ثيم جاهز وتطبيقه فورياً",
            "THEME_ANIMATION_SPEED" to "صلاحية تسريع أو إبطاء حركات الانتقال والتأثيرات",
            "THEME_HIGH_CONTRAST" to "صلاحية تفعيل وضع التباين العالي لسهولة القراءة",
            "THEME_CUSTOM_CSS_RULES" to "صلاحية تطبيق أنماط متقدمة مخصصة على الواجهات",
            "THEME_SECTION_BADGES" to "صلاحية تخصيص ألوان شارات الأقسام والمتاجر",
            "THEME_RTL_MIRRORING" to "صلاحية ضبط اتجاهات النصوص والمحاذاة للغة العربية",
            "THEME_RESET_DEFAULTS" to "صلاحية إعادة ضبط جميع الألوان والثيمات للافتراضي",
            "THEME_PREVIEW_MODE" to "صلاحية معاينة التعديلات على الثيم قبل اعتمادها"
        )
        themeItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(0, 1, 23)) PermissionLevel.SENSITIVE else if (idx in listOf(3, 4, 5, 9, 10, 11)) PermissionLevel.ADVANCED else PermissionLevel.BASIC
            list.add(AdminPermissionItem("7.${idx+1}", k, n, "تمكين $n للواجهات والمظهر", "الأيقونات والثيمات", "المظهر والتخصيص", lvl, PermissionCategory.THEMES_ICONS))
        }

        // 8. الأقسام الرئيسية والفرعية والتصنيفات (35 صلاحية)
        val catItems = listOf(
            "CAT_VIEW_ALL" to "صلاحية استعراض جميع الأقسام والتصنيفات",
            "CAT_ADD_MAIN" to "صلاحية إضافة قسم رئيسي جديد في التطبيق",
            "CAT_ADD_SUB" to "صلاحية إضافة قسم فرعي تحت أي قسم رئيسي",
            "CAT_EDIT_NAME" to "صلاحية تعديل اسم القسم ووصفه بالعربية والإنجليزية",
            "CAT_CHANGE_ICON" to "صلاحية تغيير أيقونة أو صورة القسم",
            "CAT_DELETE" to "صلاحية حذف القسم نهائياً من قاعدة البيانات",
            "CAT_HIDE_SHOW" to "صلاحية إخفاء أو إظهار القسم من الواجهة مؤقتاً",
            "CAT_REORDER" to "صلاحية إعادة ترتيب تسلسل ظهور الأقسام بالصفحة",
            "CAT_PIN_HOMEPAGE" to "صلاحية تثبيت القسم في الصفحة الرئيسية للتطبيق",
            "CAT_SET_COLOR" to "صلاحية تخصيص لون مميز لبطاقة وخلفية القسم",
            "CAT_MOVE_ITEMS" to "صلاحية نقل مقدمي الخدمات والمحلات بين الأقسام",
            "CAT_MERGE_CATEGORIES" to "صلاحية دمج قسمين فرعيين في قسم واحد",
            "CAT_SPLIT_CATEGORY" to "صلاحية فصل قسم فرعي وتحويله لقسم مستقل",
            "CAT_SET_BADGE" to "صلاحية إضافة شارة مميزة على القسم (جديد، عروض)",
            "CAT_COMMISSION_RATE" to "صلاحية ضبط نسبة عمولة أو رسوم اشتراك مخصصة للقسم",
            "CAT_CUSTOM_FORM_LINK" to "صلاحية ربط القسم باستمارة تسجيل أو حجز مخصصة",
            "CAT_GEO_AVAILABILITY" to "صلاحية تحديد المحافظات والمدن التي يظهر فيها القسم",
            "CAT_REQUIRED_DOCUMENTS" to "صلاحية تحديد الوثائق الإلزامية لمقدمي خدمة القسم",
            "CAT_AGE_RESTRICTION" to "صلاحية ضبط تقييد العمر للمحتوى الخاص بالقسم",
            "CAT_TAGS_KEYWORDS" to "صلاحية إدارة الكلمات المفتاحية لتسهيل البحث عن القسم",
            "CAT_EXPORT_STRUCTURE" to "صلاحية تصدير شجرة وهيكلية الأقسام بالكامل",
            "CAT_IMPORT_STRUCTURE" to "صلاحية استيراد هيكلية أقسام جديدة مسبقة الصنع",
            "CAT_BULK_DELETE" to "صلاحية الحذف الجماعي للأقسام غير المستخدمة",
            "CAT_COUNT_STATS" to "صلاحية مشاهدة إحصائيات عدد المنشآت والطلبات بالقسم",
            "CAT_DEFAULT_IMAGE_FALLBACK" to "صلاحية تعيين صورة افتراضية لمقدمي خدمة القسم",
            "CAT_BANNER_LINK" to "صلاحية ربط القسم ببنر إعلاني خاص يظهر في قمته",
            "CAT_DIRECT_DISPATCH_RULE" to "صلاحية ضبط قواعد التوجيه السريع الخاصة بالقسم",
            "CAT_MINIMUM_PRICE_RULE" to "صلاحية وضع حد أدنى لأسعار الخدمات في هذا القسم",
            "CAT_VERIFICATION_BADGE_REQ" to "صلاحية فرض التوثيق الإلزامي للمنضمين للقسم",
            "CAT_SUBSCRIPTION_PLAN_LINK" to "صلاحية ربط القسم بباقات اشتراك مدفوعة محددة",
            "CAT_SHOW_IN_DRAWER" to "صلاحية إظهار القسم في القائمة الجانبية للتطبيق",
            "CAT_SHOW_IN_EXPLORE" to "صلاحية إظهار القسم في شاشة الاستكشاف والبحث",
            "CAT_CUSTOM_HEADER_TITLE" to "صلاحية تخصيص عنوان فرعي إرشادي أعلى القسم",
            "CAT_SEO_META_TAGS" to "صلاحية ضبط نصوص المشاركة والروابط العميقة للقسم",
            "CAT_RESTORE_DELETED" to "صلاحية استرجاع قسم محذوف من سلة المحذوفات"
        )
        catItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(5, 11, 22)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 2, 3, 4, 10, 14, 16)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("8.${idx+1}", k, n, "تمكين $n لهيكلية التطبيق", "الأقسام والتصنيفات", "التصنيفات والهيكلية", lvl, PermissionCategory.NEW_SECTIONS))
        }

        // 9. الخرائط والمواقع الجغرافية (35 صلاحية)
        val mapItems = listOf(
            "MAP_VIEW_ALL_PINS" to "صلاحية استعراض جميع النقاط والمنشآت على الخريطة",
            "MAP_FILTER_BY_CATEGORY" to "صلاحية تصفية المنشآت على الخريطة حسب النشاط",
            "MAP_FILTER_BY_CITY" to "صلاحية التبديل والتركيز على أي محافظة أو مدينة",
            "MAP_RADAR_SEARCH" to "صلاحية استخدام رادار البحث بنطاق الكيلومترات (Radius)",
            "MAP_CUSTOM_PIN_ICONS" to "صلاحية تخصيص أيقونات ودبابيس المواقع على الخريطة",
            "MAP_PROVIDER_LIVE_TRACK" to "صلاحية التتبع المباشر لحركة الفنيين والمناديب",
            "MAP_EDIT_STORE_LOCATION" to "صلاحية تعديل إحداثيات موقع أي متجر أو فني",
            "MAP_GEO_FENCING_MANAGE" to "صلاحية رسم وتحديد النطاقات الجغرافية والمناطق",
            "MAP_ROUTING_NAVIGATION" to "صلاحية تفعيل حساب المسارات والاتجاهات الملاحية",
            "MAP_SATELLITE_VIEW_TOGGLE" to "صلاحية التبديل لنمط خريطة القمر الصناعي",
            "MAP_HEATMAP_ORDERS" to "صلاحية عرض الخريطة الحرارية (Heatmap) لكثافة الطلبات",
            "MAP_CLUSTER_MARKERS" to "صلاحية تجميع النقاط المتقاربة في مجموعات (Clustering)",
            "MAP_OFFLINE_TILES_CACHE" to "صلاحية تنزيل وتخزين الخرائط للعمل دون إنترنت",
            "MAP_ADD_NEW_POINT_MANUAL" to "صلاحية إضافة نقطة أو معلم جديد على الخريطة يدوياً",
            "MAP_DELETE_POINT_PIN" to "صلاحية حذف نقطة أو معلم من الخريطة",
            "MAP_DISTANCE_CALCULATOR" to "صلاحية حساب وقياس المسافات المباشرة بين نقطتين",
            "MAP_COVERAGE_ZONES" to "صلاحية تحديد مناطق التغطية والمناطق غير المخدومة",
            "MAP_STREET_VIEW_LINK" to "صلاحية ربط النقاط بمشاهد الشوارع الافتراضية",
            "MAP_DEFAULT_CENTER_ZOOM" to "صلاحية تحديد مركز الخريطة الافتراضي ومستوى التكبير",
            "MAP_TRAFFIC_LAYER" to "صلاحية تفعيل طبقة الازدحام المروري وحالة الطرق",
            "MAP_NEIGHBORHOOD_BOUNDS" to "صلاحية تحديد حدود وأسماء الأحياء والحارات",
            "MAP_EXPORT_GEOJSON" to "صلاحية تصدير بيانات المواقع بصيغة GeoJSON و KML",
            "MAP_IMPORT_GEOJSON" to "صلاحية استيراد قوائم المعالم والمواقع الجغرافية",
            "MAP_ACCURACY_THRESHOLD" to "صلاحية ضبط الحد الأدنى لدقة GPS المقبولة",
            "MAP_AUTO_LOCATE_USER" to "صلاحية التحديد التلقائي لموقع المستخدم عند الفتح",
            "MAP_FAVORITE_PLACES_LAYER" to "صلاحية إظهار الأماكن الأكثر زيارة وتقييماً",
            "MAP_STORE_OPEN_STATUS_PIN" to "صلاحية تلوين الدبابيس حسب حالة الفتح والإغلاق",
            "MAP_VIP_PINS_ENLARGE" to "صلاحية تكبير وتألق دبابيس الحسابات المميزة VIP",
            "MAP_SPEED_LIMIT_ALERTS" to "صلاحية تتبع سرعة حركة الفنيين وتنبيهات التجاوز",
            "MAP_RESTRICTED_ZONES" to "صلاحية تحديد المناطق المحظورة أمنياً أو المحظور التوصيل لها",
            "MAP_CUSTOM_MAPBOX_STYLE" to "صلاحية ربط ستايل مخصص للخرائط من خوادم السحاب",
            "MAP_DIRECTIONS_API_SWITCH" to "صلاحية التبديل بين مزودي خدمات الملاحة والاتجاهات",
            "MAP_COORDINATES_CLIPBOARD" to "صلاحية نسخ ومشاركة الإحداثيات الجغرافية بدقة",
            "MAP_GEOFENCE_NOTIFY" to "صلاحية إرسال إشعار تلقائي عند دخول نطاق جغرافي",
            "MAP_SUMMARY_METRICS" to "صلاحية تقرير توزيع المنشآت حسب المحافظات والمديريات"
        )
        mapItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(6, 7, 14, 29)) PermissionLevel.SENSITIVE else if (idx in listOf(0, 3, 5, 8, 10, 16)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("9.${idx+1}", k, n, "تمكين $n بشاشة الخرائط", "الخرائط والمواقع", "المواقع والملاحة", lvl, PermissionCategory.MAPS))
        }

        // 10. المحلات والمراكز التجارية (35 صلاحية)
        val storeItems = listOf(
            "STORE_VIEW_ALL" to "صلاحية عرض جميع المحلات والمراكز التجارية",
            "STORE_VIEW_SPECIFIC" to "صلاحية عرض وتفقد بيانات محل محدد بدقة",
            "STORE_ADD" to "صلاحية إضافة محل أو مركز تجاري جديد يدوياً",
            "STORE_EDIT" to "صلاحية تعديل بيانات المحل وهاتفه وموقعه",
            "STORE_DELETE" to "صلاحية حذف المحل نهائياً من قاعدة البيانات",
            "STORE_BLOCK" to "صلاحية حظر المحل ومنعه من الظهور والتفاعل",
            "STORE_UNBLOCK" to "صلاحية فك الحظر وإعادة تنشيط المحل",
            "STORE_ACTIVATE" to "صلاحية قبول وتفعيل طلب انضمام المحل",
            "STORE_DEACTIVATE" to "صلاحية تعطيل وإيقاف المحل مؤقتاً",
            "STORE_PIN" to "صلاحية تثبيت المحل في صدارة القوائم والبحث",
            "STORE_UNPIN" to "صلاحية إلغاء تثبيت المحل وإعادته للترتيب الطبيعي",
            "STORE_SET_VIP" to "صلاحية منح المحل شارة وعضوية VIP الذهبية",
            "STORE_UNSET_VIP" to "صلاحية سحب شارة وعضوية VIP عن المحل",
            "STORE_VERIFY" to "صلاحية توثيق المحل ومنحه شارة التوثيق الرسمية",
            "STORE_UNVERIFY" to "صلاحية إلغاء التوثيق عن المحل وسحب الشارة",
            "STORE_RECOMMEND" to "صلاحية ترشيح المحل كموصى به في الصفحة الرئيسية",
            "STORE_UNRECOMMEND" to "صلاحية إلغاء ترشيح المحل كموصى به",
            "STORE_PASSWORD_RESET" to "صلاحية تغيير وتعيين كلمة مرور جديدة للمحل",
            "STORE_PASSWORD_VIEW" to "صلاحية الاطلاع وإظهار كلمة المرور الحالية للمحل",
            "STORE_PRODUCTS_MANAGE" to "صلاحية إدارة منتجات وقوائم أسعار المحل",
            "STORE_WORKING_HOURS" to "صلاحية ضبط أوقات دوام وساعات فتح وإغلاق المحل",
            "STORE_GALLERY_MANAGE" to "صلاحية إدارة وتدقيق صور المعرض التابعة للمحل",
            "STORE_COMMISSION_CUSTOM" to "صلاحية تحديد نسبة عمولة مخصصة لهذا المحل",
            "STORE_SUBSCRIPTION_EXPIRY" to "صلاحية تعديل تاريخ انتهاء اشتراك المحل",
            "STORE_TRANSFER_OWNERSHIP" to "صلاحية نقل ملكية المحل لرقم هاتف مستخدم آخر",
            "STORE_BRANCHES_MANAGE" to "صلاحية إضافة وإدارة فروع المحل المتعددة",
            "STORE_DISCOUNT_COUPONS" to "صلاحية إصدار وتفعيل كوبونات خصم خاصة بالمحل",
            "STORE_REVIEWS_MODERATE" to "صلاحية مراجعة وحذف التقييمات والتعليقات المسيئة",
            "STORE_DIRECT_CHAT_ACCESS" to "صلاحية التحكم في استقبال المحل للدردشة المباشرة",
            "STORE_EXPORT_LIST" to "صلاحية تصدير قائمة كاملة بالمحلات كملف إكسل",
            "STORE_COMMERCIAL_REG_VERIFY" to "صلاحية تدقيق ومطابقة السجل التجاري المرفق",
            "STORE_OFFERS_APPROVE" to "صلاحية الموافقة على عروض التخفيضات التي ينشرها المحل",
            "STORE_VISITS_ANALYTICS" to "صلاحية مشاهدة تقارير الزيارات والمشاهدات للمحل",
            "STORE_BADGE_ASSIGN" to "صلاحية إضافة وسام مخصص للمحل (الأكثر مبيعاً، حصري)",
            "STORE_AUDIT_LOG" to "صلاحية فحص سجل العمليات والتعديلات التي تمت على المحل"
        )
        storeItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(4, 5, 6, 17, 18, 24)) PermissionLevel.SENSITIVE else if (idx in listOf(2, 3, 7, 8, 11, 13, 15, 23)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("10.${idx+1}", k, n, "تمكين $n لإدارة المحلات", "المحلات والمراكز", "المراكز التجارية", lvl, PermissionCategory.STORES))
        }

        // 11. المطاعم والكافيهات (35 صلاحية)
        val restItems = listOf(
            "REST_VIEW_ALL" to "صلاحية عرض جميع المطاعم والكافيهات",
            "REST_VIEW_SPECIFIC" to "صلاحية عرض بيانات وتفاصيل مطعم محدد",
            "REST_ADD" to "صلاحية إضافة مطعم أو كافيه جديد إلى النظام",
            "REST_EDIT" to "صلاحية تعديل بيانات المطعم ومعلومات التواصل",
            "REST_DELETE" to "صلاحية حذف المطعم نهائياً وسجلاته",
            "REST_BLOCK" to "صلاحية حظر المطعم ومنعه من استقبال الطلبات",
            "REST_UNBLOCK" to "صلاحية فك الحظر وإعادة تنشيط المطعم",
            "REST_ACTIVATE" to "صلاحية تفعيل والموافقة على انضمام المطعم",
            "REST_DEACTIVATE" to "صلاحية إيقاف المطعم وتعطيله مؤقتاً",
            "REST_PIN" to "صلاحية تثبيت المطعم في أعلى قائمة المطاعم",
            "REST_UNPIN" to "صلاحية إلغاء تثبيت المطعم",
            "REST_SET_VIP" to "صلاحية منح المطعم شارة وعضوية VIP",
            "REST_UNSET_VIP" to "صلاحية إلغاء شارة وعضوية VIP للمطعم",
            "REST_VERIFY" to "صلاحية توثيق حساب المطعم رسمياً",
            "REST_UNVERIFY" to "صلاحية إلغاء توثيق حساب المطعم",
            "REST_RECOMMEND" to "صلاحية ترشيح المطعم في قائمة الموصى بها",
            "REST_UNRECOMMEND" to "صلاحية إلغاء ترشيح المطعم",
            "REST_MENU_MANAGE" to "صلاحية تعديل وإدارة قائمة الطعام والوجبات (Menu)",
            "REST_MENU_PRICES" to "صلاحية تعديل وتحديث أسعار الوجبات والمشروبات",
            "REST_TABLE_RESERVATION" to "صلاحية إدارة وتأكيد طلبات حجز الطاولات",
            "REST_DELIVERY_RADIUS" to "صلاحية تحديد نطاق ومناطق التوصيل للمطعم",
            "REST_MIN_ORDER_AMOUNT" to "صلاحية تحديد الحد الأدنى لقيمة طلب التوصيل",
            "REST_PASSWORD_RESET" to "صلاحية تعيين كلمة مرور جديدة لحساب المطعم",
            "REST_PASSWORD_VIEW" to "صلاحية إظهار كلمة المرور الحالية للمطعم",
            "REST_MEAL_OFFERS" to "صلاحية تفعيل وإدارة العروض والوجبات التوفيرية",
            "REST_PHOTOS_GALLERY" to "صلاحية إدارة صور الأطباق وجلسات المطعم",
            "REST_HEALTH_LICENSE_VERIFY" to "صلاحية تدقيق ومطابقة كرت البلدية والترخيص الصحي",
            "REST_FAMILY_SECTION_TAG" to "صلاحية إضافة شارات التوفر (قسم عوائل، ألعاب أطفال)",
            "REST_EXPORT_LIST" to "صلاحية تصدير قائمة المطاعم وإحصائياتها",
            "REST_ORDERS_LOG" to "صلاحية مراجعة سجل الطلبات المنفذة عبر المطعم",
            "REST_WORKING_HOURS" to "صلاحية ضبط مواعيد الوجبات (فطور، غداء، عشاء)",
            "REST_REVIEWS_MODERATE" to "صلاحية إدارة وحذف تقييمات وتعليقات العملاء",
            "REST_SUBSCRIPTION_EXPIRY" to "صلاحية ضبط وتمديد اشتراك المطعم بالدليل",
            "REST_DIRECT_WHATSAPP" to "صلاحية تفعيل الطلب المباشر عبر واتساب المطعم",
            "REST_AUDIT_LOG" to "صلاحية الاطلاع على سجل تعديلات إدارة المطعم"
        )
        restItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(4, 5, 6, 22, 23)) PermissionLevel.SENSITIVE else if (idx in listOf(2, 3, 7, 8, 11, 13, 15, 17)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("11.${idx+1}", k, n, "تمكين $n لإدارة المطاعم", "المطاعم والكافيهات", "المطاعم والمأكولات", lvl, PermissionCategory.RESTAURANTS))
        }

        // 12. المراكز الطبية والعيادات (35 صلاحية)
        val medItems = listOf(
            "MED_VIEW_ALL" to "صلاحية عرض جميع المراكز الطبية والمستشفيات والعيادات",
            "MED_VIEW_SPECIFIC" to "صلاحية تفقد بيانات مركز طبي أو عيادة محددة",
            "MED_ADD" to "صلاحية إضافة عيادة أو مركز طبي أو صيدلية جديدة",
            "MED_EDIT" to "صلاحية تعديل بيانات المركز الطبي وهواتف التواصل",
            "MED_DELETE" to "صلاحية حذف المنشأة الطبية نهائياً من الدليل",
            "MED_BLOCK" to "صلاحية حظر المركز الطبي ومنعه من الظهور",
            "MED_UNBLOCK" to "صلاحية فك الحظر وإعادة تنشيط المنشأة الطبية",
            "MED_ACTIVATE" to "صلاحية تفعيل وقبول طلب انضمام المركز الطبي",
            "MED_DEACTIVATE" to "صلاحية تعطيل وإيقاف المركز الطبي مؤقتاً",
            "MED_PIN" to "صلاحية تثبيت المركز الطبي في أعلى قائمة القطاع الصحي",
            "MED_UNPIN" to "صلاحية إلغاء تثبيت المركز الطبي",
            "MED_SET_VIP" to "صلاحية منح المركز الطبي شارة VIP المميزة",
            "MED_UNSET_VIP" to "صلاحية إلغاء شارة VIP عن المركز الطبي",
            "MED_VERIFY" to "صلاحية توثيق المركز الطبي وشارة الاعتماد الطبي",
            "MED_UNVERIFY" to "صلاحية إلغاء التوثيق والاعتماد الطبي",
            "MED_RECOMMEND" to "صلاحية ترشيح العيادة في قائمة المراكز الموصى بها",
            "MED_UNRECOMMEND" to "صلاحية إلغاء ترشيح العيادة",
            "MED_SPECIALTIES_MANAGE" to "صلاحية إدارة قائمة التخصصات الطبية للعيادة",
            "MED_DOCTORS_LIST" to "صلاحية إضافة وإدارة أسماء ومواعيد الأطباء والاستشاريين",
            "MED_APPOINTMENTS_SCHEDULE" to "صلاحية تنظيم ومتابعة حجوزات المواعيد الطبية",
            "MED_EMERGENCY_24H_TAG" to "صلاحية تفعيل شارة طوارئ 24 ساعة للصيدليات والمستشفيات",
            "MED_INSURANCE_COMPANIES" to "صلاحية إدارة قائمة شركات التأمين المعتمدة لدى المركز",
            "MED_LICENSE_VERIFICATION" to "صلاحية تدقيق ومطابقة ترخيص مزاولة المهنة الطبي",
            "MED_PASSWORD_RESET" to "صلاحية إعادة تعيين كلمة مرور حساب المركز الطبي",
            "MED_PASSWORD_VIEW" to "صلاحية إظهار كلمة المرور الحالية للمنشأة الطبية",
            "MED_HOME_VISIT_SERVICE" to "صلاحية تفعيل ميزة الكشف والزيارات المنزلية للتمريض",
            "MED_PRICING_CONSULTATION" to "صلاحية عرض وتعديل رسوم كشف الاستشارات الطبية",
            "MED_EXPORT_DIRECTORY" to "صلاحية تصدير الدليل الطبي والعيادات كملف",
            "MED_GALLERY_PHOTOS" to "صلاحية تدقيق صور المرافق والتجهيزات الطبية",
            "MED_WORKING_HOURS" to "صلاحية ضبط أوقات الدوام واستقبال الحالات",
            "MED_PHARMACY_DUTY_ROSTER" to "صلاحية إدارة جدول الصيدليات المناوبة ليلياً",
            "MED_REVIEWS_MODERATE" to "صلاحية الرقابة على التقييمات وحذف التعليقات غير اللائقة",
            "MED_SUBSCRIPTION_EXPIRY" to "صلاحية تعديل وتجديد اشتراك المركز الطبي",
            "MED_DIRECT_CONSULT_CHAT" to "صلاحية تمكين الاستشارات الطبية السريعة عبر الشات",
            "MED_AUDIT_LOG" to "صلاحية مراجعة سجل العمليات بالقطاع الطبي"
        )
        medItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(4, 5, 6, 22, 23, 24)) PermissionLevel.SENSITIVE else if (idx in listOf(2, 3, 7, 8, 11, 13, 15, 18)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("12.${idx+1}", k, n, "تمكين $n لإدارة القطاع الطبي", "المراكز الطبية والعيادات", "القطاع الطبي والصحي", lvl, PermissionCategory.MEDICAL))
        }

        // 13. العقارات والأملاك (35 صلاحية)
        val propItems = listOf(
            "PROP_VIEW_ALL" to "صلاحية عرض جميع العقارات المعروضة بالدليل",
            "PROP_VIEW_SPECIFIC" to "صلاحية فحص وتفقد بيانات عقار محدد بالتفصيل",
            "PROP_ADD" to "صلاحية إضافة عقار جديد (شقة، فيلا، أرض، عمارة) يدوياً",
            "PROP_EDIT" to "صلاحية تعديل مواصفات العقار وسعره وهواتف المعلن",
            "PROP_DELETE" to "صلاحية حذف إعلان العقار نهائياً من التطبيق",
            "PROP_BLOCK" to "صلاحية حظر إعلان العقار ومنعه من الظهور",
            "PROP_UNBLOCK" to "صلاحية فك الحظر وإعادة تنشيط إعلان العقار",
            "PROP_ACTIVATE" to "صلاحية قبول ونشر إعلان العقار للجمهور",
            "PROP_DEACTIVATE" to "صلاحية إيقاف العقار مؤقتاً (تم البيع أو التأجير)",
            "PROP_PIN" to "صلاحية تثبيت العقار في صدارة نتائج البحث العقاري",
            "PROP_UNPIN" to "صلاحية إلغاء تثبيت العقار",
            "PROP_SET_VIP" to "صلاحية منح العقار شارة VIP المميزة",
            "PROP_UNSET_VIP" to "صلاحية إلغاء شارة VIP عن العقار",
            "PROP_VERIFY" to "صلاحية توثيق ملكية العقار ومنحه شارة التوثيق",
            "PROP_UNVERIFY" to "صلاحية إلغاء توثيق إعلان العقار",
            "PROP_RECOMMEND" to "صلاحية ترشيح العقار كفرصة ذهبية أو لقطة",
            "PROP_UNRECOMMEND" to "صلاحية إلغاء ترشيح العقار",
            "PROP_PRICE_CURRENCY" to "صلاحية تحديد وتغيير عملة السعر (ريال يمني، سعودي، دولار)",
            "PROP_MAP_LOCATION_ACCURACY" to "صلاحية تثبيت ومراجعة موقع العقار على الخريطة",
            "PROP_PHOTOS_VIDEOS_MANAGE" to "صلاحية إدارة صور وفيديوهات الجولة الافتراضية للعقار",
            "PROP_DEED_DOCUMENT_VERIFY" to "صلاحية تدقيق وثائق وبصيرة ملكية العقار",
            "PROP_PASSWORD_RESET" to "صلاحية إعادة تعيين كلمة مرور حساب المعلن العقاري",
            "PROP_PASSWORD_VIEW" to "صلاحية إظهار كلمة المرور الحالية للمعلن العقاري",
            "PROP_AGENCY_OFFICES_MANAGE" to "صلاحية إضافة وإدارة المكاتب والشركات العقارية",
            "PROP_COMMISSION_CALCULATOR" to "صلاحية حساب وإدارة نسبة السعاية والعمولة العقارية",
            "PROP_RENT_INSTALLMENTS" to "صلاحية تحديد دورية الإيجار (شهري، سنوي، مقدم)",
            "PROP_FURNISHED_STATUS" to "صلاحية تصنيف العقار (مفروش بالكامل، غير مفروش)",
            "PROP_EXPORT_LISTINGS" to "صلاحية تصدير جدول العقارات المتاحة كملف",
            "PROP_INQUIRIES_LOG" to "صلاحية متابعة طلبات الشراء والمعاينة المرسلة للعقار",
            "PROP_EXPIRY_DATE_EXTEND" to "صلاحية تمديد فترة صلاحية وبقاء الإعلان العقاري",
            "PROP_WATERMARK_PHOTOS" to "صلاحية وضع علامة مائية باسم التطبيق على صور العقار",
            "PROP_DIRECT_WHATSAPP_CALL" to "صلاحية تفعيل أزرار الاتصال والواتساب المباشر للمالك",
            "PROP_AUCTION_BIDDING" to "صلاحية تفعيل نظام المزادات العقارية على الأملاك",
            "PROP_REVIEWS_MODERATE" to "صلاحية إدارة تعليقات وتقييمات العملاء على العقار",
            "PROP_AUDIT_LOG" to "صلاحية مراجعة سجل التعديلات العقارية"
        )
        propItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(4, 5, 6, 20, 21, 22)) PermissionLevel.SENSITIVE else if (idx in listOf(2, 3, 7, 8, 11, 13, 15, 23)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("13.${idx+1}", k, n, "تمكين $n للعقارات والأملاك", "العقارات والأملاك", "سوق العقارات", lvl, PermissionCategory.PROPERTIES))
        }

        // 14. الوظائف والشركات والتوظيف (35 صلاحية)
        val jobItems = listOf(
            "JOB_VIEW_ALL" to "صلاحية عرض جميع الوظائف الشاغرة المعلنة",
            "JOB_VIEW_SPECIFIC" to "صلاحية فحص تفاصيل إعلان وظيفة محددة",
            "JOB_ADD" to "صلاحية نشر إعلان وظيفة جديدة يدوياً من لوحة التحكم",
            "JOB_EDIT" to "صلاحية تعديل المسمى الوظيفي والمتطلبات والراتب",
            "JOB_DELETE" to "صلاحية حذف إعلان الوظيفة نهائياً",
            "JOB_BLOCK" to "صلاحية حظر إعلان الوظيفة أو الشركة المعلنة",
            "JOB_UNBLOCK" to "صلاحية فك الحظر وإعادة تنشيط إعلان الوظيفة",
            "JOB_ACTIVATE" to "صلاحية قبول ونشر إعلان الوظيفة للباحثين عن عمل",
            "JOB_DEACTIVATE" to "صلاحية إغلاق الوظيفة (اكتفاء بالعدد المطلوب)",
            "JOB_PIN" to "صلاحية تثبيت الوظيفة في صدارة قائمة الوظائف الشاغرة",
            "JOB_UNPIN" to "صلاحية إلغاء تثبيت الوظيفة",
            "JOB_SET_VIP" to "صلاحية منح إعلان الوظيفة شارة VIP المميزة",
            "JOB_UNSET_VIP" to "صلاحية إلغاء شارة VIP عن الوظيفة",
            "JOB_VERIFY_COMPANY" to "صلاحية توثيق حساب الشركة المشغلة ومنحها شارة التوثيق",
            "JOB_UNVERIFY_COMPANY" to "صلاحية إلغاء توثيق الشركة المعلنة",
            "JOB_RECOMMEND" to "صلاحية ترشيح الوظيفة في الوظائف المميزة والموصى بها",
            "JOB_UNRECOMMEND" to "صلاحية إلغاء ترشيح الوظيفة",
            "JOB_CV_VIEW_ALL" to "صلاحية الاطلاع ومراجعة السير الذاتية (CV) للمتقدمين",
            "JOB_CV_DOWNLOAD" to "صلاحية تحميل وتنزيل ملفات السير الذاتية والمستندات",
            "JOB_APPLICATION_STATUS" to "صلاحية تغيير حالة المتقدم (مقبول مبدئياً، مقابلة، مرفوض)",
            "JOB_SALARY_RANGE_CONFIG" to "صلاحية تحديد وإخفاء نطاق الراتب المتوقع",
            "JOB_PASSWORD_RESET" to "صلاحية إعادة تعيين كلمة مرور حساب الشركة المعلنة",
            "JOB_PASSWORD_VIEW" to "صلاحية إظهار كلمة المرور الحالية للشركة المعلنة",
            "JOB_EXPERIENCE_LEVEL_TAG" to "صلاحية تصنيف مستوى الخبرة (مبتدئ، متوسط، محترف)",
            "JOB_EMPLOYMENT_TYPE" to "صلاحية تصنيف نوع الدوام (دوام كامل، جزئي، عن بعد)",
            "JOB_EDUCATION_DEGREE" to "صلاحية تحديد المؤهل العلمي المطلوب (ثانوية، بكالوريوس)",
            "JOB_EXPORT_APPLICANTS" to "صلاحية تصدير كشف كامل بأسماء المتقدمين وإيميلاتهم",
            "JOB_AUTOMATED_INTERVIEW_EMAIL" to "صلاحية إرسال بريد دعوة المقابلة التلقائي",
            "JOB_APPLICATION_DEADLINE" to "صلاحية ضبط آخر موعد لاستقبال طلبات التوظيف",
            "JOB_GENDER_PREFERENCE" to "صلاحية تحديد الفئة المطلوبة (ذكور، إناث، كلاهما)",
            "JOB_BENEFITS_TAGS" to "صلاحية إضافة مزايا العمل (تأمين طبي، سكن، مواصلات)",
            "JOB_INTERVIEW_QUIZ" to "صلاحية إضافة اختبار تقييم مبدئي للمتقدمين",
            "JOB_NOTIFICATION_ALERT" to "صلاحية إرسال إشعار فوري للباحثين عند نشر وظيفة تناسبهم",
            "JOB_SUBSCRIPTION_EXPIRY" to "صلاحية تمديد وتعديل اشتراك باقة التوظيف للشركة",
            "JOB_AUDIT_LOG" to "صلاحية مراجعة سجل تعديلات وحركات بوابة التوظيف"
        )
        jobItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(4, 5, 6, 17, 21, 22)) PermissionLevel.SENSITIVE else if (idx in listOf(2, 3, 7, 8, 11, 13, 15, 18, 19)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("14.${idx+1}", k, n, "تمكين $n لسوق الوظائف والشركات", "الوظائف والشركات", "بوابة التوظيف", lvl, PermissionCategory.JOBS))
        }

        // 15. التبويبات المخصصة والتنقل (30 صلاحية)
        val tabItems = listOf(
            "TAB_CREATE" to "صلاحية إنشاء وإضافة تبويب مخصص جديد في الواجهة",
            "TAB_SET_NAME" to "صلاحية كتابة وتعديل اسم التبويب المعروض للمستخدم",
            "TAB_SET_ICON" to "صلاحية اختيار وتغيير أيقونة التبويب",
            "TAB_SET_CONTENT_TYPE" to "صلاحية تحديد نوع محتوى التبويب (نص، روابط، نماذج)",
            "TAB_SET_FIELDS_COUNT" to "صلاحية تحديد عدد الحقول داخل التبويب",
            "TAB_SET_FIELDS_ORDER" to "صلاحية تحديد ترتيب تسلسل ظهور الحقول",
            "TAB_SET_SIZE" to "صلاحية ضبط مقاس وحجم التبويب في الشاشة",
            "TAB_SHOW_ALL" to "صلاحية إظهار التبويب لكافة مستخدمي التطبيق",
            "TAB_SHOW_USERS" to "صلاحية إظهار التبويب للعملاء والمواطنين فقط",
            "TAB_SHOW_PROVIDERS" to "صلاحية إظهار التبويب للفنيين ومقدمي الخدمات فقط",
            "TAB_SHOW_STORES" to "صلاحية إظهار التبويب للمحلات والمراكز التجارية فقط",
            "TAB_SHOW_RESTAURANTS" to "صلاحية إظهار التبويب للمطاعم والكافيهات فقط",
            "TAB_SHOW_MEDICAL" to "صلاحية إظهار التبويب للمراكز الطبية والعيادات فقط",
            "TAB_SHOW_PROPERTIES" to "صلاحية إظهار التبويب لمالكي العقارات فقط",
            "TAB_SHOW_JOBS" to "صلاحية إظهار التبويب لمعلني الوظائف فقط",
            "TAB_SHOW_AREA" to "صلاحية تقييد إظهار التبويب لمحافظة أو مدينة معينة",
            "TAB_EDIT" to "صلاحية تعديل بيانات وإعدادات تبويب موجود",
            "TAB_DELETE" to "صلاحية حذف التبويب نهائياً ومحتوياته",
            "TAB_ACTIVATE" to "صلاحية تفعيل وجعل التبويب مرئياً للجميع",
            "TAB_DEACTIVATE" to "صلاحية تعطيل وإخفاء التبويب مؤقتاً",
            "TAB_REORDER" to "صلاحية تغيير ترتيب موقع التبويب بين التبويبات الأخرى",
            "TAB_RESIZE" to "صلاحية تعديل أبعاد وأحجام العناصر داخل التبويب",
            "TAB_SET_COLOR" to "صلاحية تخصيص لون خلفية وأزرار التبويب",
            "TAB_SET_FONT" to "صلاحية تغيير نوع خط النصوص داخل التبويب",
            "TAB_ADD_FIELD" to "صلاحية إضافة حقل إدخال جديد للتبويب",
            "TAB_EDIT_FIELD" to "صلاحية تعديل خيارات الحقل المضاف بالتبويب",
            "TAB_DELETE_FIELD" to "صلاحية حذف حقل معين من داخل التبويب",
            "TAB_BADGE_ALERT" to "صلاحية إضافة شارة تنبيهية حمراء فوق التبويب",
            "TAB_EXPORT_LAYOUT" to "صلاحية تصدير تخطيط وهيكلية التبويبات المخصصة",
            "TAB_IMPORT_LAYOUT" to "صلاحية استيراد تصميم وهيكلية تبويبات جاهزة"
        )
        tabItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(0, 17, 18, 19)) PermissionLevel.SENSITIVE else if (idx in listOf(1, 2, 7, 8, 9, 10, 15, 16)) PermissionLevel.ADVANCED else PermissionLevel.BASIC
            list.add(AdminPermissionItem("15.${idx+1}", k, n, "تمكين $n للتبويبات المخصصة", "التبويبات المخصصة", "التنقل والواجهات", lvl, PermissionCategory.CUSTOM_TABS))
        }

        // 16. الرقابة الأمنية وحظر الحسابات وفحص الثغرات (30 صلاحية)
        val secItems = listOf(
            "SEC_VIEW_AUDIT_LOGS" to "صلاحية الاطلاع على سجل العمليات والتدقيق الأمني العام",
            "SEC_SCAN_VULNERABILITIES" to "صلاحية تشغيل الفحص الأمني التلقائي وكشف الثغرات",
            "SEC_ENFORCE_ENCRYPTION" to "صلاحية فرض وتدقيق تشفير البيانات الحساسة (AES-256)",
            "SEC_BLOCK_IP_DEVICE" to "صلاحية حظر عنوان IP أو معرف الجهاز المريب نهائياً",
            "SEC_UNBLOCK_IP_DEVICE" to "صلاحية فك الحظر عن عنوان IP أو جهاز تم حظره",
            "SEC_RATE_LIMIT_MANAGE" to "صلاحية ضبط جدران الحماية ضد هجمات DDoS والطلبات الكثيفة",
            "SEC_MEDIA_SCAN_GUARD" to "صلاحية تفعيل الفاحص الآلي للصور ومنع المحتوى المحظور",
            "SEC_MEDIA_METADATA_STRIP" to "صلاحية تفريغ البيانات الوصفية (EXIF) من الصور المرفوعة لحماية الخصوصية",
            "SEC_SESSION_TERMINATE_ALL" to "صلاحية تسجيل الخروج الإجباري لكافة الجلسات والأجهزة",
            "SEC_FORCE_PASSWORD_CHANGE" to "صلاحية إجبار مستخدم أو فني على تغيير كلمة مروره",
            "SEC_TWO_FACTOR_TOGGLE" to "صلاحية تفعيل التحقق بخطوتين (2FA) لحسابات الإدارة",
            "SEC_SUPERVISORS_MANAGE" to "صلاحية إضافة وتعديل المشرفين وتعيين صلاحياتهم",
            "SEC_SUPERVISORS_DELETE" to "صلاحية حذف حساب مشرف وسحب كامل صلاحياته فورياً",
            "SEC_ROOT_JAILBREAK_DETECT" to "صلاحية منع تشغيل التطبيق على الهواتف المكسورة الحماية (Rooted)",
            "SEC_SCREENSHOT_PREVENT" to "صلاحية منع التقاط الشاشة في الصفحات الحساسة",
            "SEC_EXPORT_SECURITY_REPORT" to "صلاحية تصدير التقرير الأمني الرسمي لسجلات النظام",
            "SEC_FAILED_LOGINS_MONITOR" to "صلاحية مراقبة وتتبع محاولات تسجيل الدخول الفاشلة",
            "SEC_SUSPICIOUS_ALERTS" to "صلاحية استلام تنبيهات الأنشطة المشبوهة اللحظية",
            "SEC_CLEAR_TEMP_CACHE" to "صلاحية تفريغ الذاكرة المؤقتة ومسح الملفات غير الآمنة",
            "SEC_API_KEY_REVOKE" to "صلاحية إبطال وتوليد مفاتيح الربط السحابية (API Keys)",
            "SEC_FIREBASE_RULES_AUDIT" to "صلاحية تدقيق قواعد أمان وتصاريح Firestore و Cloud Storage",
            "SEC_BACKDOOR_ACCESS" to "صلاحية الدخول لبوابة الإدارة العليا والتحكم الشامل",
            "SEC_DATA_RETENTION_POLICY" to "صلاحية ضبط سياسات الاحتفاظ بالبيانات وحذف السجلات القديمة",
            "SEC_GEO_IP_RESTRICTIONS" to "صلاحية تقييد استخدام لوحة التحكم لدول أو نطاقات محددة",
            "SEC_DATABASE_LEAK_TEST" to "صلاحية فحص واختبار تسريب البيانات وتأمين المنافذ",
            "SEC_TAMPER_PROTECTION" to "صلاحية تفعيل حماية توقيع التطبيق ومكافحة التعديل",
            "SEC_DEVICE_INTEGRITY_CHECK" to "صلاحية فحص نزاهة جهاز المستخدم عبر Play Integrity",
            "SEC_HONEYPOT_ALERTS" to "صلاحية تفعيل مصائد الاختراق لكشف محاولات التسلل",
            "SEC_AUDIT_EXPORTS_LOG" to "صلاحية تسجيل وتتبع كل عملية تصدير للبيانات",
            "SEC_EMERGENCY_LOCKDOWN" to "صلاحية الإغلاق الأمني الشامل للنظام في حالات الطوارئ"
        )
        secItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(1, 3, 8, 11, 12, 19, 21, 29)) PermissionLevel.SENSITIVE else if (idx in listOf(0, 2, 4, 5, 6, 7, 10, 13, 20)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("16.${idx+1}", k, n, "تمكين $n للرقابة والحماية", "الرقابة والحماية الأمنية", "الأمان والرقابة", lvl, PermissionCategory.SECURITY_AUDIT))
        }

        // 17. الإدارة المالية والاشتراكات والمدفوعات (25 صلاحية)
        val finItems = listOf(
            "FIN_VIEW_DASHBOARD" to "صلاحية استعراض لوحة الإحصائيات المالية والإيرادات",
            "FIN_SUBSCRIPTION_PLANS" to "صلاحية إنشاء وتعديل باقات وأسعار الاشتراكات الشهرية والسنوية",
            "FIN_COLLECT_COMMISSIONS" to "صلاحية تحصيل وتسجيل عمولات الفنيين والمنشآت",
            "FIN_PAYMENT_GATEWAYS" to "صلاحية ضبط بوابات الدفع الإلكتروني والمحافظ (كريمي، ون كاش)",
            "FIN_INVOICES_MANAGE" to "صلاحية إصدار ومراجعة الفواتير وسندات القبض الإلكترونية",
            "FIN_MANUAL_PAYMENT_APPROVE" to "صلاحية تدقيق وقبول إشعارات التحويل البنكي اليدوية",
            "FIN_REFUND_TRANSACTIONS" to "صلاحية اعتماد وتنفيذ عمليات استرجاع المبالغ للعملاء",
            "FIN_DISCOUNT_PROMO_CODES" to "صلاحية إنشاء وإدارة أكواد وقسائم الخصم الترويجية",
            "FIN_WALLET_BALANCE_ADJUST" to "صلاحية شحن أو تعديل رصيد المحفظة لحساب أي مستخدم أو فني",
            "FIN_TRANSACTION_LOGS" to "صلاحية تدقيق ومراجعة سجل كافة الحركات والتحويلات المالية",
            "FIN_EXPORT_FINANCIAL_SHEET" to "صلاحية تصدير الكشوفات والتقارير المالية كملف Excel",
            "FIN_TAX_SETTING" to "صلاحية ضبط نسب الضرائب والرسوم الحكومية إن وجدت",
            "FIN_SETTLEMENTS_PAYOUT" to "صلاحية اعتماد وتصفية مستحقات مقدمي الخدمات",
            "FIN_BANK_ACCOUNTS_CONFIG" to "صلاحية تعديل أرقام الحسابات البنكية لاستقبال الدفعات",
            "FIN_SUBSCRIPTION_REMINDERS" to "صلاحية إرسال إشعارات التذكير التلقائي بقرب انتهاء الاشتراك",
            "FIN_VIP_PACKAGES_PRICE" to "صلاحية تسعير باقات التمييز وعضوية VIP الذهبية",
            "FIN_REVENUE_CHART_ANALYTICS" to "صلاحية مشاهدة الرسوم البيانية لمعدل نمو الأرباح",
            "FIN_OVERDUE_PENALTIES" to "صلاحية إدارة وتطبيق غرامات التأخير في سداد العمولات",
            "FIN_RECEIPT_TEMPLATE_EDIT" to "صلاحية تخصيص قالب وشعار سندات القبض المطبوعة",
            "FIN_CURRENCY_EXCHANGE_RATES" to "صلاحية تحديث أسعار صرف العملات المعتمدة بالتطبيق",
            "FIN_RECURRING_BILLING" to "صلاحية تفعيل نظام الخصم والتجديد التلقائي للاشتراك",
            "FIN_PROVIDER_EARNINGS_LIMIT" to "صلاحية ضبط سقف السحب اليومي والأسبوعي للأرصدة",
            "FIN_AUDIT_DISCREPANCIES" to "صلاحية مراجعة الفروقات المحاسبية ومطابقة الأرصدة",
            "FIN_AGENT_COMMISSION_SHARE" to "صلاحية إدارة حصص ونسب الوكلاء والمسوقين",
            "FIN_FINANCIAL_LOCK_MONTH" to "صلاحية إقفال الشهر المالي ومنع التعديل على القيود"
        )
        finItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(1, 3, 5, 6, 8, 12, 13, 24)) PermissionLevel.SENSITIVE else if (idx in listOf(0, 2, 4, 7, 9, 10, 15)) PermissionLevel.ADVANCED else PermissionLevel.MEDIUM
            list.add(AdminPermissionItem("17.${idx+1}", k, n, "تمكين $n للمنظومة المالية", "الإدارة المالية والاشتراكات", "المالية والمدفوعات", lvl, PermissionCategory.FINANCIAL))
        }

        // 18. النظام وقواعد البيانات السحابية (13 صلاحية)
        val sysItems = listOf(
            "SYS_BACKUP_DATABASE" to "صلاحية إنشاء نسخة احتياطية سحابية فورية لكامل قاعدة البيانات",
            "SYS_RESTORE_DATABASE" to "صلاحية استرجاع قاعدة البيانات من نسخة احتياطية سابقة",
            "SYS_MAINTENANCE_MODE_TOGGLE" to "صلاحية تفعيل وإلغاء وضع الصيانة العام للتطبيق",
            "SYS_FORCE_UPDATE_APP" to "صلاحية فرض التحديث الإجباري للتطبيق لجميع المستخدمين",
            "SYS_AUTO_DISPATCH_CONFIG" to "صلاحية ضبط خوارزميات التوجيه الذكي وتوزيع الطلبات",
            "SYS_API_KEYS_CONFIG" to "صلاحية إدارة مفاتيح الخدمات السحابية (Google Maps, Firebase, AI)",
            "SYS_STORAGE_CLEANUP" to "صلاحية تنظيف وضغط الملفات والبيانات المؤقتة من السيرفر",
            "SYS_APP_CONFIG_PARAMS" to "صلاحية تعديل إعدادات النظام العامة (اسم التطبيق، أرقام الدعم)",
            "SYS_CRON_JOBS_SCHEDULE" to "صلاحية جدولة وإدارة المهام التلقائية الدورية في السيرفر",
            "SYS_SERVER_HEALTH_MONITOR" to "صلاحية مراقبة كفاءة واستقرار السيرفر وسرعة الاستجابة",
            "SYS_EXPORT_ALL_DATA" to "صلاحية تصدير أرشيف كامل لجميع جداول ومحتويات التطبيق",
            "SYS_FACTORY_RESET_MODULE" to "صلاحية إعادة تهيئة وتصفير قسم معين للوضع المصنعي الافتراضي",
            "SYS_RESTART_SERVICES" to "صلاحية إعادة تشغيل الخدمات السحابية ومزامنة المستمعين"
        )
        sysItems.forEachIndexed { idx, (k, n) ->
            val lvl = if (idx in listOf(0, 1, 2, 3, 5, 10, 11)) PermissionLevel.SENSITIVE else PermissionLevel.ADVANCED
            list.add(AdminPermissionItem("18.${idx+1}", k, n, "تمكين $n لإدارة السيرفر والنظام", "النظام وقواعد البيانات", "السيرفر وقواعد البيانات", lvl, PermissionCategory.SYSTEM_BACKUP))
        }

        list
    }

    fun getByCategory(category: PermissionCategory): List<AdminPermissionItem> {
        return allPermissions.filter { it.category == category }
    }

    fun getByLevel(level: PermissionLevel): List<AdminPermissionItem> {
        return allPermissions.filter { it.level == level }
    }

    fun searchPermissions(query: String): List<AdminPermissionItem> {
        if (query.isBlank()) return allPermissions
        val clean = query.trim().lowercase()
        return allPermissions.filter {
            it.name.lowercase().contains(clean) ||
            it.key.lowercase().contains(clean) ||
            it.description.lowercase().contains(clean) ||
            it.targetGroup.lowercase().contains(clean) ||
            it.category.arabicTitle.lowercase().contains(clean)
        }
    }
}
