package com.example.ui.screens.register.forms

/**
 * 📋 RegistrationForm - واجهة موحدة لجميع نماذج التسجيل والانضمام
 * تلزم كل نموذج بالتحقق من الحقول، الإرسال وإعادة التعيين
 */
interface RegistrationForm {
    /**
     * التحقق من صحة المدخلات قبل الإرسال
     * @return FormValidationResult نتيجة التحقق
     */
    fun validate(): FormValidationResult

    /**
     * إرسال النموذج وحفظ البيانات بالسحابة
     */
    fun submit()

    /**
     * إعادة تعيين الحقول إلى حالتها الافتراضية
     */
    fun reset()
}

/**
 * نتيجة التحقق من صحة النموذج
 */
data class FormValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)
