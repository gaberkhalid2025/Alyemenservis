# R8 / ProGuard Configuration Rules for Yemen Services Platform

# Keep Kotlin reflect and serializable models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.example.model.** { *; }
-keep class com.example.entity.** { *; }
-keep class com.example.data.** { *; }

# Security, Hashing, and Anti-Tampering Protection Rules
-keep class com.example.util.PasswordHasher { *; }
-keep class com.example.util.SecurityManager { *; }
-keep class com.example.util.SecurityCryptoUtils { *; }
-keep class com.example.util.FirestoreLocalBackupWorker { *; }
-keep class com.example.util.Validators { *; }
-keep class com.example.ui.MainViewModel { *; }

# Firebase Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepclassmembers class * extends com.google.firebase.firestore.EventListener { *; }

# Compose Rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Moshi & OkHttp / Retrofit
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**


