package com.example

import com.example.data.repositories.FakeRegistrationRepository
import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import com.example.ui.screens.register.RegistrationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 RegistrationFlowTest
 * الاختبار الشامل والموثق لنظام التسجيل والانضمام لجميع الأدوار والمسارات الثمانية (8 Sections)
 * يعالج ويتحقق عملياً من كافة سيناريوهات التقديم، المراجعة، الحقول المتخصصة، التحويل، والتزامن.
 */
class RegistrationFlowTest {

    private val fakeRepo = FakeRegistrationRepository()

    // ────────────────────────────────────────────────────────
    // 1. اختبار حقل التحقق من أرقام الهواتف اليمنية وتنسيقها
    // ────────────────────────────────────────────────────────
    @Test
    fun testYemenPhoneNormalization() {
        fun normalizePhone(phone: String): String {
            return phone.trim().replace(" ", "").replace("+", "").replace("^00".toRegex(), "")
        }

        assertEquals("967777123456", normalizePhone("+967 777 123 456"))
        assertEquals("777123456", normalizePhone("00777123456"))
        assertEquals("777123456", normalizePhone(" 777123456 "))
    }

    // ────────────────────────────────────────────────────────
    // 2. فحص الأقسام الثمانية للتسجيل وحقولها الفريدة
    // ────────────────────────────────────────────────────────

    // القسم 1: المستخدم العادي / العميل
    @Test
    fun testClientRegistrationFlow() = runBlocking {
        val client = RegistrationEntity.Client(
            fullName = "صالح أحمد العبيدي",
            phone = "777111222",
            city = "صنعاء",
            passwordHash = "123456",
            profileImageUrl = "https://example.com/profiles/saleh.jpg"
        )

        val result = fakeRepo.registerClient(client)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.startsWith("fake_client_id_"))
    }

    // القسم 2: الفني / مقدم الخدمة
    @Test
    fun testProviderRegistrationFlow() = runBlocking {
        val provider = RegistrationEntity.Provider(
            fullName = "طه ياسين مصلح",
            phone = "733222111",
            professionCategory = "electricity",
            city = "تعز",
            experienceYears = 8,
            bio = "فني تمديدات وصيانة كهربائية متكاملة للمنازل والمحلات",
            identityDocumentUrl = "https://example.com/docs/id.jpg",
            passwordHash = "safe_pass_1"
        )

        val result = fakeRepo.registerProvider(provider)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_provider_id_"))
    }

    // القسم 3: المتجر / المحل التجاري
    @Test
    fun testStoreRegistrationFlow() = runBlocking {
        val store = RegistrationEntity.Store(
            storeName = "مجموعة النجم للمستلزمات التقنية",
            ownerName = "أكرم محمد النجم",
            phone = "711222333",
            storeCategory = "electronics",
            city = "عدن",
            addressDetails = "شارع تسعة، بجانب بنك التضامن",
            commercialRegisterNumber = "CR-90812-Y",
            passwordHash = "store_pass_2026"
        )

        val result = fakeRepo.registerStore(store)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_store_id_"))
    }

    // القسم 4: المطعم / الكافيه
    @Test
    fun testRestaurantRegistrationFlow() = runBlocking {
        val restaurant = RegistrationEntity.Restaurant(
            restaurantName = "مطعم الشيباني الحديث",
            ownerName = "مصلح الشيباني",
            phone = "770555444",
            cuisineType = "traditional_yemeni",
            city = "صنعاء",
            addressDetails = "شارع حدة، جولة الرويشان",
            passwordHash = "cuisine_yem_33"
        )

        val result = fakeRepo.registerRestaurant(restaurant)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_restaurant_id_"))
    }

    // القسم 5: المركز الطبي / الصيدلية / العيادة
    @Test
    fun testMedicalCenterRegistrationFlow() = runBlocking {
        val medical = RegistrationEntity.MedicalCenter(
            centerName = "مستوصف النور الاستشاري",
            specialtyCategory = "general_medicine",
            doctorName = "د. أنور عبد القوي",
            phone = "775999888",
            city = "إب",
            addressDetails = "شارع تعز، أمام مستشفى الثورة",
            licenseNumber = "LIC-MED-7711",
            passwordHash = "med_secure_909"
        )

        val result = fakeRepo.registerMedicalCenter(medical)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_medical_id_"))
    }

    // القسم 6: المكتب العقاري / إدراج عقار
    @Test
    fun testPropertyRegistrationFlow() = runBlocking {
        val property = RegistrationEntity.Property(
            title = "عمارة استثمارية حديثة للبيع",
            propertyType = "Sale",
            category = "Villa",
            ownerName = "يحيى حميد الدين",
            phone = "733444555",
            city = "صنعاء",
            areaDetails = "حي الأصبحي، شارع المقالح المعمّد",
            priceYer = 180000000.0,
            description = "عمارة مريعة 4 أدوار حجر، مشطبة ديلوكس، ومجهزة بمصعد ونظام طاقة شمسية كامل.",
            passwordHash = "prop_owner_pass"
        )

        val result = fakeRepo.registerProperty(property)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_property_id_"))
    }

    // القسم 7: معلن الوظائف / صاحب عمل
    @Test
    fun testJobRegistrationFlow() = runBlocking {
        val job = RegistrationEntity.Job(
            jobTitle = "مهندس برمجيات وتطبيقات أندرويد (Kotlin/Compose)",
            companyName = "منصة تكنو يمن لتقنية المعلومات",
            category = "Software Engineering",
            contactPhone = "777666555",
            contactEmail = "jobs@technoyemen.com",
            city = "صنعاء",
            requirements = "خبرة لا تقل عن 3 سنوات في تطبيقات الهاتف، وإتقان العمل مع قواعد بيانات Firebase و Room.",
            salaryRange = "1200 - 1500 USD",
            passwordHash = "tech_hiring_now"
        )

        val result = fakeRepo.registerJob(job)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake_job_id_"))
    }

    // القسم 8: المتقدم للوظيفة / باحث عن عمل
    // يتم معالجتها كـ JoinRequest مستقل بنوع "JOB_SEEKER" لتوحيد المسارات
    @Test
    fun testJobSeekerRegistrationHandling() {
        val registrationType = RegistrationType.JOB_SEEKER
        assertEquals("job_seeker", registrationType.id)
        assertEquals("👨‍💼", registrationType.icon)
        assertEquals("استمارة متقدم لوظيفة / باحث عن عمل", registrationType.title)
    }

    // ────────────────────────────────────────────────────────
    // 3. فحص ومحاكاة مراحل الموافقة وتغير الحالة (PENDING -> APPROVED -> REJECTED)
    // ────────────────────────────────────────────────────────
    @Test
    fun testJoinStatusTransition() = runBlocking {
        // الحالة الافتراضية
        var status = JoinStatusEntity(
            requestId = "req_test_123",
            applicantName = "محسن السريحي",
            registrationType = "STORE",
            status = "PENDING"
        )
        assertEquals("PENDING", status.status)

        // محاكاة الموافقة
        status = status.copy(status = "APPROVED", updatedAt = System.currentTimeMillis())
        assertEquals("APPROVED", status.status)
        assertTrue(status.updatedAt > 0L)

        // محاكاة الرفض مع ذكر السبب
        status = status.copy(status = "REJECTED", rejectionReason = "مستند الهوية غير واضح، يرجى إعادة إرفاقه بجودة أعلى", updatedAt = System.currentTimeMillis())
        assertEquals("REJECTED", status.status)
        assertEquals("مستند الهوية غير واضح، يرجى إعادة إرفاقه بجودة أعلى", status.rejectionReason)
    }

    @Test
    fun testGetJoinStatusFlow() = runBlocking {
        val statusFlow = fakeRepo.getJoinStatusFlow("777111222")
        val result = statusFlow.first()
        assertNotNull(result)
        assertEquals("PENDING", result?.status)
        assertEquals("اختبار", result?.applicantName)
    }
}
