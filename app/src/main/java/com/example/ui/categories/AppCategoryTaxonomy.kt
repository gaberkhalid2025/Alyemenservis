package com.example.ui.categories

import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity

/**
 * 🗂️ الهيكلة الشاملة للأقسام الرئيسية والفرعية
 * تصنيفات دقيقة تغطي قطاعات المتاجر، المطاعم، المراكز الطبية، العقارات، والوظائف في اليمن.
 */
data class SubCategoryItem(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val keywords: List<String> = emptyList()
)

object AppCategoryTaxonomy {

    // 1) المحلات والمتاجر والمراكز التجارية (Stores & Malls)
    val storesSubCategories = listOf(
        SubCategoryItem("all", "الكل", "🏪"),
        SubCategoryItem(
            "grocery",
            "سوبرماركت ومواد غذائية 🛒",
            "🛒",
            listOf("سوبرماركت", "مواد غذائية", "تموينات", "بهارات", "توابل", "معلبات", "زيوت", "أجبان", "غذائي", "سوق")
        ),
        SubCategoryItem(
            "fashion",
            "ملابس وأزياء رجالية ونسائية 👔👗",
            "👔",
            listOf("ملابس", "أزياء", "معاوز", "أقمشة", "عبايات", "أطفال", "بدل", "ثياب", "رجالي", "نسائي")
        ),
        SubCategoryItem(
            "electronics",
            "إلكترونيات وهواتف ذكية 📱",
            "📱",
            listOf("إلكترونيات", "هواتف", "جوالات", "شواحن", "ملحقات", "لابتوب", "شاشات", "كمبيوتر", "صيانة جوالات")
        ),
        SubCategoryItem(
            "appliances",
            "أجهزة منزلية وكهربائية 🔌",
            "🔌",
            listOf("أجهزة منزلية", "كهربائية", "طاقة شمسية", "بطاريات", "برادات", "غسالات", "أفران", "ثلاجات", "انفرتر")
        ),
        SubCategoryItem(
            "home_kitchen",
            "أدوات منزلية ومطبخ 🍳",
            "🍳",
            listOf("أدوات منزلية", "مطبخ", "أواني", "مفروشات", "ستائر", "ضيافة", "أواني منزلية", "ديكور")
        ),
        SubCategoryItem(
            "shoes_bags",
            "أحذية وحقائب ومصنوعات جلدية 👞👜",
            "👞",
            listOf("أحذية", "حقائب", "مصنوعات جلدية", "شنط", "جزم", "أحذية رجالية", "أحذية نسائية")
        ),
        SubCategoryItem(
            "perfumes_beauty",
            "عطور وأدوات تجميل 🧴💄",
            "🧴",
            listOf("عطور", "أدوات تجميل", "بخور", "لادن", "عطورات", "عناية شخصية", "مكياج", "عود")
        )
    )

    // 2) المطاعم والكافيهات والمخابز (Restaurants & Cafes)
    val restaurantsSubCategories = listOf(
        SubCategoryItem("all", "الكل", "🍽️"),
        SubCategoryItem(
            "yemeni_traditional",
            "مطاعم شعبية ويمنية 🍲",
            "🍲",
            listOf("شعبي", "يمنية", "فحسة", "سلتة", "مندي", "مظبي", "زربيان", "حنيذ", "بلدي", "مطعم شعبي")
        ),
        SubCategoryItem(
            "seafood",
            "مأكولات بحرية وأسماك 🐟",
            "🐟",
            listOf("مأكولات بحرية", "أسماك", "سمك", "مشوي", "صيد طازج", "بحري", "جمبري", "سمك مشوي")
        ),
        SubCategoryItem(
            "fast_food",
            "وجبات سريعة وبوفيهات 🍔🍟",
            "🍔",
            listOf("وجبات سريعة", "بوفيه", "برجر", "شاورما", "سندوتش", "بيتزا", "فطائر", "بروست")
        ),
        SubCategoryItem(
            "bbq_meat",
            "مشاوي ومطابخ اللحوم 🍢🥩",
            "🍢",
            listOf("مشاوي", "لحوم", "كباب", "شيش طاووق", "مضغوط", "مشوي", "لحم بلدي", "مطبخ لحوم")
        ),
        SubCategoryItem(
            "bakery_sweets",
            "مخابز ومعجنات وحلويات 🥐🍰",
            "🥐",
            listOf("مخابز", "معجنات", "حلويات", "روتي", "رغيف", "بنت الصحن", "كنافة", "بقلاوة", "كيك")
        ),
        SubCategoryItem(
            "cafes_juices",
            "كافيهات ومقاهي وعصائر ☕🧃",
            "☕",
            listOf("كافيه", "مقهى", "عصائر", "بن يمني", "قشر", "فريش", "مشروبات باردة", "مثلجة", "شاي")
        )
    )

    // 3) المراكز والمجمعات الطبية (Medical & Health Centers)
    val medicalSubCategories = listOf(
        SubCategoryItem("all", "الكل", "🏥"),
        SubCategoryItem(
            "hospitals_complexes",
            "مستشفيات ومجمعات طبية عامة 🏥",
            "🏥",
            listOf("مستشفى", "مجمع طبي", "مركز طبي", "عام", "طوارئ", "مستوصف")
        ),
        SubCategoryItem(
            "specialized_clinics",
            "عيادات تخصصية 🩺",
            "🩺",
            listOf("عيادة", "عيادات تخصصية", "باطنية", "عظام", "أطفال", "نساء وتوليد", "جراحة", "جلدية", "قلب")
        ),
        SubCategoryItem(
            "dental",
            "طب وجراحة الأسنان 🦷",
            "🦷",
            listOf("أسنان", "طب أسنان", "تقويم", "زراعة أسنان", "جراحة أسنان")
        ),
        SubCategoryItem(
            "pharmacies",
            "صيدليات ومستلزمات علاجية 💊",
            "💊",
            listOf("صيدلية", "صيدليات", "مستلزمات علاجية", "أدوية", "مكملات", "دواء")
        ),
        SubCategoryItem(
            "laboratories",
            "مختبرات وتحاليل طبية 🧪",
            "🧪",
            listOf("مختبر", "مختبرات", "تحاليل طبية", "فحص", "دم", "تحليل")
        ),
        SubCategoryItem(
            "optics",
            "بصريات ونظارات 👓",
            "👓",
            listOf("بصريات", "نظارات", "عدسات", "فحص نظر", "عيون")
        )
    )

    // 4) العقارات والأملاك (Properties & Real Estate)
    val propertiesSubCategories = listOf(
        SubCategoryItem("all", "الكل", "🏢"),
        SubCategoryItem(
            "apartments_rent",
            "شقق للإيجار 🏢",
            "🏢",
            listOf("شقة", "شقق", "للإيجار", "مفروشة", "عوائل", "عزاب", "إيجار")
        ),
        SubCategoryItem(
            "houses_villas_sale",
            "بيوت وفلل للبيع 🏡",
            "🏡",
            listOf("بيت", "فيلا", "فلل", "للبيع", "منازل", "عمارة", "دور")
        ),
        SubCategoryItem(
            "land_plots",
            "أراضي ومخططات سكنية وتجارية 📐",
            "📐",
            listOf("أرض", "أراضي", "مخطط", "سكنية", "تجارية", "لبنة", "قصبة")
        ),
        SubCategoryItem(
            "commercial_offices",
            "محلات ومكاتب تجارية للإيجار والاستثمار 🏬",
            "🏬",
            listOf("محل", "مكاتب", "تجاري", "معرض", "استثمار", "فتحة", "دكان")
        )
    )

    // 5) إعلانات الوظائف والفرص المهنية (Careers & Jobs)
    val jobsSubCategories = listOf(
        SubCategoryItem("all", "الكل", "💼"),
        SubCategoryItem(
            "tech_engineering",
            "وظائف تقنية وهندسية 💻",
            "💻",
            listOf("تقنية", "هندسة", "برمجة", "شبكات", "كمبيوتر", "مصمم", "مهندس")
        ),
        SubCategoryItem(
            "sales_accounting",
            "وظائف مبيعات ومحاسبة وإدارة 📊",
            "📊",
            listOf("مبيعات", "محاسبة", "إدارة", "كاشير", "تسويق", "مندوب", "محاسب")
        ),
        SubCategoryItem(
            "crafts_trades",
            "فرص حرفية ومهنية (فنيين وصناع) 🔧",
            "🔧",
            listOf("فني", "صانع", "حرفية", "مهنية", "كهربائي", "سباك", "نجار", "حداد", "ميكانيكي")
        ),
        SubCategoryItem(
            "education_health",
            "وظائف تعليمية وصحية 📚🩺",
            "📚",
            listOf("تعليم", "مدرس", "صحي", "تمريض", "طبيب", "صيدلي", "معلم")
        )
    )

    /**
     * مطابقة تصنيف المتجر مع القسم الفرعي المختار
     */
    fun matchesStoreSubCategory(store: StoreEntity, selectedSubCategoryName: String): Boolean {
        if (selectedSubCategoryName == "الكل" || selectedSubCategoryName.isBlank()) return true
        val item = storesSubCategories.find { it.name == selectedSubCategoryName } ?: return true
        if (item.id == "all") return true

        val searchTarget = "${store.name} ${store.description} ${store.categoryId}".lowercase()
        return item.keywords.any { keyword -> searchTarget.contains(keyword.lowercase()) }
    }

    /**
     * مطابقة تصنيف المطعم مع القسم الفرعي المختار
     */
    fun matchesRestaurantSubCategory(store: StoreEntity, selectedSubCategoryName: String): Boolean {
        if (selectedSubCategoryName == "الكل" || selectedSubCategoryName.isBlank()) return true
        val item = restaurantsSubCategories.find { it.name == selectedSubCategoryName } ?: return true
        if (item.id == "all") return true

        val searchTarget = "${store.name} ${store.description} ${store.categoryId}".lowercase()
        return item.keywords.any { keyword -> searchTarget.contains(keyword.lowercase()) }
    }

    /**
     * مطابقة المركز الطبي مع القسم الفرعي
     */
    fun matchesMedicalSubCategory(provider: ProviderEntity, selectedSubCategoryName: String): Boolean {
        if (selectedSubCategoryName == "الكل" || selectedSubCategoryName.isBlank()) return true
        val item = medicalSubCategories.find { it.name == selectedSubCategoryName } ?: return true
        if (item.id == "all") return true

        val searchTarget = "${provider.name} ${provider.profession} ${provider.specialization} ${provider.categoryId}".lowercase()
        return item.keywords.any { keyword -> searchTarget.contains(keyword.lowercase()) }
    }

    /**
     * مطابقة العقار مع القسم الفرعي
     */
    fun matchesPropertySubCategory(property: PropertyEntity, selectedSubCategoryName: String): Boolean {
        if (selectedSubCategoryName == "الكل" || selectedSubCategoryName.isBlank()) return true
        val item = propertiesSubCategories.find { it.name == selectedSubCategoryName } ?: return true
        if (item.id == "all") return true

        val searchTarget = "${property.title} ${property.description} ${property.propertyType} ${property.type}".lowercase()
        return item.keywords.any { keyword -> searchTarget.contains(keyword.lowercase()) }
    }

    /**
     * 🕒 تنسيق ساعات العمل بصيغة عربية صحيحة اتجاهياً (RTL)
     * تضمن عدم عكس الوقت مثل AM - 10:00 PM 9:00 وتحولها إلى 9:00 ص - 10:00 م بدقة.
     */
    fun formatArabicWorkingHours(raw: String): String {
        if (raw.isBlank()) return "\u200F9:00 ص - 10:00 م\u200F"
        var clean = raw
            .replace("AM", "ص", ignoreCase = true)
            .replace("PM", "م", ignoreCase = true)
            .replace("am", "ص")
            .replace("pm", "م")
            .trim()

        return "\u200F$clean\u200F"
    }
}
