# CHANGELOG - المرحلة الثانية: إعادة الهيكلة وإصلاح البنية المعمارية

## [الإصلاح المعماري والتقسيم النمطي الشامل] - 2026-09-11

### 1. تقسيم وتفكيك لوحة الصلاحيات (`AdminRolesPermissionsPanel`):
- إنشاء حزمة مخصصة `com.example.ui.screens.admin.permissions`.
- إنشاء `AdminPermissionsViewModel`: إدارة حالات الفلترة، البحث، واختيار الصلاحيات والـ 38 مجالاً بشكل مستقل.
- إنشاء `AdminRoleSelectorSection`: اختيار الأدوار الإدارية (Owner, Admin, Supervisor, Auditor, Support, Operations).
- إنشاء `AdminPermissionsActionsSection`: شريط الأدوات، الإجراءات السريعة، وأدوات البحث والفلاتر.
- إنشاء `AdminPermissionsMatrixListSection`: عرض مصفوفة الـ 538 صلاحية مقسمة ومصنفة حسب مستويات الأمان.
- تحويل `AdminRolesPermissionsPanel.kt` إلى Composable موجه ومجمع (Router/Container) نظيف ومحدد المسؤوليات.

### 2. تقسيم وتفكيك `AdminViewModel`:
- استخراج إدارة المتاجر إلى `StoreManagementViewModel`.
- استخراج إدارة العقارات إلى `PropertyManagementViewModel`.
- استخراج إدارة الوظائف والتقديم إلى `JobManagementViewModel`.
- استخراج إدارة المحافظ والمعاملات المالية والدفعات إلى `PaymentManagementViewModel`.
- ربط النماذج الفرعية ضمن `MainViewModel` مع إبقاء التوافقية العكسية بدون كسر أي استدعاءات.

### 3. تفكيك وهيكلة `ChatViewModel`:
- إنشاء حزمة الإدارة المتخصصة `com.example.ui.screens.chat.managers`.
- إنشاء `ChatMessagesManager`: معالجة إرسال واسترجاع الرسائل والوسائط وحالات الإرسال.
- إنشاء `ChatTypingManager`: إدارة حالة الكتابة ومؤشرات جاري الكتابة مع المؤقتات.
- إنشاء `ChatPresenceManager`: تتبع حضور وحالة اتصال المستخدمين الآخرين.
- إنشاء `ChatEditManager`: إدارة تعديل وحذف والرد على الرسائل.
- ربط هذه المدراء داخل `ChatViewModel` لتقليل التعقيد البرمجي وتحسين القابلية للصيانة.

### 4. ربط وتفعيل الأدوات والذاكرة المؤقتة لتقليل استهلاك الإنترنت:
- تفعيل وتضمين `BookingCache` مع `BookingRepository` لتقليل قراءات الـ Firestore وتحسين تجربة الـ Offline-First.
- تأمين استمارات التسجيل في `RegistrationHelper` باستخدام `PasswordHasher` للتشفير والتحقق المحكم.
- وضع قيود استعلامية `.limit()` على مجموعات Firestore المستمعة دورياً مثل `registered_users` و `internal_wallets` و `wallet_transactions` لتوفير استهلاك بيانات الإنترنت وحماية الحصص الشهرية.
