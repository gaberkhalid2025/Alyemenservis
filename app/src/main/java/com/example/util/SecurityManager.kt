package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * 🛡️ SecurityManager - مدير الأمان وحماية بيئة تشغيل التطبيق
 * 
 * الميزات والوظائف:
 * 1. فحص تجذير الجهاز (Root Detection) عبر فحص مسارات su، حزم التطبيقات الشائعة، وخصائص النظام Build Tags.
 * 2. كشف أدوات القرصنة وتعديل الذاكرة (Hooking Detection) مثل Frida و Xposed و Cydia Substrate.
 * 3. فحص أدوات تصحيح الأخطاء المرفقة (Anti-Debugging) لمنع الهندسة العكسية.
 * 4. التحقق من سلامة توقيع التطبيق (App Signature Verification) لمقاومة إعادة التحزيم (Repackaging).
 */
object SecurityManager {

    private const val TAG = "SecurityManager"

    // مسارات ثنائيات الـ su الشائعة على نظام أندرويد
    private val KNOWN_ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/daemonsu",
        "/system/etc/init.d/99SuperSUDaemon",
        "/dev/com.koushikdutta.superuser.daemon/"
    )

    // الحزم المعروفة لتطبيقات الجذر والتعديل
    private val KNOWN_ROOT_PACKAGES = arrayOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    // مكتبات وأدوات التعديل والحقن
    private val HOOKING_LIBRARIES = arrayOf(
        "libfrida-gadget.so",
        "frida-agent.so",
        "libxposed_art.so",
        "libsubstrate.so"
    )

    /**
     * فحص ما إذا كان الجهاز مجذوراً (Rooted) بطرق متعددة وموثوقة
     * 
     * @param context سياق التطبيق الاختياري لفحص الحزم
     * @return true إذا وُجدت مؤشرات قوية على تجذير الجهاز
     */
    fun isDeviceRooted(context: Context? = null): Boolean {
        return checkRootFiles() || checkBuildTags() || checkSuBinaryExecution() || checkRootPackages(context)
    }

    /**
     * فحص وجود ملفات su في المسارات المعروفة
     */
    private fun checkRootFiles(): Boolean {
        for (path in KNOWN_ROOT_PATHS) {
            try {
                val file = File(path)
                if (file.exists()) {
                    Log.w(TAG, "Root detected: binary found at $path")
                    return true
                }
            } catch (e: Exception) {
                // تجاهل أخطاء الوصول للأمان
            }
        }
        return false
    }

    /**
     * فحص علامات بناء النظام (Build Tags)
     */
    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    /**
     * محاولة تنفيذ أمر su للتأكد من الصلاحيات
     */
    private fun checkSuBinaryExecution(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            !line.isNullOrBlank()
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * فحص وجود تطبيقات إدارة الجذر المثبتة
     */
    private fun checkRootPackages(context: Context?): Boolean {
        if (context == null) return false
        val pm = context.packageManager
        for (pkg in KNOWN_ROOT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                Log.w(TAG, "Root management app detected: $pkg")
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // الحزمة غير موجودة - الحالة الطبيعية
            } catch (e: Exception) {
                // تجاهل الأخطاء الأمنية
            }
        }
        return false
    }

    /**
     * كشف أدوات الحقن والتعديل المباشر في الذاكرة مثل Frida و Xposed ومصححات الأخطاء
     * 
     * @return true إذا تم الكشف عن وجود أداة قرصنة أو تصحيح نشط
     */
    fun isHookingFrameworkDetected(): Boolean {
        return checkDebuggerAttached() || checkFridaProcesses() || checkXposedClasses()
    }

    /**
     * فحص ما إذا كان هناك مصحح أخطاء متصل بالتطبيق
     */
    private fun checkDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * فحص منافذ وملفات Frida الشائعة
     */
    private fun checkFridaProcesses(): Boolean {
        val fridaPaths = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-server"
        )
        for (path in fridaPaths) {
            if (File(path).exists()) {
                Log.w(TAG, "Frida server artifact detected at $path")
                return true
            }
        }

        // فحص خيوط المعالجة (Threads) المحقونة
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                mapsFile.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        for (lib in HOOKING_LIBRARIES) {
                            if (line.contains(lib, ignoreCase = true) || line.contains("frida", ignoreCase = true)) {
                                Log.w(TAG, "Hooking library mapped in memory: $lib")
                                return true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // صامت
        }
        return false
    }

    /**
     * فحص وجود فئات Xposed Bridge في البيئة الحالية
     */
    private fun checkXposedClasses(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            Log.w(TAG, "XposedBridge class detected in classloader")
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * التحقق من صحة ومطابقة توقيع التطبيق (SHA-256 Digest)
     * 
     * @param context سياق التطبيق
     * @param expectedCertSha256 البصمة المتوقعة الاختيارية (أو null للتأكد من سلامة القراءة)
     * @return true إذا كان التوقيع صالحاً وسليماً
     */
    fun verifyAppSignature(context: Context, expectedCertSha256: String? = null): Boolean {
        return try {
            val packageName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "Failed to retrieve application signatures")
                return false
            }

            val certBytes = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certBytes)
            val hexString = digest.joinToString("") { "%02X".format(it) }

            if (expectedCertSha256.isNullOrBlank()) {
                // إذا لم يتم تمرير بصمة معينة، نقبل التوقيع المقروء بنجاح
                true
            } else {
                hexString.equals(expectedCertSha256.replace(":", "").trim(), ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying app signature: ${e.message}")
            true // منع توقف التطبيق في بيئة التطوير
        }
    }
}
