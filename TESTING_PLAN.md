# 📋 خطة اختبارات الوحدة — دليل خدمات اليمن

## الوضع الحالي
- لا توجد اختبارات وحدة فعلية في المشروع
- البنية جاهزة (`app/src/test/`)

## الأولوية العالية — يُبدأ بها فوراً

### 1. BookingRepository
**السبب:** قلب نظام الحجوزات.
**ما يجب اختباره:**
- إنشاء حجز جديد بنجاح
- فشل الإنشاء عند تعارض الوقت
- التحقق من PIN بشكل صحيح
- قفل الحجز بعد 3 محاولات
- قاعدة 8 ساعات للإلغاء
- حفظ الحجز في الكاش المحلي

### 2. ChatRepository
**السبب:** نظام المحادثات الأساسي.
**ما يجب اختباره:**
- إرسال رسالة بنجاح
- استقبال الرسائل من Firestore
- إضافة الرسالة لطابور الأوفلاين عند الفشل
- إعادة إرسال الرسائل المعلقة

### 3. WalletManager
**السبب:** عمليات مالية حساسة.
**ما يجب اختباره:**
- إيداع رصيد صحيح
- سحب رصيد مع كفاية الرصيد
- فشل السحب عند عدم الكفاية
- رفض التحويل عند تجميد المحفظة
- منع الكسور في الريال اليمني

### 4. ValidatePhoneUseCase
**السبب:** مستخدم في كل التسجيلات.
**ما يجب اختباره:**
- قبول 771234567
- قبول +967771234567
- قبول 00967771234567
- قبول 0771234567
- رفض أرقام لا تبدأ بـ 77/73/71/70/78

### 5. AppPreferenceHelper.normalizePhoneNumber
**السبب:** جديد ومستخدم في كل مقارنات الهواتف.
**ما يجب اختباره:**
- تطبيع كل صيغ الأرقام اليمنية
- إرجاع 9 أرقام دائماً
- التعامل مع القيم الفارغة

## الأولوية المتوسطة

### 6. BookingStateMachine
- صحة الانتقالات المسموحة
- رفض الانتقالات غير المسموحة

### 7. ChatCryptoManager
- تشفير الرسائل بنجاح
- فك التشفير بنجاح
- فشل الفك عند التلاعب

## الأولوية المنخفضة

### 8. Validators
- التحقق من الأسماء والبريد وكلمات المرور

## الملفات المتوقع اختبارها (الترتيب)

1. `app/src/test/java/com/example/data/repositories/BookingRepositoryTest.kt`
2. `app/src/test/java/com/example/data/repositories/ChatRepositoryTest.kt`
3. `app/src/test/java/com/example/utils/WalletManagerTest.kt`
4. `app/src/test/java/com/example/domain/usecases/ValidatePhoneUseCaseTest.kt`
5. `app/src/test/java/com/example/ui/helpers/AppPreferenceHelperTest.kt`

## المكتبات المطلوبة
- JUnit 4 (موجودة)
- Mockito أو MockK (يجب إضافتها)
- kotlinx-coroutines-test (موجودة)
- Turbine (لاختبار StateFlow — يجب إضافتها)

## قواعد الاختبار
- استخدم FakeRepositories بدلاً من Firebase
- استخدم runTest من kotlinx-coroutines-test
- اكتب أسماء اختبارات واضحة بالعربية
- كل اختبار يجب أن يكون سريعاً (أقل من ثانية)

## الحالة الحالية للبنية
- ✅ البنية جاهزة (app/src/test/)
- ✅ JUnit و coroutines-test موجودان
- ⚠️ يجب إضافة Mockito/MockK قبل البدء
- ⚠️ يجب إضافة Turbine لاختبار الـ Flow
