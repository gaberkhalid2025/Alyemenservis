# 🧪 دليل اختبارات الوحدة — دليل خدمات اليمن

يوضح هذا الدليل كيفية كتابة اختبارات وحدة (Unit Tests) فعالة باستخدام JUnit 4، Coroutines، و MockK في هذا المشروع.

## 🛠️ الأدوات المستخدمة
- **JUnit 4**: الإطار الأساسي للاختبارات.
- **MockK**: مكتبة المحاكاة (Mocking) المفضلة للغة Kotlin.
- **kotlinx-coroutines-test**: لاختبار الأكواد غير المتزامنة.
- **Turbine**: لاختبار الـ StateFlow والـ Flow بشكل مبسط.

---

## 🏗️ الهيكل الأساسي للاختبار

يجب وضع ملفات الاختبار في المجلد `app/src/test/java/com/example/`.

```kotlin
class ExampleTest {

    // 1. استخدام CoroutineTestRule لتوحيد الـ Dispatcher
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    // 2. تعريف التبعيات المراد محاكاتها
    private val repository = TestMockFactory.createBookingRepository()
    
    // 3. تعريف الفئة المراد اختبارها (Subject Under Test)
    private lateinit var viewModel: MyViewModel

    @Before
    fun setup() {
        viewModel = MyViewModel(repository)
    }

    @Test
    fun `يجب تحديث الحالة عند نجاح العملية`() = runTest {
        // Arrange: إعداد السلوك المتوقع للمحاكي
        coEvery { repository.getData() } returns Result.success(data)

        // Act: تنفيذ العملية
        viewModel.fetchData()

        // Assert: التحقق من النتائج
        assertEquals(expectedState, viewModel.uiState.value)
    }
}
```

---

## 🎭 محاكاة التبعيات (Mocking) باستخدام MockK

### المحاكاة البسيطة
```kotlin
val repo = mockk<MyRepository>()
every { repo.name } returns "Test Name"
```

### محاكاة الدوال المعلقة (Suspend Functions)
```kotlin
coEvery { repo.fetchData() } returns Result.success(list)
```

### التحقق من الاستدعاء (Verification)
```kotlin
coVerify { repo.saveData(any()) }
```

---

## ⏳ اختبار الـ Coroutines

استخدم دائماً `runTest` من مكتبة `kotlinx-coroutines-test`. ستقوم هذه الدالة بتسريع الوقت الافتراضي وتوفير بيئة اختبار مستقرة.

```kotlin
@Test
fun myCoroutineTest() = runTest {
    // الكود هنا يعمل في بيئة اختبار متزامنة زمنياً
}
```

---

## 🌊 اختبار الـ Flows باستخدام Turbine

بدلاً من تجميع الـ Flow يدوياً، استخدم Turbine:

```kotlin
@Test
fun testFlow() = runTest {
    viewModel.uiState.test {
        assertEquals(InitialState, awaitItem())
        viewModel.load()
        assertEquals(LoadingState, awaitItem())
        assertEquals(SuccessState, awaitItem())
    }
}
```

---

## 📏 القواعد الذهبية
1. **السرعة**: يجب أن يكون اختبار الوحدة سريعاً جداً.
2. **الاستقلالية**: لا يجب أن يعتمد اختبار على نتيجة اختبار آخر.
3. **الوضوح**: استخدم أسماء اختبارات تعبر عن السلوك المتوقع (يفضل بالعربية لمطابقة سياق المشروع).
4. **عزل التبعيات**: لا تستخدم Firebase أو الشبكة أو قاعدة البيانات الحقيقية؛ استخدم المحاكاة (Mocks) أو النسخ المزيفة (Fakes).
