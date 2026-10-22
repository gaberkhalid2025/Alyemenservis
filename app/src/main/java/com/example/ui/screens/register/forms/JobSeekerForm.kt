package com.example.ui.screens.register.forms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.submitJoinForm
import com.example.ui.setJoinRequestPhone
import com.example.utils.VisualThemePalette

/**
 * 👨‍💼 JobSeekerForm
 * استمارة المتقدم للوظائف (باحث عن عمل)
 * الحقول المعتمدة: الاسم الثلاثي، الهاتف، كلمة المرور، التأكيد، المجال المطلوب، سنوات الخبرة، المدينة، الحي، المؤهل العلمي، نبذة، رابط السيرة الذاتية
 */
@Composable
fun JobSeekerForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    UnifiedRegistrationForm(
        role = "JOB_SEEKER",
        themeColors = themeColors,
        onRegistrationSuccess = { data ->
            val fullName = (data["fullName"] as? String) ?: ""
            val phone = (data["phone"] as? String) ?: ""
            val password = (data["password"] as? String) ?: ""
            val city = (data["city"] as? String) ?: "صنعاء"
            val address = (data["address"] as? String) ?: ""
            val craftType = (data["craftType"] as? String) ?: "باحث عن عمل"

            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")

            viewModel.submitJoinForm(
                context = context,
                name = fullName,
                phone = cleanPhone,
                catId = "JOB_SEEKER",
                area = city,
                neighborhood = address,
                photoPath = (data["imageUri"] as? String) ?: "",
                idCardPath = (data["idCardUri"] as? String) ?: "",
                gpsCoords = "",
                customCategoryName = craftType,
                password = password
            )

            viewModel.setJoinRequestPhone(context, cleanPhone)
            viewModel.triggerNotification("✅ تم إرسال طلب التقديم للوظيفة بنجاح، جاري المراجعة من الإدارة")
            onSuccess()
        }
    )
}
