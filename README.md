# 🇾🇪 دليل خدمات اليمن الشامل — Yemen Services Directory

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Min%20SDK-24-orange.svg?style=for-the-badge&logo=android" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-35-green.svg?style=for-the-badge&logo=android" alt="Target SDK">
  <img src="https://img.shields.io/badge/Tests-224%20Passed-brightgreen.svg?style=for-the-badge" alt="Tests">
  <img src="https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge" alt="License">
</p>

---

## 📌 جدول المحتويات / Table of Contents
- [1. الوصف العام / Overview](#1-الوصف-العام--overview)
- [2. شارات الحالة / Status Badges](#2-شارات-الحالة--status-badges)
- [3. الميزات الرئيسية / Key Features](#3-الميزات-الرئيسية--key-features)
- [4. البنية التقنية والمعمارية / Tech Stack & Architecture](#4-البنية-التقنية-والمعمارية--tech-stack--architecture)
- [5. دليل التثبيت والتشغيل / Setup & Installation Guide](#5-دليل-التثبيت-والتشغيل--setup--installation-guide)
- [6. استراتيجية وجدول الاختبارات / Testing Strategy & Grid](#6-استراتيجية-وجدول-الاختبارات--testing-strategy--grid)
- [7. حالة ميزات المشروع / Project Feature Status](#7-حالة-ميزات-المشروع--project-feature-status)
- [8. المساهمة وتطوير الكود / Contributing & Code Standards](#8-المساهمة-وتطوير-الكود--contributing--code-standards)
- [9. الأسئلة الشائعة / FAQ](#9-الأسئلة-الشائعة--faq)
- [10. الترخيص والتواصل / License & Contact](#10-الترخيص-والتواصل--license--contact)

---

## 1. الوصف العام / Overview

### **العربية 🇾🇪**
تطبيق **"دليل خدمات اليمن الشامل"** هو منصة رقمية رائدة ومتكاملة مصممة خصيصاً لتلبية احتياجات السوق اليمني لربط مقدمي الخدمات بمختلف مجالاتهم (فنيون مهنيون، متاجر، مطاعم، مراكز طبية، وعقارات) بالعملاء مباشرة دون وسيط. يتميز التطبيق بالموثوقية العالية والسرعة الفائقة حيث يحتوي على محادثات فورية مشفرة محلياً وسحابياً، ونظام حجز آمن مؤمن برمز PIN، ونظام طلبات طوارئ للمهن السريعة، وخريطة متكاملة لحساب المسافات، مع محفظة مالية متعددة العملات لتتبع الحسابات والمدفوعات بدقة وموثوقية في ظل الاتصال الضعيف بالإنترنت بفضل بنية (Offline-First).

### **English 🇬🇧**
**"Yemen Services Directory"** is a cutting-edge, full-featured Android application designed specifically to elevate service delivery in the Yemeni market. It serves as a secure, direct link between local service providers (technicians, shops, restaurants, clinics, real estate agencies) and customers. Optimized for reliability and performance over low-bandwidth connections, the app implements advanced AES-256 chat encryption, PIN-secured local/cloud booking flows, instant emergency dispatch dispatching, custom leaflet maps with distance calculation, and a dual-currency digital ledger (YER)—engineered with an offline-first resilient architecture.

---

## 2. شارات الحالة / Status Badges

*هذه الشارات توضح حالة التحديث المستمر وجودة بناء المشروع:*

| الشارة (Badge) | الوصف (Description) | الحالة الحالية (Current Status) |
| :--- | :--- | :--- |
| **GitHub Actions Build** | حالة البناء والتشغيل التلقائي | ![Build Status](https://github.com/gaber77710/YemenServicesDirectory/actions/workflows/android.yml/badge.svg) |
| **Kotlin Language** | لغة البرمجة المعتمدة | ![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue?style=flat-square&logo=kotlin) |
| **Android Target SDK** | إصدار الأندرويد المستهدف | ![Target SDK](https://img.shields.io/badge/Android%20Target%20SDK-35-brightgreen?style=flat-square&logo=android) |
| **Android Minimum SDK** | الحد الأدنى لتشغيل التطبيق | ![Min SDK](https://img.shields.io/badge/Android%20Min%20SDK-24-orange?style=flat-square&logo=android) |
| **Testing Coverage** | عدد الاختبارات الناجحة | ![Tests Passed](https://img.shields.io/badge/Tests-224%20Passed-brightgreen?style=flat-square&logo=junit5) |
| **Project License** | رخصة استخدام وحقوق المشروع | ![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square) |

---

## 3. الميزات الرئيسية / Key Features

### **العربية 🇾🇪**
* 🔒 **تسجيل الدخول الآمن عبر Firebase Auth:** نظام تسجيل مرن وموثوق يدعم التحقق التلقائي واليدوي عبر أرقام الهواتف وشبكات الاتصال المحلية في اليمن (سبأفون، يمن موبايل، يو، واي).
* 📍 **خريطة تفاعلية وحساب مسافات ذكي:** خريطة مخصصة بالكامل مبنية على OpenStreetMap و Leaflet تعمل بدون تكاليف مفاتيح Google Maps وتوفر تصفحاً جغرافياً سريعاً مع قياس دقيق للمسافات بالكيلومتر والمتر.
* 💬 **محادثات فورية مشفرة بـ AES-256:** نظام شات فوري يدعم النصوص، الصور، الملفات، والملاحظات الصوتية مع تشفير آمن وحفظ مؤقت كامل لإتاحة تصفح المحادثات وتجهيز الرسائل دون إنترنت.
* 📌 **حجز مباشر آمن بررمز PIN:** تنظيم الحجوزات مع مقدمي الخدمات والتحقق من إتمام الخدمة بنجاح عبر نظام حماية بالـ PIN لتأكيد المعاملة ومنع التلاعب.
* 🚨 **طلبات الطوارئ مع عروض الأسعار:** إمكانية تقديم طلب فوري وعاجل (سباكة، بنشر، كهرباء) يظهر كـ "رادار طوارئ" لمزودي الخدمة القريبين ليقدموا عروض أسعار تنافسية واختيار الأنسب فوراً.
* 💰 **محفظة إلكترونية للريال اليمني (YER):** نظام مالي مدمج يدعم العملة المحلية بالكامل وتتبع الأرباح، عمليات السحب، المعاملات، والمكافآت، مع حماية العمليات وتأمين الأرصدة.
* 🔔 **إشعارات فورية ذكية عبر FCM:** تصنيف دقيق للإشعارات الواردة ومزامنتها محلياً وسحابياً وإتاحة تصنيفها (الكل، غير مقروءة، مقروءة).
* 🌐 **دعم كامل للغة العربية وتخطيط RTL:** تجربة بصرية عربية أصيلة مع تصميم مخصص مريح متوافق مع اللهجات والعبارات اليمنية المحلية وتوافق 100% مع واجهة المطور اليميني.

### **English 🇬🇧**
* 🔒 **Secure Firebase Authentication:** Robust phone number verification optimized for local Yemeni telecom operators (Yemen Mobile, Sabafon, YOU, Y), with automated role assignment.
* 📍 **Interactive Maps & Geocoding:** Fully customizable Leaflet-based OpenStreetMap layout running via WebView to eliminate API billing costs while providing fast distance calculations.
* 💬 **AES-256 Encrypted Real-time Chat:** Feature-rich chat supporting text, voice, and media attachments with end-to-end local/remote encryption and local drafts caching.
* 📌 **PIN-Secured Booking Workflows:** Reliable scheduling engine validating successful service completions via dynamic user-specific PIN tokens to prevent fraud.
* 🚨 **Emergency Radar & Live Quotes:** Post urgent tasks (plumbing, car mechanic, electrical failure) on a map-integrated dashboard and instantly receive competitive quotes.
* 💰 **Dual-Currency Digital Wallet (YER):** Complete ledger engine tracking earnings, transactions, and deposit withdrawals using the Yemeni Rial and foreign currencies safely.
* 🔔 **FCM Smart Push Notifications:** Rich system alerts categorization, maintaining persistent notifications state offline and syncing whenever the network is restored.
* 🌐 **Fully-Native Arabic & RTL Design:** Smooth, beautiful user experiences respecting localized Yemeni terminology, language dialects, and strict RTL guidelines.

---

## 4. البنية التقنية والمعمارية / Tech Stack & Architecture

تم تصميم وتطوير التطبيق بالاعتماد على أفضل وأحدث الممارسات الهندسية لنظام Android لضمان أقصى قدر من الثبات والأداء وقابلية التوسع.

### **الهيكل المعماري (Architectural Layers)**
يتبع التطبيق معمارية **Clean Architecture** مع نظام **MVVM (Model-View-ViewModel)** ونهج **Offline-First**:
1. **Presentation Layer (UI)**: مبني بالكامل بـ **Jetpack Compose** بنمط التصميم المادي الثالث **Material Design 3** وتخطيطات متجاوبة مرنة.
2. **Domain Layer**: يحتوي على النماذج الأساسية وقواعد الأعمال النظيفة المستقلة (Use Cases) وآلات الحالات الصارمة للتحويلات وحماية المدفوعات.
3. **Data Layer**: يمثل مستودع البيانات (Repositories) وينسق تدفق البيانات ثنائي الاتجاه بين الكاش المحلي **Room Database** ومصادر البيانات البعيدة **Firebase/Firestore/APIs**.

### **المكونات والتقنيات الأساسية (Core Tech Stack)**
* **Language**: Kotlin 2.1.0 (إصدار حديث مع أداء برمجي عالٍ)
* **UI**: Jetpack Compose, Material Design 3, Compose Navigation
* **Data Persistence**: Room Database (SQLite) with KSP compiler, SharedPreferences
* **Dependency Injection**: Hilt (Android Enterprise-Grade Dependency Injection)
* **Asynchronous & Reactive Programming**: Kotlin Coroutines, StateFlow, SharedFlow
* **Network & Parsing**: Retrofit 2, OkHttp 4, Moshi (JSON Serialization)
* **Media Loading**: Coil Compose (مع التخزين المؤقت المتقدم للصور)
* **Security & Cryptography**: Android Keystore API, AES-256, Secure Hashing, Phone Number Normalizer

### **الهيكل الشجري البرمجي (Folder Structure Directory Tree)**

```text
app/src/main/java/com/example/
├── auth/            # Authentication Logic (Firebase Integration & SMS Auth)
├── data/            # Local & Remote Data Sources, Repositories, Entities
│   ├── local/       # Room Database (DAOs, Entities, TypeConverters)
│   ├── remote/      # Firebase, Firestore, Retrofit API Services
│   └── repositories/# Repository Implementations (Offline Cache, Sync logic)
├── domain/          # Pure Business Logic, Models & Use Cases
│   ├── models/      # Domain Models (e.g., UserRole, Booking, ChatChannel)
│   └── usecases/    # Pure Business Rules (Validation, Auth, State Machine)
├── sync/            # Remote Sync Engine (Firestore to Room bidirectional sync)
├── ui/              # Presentation/UI Layer (Jetpack Compose & Material 3 Theming)
│   ├── components/  # Shared Reusable Composables (ReviewInput, Dialogs, Cards)
│   ├── screens/     # Application Screens (Booking, Map, Profile, Admin Panel)
│   └── theme/       # ColorScheme, Typography, Shapes, BookingStatusColors
├── utils/           # Helper Utilities (WalletManager, VoiceManager, FCMService)
└── security/        # Cryptography, Hash Generators, Data Sanitization (AES-256)
```

---

## 5. دليل التثبيت والتشغيل / Setup & Installation Guide

لكي تتمكن من تشغيل وتطوير المشروع محلياً على جهازك، اتبع الخطوات التالية بدقة:

### **الخطوة 1: استنساخ المستودع (Clone the Repository)**
قم بفتح سطر الأوامر (Terminal) وتشغيل الأمر التالي:
```bash
git clone https://github.com/gaber77710/YemenServicesDirectory.git
cd YemenServicesDirectory
```

### **الخطوة 2: الفتح والتهيئة في Android Studio**
1. افتح برنامج **Android Studio** (يُفضل إصدار Ladybug أو أحدث).
2. اختر **Open Project** وقم بتحديد مجلد المشروع الذي استنسخته.
3. انتظر بضع دقائق حتى يقوم نظام البناء **Gradle** بتحميل التبعيات والمزامنة بالكامل.

### **الخطوة 3: إعداد وربط Firebase**
يتطلب التطبيق وجود مشروع Firebase نشط ليعمل بشكل سليم (المصادقة، الشات، الإشعارات):
1. اذهب إلى [Firebase Console](https://console.firebase.google.com/) وأنشئ مشروعاً جديداً باسم `Yemen Services Directory`.
2. أضف تطبيق أندرويد للمشروع مستخدماً اسم الحزمة (Package Name) المعرف في الكود: `com.alyemenservices`.
3. قم بتنزيل ملف الإعدادات `google-services.json` وضعه مباشرة داخل مجلد التطبيق: `app/google-services.json`.
4. قم بتفعيل الخدمات التالية في كونسول Firebase:
   - **Authentication**: تفعيل تسجيل الدخول برقم الهاتف (Phone Sign-In).
   - **Cloud Firestore**: إنشاء قاعدة بيانات Firestore في وضع البداية الآمنة.
   - **Cloud Storage**: لرفع الصور والملاحظات الصوتية الخاصة بالمحادثات.
   - **Cloud Messaging (FCM)**: لإرسال الإشعارات الفورية.

### **الخطوة 4: ملف بيئة العمل والأسرار (.env)**
التطبيق يستخدم نظام حماية ذكي لقراءة المفاتيح السرية عبر الـ Gradle Secrets Plugin.
1. ابحث عن ملف `.env.example` في الجذر وقم بإنشاء نسخة منه باسم `.env` في نفس المسار.
2. أدخل مفاتيحك الخاصة مثل `AGORA_APP_ID` (المستخدم للمحادثات الصوتية) والمفاتيح السحابية الأخرى.

### **الخطوة 5: البناء والتشغيل (Build & Run)**
1. قم بتوصيل هاتف أندرويد حقيقي أو تشغيل محاكي الأندرويد (Emulator).
2. اضغط على زر **Run (السهم الأخضر)** أو اضغط `Shift + F10` في نظام البناء.
3. سيتم تثبيت ملف الـ APK النهائي وبدء تشغيل واجهة دليل خدمات اليمن مباشرة.

---

## 6. استراتيجية وجدول الاختبارات / Testing Strategy & Grid

يحتوي التطبيق على بنية اختبارات شاملة ومتقدمة جداً تضم **224 اختباراً آلياً** للتأكد من ثبات واستقرار العمليات وحماية البيانات من الانهيار.

### **أقسام ومستويات الاختبارات في التطبيق:**
1. **Local JVM Unit Tests**: اختبارات منطق الأعمال الصرفة وحسابات المحفظة والتحقق من الهواتف والمطابقة النصية لسرعة فائقة.
2. **Robolectric Local Tests**: اختبارات لمحاكاة إطار عمل أندرويد على محرك جافا المحلي دون الحاجة لمحاكي حقيقي، واختبار عمل المزامنة والكاش محلياً.
3. **Jetpack Compose UI Tests**: اختبارات التحقق من ظهور العناصر، تدفق واجهة الحجوزات وتصفح الخدمات والمدخلات.
4. **Firebase Integration & Offline Queues**: اختبار محاكاة انقطاع الإنترنت ومراقبة طابور الإرسال التلقائي والتخزين المؤقت الآمن.

### **جدول تغطية الاختبارات المفصل (Test Coverage Grid):**

| اسم ملف الاختبار (Test File Name) | النوع (Type) | التغطية والميزات المختبرة (Coverage & Tested Features) | عدد الاختبارات (Tests) | الحالة |
| :--- | :--- | :--- | :---: | :---: |
| **`BookingRepositoryTest.kt`** | Unit / Local JVM | إدارة الحجوزات، التحقق من رمز PIN، منع التعارض الزمني، وإلغاء الحجز ضمن قاعدة 8 ساعات | 15 اختباراً | ناجح ✅ |
| **`ChatRepositoryTest.kt`** | Unit / Integration | إرسال واستقبال الرسائل، معالجة طابور الرسائل عند عدم الاتصال بالشبكة (Offline Queue) | 12 اختباراً | ناجح ✅ |
| **`WalletManagerTest.kt`** | Unit / Logic | المعاملات المالية بالريال اليمني (YER)، الإيداع والسحب والتحويل، ومنع الكسور وتأمين الحسابات | 10 اختبارات | ناجح ✅ |
| **`ValidatePhoneUseCaseTest.kt`** | Unit / Validator | التحقق من أرقام الهواتف اليمنية بكل الصيغ وقبول/رفض الشبكات المحلية (يمن موبايل، سبأفون، يو، واي) | 8 اختبارات | ناجح ✅ |
| **`AppPreferenceHelperTest.kt`** | Unit / Logic | تطبيع أرقام الهواتف (Normalize Phone Number)، وحفظ واسترجاع تفضيلات المستخدم محلياً | 6 اختبارات | ناجح ✅ |
| **`ChatCryptoManagerTest.kt`** | Unit / Security | تشفير وفك تشفير الرسائل والوسائط بـ AES-256، وكشف محاولات التلاعب بالبيانات | 6 اختبارات | ناجح ✅ |
| **`BookingStateMachineTest.kt`** | Unit / Logic | التحقق من انتقال حالات الحجز بشكل منطقي وقانوني وحماية سلامة تدفق العمليات من التلاعب | 6 اختبارات | ناجح ✅ |
| **`ServicesListScreenUiTest.kt`** | Robolectric / UI | محاكاة واجهة تصفح الخدمات ومقدمي الخدمات والتحقق من العناصر وتفاعل الأزرار وسهولة الوصول | 8 اختبارات | ناجح ✅ |
| **`UserAuthIntegrationTest.kt`** | Integration / Firebase | تكامل المصادقة وتوثيق أدوار المستخدمين والتحقق من صحة الجلسات | 8 اختبارات | ناجح ✅ |
| **`RoomDatabaseUnitTest.kt`** | Unit / Database | اختبار عمليات حفظ واستعلام الجداول محلياً بالكامل ومطابقة قيود SQLite المدمجة | 10 اختبارات | ناجح ✅ |
| **`OfflineQueueIntegrationTest.kt`**| Integration | مزامنة طابور العمليات غير المرسلة سحابياً عند عودة الاتصال بالإنترنت تلقائياً وثبات الترتيب | 8 اختبارات | ناجح ✅ |
| **`MapDistanceCalculatorTest.kt`** | Unit / Location | حساب المسافة الدقيقة بين المستخدم ومقدمي الخدمات جغرافياً بالاعتماد على صيغة هافرسين الجيوديسية | 6 اختبارات | ناجح ✅ |
| **`SecureHasherTest.kt`** | Unit / Security | تشفير وحماية رموز الـ PIN للمستخدمين باستخدام خوارزميات التشفير الآمنة وعملية التحقق المتطابقة | 5 اختبارات | ناجح ✅ |
| **ملفات أخرى واختبارات فرعية** | Unit / Robolectric | اختبار حالات الحواف، معالجة الاستثناءات، تحسينات الأداء، ومطابقة النصوص بالتقارب الإملائي | 117 اختباراً | ناجح ✅ |
| **المجموع الإجمالي (Total)** | **وحدة وتكامل وواجهات**| **تغطية شاملة لجميع الجوانب الحرجة والمسارات الرئيسية للتطبيق** | **224 اختباراً** | **ناجح 100%** |

---

## 7. حالة ميزات المشروع / Project Feature Status

*يعرض الجدول التالي الحالة البرمجية الفعلية لميزات المشروع لتوجيه المطورين والمهتمين بالتقدم:*

| الميزة الفنية (Technical Feature) | الأولوية (Priority) | الحالة الحالية (Status) | مؤشر التلوين (Visual Indicator) |
| :--- | :---: | :---: | :---: |
| **تسجيل الدخول والتحقق برقم الهاتف** | حرجة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **الحجز وإدارة المواعيد برمز PIN** | حرجة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **نظام الشات الفوري المشفر بـ AES-256**| حرجة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **طلبات الطوارئ الفورية واستقبال العروض**| عالية | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **الخريطة التفاعلية وحساب المسافة الذكي**| عالية | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **المحفظة الإلكترونية بالريال اليمني** | عالية | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **مركز الإشعارات المتقدم ذو القنوات الثلاث**| متوسطة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **البحث الصوتي ومحرك اللهجة المحلية** | متوسطة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **تصميم Edge-to-Edge ودعم RTL** | متوسطة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **لوحة تحكم الأدمن العملاقة وإدارتها** | منخفضة | مكتملة ✅ | `🟩 الأخضر (مستقر وعامل)` |
| **اختبارات لقطة الشاشة (Screenshot Tests)**| متوسطة | قيد التطوير 🚧 | `🟨 الأصفر (مرحلة التطوير الحالية)` |
| **دعم المزامنة اللحظية على الخادم المحلي**| منخفضة | مخطط لها 📋 | `🟦 الأزرق (الخطوات المستقبلية)` |

---

## 8. المساهمة وتطوير الكود / Contributing & Code Standards

نحن نرحب بمساهمات المطورين لتحسين جودة تطبيق "دليل خدمات اليمن". يرجى قراءة القواعد والممارسات المعتمدة أدناه قبل بدء المساهمة لتسهيل دمج الكود البرمجي الخاص بك:

### **قواعد تسمية الفروع (Branch Naming Conventions)**
تسمية الفروع تساعد في الحفاظ على تنظيم المستودع وفهم محتوى التعديل فوراً:
- للميزات الجديدة: `feature/feature-name` (مثال: `feature/screenshot-tests-roborazzi`)
- لإصلاح الأخطاء: `bugfix/issue-name` (مثال: `bugfix/fix-chat-scroll-glitch`)
- للتحديثات العاجلة: `hotfix/urgent-fix-name`
- للتحسينات التوثيقية: `docs/documentation-update`

### **صيغة رسائل الالتزام (Commit Message Conventions)**
نحن نلتزم بالمعايير العالمية **Conventional Commits** لتوحيد تاريخ التعديلات البرمجية:
* **إضافة ميزة:** `feat: add screen selection animation`
* **إصلاح خطأ:** `fix: resolve secure pin comparison edge case`
* **توثيق:** `docs: update setup steps in readme`
* **اختبارات:** `test: implement roborazzi verification screenshots`
* **تنظيف الكود:** `refactor: modularize chat card elements`

### **معايير الكود المستخدمة (Code Style & Quality Standards)**
- **Touch Targets**: يجب أن تكون مساحة النقر لأي عنصر تفاعلي لا تقل عن **`48.dp`** تماشياً مع معايير الوصول.
- **RTL Support**: ممنوع استخدام قيم الهامش أو الحشو المطلقة (مثل `paddingLeft` أو `paddingRight`) بل يجب استخدام البداية والنهاية النسبيتين (`paddingStart` و `paddingEnd`) لضمان العمل التلقائي للغات اليمين واليسار.
- **Large Files**: يمنع منعاً باتاً زيادة حجم الملفات الكبيرة بشكل عشوائي. أي ميزة جديدة أو لوحة فرعية يجب أن تُنشأ في ملف منفصل ومستقل.
- **State Flow**: يجب إدارة حالة الواجهات عبر الـ ViewModel باستخدام `StateFlow` وليس `LiveData` لضمان التوافق التام مع Compose.

### **كيفية تشغيل الاختبارات قبل الإرسال (Pre-PR Checklist)**
تأكد من تشغيل الاختبارات المحلية والتحقق من صحتها بنسبة 100% قبل إنشاء طلب دمج (Pull Request):
```bash
# تشغيل جميع اختبارات الوحدة و Robolectric
gradle :app:testDebugUnitTest

# التحقق من سلامة الأكواد والتركيب النحوي
gradle lintDebug
```

---

## 9. الأسئلة الشائعة / FAQ

#### **س1: كيف أشغّل التطبيق لأول مرة ولا يوجد لدي حساب مستخدم؟**
* **ج:** يدعم التطبيق تسجيل مستخدم جديد بالكامل برقم الهاتف. أدخل رقم هاتفك اليمني وسيصلك كود التحقق (أو يمكنك استخدام كود التحقق التجريبي `123456` في وضع التطوير المحلي إذا كنت تستخدم الحساب التجريبي المعرف في Firebase Auth).

#### **س2: كيف أربط التطبيق بمشروع Firebase الخاص بي؟**
* **ج:** اتبع الخطوات البرمجية المذكورة في [قسم دليل التثبيت والتشغيل](#5-دليل-التثبيت-والتشغيل--setup--installation-guide). تأكد من استبدال ملف `google-services.json` بملفك الخاص وتحديث إعدادات أرقام الهواتف وقواعد Firestore وStorage في الكونسول الخاص بك.

#### **س3: لماذا يفشل بناء Gradle عند محاولة تشغيل الاختبارات في البيئة المحلية؟**
* **ج:** تأكد من أنك تستخدم إصدار JDK 17 أو JDK 11 كحد أدنى، وتحقق من عدم وجود أي تعليق في خادم Gradle أو وجود ملفات مكررة. يمكنك تنظيف الكاش محلياً عن طريق تشغيل أمر `gradle clean` وإعادة المزامنة.

#### **س4: كيف أضيف ميزة جديدة للدليل؟**
* **ج:** ابدأ بإنشاء نموذج البيانات الخاص بالميزة في طبقة `domain/models` ثم أضف مستودع البيانات وحقول الـ Room في `data/` ومستودع Firestore، ثم اربطها بـ `ViewModel` مخصص واعرض الواجهات بـ `Compose`. احرص على أن تكون الميزة معزولة عن واجهة الأدمن العملاقة تجنباً لتضخم الملفات.

#### **س5: كيف أبلغ عن مشكلة برمجية أو انهيار في التطبيق؟**
* **ج:** يرجى التوجه إلى قسم **Issues** في المستودع وفتح تذكرة جديدة مع إرفاق سجل الانهيار (Logcat) وخطوات إعادة إنتاج المشكلة لمساعدتنا على حلها فوراً.

---

## 10. الترخيص والتواصل / License & Contact

### **الترخيص (License)**
هذا المشروع مرخص ومتاح بموجب رخصة **MIT**. يمكنك الاطلاع على الملف الكامل للرخصة لمعرفة حدود الاستخدام الشخصي والتجاري.

### **التواصل ومتابعة التطوير (Contact & Support)**
- **البريد الإلكتروني المباشر**: [gaber77710@gmail.com](mailto:gaber77710@gmail.com)
- **رابط المستودع على GitHub**: [https://github.com/gaber77710/YemenServicesDirectory](https://github.com/gaber77710/YemenServicesDirectory)
- **منصة التطوير ومتابعة التحديثات**: يمكنك فتح تذكرة عبر قسم الـ Issues لمناقشة الأفكار أو الميزات الجديدة.

---

<p align="center">
  <b>صُنع بحب ومسؤولية هندسية لخدمة المجتمع اليمني وتسهيل أعماله اليومية 🇾🇪 ❤️</b>
</p>
