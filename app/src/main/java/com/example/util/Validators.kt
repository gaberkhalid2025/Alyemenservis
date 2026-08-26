package com.example.util

/**
 * 📋 ValidationResult - نتيجة التحقق من صحة البيانات
 * @param isValid هل البيانات مقبولة وصالحة
 * @param errorMessage رسالة الخطأ التوجيهية بالعربية عند عدم الصلاحية
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * 🛡️ Validators - طبقة التحقق من صحة المدخلات وأرقام الهواتف وكلمات المرور في اليمن
 * 
 * الميزات:
 * 1. التحقق من أرقام الهواتف اليمنية (يمن موبايل 77/78، يو 73، سبأفون 71، واي 70) مع دعم البادئة الدولية +967.
 * 2. التحقق من قوة كلمة المرور والامتثال لسياسة الأمان ومنع التخمين السهل.
 * 3. التحقق من صحة البريد الإلكتروني والأسماء والعناوين والمبالغ المالية.
 */
object Validators {

    /**
     * التحقق من صحة رقم الهاتف اليمني (9 أرقام تبدأ بـ 77، 78، 73، 71، 70)
     * 
     * @param phone رقم الهاتف المدخل
     * @return نتيجة التحقق مع رسالة التوجيه
     */
    fun validateYemenPhone(phone: String?): ValidationResult {
        if (phone.isNullOrBlank()) {
            return ValidationResult(false, "يرجى إدخال رقم الهاتف.")
        }
        val clean = phone.trim().replace(" ", "").replace("-", "")
        val localDigits = when {
            clean.startsWith("+967") -> clean.substring(4)
            clean.startsWith("00967") -> clean.substring(5)
            clean.startsWith("967") -> clean.substring(3)
            clean.startsWith("0") -> clean.substring(1)
            else -> clean
        }

        if (localDigits.length != 9 || !localDigits.all { it.isDigit() }) {
            return ValidationResult(false, "رقم الهاتف يجب أن يتكون من 9 أرقام (مثال: 771234567).")
        }

        val prefix = localDigits.substring(0, 2)
        val validPrefixes = listOf("77", "78", "73", "71", "70")
        if (prefix !in validPrefixes) {
            return ValidationResult(false, "رقم الهاتف يجب أن يبدأ بشركة اتصالات يمنية معتمدة (77، 78، 73، 71، 70).")
        }

        return ValidationResult(true)
    }

    /**
     * التحقق من قوة كلمة المرور ومطابقتها للشروط
     */
    fun validatePassword(password: String?): ValidationResult {
        if (password.isNullOrBlank()) {
            return ValidationResult(false, "يرجى إدخال كلمة المرور.")
        }
        val (policyValid, policyError) = SecurityCryptoUtils.validatePasswordPolicy(password)
        if (!policyValid) {
            return ValidationResult(false, policyError)
        }
        val pass = password.trim()
        val hasLetter = pass.any { it.isLetter() }
        val hasDigit = pass.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return ValidationResult(false, "كلمة المرور يجب أن تحتوي على أحرف وأرقام معاً.")
        }
        return ValidationResult(true)
    }

    /**
     * التحقق من صحة الاسم
     */
    fun validateName(name: String?, fieldLabel: String = "الاسم"): ValidationResult {
        if (name.isNullOrBlank()) {
            return ValidationResult(false, "يرجى إدخال $fieldLabel.")
        }
        val cleanName = SecurityCryptoUtils.sanitizeInput(name)
        if (cleanName.length < 3) {
            return ValidationResult(false, "$fieldLabel يجب أن يتكون من 3 أحرف على الأقل.")
        }
        return ValidationResult(true)
    }

    /**
     * التحقق من صحة البريد الإلكتروني
     */
    fun validateEmail(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult(true) // البريد اختياري في أغلب تدفقات التطبيق
        }
        val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        if (!emailPattern.matches(email.trim())) {
            return ValidationResult(false, "صيغة البريد الإلكتروني غير صحيحة.")
        }
        return ValidationResult(true)
    }

    /**
     * التحقق من صحة المبلغ المالي
     */
    fun validateAmount(amountStr: String?): ValidationResult {
        if (amountStr.isNullOrBlank()) {
            return ValidationResult(false, "يرجى تحديد المبلغ.")
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            return ValidationResult(false, "المبلغ يجب أن يكون رقماً موجباً أكبر من الصفر.")
        }
        return ValidationResult(true)
    }
}

/**
 * 👨‍🔧 ProviderValidator - التحقق من بيانات الفني أو مزود الخدمة
 */
object ProviderValidator {
    fun validate(name: String?, phone: String?, serviceCategory: String?): ValidationResult {
        val nameRes = Validators.validateName(name, "اسم مزود الخدمة")
        if (!nameRes.isValid) return nameRes

        val phoneRes = Validators.validateYemenPhone(phone)
        if (!phoneRes.isValid) return phoneRes

        if (serviceCategory.isNullOrBlank()) {
            return ValidationResult(false, "يرجى اختيار القسم الرئيسي للخدمة.")
        }

        return ValidationResult(true)
    }
}

/**
 * 🏪 StoreValidator - التحقق من بيانات المحل أو المركز التجاري
 */
object StoreValidator {
    fun validate(storeName: String?, phone: String?, category: String?, address: String?): ValidationResult {
        val nameRes = Validators.validateName(storeName, "اسم المحل / المركز")
        if (!nameRes.isValid) return nameRes

        val phoneRes = Validators.validateYemenPhone(phone)
        if (!phoneRes.isValid) return phoneRes

        if (category.isNullOrBlank()) {
            return ValidationResult(false, "يرجى تحديد النشاط أو القسم التجاري.")
        }

        if (address.isNullOrBlank() || address.trim().length < 3) {
            return ValidationResult(false, "يرجى تحديد تفاصيل العنوان أو المنطقة.")
        }

        return ValidationResult(true)
    }
}

/**
 * 📅 BookingValidator - التحقق من بيانات الحجز والطلب
 */
object BookingValidator {
    fun validate(customerName: String?, customerPhone: String?, serviceId: String?, date: String?): ValidationResult {
        val nameRes = Validators.validateName(customerName, "اسم صاحب الطلب")
        if (!nameRes.isValid) return nameRes

        val phoneRes = Validators.validateYemenPhone(customerPhone)
        if (!phoneRes.isValid) return phoneRes

        if (serviceId.isNullOrBlank()) {
            return ValidationResult(false, "لم يتم تحديد الخدمة المطلوبة للحجز.")
        }

        if (date.isNullOrBlank()) {
            return ValidationResult(false, "يرجى تحديد التاريخ والوقت المناسب للحجز.")
        }

        return ValidationResult(true)
    }
}

/**
 * 👤 UserValidator - التحقق من بيانات تسجيل المستخدم وتسجيل الدخول
 */
object UserValidator {
    fun validateRegistration(name: String?, phone: String?, password: String?): ValidationResult {
        val nameRes = Validators.validateName(name, "اسم المستخدم")
        if (!nameRes.isValid) return nameRes

        val phoneRes = Validators.validateYemenPhone(phone)
        if (!phoneRes.isValid) return phoneRes

        val passRes = Validators.validatePassword(password)
        if (!passRes.isValid) return passRes

        return ValidationResult(true)
    }

    fun validateLogin(phone: String?, password: String?): ValidationResult {
        val phoneRes = Validators.validateYemenPhone(phone)
        if (!phoneRes.isValid) return phoneRes

        if (password.isNullOrBlank()) {
            return ValidationResult(false, "يرجى إدخال كلمة المرور.")
        }

        return ValidationResult(true)
    }
}
