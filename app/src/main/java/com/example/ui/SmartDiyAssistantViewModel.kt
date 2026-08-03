package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DiyTroubleIssue(
    val id: String,
    val category: String,
    val title: String,
    val icon: String,
    val description: String,
    val repairSteps: List<String>,
    val targetProfession: String
)

val defaultDiyIssues = listOf(
    DiyTroubleIssue(
        id = "diy_plumbing_leak",
        category = "سباكة ومياه",
        title = "تسريب مياه في الأنابيب أو الحنفية",
        icon = "🚰",
        description = "خطوات الفحص السريع لإيقاف تسرب المياه المنزلي قبل استدعاء السباك",
        repairSteps = listOf(
            "1. قم بإغلاق محبس المياه الرئيسي للشقة أو المنزل فوراً لمنع غرق المكان.",
            "2. قم بفك الصامولة الخارجية وتفقد جلدة المطاط (أو الشريط الكتاني/التفلون).",
            "3. ضع شريط تفلون (Teflon Tape) على قلاووظ الأنبوب وقم بربطه بإحكام باستعمال المفك.",
            "4. في حال استمرار التسريب من الأنبوب الرئيسي الداخلي، اضغط زر طلب فني السباكة الآن."
        ),
        targetProfession = "سباكة"
    ),
    DiyTroubleIssue(
        id = "diy_ac_not_cooling",
        category = "تكييف وتبريد",
        title = "المكيف يخرج هواء حار أو لا يبرد",
        icon = "❄️",
        description = "حلول معالجة ضعف تبريد المكيفات العادية والإسبليت",
        repairSteps = listOf(
            "1. قم بإيقاف تشغيل المكيف وفصل مفتاح الأمان الكهربائي.",
            "2. افتح الغطاء الأمامي وقم بسحب الفلاتر واغسلها بالماء الجاري وتجفيفها.",
            "3. تأكد من إعدادات الريموت على وضعية التبريد (Cooling) بدرجة حرارة 22-24.",
            "4. افحص الوحدة الخارجية، إذا كانت متسخة جداً أو مراوحها متوقفة قم باستدعاء فني التكييف."
        ),
        targetProfession = "تكييف وتبريد"
    ),
    DiyTroubleIssue(
        id = "diy_breaker_tripped",
        category = "كهرباء وطاقة",
        title = "انقطاع الكهرباء وتكرار فصل القاطع الرئيسي",
        icon = "⚡",
        description = "تشخيص القصر الكهربائي (Short Circuit) بطريقة آمنة",
        repairSteps = listOf(
            "1. قم بفصل جميع الأجهزة الكهربائية الثقيلة (المكيف، السخان، الثلاجة، الغسالة).",
            "2. اذهب للوحة القواطع واعد رفع القاطع الرئيسي (Main Breaker).",
            "3. قم بتوصيل الأجهزة جهازا تلو الآخر لمعرفة الجهاز المسبب لسرقة الكهرباء أو الماس.",
            "4. إذا فصل القاطع مجدداً بدون توصيل أي جهاز، لا تلمس الأسلاك واطلب فني الكهرباء فوراً."
        ),
        targetProfession = "كهربائي"
    ),
    DiyTroubleIssue(
        id = "diy_fridge_frost",
        category = "أجهزة منزلية",
        title = "تراكم الثلج بكثرة داخل الثلاجة",
        icon = "🧊",
        description = "خطوات معالجة انسداد مجرى الذوبان ورطوبة الثلاجات",
        repairSteps = listOf(
            "1. افصل الثلاجة عن الكهرباء تماماً لمدة 4 إلى 6 ساعات وافتح الأبواب.",
            "2. نظف الرباط المطاطي (الجلدة) حول الباب وتأكد من محاذاة الإغلاق التام.",
            "3. اسكب ماء دافئاً في فتحة التصريف الخلفية لتسليك أنبوب التكثيف.",
            "4. اعد تشغيل الثلاجة، وفي حال استمرار تراكم الثلج طلب فني صيانة الأجهزة."
        ),
        targetProfession = "صيانة ثلاجات"
    )
)

/**
 * 🛠️ SmartDiyAssistantViewModel:
 * Handles DIY troubleshooting guide & instant conversion to direct service booking.
 */
class SmartDiyAssistantViewModel : ViewModel() {
    private val _issues = MutableStateFlow<List<DiyTroubleIssue>>(defaultDiyIssues)
    val issues: StateFlow<List<DiyTroubleIssue>> = _issues

    private val _selectedIssue = MutableStateFlow<DiyTroubleIssue?>(null)
    val selectedIssue: StateFlow<DiyTroubleIssue?> = _selectedIssue

    fun selectIssue(issue: DiyTroubleIssue?) {
        _selectedIssue.value = issue
    }

    fun filterByCategory(query: String) {
        if (query.isBlank()) {
            _issues.value = defaultDiyIssues
        } else {
            _issues.value = defaultDiyIssues.filter {
                it.category.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
    }
}
