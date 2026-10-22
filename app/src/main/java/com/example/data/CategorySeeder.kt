package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * 🚀 CategorySeeder
 * تهيئة ومزامنة 170 قسماً وتصنيفاً للخدمات في اليمن سحابياً
 */
object CategorySeeder {
    private const val TAG = "CategorySeeder"

    val yemenCategories = listOf(
        CategoryEntity(id = "cat_001", name = "سباكة منزلية", icon = "🚰", order = 1),
        CategoryEntity(id = "cat_002", name = "تسليك مجاري", icon = "🪠", order = 2),
        CategoryEntity(id = "cat_003", name = "تركيب مغاسل وخلاطات", icon = "🚿", order = 3),
        CategoryEntity(id = "cat_004", name = "صيانة دينمو الماء", icon = "⚙️", order = 4),
        CategoryEntity(id = "cat_005", name = "تركيب عوامات خزانات", icon = "🎈", order = 5),
        CategoryEntity(id = "cat_006", name = "عزل خزانات مياه", icon = "🛡️", order = 6),
        CategoryEntity(id = "cat_007", name = "حفر آبار", icon = "🏗️", order = 7),
        CategoryEntity(id = "cat_008", name = "كهرباء منازل", icon = "⚡", order = 8),
        CategoryEntity(id = "cat_009", name = "تمديد أسلاك كهربائية", icon = "🔌", order = 9),
        CategoryEntity(id = "cat_010", name = "تركيب ثريات وإضاءة", icon = "💡", order = 10),
        CategoryEntity(id = "cat_011", name = "صيانة طبالين وقواطع", icon = "📟", order = 11),
        CategoryEntity(id = "cat_012", name = "تركيب أنظمة طاقة شمسية", icon = "☀️", order = 12),
        CategoryEntity(id = "cat_013", name = "صيانة بطاريات طاقة شمسية", icon = "🔋", order = 13),
        CategoryEntity(id = "cat_014", name = "تركيب انفرترات", icon = "📼", order = 14),
        CategoryEntity(id = "cat_015", name = "تنظيف ألواح شمسية", icon = "🧹", order = 15),
        CategoryEntity(id = "cat_016", name = "تكييف سبليت", icon = "❄️", order = 16),
        CategoryEntity(id = "cat_017", name = "تكييف شباك", icon = "🧊", order = 17),
        CategoryEntity(id = "cat_018", name = "تكييف صحراوي", icon = "💨", order = 18),
        CategoryEntity(id = "cat_019", name = "تعبئة فريون", icon = "🧪", order = 19),
        CategoryEntity(id = "cat_020", name = "صيانة ثلاجات", icon = "🍦", order = 20),
        CategoryEntity(id = "cat_021", name = "صيانة غسالات", icon = "🧺", order = 21),
        CategoryEntity(id = "cat_022", name = "صيانة أفران وبوتاجازات", icon = "🔥", order = 22),
        CategoryEntity(id = "cat_023", name = "صيانة ميكروويف", icon = "📻", order = 23),
        CategoryEntity(id = "cat_024", name = "نجارة أبواب وشبابيك", icon = "🚪", order = 24),
        CategoryEntity(id = "cat_025", name = "تركيب غرف نوم", icon = "🛏️", order = 25),
        CategoryEntity(id = "cat_026", name = "تنجيد كنب ومجالس", icon = "🛋️", order = 26),
        CategoryEntity(id = "cat_027", name = "صيانة مطابخ خشب", icon = "🪵", order = 27),
        CategoryEntity(id = "cat_028", name = "صيانة مطابخ ألمنيوم", icon = "🏗️", order = 28),
        CategoryEntity(id = "cat_029", name = "حدادة شبابيك وحمايات", icon = "🛡️", order = 29),
        CategoryEntity(id = "cat_030", name = "حدادة أبواب ليزر", icon = "💥", order = 30),
        CategoryEntity(id = "cat_031", name = "تركيب مظلات وسواتر", icon = "⛱️", order = 31),
        CategoryEntity(id = "cat_032", name = "لحام خزانات حديد", icon = "🔦", order = 32),
        CategoryEntity(id = "cat_033", name = "دهان جدران داخلي", icon = "🎨", order = 33),
        CategoryEntity(id = "cat_034", name = "دهان واجهات خارجية", icon = "🖌️", order = 34),
        CategoryEntity(id = "cat_035", name = "تركيب ورق جدران", icon = "📜", order = 35),
        CategoryEntity(id = "cat_036", name = "جبس بورد وديكور", icon = "🖼️", order = 36),
        CategoryEntity(id = "cat_037", name = "بناء طوب وبلوك", icon = "🧱", order = 37),
        CategoryEntity(id = "cat_038", name = "لياسة ولياسة ملونة", icon = "🏗️", order = 38),
        CategoryEntity(id = "cat_039", name = "تركيب بلاط وسيراميك", icon = "🔳", order = 39),
        CategoryEntity(id = "cat_040", name = "تركيب رخام وجرانيت", icon = "💎", order = 40),
        CategoryEntity(id = "cat_041", name = "صيانة مصاعد", icon = "🛗", order = 41),
        CategoryEntity(id = "cat_042", name = "صيانة مولدات كهربائية", icon = "🏭", order = 42),
        CategoryEntity(id = "cat_043", name = "صيانة مضخات ديزل", icon = "⛽", order = 43),
        CategoryEntity(id = "cat_044", name = "ميكانيكا سيارات", icon = "🚗", order = 44),
        CategoryEntity(id = "cat_045", name = "كهرباء سيارات", icon = "🔌", order = 45),
        CategoryEntity(id = "cat_046", name = "بنشر وتغيير زيوت", icon = "🔧", order = 46),
        CategoryEntity(id = "cat_047", name = "سمكرة ورش سيارات", icon = "🔨", order = 47),
        CategoryEntity(id = "cat_048", name = "تلميع سيارات", icon = "✨", order = 48),
        CategoryEntity(id = "cat_049", name = "غسيل سيارات متنقل", icon = "🧼", order = 49),
        CategoryEntity(id = "cat_050", name = "صيانة دراجات نارية", icon = "🏍️", order = 50),
        CategoryEntity(id = "cat_051", name = "نقل عفش وأثاث", icon = "🚚", order = 51),
        CategoryEntity(id = "cat_052", name = "تغليف أثاث", icon = "📦", order = 52),
        CategoryEntity(id = "cat_053", name = "تخزين أثاث", icon = "🏠", order = 53),
        CategoryEntity(id = "cat_054", name = "تنظيف فلل وشقق", icon = "🧹", order = 54),
        CategoryEntity(id = "cat_055", name = "تنظيف مجالس وستائر", icon = "🛋️", order = 55),
        CategoryEntity(id = "cat_056", name = "جلي بلاط وتلميع رخام", icon = "✨", order = 56),
        CategoryEntity(id = "cat_057", name = "رش مبيدات حشرية", icon = "🐜", order = 57),
        CategoryEntity(id = "cat_058", name = "مكافحة نمل أبيض", icon = "🦟", order = 58),
        CategoryEntity(id = "cat_059", name = "تعقيم منازل ومكاتب", icon = "🧼", order = 59),
        CategoryEntity(id = "cat_060", name = "تنسيق حدائق وشلالات", icon = "🌳", order = 60),
        CategoryEntity(id = "cat_061", name = "تركيب عشب صناعي", icon = "🍃", order = 61),
        CategoryEntity(id = "cat_062", name = "صيانة شبكات ري", icon = "💦", order = 62),
        CategoryEntity(id = "cat_063", name = "صيانة جوالات آيفون", icon = "📱", order = 63),
        CategoryEntity(id = "cat_064", name = "صيانة جوالات أندرويد", icon = "🤖", order = 64),
        CategoryEntity(id = "cat_065", name = "برمجة جوالات وفك شفرات", icon = "🔓", order = 65),
        CategoryEntity(id = "cat_066", name = "صيانة لابتوب وكمبيوتر", icon = "💻", order = 66),
        CategoryEntity(id = "cat_067", name = "تركيب شبكات انترنت", icon = "🌐", order = 67),
        CategoryEntity(id = "cat_068", name = "تقوية إشارة واي فاي", icon = "📶", order = 68),
        CategoryEntity(id = "cat_069", name = "تركيب كاميرات مراقبة", icon = "📷", order = 69),
        CategoryEntity(id = "cat_070", name = "صيانة أجهزة بصمة وأمن", icon = "☝️", order = 70),
        CategoryEntity(id = "cat_071", name = "برمجة ريسفرات ودشات", icon = "📡", order = 71),
        CategoryEntity(id = "cat_072", name = "صيانة شاشات وتلفزيونات", icon = "📺", order = 72),
        CategoryEntity(id = "cat_073", name = "خدمات ترجمة معتمدة", icon = "🌍", order = 73),
        CategoryEntity(id = "cat_074", name = "تعقيب معاملات رسمية", icon = "📄", order = 74),
        CategoryEntity(id = "cat_075", name = "خدمات محاسبة وتدقيق", icon = "📊", order = 75),
        CategoryEntity(id = "cat_076", name = "دراسات جدوى مشاريع", icon = "📈", order = 76),
        CategoryEntity(id = "cat_077", name = "تصميم جرافيك وشعارات", icon = "✒️", order = 77),
        CategoryEntity(id = "cat_078", name = "تسويق إلكتروني", icon = "📢", order = 78),
        CategoryEntity(id = "cat_079", name = "إدارة صفحات تواصل", icon = "👥", order = 79),
        CategoryEntity(id = "cat_080", name = "برمجة مواقع وتطبيقات", icon = "⌨️", order = 80),
        CategoryEntity(id = "cat_081", name = "تصوير فوتوغرافي وفيديو", icon = "📸", order = 81),
        CategoryEntity(id = "cat_082", name = "مونتاج وتحرير فيديو", icon = "🎞️", order = 82),
        CategoryEntity(id = "cat_083", name = "تنظيم حفلات ومناسبات", icon = "🎉", order = 83),
        CategoryEntity(id = "cat_084", name = "خدمات ضيافة وقهوجيين", icon = "☕", order = 84),
        CategoryEntity(id = "cat_085", name = "تأجير لوازم مناسبات", icon = "🎪", order = 85),
        CategoryEntity(id = "cat_086", name = "خياطة رجالية (ثياب)", icon = "🧵", order = 86),
        CategoryEntity(id = "cat_087", name = "خياطة نسائية (عبايات)", icon = "👗", order = 87),
        CategoryEntity(id = "cat_088", name = "مغاسل ملابس (دراي كلين)", icon = "🧼", order = 88),
        CategoryEntity(id = "cat_089", name = "صالون حلاقة رجالي", icon = "✂️", order = 89),
        CategoryEntity(id = "cat_090", name = "مشغل نسائي وتجميل", icon = "💄", order = 90),
        CategoryEntity(id = "cat_091", name = "نقش حناء", icon = "✋", order = 91),
        CategoryEntity(id = "cat_092", name = "تجارة جملة مواد غذائية", icon = "🍱", order = 92),
        CategoryEntity(id = "cat_093", name = "سوبر ماركت وبقالة", icon = "🛒", order = 93),
        CategoryEntity(id = "cat_094", name = "خضروات وفواكه", icon = "🍏", order = 94),
        CategoryEntity(id = "cat_095", name = "لحوم وطازج", icon = "🥩", order = 95),
        CategoryEntity(id = "cat_096", name = "دواجن وبيض", icon = "🍗", order = 96),
        CategoryEntity(id = "cat_097", name = "مخابز وأفران آلي", icon = "🥖", order = 97),
        CategoryEntity(id = "cat_098", name = "حلويات ومعجنات", icon = "🍰", order = 98),
        CategoryEntity(id = "cat_099", name = "مكسرات وعطارة", icon = "🥜", order = 99),
        CategoryEntity(id = "cat_100", name = "عسل يمني أصلي", icon = "🍯", order = 100),
        CategoryEntity(id = "cat_101", name = "مطعم شعبي (مندي وسلتة)", icon = "🥘", order = 101),
        CategoryEntity(id = "cat_102", name = "مطعم وجبات سريعة", icon = "🍔", order = 102),
        CategoryEntity(id = "cat_103", name = "مطعم أسماك ومأكولات بحرية", icon = "🐟", order = 103),
        CategoryEntity(id = "cat_104", name = "مطعم مشويات ومعجنات", icon = "🍢", order = 104),
        CategoryEntity(id = "cat_105", name = "مطعم بخاري وارز", icon = "🍚", order = 105),
        CategoryEntity(id = "cat_106", name = "كافيه ومقهى", icon = "☕", order = 106),
        CategoryEntity(id = "cat_107", name = "محل عصائر وآيس كريم", icon = "🍹", order = 107),
        CategoryEntity(id = "cat_108", name = "بوفيه وسندوتشات", icon = "🥪", order = 108),
        CategoryEntity(id = "cat_109", name = "صيدلية دواء", icon = "💊", order = 109),
        CategoryEntity(id = "cat_110", name = "مستشفى عام", icon = "🏥", order = 110),
        CategoryEntity(id = "cat_111", name = "مستوصف طبي", icon = "🩺", order = 111),
        CategoryEntity(id = "cat_112", name = "عيادة أسنان", icon = "🦷", order = 112),
        CategoryEntity(id = "cat_113", name = "عيادة عيون", icon = "👁️", order = 113),
        CategoryEntity(id = "cat_114", name = "عيادة أطفال", icon = "👶", order = 114),
        CategoryEntity(id = "cat_115", name = "عيادة نساء وتوليد", icon = "🤰", order = 115),
        CategoryEntity(id = "cat_116", name = "عيادة باطنية وقلب", icon = "❤️", order = 116),
        CategoryEntity(id = "cat_117", name = "عيادة جلدية وتجميل", icon = "✨", order = 117),
        CategoryEntity(id = "cat_118", name = "مختبر تحاليل طبية", icon = "🔬", order = 118),
        CategoryEntity(id = "cat_119", name = "مركز أشعة وتشخيص", icon = "🩻", order = 119),
        CategoryEntity(id = "cat_120", name = "علاج طبيعي وتأهيل", icon = "🏃", order = 120),
        CategoryEntity(id = "cat_121", name = "عيادة طب نفسي", icon = "🧠", order = 121),
        CategoryEntity(id = "cat_122", name = "محل نظارات طبية", icon = "👓", order = 122),
        CategoryEntity(id = "cat_123", name = "مستلزمات طبية", icon = "🩹", order = 123),
        CategoryEntity(id = "cat_124", name = "مدرسة أهلية", icon = "🏫", order = 124),
        CategoryEntity(id = "cat_125", name = "روضة أطفال", icon = "🧸", order = 125),
        CategoryEntity(id = "cat_126", name = "معهد لغات وكمبيوتر", icon = "📚", order = 126),
        CategoryEntity(id = "cat_127", name = "مركز تدريب وتطوير", icon = "🎓", order = 127),
        CategoryEntity(id = "cat_128", name = "مدرس خصوصي", icon = "👨‍🏫", order = 128),
        CategoryEntity(id = "cat_129", name = "مكتب عقارات", icon = "🏢", order = 129),
        CategoryEntity(id = "cat_130", name = "بيع وشراء أراضي", icon = "🏗️", order = 130),
        CategoryEntity(id = "cat_131", name = "تأجير شقق وفلل", icon = "🔑", order = 131),
        CategoryEntity(id = "cat_132", name = "إدارة أملاك وعقارات", icon = "📝", order = 132),
        CategoryEntity(id = "cat_133", name = "هندسة وتخطيط عمراني", icon = "📐", order = 133),
        CategoryEntity(id = "cat_134", name = "مقاولات بناء وترميم", icon = "🏗️", order = 134),
        CategoryEntity(id = "cat_135", name = "مكتب محاماة واستشارات", icon = "⚖️", order = 135),
        CategoryEntity(id = "cat_136", name = "مكتب تخليص جمركي", icon = "🚢", order = 136),
        CategoryEntity(id = "cat_137", name = "مكتب استقدام عمالة", icon = "🛂", order = 137),
        CategoryEntity(id = "cat_138", name = "وكالة سفر وسياحة", icon = "✈️", order = 138),
        CategoryEntity(id = "cat_139", name = "مكتب حج وعمرة", icon = "🕋", order = 139),
        CategoryEntity(id = "cat_140", name = "حجز تذاكر طيران", icon = "🎟️", order = 140),
        CategoryEntity(id = "cat_141", name = "تأجير سيارات", icon = "🚘", order = 141),
        CategoryEntity(id = "cat_142", name = "خدمة ليموزين وتاكسي", icon = "🚕", order = 142),
        CategoryEntity(id = "cat_143", name = "نقل ركاب دولي (باصات)", icon = "🚌", order = 143),
        CategoryEntity(id = "cat_144", name = "شحن بضائع داخلي", icon = "🚛", order = 144),
        CategoryEntity(id = "cat_145", name = "شحن دولي وسريع", icon = "📦", order = 145),
        CategoryEntity(id = "cat_146", name = "بيع مواد بناء", icon = "🧱", order = 146),
        CategoryEntity(id = "cat_147", name = "بيع أدوات صحية", icon = "🚽", order = 147),
        CategoryEntity(id = "cat_148", name = "بيع مواد كهربائية", icon = "💡", order = 148),
        CategoryEntity(id = "cat_149", name = "بيع قطع غيار سيارات", icon = "⚙️", order = 149),
        CategoryEntity(id = "cat_150", name = "بيع إطارات وبطاريات", icon = "⭕", order = 150),
        CategoryEntity(id = "cat_151", name = "معرض أجهزة إلكترونية", icon = "🖥️", order = 151),
        CategoryEntity(id = "cat_152", name = "معرض أثاث منزلي", icon = "🛋️", order = 152),
        CategoryEntity(id = "cat_153", name = "معرض سجاد ومفروشات", icon = "🧶", order = 153),
        CategoryEntity(id = "cat_154", name = "معرض ملابس جاهزة", icon = "👕", order = 154),
        CategoryEntity(id = "cat_155", name = "معرض أحذية وحقائب", icon = "👞", order = 155),
        CategoryEntity(id = "cat_156", name = "محل عطور وبخور", icon = "🧴", order = 156),
        CategoryEntity(id = "cat_157", name = "محل ذهب ومجوهرات", icon = "💍", order = 157),
        CategoryEntity(id = "cat_158", name = "محل ساعات وهدايا", icon = "🎁", order = 158),
        CategoryEntity(id = "cat_159", name = "مكتبة وقرطاسية", icon = "✏️", order = 159),
        CategoryEntity(id = "cat_160", name = "بيع ألعاب أطفال", icon = "🧸", order = 160),
        CategoryEntity(id = "cat_161", name = "نادي رياضي (جيم)", icon = "💪", order = 161),
        CategoryEntity(id = "cat_162", name = "ملعب كرة قدم إيجار", icon = "⚽", order = 162),
        CategoryEntity(id = "cat_163", name = "مسبح واستراحة إيجار", icon = "🏊", order = 163),
        CategoryEntity(id = "cat_164", name = "كوافير أطفال", icon = "💈", order = 164),
        CategoryEntity(id = "cat_165", name = "صيانة خزائن ودواليب", icon = "🗄️", order = 165),
        CategoryEntity(id = "cat_166", name = "تركيب باركيه", icon = "🪵", order = 166),
        CategoryEntity(id = "cat_167", name = "صيانة ثلاجات عرض", icon = "🥩", order = 167),
        CategoryEntity(id = "cat_168", name = "فني مطابخ مركزي", icon = "🏢", order = 168),
        CategoryEntity(id = "cat_169", name = "صيانة مكائن خياطة", icon = "🧵", order = 169),
        CategoryEntity(id = "cat_170", name = "خدمات أمنية وحراسة", icon = "👮", order = 170)
    )

    /**
     * تنفيذ مزامنة الـ 170 قسماً إلى Firestore
     */
    suspend fun sync170Categories() {
        val db = FirebaseFirestore.getInstance()
        val collection = db.collection("categories")

        // تقسيم البيانات إلى مجموعات (Batches) لأن Firestore يدعم 500 عملية في الـ Batch الواحد
        // هنا 170 تكفي في Batch واحد، ولكن سنستخدم WriteBatch للأمان
        val batch: WriteBatch = db.batch()

        yemenCategories.forEach { category ->
            val docRef = collection.document(category.id)
            batch.set(docRef, category)
        }

        try {
            batch.commit().await()
            Log.i(TAG, "Successfully synced 170 categories to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync categories: ${e.message}", e)
            throw e
        }
    }
}
