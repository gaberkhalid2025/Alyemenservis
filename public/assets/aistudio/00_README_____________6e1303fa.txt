# حزمة الإصلاحات – نظام الخرائط والملاحة (اليمن)

## كيف تستخدم هذه الحزمة مع AI Studio
1. ألصق **MASTER_PROMPT.md** في AI Studio أولاً.
2. ثم أرفق/ألصق ملفات المشروع المذكورة في كل مستند (الملف الحالي + مستند الإصلاح الخاص به).
3. نفّذ **مستنداً واحداً في كل مرة** بالترتيب أدناه، وابنِ المشروع (Build) بعد كل مستند. لا تدمج كل شيء دفعة واحدة.
4. إن ظهر خطأ بناء، أرسله لي كما هو.

## ترتيب التنفيذ (مهم)
| # | المستند | الملف الهدف | النوع |
|---|---------|-------------|-------|
| 01 | RealValhallaNavigator | com.example.utils/RealValhallaNavigator.kt | تعديل دوال |
| 02 | MockValhallaNavigator | com.example.utils/MockValhallaNavigator.kt | استبدال كامل |
| 03 | RouteTracker (جديد) | com.example.utils/RouteTracker.kt | **ملف جديد** |
| 04 | NavigationScreen | ui/screens/map/NavigationScreen.kt | **استبدال كامل** |
| 05 | MapScreenState | ui/screens/map/MapScreenState.kt | استبدال كامل |
| 06 | MapScreenContent | ui/screens/map/MapScreenContent.kt | تعديلات موضعية |
| 07 | RealLeafletMapView + map.html | RealLeafletMapView.kt + assets/map.html | تعديلات |
| 08 | OfflineTileInterceptor (جديد) | map/utils/OfflineTileInterceptor.kt | **ملف جديد** |
| 09 | البحث | RealValhallaNavigator + MapScreenContent | تعديلات |
| 10 | ValhallaTileDownloader | com.example.utils/ValhallaTileDownloader.kt | استبدال دوال |
| 11 | OfflineMapManager + MapDistanceCalculator | map/utils | تعديلات |
| 12 | MapBottomSheet + MapJsonBuilder | map/components + map/utils | تعديلات |
| 13 | Manifest + Gradle + تسجيل الانهيار | AndroidManifest.xml, build.gradle.kts, Application | تعديلات |
| 14 | تحسينات متفرقة (رادار/AR/كود ميت) | عدة ملفات | اختياري |

## ملفات لم أرها ويجب على AI Studio فتحها قبل التطبيق
`NavModels` (NavWaypoint, NavRouteResult, NavLeg, NavStep, NavError, ManeuverType, WaypointType)،
`NavConfig`، `OnlineOsrmNavigator`، `ArabicInstructionFormatter`، `HaversinePrecisionNavigator`،
`MainViewModel` (userLatitude/userLongitude/startLocationUpdates)، `getProviderCoords/getStoreCoords`، `AndroidManifest.xml`، `app/build.gradle.kts`.
**إن اختلفت أسماء الحقول عمّا هو مكتوب هنا، يعدّل AI Studio الكود ليطابق الموجود ولا يغيّر الموجود.**

## القواعد العامة
- لا تحذف كوداً يعمل. لا Placeholder. لا TODO.
- كل نص للمستخدم بالعربية (RTL).
- أي قيمة غير محسوبة من مسار حقيقي تُعرض بوسم «تقريبي».
- كل استدعاء شبكة/Valhalla خارج الخيط الرئيسي وداخل try/catch (يُلتقط `Throwable` ما عدا `CancellationException`).

## اختبار القبول بعد كل المراحل (على الهاتف)
1. أوفلاين + حزمة Valhalla سليمة: مسار حقيقي مرسوم على الخريطة، الأرقام غير مستديرة، تقل المسافة فقط عند التحرك.
2. أونلاين: الضغط على زر الملاحة لا ينهار.
3. بدون GPS: رسالة «فعّل GPS» وليس مساراً من وسط صنعاء.
4. كتابة «هايل» في البحث تعرض نتائج أو رسالة واضحة.
5. إضافة محطتين ثم الوصول للأولى: نطق «لقد وصلت إلى…» ثم «الوجهة التالية…» وإعادة حساب المسار.
6. الخروج عن المسار 5 ثوانٍ: «جارٍ إعادة حساب المسار».

## التقاط سبب الانهيار (إن بقي)
`adb logcat -b crash` أو Logcat في Android Studio بمستوى Error، ثم اضغط زر الملاحة وأرسل أسطر `FATAL EXCEPTION`.
