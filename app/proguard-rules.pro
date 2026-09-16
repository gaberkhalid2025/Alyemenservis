# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.gms.internal.**

# ===== Firestore Models =====
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepclassmembers class * extends com.google.firebase.firestore.EventListener { *; }

# ===== App Models (data classes) =====
-keep class com.example.data.** { *; }
-keep class com.example.data.models.** { *; }
-keep class com.example.domain.entities.** { *; }
-keep class com.example.model.** { *; }
-keep class com.example.entity.** { *; }

# ===== Moshi =====
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json *;
}
-dontwarn com.squareup.moshi.**
-dontwarn okio.**

# ===== Retrofit / OkHttp =====
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===== Hilt =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# ===== Kotlin Coroutines =====
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ===== Compose =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===== Enum =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Parcelable =====
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ===== General =====
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ===== App Specific =====
-keep class com.example.utils.PasswordHasher { *; }
-keep class com.example.utils.SecureHasher { *; }
-keep class com.example.utils.PinHasher { *; }
-keep class com.example.security.SecurityManager { *; }
-keep class com.example.security.BookingSecurityHelper { *; }
-keep class com.example.utils.SecurityCryptoUtils { *; }
-keep class com.example.utils.ChatCryptoManager { *; }
-keep class com.example.ui.MainViewModel { *; }
-keep class com.example.utils.FirestoreLocalBackupWorker { *; }
-keep class com.example.utils.Validators { *; }



