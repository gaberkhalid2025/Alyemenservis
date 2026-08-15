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
    NOTIFICATIONS("MANAGE_NOTIFICATIONS", "صلاحيات الإشعارات", "🔔", 25),
    BANNERS("MANAGE_BANNERS", "صلاحيات البنرات الإعلانية", "🖼️", 24),
    REGISTRATION_FORMS("MANAGE_REG_FORMS", "صلاحيات استمارات التسجيل", "📝", 12),
    BOOKING_FORMS("MANAGE_BOOKINGS", "صلاحيات استمارات الحجز", "📅", 24),
    QUICK_SERVICE("MANAGE_QUICK_SERVICE", "صلاحيات استمارة اطلب خدمتك الآن", "⚡", 19),
    CHAT("MANAGE_ADVANCED_CHAT", "صلاحيات المحادثات الفورية", "💬", 45),
    THEMES_ICONS("MANAGE_THEMES", "صلاحيات الأيقونات الذهبية والثيمات", "🎨", 13),
    NEW_SECTIONS("MANAGE_NEW_SECTIONS", "صلاحيات الأقسام الجديدة", "📂", 21),
    MAPS("MANAGE_MAP", "صلاحيات شاشة الخرائط", "🗺️", 29),
    STORES("MANAGE_STORES", "صلاحيات إدارة المحلات والمراكز", "🏬", 17),
    RESTAURANTS("MANAGE_RESTAURANTS", "صلاحيات إدارة المطاعم", "🍽️", 17),
    MEDICAL("MANAGE_MEDICAL", "صلاحيات إدارة المراكز الطبية", "🏥", 17),
    PROPERTIES("MANAGE_PROPERTIES", "صلاحيات إدارة العقارات", "🏢", 17),
    JOBS("MANAGE_JOBS", "صلاحيات إدارة الوظائف", "💼", 13),
    CUSTOM_TABS("MANAGE_CUSTOM_TABS", "صلاحيات التبويبات المخصصة", "📑", 27)
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
        
        // 1. الإشعارات (25 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("1.1", "NOTIF_SEND_ALL", "صلاحية إرسال إشعار للجميع", "إرسال إشعار تنبيهي أو ترويجي لجميع مستخدمي التطبيق المسجلين دون استثناء", "جميع مستخدمي التطبيق", "شامل لكامل قاعدة المستخدمين", PermissionLevel.ADVANCED, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.2", "NOTIF_SEND_USERS", "صلاحية إرسال إشعار للمستخدمين فقط", "إرسال إشعار مخصص للعملاء والمواطنين المسجلين في التطبيق فقط", "العملاء والمواطنين المسجلين", "يقتصر على حساب العملاء فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.3", "NOTIF_SEND_PROVIDERS", "صلاحية إرسال إشعار للفنيين فقط", "إرسال إشعار لجميع الفنيين ومقدمي الخدمات المسجلين في التطبيق", "جميع الفنيين ومقدمي الخدمات", "يقتصر على حسابات الفنيين فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.4", "NOTIF_SEND_STORES", "صلاحية إرسال إشعار للمحلات فقط", "إرسال إشعار لجميع المحلات والمراكز التجارية المسجلة في التطبيق", "جميع المحلات والمراكز التجارية", "يقتصر على حسابات المحلات فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.5", "NOTIF_SEND_RESTAURANTS", "صلاحية إرسال إشعار للمطاعم فقط", "إرسال إشعار لجميع المطاعم والكافيهات المسجلة في التطبيق", "جميع المطاعم والكافيهات", "يقتصر على حسابات المطاعم فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.6", "NOTIF_SEND_MEDICAL", "صلاحية إرسال إشعار للمراكز الطبية فقط", "إرسال إشعار لجميع المراكز الطبية والعيادات المسجلة في التطبيق", "جميع المراكز الطبية والعيادات", "يقتصر على حسابات المراكز الطبية فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.7", "NOTIF_SEND_PROPERTIES", "صلاحية إرسال إشعار للعقارات فقط", "إرسال إشعار لجميع مالكي ومعلني العقارات المسجلين في التطبيق", "جميع مالكي ومعلني العقارات", "يقتصر على حسابات العقارات فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.8", "NOTIF_SEND_JOBS", "صلاحية إرسال إشعار لمعلني الوظائف فقط", "إرسال إشعار لجميع الشركات والمعلنين عن وظائف في التطبيق", "جميع الشركات المعلنة عن وظائف", "يقتصر على حسابات معلني الوظائف فقط", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.9", "NOTIF_SEND_SUPERVISORS", "صلاحية إرسال إشعار للمشرفين فقط", "إرسال إشعار لجميع المشرفين والمراقبين في نظام إدارة التطبيق", "جميع المشرفين والمراقبين", "يقتصر على حسابات المشرفين فقط", PermissionLevel.ADVANCED, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.10", "NOTIF_SEND_USER_SPECIFIC", "صلاحية إرسال إشعار لمستخدم محدد", "إرسال إشعار موجه لمستخدم معين عن طريق رقم هاتفه المسجل في التطبيق", "مستخدم محدد برقم هاتفه", "يقتصر على مستخدم واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.11", "NOTIF_SEND_PROVIDER_SPECIFIC", "صلاحية إرسال إشعار لفني محدد", "إرسال إشعار موجه لفني معين عن طريق رقم هاتفه المسجل في التطبيق", "فني محدد برقم هاتفه", "يقتصر على فني واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.12", "NOTIF_SEND_STORE_SPECIFIC", "صلاحية إرسال إشعار لمحل محدد", "إرسال إشعار موجه لمحل معين عن طريق رقم هاتفه أو معرفه في التطبيق", "محل محدد برقم هاتفه أو معرفه", "يقتصر على محل واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.13", "NOTIF_SEND_RESTAURANT_SPECIFIC", "صلاحية إرسال إشعار لمطعم محدد", "إرسال إشعار موجه لمطعم معين عن طريق رقم هاتفه أو معرفه في التطبيق", "مطعم محدد برقم هاتفه أو معرفه", "يقتصر على مطعم واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.14", "NOTIF_SEND_MEDICAL_SPECIFIC", "صلاحية إرسال إشعار لمركز طبي محدد", "إرسال إشعار موجه لمركز طبي معين عن طريق رقم هاتفه أو معرفه في التطبيق", "مركز طبي محدد برقم هاتفه أو معرفه", "يقتصر على مركز طبي واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.15", "NOTIF_SEND_PROPERTY_SPECIFIC", "صلاحية إرسال إشعار لعقار محدد", "إرسال إشعار موجه لعقار معين عن طريق رقم هاتف المالك أو معرف العقار في التطبيق", "عقار محدد برقم هاتف المالك أو معرف العقار", "يقتصر على عقار واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.16", "NOTIF_SEND_JOB_SPECIFIC", "صلاحية إرسال إشعار لمعلن وظيفة محدد", "إرسال إشعار موجه لمعلن وظيفة معين عن طريق رقم هاتفه أو معرفه في التطبيق", "معلن وظيفة محدد برقم هاتفه أو معرفه", "يقتصر على معلن وظيفة واحد محدد", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.17", "NOTIF_SEND_SUPERVISOR_SPECIFIC", "صلاحية إرسال إشعار لمشرف محدد", "إرسال إشعار موجه لمشرف معين عن طريق اسمه أو معرفه في نظام الإدارة", "مشرف محدد باسمه أو معرفه", "يقتصر على مشرف واحد محدد", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.18", "NOTIF_SEND_AREA", "صلاحية إرسال إشعار لمنطقة محددة", "إرسال إشعار لجميع المستخدمين المسجلين في محافظة أو مدينة معينة", "جميع المستخدمين في منطقة جغرافية محددة", "يقتصر على منطقة جغرافية معينة", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.19", "NOTIF_SEND_CATEGORY", "صلاحية إرسال إشعار لقسم محدد", "إرسال إشعار لجميع الفنيين أو مقدمي الخدمات المسجلين في قسم معين", "فنيو قسم محدد", "يقتصر على قسم خدمي معين", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.20", "NOTIF_SCHEDULE", "صلاحية جدولة إشعار", "تحديد وقت وتاريخ محدد مسبقاً لإرسال الإشعار بشكل آلي في المستقبل", "جميع الفئات المستهدفة مع تحديد الوقت", "يحدد وقت وتاريخ الإرسال المستقبلي", PermissionLevel.ADVANCED, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.21", "NOTIF_SET_EXPIRY", "صلاحية تحديد مدة صلاحية الإشعار", "تحديد المدة الزمنية التي يبقى فيها الإشعار صالحاً للعرض في التطبيق قبل انتهاء صلاحيته", "جميع الفئات المستهدفة مع تحديد المدة", "يحدد عمر الإشعار الافتراضي", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.22", "NOTIF_DELETE_SINGLE", "صلاحية حذف إشعار", "حذف إشعار مرسل بشكل نهائي من سجل الإشعارات ومن أجهزة المستخدمين", "جميع الإشعارات المرسلة", "يسمح بحذف إشعار واحد", PermissionLevel.MEDIUM, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.23", "NOTIF_DELETE_ALL", "صلاحية حذف جميع الإشعارات", "حذف جميع الإشعارات المرسلة دفعة واحدة من سجل الإشعارات ومن أجهزة المستخدمين", "جميع الإشعارات المرسلة", "يسمح بحذف جميع الإشعارات دفعة واحدة", PermissionLevel.SENSITIVE, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.24", "NOTIF_VIEW_LOGS", "صلاحية عرض سجل الإشعارات", "عرض جميع الإشعارات المرسلة مسبقاً مع تفاصيلها وتواريخ إرسالها وحالتها", "جميع الإشعارات المرسلة", "يسمح بعرض سجل الإشعارات بالكامل", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS),
            AdminPermissionItem("1.25", "NOTIF_FILTER", "صلاحية تصفية الإشعارات", "تصفية سجل الإشعارات حسب الفئة المستهدفة، التاريخ، الحالة، أو أي معيار آخر", "سجل الإشعارات", "يسمح بتصفية وعرض الإشعارات حسب معايير محددة", PermissionLevel.BASIC, PermissionCategory.NOTIFICATIONS)
        ))

        // 2. البنرات الإعلانية (24 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("2.1", "BANNER_CREATE", "صلاحية إنشاء بنر جديد", "إضافة بنر إعلاني جديد في التطبيق بكافة محتوياته وإعداداته", "جميع مستخدمي التطبيق", "يسمح بإنشاء بنر إعلاني جديد", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.2", "BANNER_CHOOSE_TYPE", "صلاحية اختيار نوع البنر", "تحديد نوع البنر الإعلاني (نصي فقط، صورة ثابتة، صورة متحركة، فيديو)", "البنر الجديد", "يحدد نوع محتوى البنر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.3", "BANNER_UPLOAD_IMAGE", "صلاحية رفع صورة", "رفع صورة من معرض الهاتف أو الجهاز لاستخدامها كبنر إعلاني", "محتوى البنر", "يسمح برفع صورة واحدة أو أكثر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.4", "BANNER_UPLOAD_VIDEO", "صلاحية رفع فيديو", "رفع فيديو من معرض الهاتف أو الجهاز لاستخدامه كبنر إعلاني", "محتوى البنر", "يسمح برفع فيديو واحد أو أكثر", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.5", "BANNER_SET_DURATION", "صلاحية تحديد مدة العرض", "تحديد المدة الزمنية لظهور البنر على الشاشة بالثواني (على سبيل المثال: 3 ثواني، 5 ثواني، 10 ثواني)", "إعدادات عرض البنر", "يحدد مدة عرض البنر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.6", "BANNER_SET_STYLE", "صلاحية تحديد نمط العرض", "تحديد طريقة انتقال وظهور البنر (انزلاق من اليمين، انزلاق من اليسار، تلاشي تدريجي، وميض، تمرير أفقي، تمرير عمودي)", "إعدادات عرض البنر", "يحدد نمط حركة وظهور البنر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.7", "BANNER_SET_LOCATION", "صلاحية تحديد موقع البنر", "تحديد مكان ظهور البنر على الشاشة (أعلى الشاشة، أسفل الشاشة، منتصف الشاشة، أعلى يسار، أعلى يمين، أسفل يسار، أسفل يمين)", "إعدادات عرض البنر", "يحدد موقع البنر على الشاشة", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.8", "BANNER_SET_SIZE", "صلاحية تحديد حجم البنر", "تحديد حجم البنر المعروض (صغير جداً، صغير، متوسط، كبير، كبير جداً، عرض كامل للشاشة، ارتفاع كامل للشاشة)", "إعدادات عرض البنر", "يحدد أبعاد وحجم البنر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.9", "BANNER_TARGET_CUSTOM", "صلاحية توجيه البنر لفئة محددة", "إظهار البنر لفئة محددة من المستخدمين فقط بناءً على نوع حسابهم", "فئة محددة من المستخدمين", "يحدد الفئة المستهدفة للبنر", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.10", "BANNER_SHOW_ALL", "صلاحية إظهار البنر للجميع", "إظهار البنر لجميع مستخدمي التطبيق بدون استثناء", "جميع مستخدمي التطبيق", "شامل لكامل قاعدة المستخدمين", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.11", "BANNER_SHOW_USERS", "صلاحية إظهار البنر للمستخدمين فقط", "إظهار البنر للعملاء والمواطنين المسجلين فقط", "العملاء والمواطنين المسجلين", "يقتصر على حسابات العملاء", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.12", "BANNER_SHOW_PROVIDERS", "صلاحية إظهار البنر للفنيين فقط", "إظهار البنر للفنيين ومقدمي الخدمات فقط", "الفنيين ومقدمي الخدمات", "يقتصر على حسابات الفنيين", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.13", "BANNER_SHOW_STORES", "صلاحية إظهار البنر للمحلات فقط", "إظهار البنر للمحلات والمراكز التجارية فقط", "المحلات والمراكز التجارية", "يقتصر على حسابات المحلات", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.14", "BANNER_SHOW_RESTAURANTS", "صلاحية إظهار البنر للمطاعم فقط", "إظهار البنر للمطاعم والكافيهات فقط", "المطاعم والكافيهات", "يقتصر على حسابات المطاعم", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.15", "BANNER_SHOW_MEDICAL", "صلاحية إظهار البنر للمراكز الطبية فقط", "إظهار البنر للمراكز الطبية والعيادات فقط", "المراكز الطبية والعيادات", "يقتصر على حسابات المراكز الطبية", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.16", "BANNER_SHOW_PROPERTIES", "صلاحية إظهار البنر للعقارات فقط", "إظهار البنر لمالكي ومعلني العقارات فقط", "مالكي ومعلني العقارات", "يقتصر على حسابات العقارات", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.17", "BANNER_SHOW_JOBS", "صلاحية إظهار البنر لمعلني الوظائف فقط", "إظهار البنر للشركات والمعلنين عن وظائف فقط", "الشركات المعلنة عن وظائف", "يقتصر على حسابات معلني الوظائف", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.18", "BANNER_SHOW_AREA", "صلاحية إظهار البنر لمنطقة محددة", "إظهار البنر للمستخدمين المسجلين في محافظة أو مدينة محددة فقط", "مستخدمون في منطقة جغرافية محددة", "يقتصر على منطقة جغرافية معينة", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.19", "BANNER_SHOW_CATEGORY", "صلاحية إظهار البنر لقسم محدد", "إظهار البنر للفنيين أو مقدمي الخدمات في قسم خدمي محدد فقط", "فنيو قسم محدد", "يقتصر على قسم خدمي معين", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.20", "BANNER_EDIT", "صلاحية تعديل البنر", "تعديل بيانات ومحتويات وإعدادات بنر موجود مسبقاً", "البنر المحدد", "يسمح بتعديل بنر واحد", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.21", "BANNER_DELETE", "صلاحية حذف البنر", "حذف بنر نهائياً من النظام ومن جميع أجهزة المستخدمين", "البنر المحدد", "يسمح بحذف بنر واحد", PermissionLevel.MEDIUM, PermissionCategory.BANNERS),
            AdminPermissionItem("2.22", "BANNER_REORDER", "صلاحية ترتيب البنرات", "رفع أو خفض ترتيب ظهور البنرات لتحديد أسبقية العرض (البنر الأول يظهر أولاً)", "جميع البنرات", "يسمح بتغيير ترتيب البنرات", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.23", "BANNER_PREVIEW", "صلاحية معاينة البنر", "معاينة البنر بشكل حقيقي قبل نشره وإرساله للمستخدمين", "البنر الجديد أو المعدل", "يسمح بمعاينة البنر", PermissionLevel.BASIC, PermissionCategory.BANNERS),
            AdminPermissionItem("2.24", "BANNER_VIEW_STATS", "صلاحية عرض إحصائيات البنر", "مشاهدة عدد المشاهدات والنقرات والتفاعل مع كل بنر إعلاني", "جميع البنرات المنشورة", "يسمح بعرض إحصائيات البنرات", PermissionLevel.MEDIUM, PermissionCategory.BANNERS)
        ))

        // 3. استمارات التسجيل (12 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("3.1", "REG_TOGGLE_PROVIDERS", "صلاحية تفعيل/تعطيل استمارة الفنيين", "تمكين أو تعطيل إمكانية تسجيل فنيين جدد من خلال استمارة التسجيل المخصصة للفنيين", "الفنيين الجدد", "يتحكم في استمارة تسجيل الفنيين", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.2", "REG_TOGGLE_STORES", "صلاحية تفعيل/تعطيل استمارة المحلات", "تمكين أو تعطيل إمكانية تسجيل محلات جديدة من خلال استمارة التسجيل المخصصة للمحلات", "المحلات الجديدة", "يتحكم في استمارة تسجيل المحلات", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.3", "REG_TOGGLE_RESTAURANTS", "صلاحية تفعيل/تعطيل استمارة المطاعم", "تمكين أو تعطيل إمكانية تسجيل مطاعم جديدة من خلال استمارة التسجيل المخصصة للمطاعم", "المطاعم الجديدة", "يتحكم في استمارة تسجيل المطاعم", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.4", "REG_TOGGLE_MEDICAL", "صلاحية تفعيل/تعطيل استمارة المراكز الطبية", "تمكين أو تعطيل إمكانية تسجيل مراكز طبية جديدة من خلال استمارة التسجيل المخصصة للمراكز الطبية", "المراكز الطبية الجديدة", "يتحكم في استمارة تسجيل المراكز الطبية", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.5", "REG_TOGGLE_PROPERTIES", "صلاحية تفعيل/تعطيل استمارة العقارات", "تمكين أو تعطيل إمكانية تسجيل عقارات جديدة من خلال استمارة التسجيل المخصصة للعقارات", "العقارات الجديدة", "يتحكم في استمارة تسجيل العقارات", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.6", "REG_TOGGLE_JOBS", "صلاحية تفعيل/تعطيل استمارة الوظائف", "تمكين أو تعطيل إمكانية إضافة وظائف جديدة من خلال استمارة الوظائف المخصصة", "الوظائف الجديدة", "يتحكم في استمارة إضافة الوظائف", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.7", "REG_SET_REQUIRED_FIELDS", "صلاحية تحديد الحقول الإجبارية لكل استمارة", "تحديد الحقول التي تصبح إجبارية ويجب على المستخدم تعبئتها قبل إكمال التسجيل، مع إمكانية التخصيص لكل استمارة على حدة", "جميع استمارات التسجيل", "يسمح بتخصيص الحقول الإجبارية لكل استمارة", PermissionLevel.ADVANCED, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.8", "REG_ADD_FIELD", "صلاحية إضافة حقل جديد لكل استمارة", "إضافة حقل جديد إلى استمارة التسجيل لزيادة المعلومات المطلوبة من المستخدم، مع إمكانية التخصيص لكل استمارة على حدة", "جميع استمارات التسجيل", "يسمح بإضافة حقل جديد لكل استمارة", PermissionLevel.ADVANCED, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.9", "REG_EDIT_FIELD", "صلاحية تعديل حقل لكل استمارة", "تعديل اسم أو نوع أو خصائص حقل موجود في استمارة التسجيل، مع إمكانية التخصيص لكل استمارة على حدة", "جميع استمارات التسجيل", "يسمح بتعديل حقل موجود لكل استمارة", PermissionLevel.ADVANCED, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.10", "REG_DELETE_FIELD", "صلاحية حذف حقل لكل استمارة", "حذف حقل من استمارة التسجيل بشكل نهائي، مع إمكانية التخصيص لكل استمارة على حدة", "جميع استمارات التسجيل", "يسمح بحذف حقل لكل استمارة", PermissionLevel.ADVANCED, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.11", "REG_SET_FIELD_TYPE", "صلاحية تحديد نوع الحقل", "تحديد نوع الحقل المضاف أو المعدل (نص عادي، رقم هاتف، بريد إلكتروني، صورة، قائمة منسدلة، اختيار متعدد، تاريخ، وقت، رقم، عنوان، رابط)", "جميع استمارات التسجيل", "يسمح بتحديد نوع الحقل", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS),
            AdminPermissionItem("3.12", "REG_REORDER_FIELDS", "صلاحية تحديد ترتيب الحقول", "تحديد ترتيب ظهور الحقول في استمارة التسجيل (الحقل الأول يظهر أولاً، وهكذا)", "جميع استمارات التسجيل", "يسمح بترتيب الحقول", PermissionLevel.MEDIUM, PermissionCategory.REGISTRATION_FORMS)
        ))

        // 4. استمارات الحجز (24 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("4.1", "BOOKING_TOGGLE_SYSTEM", "صلاحية تفعيل/تعطيل نظام الحجوزات", "تمكين أو تعطيل نظام الحجوزات بالكامل في التطبيق", "جميع المستخدمين", "يتحكم في نظام الحجوزات بالكامل", PermissionLevel.SENSITIVE, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.2", "BOOKING_CUSTOM_NAME_LABEL", "صلاحية تخصيص اسم حقل الاسم", "تغيير تسمية حقل اسم العميل في استمارة الحجز (مثلاً: الاسم الكامل، اسم العميل، المستخدم)", "استمارة الحجز", "يسمح بتغيير تسمية حقل الاسم", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.3", "BOOKING_CUSTOM_PHONE_LABEL", "صلاحية تخصيص اسم حقل الهاتف", "تغيير تسمية حقل رقم الهاتف في استمارة الحجز (مثلاً: رقم الجوال، الهاتف المحمول، رقم التواصل)", "استمارة الحجز", "يسمح بتغيير تسمية حقل الهاتف", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.4", "BOOKING_CUSTOM_AREA_LABEL", "صلاحية تخصيص اسم حقل المنطقة", "تغيير تسمية حقل المنطقة أو الموقع في استمارة الحجز (مثلاً: المنطقة، المدينة، الموقع)", "استمارة الحجز", "يسمح بتغيير تسمية حقل المنطقة", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.5", "BOOKING_CUSTOM_SERVICE_LABEL", "صلاحية تخصيص اسم حقل الخدمة", "تغيير تسمية حقل نوع الخدمة في استمارة الحجز (مثلاً: نوع الخدمة، الخدمة المطلوبة، التخصص)", "استمارة الحجز", "يسمح بتغيير تسمية حقل الخدمة", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.6", "BOOKING_CUSTOM_DATE_LABEL", "صلاحية تخصيص اسم حقل التاريخ", "تغيير تسمية حقل التاريخ في استمارة الحجز (مثلاً: التاريخ المطلوب، تاريخ الحجز، اليوم)", "استمارة الحجز", "يسمح بتغيير تسمية حقل التاريخ", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.7", "BOOKING_CUSTOM_TIME_LABEL", "صلاحية تخصيص اسم حقل الوقت", "تغيير تسمية حقل الوقت في استمارة الحجز (مثلاً: الوقت المطلوب، الساعة، وقت الحجز)", "استمارة الحجز", "يسمح بتغيير تسمية حقل الوقت", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.8", "BOOKING_CUSTOM_NOTES_LABEL", "صلاحية تخصيص اسم حقل الملاحظات", "تغيير تسمية حقل الملاحظات في استمارة الحجز (مثلاً: ملاحظات إضافية، تعليمات خاصة، طلب خاص)", "استمارة الحجز", "يسمح بتغيير تسمية حقل الملاحظات", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.9", "BOOKING_SET_REQUIRED_FIELDS", "صلاحية تحديد الحقول الإجبارية", "تحديد الحقول التي تصبح إجبارية ويجب على العميل تعبئتها قبل إتمام عملية الحجز", "استمارة الحجز", "يسمح بتخصيص الحقول الإجبارية في الحجز", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.10", "BOOKING_ADD_FIELD", "صلاحية إضافة حقل جديد", "إضافة حقل جديد إلى استمارة الحجز لزيادة المعلومات المطلوبة من العميل", "استمارة الحجز", "يسمح بإضافة حقل جديد للحجز", PermissionLevel.ADVANCED, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.11", "BOOKING_EDIT_FIELD", "صلاحية تعديل حقل", "تعديل اسم أو نوع أو خصائص حقل موجود في استمارة الحجز", "استمارة الحجز", "يسمح بتعديل حقل موجود في الحجز", PermissionLevel.ADVANCED, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.12", "BOOKING_DELETE_FIELD", "صلاحية حذف حقل", "حذف حقل من استمارة الحجز بشكل نهائي", "استمارة الحجز", "يسمح بحذف حقل من الحجز", PermissionLevel.ADVANCED, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.13", "BOOKING_ROUTE_ADMIN", "صلاحية توجيه الحجز للأدمن", "تحويل الحجز مباشرة للأدمن المسؤول لتولي إدارته ومتابعته", "الحجوزات", "يسمح بتوجيه الحجز للأدمن", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.14", "BOOKING_ROUTE_PROVIDER", "صلاحية توجيه الحجز لفني محدد", "تحويل الحجز لفني معين لتولي تنفيذ الخدمة المطلوبة", "الحجوزات", "يسمح بتوجيه الحجز لفني محدد", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.15", "BOOKING_ROUTE_NEAREST", "صلاحية توجيه الحجز لأقرب فني", "تحويل الحجز لأقرب فني جغرافياً بناءً على موقع العميل الحالي", "الحجوزات", "يسمح بتوجيه الحجز لأقرب فني", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.16", "BOOKING_ROUTE_CATEGORY", "صلاحية توجيه الحجز لقسم كامل", "بث الحجز لجميع فنيي قسم معين ليتمكن أي منهم من قبول الحجز وتنفيذه", "الحجوزات", "يسمح بتوجيه الحجز لقسم كامل", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.17", "BOOKING_ROUTE_STORE", "صلاحية توجيه الحجز لمحل محدد", "تحويل الحجز لمحل معين لتولي الخدمة المطلوبة", "الحجوزات", "يسمح بتوجيه الحجز لمحل محدد", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.18", "BOOKING_ROUTE_RESTAURANT", "صلاحية توجيه الحجز لمطعم محدد", "تحويل الحجز لمطعم معين لتولي الخدمة المطلوبة", "الحجوزات", "يسمح بتوجيه الحجز لمطعم محدد", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.19", "BOOKING_ROUTE_MEDICAL", "صلاحية توجيه الحجز لمركز طبي محدد", "تحويل الحجز لمركز طبي معين لتولي الخدمة المطلوبة", "الحجوزات", "يسمح بتوجيه الحجز لمركز طبي محدد", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.20", "BOOKING_SET_RESPONSE_TIME", "صلاحية تحديد مدة الاستجابة", "تحديد المهلة الزمنية المسموحة للفني أو مقدم الخدمة للرد على طلب الحجز وقبوله أو رفضه", "الحجوزات", "يحدد مدة انتظار استجابة الفني", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.21", "BOOKING_TOGGLE_PASSWORD", "صلاحية تفعيل كلمة مرور الحجز", "تفعيل نظام كلمة المرور لتأكيد الحجز من قبل العميل قبل تنفيذه", "الحجوزات", "يفعّل نظام كلمة المرور للحجز", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.22", "BOOKING_SET_PIN_LENGTH", "صلاحية تحديد طول كلمة المرور", "تحديد عدد أرقام كلمة مرور الحجز (إما 4 أرقام أو 6 أرقام)", "الحجوزات", "يحدد طول كلمة مرور الحجز", PermissionLevel.BASIC, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.23", "BOOKING_SET_ELIGIBILITY", "صلاحية تحديد صلاحية الحجز", "تحديد من يمكنه إجراء الحجز (جميع المستخدمين، فقط المسجلين والمصادق عليهم، أو تعطيل الحجز بالكامل)", "الحجوزات", "يحدد صلاحية إجراء الحجز", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS),
            AdminPermissionItem("4.24", "BOOKING_SET_CANCEL_ATTEMPTS", "صلاحية تحديد عدد محاولات الإلغاء", "تحديد عدد المرات المسموح بها للعميل لإلغاء الحجز قبل تطبيق قيود أو عقوبات", "الحجوزات", "يحدد عدد محاولات الإلغاء المسموح بها", PermissionLevel.MEDIUM, PermissionCategory.BOOKING_FORMS)
        ))

        // 5. استمارة اطلب خدمتك الآن (19 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("5.1", "QUICK_TOGGLE_FORM", "صلاحية تفعيل/تعطيل الاستمارة", "تمكين أو تعطيل استمارة اطلب خدمتك الآن بالكامل في التطبيق", "جميع المستخدمين", "يتحكم في استمارة الخدمة السريعة", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.2", "QUICK_CUSTOM_TITLE", "صلاحية تخصيص عنوان الاستمارة", "تغيير عنوان استمارة الخدمة السريعة (مثلاً: اطلب خدمتك الآن، طلب خدمة فورية، خدمة عاجلة)", "استمارة الخدمة السريعة", "يسمح بتغيير عنوان الاستمارة", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.3", "QUICK_CUSTOM_NAME_LABEL", "صلاحية تخصيص اسم حقل الاسم", "تغيير تسمية حقل اسم العميل في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل الاسم", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.4", "QUICK_CUSTOM_PHONE_LABEL", "صلاحية تخصيص اسم حقل الهاتف", "تغيير تسمية حقل رقم الهاتف في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل الهاتف", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.5", "QUICK_CUSTOM_AREA_LABEL", "صلاحية تخصيص اسم حقل المنطقة", "تغيير تسمية حقل المنطقة في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل المنطقة", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.6", "QUICK_CUSTOM_SERVICE_LABEL", "صلاحية تخصيص اسم حقل الخدمة", "تغيير تسمية حقل نوع الخدمة في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل الخدمة", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.7", "QUICK_CUSTOM_DESC_LABEL", "صلاحية تخصيص اسم حقل الوصف", "تغيير تسمية حقل وصف المشكلة أو الطلب في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل الوصف", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.8", "QUICK_CUSTOM_PASS_LABEL", "صلاحية تخصيص اسم حقل كلمة المرور", "تغيير تسمية حقل كلمة المرور في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتغيير تسمية حقل كلمة المرور", PermissionLevel.BASIC, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.9", "QUICK_SET_REQUIRED_FIELDS", "صلاحية تحديد الحقول الإجبارية", "تحديد الحقول التي تصبح إجبارية في استمارة الخدمة السريعة", "استمارة الخدمة السريعة", "يسمح بتخصيص الحقول الإجبارية", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.10", "QUICK_SET_CATEGORIES", "صلاحية تحديد الأقسام المتاحة", "تحديد الأقسام الخدمية التي تظهر في استمارة الخدمة السريعة ليختار منها العميل", "استمارة الخدمة السريعة", "يحدد الأقسام المتاحة في الاستمارة", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.11", "QUICK_SET_CITIES", "صلاحية تحديد المدن المتاحة", "تحديد المدن أو المناطق التي تظهر في استمارة الخدمة السريعة ليختار منها العميل", "استمارة الخدمة السريعة", "يحدد المدن المتاحة في الاستمارة", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.12", "QUICK_ROUTE_ADMIN", "صلاحية توجيه الطلب للأدمن", "تحويل طلب الخدمة السريعة مباشرة للأدمن المسؤول", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب للأدمن", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.13", "QUICK_ROUTE_ALL_PROVIDERS", "صلاحية توجيه الطلب لجميع الفنيين", "بث طلب الخدمة السريعة لجميع الفنيين في التطبيق", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب لجميع الفنيين", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.14", "QUICK_ROUTE_ALL_STORES", "صلاحية توجيه الطلب لجميع المحلات", "بث طلب الخدمة السريعة لجميع المحلات في التطبيق", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب لجميع المحلات", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.15", "QUICK_ROUTE_ALL_RESTAURANTS", "صلاحية توجيه الطلب لجميع المطاعم", "بث طلب الخدمة السريعة لجميع المطاعم في التطبيق", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب لجميع المطاعم", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.16", "QUICK_ROUTE_ALL_MEDICAL", "صلاحية توجيه الطلب لجميع المراكز الطبية", "بث طلب الخدمة السريعة لجميع المراكز الطبية في التطبيق", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب لجميع المراكز الطبية", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.17", "QUICK_ROUTE_NEAREST", "صلاحية توجيه الطلب لأقرب فني", "تحويل طلب الخدمة السريعة لأقرب فني جغرافياً بناءً على موقع العميل", "طلبات الخدمة السريعة", "يسمح بتوجيه الطلب لأقرب فني", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.18", "QUICK_ENABLE_AUCTION", "صلاحية تفعيل نظام المزاد العكسي", "تفعيل نظام تلقي العروض من الفنيين أو مقدمي الخدمات على طلب العميل، ليختار العميل العرض المناسب", "طلبات الخدمة السريعة", "يفعّل نظام المزاد العكسي", PermissionLevel.ADVANCED, PermissionCategory.QUICK_SERVICE),
            AdminPermissionItem("5.19", "QUICK_SET_RESPONSE_TIME", "صلاحية تحديد مدة الاستجابة", "تحديد المهلة الزمنية المسموحة للفنيين أو مقدمي الخدمات للرد على طلب العميل", "طلبات الخدمة السريعة", "يحدد مدة استجابة مقدمي الخدمة", PermissionLevel.MEDIUM, PermissionCategory.QUICK_SERVICE)
        ))

        // 6. المحادثات الفورية (45 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("6.1", "CHAT_TOGGLE_SYSTEM", "صلاحية تفعيل/تعطيل المحادثات", "تمكين أو تعطيل نظام المحادثات الفورية بالكامل في التطبيق", "جميع المستخدمين", "يتحكم في نظام المحادثات بالكامل", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.2", "CHAT_DISABLE_USERS", "صلاحية تعطيل المحادثات عن العملاء", "تعطيل إمكانية إجراء المحادثات للعملاء والمواطنين فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "العملاء والمواطنين", "يقتصر على حسابات العملاء", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.3", "CHAT_DISABLE_PROVIDERS", "صلاحية تعطيل المحادثات عن الفنيين", "تعطيل إمكانية إجراء المحادثات للفنيين فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "الفنيين ومقدمي الخدمات", "يقتصر على حسابات الفنيين", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.4", "CHAT_DISABLE_STORES", "صلاحية تعطيل المحادثات عن المحلات", "تعطيل إمكانية إجراء المحادثات للمحلات فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "المحلات والمراكز التجارية", "يقتصر على حسابات المحلات", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.5", "CHAT_DISABLE_RESTAURANTS", "صلاحية تعطيل المحادثات عن المطاعم", "تعطيل إمكانية إجراء المحادثات للمطاعم فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "المطاعم والكافيهات", "يقتصر على حسابات المطاعم", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.6", "CHAT_DISABLE_MEDICAL", "صلاحية تعطيل المحادثات عن المراكز الطبية", "تعطيل إمكانية إجراء المحادثات للمراكز الطبية فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "المراكز الطبية والعيادات", "يقتصر على حسابات المراكز الطبية", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.7", "CHAT_DISABLE_PROPERTIES", "صلاحية تعطيل المحادثات عن العقارات", "تعطيل إمكانية إجراء المحادثات للعقارات فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "مالكي ومعلني العقارات", "يقتصر على حسابات العقارات", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.8", "CHAT_DISABLE_JOBS", "صلاحية تعطيل المحادثات عن معلني الوظائف", "تعطيل إمكانية إجراء المحادثات لمعلني الوظائف فقط، مع بقاء المحادثات متاحة للفئات الأخرى", "الشركات المعلنة عن وظائف", "يقتصر على حسابات معلني الوظائف", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.9", "CHAT_ROUTE_DIRECT", "صلاحية توجيه مباشر بين العميل والفني", "تمكين التواصل المباشر بين العميل والفني دون وسيط أو تدخل من الأدمن", "العملاء والفنيين", "يسمح بالتواصل المباشر بين العميل والفني", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.10", "CHAT_ROUTE_ADMIN", "صلاحية توجيه للأدمن", "توجيه جميع المحادثات للأدمن لتكون تحت إشرافه وإدارته", "جميع المحادثات", "يسمح بتوجيه جميع المحادثات للأدمن", PermissionLevel.ADVANCED, PermissionCategory.CHAT),
            AdminPermissionItem("6.11", "CHAT_ROUTE_ADMIN_SUPERVISORS", "صلاحية توجيه للأدمن والمشرفين", "توجيه جميع المحادثات للأدمن والمشرفين لتكون تحت إشرافهم وإدارتهم", "جميع المحادثات", "يسمح بتوجيه المحادثات للأدمن والمشرفين", PermissionLevel.ADVANCED, PermissionCategory.CHAT),
            AdminPermissionItem("6.12", "CHAT_VIEW_ALL", "صلاحية عرض جميع المحادثات", "عرض جميع قنوات المحادثات في التطبيق دون استثناء", "جميع المحادثات", "يسمح بعرض جميع المحادثات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.13", "CHAT_VIEW_PROVIDERS", "صلاحية عرض محادثات الفنيين", "عرض محادثات الفنيين فقط في لوحة التحكم", "محادثات الفنيين", "يسمح بعرض محادثات الفنيين فقط", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.14", "CHAT_VIEW_STORES", "صلاحية عرض محادثات المحلات", "عرض محادثات المحلات فقط في لوحة التحكم", "محادثات المحلات", "يسمح بعرض محادثات المحلات فقط", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.15", "CHAT_VIEW_RESTAURANTS", "صلاحية عرض محادثات المطاعم", "عرض محادثات المطاعم فقط في لوحة التحكم", "محادثات المطاعم", "يسمح بعرض محادثات المطاعم فقط", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.16", "CHAT_VIEW_MEDICAL", "صلاحية عرض محادثات المراكز الطبية", "عرض محادثات المراكز الطبية فقط في لوحة التحكم", "محادثات المراكز الطبية", "يسمح بعرض محادثات المراكز الطبية فقط", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.17", "CHAT_VIEW_PROPERTIES", "صلاحية عرض محادثات العقارات", "عرض محادثات العقارات فقط في لوحة التحكم", "محادثات العقارات", "يسمح بعرض محادثات العقارات فقط", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.18", "CHAT_REPLY_AS_ADMIN", "صلاحية الرد كأدمن", "إرسال رسائل في المحادثات باسم الأدمن وبهويته الرسمية", "جميع المحادثات", "يسمح بالرد باسم الأدمن", PermissionLevel.ADVANCED, PermissionCategory.CHAT),
            AdminPermissionItem("6.19", "CHAT_EDIT_MESSAGE", "صلاحية تعديل رسالة", "تعديل محتوى رسالة موجودة في المحادثة (للأدمن فقط)", "جميع المحادثات", "يسمح بتعديل رسالة موجودة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.20", "CHAT_DELETE_MESSAGE", "صلاحية حذف رسالة", "حذف رسالة من المحادثة بشكل نهائي (للأدمن فقط)", "جميع المحادثات", "يسمح بحذف رسالة من المحادثة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.21", "CHAT_BLOCK_CHANNEL", "صلاحية حظر المحادثة", "حظر المحادثة بالكامل ومنع أي طرف من إرسال رسائل جديدة فيها", "محادثة محددة", "يسمح بحظر محادثة كاملة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.22", "CHAT_UNBLOCK_CHANNEL", "صلاحية إلغاء حظر المحادثة", "إلغاء حظر المحادثة وإعادة تفعيل إمكانية إرسال الرسائل فيها", "محادثة محددة", "يسمح بإلغاء حظر محادثة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.23", "CHAT_BLOCK_PROVIDER", "صلاحية حظر فني", "منع فني معين من استخدام نظام المحادثات الفورية", "فني محدد", "يسمح بحظر فني من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.24", "CHAT_BLOCK_STORE", "صلاحية حظر محل", "منع محل معين من استخدام نظام المحادثات الفورية", "محل محدد", "يسمح بحظر محل من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.25", "CHAT_BLOCK_RESTAURANT", "صلاحية حظر مطعم", "منع مطعم معين من استخدام نظام المحادثات الفورية", "مطعم محدد", "يسمح بحظر مطعم من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.26", "CHAT_BLOCK_MEDICAL", "صلاحية حظر مركز طبي", "منع مركز طبي معين من استخدام نظام المحادثات الفورية", "مركز طبي محدد", "يسمح بحظر مركز طبي من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.27", "CHAT_BLOCK_PROPERTY", "صلاحية حظر عقار", "منع عقار معين من استخدام نظام المحادثات الفورية", "عقار محدد", "يسمح بحظر عقار من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.28", "CHAT_BLOCK_JOB", "صلاحية حظر معلن وظيفة", "منع معلن وظيفة معين من استخدام نظام المحادثات الفورية", "معلن وظيفة محدد", "يسمح بحظر معلن وظيفة من الشات", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.29", "CHAT_UNBLOCK_ANY", "صلاحية إلغاء حظر أي فئة", "إلغاء حظر أي من الفئات السابقة (فني، محل، مطعم، مركز طبي، عقار، معلن وظيفة)", "أي فئة محظورة", "يسمح بإلغاء حظر أي فئة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.30", "CHAT_BLOCK_CATEGORY", "صلاحية حظر قسم كامل", "منع جميع فنيي قسم خدمي معين من استخدام نظام المحادثات الفورية", "قسم خدمي كامل", "يسمح بحظر قسم كامل", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.31", "CHAT_TRANSFER_PROVIDER", "صلاحية تحويل المحادثة لفني", "تحويل المحادثة من الأدمن أو المشرف إلى فني محدد لمتابعتها", "محادثة محددة", "يسمح بتحويل المحادثة لفني", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.32", "CHAT_DELETE_CHANNEL", "صلاحية حذف المحادثة", "حذف المحادثة بالكامل من سجل المحادثات بشكل نهائي", "محادثة محددة", "يسمح بحذف محادثة كاملة", PermissionLevel.SENSITIVE, PermissionCategory.CHAT),
            AdminPermissionItem("6.33", "CHAT_EXPORT_HISTORY", "صلاحية تصدير سجل المحادثة", "تصدير سجل المحادثة كاملاً بصيغة CSV أو PDF لحفظه أو طباعته", "محادثة محددة", "يسمح بتصدير سجل المحادثة", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.34", "CHAT_SET_FONT_SIZE", "صلاحية تحديد حجم خط الشات", "تحديد حجم الخط المستخدم في عرض رسائل المحادثة", "واجهة المحادثة", "يسمح بتحديد حجم خط الشات", PermissionLevel.BASIC, PermissionCategory.CHAT),
            AdminPermissionItem("6.35", "CHAT_SET_BG_COLOR", "صلاحية تحديد لون خلفية الشات", "تحديد لون خلفية نافذة المحادثة في التطبيق", "واجهة المحادثة", "يسمح بتحديد لون خلفية الشات", PermissionLevel.BASIC, PermissionCategory.CHAT),
            AdminPermissionItem("6.36", "CHAT_SET_ICON_SIZE", "صلاحية تحديد حجم أيقونة الشات", "تحديد حجم الأيقونة العائمة للدخول إلى المحادثة على الشاشة الرئيسية", "واجهة المحادثة", "يسمح بتحديد حجم أيقونة الشات", PermissionLevel.BASIC, PermissionCategory.CHAT),
            AdminPermissionItem("6.37", "CHAT_SET_ICON_POS", "صلاحية تحديد موقع أيقونة الشات", "تحديد موقع الأيقونة العائمة للدخول إلى المحادثة (أعلى يسار، أعلى يمين، أسفل يسار، أسفل يمين)", "واجهة المحادثة", "يسمح بتحديد موقع أيقونة الشات", PermissionLevel.BASIC, PermissionCategory.CHAT),
            AdminPermissionItem("6.38", "CHAT_TOGGLE_VOICE_INPUT", "صلاحية تفعيل الإدخال الصوتي", "تفعيل ميزة الإدخال الصوتي (Speech-to-Text) لتحويل الكلام إلى نص في المحادثة", "واجهة المحادثة", "يفعّل الإدخال الصوتي", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.39", "CHAT_TOGGLE_TTS", "صلاحية تفعيل النطق الصوتي", "تفعيل ميزة النطق الصوتي (Text-to-Speech) لقراءة رسائل المحادثة بصوت مسموع", "واجهة المحادثة", "يفعّل النطق الصوتي", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.40", "CHAT_TOGGLE_SEND_IMAGES", "صلاحية تفعيل إرسال الصور", "السماح للمستخدمين بإرسال الصور في المحادثة الفورية", "واجهة المحادثة", "يفعّل إرسال الصور", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.41", "CHAT_TOGGLE_SEND_VIDEOS", "صلاحية تفعيل إرسال الفيديو", "السماح للمستخدمين بإرسال مقاطع الفيديو في المحادثة الفورية", "واجهة المحادثة", "يفعّل إرسال الفيديو", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.42", "CHAT_TOGGLE_SEND_AUDIO", "صلاحية تفعيل إرسال الصوت", "السماح للمستخدمين بإرسال رسائل صوتية مسجلة في المحادثة الفورية", "واجهة المحادثة", "يفعّل إرسال الصوت", PermissionLevel.MEDIUM, PermissionCategory.CHAT),
            AdminPermissionItem("6.43", "CHAT_TOGGLE_VOICE_CALLS", "صلاحية تفعيل المكالمات الصوتية", "السماح للمستخدمين بإجراء مكالمات صوتية عبر التطبيق", "واجهة المحادثة", "يفعّل المكالمات الصوتية", PermissionLevel.ADVANCED, PermissionCategory.CHAT),
            AdminPermissionItem("6.44", "CHAT_SET_MAX_IMAGES", "صلاحية تحديد عدد الصور المسموح بها", "تحديد الحد الأقصى لعدد الصور التي يمكن إرسالها في المحادثة الواحدة", "واجهة المحادثة", "يحدد عدد الصور المسموح بها", PermissionLevel.BASIC, PermissionCategory.CHAT),
            AdminPermissionItem("6.45", "CHAT_SET_MAX_VIDEOS", "صلاحية تحديد عدد الفيديوهات المسموح بها", "تحديد الحد الأقصى لعدد مقاطع الفيديو التي يمكن إرسالها في المحادثة الواحدة", "واجهة المحادثة", "يحدد عدد الفيديوهات المسموح بها", PermissionLevel.BASIC, PermissionCategory.CHAT)
        ))

        // 7. الأيقونات الذهبية والثيمات (13 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("7.1", "THEME_CHOOSE_ICON_STYLE", "صلاحية اختيار نمط الأيقونات", "اختيار نمط أيقونات التنقل في التطبيق من بين (ذهبي ثلاثي الأبعاد، معدني، خطي، مسطح، كرتوني، عصري)", "واجهة التطبيق", "يحدد نمط الأيقونات بالكامل", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.2", "THEME_SET_ICON_SIZE", "صلاحية تحديد حجم الأيقونات", "تحديد حجم أيقونات التنقل الرئيسية في التطبيق (صغير جداً، صغير، متوسط، كبير، كبير جداً)", "واجهة التطبيق", "يحدد حجم الأيقونات", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.3", "THEME_CUSTOM_HOME_ICON", "صلاحية تخصيص أيقونة الرئيسية", "تغيير أيقونة تبويب الرئيسية في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الرئيسية", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.4", "THEME_CUSTOM_MAP_ICON", "صلاحية تخصيص أيقونة الخرائط", "تغيير أيقونة تبويب الخرائط في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الخرائط", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.5", "THEME_CUSTOM_JOIN_ICON", "صلاحية تخصيص أيقونة الانضمام", "تغيير أيقونة تبويب الانضمام أو التسجيل في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الانضمام", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.6", "THEME_CUSTOM_NOTIF_ICON", "صلاحية تخصيص أيقونة الإشعارات", "تغيير أيقونة تبويب الإشعارات في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الإشعارات", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.7", "THEME_CUSTOM_CHAT_ICON", "صلاحية تخصيص أيقونة المحادثات", "تغيير أيقونة تبويب المحادثات في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة المحادثات", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.8", "THEME_CUSTOM_ABOUT_ICON", "صلاحية تخصيص أيقونة عن التطبيق", "تغيير أيقونة تبويب عن التطبيق أو الإعدادات في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة عن التطبيق", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.9", "THEME_CUSTOM_BOOKING_ICON", "صلاحية تخصيص أيقونة الحجوزات", "تغيير أيقونة تبويب الحجوزات في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الحجوزات", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.10", "THEME_CUSTOM_LANG_ICON", "صلاحية تخصيص أيقونة اللغة", "تغيير أيقونة تبديل اللغة في واجهة التطبيق", "واجهة التطبيق", "يسمح بتغيير أيقونة اللغة", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.11", "THEME_CUSTOM_ADMIN_ICON", "صلاحية تخصيص أيقونة الإدارة", "تغيير أيقونة تبويب الإدارة أو لوحة التحكم في شريط التنقل السفلي", "واجهة التطبيق", "يسمح بتغيير أيقونة الإدارة", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.12", "THEME_CHOOSE_FONT_FAMILY", "صلاحية اختيار نوع الخط", "اختيار نوع الخط المستخدم في التطبيق من بين (Cairo، Amiri، Tahoma، System، Droid Arabic Kufi، من أنواع الخطوط الأخرى)", "واجهة التطبيق", "يحدد نوع الخط بالكامل", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS),
            AdminPermissionItem("7.13", "THEME_SET_FONT_SCALE", "صلاحية تحديد مقياس حجم الخط", "تحديد مقياس حجم الخط العام في التطبيق بنسبة مئوية (من 85% إلى 130%)", "واجهة التطبيق", "يحدد حجم الخط العام", PermissionLevel.BASIC, PermissionCategory.THEMES_ICONS)
        ))

        // 8. الأقسام الجديدة (21 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("8.1", "SECTION_CREATE", "صلاحية إنشاء قسم جديد", "إضافة قسم خدمي جديد بالكامل إلى التطبيق بكافة محتوياته وإعداداته", "جميع المستخدمين", "يسمح بإنشاء قسم جديد", PermissionLevel.SENSITIVE, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.2", "SECTION_SET_NAME", "صلاحية تحديد اسم القسم", "كتابة وتحديد اسم القسم الجديد (مثلاً: خدمات التوصيل، محفظتي، خدمات منزلية)", "القسم الجديد", "يسمح بتحديد اسم القسم", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.3", "SECTION_SET_ICON", "صلاحية تحديد أيقونة القسم", "اختيار أيقونة مناسبة للقسم الجديد من مكتبة الأيقونات المتاحة", "القسم الجديد", "يسمح بتحديد أيقونة القسم", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.4", "SECTION_SET_TYPE", "صلاحية تحديد نوع القسم", "تحديد نوع القسم الجديد (توصيل، محفظة مالية، خدمي، تعليمي، صحي، ترفيهي، سياحي)", "القسم الجديد", "يسمح بتحديد نوع القسم", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.5", "SECTION_LINK_MAPS", "صلاحية ربط القسم بالخرائط", "ربط القسم الجديد بشاشة الخرائط لعرض المواقع المتعلقة به", "القسم الجديد", "يسمح بربط القسم بالخرائط", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.6", "SECTION_LINK_ORDERS", "صلاحية ربط القسم بالطلبات", "ربط القسم الجديد بشاشة الطلبات لإدارة طلبات الخدمة المتعلقة به", "القسم الجديد", "يسمح بربط القسم بالطلبات", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.7", "SECTION_LINK_PAYMENT", "صلاحية ربط القسم بالدفع", "ربط القسم الجديد بشاشة الدفع لإجراء المعاملات المالية المتعلقة به", "القسم الجديد", "يسمح بربط القسم بالدفع", PermissionLevel.ADVANCED, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.8", "SECTION_ENABLE_NOTIFS", "صلاحية تمكين الإشعارات للقسم", "تفعيل نظام الإشعارات للقسم الجديد لإرسال تنبيهات للمستخدمين", "القسم الجديد", "يفعّل إشعارات القسم", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.9", "SECTION_ENABLE_BOOKINGS", "صلاحية تمكين الحجوزات للقسم", "تفعيل نظام الحجوزات للقسم الجديد لتمكين المستخدمين من حجز الخدمات", "القسم الجديد", "يفعّل حجوزات القسم", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.10", "SECTION_SET_ACCESS", "صلاحية تحديد صلاحية القسم", "تحديد من يمكنه الوصول إلى القسم الجديد (جميع المستخدمين، فئة معينة فقط)", "القسم الجديد", "يحدد صلاحية الوصول للقسم", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.11", "SECTION_SHOW_ALL", "صلاحية إظهار القسم للجميع", "إظهار القسم الجديد لجميع مستخدمي التطبيق بدون استثناء", "القسم الجديد", "يظهر القسم للجميع", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.12", "SECTION_SHOW_USERS", "صلاحية إظهار القسم للمستخدمين فقط", "إظهار القسم الجديد للعملاء والمواطنين المسجلين فقط", "القسم الجديد", "يظهر القسم للعملاء فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.13", "SECTION_SHOW_PROVIDERS", "صلاحية إظهار القسم للفنيين فقط", "إظهار القسم الجديد للفنيين ومقدمي الخدمات فقط", "القسم الجديد", "يظهر القسم للفنيين فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.14", "SECTION_SHOW_STORES", "صلاحية إظهار القسم للمحلات فقط", "إظهار القسم الجديد للمحلات والمراكز التجارية فقط", "القسم الجديد", "يظهر القسم للمحلات فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.15", "SECTION_SHOW_RESTAURANTS", "صلاحية إظهار القسم للمطاعم فقط", "إظهار القسم الجديد للمطاعم والكافيهات فقط", "القسم الجديد", "يظهر القسم للمطاعم فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.16", "SECTION_SHOW_MEDICAL", "صلاحية إظهار القسم للمراكز الطبية فقط", "إظهار القسم الجديد للمراكز الطبية والعيادات فقط", "القسم الجديد", "يظهر القسم للمراكز الطبية فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.17", "SECTION_SHOW_PROPERTIES", "صلاحية إظهار القسم للعقارات فقط", "إظهار القسم الجديد لمالكي ومعلني العقارات فقط", "القسم الجديد", "يظهر القسم للعقارات فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.18", "SECTION_SHOW_JOBS", "صلاحية إظهار القسم لمعلني الوظائف فقط", "إظهار القسم الجديد للشركات والمعلنين عن وظائف فقط", "القسم الجديد", "يظهر القسم لمعلني الوظائف فقط", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.19", "SECTION_EDIT", "صلاحية تعديل القسم", "تعديل بيانات ومحتويات وإعدادات قسم موجود مسبقاً", "قسم محدد", "يسمح بتعديل قسم موجود", PermissionLevel.MEDIUM, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.20", "SECTION_DELETE", "صلاحية حذف القسم", "حذف قسم نهائياً من التطبيق وجميع بياناته المرتبطة", "قسم محدد", "يسمح بحذف قسم نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.NEW_SECTIONS),
            AdminPermissionItem("8.21", "SECTION_REORDER", "صلاحية ترتيب الأقسام", "تحديد ترتيب ظهور الأقسام في واجهة التطبيق (القسم الأول يظهر أولاً)", "جميع الأقسام", "يسمح بترتيب الأقسام", PermissionLevel.BASIC, PermissionCategory.NEW_SECTIONS)
        ))

        // 9. شاشة الخرائط (29 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("9.1", "MAP_SHOW_ALL", "صلاحية إظهار الخريطة للجميع", "إظهار شاشة الخرائط لجميع مستخدمي التطبيق بدون استثناء", "جميع المستخدمين", "يظهر الخريطة للجميع", PermissionLevel.BASIC, PermissionCategory.MAPS),
            AdminPermissionItem("9.2", "MAP_HIDE_ALL", "صلاحية إخفاء الخريطة عن الجميع", "إخفاء شاشة الخرائط بالكامل عن جميع مستخدمي التطبيق", "جميع المستخدمين", "يخفي الخريطة عن الجميع", PermissionLevel.SENSITIVE, PermissionCategory.MAPS),
            AdminPermissionItem("9.3", "MAP_SHOW_USERS_ONLY", "صلاحية إظهار الخريطة للمستخدمين فقط", "إظهار شاشة الخرائط للعملاء والمواطنين المسجلين فقط", "العملاء والمواطنين", "يظهر الخريطة للعملاء فقط", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.4", "MAP_HIDE_PROVIDERS", "صلاحية إخفاء الخريطة عن الفنيين", "منع الفنيين ومقدمي الخدمات من رؤية شاشة الخرائط", "الفنيين ومقدمي الخدمات", "يخفي الخريطة عن الفنيين", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.5", "MAP_HIDE_STORES", "صلاحية إخفاء الخريطة عن المحلات", "منع المحلات والمراكز التجارية من رؤية شاشة الخرائط", "المحلات والمراكز التجارية", "يخفي الخريطة عن المحلات", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.6", "MAP_HIDE_RESTAURANTS", "صلاحية إخفاء الخريطة عن المطاعم", "منع المطاعم والكافيهات من رؤية شاشة الخرائط", "المطاعم والكافيهات", "يخفي الخريطة عن المطاعم", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.7", "MAP_HIDE_MEDICAL", "صلاحية إخفاء الخريطة عن المراكز الطبية", "منع المراكز الطبية والعيادات من رؤية شاشة الخرائط", "المراكز الطبية والعيادات", "يخفي الخريطة عن المراكز الطبية", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.8", "MAP_HIDE_PROPERTIES", "صلاحية إخفاء الخريطة عن العقارات", "منع مالكي ومعلني العقارات من رؤية شاشة الخرائط", "مالكي ومعلني العقارات", "يخفي الخريطة عن العقارات", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.9", "MAP_SET_SYSTEM_ALL", "صلاحية تغيير نظام الخريطة للجميع", "تغيير نظام الخرائط المستخدم لجميع المستخدمين (من MapLibre إلى Google Maps أو Mapbox)", "جميع المستخدمين", "يغير نظام الخريطة للجميع", PermissionLevel.ADVANCED, PermissionCategory.MAPS),
            AdminPermissionItem("9.10", "MAP_SET_SYSTEM_USERS", "صلاحية تغيير نظام الخريطة للمستخدمين", "تغيير نظام الخرائط للعملاء فقط دون الفئات الأخرى", "العملاء والمواطنين", "يغير نظام الخريطة للمستخدمين فقط", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.11", "MAP_SET_SYSTEM_PROVIDERS", "صلاحية تغيير نظام الخريطة للفنيين", "تغيير نظام الخرائط للفنيين فقط دون الفئات الأخرى", "الفنيين ومقدمي الخدمات", "يغير نظام الخريطة للفنيين فقط", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.12", "MAP_CHOOSE_PROVIDER", "صلاحية تحديد نظام الخريطة", "تحديد نظام الخرائط المستخدم في التطبيق من بين (MapLibre، Google Maps، Mapbox، OpenStreetMap)", "شاشة الخرائط", "يحدد نظام الخريطة", PermissionLevel.ADVANCED, PermissionCategory.MAPS),
            AdminPermissionItem("9.13", "MAP_LINK_CATEGORY", "صلاحية ربط الخريطة بقسم", "عرض فنيي قسم خدمي معين فقط على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بقسم محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.14", "MAP_LINK_PROVIDER", "صلاحية ربط الخريطة بفني محدد", "عرض موقع فني معين على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بفني محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.15", "MAP_LINK_STORE", "صلاحية ربط الخريطة بمحل محدد", "عرض موقع محل معين على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بمحل محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.16", "MAP_LINK_RESTAURANT", "صلاحية ربط الخريطة بمطعم محدد", "عرض موقع مطعم معين على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بمطعم محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.17", "MAP_LINK_MEDICAL", "صلاحية ربط الخريطة بمركز طبي محدد", "عرض موقع مركز طبي معين على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بمركز طبي محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.18", "MAP_LINK_PROPERTY", "صلاحية ربط الخريطة بعقار محدد", "عرض موقع عقار معين على شاشة الخرائط", "شاشة الخرائط", "يربط الخريطة بعقار محدد", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.19", "MAP_UNLINK_CATEGORY", "صلاحية إلغاء ربط الخريطة بقسم", "إلغاء ربط الخريطة بقسم معين وعرض جميع المواقع مرة أخرى", "شاشة الخرائط", "يلغي ربط الخريطة بقسم", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.20", "MAP_REMOVE_PROVIDER", "صلاحية إزالة فني من الخريطة", "إخفاء موقع فني معين من شاشة الخرائط", "شاشة الخرائط", "يزيل فني محدد من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.21", "MAP_REMOVE_STORE", "صلاحية إزالة محل من الخريطة", "إخفاء موقع محل معين من شاشة الخرائط", "شاشة الخرائط", "يزيل محل محدد من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.22", "MAP_REMOVE_RESTAURANT", "صلاحية إزالة مطعم من الخريطة", "إخفاء موقع مطعم معين من شاشة الخرائط", "شاشة الخرائط", "يزيل مطعم محدد من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.23", "MAP_REMOVE_MEDICAL", "صلاحية إزالة مركز طبي من الخريطة", "إخفاء موقع مركز طبي معين من شاشة الخرائط", "شاشة الخرائط", "يزيل مركز طبي محدد من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.24", "MAP_REMOVE_PROPERTY", "صلاحية إزالة عقار من الخريطة", "إخفاء موقع عقار معين من شاشة الخرائط", "شاشة الخرائط", "يزيل عقار محدد من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.25", "MAP_REMOVE_CATEGORY_PROVIDERS", "صلاحية إزالة جميع فنيي قسم", "إخفاء جميع فنيي قسم خدمي معين من شاشة الخرائط دفعة واحدة", "شاشة الخرائط", "يزيل جميع فنيي قسم من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.26", "MAP_REMOVE_ALL_STORES", "صلاحية إزالة جميع المحلات", "إخفاء جميع المحلات والمراكز التجارية من شاشة الخرائط دفعة واحدة", "شاشة الخرائط", "يزيل جميع المحلات من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.27", "MAP_REMOVE_ALL_RESTAURANTS", "صلاحية إزالة جميع المطاعم", "إخفاء جميع المطاعم والكافيهات من شاشة الخرائط دفعة واحدة", "شاشة الخرائط", "يزيل جميع المطاعم من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.28", "MAP_REMOVE_ALL_MEDICAL", "صلاحية إزالة جميع المراكز الطبية", "إخفاء جميع المراكز الطبية والعيادات من شاشة الخرائط دفعة واحدة", "شاشة الخرائط", "يزيل جميع المراكز الطبية من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS),
            AdminPermissionItem("9.29", "MAP_REMOVE_ALL_PROPERTIES", "صلاحية إزالة جميع العقارات", "إخفاء جميع العقارات من شاشة الخرائط دفعة واحدة", "شاشة الخرائط", "يزيل جميع العقارات من الخريطة", PermissionLevel.MEDIUM, PermissionCategory.MAPS)
        ))

        // 10. إدارة المحلات والمراكز التجارية (17 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("10.1", "STORE_VIEW_ALL", "صلاحية عرض جميع المحلات", "عرض قائمة كاملة بجميع المحلات والمراكز التجارية المسجلة في التطبيق", "المحلات والمراكز التجارية", "يسمح بعرض جميع المحلات", PermissionLevel.BASIC, PermissionCategory.STORES),
            AdminPermissionItem("10.2", "STORE_VIEW_SPECIFIC", "صلاحية عرض محل محدد", "عرض بيانات ومعلومات محل معين بشكل مفصل", "محل محدد", "يسمح بعرض محل محدد", PermissionLevel.BASIC, PermissionCategory.STORES),
            AdminPermissionItem("10.3", "STORE_ADD", "صلاحية إضافة محل جديد", "إضافة محل أو مركز تجاري جديد إلى التطبيق يدوياً من لوحة التحكم", "محلات جديدة", "يسمح بإضافة محل جديد", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.4", "STORE_EDIT", "صلاحية تعديل محل", "تعديل بيانات ومعلومات محل موجود مسبقاً في التطبيق", "محل محدد", "يسمح بتعديل محل", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.5", "STORE_DELETE", "صلاحية حذف محل", "حذف محل نهائياً من التطبيق وجميع بياناته المرتبطة", "محل محدد", "يسمح بحذف محل نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.STORES),
            AdminPermissionItem("10.6", "STORE_BLOCK", "صلاحية حظر محل", "حظر محل ومنعه من استخدام التطبيق بشكل مؤقت أو دائم", "محل محدد", "يسمح بحظر محل", PermissionLevel.SENSITIVE, PermissionCategory.STORES),
            AdminPermissionItem("10.7", "STORE_UNBLOCK", "صلاحية إلغاء حظر محل", "إلغاء حظر محل وإعادة تفعيل حسابه في التطبيق", "محل محدد", "يسمح بإلغاء حظر محل", PermissionLevel.SENSITIVE, PermissionCategory.STORES),
            AdminPermissionItem("10.8", "STORE_ACTIVATE", "صلاحية تفعيل محل", "تفعيل حساب محل وجعله نشطاً وقابلاً للاستخدام في التطبيق", "محل محدد", "يسمح بتفعيل محل", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.9", "STORE_DEACTIVATE", "صلاحية تعطيل محل", "تعطيل حساب محل مؤقتاً وجعله غير قابل للاستخدام في التطبيق", "محل محدد", "يسمح بتعطيل محل", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.10", "STORE_PIN", "صلاحية تثبيت محل", "تثبيت محل في أعلى القوائم والنتائج لجعله أكثر ظهوراً للمستخدمين", "محل محدد", "يسمح بتثبيت محل", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.11", "STORE_UNPIN", "صلاحية إلغاء تثبيت محل", "إلغاء تثبيت محل وإعادته إلى ترتيبه الطبيعي في القوائم", "محل محدد", "يسمح بإلغاء تثبيت محل", PermissionLevel.MEDIUM, PermissionCategory.STORES),
            AdminPermissionItem("10.12", "STORE_SET_VIP", "صلاحية تفعيل VIP لمحل", "منح محل عضوية VIP مع ميزات وحقوق إضافية في التطبيق", "محل محدد", "يسمح بمنح VIP لمحل", PermissionLevel.ADVANCED, PermissionCategory.STORES),
            AdminPermissionItem("10.13", "STORE_UNSET_VIP", "صلاحية إلغاء VIP لمحل", "إلغاء عضوية VIP عن محل وإعادة الميزات الأساسية فقط", "محل محدد", "يسمح بإلغاء VIP عن محل", PermissionLevel.ADVANCED, PermissionCategory.STORES),
            AdminPermissionItem("10.14", "STORE_VERIFY", "صلاحية توثيق محل", "منح محل شارة التوثيق (الحساب الموثق) لإثبات مصداقيته", "محل محدد", "يسمح بتوثيق محل", PermissionLevel.ADVANCED, PermissionCategory.STORES),
            AdminPermissionItem("10.15", "STORE_UNVERIFY", "صلاحية إلغاء توثيق محل", "إلغاء شارة التوثيق عن محل وإزالة الحساب الموثق", "محل محدد", "يسمح بإلغاء توثيق محل", PermissionLevel.ADVANCED, PermissionCategory.STORES),
            AdminPermissionItem("10.16", "STORE_RECOMMEND", "صلاحية توصية محل", "منح محل شارة التوصية كدليل على جودة خدماته", "محل محدد", "يسمح بتوصية محل", PermissionLevel.ADVANCED, PermissionCategory.STORES),
            AdminPermissionItem("10.17", "STORE_UNRECOMMEND", "صلاحية إلغاء توصية محل", "إلغاء شارة التوصية عن محل وإزالة التوصية", "محل محدد", "يسمح بإلغاء توصية محل", PermissionLevel.ADVANCED, PermissionCategory.STORES)
        ))

        // 11. إدارة المطاعم والكافيهات (17 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("11.1", "REST_VIEW_ALL", "صلاحية عرض جميع المطاعم", "عرض قائمة كاملة بجميع المطاعم والكافيهات المسجلة في التطبيق", "المطاعم والكافيهات", "يسمح بعرض جميع المطاعم", PermissionLevel.BASIC, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.2", "REST_VIEW_SPECIFIC", "صلاحية عرض مطعم محدد", "عرض بيانات ومعلومات مطعم معين بشكل مفصل", "مطعم محدد", "يسمح بعرض مطعم محدد", PermissionLevel.BASIC, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.3", "REST_ADD", "صلاحية إضافة مطعم جديد", "إضافة مطعم أو كافي جديد إلى التطبيق يدوياً من لوحة التحكم", "مطاعم جديدة", "يسمح بإضافة مطعم جديد", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.4", "REST_EDIT", "صلاحية تعديل مطعم", "تعديل بيانات ومعلومات مطعم موجود مسبقاً في التطبيق", "مطعم محدد", "يسمح بتعديل مطعم", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.5", "REST_DELETE", "صلاحية حذف مطعم", "حذف مطعم نهائياً من التطبيق وجميع بياناته المرتبطة", "مطعم محدد", "يسمح بحذف مطعم نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.6", "REST_BLOCK", "صلاحية حظر مطعم", "حظر مطعم ومنعه من استخدام التطبيق بشكل مؤقت أو دائم", "مطعم محدد", "يسمح بحظر مطعم", PermissionLevel.SENSITIVE, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.7", "REST_UNBLOCK", "صلاحية إلغاء حظر مطعم", "إلغاء حظر مطعم وإعادة تفعيل حسابه في التطبيق", "مطعم محدد", "يسمح بإلغاء حظر مطعم", PermissionLevel.SENSITIVE, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.8", "REST_ACTIVATE", "صلاحية تفعيل مطعم", "تفعيل حساب مطعم وجعله نشطاً وقابلاً للاستخدام في التطبيق", "مطعم محدد", "يسمح بتفعيل مطعم", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.9", "REST_DEACTIVATE", "صلاحية تعطيل مطعم", "تعطيل حساب مطعم مؤقتاً وجعله غير قابل للاستخدام في التطبيق", "مطعم محدد", "يسمح بتعطيل مطعم", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.10", "REST_PIN", "صلاحية تثبيت مطعم", "تثبيت مطعم في أعلى القوائم والنتائج لجعله أكثر ظهوراً للمستخدمين", "مطعم محدد", "يسمح بتثبيت مطعم", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.11", "REST_UNPIN", "صلاحية إلغاء تثبيت مطعم", "إلغاء تثبيت مطعم وإعادته إلى ترتيبه الطبيعي في القوائم", "مطعم محدد", "يسمح بإلغاء تثبيت مطعم", PermissionLevel.MEDIUM, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.12", "REST_SET_VIP", "صلاحية تفعيل VIP لمطعم", "منح مطعم عضوية VIP مع ميزات وحقوق إضافية في التطبيق", "مطعم محدد", "يسمح بمنح VIP لمطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.13", "REST_UNSET_VIP", "صلاحية إلغاء VIP لمطعم", "إلغاء عضوية VIP عن مطعم وإعادة الميزات الأساسية فقط", "مطعم محدد", "يسمح بإلغاء VIP عن مطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.14", "REST_VERIFY", "صلاحية توثيق مطعم", "منح مطعم شارة التوثيق (الحساب الموثق) لإثبات مصداقيته", "مطعم محدد", "يسمح بتوثيق مطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.15", "REST_UNVERIFY", "صلاحية إلغاء توثيق مطعم", "إلغاء شارة التوثيق عن مطعم وإزالة الحساب الموثق", "مطعم محدد", "يسمح بإلغاء توثيق مطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.16", "REST_RECOMMEND", "صلاحية توصية مطعم", "منح مطعم شارة التوصية كدليل على جودة خدماته", "مطعم محدد", "يسمح بتوصية مطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS),
            AdminPermissionItem("11.17", "REST_UNRECOMMEND", "صلاحية إلغاء توصية مطعم", "إلغاء شارة التوصية عن مطعم وإزالة التوصية", "مطعم محدد", "يسمح بإلغاء توصية مطعم", PermissionLevel.ADVANCED, PermissionCategory.RESTAURANTS)
        ))

        // 12. إدارة المراكز الطبية والعيادات (17 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("12.1", "MED_VIEW_ALL", "صلاحية عرض جميع المراكز الطبية", "عرض قائمة كاملة بجميع المراكز الطبية والعيادات المسجلة في التطبيق", "المراكز الطبية والعيادات", "يسمح بعرض جميع المراكز الطبية", PermissionLevel.BASIC, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.2", "MED_VIEW_SPECIFIC", "صلاحية عرض مركز طبي محدد", "عرض بيانات ومعلومات مركز طبي معين بشكل مفصل", "مركز طبي محدد", "يسمح بعرض مركز طبي محدد", PermissionLevel.BASIC, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.3", "MED_ADD", "صلاحية إضافة مركز طبي جديد", "إضافة مركز طبي أو عيادة جديدة إلى التطبيق يدوياً من لوحة التحكم", "مراكز طبية جديدة", "يسمح بإضافة مركز طبي جديد", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.4", "MED_EDIT", "صلاحية تعديل مركز طبي", "تعديل بيانات ومعلومات مركز طبي موجود مسبقاً في التطبيق", "مركز طبي محدد", "يسمح بتعديل مركز طبي", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.5", "MED_DELETE", "صلاحية حذف مركز طبي", "حذف مركز طبي نهائياً من التطبيق وجميع بياناته المرتبطة", "مركز طبي محدد", "يسمح بحذف مركز طبي نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.6", "MED_BLOCK", "صلاحية حظر مركز طبي", "حظر مركز طبي ومنعه من استخدام التطبيق بشكل مؤقت أو دائم", "مركز طبي محدد", "يسمح بحظر مركز طبي", PermissionLevel.SENSITIVE, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.7", "MED_UNBLOCK", "صلاحية إلغاء حظر مركز طبي", "إلغاء حظر مركز طبي وإعادة تفعيل حسابه في التطبيق", "مركز طبي محدد", "يسمح بإلغاء حظر مركز طبي", PermissionLevel.SENSITIVE, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.8", "MED_ACTIVATE", "صلاحية تفعيل مركز طبي", "تفعيل حساب مركز طبي وجعله نشطاً وقابلاً للاستخدام في التطبيق", "مركز طبي محدد", "يسمح بتفعيل مركز طبي", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.9", "MED_DEACTIVATE", "صلاحية تعطيل مركز طبي", "تعطيل حساب مركز طبي مؤقتاً وجعله غير قابل للاستخدام في التطبيق", "مركز طبي محدد", "يسمح بتعطيل مركز طبي", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.10", "MED_PIN", "صلاحية تثبيت مركز طبي", "تثبيت مركز طبي في أعلى القوائم والنتائج لجعله أكثر ظهوراً للمستخدمين", "مركز طبي محدد", "يسمح بتثبيت مركز طبي", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.11", "MED_UNPIN", "صلاحية إلغاء تثبيت مركز طبي", "إلغاء تثبيت مركز طبي وإعادته إلى ترتيبه الطبيعي في القوائم", "مركز طبي محدد", "يسمح بإلغاء تثبيت مركز طبي", PermissionLevel.MEDIUM, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.12", "MED_SET_VIP", "صلاحية تفعيل VIP لمركز طبي", "منح مركز طبي عضوية VIP مع ميزات وحقوق إضافية في التطبيق", "مركز طبي محدد", "يسمح بمنح VIP لمركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.13", "MED_UNSET_VIP", "صلاحية إلغاء VIP لمركز طبي", "إلغاء عضوية VIP عن مركز طبي وإعادة الميزات الأساسية فقط", "مركز طبي محدد", "يسمح بإلغاء VIP عن مركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.14", "MED_VERIFY", "صلاحية توثيق مركز طبي", "منح مركز طبي شارة التوثيق (الحساب الموثق) لإثبات مصداقيته", "مركز طبي محدد", "يسمح بتوثيق مركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.15", "MED_UNVERIFY", "صلاحية إلغاء توثيق مركز طبي", "إلغاء شارة التوثيق عن مركز طبي وإزالة الحساب الموثق", "مركز طبي محدد", "يسمح بإلغاء توثيق مركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.16", "MED_RECOMMEND", "صلاحية توصية مركز طبي", "منح مركز طبي شارة التوصية كدليل على جودة خدماته", "مركز طبي محدد", "يسمح بتوصية مركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL),
            AdminPermissionItem("12.17", "MED_UNRECOMMEND", "صلاحية إلغاء توصية مركز طبي", "إلغاء شارة التوصية عن مركز طبي وإزالة التوصية", "مركز طبي محدد", "يسمح بإلغاء توصية مركز طبي", PermissionLevel.ADVANCED, PermissionCategory.MEDICAL)
        ))

        // 13. إدارة العقارات (17 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("13.1", "PROP_VIEW_ALL", "صلاحية عرض جميع العقارات", "عرض قائمة كاملة بجميع العقارات المسجلة في التطبيق (للبيع، للإيجار، وغيرها)", "العقارات", "يسمح بعرض جميع العقارات", PermissionLevel.BASIC, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.2", "PROP_VIEW_SPECIFIC", "صلاحية عرض عقار محدد", "عرض بيانات ومعلومات عقار معين بشكل مفصل", "عقار محدد", "يسمح بعرض عقار محدد", PermissionLevel.BASIC, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.3", "PROP_ADD", "صلاحية إضافة عقار جديد", "إضافة عقار جديد (شقة، فيلا، أرض، مكتب، محل تجاري) إلى التطبيق يدوياً من لوحة التحكم", "عقارات جديدة", "يسمح بإضافة عقار جديد", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.4", "PROP_EDIT", "صلاحية تعديل عقار", "تعديل بيانات ومعلومات عقار موجود مسبقاً في التطبيق", "عقار محدد", "يسمح بتعديل عقار", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.5", "PROP_DELETE", "صلاحية حذف عقار", "حذف عقار نهائياً من التطبيق وجميع بياناته المرتبطة", "عقار محدد", "يسمح بحذف عقار نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.6", "PROP_BLOCK", "صلاحية حظر عقار", "حظر عقار ومنع مالكه من استخدام التطبيق بشكل مؤقت أو دائم", "عقار محدد", "يسمح بحظر عقار", PermissionLevel.SENSITIVE, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.7", "PROP_UNBLOCK", "صلاحية إلغاء حظر عقار", "إلغاء حظر عقار وإعادة تفعيل حسابه في التطبيق", "عقار محدد", "يسمح بإلغاء حظر عقار", PermissionLevel.SENSITIVE, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.8", "PROP_ACTIVATE", "صلاحية تفعيل عقار", "تفعيل حساب عقار وجعله نشطاً وقابلاً للظهور في التطبيق", "عقار محدد", "يسمح بتفعيل عقار", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.9", "PROP_DEACTIVATE", "صلاحية تعطيل عقار", "تعطيل حساب عقار مؤقتاً وجعله غير قابل للظهور في التطبيق", "عقار محدد", "يسمح بتعطيل عقار", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.10", "PROP_PIN", "صلاحية تثبيت عقار", "تثبيت عقار في أعلى القوائم والنتائج لجعله أكثر ظهوراً للمستخدمين", "عقار محدد", "يسمح بتثبيت عقار", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.11", "PROP_UNPIN", "صلاحية إلغاء تثبيت عقار", "إلغاء تثبيت عقار وإعادته إلى ترتيبه الطبيعي في القوائم", "عقار محدد", "يسمح بإلغاء تثبيت عقار", PermissionLevel.MEDIUM, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.12", "PROP_SET_VIP", "صلاحية تفعيل VIP لعقار", "منح عقار عضوية VIP مع ميزات وحقوق إضافية في التطبيق", "عقار محدد", "يسمح بمنح VIP لعقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.13", "PROP_UNSET_VIP", "صلاحية إلغاء VIP لعقار", "إلغاء عضوية VIP عن عقار وإعادة الميزات الأساسية فقط", "عقار محدد", "يسمح بإلغاء VIP عن عقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.14", "PROP_VERIFY", "صلاحية توثيق عقار", "منح عقار شارة التوثيق (الحساب الموثق) لإثبات مصداقيته", "عقار محدد", "يسمح بتوثيق عقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.15", "PROP_UNVERIFY", "صلاحية إلغاء توثيق عقار", "إلغاء شارة التوثيق عن عقار وإزالة الحساب الموثق", "عقار محدد", "يسمح بإلغاء توثيق عقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.16", "PROP_RECOMMEND", "صلاحية توصية عقار", "منح عقار شارة التوصية كدليل على جودة خدماته", "عقار محدد", "يسمح بتوصية عقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES),
            AdminPermissionItem("13.17", "PROP_UNRECOMMEND", "صلاحية إلغاء توصية عقار", "إلغاء شارة التوصية عن عقار وإزالة التوصية", "عقار محدد", "يسمح بإلغاء توصية عقار", PermissionLevel.ADVANCED, PermissionCategory.PROPERTIES)
        ))

        // 14. إدارة الوظائف (13 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("14.1", "JOB_VIEW_ALL", "صلاحية عرض جميع الوظائف", "عرض قائمة كاملة بجميع الوظائف المعلنة في التطبيق", "الوظائف", "يسمح بعرض جميع الوظائف", PermissionLevel.BASIC, PermissionCategory.JOBS),
            AdminPermissionItem("14.2", "JOB_VIEW_SPECIFIC", "صلاحية عرض وظيفة محددة", "عرض بيانات ومعلومات وظيفة معينة بشكل مفصل", "وظيفة محددة", "يسمح بعرض وظيفة محددة", PermissionLevel.BASIC, PermissionCategory.JOBS),
            AdminPermissionItem("14.3", "JOB_ADD", "صلاحية إضافة وظيفة جديدة", "إضافة وظيفة جديدة إلى التطبيق يدوياً من لوحة التحكم (المسمى، المتطلبات، المزايا، الراتب، الموقع)", "وظائف جديدة", "يسمح بإضافة وظيفة جديدة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.4", "JOB_EDIT", "صلاحية تعديل وظيفة", "تعديل بيانات ومعلومات وظيفة موجودة مسبقاً في التطبيق", "وظيفة محددة", "يسمح بتعديل وظيفة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.5", "JOB_DELETE", "صلاحية حذف وظيفة", "حذف وظيفة نهائياً من التطبيق وجميع بياناتها المرتبطة", "وظيفة محددة", "يسمح بحذف وظيفة نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.JOBS),
            AdminPermissionItem("14.6", "JOB_BLOCK", "صلاحية حظر وظيفة", "حظر وظيفة ومنع الشركة المعلنة من استخدام التطبيق بشكل مؤقت أو دائم", "وظيفة محددة", "يسمح بحظر وظيفة", PermissionLevel.SENSITIVE, PermissionCategory.JOBS),
            AdminPermissionItem("14.7", "JOB_UNBLOCK", "صلاحية إلغاء حظر وظيفة", "إلغاء حظر وظيفة وإعادة تفعيلها في التطبيق", "وظيفة محددة", "يسمح بإلغاء حظر وظيفة", PermissionLevel.SENSITIVE, PermissionCategory.JOBS),
            AdminPermissionItem("14.8", "JOB_ACTIVATE", "صلاحية تفعيل وظيفة", "تفعيل وظيفة وجعلها نشطة وقابلة للظهور في التطبيق", "وظيفة محددة", "يسمح بتفعيل وظيفة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.9", "JOB_DEACTIVATE", "صلاحية تعطيل وظيفة", "تعطيل وظيفة مؤقتاً وجعلها غير قابلة للظهور في التطبيق", "وظيفة محددة", "يسمح بتعطيل وظيفة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.10", "JOB_PIN", "صلاحية تثبيت وظيفة", "تثبيت وظيفة في أعلى القوائم والنتائج لجعلها أكثر ظهوراً للباحثين عن عمل", "وظيفة محددة", "يسمح بتثبيت وظيفة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.11", "JOB_UNPIN", "صلاحية إلغاء تثبيت وظيفة", "إلغاء تثبيت وظيفة وإعادتها إلى ترتيبها الطبيعي في القوائم", "وظيفة محددة", "يسمح بإلغاء تثبيت وظيفة", PermissionLevel.MEDIUM, PermissionCategory.JOBS),
            AdminPermissionItem("14.12", "JOB_SET_VIP", "صلاحية تفعيل VIP لوظيفة", "منح وظيفة عضوية VIP مع ميزات وحقوق إضافية في التطبيق", "وظيفة محددة", "يسمح بمنح VIP لوظيفة", PermissionLevel.ADVANCED, PermissionCategory.JOBS),
            AdminPermissionItem("14.13", "JOB_UNSET_VIP", "صلاحية إلغاء VIP لوظيفة", "إلغاء عضوية VIP عن وظيفة وإعادة الميزات الأساسية فقط", "وظيفة محددة", "يسمح بإلغاء VIP عن وظيفة", PermissionLevel.ADVANCED, PermissionCategory.JOBS)
        ))

        // 15. التبويبات المخصصة (27 صلاحية)
        list.addAll(listOf(
            AdminPermissionItem("15.1", "TAB_CREATE", "صلاحية إنشاء تبويب جديد", "إضافة تبويب مخصص جديد إلى واجهة التطبيق بكافة محتوياته وإعداداته", "جميع المستخدمين", "يسمح بإنشاء تبويب جديد", PermissionLevel.ADVANCED, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.2", "TAB_SET_NAME", "صلاحية تحديد اسم التبويب", "كتابة وتحديد اسم التبويب الجديد الذي سيظهر للمستخدمين", "التبويب الجديد", "يسمح بتحديد اسم التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.3", "TAB_SET_ICON", "صلاحية تحديد أيقونة التبويب", "اختيار أيقونة مناسبة للتبويب الجديد من مكتبة الأيقونات المتاحة", "التبويب الجديد", "يسمح بتحديد أيقونة التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.4", "TAB_SET_CONTENT_TYPE", "صلاحية تحديد نوع المحتوى", "تحديد نوع محتوى التبويب الجديد (نص عادي، HTML، روابط خارجية، صور، فيديو، نموذج تفاعلي)", "التبويب الجديد", "يسمح بتحديد نوع المحتوى", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.5", "TAB_SET_FIELDS_COUNT", "صلاحية تحديد عدد الحقول", "تحديد عدد الحقول التي سيحتوي عليها التبويب الجديد", "التبويب الجديد", "يسمح بتحديد عدد الحقول", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.6", "TAB_SET_FIELDS_ORDER", "صلاحية تحديد ترتيب الحقول", "تحديد ترتيب ظهور الحقول داخل التبويب (الحقل الأول يظهر أولاً)", "التبويب الجديد", "يسمح بترتيب الحقول", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.7", "TAB_SET_SIZE", "صلاحية تحديد حجم التبويب", "تحديد حجم التبويب المعروض في واجهة المستخدم (صغير، متوسط، كبير)", "التبويب الجديد", "يسمح بتحديد حجم التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.8", "TAB_SHOW_ALL", "صلاحية إظهار التبويب للجميع", "إظهار التبويب الجديد لجميع مستخدمي التطبيق بدون استثناء", "التبويب الجديد", "يظهر التبويب للجميع", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.9", "TAB_SHOW_USERS", "صلاحية إظهار التبويب للمستخدمين فقط", "إظهار التبويب الجديد للعملاء والمواطنين المسجلين فقط", "التبويب الجديد", "يظهر التبويب للعملاء فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.10", "TAB_SHOW_PROVIDERS", "صلاحية إظهار التبويب للفنيين فقط", "إظهار التبويب الجديد للفنيين ومقدمي الخدمات فقط", "التبويب الجديد", "يظهر التبويب للفنيين فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.11", "TAB_SHOW_STORES", "صلاحية إظهار التبويب للمحلات فقط", "إظهار التبويب الجديد للمحلات والمراكز التجارية فقط", "التبويب الجديد", "يظهر التبويب للمحلات فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.12", "TAB_SHOW_RESTAURANTS", "صلاحية إظهار التبويب للمطاعم فقط", "إظهار التبويب الجديد للمطاعم والكافيهات فقط", "التبويب الجديد", "يظهر التبويب للمطاعم فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.13", "TAB_SHOW_MEDICAL", "صلاحية إظهار التبويب للمراكز الطبية فقط", "إظهار التبويب الجديد للمراكز الطبية والعيادات فقط", "التبويب الجديد", "يظهر التبويب للمراكز الطبية فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.14", "TAB_SHOW_PROPERTIES", "صلاحية إظهار التبويب للعقارات فقط", "إظهار التبويب الجديد لمالكي ومعلني العقارات فقط", "التبويب الجديد", "يظهر التبويب للعقارات فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.15", "TAB_SHOW_JOBS", "صلاحية إظهار التبويب لمعلني الوظائف فقط", "إظهار التبويب الجديد للشركات والمعلنين عن وظائف فقط", "التبويب الجديد", "يظهر التبويب لمعلني الوظائف فقط", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.16", "TAB_SHOW_AREA", "صلاحية إظهار التبويب لمنطقة محددة", "إظهار التبويب الجديد للمستخدمين المسجلين في محافظة أو مدينة محددة فقط", "التبويب الجديد", "يظهر التبويب لمنطقة محددة فقط", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.17", "TAB_EDIT", "صلاحية تعديل تبويب", "تعديل بيانات ومحتويات وإعدادات تبويب موجود مسبقاً", "تبويب محدد", "يسمح بتعديل تبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.18", "TAB_DELETE", "صلاحية حذف تبويب", "حذف تبويب نهائياً من التطبيق وجميع محتوياته", "تبويب محدد", "يسمح بحذف تبويب نهائياً", PermissionLevel.SENSITIVE, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.19", "TAB_ACTIVATE", "صلاحية تفعيل تبويب", "تفعيل تبويب وجعله مرئياً وقابلاً للاستخدام في التطبيق", "تبويب محدد", "يسمح بتفعيل تبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.20", "TAB_DEACTIVATE", "صلاحية تعطيل تبويب", "تعطيل تبويب مؤقتاً وجعله غير مرئي وغير قابل للاستخدام في التطبيق", "تبويب محدد", "يسمح بتعطيل تبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.21", "TAB_REORDER", "صلاحية ترتيب التبويبات", "تحديد ترتيب ظهور التبويبات في واجهة التطبيق (التبويب الأول يظهر أولاً)", "جميع التبويبات", "يسمح بترتيب التبويبات", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.22", "TAB_RESIZE", "صلاحية تغيير حجم التبويب", "تغيير حجم تبويب موجود مسبقاً (صغير، متوسط، كبير)", "تبويب محدد", "يسمح بتغيير حجم التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.23", "TAB_SET_COLOR", "صلاحية تغيير لون التبويب", "تغيير لون خلفية أو حدود التبويب في واجهة المستخدم", "تبويب محدد", "يسمح بتغيير لون التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.24", "TAB_SET_FONT", "صلاحية تغيير خط التبويب", "تغيير نوع خط نص التبويب (Cairo، Amiri، Tahoma، وغيرها)", "تبويب محدد", "يسمح بتغيير خط التبويب", PermissionLevel.BASIC, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.25", "TAB_ADD_FIELD", "صلاحية إضافة حقل جديد", "إضافة حقل جديد إلى التبويب المخصص لزيادة المحتوى المعروض", "تبويب محدد", "يسمح بإضافة حقل جديد للتبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.26", "TAB_EDIT_FIELD", "صلاحية تعديل حقل", "تعديل اسم أو نوع أو محتوى حقل موجود في التبويب", "تبويب محدد", "يسمح بتعديل حقل في التبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS),
            AdminPermissionItem("15.27", "TAB_DELETE_FIELD", "صلاحية حذف حقل", "حذف حقل من التبويب بشكل نهائي", "تبويب محدد", "يسمح بحذف حقل من التبويب", PermissionLevel.MEDIUM, PermissionCategory.CUSTOM_TABS)
        ))

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
