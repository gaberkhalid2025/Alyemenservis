# 🛠️ دليل المطور (Developer Architecture & Guidelines)

## 📁 هيكلية المشروع (Project Architecture)

المشروع مبني وفق معمارية **MVVM + Clean Architecture** النظيفة في نظام Android:

```
com.example/
├── data/                       # نماذج البيانات وكيانات Room
│   ├── ProviderEntity.kt
│   ├── StoreEntity.kt
│   ├── PropertyEntity.kt
│   ├── BookingEntity.kt
│   ├── NotificationEntity.kt
│   ├── ChatMessageEntity.kt
│   └── AdminSettingsEntity.kt
├── ui/
│   ├── MainViewModel.kt        # إدارة الحالة المركزية
│   ├── screens/
│   │   ├── assistant/          # المساعد الذكي والصوتي
│   │   ├── map/                # خريطة Leaflet والتحديد الجغرافي
│   │   ├── chat/               # المحادثات الفورية والوسائط
│   │   ├── dashboard/          # لوحات تحكم مزودي الخدمات
│   │   ├── notifications/      # مركز الإشعارات
│   │   └── home/               # الشاشة الرئيسية والتصنيفات
│   └── theme/                  # ثيمات التطبيق والألوان
└── util/
    ├── AiAssistantEngine.kt    # محرك الذكاء الاصطناعي واللهجة اليمنية
    ├── VoiceManager.kt         # محرك النطق والتعرف الصوتي
    ├── LeafletMapHelper.kt     # أدوات الخرائط وإحداثيات المحافظات
    └── BookingNotificationManager.kt
```

---

## ⚡ قواعد البناء والأداء
- **توفير البيانات**: تم ضبط التخزين المؤقت المحلي (Local Cache First) للتقليل من استهلاك حزم الإنترنت.
- **التوافق**: الحد الأدنى لإصدار أندرويد SDK 24+ مع دعم كامل لـ Android 14 و Android 15.
- **الخرائط**: تعتمد على Leaflet JS و OpenStreetMap دون أي تكاليف اشتراك أو قيود Google Maps API.
- **الصوت**: يعتمد على محرك TextToSpeech ومسجل Android SpeechRecognizer المدمج بالنظام لضمان الاستجابة السريعة دون اتصال.
